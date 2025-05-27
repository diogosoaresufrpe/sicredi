package io.sicredi.spirecorrencia.api.automatico.autorizacacaocancelamento;

import br.com.sicredi.canaisdigitais.dto.protocolo.ComandoProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.enums.AcaoProtocoloEnum;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11;
import br.com.sicredi.spi.entities.type.TipoFrequencia;
import br.com.sicredi.spi.entities.type.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamentoRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento.CancelamentoAutorizacaoRequest;
import io.sicredi.spirecorrencia.api.automatico.enums.*;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@SqlGroup(
        value = {
                @Sql(scripts = {
                        "/db/clear.sql",
                        "/db/autorizacao_recorrencia.sql",
                }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
@EnableAutoConfiguration(exclude = {
        BatchAutoConfiguration.class,
        TaskSchedulingAutoConfiguration.class,
})
@SpringBootTest(properties = "app.scheduling.enable=false")
class ProcessaAutorizacaoCancelamentoComProtocoloTest extends AbstractIntegrationTest {

    private static final String CPF_CNPJ_SOLICITANTE_CANCELAMENTO = "63262164004";
    private static final String CPF_CNPJ_PAGADOR = "63262164004";
    private static final String CPF_CNPJ_RECEBEDOR = "63262164005";
    private static final String COOPERATIVA = "0101";
    private static final String TOPICO_ICOM_PAIN_ENVIO_V1 = "icom-pain-envio-v1";
    private static final String ID_RECORRENCIA = "RN3428152120250425041bYqAj6ef";
    private static final String TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1 = "canaisdigitais-protocolo-comando-v1";
    private static final String TOPICO_CANCELAMENTO_AUTORIZACAO = "automatico-recorrente-autorizacao-cancelamento-protocolo-v1";
    private static final String ID_INFORMACAO_CANCELAMENTO = "ID_CANCELAMENTO";
    private static final String CONFIRMADA_POR_OUTRA_JORNADA = "CONFIRMADA_POR_OUTRA_JORNADA";
    private static final String ERRO_VALIDACAO_CONSTRAINT_DEFAULT = "Não foi possível processar sua solicitação. Por favor, tente novamente mais tarde.";
    private static final String CONTA_PAGADOR = "223190";

    @Autowired
    private RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;

    @Autowired
    private RecorrenciaAutorizacaoRepository recorrenciaAutorizacaoRepository;

    @ParameterizedTest(name = """
            Dado uma Request de solicitação de cancelamento invalida,
            Quando processarAutorizacaoCancelamento
            Deve enviar IdempotenciaRequest com error
            """)
    @MethodSource("dadosParaRequisicaoInvalida")
    void dadoUmRequestInvalida_quandoProcessarAutorizacaoCancelamento_deveEnviarIdempotenciaRequestComError(
            CancelamentoAutorizacaoRequest request,
            AppExceptionCode code,
            TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum,
            String mensagem) {

        var protocoloDTO = criaProtocolo(ID_RECORRENCIA, ObjectMapperUtil.converterObjetoParaString(request));
        try (var consumer = configureMessageConsumer(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1)) {
            enviarMensagem(protocoloDTO);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        var mensagens = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(2000), 2)
                                .records(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1);

                        var off = KafkaTestUtils.getEndOffsets(consumer, TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1)
                                .get(new TopicPartition(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1, 0)) - 1;
                        validaEventosDeErros(code, tipoRetornoTransacaoEnum, mensagem, mensagens, protocoloDTO, off);
                    });
        }
    }

    @ParameterizedTest(name = """
            Dado uma Request de solicitação de cancelamento valida, ocorre um erro de negocio {3},
            Quando processarAutorizacaoCancelamento
            Deve enviar IdempotenciaRequest com error
            """)
    @MethodSource("dadosParaRegraDeNegocioInvalida")
    void dadoUmRequestValidaOcorreErrorDeNegocio_quandoProcessarAutorizacaoCancelamento_deveEnviarIdempotenciaRequestComError(
            CancelamentoAutorizacaoRequest request,
            AppExceptionCode code,
            TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum,
            String mensagem) {

        if (request.getOidRecorrenciaAutorizacao() == 2) {
            var recorrenciaAutorizacao = criaRecorrenciaAutorizacao(TipoStatusAutorizacao.CANCELADA);
            recorrenciaAutorizacaoRepository.save(recorrenciaAutorizacao);
            request.setOidRecorrenciaAutorizacao(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao());
        } else if (request.getOidRecorrenciaAutorizacao() > 2) {
            var recorrenciaAutorizacao = criaRecorrenciaAutorizacao(TipoStatusAutorizacao.APROVADA);
            recorrenciaAutorizacaoRepository.save(recorrenciaAutorizacao);
            request.setOidRecorrenciaAutorizacao(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao());
        }

        var protocoloDTO = criaProtocolo(ID_RECORRENCIA, ObjectMapperUtil.converterObjetoParaString(request));
        try (var consumer = configureMessageConsumer(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1)) {
            enviarMensagem(protocoloDTO);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        var mensagens = KafkaTestUtils.getRecords(consumer, Duration.ofMillis(2000), 2)
                                .records(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1);
                        var off = KafkaTestUtils.getEndOffsets(consumer, TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1)
                                .get(new TopicPartition(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1, 0)) - 1;
                        validaEventosDeErros(code, tipoRetornoTransacaoEnum, mensagem, mensagens, protocoloDTO, off);
                    });
        }
    }

    @Test
    @DisplayName("""
            Dado uma requisição valida e passando nas regras de negocio
            Quando processar solicitação de cancelamento com protocolo
            Deve criar e salvar RecorrenciaAutorizacaoCancelamento, atualizar RecorrenciaAutorizacao e publicar os eventos
            """)
    void dadoUmRequestValida_quandoquandoProcessarAutorizacaoCancelamento_deveCriarRecorrenciaAutorizacaoCancelamentoEAtualizarRecorrenciaAutorizacaoEPublicarEventos() {
        var recorrenciaAutorizacao = criaRecorrenciaAutorizacao(TipoStatusAutorizacao.APROVADA);
        recorrenciaAutorizacaoRepository.save(recorrenciaAutorizacao);

        var request = criaCancelamentoAutorizacaoRecorrencia(recorrenciaAutorizacao);
        var protocoloDTO = criaProtocolo(ID_RECORRENCIA, ObjectMapperUtil.converterObjetoParaString(request));

        try (var consumer = configureMessageConsumer(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1)) {
            enviarMensagem(protocoloDTO);

            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        var mensage = getLastRecord(consumer, TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1);
                        var payload = ObjectMapperUtil.converterStringParaObjeto(mensage.value(), ComandoProtocoloDTO.class);
                        assertThat(payload.getAcao()).isEqualTo(AcaoProtocoloEnum.SINALIZAR_RECEPCAO);
                    });
        }

        var recorrenciaAtualizada = recorrenciaAutorizacaoRepository.findById(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao())
                .orElseThrow();

        assertThat(recorrenciaAtualizada)
                .extracting(
                        RecorrenciaAutorizacao::getTipoStatus,
                        RecorrenciaAutorizacao::getTipoSubStatus
                ).satisfies(tuple -> {
                    assertThat(tuple.get(0)).isEqualTo(TipoStatusAutorizacao.APROVADA);
                    assertThat(tuple.get(1)).isEqualTo(TipoSubStatus.AGUARDANDO_CANCELAMENTO.name());
                });

        var recorrenciaAutorizacaoCancelamento = recorrenciaAutorizacaoCancelamentoRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(recorrenciaAutorizacaoCancelamento)
                .extracting(
                        RecorrenciaAutorizacaoCancelamento::getIdInformacaoCancelamento,
                        RecorrenciaAutorizacaoCancelamento::getIdRecorrencia,
                        RecorrenciaAutorizacaoCancelamento::getTipoCancelamento,
                        RecorrenciaAutorizacaoCancelamento::getTipoSolicitanteCancelamento,
                        RecorrenciaAutorizacaoCancelamento::getTipoStatus,
                        RecorrenciaAutorizacaoCancelamento::getMotivoCancelamento,
                        RecorrenciaAutorizacaoCancelamento::getDataCancelamento
                )
                .satisfies(tuple -> {
                    assertThat(tuple.get(0)).isEqualTo(request.getIdInformacaoCancelamento());
                    assertThat(tuple.get(1)).isEqualTo(recorrenciaAtualizada.getIdRecorrencia());
                    assertThat(tuple.get(2)).isEqualTo(TipoCancelamento.RECORRENCIA_AUTORIZACAO);
                    assertThat(tuple.get(3)).isEqualTo(TipoSolicitanteCancelamento.PAGADOR);
                    assertThat(tuple.get(4)).isEqualTo(TipoStatusCancelamentoAutorizacao.CRIADA);
                    assertThat(tuple.get(5)).isEqualTo("SUPEITA_DE_FRAUDE");
                    assertThat(tuple.get(6)).isNotNull();
                });

        try (var consumer = configureMessageConsumer(TOPICO_ICOM_PAIN_ENVIO_V1)) {
            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        var mensage = getLastRecord(consumer, TOPICO_ICOM_PAIN_ENVIO_V1);
                        var payload = ObjectMapperUtil.converterStringParaObjeto(mensage.value(), Pain011Dto.class);

                        validaPain011(payload, request, recorrenciaAtualizada, recorrenciaAutorizacaoCancelamento);
                    });
        }
    }

    private static void validaPain011(Pain011Dto payload, CancelamentoAutorizacaoRequest request,
                                      RecorrenciaAutorizacao recorrenciaAtualizada,
                                      RecorrenciaAutorizacaoCancelamento recorrenciaAutorizacaoCancelamento) {
        assertThat(payload)
                .extracting(
                        Pain011Dto::getCpfCnpjSolicitanteCancelamento,
                        Pain011Dto::getMotivoCancelamento,
                        Pain011Dto::getIdRecorrencia,
                        Pain011Dto::getIdInformacaoCancelamento,
                        Pain011Dto::getTipoRecorrencia,
                        Pain011Dto::getTipoFrequencia,
                        Pain011Dto::getDataFinalRecorrencia,
                        Pain011Dto::getValor,
                        Pain011Dto::getDataInicialRecorrencia,
                        Pain011Dto::getCpfCnpjUsuarioRecebedor,
                        Pain011Dto::getContaUsuarioPagador
                )
                .satisfies(tuple -> {
                    assertThat(tuple.get(0)).isEqualTo(request.getCpfCnpjSolicitanteCancelamento());
                    assertThat(tuple.get(1)).isEqualTo(request.getMotivoCancelamento());
                    assertThat(tuple.get(2)).isEqualTo(recorrenciaAtualizada.getIdRecorrencia());
                    assertThat(tuple.get(3)).isEqualTo(recorrenciaAutorizacaoCancelamento.getIdInformacaoCancelamento());
                    assertThat(tuple.get(4)).isEqualTo(TipoRecorrencia.RECORRENTE.name());
                    assertThat(tuple.get(5)).isEqualTo(TipoFrequencia.MENSAL.name());
                    assertThat(tuple.get(6)).isEqualTo(recorrenciaAtualizada.getDataFinalRecorrencia());
                    assertThat(tuple.get(7)).isEqualTo(recorrenciaAtualizada.getValor());
                    assertThat(tuple.get(8)).isEqualTo(recorrenciaAtualizada.getDataInicialRecorrencia());
                    assertThat(tuple.get(9)).isEqualTo(recorrenciaAtualizada.getCpfCnpjRecebedor());
                    assertThat(tuple.get(10)).isEqualTo(recorrenciaAtualizada.getContaPagador());
                });
    }

    private static CancelamentoAutorizacaoRequest criaCancelamentoAutorizacaoRecorrencia(RecorrenciaAutorizacao recorrenciaAutorizacao) {
        return CancelamentoAutorizacaoRequest.builder()
                .oidRecorrenciaAutorizacao(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao())
                .idInformacaoCancelamento(ID_INFORMACAO_CANCELAMENTO)
                .motivoCancelamento(MotivoCancelamentoPain11.SUPEITA_DE_FRAUDE.name())
                .cpfCnpjSolicitanteCancelamento(CPF_CNPJ_SOLICITANTE_CANCELAMENTO)
                .build();
    }

    private void validaEventosDeErros(AppExceptionCode code,
                                             TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum,
                                             String mensagem,
                                             Iterable<ConsumerRecord<String, String>> mensagens,
                                             ProtocoloDTO protocoloDTO,
                                             long offSet) {
        // offSet utilizado como ponteiro para a lista de mensagens
        var listaDeEventos = StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(mensagens.iterator(), Spliterator.ORDERED), false)
                .filter(men -> men.offset() == offSet || men.offset() == offSet - 1)
                .map(consumerRecord -> ObjectMapperUtil.converterStringParaObjeto(consumerRecord.value(), ComandoProtocoloDTO.class))
                .toList();

        listaDeEventos.stream()
                .filter(comandoProtocoloDTO -> comandoProtocoloDTO.getAcao() == AcaoProtocoloEnum.SINALIZAR_RECEPCAO)
                .findFirst()
                .ifPresentOrElse(
                        comandoProtocoloDTO ->
                            assertThat(comandoProtocoloDTO)
                                    .extracting(
                                            ComandoProtocoloDTO::getConta,
                                            ComandoProtocoloDTO::getCooperativa,
                                            ComandoProtocoloDTO::getIdentificadorTransacao,
                                            ComandoProtocoloDTO::getCodigoCanal
                                    )
                                    .satisfies(tuple -> {
                                        assertThat(tuple.get(0)).isEqualTo(protocoloDTO.getConta());
                                        assertThat(tuple.get(1)).isEqualTo(protocoloDTO.getCooperativa());
                                        assertThat(tuple.get(2)).isEqualTo(protocoloDTO.getIdentificadorTransacao());
                                        assertThat(tuple.get(3)).isEqualTo(protocoloDTO.getCodigoCanal());
                                    })
                        ,() -> fail("Sinalização de recepção deveria ser encontrada")
                );

        listaDeEventos.stream()
                .filter(comandoProtocoloDTO -> comandoProtocoloDTO.getAcao() == AcaoProtocoloEnum.SINALIZAR_ERRO_PROCESSAMENTO)
                .findFirst()
                .ifPresentOrElse(
                        comandoProtocoloDTO ->
                            assertThat(comandoProtocoloDTO.getPayload())
                                    .isNotNull()
                                    .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
                                    .extracting(
                                            m -> m.get("mensagem"),
                                            m -> m.get("codigo").toString(),
                                            m -> m.get("tipoRetorno").toString()

                                    )
                                    .containsExactly(mensagem, code.toString(), tipoRetornoTransacaoEnum.toString())
                        ,() -> fail("Sinalização de erro de processamento deveria ser encontrada")
                );
    }

    private Consumer<String, String> configureMessageConsumer(String topico) {
        return configurarConsumer(
                "latest",
                Boolean.TRUE,
                topico
        );
    }

    private void enviarMensagem(Object payload) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, TOPICO_CANCELAMENTO_AUTORIZACAO)
                .setHeader("X-Ultima-Interacao", "2025-05-05 16:02:30.160-0300")
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private static RecorrenciaAutorizacao criaRecorrenciaAutorizacao(TipoStatusAutorizacao statusAutorizacao) {
        return RecorrenciaAutorizacao.builder()
                .idRecorrencia(ID_RECORRENCIA)
                .idInformacaoStatusEnvio("id_informacao_status_envio")
                .tipoJornada("JORNADA_1")
                .permiteLinhaCredito("N")
                .permiteRetentativa("N")
                .permiteNotificacaoAgendamento("N")
                .cpfCnpjPagador(CPF_CNPJ_PAGADOR)
                .nomePagador("nome pagador")
                .agenciaPagador("0101")
                .valor(BigDecimal.TEN)
                .pisoValorMaximo(BigDecimal.TEN)
                .tipoContaPagador("CORRENTE")
                .tipoFrequencia(TipoFrequencia.MENSAL.name())
                .tipoPessoaPagador(TipoPessoaEnum.PF)
                .contaPagador(CONTA_PAGADOR)
                .tipoStatus(statusAutorizacao)
                .instituicaoPagador("12121212")
                .postoPagador("12")
                .tipoSistemaPagador(OrigemEnum.LEGADO)
                .tipoCanalPagador(TipoCanalEnum.SICREDI_X)
                .nomeRecebedor("nome recebedor")
                .instituicaoRecebedor("121")
                .cpfCnpjRecebedor(CPF_CNPJ_RECEBEDOR)
                .nomeDevedor("nome_devedor")
                .cpfCnpjDevedor(CPF_CNPJ_PAGADOR)
                .descricao("descricao")
                .numeroContrato("12331223-1")
                .dataInicialRecorrencia(LocalDate.now())
                .dataInicioConfirmacao(LocalDateTime.now())
                .dataFinalRecorrencia(LocalDate.now().plusMonths(1L))
                .dataCriacaoRecorrencia(LocalDateTime.now())
                .codigoMunicipioIBGE("4313466")
                .build();
    }

    private ProtocoloDTO criaProtocolo(String idRecorrencia, String payload) {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setIdProtocolo(1L);
        protocoloDTO.setIdentificadorTransacao(idRecorrencia);
        protocoloDTO.setIdentificadorSimulacaoLimite(UUID.randomUUID().toString());
        protocoloDTO.setPayloadTransacao(payload);
        protocoloDTO.setDataRequisicao(LocalDateTime.now());
        protocoloDTO.setDataAgendamento(LocalDateTime.now());
        protocoloDTO.setCodigoTipoTransacao("codigoTransacao");
        protocoloDTO.setCooperativa(COOPERATIVA);
        protocoloDTO.setConta(CONTA_PAGADOR);
        protocoloDTO.setCodigoCanal(441);
        protocoloDTO.setIdentificadorUsuario(RandomStringUtils.randomNumeric(6));
        return protocoloDTO;
    }

    private static Stream<Arguments> dadosParaRequisicaoInvalida() {
        return Stream.of(
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .idInformacaoCancelamento(ID_INFORMACAO_CANCELAMENTO)
                                .motivoCancelamento(CONFIRMADA_POR_OUTRA_JORNADA)
                                .cpfCnpjSolicitanteCancelamento(CPF_CNPJ_SOLICITANTE_CANCELAMENTO)
                                .build(),
                        AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA,
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO,
                        ERRO_VALIDACAO_CONSTRAINT_DEFAULT
                ),
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .oidRecorrenciaAutorizacao(1L)
                                .motivoCancelamento(CONFIRMADA_POR_OUTRA_JORNADA)
                                .cpfCnpjSolicitanteCancelamento(CPF_CNPJ_SOLICITANTE_CANCELAMENTO)
                                .build(),
                        AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA,
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO,
                        ERRO_VALIDACAO_CONSTRAINT_DEFAULT
                ),
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .oidRecorrenciaAutorizacao(1L)
                                .idInformacaoCancelamento(ID_INFORMACAO_CANCELAMENTO)
                                .cpfCnpjSolicitanteCancelamento(CPF_CNPJ_SOLICITANTE_CANCELAMENTO)
                                .build(),
                        AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA,
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO,
                        ERRO_VALIDACAO_CONSTRAINT_DEFAULT
                ),
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .oidRecorrenciaAutorizacao(1L)
                                .motivoCancelamento(CONFIRMADA_POR_OUTRA_JORNADA)
                                .build(),
                        AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA,
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO,
                        ERRO_VALIDACAO_CONSTRAINT_DEFAULT
                ),
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .oidRecorrenciaAutorizacao(1L)
                                .motivoCancelamento(CONFIRMADA_POR_OUTRA_JORNADA)
                                .cpfCnpjSolicitanteCancelamento("cpf_invalido")
                                .build(),
                        AppExceptionCode.SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA,
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO,
                        ERRO_VALIDACAO_CONSTRAINT_DEFAULT
                )
        );
    }

    private static Stream<Arguments> dadosParaRegraDeNegocioInvalida() {
        return Stream.of(
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .oidRecorrenciaAutorizacao(0L)
                                .idInformacaoCancelamento(ID_INFORMACAO_CANCELAMENTO)
                                .motivoCancelamento(CONFIRMADA_POR_OUTRA_JORNADA)
                                .cpfCnpjSolicitanteCancelamento(CPF_CNPJ_SOLICITANTE_CANCELAMENTO)
                                .build(),
                        AppExceptionCode.AUTORIZACAO_NAO_ENCONTRADA,
                        TipoRetornoTransacaoEnum.ERRO_NEGOCIO,
                        AppExceptionCode.AUTORIZACAO_NAO_ENCONTRADA.getMessage()
                ),
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .oidRecorrenciaAutorizacao(2L)
                                .idInformacaoCancelamento(ID_INFORMACAO_CANCELAMENTO)
                                .motivoCancelamento(CONFIRMADA_POR_OUTRA_JORNADA)
                                .cpfCnpjSolicitanteCancelamento(CPF_CNPJ_SOLICITANTE_CANCELAMENTO)
                                .build(),
                        AppExceptionCode.RECORRENCIA_COM_STATUS_DIFERENTE_DE_APROVADA,
                        TipoRetornoTransacaoEnum.ERRO_NEGOCIO,
                        AppExceptionCode.RECORRENCIA_COM_STATUS_DIFERENTE_DE_APROVADA.getMessage()
                ),
                Arguments.of(
                        CancelamentoAutorizacaoRequest.builder()
                                .oidRecorrenciaAutorizacao(3L)
                                .idInformacaoCancelamento(ID_INFORMACAO_CANCELAMENTO)
                                .motivoCancelamento(CONFIRMADA_POR_OUTRA_JORNADA)
                                .cpfCnpjSolicitanteCancelamento("63262164009")
                                .build(),
                        AppExceptionCode.SOLICITANTE_DO_CANCELAMENTO_DIFERENTE,
                        TipoRetornoTransacaoEnum.ERRO_NEGOCIO,
                        AppExceptionCode.SOLICITANTE_DO_CANCELAMENTO_DIFERENTE.getMessage())

        );
    }
}
