package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.DetalheRecorrenciaPain011Dto;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spi.entities.type.TipoSituacaoPain011;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamentoRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.*;
import io.sicredi.spirecorrencia.api.automatico.pain.Pain012ResponseFactory;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.protocolo.CanaisDigitaisProtocoloInfoInternalApiClient;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11.CONFIRMADA_POR_OUTRA_JORNADA;
import static br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11.ERROR_SOLICITACAO_DE_CONFIRMACAO;
import static br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11.FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR;
import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CANCELAMENTO_DE_AUTORIZACAO;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoCancelamento.RECORRENCIA_AUTORIZACAO;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoCancelamento.RECORRENCIA_SOLICITACAO;

@Slf4j
@Service
@RequiredArgsConstructor
class AutorizacaoCancelamentoServiceImpl implements AutorizacaoCancelamentoService {

    private static final String HEADER_OPERACAO = "CANCELAMENTO_RECEBEDOR";

    private final SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;
    private final RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;
    private final RecorrenciaAutorizacaoRepository autorizacaoRepository;
    private final EventoResponseFactory eventoResponseFactory;
    private final CriaResponseStrategyFactory<OperacaoRequest> operacaoRequestCriaResponseStrategyFactory;
    private final CriaResponseStrategyFactory<IdempotenteRequest> idempotenteRequestCriaResponseStrategyFactory;
    private final CanaisDigitaisProtocoloInfoInternalApiClient canaisDigitaisProtocoloInfoInternalApiClient;

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarPedidoCancelamentoRecebedor(IdempotentRequest<Pain011Dto> request) {
        try {
            var pain011 = request.getValue();
            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), pain011.getIdRecorrencia());
            MDC.put(RecorrenciaMdc.ID_INFORMACAO_CANCELAMENTO.getChave(), pain011.getIdInformacaoCancelamento());
            log.debug("(Pain011) Processamento de pedido de cancelamento.");

            var motivoCancelamento = MotivoCancelamentoPain11.of(pain011.getMotivoCancelamento());

