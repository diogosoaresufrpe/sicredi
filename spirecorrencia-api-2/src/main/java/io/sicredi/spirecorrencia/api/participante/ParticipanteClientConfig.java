package io.sicredi.spirecorrencia.api.participante;

import org.springframework.context.annotation.Bean;

@SuppressWarnings("unused")
class ParticipanteClientConfig {
    @Bean
    public ParticipanteClientErrorDecoder errorDecoder() {
        return new ParticipanteClientErrorDecoder();
    }
}