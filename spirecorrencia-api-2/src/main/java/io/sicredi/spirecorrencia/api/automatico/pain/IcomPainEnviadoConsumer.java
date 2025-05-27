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
class IcomPainEnviadoConsumer {

    private static final String PAGADOR = "PAGADOR";

    private final ProcessarPainEnviadoStrategy processarPainEnviadoStrategy;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final AppConfig appConfig;

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.icomPainEnviado.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.icomPainEnviado.retry.delay}"),
            include = {KafkaException.class, TechnicalException.class},
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            timeout = "#{@appConfig.kafka.consumer.icomPainEnviado.retry.timeout}",
            retryTopicSuffix = "-spirecorrencia-api-v1-retry",
            dltStrategy = DltStrategy.NO_DLT
    )
    @KafkaListener(
            topics = {
                    "#{@appConfig.kafka.consumer.icomPainEnviado.nome}",
                    "#{@appConfig.kafka.consumer.icomPainEnviadoFalha.nome}"
            },
            concurrency = "#{@appConfig.kafka.consumer.icomPainEnviado.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.icomPainEnviado.groupId}",
            containerFactory = "icomPainContainerFactory"
    )
    public void consumir(@Header("TIPO_MENSAGEM") String tipoMensagem,
                         @Header("OPERACAO") String operacao,
                         @Header("PSP_EMISSOR") String pspEmissor,
                         @Header("STATUS_ENVIO") String statusEnvio,
                         @Header("ID_IDEMPOTENCIA") String idIdempotencia,
                         ConsumerRecord<String, String> consumerRecord,
                         Acknowledgment acknowledgment) {

        if(!PAGADOR.equals(pspEmissor)){
            log.warn("Descartando evento do tópico {} pois mensagem não contém o PSP_EMISSOR com valor esperado. PSP_EMISSOR recebido = {}", consumerRecord.topic(), pspEmissor);
        }

        var atributos = Map.of(
                RecorrenciaMdc.ID_IDEMPOTENCIA, idIdempotencia,
                RecorrenciaMdc.TIPO_MENSAGEM, tipoMensagem,
                RecorrenciaMdc.OPERACAO_AUTOMATICO, operacao);

        observabilidadeDecorator.executar(atributos, () -> {
            var wrapper = PainMensagemEnviadaWrapper.of(tipoMensagem, operacao, statusEnvio, consumerRecord.value(), idIdempotencia);
            var idIdempotenciaFormatada = wrapper.getIdIdempotencia();

            log.debug("[Pain Enviada] - Inicio do consumo de pain enviada. Tipo Mensagem: {}, Idempotência Recebida: {}, Idempotência Formatada: {}", tipoMensagem, idIdempotencia, idIdempotenciaFormatada);

            processarPainEnviadoStrategy.processar(wrapper);

            log.debug("[Pain Enviada] - Fim do consumo de pain enviada. Tipo Mensagem: {}, Idempotência Recebida: {}, Idempotência Formatada: {}", tipoMensagem, idIdempotencia, idIdempotenciaFormatada);
        });

        acknowledgment.acknowledge();
    }

}
