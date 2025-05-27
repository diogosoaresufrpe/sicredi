package io.sicredi.spirecorrencia.api.dict;

import feign.Logger;
import org.springframework.context.annotation.Bean;

public class DictFeignClientConfig {

    @Bean
    public DictErrorDecoder errorDecoder() {
        return new DictErrorDecoder();
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
