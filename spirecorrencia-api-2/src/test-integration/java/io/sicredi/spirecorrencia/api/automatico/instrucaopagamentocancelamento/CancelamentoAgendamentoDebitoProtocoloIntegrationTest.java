package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;


import br.com.sicredi.canaisdigitais.dto.protocolo.ComandoProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.transacao.OutTransacaoDTO;
import br.com.sicredi.canaisdigitais.enums.AcaoProtocoloEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CancelamentoAgendamentoDebitoTransacaoDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoRepository;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.*;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.RECURSO_NAO_ENCONTRADO;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/consulta_autorizacao.sql", "/db/instrucao_pagamento.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class CancelamentoAgendamentoDebitoProtocoloIntegrationTest extends AbstractIntegrationTest {

    private static final String CONTA = "223190";
    private static final String COOPERATIVA = "0101";
    private static final String ID_FIM_A_FIM = "E8785320620250506210800n2f1PQijN";
    private static final String ID_CANCELAMENTO_AGENDAMENTO = "CA1111111120240101qwertyuiopa";
    private static final String ID_IDEMPOTENCIA = "CA1111111120240101qwertyuiopa_CANCEL_DEB";
    private static final String TOPICO_ICOM_CAMT_ENVIO_V1 = "icom-camt-envio-v1";
    private static final String TOPICO_PROTOCOLO = "canaisdigitais-protocolo-comando-v1";
    private static final String TOPICO_CANCELAMENTO_AGENDAMENTO = "automatico-recorrente-instrucao-pagamento-cancelamento-protocolo-v1";

    private Consumer<String, String> consumer;

    @Autowired
    private RecorrenciaInstrucaoPagamentoCancelamentoRepository instrucaoPagamentoCancelamentoRepository;

    @Autowired
    private RecorrenciaInstrucaoPagamentoRepository instrucaoPagamentoRepository;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoPedidoCancelamento_quandoProcessarCancelamentoDebito_deveCriarCamt55EEnviarEvento() {
        var transacaoDTO = criarCancelamentoTransacaoDTO(ID_FIM_A_FIM, ID_CANCELAMENTO_AGENDAMENTO);
        var protocoloDTO = getProtocoloDTO(transacaoDTO.getIdCancelamentoAgendamento(), ObjectMapperUtil.converterObjetoParaString(transacaoDTO));

        enviarMensagem(protocoloDTO);

        validarSucessoCancelamento(ID_IDEMPOTENCIA, ID_FIM_A_FIM, ID_CANCELAMENTO_AGENDAMENTO, transacaoDTO.getCpfCnpjSolicitanteCancelamento());
    }

    @Test
    void dadoErroNegocioCpfDiferente_quandoProcessarCancelamentoDebito_deveCriarEventoErro() {
        var transacaoDTO = criarCancelamentoTransacaoDTO(ID_FIM_A_FIM, ID_CANCELAMENTO_AGENDAMENTO);

        transacaoDTO.setCpfCnpjSolicitanteCancelamento("14526886524");

        var protocoloDTO = getProtocoloDTO(transacaoDTO.getIdCancelamentoAgendamento(), ObjectMapperUtil.converterObjetoParaString(transacaoDTO));

        enviarMensagem(protocoloDTO);

        validarErroCancelamento(ID_IDEMPOTENCIA, ID_FIM_A_FIM, ID_CANCELAMENTO_AGENDAMENTO, DADOS_SOLICITANTE_DIFERENTE_DA_INSTRUCAO_PAGAMENTO.name(), DADOS_SOLICITANTE_DIFERENTE_DA_INSTRUCAO_PAGAMENTO.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
    }

    @Test
    void dadoErroConstraints_quandoProcessarCancelamentoDebito_deveCriarEventoErro() {
        var transacaoDTO = criarCancelamentoTransacaoDTO(ID_FIM_A_FIM, ID_CANCELAMENTO_AGENDAMENTO);

        transacaoDTO.setCpfCnpjSolicitanteCancelamento(null);

        var protocoloDTO = getProtocoloDTO(transacaoDTO.getIdCancelamentoAgendamento(), ObjectMapperUtil.converterObjetoParaString(transacaoDTO));

        enviarMensagem(protocoloDTO);

        validarErroCancelamento(ID_IDEMPOTENCIA, ID_FIM_A_FIM, ID_CANCELAMENTO_AGENDAMENTO, SPIRECORRENCIA_REC0001.name(), "Não foi possível processar sua solicitação. Por favor, tente novamente mais tarde.", TipoRetornoTransacaoEnum.ERRO_VALIDACAO);
    }

    @Test
    void dadoPayloadInvalido_quandoProcessarCancelamentoDebito_deveEnviarParaDLT() {
        var transacaoDTO = criarCancelamentoTransacaoDTO(ID_FIM_A_FIM, ID_CANCELAMENTO_AGENDAMENTO);

        var protocoloDTO = getProtocoloDTO(transacaoDTO.getIdCancelamentoAgendamento(), ObjectMapperUtil.converterObjetoParaString("teste"));

        enviarMensagem(protocoloDTO);

        var erro = verificarMensagemErroKafka();

        assertAll(
                () -> assertEquals(SPIRECORRENCIA_BU9001.name(), erro.getCodigo()),
                () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_INFRA, erro.getTipoRetorno()),
                () -> assertEquals("Não foi possível processar sua solicitação. Por favor, tente novamente mais tarde.", erro.getMensagem())
        );
    }

    private void validarErroCancelamento(String idIdempotencia, String idFimAFim, String idCancelamentoAgendamento, String code, String message, TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum) {
        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    var optInstrucao = instrucaoPagamentoRepository.buscarPorCodFimAFimComAutorizacao(idFimAFim);
                    optInstrucao.ifPresentOrElse(instrucaoPagamento ->
                                    assertAll(
                                            () -> assertEquals("AGUARDANDO_ENVIO", instrucaoPagamento.getTpoSubStatus())
                                    ),
                            () -> fail(RECURSO_NAO_ENCONTRADO)
                    );

                    var optInstrucaoCancelamento = instrucaoPagamentoCancelamentoRepository.findById(idCancelamentoAgendamento);
                    assertTrue(optInstrucaoCancelamento.isEmpty(), "O cancelamento não deveria estar presente no banco");


                    var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
                    var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

                    assertAll(
                            () -> assertEquals(1, idempotenteIn.size()),
                            () -> assertEquals(2, idempotenteOut.size())
                    );

                    var erro = verificarMensagemErroKafka();

                    assertAll(
                            () -> assertEquals(code, erro.getCodigo()),
                            () -> assertEquals(tipoRetornoTransacaoEnum, erro.getTipoRetorno()),
                            () -> assertEquals(message, erro.getMensagem())
                    );
                });
    }


    private void validarSucessoCancelamento(String idIdempotencia, String idFimAFim, String idCancelamentoAgendamento, String cpfSolicitante) {
        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    var optInstrucao = instrucaoPagamentoRepository.buscarPorCodFimAFimComAutorizacao(idFimAFim);
                    optInstrucao.ifPresentOrElse(instrucaoPagamento ->
                                    assertAll(
                                            () -> assertEquals("AGUARDANDO_CANCELAMENTO", instrucaoPagamento.getTpoSubStatus())
                                    ),
                            () -> fail(RECURSO_NAO_ENCONTRADO)
                    );

                    var optInstrucaoCancelamento = instrucaoPagamentoCancelamentoRepository.findById(idCancelamentoAgendamento);
                    optInstrucaoCancelamento.ifPresentOrElse(instrucaoPagamentoCancelamento ->
                                    assertAll(
                                            () -> assertEquals(ID_FIM_A_FIM, instrucaoPagamentoCancelamento.getCodFimAFim()),
                                            () -> assertEquals("CRIADA", instrucaoPagamentoCancelamento.getTpoStatus()),
                                            () -> assertEquals(cpfSolicitante, instrucaoPagamentoCancelamento.getNumCpfCnpjSolicitanteCancelamento()),
                                            () -> assertEquals(SOLICITADO_PELO_PAGADOR.name(), instrucaoPagamentoCancelamento.getCodMotivoCancelamento())
                                    ),
                            () -> fail(RECURSO_NAO_ENCONTRADO)
                    );

                    var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
                    var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

                    assertAll(
                            () -> assertEquals(1, idempotenteIn.size()),
                            () -> assertEquals(2, idempotenteOut.size())
                    );

                    var camt055Dto = verificarMensagemCamt55();

                    assertAll(
                            () -> assertEquals(idCancelamentoAgendamento, camt055Dto.getIdCancelamentoAgendamento()),
                            () -> assertEquals("idConciliacaoRecebedor", camt055Dto.getIdConciliacaoRecebedorOriginal()),
                            () -> assertEquals(cpfSolicitante, camt055Dto.getCpfCnpjUsuarioSolicitanteCancelamento()),
                            () -> assertEquals(SOLICITADO_PELO_PAGADOR.name(), camt055Dto.getMotivoCancelamento()),
                            () -> assertEquals(idFimAFim, camt055Dto.getIdFimAFimOriginal()),
                            () -> assertEquals(SOLICITADO_PELO_PAGADOR.name(), camt055Dto.getTipoSolicitacaoOuInformacao()),
                            () -> assertEquals("2025-05-21T16:58:03.236", camt055Dto.getDataHoraSolicitacaoOuInformacao().toString()),
                            () -> assertEquals("123", camt055Dto.getParticipanteSolicitanteDoCancelamento()),
                            () -> assertEquals("11", camt055Dto.getParticipanteDestinatarioDoCancelamento())
                    );
                });
    }

    private OutTransacaoDTO verificarMensagemErroKafka() {
        try {
            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(15), 2);
            for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> iteration : response.records(TOPICO_PROTOCOLO)) {
                var eventoErroSemProtocoloDTO = ObjectMapperUtil.converterStringParaObjeto(
                        iteration.value(), new TypeReference<ComandoProtocoloDTO>() {});
                if (AcaoProtocoloEnum.SINALIZAR_ERRO_PROCESSAMENTO.equals(eventoErroSemProtocoloDTO.getAcao())) {
                    return ObjectMapperUtil.converterStringParaObjeto(
                            ObjectMapperUtil.converterObjetoParaString(eventoErroSemProtocoloDTO.getPayload()), new TypeReference<>() {});
                }
            }
            return new OutTransacaoDTO();
        } catch (Exception ignore) {
            return new OutTransacaoDTO();
        }
    }

    private Camt055Dto verificarMensagemCamt55() {
        var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);

        for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> iteration : response.records(TOPICO_ICOM_CAMT_ENVIO_V1)) {
            var headers = iteration.headers();

            boolean temPspEmissor = headers.lastHeader("PSP_EMISSOR") != null;
            boolean temIdIdempotencia = headers.lastHeader("ID_IDEMPOTENCIA") != null;
            boolean temTipoMensagem = headers.lastHeader("TIPO_MENSAGEM") != null;

            if (!temTipoMensagem || !temPspEmissor || !temIdIdempotencia) {
                throw new IllegalArgumentException("Mensagem Kafka recebida está sem os headers obrigatórios: "
                                           + (temTipoMensagem ? "" : "[TIPO_MENSAGEM] ")
                                           + (temIdIdempotencia ? "" : "[ID_IDEMPOTENCIA] ")
                                           + (temPspEmissor ? "" : "[PSP_EMISSOR] "));
            }

            if (iteration.value() != null) {
                return ObjectMapperUtil.converterStringParaObjeto(iteration.value(), new TypeReference<Camt055Dto>() {
                });
            }
        }

        throw new AssertionError("Nenhuma mensagem válida foi recebida no tópico.");
    }

    private CancelamentoAgendamentoDebitoTransacaoDTO criarCancelamentoTransacaoDTO(String idFImAFim, String idCancelamentoAgendamento) {
        return CancelamentoAgendamentoDebitoTransacaoDTO.builder()
                .oidRecorrenciaAutorizacao(123L)
                .idFimAFim(idFImAFim)
                .idCancelamentoAgendamento(idCancelamentoAgendamento)
                .cpfCnpjSolicitanteCancelamento("78351090000")
                .motivoCancelamento(SOLICITADO_PELO_PAGADOR.name())
                .build();
    }

    private ProtocoloDTO getProtocoloDTO(String idCancelamentoAgendamento, String cancelamentoRequest) {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setIdProtocolo(1L);
        protocoloDTO.setCodigoTipoTransacao("codigoTransacao");
        protocoloDTO.setCooperativa(COOPERATIVA);
        protocoloDTO.setConta(CONTA);
        protocoloDTO.setIdentificadorTransacao(idCancelamentoAgendamento);
        protocoloDTO.setPayloadTransacao(cancelamentoRequest);
        protocoloDTO.setDataRequisicao(LocalDateTime.now());
        protocoloDTO.setDataAgendamento(LocalDateTime.now());
        protocoloDTO.setCodigoCanal(1);
        protocoloDTO.setIdentificadorUsuario(RandomStringUtils.randomNumeric(6));
        return protocoloDTO;
    }

    private void enviarMensagem(Object payload) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader("X-Ultima-Interacao", "2025-05-21 19:58:03.236+0000")
                .setHeader(KafkaHeaders.TOPIC, TOPICO_CANCELAMENTO_AGENDAMENTO)
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_PROTOCOLO, TOPICO_ICOM_CAMT_ENVIO_V1);

        Map<java.lang.String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }
}
