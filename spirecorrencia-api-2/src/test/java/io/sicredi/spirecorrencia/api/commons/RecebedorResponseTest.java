package io.sicredi.spirecorrencia.api.commons;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class RecebedorResponseTest {

    private static final String NOME_RECEBEDOR = "NOME DO RECEBEDOR";
    private static final String CPF_CNPJ_RECEBEDOR = "11111111111";

    @Test
    void dadoRecebedor_quandoFromDetalhesParcelas_deveRetornarRecebedor() {
        var recebedorMock = TestFactory.RecebedorTestFactory.criarRecebedor();
        var recebedorResponse = RecebedorResponse.fromDetalhesParcelas(recebedorMock);

        assertThat(recebedorResponse)
                .usingRecursiveComparison()
                .isEqualTo(RecebedorResponseTest.ProviderTest.criarRecebedorFromDetalhesParcelasEsperado());
    }

    @Test
    void dadoNomeECpfCnpj_quandoFromDetalhesParcelas_deveRetornarRecebedor() {
        var recebedorResponse = RecebedorResponse.from(NOME_RECEBEDOR, CPF_CNPJ_RECEBEDOR);

        assertThat(recebedorResponse)
                .usingRecursiveComparison()
                .isEqualTo(RecebedorResponseTest.ProviderTest.criarRecebedorFromEsperado());
    }

    private static final class ProviderTest {

        public static RecebedorResponse criarRecebedorFromDetalhesParcelasEsperado() {
            return RecebedorResponse.builder()
                    .cpfCnpj("00248158023")
                    .nome("SUPERMERCADO E ACOUGUE SAO JOSE")
                    .chave("pix@sicredi.com.br")
                    .agencia("0101")
                    .conta("052124")
                    .instituicao("91586982")
                    .tipoChave(TipoChaveEnum.EMAIL)
                    .build();

        }

        public static RecebedorResponse criarRecebedorFromEsperado() {
            return RecebedorResponse.builder()
                    .cpfCnpj(CPF_CNPJ_RECEBEDOR)
                    .nome(NOME_RECEBEDOR)
                    .build();

        }
    }
}
