package io.sicredi.spirecorrencia.api.automatico.pain;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.dto.Pain014Dto;
import br.com.sicredi.spi.entities.type.StatusMensagem;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoService;
import io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento.AutorizacaoCancelamentoService;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoService;
import io.sicredi.spirecorrencia.api.automatico.solicitacao.SolicitacaoAutorizacaoRecorrenciaService;
import io.sicredi.spirecorrencia.api.utils.IdempotenteUtils;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
class ProcessarPainEnviadoStrategy {

    private static final String SUCESSO = "SUCESSO";
    private static final String RECORRENCIA_SOLICITACAO = "RECORRENCIA_SOLICITACAO";
    private static final String RECORRENCIA_AUTORIZACAO = "RECORRENCIA_AUTORIZACAO";
    private static final String CANCELAMENTO_RECEBEDOR = "CANCELAMENTO_RECEBEDOR";

    private final AutorizacaoService autorizacaoService;
    private final SolicitacaoAutorizacaoRecorrenciaService solicitacaoAutorizacaoRecorrenciaService;
    private final RecorrenciaInstrucaoPagamentoService recorrenciaInstrucaoPagamentoService;
    private final AutorizacaoCancelamentoService autorizacaoCancelamentoService;

    private final Map<TipoMensagemPainEnviado, Consumer<PainMensagemEnviadaWrapper>> estrategiasProcessamento = new EnumMap<>(TipoMensagemPainEnviado.class);

    @PostConstruct
    void setup() {
        estrategiasProcessamento.put(TipoMensagemPainEnviado.PAIN011, this::processarPain011);
        estrategiasProcessamento.put(TipoMensagemPainEnviado.PAIN012, this::processarPain012);
        estrategiasProcessamento.put(TipoMensagemPainEnviado.PAIN014, this::processarPain014);
    }

    public void processar(PainMensagemEnviadaWrapper painWrapper) {
        var tipoPainEnviado = TipoMensagemPainEnviado.obterTipoIcomEnviado(painWrapper.getTipoMensagem())
                .orElseThrow(() -> new TechnicalException("Tipo de mensagem inválido: " + painWrapper.getTipoMensagem()));

        estrategiasProcessamento
                .getOrDefault(tipoPainEnviado, this::descartarMensagem)
                .accept(painWrapper);
    }

    private void processarPain011(PainMensagemEnviadaWrapper wrapper) {
        if (StatusMensagem.SUCESSO.name().equals(wrapper.getStatusEnvio())) {
            var pain011 = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Pain011Dto.class);

            var request = IdempotentAsyncRequest
                    .<Pain011Dto>builder()
                    .value(pain011)
                    .transactionId(wrapper.getIdIdempotencia())
                    .checkSumContentRule(() -> IdempotenteUtils.criarChecksumPain011(pain011))
                    .build();

            autorizacaoCancelamentoService.processarPedidoCancelamentoPagador(request);
        } else {
            log.info("Descartando mensagem PAIN011, pois o envio da mensagem ao bacen não foi concluido com sucesso. Status recebido: {}, ID_IDEMPOTENCIA: {}", wrapper.getStatusEnvio(), wrapper.getIdIdempotencia());
        }
    }

    private void processarPain012(PainMensagemEnviadaWrapper wrapper) {
        var pain012 = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Pain012Dto.class);

        if (!SUCESSO.equals(wrapper.getStatusEnvio())) {
            log.info("Descartando processamento pois o envio da mensagem ao bacen não foi concluido com sucesso. Status recebido: {}, ID_INFORMACAO_STATUS: {}", wrapper.getStatusEnvio(), pain012.getIdInformacaoStatus());
            return;
        }

        var request = IdempotentAsyncRequest
                .<Pain012Dto>builder()
                .value(pain012)
                .transactionId(wrapper.getIdIdempotencia())
                .checkSumContentRule(() -> IdempotenteUtils.criarChecksumPain012(pain012))
                .build();

        switch (wrapper.getOperacao()) {
            case RECORRENCIA_SOLICITACAO -> solicitacaoAutorizacaoRecorrenciaService.processarRetornoBacenSolicitacaoAutorizacao(request);
            case RECORRENCIA_AUTORIZACAO -> autorizacaoService.processarRetornoAutorizacaoAposEnvioBacen(request);
            case CANCELAMENTO_RECEBEDOR -> autorizacaoService.processarRetornoPedidoCancelamento(request);
            default -> log.warn(
                    "[Pain Enviada] - Operação inválida ou não suportada para PAIN012. Operação: {}, TipoMensagem: {}, Idempotência: {}",
                    wrapper.getOperacao(),
                    wrapper.getTipoMensagem(),
                    wrapper.getIdIdempotencia()
            );
        }
    }

    private void processarPain014(PainMensagemEnviadaWrapper wrapper) {
        var pain014 = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Pain014Dto.class);

        var request = IdempotentAsyncRequest
                .<Pain014Dto>builder()
                .value(pain014)
                .transactionId(wrapper.getIdIdempotencia())
                .checkSumContentRule(() -> IdempotenteUtils.criarChecksumPain014(pain014))
                .build();

        recorrenciaInstrucaoPagamentoService.processarRetornoPedidoAgendamentoDebito(request);
    }


    private void descartarMensagem(PainMensagemEnviadaWrapper wrapper) {
        log.debug("Mensagem descartada: tipo não possui estratégia de processamento. TIPO_MENSAGEM: {}", wrapper.getTipoMensagem());
    }

}
