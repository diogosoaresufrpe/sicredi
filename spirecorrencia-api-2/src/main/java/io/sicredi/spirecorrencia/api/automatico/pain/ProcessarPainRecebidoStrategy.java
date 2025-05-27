package io.sicredi.spirecorrencia.api.automatico.pain;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.Pain009Dto;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.dto.Pain013Dto;
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
class ProcessarPainRecebidoStrategy {

    private final AutorizacaoService autorizacaoService;
    private final SolicitacaoAutorizacaoRecorrenciaService solicitacaoAutorizacaoRecorrenciaService;
    private final RecorrenciaInstrucaoPagamentoService recorrenciaInstrucaoPagamentoService;
    private final AutorizacaoCancelamentoService autorizacaoCancelamentoService;

    private final Map<TipoMensagemPainRecebido, Consumer<PainMensagemRecebidaWrapper>> estrategiasProcessamento = new EnumMap<>(TipoMensagemPainRecebido.class);

    @PostConstruct
    void setup() {
        estrategiasProcessamento.put(TipoMensagemPainRecebido.PAIN009, this::processarPain009);
        estrategiasProcessamento.put(TipoMensagemPainRecebido.PAIN011, this::processarPain011);
        estrategiasProcessamento.put(TipoMensagemPainRecebido.PAIN012, this::processarPain012);
        estrategiasProcessamento.put(TipoMensagemPainRecebido.PAIN013, this::processarPain013);
    }

    public void processar(PainMensagemRecebidaWrapper wrapper) {
        var tipoPainRecebido = TipoMensagemPainRecebido.obterTipoIcomRecebido(wrapper.getTipoMensagem())
                .orElseThrow(() -> new TechnicalException("Tipo de mensagem inválido: " + wrapper.getTipoMensagem()));

        estrategiasProcessamento.get(tipoPainRecebido).accept(wrapper);
    }

    private void processarPain009(PainMensagemRecebidaWrapper wrapper) {
        var pain009 = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Pain009Dto.class);

        var request = IdempotentAsyncRequest
                .<Pain009Dto>builder()
                .value(pain009)
                .transactionId(wrapper.getIdIdempotencia())
                .checkSumContentRule(() -> IdempotenteUtils.criarChecksumPain009(pain009))
                .build();

        solicitacaoAutorizacaoRecorrenciaService.processarSolicitacaoAutorizacao(request);
    }

    private void processarPain011(PainMensagemRecebidaWrapper wrapper) {
        var pain011 = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Pain011Dto.class);

        var request = IdempotentAsyncRequest
                .<Pain011Dto>builder()
                .value(pain011)
                .transactionId(wrapper.getIdIdempotencia())
                .checkSumContentRule(() -> IdempotenteUtils.criarChecksumPain011(pain011))
                .build();


        autorizacaoCancelamentoService.processarPedidoCancelamentoRecebedor(request);
    }

    private void processarPain012(PainMensagemRecebidaWrapper wrapper) {
        var pain012 = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Pain012Dto.class);

        var request = IdempotentAsyncRequest
                .<Pain012Dto>builder()
                .value(pain012)
                .transactionId(wrapper.getIdIdempotencia())
                .checkSumContentRule(() -> IdempotenteUtils.criarChecksumPain012(pain012))
                .build();

        autorizacaoService.processarRecebimentoPain012Bacen(request);
    }

    private void processarPain013(PainMensagemRecebidaWrapper wrapper) {
        var pain013 = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Pain013Dto.class);

        var request = IdempotentAsyncRequest
                .<Pain013Dto>builder()
                .value(pain013)
                .transactionId(wrapper.getIdIdempotencia())
                .checkSumContentRule(() -> IdempotenteUtils.criarChecksumPain013(pain013))
                .build();

        recorrenciaInstrucaoPagamentoService.processarPedidoAgendamentoDebito(request);

    }


}