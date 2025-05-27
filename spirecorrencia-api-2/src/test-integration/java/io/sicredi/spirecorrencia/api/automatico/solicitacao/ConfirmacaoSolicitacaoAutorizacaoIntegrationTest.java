package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.ConfirmacaoSolicitacaoAutorizacaoRecorrenciaTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.MotivoRejeicao;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoService;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class ConfirmacaoSolicitacaoAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String CONTA = "223190";
    private static final String COOPERATIVA = "0101";
    private static final String TOPICO_CONFIRMACAO_AUTORIZACAO = "automatico-recorrente-autorizacao-confirmacao-protocolo-v1";
    private static final String ID_SOLICITACAO_AUTORIZACAO_1 = "SC0118152120250425041bYqAj6gg";
    private static final String ID_SOLICITACAO_AUTORIZACAO_2 = "SC0118152120250425041bYqAj6ff";
    private static final String ID_SOLICITACAO_AUTORIZACAO_3 = "SC0118152120250425041bYqAj6hh";
    private static final String ID_RECORRENCIA_1 = "idRecorrencia1";
    private static final String ID_RECORRENCIA_2 = "idRecorrencia2";
    private static final String ID_RECORRENCIA_3 = "idRecorrencia3";
    private static final String ID_RECORRENCIA_4 = "idRecorrencia4";
    private static final String ID_INFORMACAO_STATUS_1 = "teste1";
    private static final String ID_INFORMACAO_STATUS_2 = "teste2";
    private static final String ID_INFORMACAO_STATUS_3 = "teste3";

    @Autowired
    private SolicitacaoAutorizacaoRecorrenciaService solicitacaoAutorizacaoRecorrenciaService;

    @Autowired
    private AutorizacaoService autorizacaoService;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        consumer = configurarConsumer();
    }

    @SqlGroup(
            value = {
                    @Sql(scripts = {"/db/clear.sql", "/db/solicitacao_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                    @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
            }
    )
    @Nested
    class ProcessarConfirmacao {

        @Test
        void dadaUmaConfirmacaoRejeitada_quandoProcessar_deveEnviarSucesso() {
            var confirmacaoAutorizacaoRequestDTO = getPayloadConfirmacaoAutorizacaoRequestDTO(
                    ID_SOLICITACAO_AUTORIZACAO_1, ID_RECORRENCIA_1, ID_INFORMACAO_STATUS_1, false, MotivoRejeicao.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR);
            var protocoloDTO = getProtocoloDTO(ID_RECORRENCIA_1, ObjectMapperUtil.converterObjetoParaString(confirmacaoAutorizacaoRequestDTO));

            enviarMensagem(protocoloDTO);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                assertNotNull(getSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(ID_SOLICITACAO_AUTORIZACAO_1, TipoStatusSolicitacaoAutorizacao.CONFIRMADA));
                var solicitacao = getSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(ID_SOLICITACAO_AUTORIZACAO_1, TipoStatusSolicitacaoAutorizacao.CONFIRMADA);

                assertNotNull(getAutorizacao(solicitacao.getIdRecorrencia()));
                var autorizacao = getAutorizacao(solicitacao.getIdRecorrencia());

                validarCriacaoAutorizacao(solicitacao, autorizacao, confirmacaoAutorizacaoRequestDTO);
                Assertions.assertEquals(confirmacaoAutorizacaoRequestDTO.getMotivoRejeicao().name(), autorizacao.getMotivoRejeicao());
                validarQuantidadeDeEventosEnviados(2);
            });
        }

        @Test
        void dadaUmaConfirmacaoAprovada_quandoProcessar_deveEnviarSucesso() {
            var confirmacaoAutorizacaoRequestDTO = getPayloadConfirmacaoAutorizacaoRequestDTO(
                    ID_SOLICITACAO_AUTORIZACAO_2, ID_RECORRENCIA_2, ID_INFORMACAO_STATUS_2, true, null);
            var protocoloDTO = getProtocoloDTO(ID_RECORRENCIA_2, ObjectMapperUtil.converterObjetoParaString(confirmacaoAutorizacaoRequestDTO));

            enviarMensagem(protocoloDTO);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                assertNotNull(getSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(ID_SOLICITACAO_AUTORIZACAO_2, TipoStatusSolicitacaoAutorizacao.CONFIRMADA));
                var solicitacao = getSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(ID_SOLICITACAO_AUTORIZACAO_2, TipoStatusSolicitacaoAutorizacao.CONFIRMADA);

                assertNotNull(getAutorizacao(solicitacao.getIdRecorrencia()));
                var autorizacao = getAutorizacao(solicitacao.getIdRecorrencia());

                validarCriacaoAutorizacao(solicitacao, autorizacao, confirmacaoAutorizacaoRequestDTO);
                assertNull(autorizacao.getMotivoRejeicao());
                validarQuantidadeDeEventosEnviados(2);
            });
        }

        @Test
        void dadaUmaConfirmacaoComErroDeNegocio_quandoProcessar_deveEnviarErroNegocio() {
            var confirmacaoAutorizacaoRequestDTO = getPayloadConfirmacaoAutorizacaoRequestDTO(
                    ID_SOLICITACAO_AUTORIZACAO_1, ID_RECORRENCIA_3, ID_INFORMACAO_STATUS_3, true, null);
            confirmacaoAutorizacaoRequestDTO.setValorMaximo(BigDecimal.ONE);
            var protocoloDTO = getProtocoloDTO(ID_RECORRENCIA_3, ObjectMapperUtil.converterObjetoParaString(confirmacaoAutorizacaoRequestDTO));

            enviarMensagem(protocoloDTO);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                assertNotNull(getSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(ID_SOLICITACAO_AUTORIZACAO_3, TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO));
                assertThrows(NoSuchElementException.class, () -> getAutorizacao(ID_RECORRENCIA_3));
                validarQuantidadeDeEventosEnviados(2);
            });
        }

        @Test
        void dadaUmaConfirmacaoComErroDeConstraint_quandoProcessar_deveEnviarErroConstraint() {
            var confirmacaoAutorizacaoRequestDTO = getPayloadConfirmacaoAutorizacaoRequestDTO(
                    ID_SOLICITACAO_AUTORIZACAO_3, ID_RECORRENCIA_4, ID_INFORMACAO_STATUS_3, false, MotivoRejeicao.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR);
            confirmacaoAutorizacaoRequestDTO.setIdSolicitacaoRecorrencia(null);
            var protocoloDTO = getProtocoloDTO(ID_RECORRENCIA_4, ObjectMapperUtil.converterObjetoParaString(confirmacaoAutorizacaoRequestDTO));

            enviarMensagem(protocoloDTO);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                assertNotNull(getSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(ID_SOLICITACAO_AUTORIZACAO_3, TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO));
                assertThrows(NoSuchElementException.class, () -> getAutorizacao(ID_RECORRENCIA_4));
                validarQuantidadeDeEventosEnviados(2);
            });
        }

        private SolicitacaoAutorizacaoRecorrencia getSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(
                String idSolicitacaoAutorizacao, TipoStatusSolicitacaoAutorizacao status) {
            return solicitacaoAutorizacaoRecorrenciaService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(
                    idSolicitacaoAutorizacao, status);
        }

        private RecorrenciaAutorizacao getAutorizacao(String idRecorrencia) {
            return autorizacaoService.findAll().stream()
                    .filter(recAut -> recAut.getIdRecorrencia().equals(idRecorrencia))
                    .toList()
                    .getFirst();
        }
    }

    @SqlGroup(
            value = {
                    @Sql(scripts = {"/db/clear.sql", "/db/solicitacao_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                    @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
            }
    )
    @Nested
    class ProcessarDLT {

        @Test
        void dadoPayloadInvalido_quandoConsumirMensagem_deveEnviarParaDLT() {
            var confirmacaoAutorizacaoRequestDTO = getPayloadConfirmacaoAutorizacaoRequestDTO(
                    ID_SOLICITACAO_AUTORIZACAO_3, ID_RECORRENCIA_3, ID_INFORMACAO_STATUS_3, false, MotivoRejeicao.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR);
            confirmacaoAutorizacaoRequestDTO.setCanal("CANAL_INVALIDO");
            var protocoloDTO = getProtocoloDTO(ID_RECORRENCIA_3, ObjectMapperUtil.converterObjetoParaString(confirmacaoAutorizacaoRequestDTO));

            enviarMensagem(protocoloDTO);
            validarQuantidadeDeEventosEnviados(2);
        }
    }

    private void validarQuantidadeDeEventosEnviados(int expected) {
        var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);
        assertEquals(expected, response.count());
    }

    private void enviarMensagem(Object payload) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, ConfirmacaoSolicitacaoAutorizacaoIntegrationTest.TOPICO_CONFIRMACAO_AUTORIZACAO)
                .setHeader("X-Ultima-Interacao", "2025-05-05 16:02:30.160-0300")
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private static void validarCriacaoAutorizacao(
            SolicitacaoAutorizacaoRecorrencia solicitacao,
            RecorrenciaAutorizacao autorizacao,
            ConfirmacaoSolicitacaoAutorizacaoRecorrenciaTransacaoDTO confirmacaoAutorizacaoRequestDTO) {
        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(1))
                .untilAsserted(() -> assertAll(
                () -> assertEquals(solicitacao.getIdRecorrencia(), autorizacao.getIdRecorrencia()),
                () -> assertEquals(solicitacao.getIdInformacaoStatus(), autorizacao.getIdInformacaoStatusEnvio()),
                () -> assertNull(autorizacao.getIdInformacaoStatusRecebimento()),
                () -> assertEquals("JORNADA_1", autorizacao.getTipoJornada()),
                () -> assertEquals(TipoStatusAutorizacao.CRIADA, autorizacao.getTipoStatus()),
                () -> assertEquals("AGUARDANDO_ENVIO", autorizacao.getTipoSubStatus()),
                () -> assertEquals(solicitacao.getTipoFrequencia(), autorizacao.getTipoFrequencia()),
                () -> assertEquals("S", autorizacao.getPermiteLinhaCredito()),
                () -> assertEquals("N", autorizacao.getPermiteRetentativa()),
                () -> assertEquals("S", autorizacao.getPermiteNotificacaoAgendamento()),
                () -> assertEquals(solicitacao.getCpfCnpjPagador(), autorizacao.getCpfCnpjPagador()),
                () -> assertEquals(solicitacao.getNomePagador(), autorizacao.getNomePagador()),
                () -> assertEquals(solicitacao.getAgenciaPagador(), autorizacao.getAgenciaPagador()),
                () -> assertEquals(solicitacao.getValor(), autorizacao.getValor()),
                () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getValorMaximo(), autorizacao.getValorMaximo()),
                () -> assertEquals(solicitacao.getPisoValorMaximo(), autorizacao.getPisoValorMaximo()),
                () -> assertEquals(solicitacao.getTipoContaPagador(), autorizacao.getTipoContaPagador()),
                () -> assertEquals(solicitacao.getTipoPessoaPagador(), autorizacao.getTipoPessoaPagador()),
                () -> assertEquals(solicitacao.getContaPagador(), autorizacao.getContaPagador()),
                () -> assertEquals(solicitacao.getInstituicaoPagador(), autorizacao.getInstituicaoPagador()),
                () -> assertEquals(solicitacao.getPostoPagador(), autorizacao.getPostoPagador()),
                () -> assertEquals(solicitacao.getTipoSistemaPagador(), autorizacao.getTipoSistemaPagador()),
                () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getCanal(), autorizacao.getTipoCanalPagador().name()),
                () -> assertEquals(solicitacao.getNomeRecebedor(), autorizacao.getNomeRecebedor()),
                () -> assertEquals(solicitacao.getInstituicaoRecebedor(), autorizacao.getInstituicaoRecebedor()),
                () -> assertEquals(solicitacao.getCpfCnpjRecebedor(), autorizacao.getCpfCnpjRecebedor()),
                () -> assertEquals(solicitacao.getNomeDevedor(), autorizacao.getNomeDevedor()),
                () -> assertEquals(solicitacao.getCpfCnpjDevedor(), autorizacao.getCpfCnpjDevedor()),
                () -> assertEquals(solicitacao.getDescricao(), autorizacao.getDescricao()),
                () -> assertEquals(solicitacao.getNumeroContrato(), autorizacao.getNumeroContrato()),
                () -> assertEquals(solicitacao.getCodigoMunicipioIBGE(), autorizacao.getCodigoMunicipioIBGE()),
                () -> assertEquals(solicitacao.getDataInicialRecorrencia(), autorizacao.getDataInicialRecorrencia()),
                () -> assertEquals("2025-05-05T16:02:30.160", autorizacao.getDataInicioConfirmacao().toString()),
                () -> assertEquals(solicitacao.getDataFinalRecorrencia(), autorizacao.getDataFinalRecorrencia()),
                () -> assertEquals(solicitacao.getDataCriacaoRecorrencia(), autorizacao.getDataCriacaoRecorrencia())
        ));
    }

    private ConfirmacaoSolicitacaoAutorizacaoRecorrenciaTransacaoDTO getPayloadConfirmacaoAutorizacaoRequestDTO(
            String idSolicitacaoRecorrencia, String idRecorrencia, String idInformacaoStatus, boolean aprovada, MotivoRejeicao motivoRejeicao
    ) {
        var confirmacaoAutorizacaoRequestDTO = new ConfirmacaoSolicitacaoAutorizacaoRecorrenciaTransacaoDTO();
        confirmacaoAutorizacaoRequestDTO.setIdSolicitacaoRecorrencia(idSolicitacaoRecorrencia);
        confirmacaoAutorizacaoRequestDTO.setIdRecorrencia(idRecorrencia);
        confirmacaoAutorizacaoRequestDTO.setIdInformacaoStatus(idInformacaoStatus);
        confirmacaoAutorizacaoRequestDTO.setValorMaximo(BigDecimal.valueOf(100));
        confirmacaoAutorizacaoRequestDTO.setAprovada(aprovada);
        confirmacaoAutorizacaoRequestDTO.setMotivoRejeicao(motivoRejeicao);
        confirmacaoAutorizacaoRequestDTO.setCpfCnpjConta("12690422115");
        confirmacaoAutorizacaoRequestDTO.setCanal("MOBI");
        return confirmacaoAutorizacaoRequestDTO;
    }

    private ProtocoloDTO getProtocoloDTO(String idRecorrencia, String recorrenciaRequestDTO) {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setIdProtocolo(1L);
        protocoloDTO.setIdentificadorTransacao(idRecorrencia);
        protocoloDTO.setIdentificadorSimulacaoLimite(UUID.randomUUID().toString());
        protocoloDTO.setPayloadTransacao(recorrenciaRequestDTO);
        protocoloDTO.setDataRequisicao(LocalDateTime.now());
        protocoloDTO.setDataAgendamento(LocalDateTime.now());
        protocoloDTO.setCodigoTipoTransacao("codigoTransacao");
        protocoloDTO.setCooperativa(COOPERATIVA);
        protocoloDTO.setConta(CONTA);
        protocoloDTO.setCodigoCanal(1);
        protocoloDTO.setIdentificadorUsuario(RandomStringUtils.randomNumeric(6));
        return protocoloDTO;
    }

    private Consumer<String, String> configurarConsumer() {
        var consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumerTest.subscribe(List.of("canaisdigitais-protocolo-comando-v1", "icom-pain-envio-v1"));
        consumerTest.poll(Duration.ofSeconds(10));
        return consumerTest;
    }
}
