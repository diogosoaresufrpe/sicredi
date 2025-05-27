package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.DetalheRecorrenciaPain011Dto;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11;
import br.com.sicredi.spi.entities.type.TipoRecorrencia;
import br.com.sicredi.spi.entities.type.TipoSituacaoPain011;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamentoRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.*;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutorizacaoCancelamentoProtocoloServiceImpl implements AutorizacaoCancelamentoProtocoloService {

    public static final int TAMANHO_CPNJ = 14;
    private final RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;
    private final RecorrenciaAutorizacaoRepository autorizacaoRepository;
    private final EventoResponseFactory eventoResponseFactory;
    private final CriaResponseStrategyFactory<CancelamentoAutorizacaoRequest> criaResponseStrategyFactory;
    private final Validator validator;

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processaCancelamentoRecorrenciaAutorizacao(IdempotentRequest<CancelamentoAutorizacaoRequest> request) {
        try {
            var autorizacaoRecorrenciaProtocolo = request.getValue();
            MDC.put(RecorrenciaMdc.ID_INFORMACAO_CANCELAMENTO.getChave(), autorizacaoRecorrenciaProtocolo.getIdInformacaoCancelamento());
            log.debug("(Pain011) Processamento de pedido de cancelamento de autorização pelo PSP do Pagador.");

            var contemErrorDeConstraint = validarRequest(autorizacaoRecorrenciaProtocolo);
            if (contemErrorDeConstraint.isPresent()) {
                log.error("Erro de validações de constraints do pedido de cancelamento de autorização pelo PSP do Pagador. ID_INFORMACAO_CANCELAMENTO: {}, Mensagem de erro: {}",
                        autorizacaoRecorrenciaProtocolo.getIdInformacaoCancelamento(),
                        contemErrorDeConstraint.get().mensagemErro()
                );
                return criarResponseIdempotentErro(autorizacaoRecorrenciaProtocolo, request, contemErrorDeConstraint.get());
            }

            var retornoValidacaoRegra = validaRegras(autorizacaoRecorrenciaProtocolo);
            if (retornoValidacaoRegra.invalido()) {
                log.error("Erro de validações de negócio do pedido de cancelamento de autorização pelo PSP do Pagador. ID_INFORMACAO_CANCELAMENTO: {}, Mensagem de erro: {}",
                        autorizacaoRecorrenciaProtocolo.getIdInformacaoCancelamento(),
                        retornoValidacaoRegra.erroDTO.mensagemErro()
                );
                return criarResponseIdempotentErro(autorizacaoRecorrenciaProtocolo, request, retornoValidacaoRegra.erroDTO);
            }

            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), retornoValidacaoRegra.recorrenciaAutorizacao.getIdRecorrencia());
            var erroPersistenciaCancelamento = criaRecorrenciaAutorizacaoCancelamento(autorizacaoRecorrenciaProtocolo, retornoValidacaoRegra.recorrenciaAutorizacao);
            if (erroPersistenciaCancelamento.getErro().isPresent()) {
                log.error("Erro de ao persistir o pedido de cancelamento de autorização pelo PSP do Pagador. ID_INFORMACAO_CANCELAMENTO: {}, Mensagem de erro: {}",
                        autorizacaoRecorrenciaProtocolo.getIdInformacaoCancelamento(),
                        erroPersistenciaCancelamento.getErro().get().mensagemErro()
                );
                return criarResponseIdempotentErro(autorizacaoRecorrenciaProtocolo, request, retornoValidacaoRegra.erroDTO);
            }

            var recorrenciaCancelamento = erroPersistenciaCancelamento.getObjeto();
            autorizacaoRepository.atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(
                    autorizacaoRecorrenciaProtocolo.getOidRecorrenciaAutorizacao(),
                    TipoStatusAutorizacao.APROVADA,
                    TipoSubStatus.AGUARDANDO_CANCELAMENTO.name()
            );

            var pain011 = criaPain011(
                    autorizacaoRecorrenciaProtocolo,
                    recorrenciaCancelamento,
                    retornoValidacaoRegra.recorrenciaAutorizacao);
            var eventoPain011Envio = eventoResponseFactory.criaEventoPain011(pain011, "CANCELAMENTO_PAGADOR");
            var response = criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                    .criarResponseIdempotentSucesso(
                            autorizacaoRecorrenciaProtocolo,
                            request.getTransactionId(),
                            request.getHeaders(),
                            List.of(eventoPain011Envio)
                    );
            log.debug("(Pain011) Processamento de pedido de cancelamento de autorização pelo PSP do Pagador realizado com sucesso.");
            return response;
        } finally {
            MDC.remove(RecorrenciaMdc.ID_INFORMACAO_CANCELAMENTO.getChave());
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
        }
    }

    private IdempotentResponse<?> criarResponseIdempotentErro(CancelamentoAutorizacaoRequest autorizacaoRecorrenciaProtocolo,
                                                              IdempotentRequest<CancelamentoAutorizacaoRequest> request,
                                                              ErroDTO erroDTO) {
        return criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .criarResponseIdempotentErro(
                        autorizacaoRecorrenciaProtocolo,
                        request.getTransactionId(),
                        erroDTO
                );
    }

    private RetornoValidacaoRegra validaRegras(CancelamentoAutorizacaoRequest cancelamentoAutorizacaoRecorrencia) {
        var entidade = autorizacaoRepository.findById(cancelamentoAutorizacaoRecorrencia.getOidRecorrenciaAutorizacao());

        if (entidade.isEmpty()) {
            return RetornoValidacaoRegra.criaComError(
                    new ErroDTO(
                            AppExceptionCode.AUTORIZACAO_NAO_ENCONTRADA,
                            TipoRetornoTransacaoEnum.ERRO_NEGOCIO)
            );
        }

        return entidade.filter(autorizacao -> !TipoStatusAutorizacao.APROVADA.equals(autorizacao.getTipoStatus()))
                .map(autorizacao ->  RetornoValidacaoRegra.criaComError(
                        new ErroDTO(
                                AppExceptionCode.RECORRENCIA_COM_STATUS_DIFERENTE_DE_APROVADA,
                                TipoRetornoTransacaoEnum.ERRO_NEGOCIO)
                ))
                .or(() -> validaSeSolicitante(cancelamentoAutorizacaoRecorrencia, entidade.get()))
                .orElseGet(() -> RetornoValidacaoRegra.regraValidada(entidade.get()));
    }

    private Optional<ErroDTO> validarRequest(CancelamentoAutorizacaoRequest cancelamentoAutorizacaoRecorrencia) {
        try  {
            MotivoCancelamentoPain11.of(cancelamentoAutorizacaoRecorrencia.getMotivoCancelamento());
        } catch (NoSuchElementException noSuchElementException) {
            log.debug("Motivo de cancelamento inválido: {}", noSuchElementException.getMessage());
            return Optional.of(
                    new ErroDTO(
                            AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA,
                            TipoRetornoTransacaoEnum.ERRO_VALIDACAO)
            );
        }
        return validator.validate(cancelamentoAutorizacaoRecorrencia).stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .map(mensagemErro ->
                        new ErroDTO(
                                AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA,
                                AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA.getMensagemFormatada(mensagemErro),
                                TipoRetornoTransacaoEnum.ERRO_VALIDACAO)
                );
    }

    private Optional<RetornoValidacaoRegra> validaSeSolicitante(CancelamentoAutorizacaoRequest request,
                                                                RecorrenciaAutorizacao recorrenciaAutorizacao) {
        var cpfCnpjSolicitanteCancelamento = request.getCpfCnpjSolicitanteCancelamento();

        if (recorrenciaAutorizacao.getCpfCnpjPagador().equals(cpfCnpjSolicitanteCancelamento)) {
            return Optional.empty();
        }

        if (cpfCnpjSolicitanteCancelamento.length() == TAMANHO_CPNJ && recorrenciaAutorizacao.getInstituicaoPagador().equals(cpfCnpjSolicitanteCancelamento.substring(0, 8))) {
            return Optional.empty();
        }

        var erro = new ErroDTO(
                AppExceptionCode.SOLICITANTE_DO_CANCELAMENTO_DIFERENTE,
                TipoRetornoTransacaoEnum.ERRO_NEGOCIO
        );
        return Optional.of(RetornoValidacaoRegra.criaComError(erro));
    }

    private Pain011Dto criaPain011(CancelamentoAutorizacaoRequest autorizacaoRecorrenciaProtocolo,
                                   RecorrenciaAutorizacaoCancelamento recorrenciaCancelamento,
                                   RecorrenciaAutorizacao recorrenciaAutorizacao) {
        var motivoCancelamento = MotivoCancelamentoPain11.of(autorizacaoRecorrenciaProtocolo.getMotivoCancelamento());

        var detalhes = Optional.of(motivoCancelamento)
                .filter(motivo -> MotivoCancelamentoPain11.CONFIRMADA_POR_OUTRA_JORNADA != motivoCancelamento)
                .map(motivo -> {
                    var crtn = DetalheRecorrenciaPain011Dto.builder()
                            .tipoSituacao(TipoSituacaoPain011.DATA_CRIACAO.name())
                            .dataHoraRecorrencia(recorrenciaAutorizacao.getDataCriacaoRecorrencia())
                            .build();

                    var cltn = DetalheRecorrenciaPain011Dto.builder()
                            .tipoSituacao(TipoSituacaoPain011.DATA_CANCELAMENTO.name())
                            .dataHoraRecorrencia(autorizacaoRecorrenciaProtocolo.getDataHoraInicioCanal())
                            .build();

                    return List.of(crtn, cltn);
                })
                .orElse(null);

        return Pain011Dto.builder()
                .cpfCnpjSolicitanteCancelamento(autorizacaoRecorrenciaProtocolo.getCpfCnpjSolicitanteCancelamento())
                .motivoCancelamento(motivoCancelamento.name())
                .idRecorrencia(recorrenciaCancelamento.getIdRecorrencia())
                .idInformacaoCancelamento(recorrenciaCancelamento.getIdInformacaoCancelamento())
                .tipoRecorrencia(TipoRecorrencia.RECORRENTE.name())
                .tipoFrequencia(recorrenciaAutorizacao.getTipoFrequencia())
                .dataFinalRecorrencia(recorrenciaAutorizacao.getDataFinalRecorrencia())
                .indicadorObrigatorio(Boolean.FALSE)
                .valor(recorrenciaAutorizacao.getValor())
                .dataInicialRecorrencia(recorrenciaAutorizacao.getDataInicialRecorrencia())
                .cpfCnpjUsuarioRecebedor(recorrenciaAutorizacao.getCpfCnpjRecebedor())
                .participanteDoUsuarioRecebedor(recorrenciaAutorizacao.getInstituicaoRecebedor())
                .contaUsuarioPagador(recorrenciaAutorizacao.getContaPagador())
                .nomeUsuarioRecebedor(recorrenciaAutorizacao.getNomeRecebedor())
                .agenciaUsuarioPagador(recorrenciaAutorizacao.getAgenciaPagador())
                .cpfCnpjUsuarioPagador(recorrenciaAutorizacao.getCpfCnpjPagador())
                .participanteDoUsuarioPagador(recorrenciaAutorizacao.getInstituicaoPagador())
                .cpfCnpjDevedor(recorrenciaAutorizacao.getCpfCnpjDevedor())
                .numeroContrato(recorrenciaAutorizacao.getNumeroContrato())
                .nomeDevedor(recorrenciaAutorizacao.getNomeDevedor())
                .detalhesRecorrencias(detalhes)
                .build();
    }

    private ErroWrapperDTO<RecorrenciaAutorizacaoCancelamento> criaRecorrenciaAutorizacaoCancelamento(CancelamentoAutorizacaoRequest request,
                                                                                                      RecorrenciaAutorizacao recorrenciaAutorizacao) {
        var autorizacao = RecorrenciaAutorizacaoCancelamento.builder()
                .idInformacaoCancelamento(request.getIdInformacaoCancelamento())
                .idRecorrencia(recorrenciaAutorizacao.getIdRecorrencia())
                .tipoCancelamento(TipoCancelamento.RECORRENCIA_AUTORIZACAO)
                .tipoSolicitanteCancelamento(TipoSolicitanteCancelamento.PAGADOR)
                .tipoStatus(TipoStatusCancelamentoAutorizacao.CRIADA)
                .cpfCnpjSolicitanteCancelamento(request.getCpfCnpjSolicitanteCancelamento())
                .motivoCancelamento(MotivoCancelamentoPain11.of(request.getMotivoCancelamento()).name())
                .dataCancelamento(request.getDataHoraInicioCanal())
                .build();

        try {
            var autorizacaoSalva =  recorrenciaAutorizacaoCancelamentoRepository.save(autorizacao);
            return new ErroWrapperDTO<>(autorizacaoSalva);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            var erro = new ErroDTO(
                    AppExceptionCode.ERRO_PERSISTENCIA,
                    AppExceptionCode.ERRO_PERSISTENCIA.getMensagemFormatada(e.getMessage()),
                    TipoRetornoTransacaoEnum.ERRO_INFRA
            );
            return new ErroWrapperDTO<>(erro);
        }
    }

    private record RetornoValidacaoRegra(
            ErroDTO erroDTO,
            RecorrenciaAutorizacao recorrenciaAutorizacao
    ) {
        boolean invalido() {
            return Objects.nonNull(erroDTO);
        }

        public static RetornoValidacaoRegra criaComError(ErroDTO erroDTO) {
            return new RetornoValidacaoRegra(erroDTO, null);
        }

        public static RetornoValidacaoRegra regraValidada(RecorrenciaAutorizacao recorrenciaAutorizacao) {
            return new RetornoValidacaoRegra(null, recorrenciaAutorizacao);
        }
    }
}
