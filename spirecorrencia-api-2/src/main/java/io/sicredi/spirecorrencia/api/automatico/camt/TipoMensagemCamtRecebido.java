package io.sicredi.spirecorrencia.api.automatico.camt;

import java.util.Arrays;
import java.util.Optional;

enum TipoMensagemCamtRecebido {

    CAMT029, CAMT055;

    public static Optional<TipoMensagemCamtRecebido> obterTipoIcomRecebido(String tipoMensagem) {
        return Arrays.stream(TipoMensagemCamtRecebido.values())
                .filter(tipoPainRecebido -> tipoPainRecebido.name().equals(tipoMensagem))
                .findFirst();
    }
}