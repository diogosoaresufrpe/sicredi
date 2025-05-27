package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoPixEnum;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.sicredi.spirecorrencia.api.repositorio.Recebedor;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import wiremock.org.eclipse.jetty.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

public class LiquidacaoServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LiquidacaoService service;

    @Test
    void dadoTimeoutNaConsultaAoDict_quandoConsultarTipoProcessamento_deveRetornarTransacaoIgnorada() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlMatching("/v2/chaves/.*"))
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(30_000) // Força timeout
                        .withStatus(HttpStatus.OK_200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"resultado\": \"ok\" }")));

        var recorrenciaTransacao = RecorrenciaTransacao.builder()
                .recorrencia(Recorrencia.builder()
                        .tipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_BY_KEY)
                        .tipoCanal(TipoCanalEnum.MOBI)
                        .pagador(Pagador.builder().oidPagador(1L).build())
                        .recebedor(Recebedor.builder().oidRecebedor(1L).chave("pix@bcb.gov.br").tipoChave(TipoChaveEnum.EMAIL).build())
                        .build())
                .build();

        var tipoProcessamentoWrapperDTO = service.consultarTipoProcessamento("8509a5f5-1356-461f-b264-5289f6a61487", recorrenciaTransacao);

        assertThat(tipoProcessamentoWrapperDTO)
                .usingRecursiveComparison()
                .isEqualTo(TipoProcessamentoWrapperDTO.builder()
                        .identificadorTransacao("8509a5f5-1356-461f-b264-5289f6a61487")
                        .tipoProcessamentoEnum(TipoProcessamentoEnum.IGNORADA)
                        .recorrenciaTransacao(recorrenciaTransacao)
                        .tipoProcessamentoErro(TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                                .codigoErro(AppExceptionCode.REC_PROC_BU0003.getCode())
                                .mensagemErro("Read timed out executing GET http://localhost:8082/v2/chaves/pix%40bcb.gov.br?incluirEstatisticas=false")
                                .build())
                        .build());
    }

}
