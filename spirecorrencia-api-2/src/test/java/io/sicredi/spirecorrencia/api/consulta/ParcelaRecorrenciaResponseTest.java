package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ParcelaRecorrenciaResponseTest {

    private static final LocalDate DATA_TRANSACAO = LocalDate.of(2030, 9, 10);
    private static final LocalDateTime DATA_EXCLUSAO = LocalDateTime.of(2025, 9, 10, 0, 0);

    @Test
    void dadoListaRecorrenciaTransacao_quandoGerarParcelas_deveRetornarListaParcelas() {
        var recorrencia = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
        var parcela = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrencia, DATA_TRANSACAO, BigDecimal.TEN);

        parcela.setDataExclusao(DATA_EXCLUSAO);

        var parcelasResponse = ParcelaRecorrenciaResponse.gerarParcelas(List.of(parcela));

        assertThat(parcelasResponse)
                .usingRecursiveComparison()
                .isEqualTo(ProviderTest.criarListaParcelasResponseEsperado());
    }

    @Test
    void dadoListaRecorrenciaTransacaoNula_quandoGerarParcelas_deveRetornarListaVazia() {
        var parcelasResponse = ParcelaRecorrenciaResponse.gerarParcelas(null);

        assertThat(parcelasResponse).isEmpty();
    }

    private static final class ProviderTest {
        public static List<ParcelaRecorrenciaResponse> criarListaParcelasResponseEsperado() {
            return List.of(
                    ParcelaRecorrenciaResponse.builder()
                            .identificadorParcela("17b1caba-a6ab-4d25-ad8a-2b30bc17218a")
                            .idFimAFim("E91586982202208151245099rD6AIAa7")
                            .valor(BigDecimal.TEN)
                            .numeroParcela(1L)
                            .status(TipoStatusEnum.CRIADO)
                            .dataTransacao(DATA_TRANSACAO)
                            .dataExclusao(DATA_EXCLUSAO)
                            .build()
            );
        }
    }
}