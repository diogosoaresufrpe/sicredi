package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class ExclusaoRecorrenciaControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_EXCLUSAO_RECORRENCIA = "/v1/admin/recorrencia/exclusao";
    private static final String IDENTIFICADOR_RECORRENCIA_OID_1 = "2f335153-4d6a-4af1-92a0-c52c5c827af9";
    private static final String IDENTIFICADOR_PARCELA = "2DDF2A2BCF9FE8B2E063C50A17ACB3A4";
    private static final String TOPICO_EXCLUSAO_RETORNO = "spirecorrencia-gestao-api-excluidos-v1";

    @Autowired
    protected RecorrenciaRepository recorrenciaRepository;

    private static MockHttpServletRequestBuilder executarDelete(String idempotente, ExclusaoRequisicaoDTO exclusaoRequisicaoDTO) {
        return delete(URL_EXCLUSAO_RECORRENCIA)
                .queryParam("idempotente", idempotente)
                .content(ObjectMapperUtil.converterObjetoParaString(exclusaoRequisicaoDTO))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .accept(MediaType.APPLICATION_JSON);
    }

    @Test
    void dadoRecorrenciaValida_quandoExcluirRecorrencia_deveRetornarNoContent() throws Exception {
        var idRecorrencia = IDENTIFICADOR_RECORRENCIA_OID_1;
        var idempotente = UUID.randomUUID().toString();
        var exclusaoRecorrenciaProtocoloRequest = criarExclusaoRecorrenciaRequestDTO(List.of(IDENTIFICADOR_PARCELA), idRecorrencia);

        try (var consumer = configurarConsumer()) {
            var mockHttpServletRequestBuilder = executarDelete(idempotente, exclusaoRecorrenciaProtocoloRequest);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isNoContent());

            await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
                var optRecorrencia = recorrenciaRepository.findByIdRecorrencia(idRecorrencia);
                optRecorrencia.ifPresentOrElse(recorrencia ->
                                assertAll(
                                        () -> assertEquals(idRecorrencia, recorrencia.getIdRecorrencia()),
                                        () -> assertEquals(TipoStatusEnum.EXCLUIDO, recorrencia.getTipoStatus()),
                                        () -> assertEquals(TipoStatusEnum.EXCLUIDO, recorrencia.getRecorrencias().getFirst().getTpoStatus()),
                                        () -> assertNotNull(recorrencia.getRecorrencias().getFirst().getDataExclusao())
                                ),
                        () -> fail(String.format("Recorrencia com id %s não encontrado", idRecorrencia))
                );

                var idempotenteIn = buscarRegistrosIdempotenteIn(idempotente);
                var idempotenteOut = buscarRegistrosIdempotenteOut(idempotente);
                assertAll(
                        () -> assertEquals(1, idempotenteIn.size()),
                        () -> assertEquals(2, idempotenteOut.size())
                );

                var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 1);
                assertEquals(1, response.count(), "evento.protocolo");
            });
        }
    }

    private ExclusaoRequisicaoDTO criarExclusaoRecorrenciaRequestDTO(List<String> identificadoresParcelas, String idRecorrencia) {
        ExclusaoRequisicaoDTO exclusaoRecorrenciaProtocoloRequest = new ExclusaoRequisicaoDTO();
        exclusaoRecorrenciaProtocoloRequest.setIdentificadorRecorrencia(idRecorrencia);
        exclusaoRecorrenciaProtocoloRequest.setTipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_USUARIO);
        exclusaoRecorrenciaProtocoloRequest.setIdentificadoresParcelas(identificadoresParcelas);
        return exclusaoRecorrenciaProtocoloRequest;
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_EXCLUSAO_RETORNO);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }
}
