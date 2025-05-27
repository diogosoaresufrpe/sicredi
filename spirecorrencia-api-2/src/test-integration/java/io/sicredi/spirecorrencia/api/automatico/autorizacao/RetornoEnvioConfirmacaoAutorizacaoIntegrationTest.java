package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.spi.dto.Pain012Dto;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSubStatus;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.FileTestUtils;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Titles.CPF_CNPJ;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.CONFIRMADA;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/solicitacao_autorizacao.sql", "/db/autorizacao_recorrencia.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class RetornoEnvioConfirmacaoAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String MOCK_RETORNO_PROTOCOLO = "__files/mocks/canais-digitais-protocolo-info-internal-api/retorno-sucesso-confirmacao-autorizacao-protocolo.json";
    private static final String TOPICO_PROTOCOLO = "canaisdigitais-protocolo-comando-v1";
    private static final String RECURSO_NAO_ENCONTRADO = "Solicitação não encontrada para os dados informados";
    private static final String ID_RECORRENCIA = "RN0118152120250425041bYqAj6ef";
    private static final String ID_RECORRENCIA_JORNADA_2 = "RN3118152120250425041bYqAj6ef";
    private static final String ID_RECORRENCIA_JORNADA_3 = "RN5118152120250425041bYqAj6ef";
    private static final String ID_INFORMACOES_STATUS = "IS1218152120250425041bYqAj6ef";
    private static final String ID_IDEMPOTENCIA = "IS1218152120250425041bYqAj6ef_PAIN012_ENV";
    private static final String ID_INFORMACOES_STATUS_JORNADA_2 = "IS3218152120250425041bYqAj6ef";
    private static final String ID_INFORMACOES_STATUS_JORNADA_3 = "IS4218152120250425041bYqAj6ef";
    private static final String ID_IDEMPOTENCIA_JORNADA_2 = "IS3218152120250425041bYqAj6ef_PAIN012_ENV";
    private static final String ID_IDEMPOTENCIA_JORNADA_3 = "IS4218152120250425041bYqAj6ef_PAIN012_ENV";
    private static final String TOPICO_ICOM_PAIN_ENVIADO_V1 = "icom-pain-enviado-v1";
    private static final String CONTA = "223190";
    private static final String SUCESSO = "SUCESSO";

    private Consumer<String, String> consumer;

    @Autowired
    private SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;

    @Autowired
    private RecorrenciaAutorizacaoRepository autorizacaoRepository;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoConfirmacaoAprovadaEJornada1_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.TRUE);

        var urlConsultaProtocolo = URL_CONSULTA_PROTOCOLO
                .replace(PATH_CODIGO_TIPO_TRANSACAO, "438")
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS);
        var responseConsultaProtocolo = FileTestUtils.asString(new ClassPathResource(MOCK_RETORNO_PROTOCOLO))
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS);

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.OK, responseConsultaProtocolo);
        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_V1, SUCESSO, ID_INFORMACOES_STATUS);

        validarRetornoSolicitacaoAprovado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, true);
    }

    @Test
    void dadoConfirmacaoAprovadaEJornada2_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS_JORNADA_2, ID_RECORRENCIA_JORNADA_2, Boolean.TRUE);

        var urlConsultaProtocolo = URL_CONSULTA_PROTOCOLO
                .replace(PATH_CODIGO_TIPO_TRANSACAO, "439")
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS_JORNADA_2);
        var responseConsultaProtocolo = FileTestUtils.asString(new ClassPathResource(MOCK_RETORNO_PROTOCOLO))
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS_JORNADA_2);

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.OK, responseConsultaProtocolo);
        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_V1, SUCESSO, ID_INFORMACOES_STATUS_JORNADA_2);

        validarRetornoSolicitacaoAprovado(ID_IDEMPOTENCIA_JORNADA_2, ID_INFORMACOES_STATUS_JORNADA_2, false);
    }

    @Test
    void dadoConfirmacaoAprovadaEJornada3_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS_JORNADA_3, ID_RECORRENCIA_JORNADA_3, Boolean.TRUE);

        var urlConsultaProtocolo = URL_CONSULTA_PROTOCOLO
                .replace(PATH_CODIGO_TIPO_TRANSACAO, "440")
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS_JORNADA_3);
        var responseConsultaProtocolo = FileTestUtils.asString(new ClassPathResource(MOCK_RETORNO_PROTOCOLO))
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS_JORNADA_3);

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.OK, responseConsultaProtocolo);
        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_V1, SUCESSO, ID_INFORMACOES_STATUS_JORNADA_3);

        validarRetornoSolicitacaoAprovado(ID_IDEMPOTENCIA_JORNADA_3, ID_INFORMACOES_STATUS_JORNADA_3, false);
    }


    @Test
    void dadoFalseNoStatusEnvio_quandoProcessarRespostaConfirmacaoAutorizacao_deveReceberENaoFazerNada() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA_JORNADA_2, Boolean.TRUE);

        var urlConsultaProtocolo = URL_CONSULTA_PROTOCOLO
                .replace(PATH_CODIGO_TIPO_TRANSACAO, "438")
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_V1, "FALHA", ID_INFORMACOES_STATUS);

        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(urlConsultaProtocolo))));
    }

    @Test
    void dadoTimeoutAoConsultarProtocolo_quandoProcessarRespostaConfirmacaoAutorizacao_deveTentarRealizarRetry() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA_JORNADA_2, Boolean.TRUE);

        var urlConsultaProtocolo = URL_CONSULTA_PROTOCOLO
                .replace(PATH_CODIGO_TIPO_TRANSACAO, "438")
                .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_INFORMACOES_STATUS);

        wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(urlConsultaProtocolo))
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(30_000) // Força timeout
                        .withStatus(wiremock.org.eclipse.jetty.http.HttpStatus.OK_200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"resultado\": \"ok\" }")));

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_V1, SUCESSO, ID_INFORMACOES_STATUS);

        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMockServer.verify(3, getRequestedFor(urlPathEqualTo(urlConsultaProtocolo)));
            wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(PATH_RECORRENCIA_CADASTRO)));
        });
    }

    private void validarRetornoSolicitacaoAprovado(String idIdempotencia, String idInformacaoStatus, boolean jornada1) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var optAutorizacao = autorizacaoRepository.findByIdInformacaoStatusEnvio(idInformacaoStatus);

            optAutorizacao.ifPresentOrElse(solicitacao ->
                            assertAll(
                                    () -> assertEquals(TipoSubStatus.AGUARDANDO_RETORNO.name(), solicitacao.getTipoSubStatus())
                            ),
                    () -> fail(RECURSO_NAO_ENCONTRADO)

            );


            if(jornada1) {
                var optSolicitacao = solicitacaoRepository.findById("SC8118152120250425041bYqAj6ef");

                optSolicitacao.ifPresentOrElse(solicitacao ->
                                assertAll(
                                        () -> assertEquals(TipoSubStatus.AGUARDANDO_RETORNO.name(), solicitacao.getTipoSubStatus()),
                                        () -> assertEquals(CONFIRMADA, solicitacao.getTipoStatus())
                                ),
                        () -> fail(RECURSO_NAO_ENCONTRADO)
                );
            } else {
                var optSolicitacao = solicitacaoRepository.findById("SC7118152120250425041bYqAj6ef");

                optSolicitacao.ifPresentOrElse(solicitacao ->
                                assertAll(
                                        () -> assertEquals(TipoSubStatus.AGUARDANDO_ENVIO.name(), solicitacao.getTipoSubStatus()),
                                        () -> assertEquals(CONFIRMADA, solicitacao.getTipoStatus())
                                ),
                        () -> fail(RECURSO_NAO_ENCONTRADO));
            }

            var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(1, idempotenteOut.size())
            );

            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 1);
            assertEquals(1, response.count());
        });
    }

    private Pain012Dto criarRetornoSolicitacaoAutorizacao(String idInformacaoStatus, String idRecorrencia, Boolean status) {
        return Pain012Dto.builder()
                .status(status)
                .motivoRejeicao("AP01")
                .statusRecorrencia("CFDB")
                .idInformacaoStatus(idInformacaoStatus)
                .idRecorrencia(idRecorrencia)
                .dataInicialRecorrencia(LocalDate.parse("2025-05-01"))
                .dataFinalRecorrencia(LocalDate.parse("2025-12-01"))
                .valor(BigDecimal.valueOf(20.75))
                .nomeUsuarioRecebedor("João da Silva")
                .cpfCnpjUsuarioRecebedor("12345678901")
                .participanteDoUsuarioRecebedor("341BANCO")
                .cpfCnpjUsuarioPagador(CPF_CNPJ)
                .contaUsuarioPagador(CONTA)
                .agenciaUsuarioPagador("0101")
                .participanteDoUsuarioPagador("00714671")
                .nomeDevedor("Empresa XYZ LTDA")
                .cpfCnpjDevedor("98765432000199")
                .numeroContrato("CT-2025-0001")
                .build();
    }

    private void enviarMensagem(Object payload, String topico,String statusEnvio, String idInformacaoStatus) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, topico)
                .setHeader("TIPO_MENSAGEM", "PAIN012")
                .setHeader("ID_IDEMPOTENCIA", idInformacaoStatus)
                .setHeader("STATUS_ENVIO", statusEnvio)
                .setHeader("OPERACAO", "RECORRENCIA_AUTORIZACAO")
                .setHeader("PSP_EMISSOR", "PAGADOR")
                .setHeader(KafkaHeaders.KEY, "RN9118152120250425041bYqAj6ef")
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_PROTOCOLO);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);

        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        var consumerTest = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

}
