package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CancelamentoAgendamentoDebitoTransacaoDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseConsumer;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterService;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.StringJoiner;

@Slf4j
@Component
class CancelamentoAgendamentoInstrucaoPagamentoConsumer extends ProtocoloBaseConsumer<CancelamentoAgendamentoDebitoRequest> {

    private static final String CANCELAMENTO_DEBITO = "Cancelamento de um Agendamento de Débito";

    private final CancelamentoAgendamentoInstrucaoPagamentoService cancelamentoAgendamentoInstrucaoPagamentoService;
    private final AppConfig appConfig;

    public CancelamentoAgendamentoInstrucaoPagamentoConsumer(DeadLetterService reprocessamentoService,
                                                             ObservabilidadeDecorator observabilidadeDecorator,
                                                             CancelamentoAgendamentoInstrucaoPagamentoService service,
                                                             AppConfig appConfig) {
        super(reprocessamentoService, observabilidadeDecorator);
        this.cancelamentoAgendamentoInstrucaoPagamentoService = service;
        this.appConfig = appConfig;
    }

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.cancelamentoDebito.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.cancelamentoDebito.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            include = {KafkaException.class, TechnicalException.class},
            timeout = "#{@appConfig.kafka.consumer.cancelamentoDebito.retry.timeout}",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.cancelamentoDebito.nome}",
            concurrency = "#{@appConfig.kafka.consumer.cancelamentoDebito.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.cancelamentoDebito.groupId}",
            containerFactory = "protocoloKafkaListenerContainerFactory"
    )
    protected void consumir(@Header(value = "X-Ultima-Interacao", required = false) String dataHoraInicioCanal,
                            @Payload final String payload,
                            Acknowledgment acknowledgment) {
        processarMensagem(
                payload,
                dataHoraInicioCanal,
                CANCELAMENTO_DEBITO,
                cancelamentoAgendamentoInstrucaoPagamentoService::processarCancelamentoDebito,
                acknowledgment
        );
    }

    @DltHandler
    public void handlerDlt(@Payload final String payload,
                           @Header(value = KafkaHeaders.EXCEPTION_CAUSE_FQCN, required = false) String causaException,
                           @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String mensagemException,
                           @Header(value = "X-Ultima-Interacao", required = false) String dataHoraInicioCanal,
                           Acknowledgment acknowledgment) {
        processarDlt(payload, causaException, mensagemException, acknowledgment);
    }

    @Override
    protected CancelamentoAgendamentoDebitoRequest lerPayloadTransacao(ProtocoloDTO protocolo, String identificadorTransacao, String dataHoraInicioCanal, ZonedDateTime dataHoraRecebimentoMensagem) {
        var transacao = ObjectMapperUtil.converterStringParaObjeto(protocolo.getPayloadTransacao(), new TypeReference<CancelamentoAgendamentoDebitoTransacaoDTO>() {
        });

        return CancelamentoAgendamentoDebitoRequest.fromEmissaoProtocolo(
                protocolo,
                transacao,
                identificadorTransacao,
                dataHoraInicioCanal,
                dataHoraRecebimentoMensagem
        );

    }

    @Override
    protected byte[] criarCheckSum(CancelamentoAgendamentoDebitoRequest request) {
        var joiner = new StringJoiner(StringUtils.EMPTY);
        joiner.add(request.getIdCancelamentoAgendamento());
        joiner.add(request.getIdFimAFim());
        joiner.add(request.getOidRecorrenciaAutorizacao().toString());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected String criarChaveIdempotencia(String identificadorTransacao) {
        String tipoOperacao = "CANCEL";
        String tipoJornada = "DEB";

        return identificadorTransacao
                .concat("_")
                .concat(tipoOperacao)
                .concat("_")
                .concat(tipoJornada);
    }

}
