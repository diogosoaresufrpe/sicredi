package io.sicredi.spirecorrencia.api.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginadorUtil {

    /**
     * Processa páginas de forma sequencial, suprimindo exceções por item efetuando o log do mesmo.
     *
     * @param tamanhoPagina tamanho de cada página
     * @param pageSupplier  função que recebe (número da página, tamanho) e retorna uma Page<T>
     * @param processador   função que processa cada item da página
     * @param <T>           tipo dos elementos
     */
    public static <T> void paginar(
            int tamanhoPagina,
            BiFunction<Integer, Integer, Page<T>> pageSupplier,
            Consumer<T> processador
    ) {
        Stream.iterate(0, pagina -> pagina + 1)
                .map(pagina -> pageSupplier.apply(pagina, tamanhoPagina))
                .takeWhile(page -> !page.isEmpty())
                .forEach(pagina -> pagina.getContent().forEach(item -> {
                    try {
                        processador.accept(item);
                    } catch (Exception e) {
                        log.error("Erro ao realizar o processamento do item: {}, pagina: {} ", item, pagina, e);
                    }
                }));
    }
}
