package io.sicredi.spirecorrencia.api.metrica;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Serviço responsável por registrar métricas utilizando o Micrometer.
 *
 * <p>Esta classe utiliza um {@link MeterRegistry} para registrar contadores e temporizadores
 * com base no tipo de métrica fornecido. As métricas são registradas e incrementadas
 * conforme necessário.</p>
 *
 * <p>Os tipos de métricas suportados são definidos pela enum {@link TipoMetrica} e as
 * estratégias de registro são armazenadas em um mapa.</p>
 *
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistraMetricaService {

    private final MeterRegistry meterRegistry;
    private final Map<Integer, Counter.Builder> countersBuilders = new HashMap<>();

    private final Map<TipoMetrica, Consumer<Metrica>> strategies = new EnumMap<>(Map.of(
            TipoMetrica.COUNTER, metrica -> incrementarCounter((MetricaCounter) metrica)
    ));

    /**
     * Registra uma métrica utilizando a estratégia apropriada com base no tipo de métrica fornecido.
     *
     * <p>Este método verifica o tipo de métrica e aplica a estratégia de registro correspondente.
     * Se o tipo de métrica não for suportado, um erro será registrado no log.</p>
     *
     * @param metrica a métrica a ser registrada
     */
    public void registrar(Metrica metrica) {
        Optional.ofNullable(strategies.get(metrica.getTipoMetrica()))
                .ifPresentOrElse(strategy -> strategy.accept(metrica),
                        () -> log.error("Registro de métrica não suportado para o tipo: {}", metrica.getTipoMetrica())
                );
    }

    /**
     * Incrementa o contador para a métrica fornecida.
     *
     * <p>Este método cria ou recupera um builder de contador para a métrica especificada,
     * registra o contador no `MeterRegistry` e incrementa seu valor.</p>
     *
     * <p>Se ocorrer um erro durante o registro ou incremento da métrica, uma mensagem de erro
     * será registrada no log.</p>
     *
     * @param metrica a métrica do tipo `MetricaCounter` a ser incrementada
     */
    private void incrementarCounter(MetricaCounter metrica) {
        String nomeMetrica = metrica.getNome();
        int hashNomeMetrica = nomeMetrica.hashCode();
        int hashKeys = metrica.criarTags().hashCode();
        try {
            countersBuilders
                    .computeIfAbsent(hashNomeMetrica + hashKeys, key -> criarCounterBuilder(metrica))
                    .register(meterRegistry)
                    .increment();
        } catch (Exception ex) {
            log.error("Não foi possível registrar métrica para o evento {}.", nomeMetrica, ex);
        }
    }


    private Counter.Builder criarCounterBuilder(MetricaCounter metrica) {
        return Counter.builder(metrica.getNome())
                .description(metrica.getDescricao())
                .tags(metrica.criarTags());
    }

}