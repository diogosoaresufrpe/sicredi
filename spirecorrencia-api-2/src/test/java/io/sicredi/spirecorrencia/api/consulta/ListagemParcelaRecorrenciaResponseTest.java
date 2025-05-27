package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class ListagemParcelaRecorrenciaResponseTest {

    private static final String ID_PARCELA = "17b1caba-a6ab-4d25-ad8a-2b30bc17218a";
    private static final TipoStatusEnum STATUS = TipoStatusEnum.CRIADO;
    private static final LocalDate DATA_TRANSACAO = LocalDate.of(2025, 10, 10);
    private static final TipoRecorrencia TIPO_RECORRENCIA = TipoRecorrencia.AGENDADO_RECORRENTE;
    private static final LocalDateTime DATA_CRIACAO = LocalDateTime.of(2025, 9, 10, 0, 0);
    private static final String NOME_RECEBEDOR = "SUPERMERCADO E ACOUGUE SAO JOSE";

    @Test
    void dadoRecorrencia_quandoFromListagemRecorrencia_deveRetornarRecorrencia() {
        var recorrencia = TestFactory.RecorrenciaTestFactory.criarRecorrencia(DATA_CRIACAO);
        var parcela = ParcelaRecorrenciaMock.builder()
                .nomeRecebedor(NOME_RECEBEDOR)
                .identificadorParcela(ID_PARCELA)
                .status(TipoStatusEnum.CRIADO)
                .dataTransacao(DATA_TRANSACAO)
                .tipoRecorrencia(recorrencia.getTipoRecorrencia())
                .valor(BigDecimal.TEN)
                .build();

        var listagemParcelaRecorrenciaResponse = ListagemParcelaRecorrenciaResponse.fromTransacaoRecorrencia(parcela);

        assertThat(listagemParcelaRecorrenciaResponse)
                .usingRecursiveComparison()
                .isEqualTo(ProviderTest.criarListagemParcelaResponseEsperado());
    }

    @Test
    void dadoRecorrenciaNula_quandoFromListagemRecorrencia_deveRetornarNulo() {
        assertNull(ListagemParcelaRecorrenciaResponse.fromTransacaoRecorrencia(null));
    }

    private static final class ProviderTest {
        public static ListagemParcelaRecorrenciaResponse criarListagemParcelaResponseEsperado() {
            var recebedor = RecebedorResponse.builder()
                    .nome(NOME_RECEBEDOR)
                    .build();

            return ListagemParcelaRecorrenciaResponse.builder()
                    .identificadorParcela(ID_PARCELA)
                    .valor(BigDecimal.TEN)
                    .dataTransacao(DATA_TRANSACAO)
                    .status(STATUS)
                    .tipoRecorrencia(TIPO_RECORRENCIA)
                    .recebedor(recebedor)
                    .build();
        }
    }
}