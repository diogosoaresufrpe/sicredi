package io.sicredi.spirecorrencia.api.automatico.pain;

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
class IcomPainRecebidoConsumer {

    private final ProcessarPainRecebidoStrategy processarPainRecebidoStrategy;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final AppConfig appConfig;

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.icomPainRecebido.retry.tentativas}",
            autoCreateTopics = "false",
            include = {KafkaException.class, TechnicalException.class},
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.icomPainRecebido.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            timeout = "#{@appConfig.kafka.consumer.icomPainRecebido.retry.timeout}",
            retryTopicSuffix = "-spirecorrencia-api-v1-retry",
            dltStrategy = DltStrategy.NO_DLT
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.icomPainRecebido.nome}",
            concurrency = "#{@appConfig.kafka.consumer.icomPainRecebido.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.icomPainRecebido.groupId}",
            containerFactory = "icomPainContainerFactory"
    )
    public void consumir(@Header("TIPO_MENSAGEM") String tipoMensagem,
                         @Header("ID_IDEMPOTENCIA") String idIdempotencia,
                         ConsumerRecord<String, String> consumerRecord,
                         Acknowledgment acknowledgment) {

        var atributos = Map.of(
                RecorrenciaMdc.ID_IDEMPOTENCIA, idIdempotencia,
                RecorrenciaMdc.TIPO_MENSAGEM, tipoMensagem);

        observabilidadeDecorator.executar(atributos, () -> {
            var wrapper = PainMensagemRecebidaWrapper.of(tipoMensagem, consumerRecord.value(), idIdempotencia);
            var idIdempotenciaFormatada = wrapper.getIdIdempotencia();

            log.debug("[Pain Recebida] - Inicio do consumo de pain recebida. Tipo Mensagem: {}, Idempotência Recebida: {}, Idempotência Formatada: {}", tipoMensagem, idIdempotencia, idIdempotenciaFormatada);

            processarPainRecebidoStrategy.processar(wrapper);

            log.debug("[Pain Recebida] - Fim do consumo de pain recebida. Tipo Mensagem: {}, Idempotência Recebida: {}, Idempotência Formatada: {}", tipoMensagem, idIdempotencia, idIdempotenciaFormatada);
        });

        acknowledgment.acknowledge();
    }

}