            if (motivoCancelamento == ERROR_SOLICITACAO_DE_CONFIRMACAO ||
                    motivoCancelamento == FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR ||
                    motivoCancelamento == CONFIRMADA_POR_OUTRA_JORNADA) {
                log.debug("(Pain011) Processando pedido de cancelamento de solicitação de autorização. Motivo do cancelamento: {}", motivoCancelamento);
                return processaCancelamentoSolicitacaoDeAutorizacao(request);
            }
            log.debug("(Pain011) Processando pedido de cancelamento de autorização. Motivo do cancelamento: {}", motivoCancelamento);
            return processaCancelamentoDeAutorizacao(request);
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
            MDC.remove(RecorrenciaMdc.ID_INFORMACAO_CANCELAMENTO.getChave());
        }
    }

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarPedidoCancelamentoPagador(IdempotentAsyncRequest<Pain011Dto> request) {
        try {
            log.debug("(Pain011) Iniciando processamento de pedido de cancelamento pagador");
            var pain011 = request.getValue();
            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), pain011.getIdRecorrencia());
            MDC.put(RecorrenciaMdc.ID_INFORMACAO_CANCELAMENTO.getChave(), pain011.getIdInformacaoCancelamento());

            var idRecorrencia = pain011.getIdRecorrencia();
            var idInformacaoCancelamento = pain011.getIdInformacaoCancelamento();

            log.debug("(Pain011) Iniciando consulta do protocolo ID_RECORRENCIA: {} e codigo tipo transação '441'", idRecorrencia);
            var idempotentResponse = Optional.ofNullable(
                            canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(
                                    CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CANCELAMENTO_DE_AUTORIZACAO,
                                    idInformacaoCancelamento)
                    )
                    .map(proc -> idempotenteRequestCriaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                            .criarResponseIdempotentSucesso(
                                    proc,
                                    idInformacaoCancelamento,
                                    request.getHeaders(),
                                    Collections.emptyList())
                    )
                    .orElseGet(() -> {
                        log.error("Pedido de cancelamento pagador não encontrado para o ID_RECORRENCIA: {}", idRecorrencia);
                        return null;
                    });

            if (Objects.isNull(idempotentResponse)) {
                log.error("(Pain011) Pedido de cancelamento pagador processado, ocorreu um error na recuperação do protocolo ID_RECORRENCIA: {}", idRecorrencia);
                return null;
            }

            log.debug("(Pain011) Atualizando status da Recorrencia de Autorizacao para Cancelamento: ID_RECORRENCIA: {}", idRecorrencia);
            recorrenciaAutorizacaoCancelamentoRepository
                    .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(
                            idInformacaoCancelamento,
                            idRecorrencia,
                            TipoStatusCancelamentoAutorizacao.ENVIADA
                    );
            log.debug("(Pain011) Pedido de cancelamento pagador processado com sucesso: ID_RECORRENCIA: {}", idRecorrencia);
            return idempotentResponse;
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
            MDC.remove(RecorrenciaMdc.ID_INFORMACAO_CANCELAMENTO.getChave());
        }
    }

    private IdempotentResponse<?> processaCancelamentoSolicitacaoDeAutorizacao(IdempotentRequest<Pain011Dto> request) {
        var pain011 = request.getValue();
        var statusRecorrenciaAutorizacaoSolicitacao = List.of(
                TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO,
                TipoStatusSolicitacaoAutorizacao.CRIADA
        );

        var entidade = solicitacaoRepository
                .findFirstByIdRecorrenciaAndTipoStatusIn(pain011.getIdRecorrencia(), statusRecorrenciaAutorizacaoSolicitacao);

        if (entidade.isEmpty())
            return processaResultadoComRejeicao(
                    request,
                    SOLICITACAO_CONFIRMACAO_NAO_IDENTIFICADA,
                    RECORRENCIA_SOLICITACAO,
                    null);

        return validaRegraDeNegocioEretornaResultado(
                request,
                entidade.get().getCpfCnpjPagador(),
                entidade.get().getCpfCnpjRecebedor(),
                RECORRENCIA_SOLICITACAO,
                entidade.get().getCodigoMunicipioIBGE());
    }

    private IdempotentResponse<?> processaCancelamentoDeAutorizacao(IdempotentRequest<Pain011Dto> request) {
        log.debug("Iniciando processamento de pedido de cancelamento de Recorrencia Autorizacao");
        var pain011 = request.getValue();
        var statusRecorrenciaAutorizacao = List.of(TipoStatusAutorizacao.APROVADA);

        var entidade = autorizacaoRepository
                .findByIdRecorrenciaAndTipoStatusIn(pain011.getIdRecorrencia(), statusRecorrenciaAutorizacao);

        if (entidade.isEmpty()) {
            log.debug("Recorrencia de Autorizacao não encontrada para o pedido de cancelamento: ID_RECORRENCIA: {}", pain011.getIdRecorrencia());
            return processaResultadoComRejeicao(
                    request,
                    RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_INEXISTENTE,
                    RECORRENCIA_AUTORIZACAO,
                    null);
        }

        log.debug("Iniciando aplicando regra de negocio: ID_RECORRENCIA: {}", pain011.getIdRecorrencia());
        var autorizacao = entidade.get();
        autorizacaoRepository.atualizaSubStatusPorIdRecorrencia(
                pain011.getIdRecorrencia(), TipoSubStatus.AGUARDANDO_CANCELAMENTO.name());
        return validaRegraDeNegocioEretornaResultado(
                request,
                autorizacao.getCpfCnpjPagador(),
                autorizacao.getCpfCnpjRecebedor(),
                RECORRENCIA_AUTORIZACAO,
                autorizacao.getCodigoMunicipioIBGE());
    }

    private IdempotentResponse<?> validaRegraDeNegocioEretornaResultado(IdempotentRequest<Pain011Dto> request,
                                                                        String cpfPagador,
                                                                        String cpfRecebedor,
                                                                        TipoCancelamento tipoCancelamento,
                                                                        String codigoMunicipioIBGE) {
        var pain011 = request.getValue();
        var contemRejeicao = validaDocumentoPagador(pain011, cpfPagador)
                .or(() -> validaDocumentoRecebedor(pain011, cpfRecebedor))
                .or(() -> validaDocumentoSolicitante(
                        pain011,
                        cpfPagador,
                        cpfRecebedor
                ));

        if (contemRejeicao.isPresent()) {
            var motivoRejeicao = MotivoRejeicaoPain012.of(contemRejeicao.get());
            return processaResultadoComRejeicao(request, motivoRejeicao, tipoCancelamento, codigoMunicipioIBGE);
        }

        return processaResultadoComSucesso(request,tipoCancelamento, codigoMunicipioIBGE);
    }

    private IdempotentResponse<?> processaResultadoComSucesso(IdempotentRequest<Pain011Dto> request,
                                                              TipoCancelamento tipoCancelamento,
                                                              String codigoIbge) {
        var recorrenciaAutorizacaoCancelamento =
                criarRecorrenciaAutorizacaoCancelamento(request, null, tipoCancelamento);
        var entidadeAtualizada = recorrenciaAutorizacaoCancelamentoRepository.save(recorrenciaAutorizacaoCancelamento);

        var pain011 = request.getValue();
        var pain012 = Pain012ResponseFactory.fromPain011ComSucesso(
                pain011,
                recorrenciaAutorizacaoCancelamento.getIdInformacaoStatus(),
                codigoIbge,
                entidadeAtualizada.getDataAlteracaoRegistro()
        );

        log.debug("Processado com sucesso. Tipo Cancelamento:{} ", tipoCancelamento);
        return getIdempotentResponse(request, pain012);
    }

    private IdempotentResponse<?> processaResultadoComRejeicao(IdempotentRequest<Pain011Dto> request,
                                                               MotivoRejeicaoPain012 motivoRejeicaoPain012,
                                                               TipoCancelamento tipoCancelamento,
                                                               String codigoIbge) {
        try {
            var pain011 = request.getValue();
            var recorrenciaAutorizacaoCancelamento =
                    criarRecorrenciaAutorizacaoCancelamento(request, motivoRejeicaoPain012.name(), tipoCancelamento);
            recorrenciaAutorizacaoCancelamentoRepository.save(recorrenciaAutorizacaoCancelamento);

            var pain012 = Pain012ResponseFactory.fromPain011Error(
                    pain011,
                    motivoRejeicaoPain012,
                    recorrenciaAutorizacaoCancelamento.getIdInformacaoStatus(),
                    codigoIbge);

            log.debug("Processado com rejeição. Tipo Cancelamento:{}, Motivo rejeição: {}, ", tipoCancelamento, motivoRejeicaoPain012);

            return getIdempotentResponse(request, pain012);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw new TechnicalException(AppExceptionCode.ERRO_PERSISTENCIA.getMensagemFormatada(e.getMessage()), e, AppExceptionCode.ERRO_PERSISTENCIA);
        }
    }

    private IdempotentResponse<?> getIdempotentResponse(IdempotentRequest<Pain011Dto> request, Pain012Dto pain012) {
        var evento = eventoResponseFactory.criarEventoPain012(pain012, HEADER_OPERACAO);
        var operacaoRequest = OperacaoRequest.builder()
                .motivoRejeicao(pain012.getMotivoRejeicao())
                .operacao(TipoResponseIdempotente.OPERACAO.name())
                .build();
        return criarResponseIdempotentePedidoAutorizacao(request, evento, operacaoRequest);
    }

    private RecorrenciaAutorizacaoCancelamento criarRecorrenciaAutorizacaoCancelamento(IdempotentRequest<Pain011Dto> request,
                                                                                       String motivoRejeicao,
                                                                                       TipoCancelamento tipoCancelamento) {

        var dataCancelamento = request.getValue()
                .getDetalhesRecorrencias()
                .stream()
                .filter(detalheRecorrencia -> {
                    var situacao = TipoSituacaoPain011.of(detalheRecorrencia.getTipoSituacao());
                    return TipoSituacaoPain011.DATA_CANCELAMENTO == situacao;
                })
                .map(DetalheRecorrenciaPain011Dto::getDataHoraRecorrencia)
                .findFirst()
                .orElseGet(LocalDateTime::now);
        return RecorrenciaAutorizacaoCancelamento.builder()
                .idInformacaoCancelamento(request.getValue().getIdInformacaoCancelamento())
                .idRecorrencia(request.getValue().getIdRecorrencia())
                .idInformacaoStatus(getIdInformacaoStatus())
                .tipoCancelamento(tipoCancelamento)
                .tipoSolicitanteCancelamento(TipoSolicitanteCancelamento.RECEBEDOR)
                .tipoStatus(TipoStatusCancelamentoAutorizacao.CRIADA)
                .cpfCnpjSolicitanteCancelamento(request.getValue().getCpfCnpjSolicitanteCancelamento())
                .motivoCancelamento(request.getValue().getMotivoCancelamento())
                .motivoRejeicao(motivoRejeicao)
                .dataCancelamento(dataCancelamento)
                .build();
    }

    private String getIdInformacaoStatus() {
        return SpiUtil.gerarIdFimAFim(TipoId.INFORMACAO_STATUS);
    }

    private IdempotentResponse<?> criarResponseIdempotentePedidoAutorizacao(IdempotentRequest<Pain011Dto> request,
                                                                            EventoResponseDTO evento,
                                                                            OperacaoRequest operacaoRequest) {
        var listaIdempotentEvent = List.of(evento);

        return operacaoRequestCriaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)
                .criarResponseIdempotentSucesso(
                        operacaoRequest,
                        request.getTransactionId(),
                        request.getHeaders(),
                        listaIdempotentEvent
                );
    }

    private Optional<String> validaDocumentoPagador(Pain011Dto pain011, String cpfCnpjPagador) {
        var cpfCnpjUsuarioPagador = pain011.getCpfCnpjUsuarioPagador();
        return cpfCnpjPagador.equals(cpfCnpjUsuarioPagador)
                ? Optional.empty()
                : Optional.of(CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO.name());
    }

    private Optional<String> validaDocumentoRecebedor(Pain011Dto pain011,
                                                                     String cpfCnpjRecebedor) {
        var cpfCnpjUsuarioRecebedor = pain011.getCpfCnpjUsuarioRecebedor();
        return cpfCnpjRecebedor.equals(cpfCnpjUsuarioRecebedor)
                ? Optional.empty()
                : Optional.of(CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE.name());
    }

    private Optional<String> validaDocumentoSolicitante(Pain011Dto pain011,
                                                                       String cpfCnpjPagador,
                                                                       String cpfCnpjRecebedor) {
        var cpfCnpjSolicitante = pain011.getCpfCnpjSolicitanteCancelamento();

        return cpfCnpjSolicitante.equals(cpfCnpjPagador) || cpfCnpjSolicitante.equals(cpfCnpjRecebedor)
                ? Optional.empty()
                : Optional.of(CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA.name());
    }
}
