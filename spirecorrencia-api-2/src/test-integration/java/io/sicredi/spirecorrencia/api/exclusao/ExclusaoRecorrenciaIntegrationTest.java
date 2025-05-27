package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ComandoProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.transacao.OutTransacaoDTO;
import br.com.sicredi.canaisdigitais.enums.AcaoProtocoloEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaParcelaTransacaoDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.CODIGO_PROTOCOLO_EXCLUSAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.CODIGO_PROTOCOLO_EXCLUSAO_INTEGRADA;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/data_exclusao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class ExclusaoRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPICO_EXCLUSAO = "agendado-recorrente-exclusao-protocolo-v1";
    private static final String TOPICO_PROTOCOLO = "canaisdigitais-protocolo-comando-v1";
    private static final String TOPICO_EXCLUSAO_RETORNO = "spirecorrencia-gestao-api-excluidos-v1";
    private static final String TOPICO_NOTIFICACAO = "spi-notificacao-recorrencia-v2";
    private static final String IDEMPOTENCE_IN = "idempotenteIn";
    private static final String IDEMPOTENCE_OUT = "idempotenteOut";
    private static final String ID_RECORRENCIA = "2f335153-4d6a-4af1-92a0-c52c5c827af9";
    private static final String RECORRENCIA_NAO_ENCONTRADA = "Recorrencia com id %s não encontrado";
    private static final String EVENTOS = "eventos";

    @Autowired
    protected RecorrenciaRepository recorrenciaRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    @DisplayName("Dado uma recorrência válida, quando excluir recorrência, então deve executar com sucesso")
    void dadoRecorrenciaValida_quandoExcluirRecorrencia_deveExecutarComSucesso() {
        var idRecorrencia = ID_RECORRENCIA;
        var exclusaoRecorrenciaProtocoloRequest = criarExclusaoRecorrenciaRequestDTO(idRecorrencia);
        var protocoloDTO = getProtocoloDTO(idRecorrencia, ObjectMapperUtil.converterObjetoParaString(exclusaoRecorrenciaProtocoloRequest));

        enviarMensagem(protocoloDTO);

        validarOcorrenciaErro(idRecorrencia);
    }

    @Test
    @DisplayName("Dado uma recorrência válida, quando excluir recorrência pelo fluxo de liquidação, então deve executar com sucesso sem gerar novo evento")
    void dadoRecorrenciaValidaParaLiquidacao_quandoExcluirRecorrencia_deveExecutarComSucesso() {
        var idRecorrencia = ID_RECORRENCIA;
        var exclusaoRecorrenciaProtocoloRequest = criarExclusaoRecorrenciaRequestDTO(idRecorrencia);
        var protocoloDTO = getProtocoloDTO(idRecorrencia, ObjectMapperUtil.converterObjetoParaString(exclusaoRecorrenciaProtocoloRequest));
        protocoloDTO.setCodigoTipoTransacao(CODIGO_PROTOCOLO_EXCLUSAO_INTEGRADA);

        enviarMensagem(protocoloDTO);

        validarOcorrenciaErro(idRecorrencia);
    }

    @Test
    @DisplayName("Dado uma recorrência válida, quando excluir recorrência pelo fluxo de liquidação, então deve atualizar para CONCLUIDO e enviar notificação de conclusão")
    void dadoRecorrenciaValidaParaLiquidacao_quandoExcluirRecorrencia_deveProduzirMensagemNotificacao() {
        var idRecorrencia = "0y335153-4d6a-4af1-92a0-c52c5c827a9t";
        var exclusaoRecorrenciaProtocoloRequest = criarExclusaoRecorrenciaRequestDTO(idRecorrencia);
        exclusaoRecorrenciaProtocoloRequest.getFirst().setIdentificadorParcela("7DDF2A2BCF9FE8B2E063C50A17ACB23A");
        var protocoloDTO = getProtocoloDTO(idRecorrencia, ObjectMapperUtil.converterObjetoParaString(exclusaoRecorrenciaProtocoloRequest));
        protocoloDTO.setCodigoTipoTransacao(CODIGO_PROTOCOLO_EXCLUSAO_INTEGRADA);

        enviarMensagem(protocoloDTO);

        validaFluxoExclusaoLiquidacaoParcela(idRecorrencia, TipoStatusEnum.CONCLUIDO, 4);
    }

    private void validaFluxoExclusaoLiquidacaoParcela(String idRecorrencia, TipoStatusEnum tipoStatusEnum, int eventos) {
        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(1))
                .untilAsserted(() -> {
            var optRecorrencia = recorrenciaRepository.findByIdRecorrencia(idRecorrencia);
            optRecorrencia.ifPresentOrElse(recorrencia ->
                            assertAll(
                                    () -> assertEquals(idRecorrencia, recorrencia.getIdRecorrencia()),
                                    () -> assertEquals(tipoStatusEnum, recorrencia.getTipoStatus()),
                                    () -> assertEquals(TipoStatusEnum.EXCLUIDO, recorrencia.getRecorrencias().getFirst().getTpoStatus()),
                                    () -> assertNotNull(recorrencia.getRecorrencias().getFirst().getDataExclusao())
                            ),
                    () -> fail(String.format(RECORRENCIA_NAO_ENCONTRADA, idRecorrencia))
            );

            var idempotenteIn = buscarRegistrosIdempotenteIn(idRecorrencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idRecorrencia);
            assertAll(
                    () -> assertEquals(1, idempotenteIn.size(), IDEMPOTENCE_IN),
                    () -> assertEquals(eventos, idempotenteOut.size(), IDEMPOTENCE_OUT)
            );

            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 4);
            assertEquals(eventos, response.count(), EVENTOS);
        });
    }

    @Test
    @DisplayName("Dado uma recorrência válida, quando excluir recorrência pelo fluxo de liquidação, deve atualizar recorrência para EXCLUIDO, mas não enviar notificação pois é AGENDADO")
    void dadoRecorrenciaValidaParaLiquidacao_quandoExcluirRecorrencia_deveNaoProduzirMensagemNotificacao() {
        var idRecorrencia = ID_RECORRENCIA;
        var exclusaoRecorrenciaProtocoloRequest = criarExclusaoRecorrenciaRequestDTO(idRecorrencia);
        exclusaoRecorrenciaProtocoloRequest.getFirst().setIdentificadorParcela("2DDF2A2BCF9FE8B2E063C50A17ACB3A4");
        var protocoloDTO = getProtocoloDTO(idRecorrencia, ObjectMapperUtil.converterObjetoParaString(exclusaoRecorrenciaProtocoloRequest));
        protocoloDTO.setCodigoTipoTransacao(CODIGO_PROTOCOLO_EXCLUSAO_INTEGRADA);

        enviarMensagem(protocoloDTO);

        validaFluxoExclusaoLiquidacaoParcela(idRecorrencia, TipoStatusEnum.EXCLUIDO, 3);
    }

    @Test
    @DisplayName("Dado uma recorrência não existente, quando excluir recorrência, então deve retornar erro de negócio")
    void dadoRecorrenciaNaoExistente_quandoExcluirRecorrencia_deveRetornarErroNegocio() {
        var idRecorrencia = "2f335153-4d632a-4af1-92a0-c52c5c827af1";
        var exclusaoRecorrenciaProtocoloRequest = criarExclusaoRecorrenciaRequestDTO(idRecorrencia);
        var protocoloDTO = getProtocoloDTO(idRecorrencia, ObjectMapperUtil.converterObjetoParaString(exclusaoRecorrenciaProtocoloRequest));
        enviarMensagem(protocoloDTO);

        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(1))
                .untilAsserted(() -> {
            var idempotenteIn = buscarRegistrosIdempotenteIn(idRecorrencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idRecorrencia);
            assertAll(
                    () -> assertEquals(1, idempotenteIn.size(), IDEMPOTENCE_IN),
                    () -> assertEquals(2, idempotenteOut.size(), IDEMPOTENCE_OUT)
            );

            var erro = verificarMensagemErroKafka();

            assertAll(
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, erro.getTipoRetorno()),
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0010.getCode(), erro.getCodigo()),
                    () -> assertEquals("Não foi possível localizar sua recorrência. Por favor, revise as informações e tente novamente.", erro.getMensagem())
            );
        });
    }

    @Test
    @DisplayName("Dado constraints inválidas, quando excluir recorrência, então deve retornar erro de validação")
    void dadoConstraintsInvalidas_quandoExcluirRecorrencia_deveRetornarErroValidacao() {
        var idRecorrencia = UUID.randomUUID().toString();
        var exclusaoRecorrenciaProtocoloRequest = criarExclusaoRecorrenciaRequestDTO(idRecorrencia);
        exclusaoRecorrenciaProtocoloRequest.getFirst().setTipoMotivoExclusao(null);
        var protocoloDTO = getProtocoloDTO(idRecorrencia, ObjectMapperUtil.converterObjetoParaString(exclusaoRecorrenciaProtocoloRequest));

        enviarMensagem(protocoloDTO);

        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(1))
                .untilAsserted(() -> {
            var idempotenteIn = buscarRegistrosIdempotenteIn(idRecorrencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idRecorrencia);
            assertAll(
                    () -> assertEquals(1, idempotenteIn.size(), IDEMPOTENCE_IN),
                    () -> assertEquals(2, idempotenteOut.size(), IDEMPOTENCE_OUT)
            );
            var erro = verificarMensagemErroKafka();
            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001.name(), erro.getCodigo()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_VALIDACAO, erro.getTipoRetorno()),
                    () -> assertEquals("Não foi possível processar sua solicitação. Por favor, tente novamente mais tarde.", erro.getMensagem())
            );
        });
    }

    private List<ExclusaoRecorrenciaParcelaTransacaoDTO> criarExclusaoRecorrenciaRequestDTO(String idRecorrencia) {
        return List.of(
                new ExclusaoRecorrenciaParcelaTransacaoDTO("2DDF2A2BCF9FE8B2E063C50A17ACB3A4", idRecorrencia, TipoMotivoExclusao.SOLICITADO_USUARIO)
        );
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_PROTOCOLO, TOPICO_EXCLUSAO_RETORNO, TOPICO_NOTIFICACAO);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

    private void enviarMensagem(Object payload) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, TOPICO_EXCLUSAO)
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private ProtocoloDTO getProtocoloDTO(String idRecorrencia, String exclusaoRecorrenciaRequestDTO) {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setIdProtocolo(1L);
        protocoloDTO.setIdentificadorTransacao(idRecorrencia);
        protocoloDTO.setPayloadTransacao(exclusaoRecorrenciaRequestDTO);
        protocoloDTO.setDataRequisicao(LocalDateTime.now());
        protocoloDTO.setCodigoTipoTransacao(CODIGO_PROTOCOLO_EXCLUSAO);
        protocoloDTO.setCooperativa("1234");
        protocoloDTO.setConta("123456");
        protocoloDTO.setCodigoCanal(1);
        return protocoloDTO;
    }

    private OutTransacaoDTO verificarMensagemErroKafka() {
        try {
            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);
            var recordIterator = response.records(TOPICO_PROTOCOLO).iterator();
            while (recordIterator.hasNext()) {
                var iteration = recordIterator.next();
                var eventoErroSemProtocoloDTO = ObjectMapperUtil.converterStringParaObjeto(
                        iteration.value(), new TypeReference<ComandoProtocoloDTO>() {
                        });
                if (AcaoProtocoloEnum.SINALIZAR_ERRO_PROCESSAMENTO.equals(eventoErroSemProtocoloDTO.getAcao())) {
                    return ObjectMapperUtil.converterStringParaObjeto(
                            ObjectMapperUtil.converterObjetoParaString(eventoErroSemProtocoloDTO.getPayload()), new TypeReference<>() {
                            });
                }
            }
            return new OutTransacaoDTO();
        } catch (Exception ignore) {
            return new OutTransacaoDTO();
        }
    }

    private void validarOcorrenciaErro(String idRecorrencia) {
        await().atMost(10, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(1))
                .untilAsserted(() -> {
            var optRecorrencia = recorrenciaRepository.findByIdRecorrencia(idRecorrencia);
            optRecorrencia.ifPresentOrElse(recorrencia ->
                            assertAll(
                                    () -> assertEquals(idRecorrencia, recorrencia.getIdRecorrencia()),
                                    () -> assertEquals(TipoStatusEnum.EXCLUIDO, recorrencia.getTipoStatus()),
                                    () -> assertEquals(TipoStatusEnum.EXCLUIDO, recorrencia.getRecorrencias().getFirst().getTpoStatus()),
                                    () -> assertNotNull(recorrencia.getRecorrencias().getFirst().getDataExclusao())
                            ),
                    () -> fail(String.format(RECORRENCIA_NAO_ENCONTRADA, idRecorrencia))
            );

            var idempotenteIn = buscarRegistrosIdempotenteIn(idRecorrencia);
            var idempotenteOut = buscarRegistrosIdempotenteOut(idRecorrencia);
            assertAll(
                    () -> assertEquals(1, idempotenteIn.size(), IDEMPOTENCE_IN),
                    () -> assertEquals(3, idempotenteOut.size(), IDEMPOTENCE_OUT)
            );

            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 3);
            assertEquals(3, response.count(), EVENTOS);
        });
    }
}
