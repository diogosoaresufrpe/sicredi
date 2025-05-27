package io.sicredi.spirecorrencia.api.exclusao;

import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.FileTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.PATH_EMITIR_CANCELAMENTO;
import static org.awaitility.Awaitility.await;

@SuppressWarnings("squid:S5960")
@SqlGroup(value = {
        @Sql(scripts = {"/db/clear.sql", "/db/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
        @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
})
final class ExclusaoTitularidadeContaConsumerIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPICO_HOLDERS_MAINTENANCE_V1 = "holders-maintenance-v1";

    @Test
    @Sql("/db/recorrencia_com_parcelas_em_todos_status.sql")
    void dadoTitularExcluidoComParcelasCriadas_quandoConsumirHolderMaitenanceV1_deveExcluirParcelasAgendadasPeloTitular() {
        var mensagemExclusaoTitular = FileTestUtils.asString(new ClassPathResource("__files/mocks/holders-maintenance/exclusao_titular.json"))
                .replace("{{cooperativa}}", "0101")
                .replace("{{conta}}", "000023")
                .replace("{{cpfCnpjTitularRemovido}}", "28574109053")
                .replace("{{customerIdTitularRemovido}}", "8ecd1c7a-ce04-4c69-a9d6-1499be4c8d5c")
                .replace("{{cpfCnpjTitularAtual}}", "24110287000170")
                .replace("{{nomeTitularAtual}}", "Adelia Maria Moschem Colorio");
        criarStubMockResponse(PATH_EMITIR_CANCELAMENTO, HttpMethod.POST, HttpStatus.OK, "");

        kafkaTemplate.send(TOPICO_HOLDERS_MAINTENANCE_V1, mensagemExclusaoTitular);

        await().atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(PATH_EMITIR_CANCELAMENTO))
                        .withRequestBody(matchingJsonPath("$.parcelas[0].valor", equalTo("10.0")))
                        .withRequestBody(matchingJsonPath("$.parcelas[1].valor", equalTo("10.0")))
                        .withRequestBody(matchingJsonPath("$.parcelas[?(@.identificadorParcela == '7a9ea699-28fe-47cf-bc7a-caa9a26c8602')]"))
                        .withRequestBody(matchingJsonPath("$.parcelas[?(@.identificadorParcela == '03915772-9f06-4738-9a91-24432db7df4f')]"))
                        .withRequestBody(matchingJsonPath("$.parcelas[2]", absent()))
                ));
    }

}
