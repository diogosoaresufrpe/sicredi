package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.canaisdigitais.dto.seguranca.DispositivoAutenticacaoDTO;
import br.com.sicredi.canaisdigitais.enums.ClasseDispositivoSegurancaEnum;
import br.com.sicredi.spi.dto.TransacaoDto;
import br.com.sicredi.spi.entities.type.OrdemStatus;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaProtocoloRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.FileTestUtils;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("squid:S5960")
class RetornoPagamentoConsumerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Deve emitir protocolo de recorrência no consumo do tópico de retorno de transação para PAGAMENTO_COM_RECORRENCIA")
    void dadoRetornoTransacaoConcluidaPagamentoComRecorrencia_quandoConsumirMensagem_deveEmitirProtocoloDeRecorrencia() {
        var idFimAFim = SpiUtil.gerarIdFimAFim(TipoId.PAGAMENTO, ISPB);
        var transacaoDto = TransacaoDto.builder()
                .idFimAFim(idFimAFim)
                .status(OrdemStatus.CONCLUIDO)
                .build();
        var urlConsultaProtocolo = URL_CONSULTA_PROTOCOLO
                .replace(PATH_CODIGO_TIPO_TRANSACAO, "358")
                .replace(PATH_IDENTIFICADOR_TRANSACAO, idFimAFim);
        var responseConsultaProtocolo = FileTestUtils.asString(new ClassPathResource("__files/mocks/canais-digitais-protocolo-info-internal-api/retorno-sucesso-protocolo.json"))
                .replace(PATH_IDENTIFICADOR_TRANSACAO, idFimAFim);

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.OK, responseConsultaProtocolo);
        criarStubMockResponse(PATH_RECORRENCIA_CADASTRO, HttpMethod.POST, HttpStatus.CREATED, "");

        enviarMensagem(transacaoDto, TOPICO_RETORNO_TRANSACAO, Map.of(
                "tipoProduto", "PAGAMENTO_COM_RECORRENCIA",
                "idFimAFim", idFimAFim
        ));

        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(urlConsultaProtocolo)));

            var requests = wireMockServer.findAll(postRequestedFor(urlPathEqualTo(PATH_RECORRENCIA_CADASTRO)));
            assertEquals(1, requests.size());
            var cadastroRequest = ObjectMapperUtil.converterStringParaObjeto(new String(requests.getFirst().getBody(), StandardCharsets.UTF_8), new TypeReference<CadastroRecorrenciaProtocoloRequest>() {
            });

            assertEquals(2, cadastroRequest.getParcelas().size());
            assertEquals(TipoRecorrencia.AGENDADO_RECORRENTE, cadastroRequest.getTipoRecorrencia());
            assertNotNull(cadastroRequest.getDispositivoAutenticacao());
            var dispositivoAutenticacao = new DispositivoAutenticacaoDTO();
            dispositivoAutenticacao.setClasse(ClasseDispositivoSegurancaEnum.ASSINATURA_ELETRONICA);
            dispositivoAutenticacao.setOidDispositivo("10281272");
            dispositivoAutenticacao.setQrCode(false);
            assertEquals(dispositivoAutenticacao, cadastroRequest.getDispositivoAutenticacao());
        });
    }

    @Test
    @DisplayName("Deve tentar novamente se consulta de protocolo de pagamento não for encontrada no fluxo de retorno de transação")
    void dadoRetornoTransacaoConcluidaPagamentoComRecorrenciaEProtocoloNaoEncontrado_quandoConsumirMensagem_deveFazerRetrySemEmitirProtocoloDeRecorrencia() {
        var idFimAFim = SpiUtil.gerarIdFimAFim(TipoId.PAGAMENTO, ISPB);
        var transacaoDto = TransacaoDto.builder()
                .idFimAFim(idFimAFim)
                .status(OrdemStatus.CONCLUIDO)
                .build();
        var headers = Map.of(
                "tipoProduto", "PAGAMENTO_COM_RECORRENCIA",
                "idFimAFim", idFimAFim
        );
        var urlConsultaProtocolo = URL_CONSULTA_PROTOCOLO
                .replace(PATH_CODIGO_TIPO_TRANSACAO, "358")
                .replace(PATH_IDENTIFICADOR_TRANSACAO, idFimAFim);

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.OK, null);

        enviarMensagem(transacaoDto, TOPICO_RETORNO_TRANSACAO, headers);

        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMockServer.verify(3, getRequestedFor(urlPathEqualTo(urlConsultaProtocolo)));
            wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(PATH_RECORRENCIA_CADASTRO)));
        });
    }

}
