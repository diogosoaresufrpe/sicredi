package io.sicredi.spirecorrencia.api.automatico.pain;

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
class IcomPainRecebidoKafkaConfig {

    private final AppConfig appConfig;
    private final KafkaConfig containerFactoryBuilder;

    @Bean(name = "icomPainContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, String> icomPainContainerFactory(ConsumerFactory<String, String> consumerFactory) {
        var topicConfig = appConfig.getKafka().getConsumer().getIcomPainRecebido();
        return containerFactoryBuilder.buildConcurrentKafkaListenerContainerFactory(consumerFactory, topicConfig);
    }
}
