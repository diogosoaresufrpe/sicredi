package io.sicredi.spirecorrencia.api.automatico.camt;

import br.com.sicredi.framework.exception.TechnicalException;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class IcomCamtRecebidoConsumer {

    private final ProcessarCamtRecebidoStrategy processarCamtRecebidoStrategy;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final AppConfig appConfig;

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.icomCamtRecebido.retry.tentativas}",
            autoCreateTopics = "false",
            include = {KafkaException.class, TechnicalException.class},
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.icomCamtRecebido.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            timeout = "#{@appConfig.kafka.consumer.icomCamtRecebido.retry.timeout}",
            retryTopicSuffix = "-spirecorrencia-api-v1-retry",
            dltStrategy = DltStrategy.NO_DLT
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.icomCamtRecebido.nome}",
            concurrency = "#{@appConfig.kafka.consumer.icomCamtRecebido.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.icomCamtRecebido.groupId}",
            containerFactory = "icomCamtRecebidoContainerFactory"
    )
    public void consumir(@Header("TIPO_MENSAGEM") String tipoMensagem,
                         @Header("ID_IDEMPOTENCIA") String idIdempotencia,
                         ConsumerRecord<String, String> consumerRecord,
                         Acknowledgment acknowledgment) {

        var atributos = Map.of(
                RecorrenciaMdc.ID_IDEMPOTENCIA, idIdempotencia,
                RecorrenciaMdc.TIPO_MENSAGEM, tipoMensagem);

        observabilidadeDecorator.executar(atributos, () -> {
            var wrapper = CamtMensagemRecebidoWrapper.of(tipoMensagem, consumerRecord.value(), idIdempotencia);
            var idIdempotenciaFormatada = wrapper.getIdIdempotencia();
            log.debug("[Camt Recebida] - Inicio do consumo de camt recebida. Tipo Mensagem: {}, Idempotência Recebida: {}, Idempotência Formatada: {}", tipoMensagem, idIdempotencia, idIdempotenciaFormatada);


            processarCamtRecebidoStrategy.processar(wrapper);

            log.debug("[Camt Recebida] - Fim do consumo de camt recebida. Tipo Mensagem: {}, Idempotência Recebida: {}, Idempotência Formatada: {}", tipoMensagem, idIdempotencia, idIdempotenciaFormatada);
        });
        acknowledgment.acknowledge();
    }
}