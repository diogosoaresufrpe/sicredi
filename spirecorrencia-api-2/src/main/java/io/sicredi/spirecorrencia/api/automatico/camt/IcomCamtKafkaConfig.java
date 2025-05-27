package io.sicredi.spirecorrencia.api.automatico.camt;

import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Slf4j
@Configuration
@RequiredArgsConstructor
class IcomCamtKafkaConfig {

    private final AppConfig appConfig;
    private final KafkaConfig kafkaConfig;

    @Bean(name = "icomCamtRecebidoContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> icomCamtRecebidoContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        var topicConfig = appConfig.getKafka().getConsumer().getIcomCamtRecebido();
        return kafkaConfig.buildConcurrentKafkaListenerContainerFactory(consumerFactory, topicConfig);
    }
}
