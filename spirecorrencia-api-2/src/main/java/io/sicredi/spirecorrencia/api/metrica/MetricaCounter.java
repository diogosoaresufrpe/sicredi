package io.sicredi.spirecorrencia.api.metrica;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class MetricaCounter extends Metrica {
    private final String nome;
    private final String descricao;

    @Override
    public TipoMetrica getTipoMetrica() {
        return TipoMetrica.COUNTER;
    }

}
