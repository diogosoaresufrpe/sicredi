package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.spi.entities.type.TipoFrequencia;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import io.sicredi.spirecorrencia.api.commons.DevedorResponse;
import io.sicredi.spirecorrencia.api.commons.PagadorResponse;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


class SolicitacaoAutorizacaoRecorrenciaResponseTest {

    private static final LocalDate DATA_PRIMEIRO_PAGAMENTO = LocalDate.now();
    private static final LocalDate DATA_ULTIMO_PAGAMENTO = LocalDate.now().plusMonths(1);
    private static final String ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO = "1";

    @Test
    void dadoSolicitacao_quandoFromDetalhe_deveRetornarSolicitacao() {
        var solicitacaoMock = TestFactory.SolicitacaoTestFactory.criarAutorizacao();
        solicitacaoMock.setIdSolicitacaoRecorrencia(ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO);

        var solicitacaoAutorizacaoRecorrenciaResponse = SolicitacaoAutorizacaoRecorrenciaResponse.fromDetalheSolicitacao(solicitacaoMock);

        assertThat(solicitacaoAutorizacaoRecorrenciaResponse)
                .usingRecursiveComparison()
                .isEqualTo(SolicitacaoAutorizacaoRecorrenciaResponseTest.ProviderTest.criarSolicitacaoAutorizacaoRecorrenciaEsperado());
    }

    private static final class ProviderTest {
        public static SolicitacaoAutorizacaoRecorrenciaResponse criarSolicitacaoAutorizacaoRecorrenciaEsperado() {
            var recebedor = RecebedorResponse.builder()
                    .cpfCnpj(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CPF_CNPJ)
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.NOME)
                    .build();
            var pagador = PagadorResponse.builder()
                    .cpfCnpj(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ)
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.NOME)
                    .build();
            var devedor = DevedorResponse.builder()
                    .cpfCnpj(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ)
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.NOME)
                    .build();

            return SolicitacaoAutorizacaoRecorrenciaResponse.builder()
                    .idSolicitacaoRecorrencia("1")
                    .idRecorrencia("d02aefe4-91be-404e-a5c6-7dff4e8b05cb")
                    .tpoFrequencia(TipoFrequencia.MENSAL)
                    .tpoStatus("ACEITA")
                    .valor(new BigDecimal(100))
                    .contrato("0123456789")
                    .descricao("descricao")
                    .pisoValorMaximo(new BigDecimal(90))
                    .dataInicialRecorrencia(DATA_PRIMEIRO_PAGAMENTO)
                    .dataFinalRecorrencia(DATA_ULTIMO_PAGAMENTO)
                    .pagador(pagador)
                    .devedor(devedor)
                    .recebedor(recebedor)
                    .build();
        }

    }
}
