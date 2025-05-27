package io.sicredi.spirecorrencia.api.commons;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PagadorResponseTest {

    private static final String NOME_PAGADOR = "NOME DO PAGADOR";
    private static final String CPF_CNPJ_PAGADOR = "11111111111";

    @Test
    void dadoPagador_quandoFromDetalhesParcelas_deveRetornarPagador() {
        var pagadorMock = TestFactory.PagadorTestFactory.criarPagador();
        var pagadorResponse = PagadorResponse.fromDetalhesParcelas(pagadorMock);

        assertThat(pagadorResponse)
                .usingRecursiveComparison()
                .isEqualTo(PagadorResponseTest.ProviderTest.criarPagadorFromDetalhesEsperado());
    }

    @Test
    void dadoNomeECpfCnpj_quandoFrom_deveRetornarPagador() {
        var pagadorResponse = PagadorResponse.from(NOME_PAGADOR, CPF_CNPJ_PAGADOR);

        assertThat(pagadorResponse)
                .usingRecursiveComparison()
                .isEqualTo(PagadorResponseTest.ProviderTest.criarPagadorFromEsperado());
    }

    private static final class ProviderTest {

        public static PagadorResponse criarPagadorFromDetalhesEsperado() {
            return PagadorResponse.builder()
                    .cpfCnpj("00248158023")
                    .nome("Fulano de Tal")
                    .agencia("0101")
                    .instituicao("91586982")
                    .conta("003039")
                    .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                    .build();

        }

        public static PagadorResponse criarPagadorFromEsperado() {
            return PagadorResponse.builder()
                    .cpfCnpj(CPF_CNPJ_PAGADOR)
                    .nome(NOME_PAGADOR)
                    .build();

        }
    }
}
