package io.sicredi.spirecorrencia.api.idempotente;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CriaResponseStrategyFactory<T extends IdempotenteRequest> {
    private final Set<CriaResponseStrategy<T>> strategies;

    public CriaResponseStrategy<T> criar(TipoResponseIdempotente tipoResponse) {
        return Optional.ofNullable(strategies)
                .orElse(Collections.emptySet())
                .stream()
                .filter(strategy -> strategy.obterTipoResponse() == tipoResponse)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Estratégia de resposta não implementada " + tipoResponse));
    }
}
