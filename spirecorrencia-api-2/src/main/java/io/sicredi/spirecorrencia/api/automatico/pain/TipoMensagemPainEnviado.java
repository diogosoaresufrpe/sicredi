package io.sicredi.spirecorrencia.api.automatico.pain;

import java.util.Arrays;
import java.util.Optional;


enum TipoMensagemPainEnviado {
    PAIN011, PAIN012, PAIN014;

    public static Optional<TipoMensagemPainEnviado> obterTipoIcomEnviado(String tipoMensagem) {
        return Arrays.stream(TipoMensagemPainEnviado.values())
                .filter(tipoPainRecebido -> tipoPainRecebido.name().equals(tipoMensagem))
                .findFirst();
    }
}