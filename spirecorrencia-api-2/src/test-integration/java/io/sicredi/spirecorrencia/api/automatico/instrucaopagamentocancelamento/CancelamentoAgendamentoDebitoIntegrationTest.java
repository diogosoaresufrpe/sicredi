package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;


import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoRepository;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.RECURSO_NAO_ENCONTRADO;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/instrucao_pagamento.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class CancelamentoAgendamentoDebitoIntegrationTest extends AbstractIntegrationTest {

    private static final String CPF_SOLICITANTE = "78351090000";
    private static final String ID_FIM_A_FIM = "E8785320620250506210800n2f1PQijN";
    private static final String ID_FIM_A_FIM_CRIADA = "E6985320620250506210800n2f1PQijN";
    private static final String ID_CANCELAMENTO_AGENDAMENTO = "CA1111111120240101qwertyuiopa";
    private static final String TOPICO_ICOM_CAMT_ENVIO_V1 = "icom-camt-envio-v1";

    private Consumer<String, String> consumer;

    @Autowired
    private RecorrenciaInstrucaoPagamentoCancelamentoRepository instrucaoPagamentoCancelamentoRepository;

    @Autowired
    private RecorrenciaInstrucaoPagamentoRepository instrucaoPagamentoRepository;

    @Autowired
    private CancelamentoAgendamentoInstrucaoPagamentoServiceImpl service;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoPedidoCancelamento_quandoProcessarCancelamentoDebito_deveCriarCamt55EEnviarEvento() {
        var instrucaoPagamentoBanco = instrucaoPagamentoRepository.buscarPorCodFimAFimComAutorizacao(ID_FIM_A_FIM).orElse(null);
        var wrapper = CancelamentoAgendamentoWrapperDTO.fromCancelamentoInterno(instrucaoPagamentoBanco, CPF_SOLICITANTE, MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR);

        service.processarCancelamentoDebitoSemIdempotencia(wrapper);

        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    var optInstrucao = instrucaoPagamentoRepository.buscarPorCodFimAFimComAutorizacao(ID_FIM_A_FIM);
                    optInstrucao.ifPresentOrElse(instrucaoPagamento ->
                                    assertAll(
                                            () -> assertEquals("AGUARDANDO_CANCELAMENTO", instrucaoPagamento.getTpoSubStatus())
                                    ),
                            () -> fail(RECURSO_NAO_ENCONTRADO)
                    );

                    var optInstrucaoCancelamento = instrucaoPagamentoCancelamentoRepository.findById(wrapper.getIdCancelamentoAgendamento());
                    optInstrucaoCancelamento.ifPresentOrElse(instrucaoPagamentoCancelamento ->
                                    assertAll(
                                            () -> assertEquals(ID_FIM_A_FIM, instrucaoPagamentoCancelamento.getCodFimAFim()),
                                            () -> assertEquals("CRIADA", instrucaoPagamentoCancelamento.getTpoStatus()),
                                            () -> assertEquals(CPF_SOLICITANTE, instrucaoPagamentoCancelamento.getNumCpfCnpjSolicitanteCancelamento()),
                                            () -> assertEquals(SOLICITADO_PELO_PAGADOR.name(), instrucaoPagamentoCancelamento.getCodMotivoCancelamento())
                                    ),
                            () -> fail(RECURSO_NAO_ENCONTRADO)
                    );

                    var camt055Dto = verificarMensagemCamt55();

                    assertAll(
                            () -> assertEquals(wrapper.getIdCancelamentoAgendamento(), camt055Dto.getIdCancelamentoAgendamento()),
                            () -> assertEquals("idConciliacaoRecebedor", camt055Dto.getIdConciliacaoRecebedorOriginal()),
                            () -> assertEquals(CPF_SOLICITANTE, camt055Dto.getCpfCnpjUsuarioSolicitanteCancelamento()),
                            () -> assertEquals(SOLICITADO_PELO_PAGADOR.name(), camt055Dto.getMotivoCancelamento()),
                            () -> assertEquals(ID_FIM_A_FIM, camt055Dto.getIdFimAFimOriginal()),
                            () -> assertEquals(SOLICITADO_PELO_PAGADOR.name(), camt055Dto.getTipoSolicitacaoOuInformacao()),
                            () -> assertEquals(wrapper.getDataHoraSolicitacaoCancelamento().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")), camt055Dto.getDataHoraSolicitacaoOuInformacao().toString()),
                            () -> assertEquals("123", camt055Dto.getParticipanteSolicitanteDoCancelamento()),
                            () -> assertEquals("11", camt055Dto.getParticipanteDestinatarioDoCancelamento())
                    );
                });
    }

    @Test
    void dadoErroNegocioStatusIncorreto_quandoProcessarCancelamentoDebito_deveRetornarErroENaoProcessar() {
        var instrucaoPagamento = instrucaoPagamentoRepository.buscarPorCodFimAFimComAutorizacao(ID_FIM_A_FIM_CRIADA).orElse(null);
        var wrapper = CancelamentoAgendamentoWrapperDTO.fromCancelamentoInterno(instrucaoPagamento, CPF_SOLICITANTE, MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR);

        service.processarCancelamentoDebitoSemIdempotencia(wrapper);

        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    var optInstrucao = instrucaoPagamentoRepository.buscarPorCodFimAFimComAutorizacao(ID_FIM_A_FIM_CRIADA);
                    optInstrucao.ifPresentOrElse(instrucaoPagamentoOpt ->
                                    assertAll(
                                            () -> assertEquals("AGUARDANDO_ENVIO", instrucaoPagamentoOpt.getTpoSubStatus())
                                    ),
                            () -> fail(RECURSO_NAO_ENCONTRADO)
                    );

                    var optInstrucaoCancelamento = instrucaoPagamentoCancelamentoRepository.findById(ID_CANCELAMENTO_AGENDAMENTO);
                    assertTrue(optInstrucaoCancelamento.isEmpty(), "O cancelamento não deveria estar presente no banco");

                    var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);
                    assertEquals(0, response.count());
                });
    }


    private Camt055Dto verificarMensagemCamt55() {
        var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);

        for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> iteration : response.records(TOPICO_ICOM_CAMT_ENVIO_V1)) {
            var headers = iteration.headers();

            boolean temTipoMensagem = headers.lastHeader("TIPO_MENSAGEM") != null;
            boolean temPspEmissor = headers.lastHeader("PSP_EMISSOR") != null;
            boolean temIdIdempotencia = headers.lastHeader("ID_IDEMPOTENCIA") != null;

            if (!temTipoMensagem || !temPspEmissor || !temIdIdempotencia) {
                throw new IllegalArgumentException("Mensagem Kafka recebida está sem os headers obrigatórios: "
                                                   + (temTipoMensagem ? "" : "[TIPO_MENSAGEM] ")
                                                   + (temPspEmissor ? "" : "[PSP_EMISSOR] ")
                                                   + (temIdIdempotencia ? "" : "[ID_IDEMPOTENCIA] "));
            }

            if (iteration.value() != null) {
                return ObjectMapperUtil.converterStringParaObjeto(iteration.value(), new TypeReference<Camt055Dto>() {
                });
            }
        }

        throw new AssertionError("Nenhuma mensagem válida foi recebida no tópico.");
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_ICOM_CAMT_ENVIO_V1);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }
}
