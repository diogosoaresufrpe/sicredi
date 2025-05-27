package io.sicredi.spirecorrencia.api.exclusao;

import io.sicredi.spirecorrencia.api.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@RequiredArgsConstructor
class ExclusaoTitularidadeContaKafkaConfig {

    private final AppConfig appConfig;

    @Bean(name = "holdersMaintenanceContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> holdersMaintenanceContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL);
        factory.setConsumerFactory(consumerFactory);
        var topicConfig = appConfig.getKafka().getConsumer().getHoldersMaintenance();
        var concurrency = topicConfig.getConcurrency().intValue();
        factory.setConcurrency(concurrency);
        var backOff = new FixedBackOff(topicConfig.getRetry().getDelay(), topicConfig.getRetry().getTentativas());
        factory.setCommonErrorHandler(new DefaultErrorHandler((consumerRecord, exception) -> {
            if (log.isDebugEnabled()) {
                log.error("Erro ao consumir mensagem do tópico {}, partição {} e offset {}. Número de tentativas extrapolado. (headers=({}), key=({}), value=({}))",
                        consumerRecord.topic(),
                        consumerRecord.partition(),
                        consumerRecord.offset(),
                        consumerRecord.headers(),
                        consumerRecord.key(),
                        consumerRecord.value(),
                        exception
                );
            } else {
                log.error("Erro ao consumir mensagem do tópico {}, partição {} e offset {}. Número de tentativas extrapolado.",
                        consumerRecord.topic(),
                        consumerRecord.partition(),
                        consumerRecord.offset(),
                        exception
                );
            }
        }, backOff));
        return factory;
    }

}
