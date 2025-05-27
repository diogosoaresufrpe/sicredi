package io.sicredi.spirecorrencia.api.automatico.camt;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
class CamtMensagemRecebidoWrapper {

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