package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.PoliticaRetentativaRecorrenciaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoJornada;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSubStatus;
import io.sicredi.spirecorrencia.api.automatico.pain.Pain012ResponseFactory;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.CriaResponseStrategyFactory;
import io.sicredi.spirecorrencia.api.idempotente.ErroDTO;
import io.sicredi.spirecorrencia.api.idempotente.EventoResponseFactory;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CadastroAutorizacaoService {

    private static final String HEADER_OPERACAO = "RECORRENCIA_AUTORIZACAO";

    private final AppConfig appConfig;
    private final Validator validator;
    private final AutorizacaoService autorizacaoService;
    private final CriaResponseStrategyFactory<CadastroAutorizacaoRequest> criaResponseStrategyFactory;
    private final EventoResponseFactory eventoResponseFactory;
    private final RecorrenciaAutorizacaoPagamentoImediatoRepository autorizacaoPagamentoImediatoRepository;

    @IdempotentTransaction
    public IdempotentResponse<?> processarCadastroAutorizacao(IdempotentRequest<CadastroAutorizacaoRequest> request) {
        try {
            var cadastroRequest = request.getValue();
            var tipoResponse = cadastroRequest.getTipoResponse();
            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), cadastroRequest.getIdRecorrencia());

            log.debug("Processando cadastro de autorização. Tipo Jornada: {}", cadastroRequest.getTipoJornada());

            var erroValidacaoConstraint = validarRequest(cadastroRequest);
            if (erroValidacaoConstraint.isPresent()) {
                log.info("Erro de validações de constraints da cadastro de autorização. Tipo Jornada: {}, Mensagem de erro: {}", cadastroRequest.getTipoJornada(), erroValidacaoConstraint.get().mensagemErro());
                return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(cadastroRequest,
                        request.getTransactionId(),
                        erroValidacaoConstraint.get()
                );
            }

            log.debug("Início das validações de negócio do cadastro de autorização.");
            var erroValidacaoNegocio = validarRegraNegocio(cadastroRequest);
            log.debug("Fim das validações de negócio da cadastro de autorização. Erro validação de negócio: {}", erroValidacaoNegocio.isPresent());
            if (erroValidacaoNegocio.isPresent()) {
                log.info("Erro de validações de negócio no cadastro de autorização. Tipo Jornada: {}, Mensagem de erro: {}", cadastroRequest.getTipoJornada(), erroValidacaoNegocio.get().mensagemErro());
                return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(cadastroRequest,
                        request.getTransactionId(),
                        erroValidacaoNegocio.get()
                );
            }

            log.debug("Início da persistência do cadastro de autorização.");
            var erroDTO = processarCadastro(cadastroRequest);
            log.debug("Fim da persistência do cadastro de autorização. Erro ao salvar: {}", erroDTO.isPresent());
            if (erroDTO.isPresent()) {
                log.info("Erro durante o processamento do cadastro de autorização. Tipo Jornada: {}, Mensagem de erro: {}", cadastroRequest.getTipoJornada(), erroDTO.get().mensagemErro());
                return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(cadastroRequest,
                        request.getTransactionId(),
                        erroDTO.get()
                );
            }

            var pain012 = Pain012ResponseFactory.fromCadastroAutorizacaoRequest(cadastroRequest);
            var eventoPain12IcomEnvio = eventoResponseFactory.criarEventoPain012(pain012, HEADER_OPERACAO);

            var response = criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentSucesso(cadastroRequest,
                    request.getTransactionId(),
                    request.getHeaders(),
                    List.of(eventoPain12IcomEnvio)
            );
            log.debug("Processamento do cadastro de autorização realizado com sucesso. Tipo Jornada: {}", cadastroRequest.getTipoJornada());
            return response;
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
        }

    }

    private Optional<ErroDTO> validarRequest(CadastroAutorizacaoRequest autorizacaoRequest) {
        return validator.validate(autorizacaoRequest).stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(mensagemErro -> new ErroDTO(
                        AppExceptionCode.SPIRECORRENCIA_REC0001,
                        AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemErro),
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO));
    }

    private Optional<ErroDTO> validarRegraNegocio(CadastroAutorizacaoRequest cadastroAutorizacaoRequest) {
        return validarExistenciaAutorizacaoAprovada(cadastroAutorizacaoRequest)
                .or(() -> validarPermissaoRetentativa(cadastroAutorizacaoRequest));
    }

    private Optional<ErroDTO> validarPermissaoRetentativa(CadastroAutorizacaoRequest request) {
        String idRecorrencia = request.getIdRecorrencia();
        char tipoRecorrencia = idRecorrencia.charAt(1);

        boolean recorrenciaPermiteRetentativa = (tipoRecorrencia == 'R');
        boolean retentativaFoiPermitida = request.getPoliticaRetentativa() != PoliticaRetentativaRecorrenciaEnum.NAO_PERMITE;

        if (recorrenciaPermiteRetentativa != retentativaFoiPermitida) {
            return Optional.of(new ErroDTO(
                    AppExceptionCode.PERMISSAO_RETENTATIVA_INVALIDA,
                    AppExceptionCode.PERMISSAO_RETENTATIVA_INVALIDA.getMessage(),
                    TipoRetornoTransacaoEnum.ERRO_NEGOCIO
            ));
        }

        return Optional.empty();
    }

    private Optional<ErroDTO> validarExistenciaAutorizacaoAprovada(CadastroAutorizacaoRequest cadastroAutorizacaoRequest) {
        return autorizacaoService
                .consultarAutorizacaoPorIdEStatus(cadastroAutorizacaoRequest.getIdRecorrencia(), TipoStatusAutorizacao.APROVADA)
                .map(autorizacao -> new ErroDTO(
                        AppExceptionCode.AUTORIZACAO_JA_APROVADA_ANTERIORMENTE,
                        AppExceptionCode.AUTORIZACAO_JA_APROVADA_ANTERIORMENTE.getMessage(),
                        TipoRetornoTransacaoEnum.ERRO_NEGOCIO
                ));
    }

    private Optional<ErroDTO> processarCadastro(CadastroAutorizacaoRequest request) {
        try {
            var permiteRetentativa = request.getPoliticaRetentativa() == PoliticaRetentativaRecorrenciaEnum.NAO_PERMITE ? "N" : "S";

            RecorrenciaAutorizacao autorizacao = RecorrenciaAutorizacao.builder()
                    .idRecorrencia(request.getIdRecorrencia())
                    .idInformacaoStatusEnvio(request.getIdInformacaoStatus())
                    .tipoJornada(request.getTipoJornada().name())
                    .tipoStatus(TipoStatusAutorizacao.CRIADA)
                    .tipoSubStatus(TipoSubStatus.AGUARDANDO_ENVIO.name())
                    .tipoFrequencia(request.getTipoFrequencia().name())
                    .permiteLinhaCredito("S")
                    .permiteRetentativa(permiteRetentativa)
                    .permiteNotificacaoAgendamento("S")
                    .cpfCnpjPagador(request.getCpfCnpjPagador())
                    .nomePagador(request.getNomePagador())
                    .agenciaPagador(request.getAgenciaPagador())
                    .valor(request.getValor())
                    .valorMaximo(request.getValorMaximo())
                    .pisoValorMaximo(request.getPisoValorMaximo())
                    .tipoPessoaPagador(request.getTipoPessoaPagador())
                    .contaPagador(request.getContaPagador())
                    .postoPagador(request.getPostoPagador())
                    .tipoCanalPagador(request.getTipoCanal())
                    .nomeRecebedor(request.getNomeRecebedor())
                    .instituicaoRecebedor(request.getInstituicaoRecebedor())
                    .cpfCnpjRecebedor(request.getCpfCnpjRecebedor())
                    .nomeDevedor(request.getNomeDevedor())
                    .cpfCnpjDevedor(request.getCpfCnpjDevedor())
                    .descricao(request.getObjeto())
                    .numeroContrato(request.getContrato())
                    .codigoMunicipioIBGE(request.getCodigoMunicipioIbge())
                    .dataInicialRecorrencia(request.getDataInicialRecorrencia())
                    .dataFinalRecorrencia(request.getDataFinalRecorrencia())
                    .dataCriacaoRecorrencia(request.getDataCriacaoRecorrencia())
                    .tipoSistemaPagador(request.getTipoOrigemSistema())
                    .dataInicioConfirmacao(request.getDataHoraInicioCanal())
                    .instituicaoPagador(request.getInstituicaoPagador())
                    .tipoContaPagador(request.getTipoContaPagador().name())
                    .build();

            autorizacaoService.salvar(autorizacao);

            if (TipoJornada.JORNADA_3.equals(request.getTipoJornada())) {
                var autorizacaoPagamentoImediato = RecorrenciaAutorizacaoPagamentoImediato.builder()
                        .idFimAFim(request.getIdFimAFimPagamentoImediato())
                        .dataRecebimentoConfirmacao(request.getDataRecebimentoConfirmacaoPacs002PagamentoImediato())
                        .idRecorrencia(request.getIdRecorrencia())
                        .build();

                autorizacaoPagamentoImediatoRepository.save(autorizacaoPagamentoImediato);

                log.info("TipoJornada 'JORNADA_3' identificado. Registro de pagamento imediato criado: idRecorrencia={}, idFimAFim={}, dataRecebimentoConfirmacao={}",
                        request.getIdRecorrencia(),
                        request.getIdFimAFimPagamentoImediato(),
                        request.getDataRecebimentoConfirmacaoPacs002PagamentoImediato()
                );
            }

            return Optional.empty();

        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            return Optional.of(
                    new ErroDTO(
                            AppExceptionCode.SPIRECORRENCIA_REC0004,
                            AppExceptionCode.SPIRECORRENCIA_REC0004.getMensagemFormatada(e.getMessage()),
                            TipoRetornoTransacaoEnum.ERRO_INFRA
                    )
            );
        }
    }

}
