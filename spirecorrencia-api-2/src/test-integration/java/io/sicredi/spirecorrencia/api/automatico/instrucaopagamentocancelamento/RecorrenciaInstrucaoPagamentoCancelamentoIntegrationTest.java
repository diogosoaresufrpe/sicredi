package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.spi.dto.Camt029Dto;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoCamt029;
import br.com.sicredi.spi.entities.type.StatusCancelamentoCamt029;
import br.com.sicredi.spi.entities.type.TipoAceitacaoRejeicaoCamt029;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoRepository;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

public class RecorrenciaInstrucaoPagamentoCancelamentoIntegrationTest extends AbstractIntegrationTest {

    private static final String ATIVA = "ATIVA";
    private static final String CRIADA = "CRIADA";
    private static final String AGUARDANDO_ENVIO = "AGUARDANDO_ENVIO";
    private static final String ICOM_CAMT_ENVIO_V_1 = "icom-camt-envio-v1";

    @Autowired
    private RecorrenciaInstrucaoPagamentoRepository repository;

    @Autowired
    private RecorrenciaInstrucaoPagamentoCancelamentoRepository cancelamentoRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        consumer = configurarConsumer();
    }


    @SqlGroup(
            value = {
                    @Sql(scripts = {"/db/clear.sql", "/db/consulta_autorizacao.sql", "/db/instrucao_pagamento.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                    @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
            }
    )
    @Nested
    class ProcessarSucesso {

        @Test
        void dadaUmaSolicitacaoCancelamentoValida_quandoProcessar_deveEnviarCamt029DeSucesso() {
            var camt055Dto = buildCamt055Dto();

            enviarMensagem(camt055Dto);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        assertTrue(repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).isPresent());
                        var instrucaoPagamento = repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).get();
                        assertEquals(ATIVA, instrucaoPagamento.getTpoStatus());
                        assertEquals("AGUARDANDO_CANCELAMENTO", instrucaoPagamento.getTpoSubStatus());

                        assertTrue(cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).isPresent());
                        var recorrenciaInstrucaoPagamentoCancelamento = cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).get();
                        assertEquals(camt055Dto.getIdCancelamentoAgendamento(), recorrenciaInstrucaoPagamentoCancelamento.getIdCancelamentoAgendamento());
                        assertEquals(camt055Dto.getMotivoCancelamento(), recorrenciaInstrucaoPagamentoCancelamento.getCodMotivoCancelamento());
                        assertEquals("RECEBEDOR", recorrenciaInstrucaoPagamentoCancelamento.getTpoPspSolicitanteCancelamento());
                        assertEquals(camt055Dto.getDataHoraSolicitacaoOuInformacao(), recorrenciaInstrucaoPagamentoCancelamento.getDatCriacaoSolicitacaoCancelamento());
                        assertNotNull(recorrenciaInstrucaoPagamentoCancelamento.getDatAnalisadoSolicitacaoCancelamento());

                        validarIdempotencia();
                        var camt029Dto = getCamt029Dto();
                        assertAll(
                                () -> assertNotNull(camt029Dto),
                                () -> assertEquals(camt055Dto.getIdCancelamentoAgendamento(), camt029Dto.getIdCancelamentoAgendamentoOriginal()),
                                () -> assertEquals(camt055Dto.getIdConciliacaoRecebedorOriginal(), camt029Dto.getIdConciliacaoRecebedorOriginal()),
                                () -> assertEquals(camt055Dto.getIdFimAFimOriginal(), camt029Dto.getIdFimAFimOriginal()),
                                () -> assertEquals(camt055Dto.getParticipanteSolicitanteDoCancelamento(), camt029Dto.getParticipanteAtualizaSolicitacaoDoCancelamento()),
                                () -> assertEquals(camt055Dto.getParticipanteDestinatarioDoCancelamento(), camt029Dto.getParticipanteRecebeAtualizacaoDoCancelamento()),
                                () -> assertEquals(StatusCancelamentoCamt029.ACEITO.name(), camt029Dto.getStatusDoCancelamento()),
                                () -> assertEquals(TipoAceitacaoRejeicaoCamt029.DATA_HORA_ACEITACAO_CANCELAMENTO.name(), camt029Dto.getTipoAceitacaoOuRejeicao()),
                                () -> assertNotNull(camt029Dto.getDataHoraAceitacaoOuRejeicaoDoCancelamento()),
                                () -> assertNull(camt029Dto.getCodigoDeRejeicaoDoCancelamento())
                        );
                    });
        }
    }

    @SqlGroup(
            value = {
                    @Sql(scripts = {"/db/clear.sql", "/db/consulta_autorizacao.sql", "/db/instrucao_pagamento.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                    @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
            }
    )
    @Nested
    class ProcessarRejeicoes {

        @Test
        void dadaUmaSolicitacaoCancelamentoSemInstrucaoDePagamento_quandoProcessar_deveEnviarCamt029ComCodRejeicao() {
            var camt055Dto = buildCamt055Dto();
            camt055Dto.setIdFimAFimOriginal("idFimAFimDiferente");

            enviarMensagem(camt055Dto);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        assertTrue(repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).isEmpty());
                        assertTrue(cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).isPresent());
                        var instrucaoPagamentoCancelamento = cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).get();
                        validarIdempotencia();
                        validarCamt029Enviado(
                                getCamt029Dto(),
                                camt055Dto,
                                MotivoRejeicaoCamt029.ID_FIM_A_FIM_NAO_CORRESPONDE_AO_ORIGINAL_INFORMADO,
                                instrucaoPagamentoCancelamento.getCodMotivoRejeicao());
                    });
        }

        @Test
        void dadaUmaSolicitacaoCancelamentoComInstrucaoDePagamentoComStatusInvalido_quandoProcessar_deveEnviarCamt029ComCodRejeicao() {
            var camt055Dto = buildCamt055Dto();
            camt055Dto.setIdFimAFimOriginal("XXxxxxxxx202610011030kkkkkka");

            enviarMensagem(camt055Dto);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        assertTrue(repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).isPresent());
                        var instrucaoPagamento = repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).get();
                        assertEquals(ATIVA, instrucaoPagamento.getTpoStatus());
                        assertEquals(AGUARDANDO_ENVIO, instrucaoPagamento.getTpoSubStatus());
                        assertTrue(cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).isPresent());
                        var instrucaoPagamentoCancelamento = cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).get();
                        validarIdempotencia();
                        validarCamt029Enviado(
                                getCamt029Dto(),
                                camt055Dto,
                                MotivoRejeicaoCamt029.PAGAMENTO_JA_CONCLUIDO_COM_SUCESSO,
                                instrucaoPagamentoCancelamento.getCodMotivoRejeicao());
                    });
        }

        @Test
        void dadaUmaSolicitacaoCancelamentoRecebidoEmHorarioInvalido_quandoProcessar_deveEnviarCamt029ComCodRejeicao() {
            var camt055Dto = buildCamt055Dto();
            camt055Dto.setIdFimAFimOriginal("XXxxxxxxx202310011030kkkkkkf");

            enviarMensagem(camt055Dto);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        assertTrue(repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).isPresent());
                        var instrucaoPagamento = repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).get();
                        assertEquals(CRIADA, instrucaoPagamento.getTpoStatus());
                        assertEquals(AGUARDANDO_ENVIO, instrucaoPagamento.getTpoSubStatus());
                        assertTrue(cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).isPresent());
                        var instrucaoPagamentoCancelamento = cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).get();
                        validarIdempotencia();
                        validarCamt029Enviado(
                                getCamt029Dto(),
                                camt055Dto,
                                MotivoRejeicaoCamt029.SOLICITACAO_CANCELAMENTO_NAO_RECEBIDA_NO_PRAZO,
                                instrucaoPagamentoCancelamento.getCodMotivoRejeicao());
                    });
        }

        @Test
        void dadaUmaSolicitacaoCancelamentoComIdConciliacaoInvalido_quandoProcessar_devenEnviarCamt029ComCodRejeicao() {
            var camt055Dto = buildCamt055Dto();
            camt055Dto.setIdConciliacaoRecebedorOriginal("idConciliacaoDiferente");

            enviarMensagem(camt055Dto);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        assertTrue(repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).isPresent());
                        var instrucaoPagamento = repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).get();
                        assertEquals(CRIADA, instrucaoPagamento.getTpoStatus());
                        assertEquals(AGUARDANDO_ENVIO, instrucaoPagamento.getTpoSubStatus());
                        assertTrue(cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).isPresent());
                        var instrucaoPagamentoCancelamento = cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).get();
                        validarIdempotencia();
                        validarCamt029Enviado(
                                getCamt029Dto(),
                                camt055Dto,
                                MotivoRejeicaoCamt029.ID_CONCILICAO_RECEBEDOR_NAO_CORRESPONDE_AO_ORIGINALMENTE_INFORMADO,
                                instrucaoPagamentoCancelamento.getCodMotivoRejeicao());
                    });
        }

        @Test
        void dadaUmaSolicitacaoCancelamentoComCpfCnpjRecebedorInvalido_quandoProcessar_devenEnviarCamt029ComCodRejeicao() {
            var camt055Dto = buildCamt055Dto();
            camt055Dto.setIdFimAFimOriginal("XXxxxxxxx202610011030kkkkkkj");

            enviarMensagem(camt055Dto);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        assertTrue(repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).isPresent());
                        var instrucaoPagamento = repository.buscarPorCodFimAFimComAutorizacao(camt055Dto.getIdFimAFimOriginal()).get();
                        assertEquals(CRIADA, instrucaoPagamento.getTpoStatus());
                        assertEquals(AGUARDANDO_ENVIO, instrucaoPagamento.getTpoSubStatus());
                        assertTrue(cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).isPresent());
                        var instrucaoPagamentoCancelamento = cancelamentoRepository.findById(camt055Dto.getIdCancelamentoAgendamento()).get();
                        validarIdempotencia();
                        validarCamt029Enviado(
                                getCamt029Dto(),
                                camt055Dto,
                                MotivoRejeicaoCamt029.CPF_CNPJ_USUARIO_RECEBEDOR_NAO_CORRESPONDENTE_RECORRENCIA_OU_AUTORIZACAO,
                                instrucaoPagamentoCancelamento.getCodMotivoRejeicao());
                    });
        }


        private void validarCamt029Enviado(Camt029Dto camt029, Camt055Dto camt055Dto, MotivoRejeicaoCamt029 codRejeicao, String codMotivoRejeicaoInstrucaoPagamento) {
            assertAll(
                    () -> assertEquals(codRejeicao.name(), codMotivoRejeicaoInstrucaoPagamento),
                    () -> assertNotNull(camt029),
                    () -> assertEquals(camt055Dto.getIdCancelamentoAgendamento(), camt029.getIdCancelamentoAgendamentoOriginal()),
                    () -> assertEquals(camt055Dto.getIdConciliacaoRecebedorOriginal(), camt029.getIdConciliacaoRecebedorOriginal()),
                    () -> assertEquals(StatusCancelamentoCamt029.REJEITADO.name(), camt029.getStatusDoCancelamento()),
                    () -> assertEquals(codRejeicao.name(), camt029.getCodigoDeRejeicaoDoCancelamento()),
                    () -> assertEquals(camt055Dto.getIdFimAFimOriginal(), camt029.getIdFimAFimOriginal()),
                    () -> assertEquals(TipoAceitacaoRejeicaoCamt029.DATA_HORA_REJEICAO_CANCELAMENTO.name(), camt029.getTipoAceitacaoOuRejeicao()),
                    () -> assertNotNull(camt029.getDataHoraAceitacaoOuRejeicaoDoCancelamento()),
                    () -> assertEquals(camt055Dto.getParticipanteDestinatarioDoCancelamento(), camt029.getParticipanteRecebeAtualizacaoDoCancelamento()),
                    () -> assertEquals(camt055Dto.getParticipanteSolicitanteDoCancelamento(), camt029.getParticipanteAtualizaSolicitacaoDoCancelamento())
            );
        }
    }

    private void validarIdempotencia() {
        var idIdempotencia = "idIdempotencia_CAMT055_REC";
        var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
        var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);
        assertAll(
                () -> assertEquals(1, idempotenteIn.size()),
                () -> assertEquals(1, idempotenteOut.size())
        );
    }

    private Camt055Dto buildCamt055Dto() {
        return Camt055Dto.builder()
                .idCancelamentoAgendamento("idCancelamentoAgendamento")
                .idConciliacaoRecebedorOriginal("idConciliacaoRecebedor")
                .cpfCnpjUsuarioSolicitanteCancelamento("78351090000")
                .motivoCancelamento("motivoCancelamento")
                .idFimAFimOriginal("XXxxxxxxx202610011030kkkkkkk")
                .tipoSolicitacaoOuInformacao("tipoSolicitacaoOuInformacao")
                .dataHoraSolicitacaoOuInformacao(LocalDateTime.of(2025, 1, 1, 10, 20))
                .participanteSolicitanteDoCancelamento("123")
                .participanteDestinatarioDoCancelamento("321")
                .dataHoraCriacaoParaEmissao(LocalDateTime.of(2024,10,10,0,0))
                .build();
    }

    private void enviarMensagem(Object payload) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, "icom-camt-recebido-v1")
                .setHeader("TIPO_MENSAGEM", "CAMT055")
                .setHeader("ID_IDEMPOTENCIA", "idIdempotencia")
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private Consumer<String, String> configurarConsumer() {
        var consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumerTest.subscribe(List.of(ICOM_CAMT_ENVIO_V_1));
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

    private Camt029Dto getCamt029Dto() {
        var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(5), 2);

        for (ConsumerRecord<String, String> iteration : response.records(ICOM_CAMT_ENVIO_V_1)) {
            if (iteration.value() != null) {
                return ObjectMapperUtil.converterStringParaObjeto(iteration.value(), Camt029Dto.class);
            }
        }
        return new Camt029Dto();
    }
}
