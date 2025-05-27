package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoService;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.ConfirmacaoAutorizacaoRequestDTO;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoJornada;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.pain.Pain012ResponseFactory;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.CriaResponseStrategyFactory;
import io.sicredi.spirecorrencia.api.idempotente.ErroDTO;
import io.sicredi.spirecorrencia.api.idempotente.ErroWrapperDTO;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR;
import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_SEM_INTERESSE_PIX_AUTOMATICO_USUARIO_RECEBEDOR;

@Slf4j
@Service
@RequiredArgsConstructor
class ConfirmacaoSolicitacaoAutorizacaoService {

    private static final String AGUARDANDO_ENVIO = "AGUARDANDO_ENVIO";
    private static final String HEADER_OPERACAO = "RECORRENCIA_AUTORIZACAO";
    private static final String ACEITAR = "ACEITAR";
    private static final String REJEITAR = "REJEITAR";
    private static final String SIM = "S";
    private static final String NAO = "N";

    private final Validator validator;
    private final AutorizacaoService autorizacaoService;
    private final EventoResponseFactory eventoResponseFactory;
    private final CriaResponseStrategyFactory<ConfirmacaoAutorizacaoRequestDTO> criaResponseStrategyFactory;
    private final SolicitacaoAutorizacaoRecorrenciaService solicitacaoAutorizacaoService;

    @IdempotentTransaction
    public IdempotentResponse<?> processarConfirmacao(IdempotentRequest<ConfirmacaoAutorizacaoRequestDTO> requestDTO) {
        try {
            final var request = requestDTO.getValue();
            final var tipoResponse = request.getTipoResponse();
            final var identificadorTransacao = request.getIdentificadorTransacao();

            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), request.getIdRecorrencia());
            MDC.put(RecorrenciaMdc.ID_SOLICITACAO_RECORRENCIA.getChave(), request.getIdSolicitacaoRecorrencia());

            log.debug("Processando cadastro de solicitação de autorização. Tipo Jornada: JORNADA_1");

            var erroValidacaoConstraint = validarRequest(request);
            if (erroValidacaoConstraint.isPresent()) {
                log.info("Erro de validações de constraints do cadastro de solicitação autorização. Tipo Jornada: JORNADA_1, Mensagem de erro: {}", erroValidacaoConstraint.get().mensagemErro());
                return criaResponseStrategyFactory.criar(tipoResponse)
                        .criarResponseIdempotentErro(request, identificadorTransacao, erroValidacaoConstraint.get());
            }

            var solicitacaoAutorizacaoRecorrencia = consultarSolicitacao(
                    request.getIdSolicitacaoRecorrencia(),
                    TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO
            );
            var erroRegraDeNegocio = validarRegraNegocio(request, solicitacaoAutorizacaoRecorrencia);
            if (erroRegraDeNegocio.isPresent()) {
                log.info("Erro de negócio do cadastro de solicitação autorização. Tipo Jornada: JORNADA_1, Mensagem de erro: {}", erroRegraDeNegocio.get().mensagemErro());
                return criaResponseStrategyFactory.criar(tipoResponse)
                        .criarResponseIdempotentErro(request, identificadorTransacao, erroRegraDeNegocio.get());
            }

            final ResultadoProcessamento resultadoProcessamento;
            resultadoProcessamento = request.getAprovada() ? processarAprovada(request) : processarReprovada(request);
            if (resultadoProcessamento.erroDTO.isPresent()) {
                log.info("Erro no processamento do cadastro de solicitação autorização. Tipo Jornada: JORNADA_1, Mensagem de erro: {}", resultadoProcessamento.erroDTO.get().mensagemErro());
                return criaResponseStrategyFactory.criar(tipoResponse)
                        .criarResponseIdempotentErro(request, identificadorTransacao, resultadoProcessamento.erroDTO.get());
            }

            var erroPersistirRecorrenciaNoBanco = salvarAutorizacao(request, solicitacaoAutorizacaoRecorrencia.getObjeto(), resultadoProcessamento.motivoRejeicao);
            if (erroPersistirRecorrenciaNoBanco.isPresent()) {
                log.info("Erro ao salvar as informações do cadastro de solicitação autorização. Tipo Jornada: JORNADA_1, Mensagem de erro: {}", erroPersistirRecorrenciaNoBanco.get().mensagemErro());
                return criaResponseStrategyFactory.criar(tipoResponse)
                        .criarResponseIdempotentErro(request, identificadorTransacao, erroPersistirRecorrenciaNoBanco.get());
            }

            final var pain012Dto = getPain012Dto(resultadoProcessamento, request, solicitacaoAutorizacaoRecorrencia.getObjeto());

            var eventoPain12IcomEnvio = eventoResponseFactory.criarEventoPain012(pain012Dto, HEADER_OPERACAO);

