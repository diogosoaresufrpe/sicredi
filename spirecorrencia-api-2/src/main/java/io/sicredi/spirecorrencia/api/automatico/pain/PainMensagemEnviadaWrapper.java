package io.sicredi.spirecorrencia.api.automatico.pain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
class PainMensagemEnviadaWrapper {

    private final String tipoMensagem;
    private final String operacao;
    private final String statusEnvio;
    private final String payload;
    private final String idIdempotencia;

    public String getIdIdempotencia() {
        return new StringBuilder()
                .append(idIdempotencia)
                .append("_")
                .append(tipoMensagem)
                .append("_ENV")
                .toString();
    }
}
