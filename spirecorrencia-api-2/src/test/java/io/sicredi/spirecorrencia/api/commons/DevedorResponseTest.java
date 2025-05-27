package io.sicredi.spirecorrencia.api.commons;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DevedorResponseTest {

    private static final String NOME_DEVEDOR = "NOME DO DEVEDOR";
    private static final String CPF_CNPJ_DEVEDOR = "11111111111";

    @Test
    void dadoNomeCpfCnpj_quandoFrom_deveRetornarDevedor() {
        var devedorResponse = DevedorResponse.from(NOME_DEVEDOR, CPF_CNPJ_DEVEDOR);

        assertThat(devedorResponse)
                .usingRecursiveComparison()
                .isEqualTo(DevedorResponseTest.ProviderTest.criarDevedorFromEsperado());
    }

    private static final class ProviderTest {

        public static DevedorResponse criarDevedorFromEsperado() {
            return DevedorResponse.builder()
                    .cpfCnpj(CPF_CNPJ_DEVEDOR)
                    .nome(NOME_DEVEDOR)
                    .build();

        }
    }
}
