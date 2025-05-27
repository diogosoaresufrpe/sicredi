package io.sicredi.spirecorrencia.api.idempotente;


import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentEvent;

import java.util.Map;

public record EventoResponseDTO(Object mensagemJson, Map<String, String> headers, String topic) {

    public IdempotentEvent<?> toIdempotentEvent() {
        return IdempotentEvent.builder()
                .headers(this.headers)
                .topic(this.topic)
                .value(this.mensagemJson)
                .build();
    }
}
