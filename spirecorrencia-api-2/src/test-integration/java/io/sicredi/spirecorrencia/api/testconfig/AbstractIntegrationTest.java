package io.sicredi.spirecorrencia.api.testconfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.CollectionUtils;
import wiremock.com.google.common.collect.Iterables;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {
        "spi-notificacao-recorrencia-v2",
        "icom-pain-envio-v1",
        "canaisdigitais-protocolo-comando-v1",
        "automatico-recorrente-autorizacao-cancelamento-protocolo-v1",
        "icom-camt-envio-v1"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    private static final int MILLIS = 20000;
    private static final String SELECT_IDEMPOTENT_TRANSACTION = "SELECT * FROM SPI_OWNER.IDEMPOTENT_TRANSACTION_RECORRENCIA";
    private static final String SELECT_IDEMPOTENT_TRANSACTION_OUTBOX = "SELECT * FROM SPI_OWNER.IDEMPOTENT_TRANSACTION_OUTBOX_RECORRENCIA";

    public final WireMockServer wireMockServer = new WireMockServer(8082);

    @Autowired
    protected KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    public MockMvc mockMvc;
    @Autowired
    public AppConfig appConfig;
    @Autowired
    public EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    protected EntityManager entityManager;

    @BeforeAll
    void beforeAll() {
        wireMockServer.start();
    }

    @AfterAll
    void afterAll() {
        wireMockServer.stop();
    }

    @BeforeEach
    void beforeEach() {
        wireMockServer.resetAll();
    }

    public void criarStubMockResponse(String url, HttpMethod httpMethod, HttpStatus statusResponse, String bodyResponse) {
        var wireMockResponseMock = prepararStubMockResponse(statusResponse.value()).withBody(bodyResponse);
        concluirStubMockResponse(url, httpMethod, wireMockResponseMock);
    }

    public List<Object[]> listarTodosBloqueios() {
        String sql = "SELECT * FROM SPI_OWNER.SHEDLOCK";
        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    private ResponseDefinitionBuilder prepararStubMockResponse(int status) {
        return WireMock
                .aResponse()
                .withStatus(status)
                .withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    private void concluirStubMockResponse(String url, HttpMethod httpMethod,
                                          ResponseDefinitionBuilder wireMockResponseMock) {

        var wireMockUrl = WireMock.urlEqualTo(url);
        var wireMockMappingBuilder = criarMappingBuilder(wireMockUrl, httpMethod, wireMockResponseMock);
        wireMockServer.stubFor(wireMockMappingBuilder);
    }

    private MappingBuilder criarMappingBuilder(UrlPattern wireMockUrl, HttpMethod httpMethod,
                                               ResponseDefinitionBuilder wireMockResponseMock) {

        return switch (httpMethod.name()) {
            case "GET" -> WireMock.get(wireMockUrl).willReturn(wireMockResponseMock);
            case "POST" -> WireMock.post(wireMockUrl).willReturn(wireMockResponseMock);
            case "PUT" -> WireMock.put(wireMockUrl).willReturn(wireMockResponseMock);
            case "DELETE" -> WireMock.delete(wireMockUrl).willReturn(wireMockResponseMock);
            default -> throw new IllegalArgumentException("Não há implementação para o method");
        };
    }

    public Consumer<String, String> configurarConsumer(String autoOffsetReset, Boolean autoCommit, String... topicos) {
        Consumer<String, String> consumer = configurarConsumerFactory(autoOffsetReset, autoCommit.toString()).createConsumer();

        embeddedKafkaBroker.consumeFromEmbeddedTopics(consumer, topicos);

        return consumer;
    }

    public ConsumerRecord<String, String> getLastRecord(
            Consumer<String, String> consumer,
            String topic
    ) {
        return Iterables.getLast(KafkaTestUtils.getRecords(consumer, Duration.ofMillis(MILLIS)).records(topic));
    }

    private ConsumerFactory<String, String> configurarConsumerFactory(String autoOffsetReset, String autoCommit) {
        var consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), autoCommit, embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        return new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new StringDeserializer()
        );
    }

    public List<Map<String, Object>> buscarRegistrosIdempotenteIn(String id) {
        List<Map<String, Object>> resultList = entityManager.createNativeQuery(SELECT_IDEMPOTENT_TRANSACTION, Map.class).getResultList();
        return resultList.stream().filter(e -> id.equals(e.get("id"))).toList();
    }

    public List<Map<String, Object>> buscarRegistrosIdempotenteOut(String id) {
        List<Map<String, Object>> resultList = entityManager.createNativeQuery(SELECT_IDEMPOTENT_TRANSACTION_OUTBOX, Map.class).getResultList();
        return resultList.stream().filter(e -> id.equals(e.get("transaction_id"))).toList();
    }

    public <T> void enviarMensagem(T payload, String topico, Map<String, String> headers) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var messageBuilder = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, topico);

        Optional.ofNullable(headers)
                .filter(Predicate.not(CollectionUtils::isEmpty))
                .ifPresent(map -> map.forEach(messageBuilder::setHeader));

        var mensagem = messageBuilder.build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

}
