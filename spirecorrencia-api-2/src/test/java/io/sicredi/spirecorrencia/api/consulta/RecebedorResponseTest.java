package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecebedorResponseTest {

    @Test
    void dadoRecebedor_quandoFromDetalhesRecorrencia_deveRetornarRecebedor() {
        var recorrencia = TestFactory.RecebedorTestFactory.criarRecebedor();
        var recebedorResponse = RecebedorResponse.fromDetalhesRecorrencia(recorrencia);

        assertThat(recebedorResponse)
                .usingRecursiveComparison()
                .isEqualTo(ProviderTest.criarRecebedorResponseDetalhesRecorrenciaEsperado());
    }

    @Test
    void dadoRecebedor_quandoFromDetalhesParcela_deveRetornarRecebedor() {
        var recorrencia = TestFactory.RecebedorTestFactory.criarRecebedor();
        var recebedorResponse = RecebedorResponse.fromDetalhesParcelas(recorrencia);

        assertThat(recebedorResponse)
                .usingRecursiveComparison()
                .isEqualTo(ProviderTest.criarRecebedorResponseDetalhesParcelaEsperado());
    }

    @Test
    void dadoRecebedorNulo_quandoFromDetalhesRecorrencia_deveRetornarRecebedor() {
        assertNull(RecebedorResponse.fromDetalhesRecorrencia(null));
    }

    @Test
    void dadoRecebedorNulo_quandoFromDetalhesParcela_deveRetornarRecebedor() {
        assertNull(RecebedorResponse.fromDetalhesParcelas(null));
    }

    @Test
    void dadoRecebedor_quandoFromListagemRecorrencia_deveRetornarRecebedor() {
        var recebedor = TestFactory.RecebedorTestFactory.criarRecebedor();
        var recebedorResponse = RecebedorResponse.fromNomeRecebedor(recebedor.getNome());

        assertThat(recebedorResponse)
                .usingRecursiveComparison()
                .isEqualTo(ProviderTest.criarRecebedorResponseListagemRecorrenciaEsperado());
    }

    @Test
    void dadoRecebedorNulo_quandoFromListagemRecorrencia_deveRetornarRecebedor() {
        assertNull(RecebedorResponse.fromNomeRecebedor(null));
    }


    private static final class ProviderTest {

        private static final String CHAVE_PIX = "pix@sicredi.com.br";
        private static final String AGENCIA = "0101";
        private static final String CONTA = "052124";
        private static final String CPF = "00248158023";
        private static final String INSTITUICAO = "91586982";
        private static final String NOME_RECEBEDOR = "SUPERMERCADO E ACOUGUE SAO JOSE";

        public static RecebedorResponse criarRecebedorResponseDetalhesRecorrenciaEsperado() {
            return RecebedorResponse.builder()
                    .nome(NOME_RECEBEDOR)
                    .cpfCnpj(CPF)
                    .instituicao(INSTITUICAO)
                    .chave(CHAVE_PIX)
                    .agencia(AGENCIA)
                    .conta(CONTA)
                    .tipoChave(TipoChaveEnum.EMAIL)
                    .build();
        }

        public static RecebedorResponse criarRecebedorResponseDetalhesParcelaEsperado() {
            return RecebedorResponse.builder()
                    .nome(NOME_RECEBEDOR)
                    .cpfCnpj(CPF)
                    .instituicao(INSTITUICAO)
                    .chave(CHAVE_PIX)
                    .agencia(AGENCIA)
                    .conta(CONTA)
                    .tipoChave(TipoChaveEnum.EMAIL)
                    .build();
        }

        public static RecebedorResponse criarRecebedorResponseListagemRecorrenciaEsperado() {
            return RecebedorResponse.builder()
                    .nome(NOME_RECEBEDOR)
                    .build();
        }
    }
}