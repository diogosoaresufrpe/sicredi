package io.sicredi.spirecorrencia.api.automatico.camt;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.Camt055Dto;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento.RecorrenciaInstrucaoPagamentoCancelamentoService;
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
class ProcessarCamtRecebidoStrategy {

    private final RecorrenciaInstrucaoPagamentoCancelamentoService service;

    private final Map<TipoMensagemCamtRecebido, Consumer<CamtMensagemRecebidoWrapper>> estrategiasProcessamento = new EnumMap<>(TipoMensagemCamtRecebido.class);

    @PostConstruct
    void setup() {
        estrategiasProcessamento.put(TipoMensagemCamtRecebido.CAMT055, this::processarCamt055);
    }

    public void processar(CamtMensagemRecebidoWrapper wrapper) {
        var tipoPainRecebido = TipoMensagemCamtRecebido.obterTipoIcomRecebido(wrapper.getTipoMensagem())
                .orElseThrow(() -> new TechnicalException("Tipo de mensagem inválido: " + wrapper.getTipoMensagem()));

        estrategiasProcessamento.get(tipoPainRecebido).accept(wrapper);
    }

    private void processarCamt055(CamtMensagemRecebidoWrapper wrapper) {
        var camt055Dto = ObjectMapperUtil.converterStringParaObjeto(wrapper.getPayload(), Camt055Dto.class);

        var request = IdempotentAsyncRequest
                .<Camt055Dto>builder()
                .value(camt055Dto)
                .transactionId(wrapper.getIdIdempotencia())
                .checkSumContentRule(() -> IdempotenteUtils.criarCheckSumCamt055(camt055Dto))
                .build();

        service.processarSolicitacaoCancelamento(request);
    }
}