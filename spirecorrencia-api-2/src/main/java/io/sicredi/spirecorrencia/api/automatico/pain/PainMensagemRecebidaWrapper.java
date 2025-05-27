package io.sicredi.spirecorrencia.api.automatico.pain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
class PainMensagemRecebidaWrapper {

    private final String tipoMensagem;
    private final String payload;
    private final String idIdempotencia;

    public String getIdIdempotencia() {
        return new StringBuilder()
                .append(idIdempotencia)
                .append("_")
                .append(tipoMensagem)
                .append("_REC")
                .toString();
    }

}
