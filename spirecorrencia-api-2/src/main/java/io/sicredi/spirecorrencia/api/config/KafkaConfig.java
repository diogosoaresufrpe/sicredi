package io.sicredi.spirecorrencia.api.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@RefreshScope
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Bean
    @Primary
    public KafkaTemplate<String, String> kafkaTemplateDefault(ProducerFactory<String, String> producerFactory) {
        var kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setObservationEnabled(true);
        return kafkaTemplate;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> protocoloKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(null)));
        return factory;
    }

    public ConcurrentKafkaListenerContainerFactory<String, String> buildConcurrentKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            AppConfig.Kafka.Consumer.ConsumerProps topicConfig
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setObservationEnabled(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        var backOff = new FixedBackOff(topicConfig.getRetry().getDelay(), topicConfig.getRetry().getTentativas());
        factory.setConcurrency(topicConfig.getConcurrency().intValue());

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