package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.spi.dto.Pain012Dto;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
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

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_EXPIRADA;
import static br.com.sicredi.spi.entities.type.StatusRecorrenciaPain012.CANCELADA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Titles.CPF_CNPJ;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.getNotificacaoDTO;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/autorizacao_recorrencia.sql", "/db/cancelamento_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class RetornoBacenCancelamentoAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String RECURSO_NAO_ENCONTRADO = "Recurso não encontrado para os dados informados";

    private static final String ID_RECORRENCIA = "RN4118152120250425041bYqAj6ef";
    private static final String ID_INFORMACOES_STATUS = "IS1218152120250425041bYqAj6ef";
    private static final String ID_IDEMPOTENCIA = "IS1218152120250425041bYqAj6ef_PAIN012_REC";
    private static final String ID_INFORMACAO_CANCELAMENTO = "IC0118152120250425041bYqAj6ef";
    private static final String TOPICO_ICOM_PAIN_RECEBIDO_V1 = "icom-pain-recebido-v1";
    private static final String TOPICO_NOTIFICACAO = "spi-notificacao-recorrencia-v2";
    private static final String CONTA = "223190";

    @Autowired
    private RecorrenciaAutorizacaoRepository autorizacaoRepository;

    @Autowired
    private RecorrenciaAutorizacaoCancelamentoRepository cancelamentoRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoCancelamentoAprovadoPeloRecebedor_quandoProcessarRespostaBacenCancelamentoAutorizacao_deveAtualizarAutorizacaoECancelamentoEEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.TRUE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarRetornoSolicitacao(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA, TipoStatusAutorizacao.CANCELADA, TipoStatusCancelamentoAutorizacao.ACEITA, NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_SUCESSO, null);
    }

    @Test
    void dadoCancelamentoRejeitadoPeloRecebedor_quandoProcessarRespostaBacenCancelamentoAutorizacao_deveAtualizarAutorizacaoECancelamentoENaoEnviarNotificacao() {
        var retornoSolicitacaoAutorizacao = criarRetornoSolicitacaoAutorizacao(ID_INFORMACOES_STATUS, ID_RECORRENCIA, Boolean.FALSE);

        enviarMensagem(retornoSolicitacaoAutorizacao, TOPICO_ICOM_PAIN_RECEBIDO_V1);

        validarRetornoSolicitacao(ID_IDEMPOTENCIA, ID_INFORMACOES_STATUS, ID_RECORRENCIA, TipoStatusAutorizacao.APROVADA,TipoStatusCancelamentoAutorizacao.REJEITADA, NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_NEGADO, RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_EXPIRADA.name());
    }

    private void validarRetornoSolicitacao(String idIdempotencia,
                                           String idInformacaoStatus,
                                           String idRecorrencia,
                                           TipoStatusAutorizacao statusAutorizacao,
                                           TipoStatusCancelamentoAutorizacao statusCancelamento,
                                           NotificacaoDTO.TipoTemplate template,
                                           String codigoMotivo) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var autorizacoes = autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(idRecorrencia);
            autorizacoes.stream().findFirst().ifPresentOrElse(autorizacao ->
                            assertAll(
                                    () -> assertNull(autorizacao.getTipoSubStatus()),
                                    () -> assertEquals(statusAutorizacao, autorizacao.getTipoStatus())
                            ),
                    () -> fail(RECURSO_NAO_ENCONTRADO)
            );

            var optCancelamento = cancelamentoRepository.findById(ID_INFORMACAO_CANCELAMENTO);
            optCancelamento.ifPresentOrElse(cancelamento ->
                            assertAll(
                                    () -> assertEquals(statusCancelamento, cancelamento.getTipoStatus()),
                                    () -> assertEquals(idInformacaoStatus, cancelamento.getIdInformacaoStatus()),
                                    () -> assertEquals(codigoMotivo, cancelamento.getMotivoRejeicao())
                            ),
                    () -> fail(RECURSO_NAO_ENCONTRADO)
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
                    () -> assertEquals(2, notificacao.getInformacoesAdicionais().size()),
                    () -> assertEquals("MOBI", notificacao.getCanal()),
                    () -> assertEquals(CONTA, notificacao.getConta()),
                    () -> assertEquals("0101", notificacao.getAgencia()),
                    () -> assertEquals(template, notificacao.getOperacao())

            );
        });
    }

    private NotificacaoDTO verificarMensagemNotificacao() {
        return getNotificacaoDTO(consumer, TOPICO_NOTIFICACAO);
    }

    private Pain012Dto criarRetornoSolicitacaoAutorizacao(String idInformacaoStatus, String idRecorrencia, Boolean status) {
        return Pain012Dto.builder()
                .status(status)
                .motivoRejeicao(RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_EXPIRADA.name())
                .statusRecorrencia(CANCELADA.name())
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
                .setHeader("ID_IDEMPOTENCIA", ID_INFORMACOES_STATUS)
                .setHeader("TIPO_MENSAGEM", "PAIN012")
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
