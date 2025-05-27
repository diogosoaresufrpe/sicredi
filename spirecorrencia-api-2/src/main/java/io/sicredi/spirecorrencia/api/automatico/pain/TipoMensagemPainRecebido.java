package io.sicredi.spirecorrencia.api.automatico.pain;

import java.util.Arrays;
import java.util.Optional;


enum TipoMensagemPainRecebido {
    PAIN009, PAIN011, PAIN012, PAIN013;

    public static Optional<TipoMensagemPainRecebido> obterTipoIcomRecebido(String tipoMensagem) {
        return Arrays.stream(TipoMensagemPainRecebido.values())
                .filter(tipoPainRecebido -> tipoPainRecebido.name().equals(tipoMensagem))
                .findFirst();
    }
}