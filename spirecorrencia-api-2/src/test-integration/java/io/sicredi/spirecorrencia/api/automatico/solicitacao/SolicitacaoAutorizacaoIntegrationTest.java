package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.spi.dto.DetalheRecorrenciaPain009Dto;
import br.com.sicredi.spi.dto.Pain009Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spi.entities.type.TipoSituacaoPain009;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.sicredi.spirecorrencia.api.accountdata.DadosContaResponseDTO;
import io.sicredi.spirecorrencia.api.accountdata.DadosPessoaContaDTO;
import io.sicredi.spirecorrencia.api.accountdata.TipoContaEnum;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA;
import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.JA_CONFIRMADA_PREVIAMENTE_OU_STATUS_PENDENTE;
import static br.com.sicredi.spi.entities.type.StatusRecorrenciaPain012.PENDENTE_CONFIRMACAO;
import static br.com.sicredi.spi.entities.type.TipoSituacaoPain009.DATA_CRIACAO_SOLICITACAO_CONFIRMACAO;
import static br.com.sicredi.spi.entities.type.TipoSituacaoPain009.DATA_EXPIRACAO_SOLICITACAO_CONFIRMACAO;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/solicitacao_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class SolicitacaoAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPICO_ICOM_PAIN_RECEBIDO_V1 = "icom-pain-recebido-v1";
    private static final String TOPICO_ICOM_PAIN_ENVIO_V1 = "icom-pain-envio-v1";
    private static final String ID_RECORRENCIA = "RN2118152120250425041bYqAj6ef";
    private static final String ID_SOLICITACAO_AUTORIZACAO = "SC6118152120250425041bYqAj6ef";
    private static final String ID_IDEMPOTENCIA = "SC6118152120250425041bYqAj6ef_PAIN009_REC";
    private static final String PATH_CONSULTAR_DADOS_CONTA = "/accounts";
    private static final String PARAMS_CONSULTAR_DADOS_CONTA = "?document=12690422115&company=0101&number=223190&source=ALL";
    private static final String PATH_CONSULTAR_CODIGO_IBGE = "/gestent/v2/entidade-sicredi";
    private static final String PARAMS_CONSULTAR_CODIGO_IBGE = "?codigoTipoEntidade=AGENCIA&codigoAgencia=12&codigoCooperativa=0101&size=1&page=0";
    private static final String CAMINHO_CODIGO_IBGE = "__files/mocks/gestent-conector/codigo-ibge.json";
    private static final String DOCUMENTO = "12690422115";
    private static final String STATUS = "ACTIVE";

    @Autowired
    private SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoSolicitacaoValida_quandoConsumirMensagem_deveAprovarSolicitacao() {
        var pain009 = criarPain009(ID_RECORRENCIA, ID_SOLICITACAO_AUTORIZACAO);
        var mensagemExclusaoTitular = FileTestUtils.asString(new ClassPathResource(CAMINHO_CODIGO_IBGE));

        criarStubMockResponse(PATH_CONSULTAR_DADOS_CONTA + PARAMS_CONSULTAR_DADOS_CONTA, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(List.of(criarDadosContaPagador(DOCUMENTO, STATUS))));
        criarStubMockResponse(PATH_CONSULTAR_CODIGO_IBGE + PARAMS_CONSULTAR_CODIGO_IBGE, HttpMethod.GET, HttpStatus.OK, mensagemExclusaoTitular);

        enviarMensagem(pain009, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarSolicitacaoAprovada(ID_IDEMPOTENCIA, ID_RECORRENCIA, ID_SOLICITACAO_AUTORIZACAO, "123456");
    }

    @Test
    void dadoNotFoundAoConsultarCodigoIbge_quandoConsumirMensagem_deveAprovarSolicitacaoENaoSalvarCodigo() {
        var pain009 = criarPain009(ID_RECORRENCIA, ID_SOLICITACAO_AUTORIZACAO);
        var mensagemExclusaoTitular = FileTestUtils.asString(new ClassPathResource(CAMINHO_CODIGO_IBGE));

        criarStubMockResponse(PATH_CONSULTAR_DADOS_CONTA + PARAMS_CONSULTAR_DADOS_CONTA, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(List.of(criarDadosContaPagador(DOCUMENTO, STATUS))));
        criarStubMockResponse(PATH_CONSULTAR_CODIGO_IBGE + PARAMS_CONSULTAR_CODIGO_IBGE, HttpMethod.GET, HttpStatus.NOT_FOUND, mensagemExclusaoTitular);

        enviarMensagem(pain009, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarSolicitacaoAprovada(ID_IDEMPOTENCIA, ID_RECORRENCIA, ID_SOLICITACAO_AUTORIZACAO, null);
    }

    @Test
    void dadoSolicitacaoExistenteComStatusAceitaOuPendenteConfirmacao_quandoConsumirMensagem_deveReprovarSolicitacao() {
        var pain009 = criarPain009("RN0118152120250425041bYqAj6ef", ID_SOLICITACAO_AUTORIZACAO);
        var mensagemExclusaoTitular = FileTestUtils.asString(new ClassPathResource(CAMINHO_CODIGO_IBGE));

        criarStubMockResponse(PATH_CONSULTAR_DADOS_CONTA + PARAMS_CONSULTAR_DADOS_CONTA, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(List.of(criarDadosContaPagador(DOCUMENTO, STATUS))));
        criarStubMockResponse(PATH_CONSULTAR_CODIGO_IBGE + PARAMS_CONSULTAR_CODIGO_IBGE, HttpMethod.GET, HttpStatus.OK, mensagemExclusaoTitular);

        enviarMensagem(pain009, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarSolicitacaoReprovada(ID_IDEMPOTENCIA, "RN0118152120250425041bYqAj6ef", ID_SOLICITACAO_AUTORIZACAO, JA_CONFIRMADA_PREVIAMENTE_OU_STATUS_PENDENTE);
    }

    @Test
    void dadoContaNaoEncontrada_quandoConsumirMensagem_deveReprovarSolicitacao() {
        var pain009 = criarPain009(ID_RECORRENCIA, ID_SOLICITACAO_AUTORIZACAO);
        var mensagemExclusaoTitular = FileTestUtils.asString(new ClassPathResource(CAMINHO_CODIGO_IBGE));

        criarStubMockResponse(PATH_CONSULTAR_DADOS_CONTA + PARAMS_CONSULTAR_DADOS_CONTA, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(List.of()));
        criarStubMockResponse(PATH_CONSULTAR_CODIGO_IBGE + PARAMS_CONSULTAR_CODIGO_IBGE, HttpMethod.GET, HttpStatus.OK, mensagemExclusaoTitular);

        enviarMensagem(pain009, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarSolicitacaoReprovada(ID_IDEMPOTENCIA, ID_RECORRENCIA, ID_SOLICITACAO_AUTORIZACAO, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA);
    }

    @Test
    void dadoTimeOutAoConsultarConta_quandoConsumirMensagem_deveLancarExececaoENaoCriarSolicitacao() {
        var pain009 = criarPain009(ID_RECORRENCIA, ID_SOLICITACAO_AUTORIZACAO);

        wireMockServer.stubFor(WireMock.get(WireMock.urlMatching(PATH_CONSULTAR_DADOS_CONTA + ".*"))
                .willReturn(WireMock.aResponse()
                        .withFixedDelay(30_000) // Força timeout
                        .withStatus(wiremock.org.eclipse.jetty.http.HttpStatus.OK_200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ \"resultado\": \"ok\" }")));


        enviarMensagem(pain009, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        var optSolicitacao = solicitacaoRepository.findById(ID_SOLICITACAO_AUTORIZACAO);

        assertFalse(optSolicitacao.isPresent());
    }

    private void validarSolicitacaoReprovada(String idIdempotencia, String idRecorrencia, String idSolicitacaoAutorizacao, MotivoRejeicaoPain012 motivoErro) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var optSolicitacao = solicitacaoRepository.findById(idSolicitacaoAutorizacao);
            optSolicitacao.ifPresentOrElse(solicitacao ->
                            assertAll(
                                    () -> assertEquals(motivoErro.name(), solicitacao.getMotivoRejeicao()),
                                    () -> assertEquals(idRecorrencia, solicitacao.getIdRecorrencia()),
                                    () -> assertEquals(idSolicitacaoAutorizacao, solicitacao.getIdSolicitacaoRecorrencia()),
                                    () -> assertEquals(TipoStatusSolicitacaoAutorizacao.CRIADA, solicitacao.getTipoStatus())
                            ),
                    () -> fail(String.format("Solicitação não encontrada com id %s não encontrado", idSolicitacaoAutorizacao))

            );
            var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(1, idempotenteOut.size())
            );

            var pain012 = verificarMensagemPain012();

            assertAll(
                    () -> assertEquals(motivoErro.name(), pain012.getMotivoRejeicao()),
                    () -> assertFalse(pain012.getStatus()),
                    () -> assertNull(pain012.getStatusRecorrencia()),
                    () -> assertNull(pain012.getDetalhesRecorrencias())
            );
        });
    }

    private void validarSolicitacaoAprovada(String idIdempotencia, String idRecorrencia, String idSolicitacao, String codigoIbge) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var optSolicitacao = solicitacaoRepository.findById(idSolicitacao);
            optSolicitacao.ifPresentOrElse(solicitacao ->
                            assertAll(
                                    () -> assertNull(solicitacao.getMotivoRejeicao()),
                                    () -> assertEquals(idRecorrencia, solicitacao.getIdRecorrencia()),
                                    () -> assertEquals(idSolicitacao, solicitacao.getIdSolicitacaoRecorrencia()),
                                    () -> assertEquals(TipoStatusSolicitacaoAutorizacao.CRIADA, solicitacao.getTipoStatus()),
                                    () -> assertEquals(codigoIbge, solicitacao.getCodigoMunicipioIBGE())
                            ),
                    () -> fail(String.format("Solicitação não encontrada com id %s não encontrado", idSolicitacao))

            );

            var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);
            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(1, idempotenteOut.size())
            );

            var pain012 = verificarMensagemPain012();

            assertAll(
                    () -> assertNull(pain012.getMotivoRejeicao()),
                    () -> assertTrue(pain012.getStatus()),
                    () -> assertEquals(PENDENTE_CONFIRMACAO.name(), pain012.getStatusRecorrencia()),
                    () -> assertEquals(codigoIbge, pain012.getCodMunIBGE()),
                    () -> assertEquals(2, pain012.getDetalhesRecorrencias().size())
            );
        });
    }

    private Pain009Dto criarPain009(String idRecorrencia, String idSolicitacao) {
        return Pain009Dto.builder()
                .idRecorrencia(idRecorrencia)
                .idSolicitacaoRecorrencia(idSolicitacao)
                .tipoRecorrencia("RECORRENTE")
                .tipoFrequencia("SEMANAL")
                .dataInicialRecorrencia(LocalDate.parse("2025-05-01"))
                .dataFinalRecorrencia(LocalDate.parse("2025-12-01"))
                .indicadorObrigatorio(true)
                .valor(BigDecimal.valueOf(20.75))
                .indicadorPisoValorMaximo(false)
                .pisoValorMaximo(BigDecimal.valueOf(20.00))
                .nomeUsuarioRecebedor("João da Silva")
                .cpfCnpjUsuarioRecebedor("12345678901")
                .participanteDoUsuarioRecebedor("341BANCO")
                .cpfCnpjUsuarioPagador(DOCUMENTO)
                .contaUsuarioPagador("223190")
                .agenciaUsuarioPagador("0101")
                .participanteDoUsuarioPagador("00714671")
                .nomeDevedor("Empresa XYZ LTDA")
                .cpfCnpjDevedor("98765432000199")
                .numeroContrato("CT-2025-0001")
                .descricao("teste")
                .detalhesRecorrenciaPain009Dto(List.of(
                        new DetalheRecorrenciaPain009Dto(DATA_CRIACAO_SOLICITACAO_CONFIRMACAO.name(), LocalDateTime.now()),
                        new DetalheRecorrenciaPain009Dto(TipoSituacaoPain009.DATA_CRIACAO_RECORRENCIA.name(), LocalDateTime.now().plusDays(1)),
                        new DetalheRecorrenciaPain009Dto(DATA_EXPIRACAO_SOLICITACAO_CONFIRMACAO.name(), LocalDateTime.now().plusMonths(1))
                ))
                .build();
    }

    private Pain012Dto verificarMensagemPain012() {
        try {
            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 1);

            for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> iteration : response.records(TOPICO_ICOM_PAIN_ENVIO_V1)) {
                if(iteration.value() != null) {
                    return ObjectMapperUtil.converterStringParaObjeto(iteration.value(), new TypeReference<Pain012Dto>() {
                    });
                }
            }
            return new Pain012Dto();
        } catch (Exception ignore) {
            return new Pain012Dto();
        }
    }

    private void enviarMensagem(Object payload, String topico) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, topico)
                .setHeader("TIPO_MENSAGEM", "PAIN009")
                .setHeader(KafkaHeaders.KEY, "RN9118152120250425041bYqAj6ef")
                .setHeader("ID_IDEMPOTENCIA", ID_SOLICITACAO_AUTORIZACAO)
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_ICOM_PAIN_ENVIO_V1);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        var consumerTest = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

    private DadosContaResponseDTO criarDadosContaPagador(String documento, String status) {
        return DadosContaResponseDTO.builder()
                .posto("12")
                .coop("0101")
                .status(status)
                .numeroConta("223190")
                .sistema("DIGITAL")
                .temCreditoBloqueado(false)
                .titular(DadosPessoaContaDTO.builder().documento(documento).build())
                .tipoConta(TipoContaEnum.CHECKING_ACCOUNT)
                .build();
    }

}
