package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.spi.dto.Pain012Dto;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
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

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.DIVERGENCIA_ENTRE_DADOS_RECORRENCIA_E_O_STATUS_RECORRENCIA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Titles.CPF_CNPJ;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.ACEITA;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.getNotificacaoDTO;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/solicitacao_autorizacao.sql", "/db/autorizacao_recorrencia.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class RetornoBacenConfirmacaoAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String RECURSO_NAO_ENCONTRADO = "Solicitação não encontrada para os dados informados";
    private static final String ID_RECORRENCIA = "RN0118152120250425041bYqAj6ef";
    private static final String ID_RECORRENCIA_JORNADA_2 = "RN3118152120250425041bYqAj6ef";
    private static final String ID_INFORMACOES_STATUS = "IS1218152120250425041bYqAj6ef";
    private static final String ID_IDEMPOTENCIA = "IS1218152120250425041bYqAj6ef_PAIN012_REC";
    private static final String TOPICO_ICOM_PAIN_RECEBIDO_V1 = "icom-pain-recebido-v1";
    private static final String TOPICO_NOTIFICACAO = "spi-notificacao-recorrencia-v2";
    private static final String CONTA = "223190";

    @Autowired
    private SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;

    @Autowired
    private RecorrenciaAutorizacaoRepository autorizacaoRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoConfirmacaoAprovadaEJornada1_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.TRUE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarRetornoSolicitacaoAprovado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA, true);
    }

    @Test
    void dadoConfirmacaoRejeitadaEJornada1_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.FALSE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarRetornoSolicitacaoRejeitado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA, true);
    }

    @Test
    void dadoConfirmacaoAprovadaEJornada2_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA_JORNADA_2, Boolean.TRUE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarRetornoSolicitacaoAprovado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA_JORNADA_2, false);
    }

    @Test
    void dadoConfirmacaoRejeitadaEJornada2_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA_JORNADA_2, Boolean.FALSE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarRetornoSolicitacaoRejeitado(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA_JORNADA_2, false);
    }

    private void validarRetornoSolicitacaoRejeitado(String idIdempotencia, String idInformacaoStatus, String idRecorrencia, boolean jornada1) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var autorizacoes = autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(idRecorrencia);
            autorizacoes.stream().findFirst().ifPresentOrElse(autorizacao ->
                            assertAll(
                                    () -> assertNull(autorizacao.getTipoSubStatus()),
                                    () -> assertEquals(TipoStatusAutorizacao.REJEITADA, autorizacao.getTipoStatus()),
                                    () -> assertEquals(idInformacaoStatus, autorizacao.getIdInformacaoStatusRecebimento()),
                                    () -> assertEquals(DIVERGENCIA_ENTRE_DADOS_RECORRENCIA_E_O_STATUS_RECORRENCIA.name(), autorizacao.getMotivoRejeicao())

                            ),
                    () -> fail(RECURSO_NAO_ENCONTRADO)
            );

            if(jornada1) {
                var optSolicitacao = solicitacaoRepository.findById("SC8118152120250425041bYqAj6ef");
                optSolicitacao.ifPresentOrElse(solicitacao ->
                                assertAll(
                                        () -> assertNull(solicitacao.getTipoSubStatus()),
                                        () -> assertEquals(PENDENTE_CONFIRMACAO, solicitacao.getTipoStatus())
                                ),
                        () -> fail(RECURSO_NAO_ENCONTRADO)
                );
            }

            var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(1, idempotenteOut.size())
            );

            var notificacao = verificarMensagemNotificacao();

            assertAll(
                    () -> assertNotNull(notificacao),
                    () -> assertEquals(2, notificacao.getInformacoesAdicionais().size()),
                    () -> assertEquals("MOBI", notificacao.getCanal()),
                    () -> assertEquals(CONTA, notificacao.getConta()),
                    () -> assertEquals("0101", notificacao.getAgencia()),
                    () -> assertEquals(NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR, notificacao.getOperacao())

            );
        });
    }

    private void validarRetornoSolicitacaoAprovado(String idIdempotencia, String idInformacaoStatus, String idRecorrencia, boolean jornada1) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var autorizacoes = autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(idRecorrencia);
            autorizacoes.stream().findFirst().ifPresentOrElse(autorizacao ->
                            assertAll(
                                    () -> assertNull(autorizacao.getTipoSubStatus()),
                                    () -> assertEquals(TipoStatusAutorizacao.APROVADA, autorizacao.getTipoStatus()),
                                    () -> assertEquals(idInformacaoStatus, autorizacao.getIdInformacaoStatusRecebimento())
                            ),
                    () -> fail(RECURSO_NAO_ENCONTRADO)
            );

            if(jornada1) {
                var optSolicitacao = solicitacaoRepository.findById("SC8118152120250425041bYqAj6ef");

                optSolicitacao.ifPresentOrElse(solicitacao ->
                                assertAll(
                                        () -> assertNull(solicitacao.getTipoSubStatus()),
                                        () -> assertEquals(ACEITA, solicitacao.getTipoStatus())
                                ),
                        () -> fail(RECURSO_NAO_ENCONTRADO)
                );
            }

            var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(1, idempotenteOut.size())
            );

            var notificacao = verificarMensagemNotificacao();

            assertAll(
                    () -> assertNotNull(notificacao),
                    () -> assertEquals(2, notificacao.getInformacoesAdicionais().size()),
                    () -> assertEquals("MOBI", notificacao.getCanal()),
                    () -> assertEquals(CONTA, notificacao.getConta()),
                    () -> assertEquals("0101", notificacao.getAgencia()),
                    () -> assertEquals(NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_CONFIRMADA_SUCESSO, notificacao.getOperacao())

            );
        });
    }

    private NotificacaoDTO verificarMensagemNotificacao() {
        return getNotificacaoDTO(consumer, TOPICO_NOTIFICACAO);
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

    private void enviarMensagem(Object payload, String topico) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, topico)
                .setHeader("TIPO_MENSAGEM", "PAIN012")
                .setHeader("ID_IDEMPOTENCIA", ID_INFORMACOES_STATUS)
                .setHeader(KafkaHeaders.KEY, "RN9118152120250425041bYqAj6ef")
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_NOTIFICACAO);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);

        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        var consumerTest = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

}
