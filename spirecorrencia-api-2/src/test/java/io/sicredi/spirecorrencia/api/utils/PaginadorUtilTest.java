package io.sicredi.spirecorrencia.api.utils;

import org.junit.jupiter.api.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class PaginadorUtilTest {

    @Nested
    @DisplayName("PaginadorUtil deve")
    class PaginadorUtilTests {

        @Test
        @DisplayName("processar todos os elementos de múltiplas páginas")
        void dadoUmaListaQualquer_quandoPaginar_deveProcessarTodosItensComSucesso() {
            // Arrange
            int tamanhoPagina = 2;
            List<String> dados = List.of("A", "B", "C", "D", "E");
            List<String> processados = new ArrayList<>();

            BiFunction<Integer, Integer, Page<String>> pageSupplier = (pagina, tamanho) -> {
                int start = pagina * tamanho;
                int end = Math.min(start + tamanho, dados.size());
                if (start >= dados.size()) return Page.empty();
                return new PageImpl<>(dados.subList(start, end));
            };

            // Act
            PaginadorUtil.paginar(tamanhoPagina, pageSupplier, processados::add);

            // Assert
            assertAll(
                () -> assertEquals(dados.size(), processados.size()),
                () -> assertIterableEquals(dados, processados)
            );
        }

        @Test
        @DisplayName("não processar nada se a primeira página estiver vazia")
        void dadoUmaPaginaVazia_quandoPaginar_deveNaoProcessarElementos() {
            // Arrange
            BiFunction<Integer, Integer, Page<String>> pageSupplier = (pagina, tamanho) -> Page.empty();
            List<String> processados = new ArrayList<>();

            // Act
            PaginadorUtil.paginar(5, pageSupplier, processados::add);

            // Assert
            assertTrue(processados.isEmpty());
        }

        @Test
        @DisplayName("continuar processamento mesmo com exceção em um item")
        void dadoUmItemComException_quandoPaginar_deveContinuarOProcessamentoDosOutrosItemComSucesso() {
            // Arrange
            List<String> dados = List.of("A", "B", "C");
            List<String> processados = new ArrayList<>();

            BiFunction<Integer, Integer, Page<String>> pageSupplier = (pagina, tamanho) -> {
                if (pagina > 0) return Page.empty();
                return new PageImpl<>(dados);
            };

            // Act
            PaginadorUtil.paginar(3, pageSupplier, item -> {
                if ("B".equals(item)) throw new RuntimeException("Erro simulado");
                processados.add(item);
            });

            // Assert
            assertEquals(List.of("A", "C"), processados);
        }
    }
}
