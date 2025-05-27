package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.Camt029Dto;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoCamt029;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.automatico.camt.Camt029ResponseFactory;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoMensagem;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoService;
import io.sicredi.spirecorrencia.api.idempotente.CriaResponseStrategyFactory;
import io.sicredi.spirecorrencia.api.idempotente.EventoResponseFactory;
import io.sicredi.spirecorrencia.api.idempotente.OperacaoRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.sicredi.spirecorrencia.api.utils.IdentificadorTransacaoUtils;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoStatus.ATIVA;
import static io.sicredi.spirecorrencia.api.idempotente.OperacaoRequest.criarOperacaoRequest;

@Slf4j
@Service
@RequiredArgsConstructor
class RecorrenciaInstrucaoPagamentoCancelamentoServiceImpl implements RecorrenciaInstrucaoPagamentoCancelamentoService {

    private static final String CRIADA = "CRIADA";
    private static final String RECEBEDOR = "RECEBEDOR";
    private static final String AGUARDANDO_CANCELAMENTO = "AGUARDANDO_CANCELAMENTO";

    private final EventoResponseFactory eventoResponseFactory;
    private final RecorrenciaInstrucaoPagamentoService instrucaoPagamentoService;
    private final RecorrenciaInstrucaoPagamentoCancelamentoRepository repository;
    private final CriaResponseStrategyFactory<OperacaoRequest> criaResponseStrategyFactory;

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarSolicitacaoCancelamento(IdempotentRequest<Camt055Dto> request) {
        try {
            var camt055 = request.getValue();
            MDC.put(RecorrenciaMdc.ID_FIM_A_FIM.getChave(), camt055.getIdFimAFimOriginal());

            log.debug("(Camt055) Processamento pedido de cancelamento do PSP do Recebedor.");

            var instrucaoPagamento = instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(camt055.getIdFimAFimOriginal())
                    .orElse(null);
            var erroRegraDeNegocio = validarRegraDeNegocio(instrucaoPagamento, camt055);

            var camt029Dto = erroRegraDeNegocio.isEmpty() ? processarCamt055Aceita(camt055) : processarCamt055Rejeitada(camt055, erroRegraDeNegocio.get());

            var evento = eventoResponseFactory.criarEventoCamt029IcomEnvio(camt029Dto);
            return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                    criarOperacaoRequest(TipoMensagem.CAMT055, camt055.getMotivoCancelamento()),
                    request.getTransactionId(),
                    request.getHeaders(),
                    List.of(evento));
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
        }
    }

    private Optional<MotivoRejeicaoCamt029> validarRegraDeNegocio(RecorrenciaInstrucaoPagamento instrucaoDePagamento, Camt055Dto camt055Dto) {
        return validarExistenciaInstrucaoDePagamento(instrucaoDePagamento)
                .or(() -> validarStatusInstrucaoPagamento(instrucaoDePagamento))
                .or(() -> validarPrazoSolicitacao(instrucaoDePagamento, camt055Dto.getDataHoraCriacaoParaEmissao()))
                .or(() -> validarIdConciliacaoRecebedor(instrucaoDePagamento, camt055Dto))
                .or(() -> validarCpfCnpjUsuarioSolicitanteCancelamento(instrucaoDePagamento, camt055Dto.getCpfCnpjUsuarioSolicitanteCancelamento()));
    }

    private Optional<MotivoRejeicaoCamt029> validarExistenciaInstrucaoDePagamento(RecorrenciaInstrucaoPagamento instrucaoDePagamento) {
        if (Objects.nonNull(instrucaoDePagamento)) {
            return Optional.empty();
        }
        return Optional.of(MotivoRejeicaoCamt029.ID_FIM_A_FIM_NAO_CORRESPONDE_AO_ORIGINAL_INFORMADO);
    }

    private Optional<MotivoRejeicaoCamt029> validarStatusInstrucaoPagamento(RecorrenciaInstrucaoPagamento instrucaoDePagamento) {
        if (!ATIVA.name().equals(instrucaoDePagamento.getTpoStatus())) {
            return Optional.empty();
        }
        return Optional.of(MotivoRejeicaoCamt029.PAGAMENTO_JA_CONCLUIDO_COM_SUCESSO);
    }

    private Optional<MotivoRejeicaoCamt029> validarPrazoSolicitacao(RecorrenciaInstrucaoPagamento instrucaoPagamento, LocalDateTime dataHoraCriacaoParaEmissao) {
        if (dataAtualAnteriorAs22hDoDiaAnterior(instrucaoPagamento.getCodFimAFim(), dataHoraCriacaoParaEmissao)) {
            return Optional.empty();
        }
        return Optional.of(MotivoRejeicaoCamt029.SOLICITACAO_CANCELAMENTO_NAO_RECEBIDA_NO_PRAZO);
    }

    private boolean dataAtualAnteriorAs22hDoDiaAnterior(String codFimAFim, LocalDateTime dataHoraCriacaoParaEmissao) {
        var dataHoraPrevistaLiquidacao = IdentificadorTransacaoUtils.extrairData(codFimAFim);
        var limite = dataHoraPrevistaLiquidacao.minusDays(1).with(LocalTime.of(22, 0));
        return dataHoraCriacaoParaEmissao.isBefore(limite);
    }

    private Optional<MotivoRejeicaoCamt029> validarCpfCnpjUsuarioSolicitanteCancelamento(RecorrenciaInstrucaoPagamento instrucaoPagamento,
                                                                                         String cpfCnpjUsuarioSolicitanteCancelamento) {
        var autorizacao = Optional.ofNullable(instrucaoPagamento.getAutorizacao())
                .orElseThrow(() -> new NotFoundException("Não foi possível encontrar a autorização de recorrência"));

        if (autorizacao.getCpfCnpjRecebedor().equals(instrucaoPagamento.getNumCpfCnpjRecebedor())) {
            return Optional.empty();
        }

        if (cpfCnpjUsuarioSolicitanteCancelamento.length() == 14 && autorizacao.getInstituicaoRecebedor().equals(cpfCnpjUsuarioSolicitanteCancelamento.substring(0, 8))) {
            return Optional.empty();
        }
        return Optional.of(MotivoRejeicaoCamt029.CPF_CNPJ_USUARIO_RECEBEDOR_NAO_CORRESPONDENTE_RECORRENCIA_OU_AUTORIZACAO);
    }

    private Optional<MotivoRejeicaoCamt029> validarIdConciliacaoRecebedor(RecorrenciaInstrucaoPagamento instrucaoDePagamento, Camt055Dto camt055Dto) {
        if (instrucaoDePagamento.getIdConciliacaoRecebedor().equals(camt055Dto.getIdConciliacaoRecebedorOriginal())) {
            return Optional.empty();
        }
        return Optional.of(MotivoRejeicaoCamt029.ID_CONCILICAO_RECEBEDOR_NAO_CORRESPONDE_AO_ORIGINALMENTE_INFORMADO);
    }

    private Camt029Dto processarCamt055Rejeitada(Camt055Dto camt055, MotivoRejeicaoCamt029 motivoRejeicao) {
        log.info("(Camt055) Processando resposta NÃO ACEITA pelo PSP do Pagador. Motivo de rejeição: {}", motivoRejeicao.name());
        salvarInstrucaoPagamentoCancelamento(camt055, motivoRejeicao);
        return Camt029ResponseFactory.fromCamt055Rejeitada(camt055, motivoRejeicao);
    }

    private Camt029Dto processarCamt055Aceita(Camt055Dto camt055) {
        log.debug("(Camt055) Processando resposta ACEITA pelo PSP do Pagador.");
        salvarInstrucaoPagamentoCancelamento(camt055, null);
        instrucaoPagamentoService.atualizaTpoStatusETpoSubStatus(camt055.getIdFimAFimOriginal(), ATIVA.name(), AGUARDANDO_CANCELAMENTO);
        return Camt029ResponseFactory.fromCamt055Aceita(camt055);
    }

    private void salvarInstrucaoPagamentoCancelamento(Camt055Dto camt055Dto, MotivoRejeicaoCamt029 motivoRejeicao) {
        var recorrenciaInstrucaoPagamentoCancelamento = RecorrenciaInstrucaoPagamentoCancelamento.builder()
                .idCancelamentoAgendamento(camt055Dto.getIdCancelamentoAgendamento())
                .codFimAFim(camt055Dto.getIdFimAFimOriginal())
                .tpoPspSolicitanteCancelamento(RECEBEDOR)
                .tpoStatus(TipoStatusCancelamentoInstrucaoPagamento.CRIADA.name())
                .numCpfCnpjSolicitanteCancelamento(camt055Dto.getCpfCnpjUsuarioSolicitanteCancelamento())
                .codMotivoCancelamento(camt055Dto.getMotivoCancelamento())
                .codMotivoRejeicao(motivoRejeicao != null ? motivoRejeicao.name() : null)
                .datCriacaoSolicitacaoCancelamento(camt055Dto.getDataHoraSolicitacaoOuInformacao())
                .datAnalisadoSolicitacaoCancelamento(LocalDateTime.now())
                .build();
        try {
            repository.save(recorrenciaInstrucaoPagamentoCancelamento);
        } catch (Exception ex) {
            throw new TechnicalException("Erro ao persistir o cancelamento da instrução de pagamento.", ex);
        }
    }
}