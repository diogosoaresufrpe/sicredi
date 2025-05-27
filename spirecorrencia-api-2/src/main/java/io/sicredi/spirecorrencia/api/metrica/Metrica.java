package io.sicredi.spirecorrencia.api.metrica;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import java.util.HashMap;
import java.util.Optional;

public abstract class Metrica {

    private final HashMap<String, String> tags = new HashMap<>();

    public abstract TipoMetrica getTipoMetrica();

    public Tags criarTags() {
        return Tags.of(tags.entrySet().stream()
                .map(x -> Tag.of(x.getKey(), Optional.ofNullable(x.getValue()).orElse("UNKNOWN")))
                .toList());
    }

    public Metrica adicionarTag(String chave, String valor) {
        tags.put(chave, valor);
        return this;
    }

}
