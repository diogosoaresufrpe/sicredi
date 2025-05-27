package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.spi.dto.Pain012Dto;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Titles.CPF_CNPJ;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.*;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.getNotificacaoDTO;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/solicitacao_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class RetornoBacenSolicitacaoAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String ID_RECORRENCIA = "RN2118152120250425041bYqAj6ef";
    private static final String ID_INFORMACOES_STATUS = "IS01181521202504250416YqAj6ef";
    private static final String ID_IDEMPOTENCIA = "IS01181521202504250416YqAj6ef_PAIN012_ENV";
    private static final String TOPICO_ICOM_PAIN_ENVIADO_V1 = "icom-pain-enviado-v1";
    private static final String TOPICO_ICOM_PAIN_ENVIADO_FALHA_V1 = "icom-pain-enviado-falha-v1";
    private static final String TOPICO_NOTIFICACAO = "spi-notificacao-recorrencia-v2";

    @Autowired
    private SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoPedidoAprovado_quandoProcessarRetornoSolicitacao_deveAtualizarStatusEEnviarPush() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.TRUE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_V1, "SUCESSO");

        validarRetornoSolicitacaoAprovado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA);
    }

    @Test
    void dadoPedidoRejeitado_quandoProcessarRetornoSolicitacao_deveAtualizarStatusENaoEnviarPush() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.FALSE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_V1, "SUCESSO");

        validarRetornoSolicitacaoRejeitado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA, REJEITADA, 1, 0);
    }

    @Test
    void dadoStatusEnvioDiferenteAprovada_quandoProcessarRetornoSolicitacao_deveAtualizarStatusENaoEnviarPush() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.FALSE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_ENVIADO_FALHA_V1, "REJEITADO");

        validarRetornoSolicitacaoRejeitado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA, CRIADA, 0, 0);
    }

    private void validarRetornoSolicitacaoRejeitado(String idIdempotencia, String idInformacaoStatus, String idRecorrencia, TipoStatusSolicitacaoAutorizacao status, int paramIdempotenteIn, int paramIdempotenteOutValor) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var optSolicitacao = solicitacaoRepository.findByIdInformacaoStatusAndIdRecorrencia(idInformacaoStatus, idRecorrencia);
            optSolicitacao.ifPresentOrElse(solicitacao ->
                            assertAll(
                                    () -> assertEquals(status, solicitacao.getTipoStatus())
                            ),
                    () -> fail("Solicitação não encontrada para os dados informados")

            );

            var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

            assertAll(
                    () -> assertEquals(paramIdempotenteIn, idempotenteIn.size()),
                    () -> assertEquals(paramIdempotenteOutValor, idempotenteOut.size())
            );

        });
    }

    private void validarRetornoSolicitacaoAprovado(String idIdempotencia, String idInformacaoStatus, String idRecorrencia) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var optSolicitacao = solicitacaoRepository.findByIdInformacaoStatusAndIdRecorrencia(idInformacaoStatus, idRecorrencia);
            optSolicitacao.ifPresentOrElse(solicitacao ->
                            assertAll(
                                    () -> assertEquals(PENDENTE_CONFIRMACAO, solicitacao.getTipoStatus())
                            ),
                    () -> fail("Solicitação não encontrada para os dados informados")

            );

            var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(1, idempotenteOut.size())
            );

            var notificacao = verificarMensagemNotificacao();

            assertAll(
                    () -> assertNotNull(notificacao),
                    () -> assertEquals(4, notificacao.getInformacoesAdicionais().size()),
                    () -> assertEquals("MOBI", notificacao.getCanal()),
                    () -> assertEquals("003039", notificacao.getConta()),
                    () -> assertEquals("0101", notificacao.getAgencia()),
                    () -> assertEquals(NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_PENDENTE_DE_APROVACAO, notificacao.getOperacao())

            );
        });
    }

    private NotificacaoDTO verificarMensagemNotificacao() {
        return getNotificacaoDTO(consumer, TOPICO_NOTIFICACAO);
    }

    private Pain012Dto criarRetornoSolicitacaoAutorizacao(String idInformacaoStatus, String idRecorrencia, Boolean status) {
        return Pain012Dto.builder()
                .status(status)
                .idInformacaoStatus(idInformacaoStatus)
                .idRecorrencia(idRecorrencia)
                .dataInicialRecorrencia(LocalDate.parse("2025-05-01"))
                .dataFinalRecorrencia(LocalDate.parse("2025-12-01"))
                .valor(BigDecimal.valueOf(20.75))
                .nomeUsuarioRecebedor("João da Silva")
                .cpfCnpjUsuarioRecebedor("12345678901")
                .participanteDoUsuarioRecebedor("341BANCO")
                .cpfCnpjUsuarioPagador(CPF_CNPJ)
                .contaUsuarioPagador("223190")
                .agenciaUsuarioPagador("0101")
                .participanteDoUsuarioPagador("00714671")
                .nomeDevedor("Empresa XYZ LTDA")
                .cpfCnpjDevedor("98765432000199")
                .numeroContrato("CT-2025-0001")
                .build();
    }

    private void enviarMensagem(Object payload, String topico, String statusEnvio) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, topico)
                .setHeader("TIPO_MENSAGEM", "PAIN012")
                .setHeader("OPERACAO", "RECORRENCIA_SOLICITACAO")
                .setHeader("PSP_EMISSOR", "PAGADOR")
                .setHeader("STATUS_ENVIO", statusEnvio)
                .setHeader("ID_IDEMPOTENCIA", ID_INFORMACOES_STATUS)
                .setHeader(KafkaHeaders.KEY, "RN9118152120250425041bYqAj6ef")
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_NOTIFICACAO);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);

        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        var consumerTest = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

}
