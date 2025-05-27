package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import io.sicredi.spirecorrencia.api.commons.PagadorResponse;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecorrenciaResponseTest {

    private static final LocalDateTime DATA_CRIACAO = LocalDateTime.of(2025, 9, 10, 0, 0);
    private static final LocalDate DATA_PROXIMO_PAGAMENTO = LocalDate.of(2025, 10, 10);
    private static final LocalDate DATA_ULTIMO_PAGAMENTO = LocalDate.of(2025, 9, 13);
    private static final LocalDate DATA_PRIMEIRA_PARCELA = LocalDate.of(2025, 9, 13);
    private static final LocalDate DATA_ULTIMA_PARCELA = LocalDate.of(2025, 10, 10);

    @Test
    void dadoRecorrencia_quandoFromListagemRecorrencia_deveRetornarRecorrencia() {
        var recorrencia = criarRecorrencia();

        var recorrenciaResponse = RecorrenciaResponse.fromListagemRecorrencia(recorrencia);

        assertThat(recorrenciaResponse)
                .usingRecursiveComparison()
                .isEqualTo(ProviderTest.criarRecorrenciaResponseListagemRecorrenciaEsperado());
    }

    @Test
    void dadoRecorrenciaNula_quandoFromListagemRecorrencia_deveRetornarRecorrencia() {
        assertNull(RecorrenciaResponse.fromListagemRecorrencia(null));
    }

    @Test
    void dadoRecorrencia_quandoFromDetalhesRecorrencia_deveRetornarRecorrencia() {
        var recorrencia = criarRecorrencia();

        var recorrenciaResponse = RecorrenciaResponse.fromDetalhesRecorrencia(recorrencia);

        assertThat(recorrenciaResponse)
                .usingRecursiveComparison()
                .isEqualTo(ProviderTest.criarRecorrenciaResponseDetalhesRecorrenciaEsperado());
    }

    @Test
    void dadoRecorrenciaNula_quandoFromDetalhesRecorrencia_deveRetornarRecorrencia() {
        assertNull(RecorrenciaResponse.fromDetalhesRecorrencia(null));
    }

    private static Recorrencia criarRecorrencia() {
        var recorrencia = TestFactory.RecorrenciaTestFactory.criarRecorrencia(DATA_CRIACAO);
        var parcelaConcluida = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(
                recorrencia,
                DATA_ULTIMO_PAGAMENTO,
                BigDecimal.TEN
        );
        parcelaConcluida.setTpoStatus(TipoStatusEnum.CONCLUIDO);

        var parcelaCriada = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(
                recorrencia,
                DATA_PROXIMO_PAGAMENTO,
                BigDecimal.TEN
        );
        parcelaCriada.setTpoStatus(TipoStatusEnum.CRIADO);
        recorrencia.setRecorrencias(List.of(parcelaConcluida, parcelaCriada));
        return recorrencia;
    }

    private static final class ProviderTest {
        public static RecorrenciaResponse criarRecorrenciaResponseListagemRecorrenciaEsperado() {
            var recebedor = RecebedorResponse.builder()
                    .nome("SUPERMERCADO E ACOUGUE SAO JOSE")
                    .build();

            return RecorrenciaResponse.builder()
                    .identificadorRecorrencia("d02aefe4-91be-404e-a5c6-7dff4e8b05cb")
                    .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                    .nome("Recorrência Academia")
                    .tipoStatus(TipoStatusEnum.CRIADO)
                    .valorProximoPagamento(BigDecimal.TEN)
                    .dataCriacao(DATA_CRIACAO)
                    .dataUltimoPagamentoConcluido(DATA_ULTIMO_PAGAMENTO)
                    .dataProximoPagamento(DATA_PROXIMO_PAGAMENTO)
                    .recebedor(recebedor)
                    .build();
        }

        public static RecorrenciaResponse criarRecorrenciaResponseDetalhesRecorrenciaEsperado() {
            var recebedor = RecebedorResponse.builder()
                    .nome("SUPERMERCADO E ACOUGUE SAO JOSE")
                    .chave("pix@sicredi.com.br")
                    .cpfCnpj("00248158023")
                    .instituicao("91586982")
                    .agencia("0101")
                    .conta("052124")
                    .tipoChave(TipoChaveEnum.EMAIL)
                    .build();

            var pagador = PagadorResponse.builder()
                    .nome("Fulano de Tal")
                    .agencia("0101")
                    .conta("003039")
                    .cpfCnpj("00248158023")
                    .instituicao("91586982")
                    .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                    .build();

            var primeiraParcela = ParcelaRecorrenciaResponse.builder()
                    .identificadorParcela("17b1caba-a6ab-4d25-ad8a-2b30bc17218a")
                    .idFimAFim("E91586982202208151245099rD6AIAa7")
                    .valor(BigDecimal.TEN)
                    .numeroParcela(1L)
                    .status(TipoStatusEnum.CONCLUIDO)
                    .dataTransacao(DATA_PRIMEIRA_PARCELA)
                    .build();

            var segundaParcela = ParcelaRecorrenciaResponse.builder()
                    .identificadorParcela("17b1caba-a6ab-4d25-ad8a-2b30bc17218a")
                    .idFimAFim("E91586982202208151245099rD6AIAa7")
                    .valor(BigDecimal.TEN)
                    .numeroParcela(2L)
                    .status(TipoStatusEnum.CRIADO)
                    .dataTransacao(DATA_ULTIMA_PARCELA)
                    .build();

            return RecorrenciaResponse.builder()
                    .identificadorRecorrencia("d02aefe4-91be-404e-a5c6-7dff4e8b05cb")
                    .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                    .nome("Recorrência Academia")
                    .tipoFrequencia(TipoFrequencia.MENSAL)
                    .tipoStatus(TipoStatusEnum.CRIADO)
                    .tipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_BY_KEY)
                    .numeroTotalParcelas(2L)
                    .valorProximoPagamento(BigDecimal.TEN)
                    .dataCriacao(DATA_CRIACAO)
                    .dataProximoPagamento(DATA_PROXIMO_PAGAMENTO)
                    .dataPrimeiraParcela(DATA_PRIMEIRA_PARCELA)
                    .dataUltimaParcela(DATA_ULTIMA_PARCELA)
                    .recebedor(recebedor)
                    .parcelas(List.of(primeiraParcela, segundaParcela))
                    .pagador(pagador)
                    .build();
        }


    }
}