            var response = criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentSucesso(request,
                    identificadorTransacao,
                    requestDTO.getHeaders(),
                    List.of(eventoPain12IcomEnvio)
            );
            log.debug("Processamento do cadastro de solicitação de autorização realizado com sucesso. Tipo Jornada: JORNADA_1");
            return response;
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
            MDC.remove(RecorrenciaMdc.ID_SOLICITACAO_RECORRENCIA.getChave());
        }
    }

    private Optional<ErroDTO> validarRequest(ConfirmacaoAutorizacaoRequestDTO request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(mensagemErro -> new ErroDTO(
                        AppExceptionCode.SPIRECORRENCIA_REC0001,
                        AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemErro),
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO));
    }

    private Optional<ErroDTO> validarRegraNegocio(ConfirmacaoAutorizacaoRequestDTO request,
                                                  ErroWrapperDTO<SolicitacaoAutorizacaoRecorrencia> solicitacaoAutorizacaoRecorrencia) {
        if (solicitacaoAutorizacaoRecorrencia.getErro().isPresent()) {
            return solicitacaoAutorizacaoRecorrencia.getErro();
        }

        final var solicitacao = solicitacaoAutorizacaoRecorrencia.getObjeto();
        final var protocoloDTO = request.getProtocoloDTO();

        var autorizacaoComValorFixo = Optional.ofNullable(solicitacao.getValor())
                .filter(valor -> valor.compareTo(BigDecimal.ZERO) > 0)
                .map(x -> Boolean.TRUE)
                .orElse(Boolean.FALSE);

        if (autorizacaoComValorFixo && request.getValorMaximo() != null && request.getValorMaximo().compareTo(BigDecimal.ZERO) > 0) {
            return Optional.of(new ErroDTO(AppExceptionCode.VALOR_MAXIMO_COM_VALOR_FIXO, TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
        }

        if (!autorizacaoComValorFixo) {
            if (request.getValorMaximo() != null && request.getValorMaximo().compareTo(solicitacao.getPisoValorMaximo()) < 0) {
                return Optional.of(new ErroDTO(AppExceptionCode.VALOR_MAXIMO_INVALIDO, TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
            }
        }

        final var isNumCpfCnpjInvalido = !request.getCpfCnpjPagador().equals(solicitacao.getCpfCnpjPagador());
        final var isAgenciaInvalida = !protocoloDTO.getCooperativa().equals(solicitacao.getAgenciaPagador());
        final var isContaPagadorInvalida = !protocoloDTO.getConta().equals(solicitacao.getContaPagador());
        if (isNumCpfCnpjInvalido || isAgenciaInvalida || isContaPagadorInvalida) {
            return Optional.of(new ErroDTO(AppExceptionCode.DADOS_PAGADOR_INVALIDO, TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
        }

        return Optional.empty();
    }

    private ResultadoProcessamento processarAprovada(ConfirmacaoAutorizacaoRequestDTO request) {
        solicitacaoAutorizacaoService.atualizaRecorrenciaAutorizacaoSolicitacao(request.getIdSolicitacaoRecorrencia(),
                request.getDataHoraInicioCanal(), null, TipoStatusSolicitacaoAutorizacao.CONFIRMADA, AGUARDANDO_ENVIO);
        return new ResultadoProcessamento(Optional.empty(), ACEITAR);
    }

    private ResultadoProcessamento processarReprovada(ConfirmacaoAutorizacaoRequestDTO request) {
        var motivoRejeicao = request.getMotivoRejeicao();

        if (motivoRejeicao == SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR
                || motivoRejeicao == SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_SEM_INTERESSE_PIX_AUTOMATICO_USUARIO_RECEBEDOR) {
            solicitacaoAutorizacaoService.atualizaRecorrenciaAutorizacaoSolicitacao(
                    request.getIdSolicitacaoRecorrencia(),
                    request.getDataHoraInicioCanal(),
                    request.getMotivoRejeicao().name(),
                    TipoStatusSolicitacaoAutorizacao.CONFIRMADA,
                    AGUARDANDO_ENVIO);
            return new ResultadoProcessamento(Optional.empty(), request.getMotivoRejeicao(), REJEITAR);
        } else {
            return new ResultadoProcessamento(
                    Optional.of(new ErroDTO(AppExceptionCode.MOTIVO_REJEICAO_INVALIDO, TipoRetornoTransacaoEnum.ERRO_NEGOCIO)),
                    request.getMotivoRejeicao()
            );
        }
    }


    private Optional<ErroDTO> salvarAutorizacao(ConfirmacaoAutorizacaoRequestDTO request, SolicitacaoAutorizacaoRecorrencia solicitacao,
                                                MotivoRejeicaoPain012 motivoRejeicao) {
        var recorrenciaAutorizacao = RecorrenciaAutorizacao.builder()
                .idRecorrencia(solicitacao.getIdRecorrencia())
                .idInformacaoStatusEnvio(request.getIdInformacaoStatus())
                .idInformacaoStatusRecebimento(null)
                .tipoJornada(TipoJornada.JORNADA_1.name())
                .tipoStatus(TipoStatusAutorizacao.CRIADA)
                .tipoSubStatus(AGUARDANDO_ENVIO)
                .tipoFrequencia(solicitacao.getTipoFrequencia())
                .permiteLinhaCredito(SIM)
                .permiteRetentativa(getPermiteRetentativa(request.getIdRecorrencia()))
                .permiteNotificacaoAgendamento(SIM)
                .cpfCnpjPagador(solicitacao.getCpfCnpjPagador())
                .nomePagador(solicitacao.getNomePagador())
                .agenciaPagador(solicitacao.getAgenciaPagador())
                .valor(solicitacao.getValor())
                .valorMaximo(request.getValorMaximo())
                .pisoValorMaximo(solicitacao.getPisoValorMaximo())
                .tipoContaPagador(solicitacao.getTipoContaPagador())
                .tipoPessoaPagador(solicitacao.getTipoPessoaPagador())
                .contaPagador(solicitacao.getContaPagador())
                .instituicaoPagador(solicitacao.getInstituicaoPagador())
                .postoPagador(solicitacao.getPostoPagador())
                .tipoSistemaPagador(solicitacao.getTipoSistemaPagador())
                .tipoCanalPagador(request.getTipoCanalPagador())
                .nomeRecebedor(solicitacao.getNomeRecebedor())
                .instituicaoRecebedor(solicitacao.getInstituicaoRecebedor())
                .cpfCnpjRecebedor(solicitacao.getCpfCnpjRecebedor())
                .nomeDevedor(solicitacao.getNomeDevedor())
                .cpfCnpjDevedor(solicitacao.getCpfCnpjDevedor())
                .descricao(solicitacao.getDescricao())
                .numeroContrato(solicitacao.getNumeroContrato())
                .codigoMunicipioIBGE(solicitacao.getCodigoMunicipioIBGE())
                .motivoRejeicao(motivoRejeicao != null ? motivoRejeicao.name() : null)
                .dataInicialRecorrencia(solicitacao.getDataInicialRecorrencia())
                .dataInicioConfirmacao(request.getDataHoraInicioCanal())
                .dataFinalRecorrencia(solicitacao.getDataFinalRecorrencia())
                .dataCriacaoRecorrencia(solicitacao.getDataCriacaoRecorrencia())
                .build();
        try {
            autorizacaoService.salvar(recorrenciaAutorizacao);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            var erro = new ErroDTO(
                    AppExceptionCode.ERRO_PERSISTENCIA,
                    AppExceptionCode.ERRO_PERSISTENCIA.getMensagemFormatada(e.getMessage()),
                    TipoRetornoTransacaoEnum.ERRO_INFRA
            );
            return Optional.of(erro);
        }
        return Optional.empty();
    }

    /**
     * Verifica se a recorrência permite novas tentativas de pagamento após o vencimento.
     *
     * <p>O identificador de recorrência segue o formato "RRxxxxxxxxyyyyMMddkkkkkkkkkkk",
     * onde o segundo caractere indica se a recorrência permite retentativas ('R' para sim, qualquer outro caractere para não).
     *
     * @param idRecorrencia O identificador da recorrência.
     * @return "S" se a recorrência permite retentativas, "N" caso contrário.
     */
    private static String getPermiteRetentativa(String idRecorrencia) {
        return idRecorrencia.charAt(1) == 'R' ? SIM : NAO;
    }

    private static Pain012Dto getPain012Dto(ResultadoProcessamento resultadoProcessamento, ConfirmacaoAutorizacaoRequestDTO request,
                                            SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacaoRecorrencia) {
        return ACEITAR.equals(resultadoProcessamento.acao) ?
                Pain012ResponseFactory.fromConfirmacaoAceita(request, solicitacaoAutorizacaoRecorrencia)
                :
                Pain012ResponseFactory.fromConfirmacaoRejeitada(request, resultadoProcessamento.motivoRejeicao, solicitacaoAutorizacaoRecorrencia);
    }

    private record ResultadoProcessamento(
            Optional<ErroDTO> erroDTO,
            MotivoRejeicaoPain012 motivoRejeicao,
            String acao
    ) {
        public ResultadoProcessamento(Optional<ErroDTO> erroDTO, String acao) {
            this(erroDTO, null, acao);
        }

        public ResultadoProcessamento(Optional<ErroDTO> erroDTO, MotivoRejeicaoPain012 motivoRejeicao) {
            this(erroDTO, motivoRejeicao, null);
        }
    }

    private ErroWrapperDTO<SolicitacaoAutorizacaoRecorrencia> consultarSolicitacao(String idSolicitacaoRecorrencia,
                                                                                   TipoStatusSolicitacaoAutorizacao status) {
        try {
            var solicitacaoRecorrencia = solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(
                    idSolicitacaoRecorrencia, status
            );
            return new ErroWrapperDTO<>(solicitacaoRecorrencia);
        } catch (NotFoundException ex) {
            var appExceptionCode = AppExceptionCode.SOLICITACAO_NAO_ENCONTRADA;
            var erro = new ErroDTO(appExceptionCode, TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
            return new ErroWrapperDTO<>(erro);
        }
    }
}