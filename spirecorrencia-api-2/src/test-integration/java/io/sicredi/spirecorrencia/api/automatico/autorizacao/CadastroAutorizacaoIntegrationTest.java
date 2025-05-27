package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ComandoProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.transacao.OutTransacaoDTO;
import br.com.sicredi.canaisdigitais.enums.AcaoProtocoloEnum;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CadastroAutorizacaoRecorrenciaTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
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

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static br.com.sicredi.spi.entities.type.StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.AUTORIZACAO_JA_APROVADA_ANTERIORMENTE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/autorizacao_recorrencia.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class CadastroAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String CONTA = "223190";
    private static final String COOPERATIVA = "0101";
    private static final String TOPICO_CADASTRO_AUTORIZACAO = "automatico-recorrente-autorizacao-cadastro-protocolo-v1";
    private static final String TOPICO_ICOM_PAIN_ENVIO_V1 = "icom-pain-envio-v1";
    private static final String ID_RECORRENCIA = "RN3428152120250425041bYqAj6ef";
    private static final String RECURSO_NAO_ENCONTRADO = "Recurso não encontrado para os dados informados";
    private static final String TOPICO_PROTOCOLO = "canaisdigitais-protocolo-comando-v1";
    private static final String ID_IDEMPOTENCIA = "RR9999900420250517G4Jdhm5o9A6_CADASTRO_AUTN";

    @Autowired
    private RecorrenciaAutorizacaoRepository autorizacaoRepository;

    @Autowired
    private RecorrenciaAutorizacaoPagamentoImediatoRepository pagamentoImediatoRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Test
    void dadoDadosValidosEJornada2_quandoConsumirMensagem_deveCriarAutorizacaoEDispararEventos() {
        var cadastroAutorizacaoRequest = criarAutorizacaoCadastro();
        var protocoloDTO = getProtocoloDTO(cadastroAutorizacaoRequest.getIdRecorrencia(), ObjectMapperUtil.converterObjetoParaString(cadastroAutorizacaoRequest));
        var identificadorRecorrencia = cadastroAutorizacaoRequest.getIdRecorrencia();

        enviarMensagem(protocoloDTO);

        validarCadastroSucesso(ID_IDEMPOTENCIA, identificadorRecorrencia, cadastroAutorizacaoRequest, false);
    }

    @Test
    void dadoDadosValidosEJornada3_quandoConsumirMensagem_deveCriarAutorizacaoEPagamentoImediatoEDispararEventos() {
        var cadastroAutorizacaoRequest = criarAutorizacaoCadastro();

        cadastroAutorizacaoRequest.setTipoJornada(TipoJornada.JORNADA_3);

        var protocoloDTO = getProtocoloDTO(cadastroAutorizacaoRequest.getIdRecorrencia(), ObjectMapperUtil.converterObjetoParaString(cadastroAutorizacaoRequest));
        var identificadorRecorrencia = cadastroAutorizacaoRequest.getIdRecorrencia();

        enviarMensagem(protocoloDTO);

        validarCadastroSucesso(ID_IDEMPOTENCIA, identificadorRecorrencia, cadastroAutorizacaoRequest, true);
    }

    @Test
    void dadoAutorizacaoJaAprovada_quandoConsumirMensagem_deveDispararEventosDeErroNegocio() {
        var cadastroAutorizacaoRequest = criarAutorizacaoCadastro();

        cadastroAutorizacaoRequest.setIdRecorrencia("RN4118152120250425041bYqAj6ef");

        var protocoloDTO = getProtocoloDTO(cadastroAutorizacaoRequest.getIdRecorrencia(), ObjectMapperUtil.converterObjetoParaString(cadastroAutorizacaoRequest));

        enviarMensagem(protocoloDTO);

        var erro = verificarMensagemErroKafka();

        assertAll(
                () -> assertEquals(AUTORIZACAO_JA_APROVADA_ANTERIORMENTE.name(), erro.getCodigo()),
                () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, erro.getTipoRetorno()),
                () -> assertEquals(AUTORIZACAO_JA_APROVADA_ANTERIORMENTE.getMessage(), erro.getMensagem())
        );
    }

    @Test
    void dadoPayloadInvalido_quandoConsumirMensagem_deveEnviarParaDLT() {
        var protocoloDTO = getProtocoloDTO(ID_RECORRENCIA, ObjectMapperUtil.converterObjetoParaString(""));

        enviarMensagem(protocoloDTO);

        var erro = verificarMensagemErroKafka();

        assertAll(
                () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_BU9001.name(), erro.getCodigo()),
                () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_INFRA, erro.getTipoRetorno()),
                () -> assertEquals("Não foi possível processar sua solicitação. Por favor, tente novamente mais tarde.", erro.getMensagem())
        );
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

    private void validarCadastroSucesso(String idIdempotencia, String idRecorrencia, CadastroAutorizacaoRecorrenciaTransacaoDTO transacao, boolean comPagamentoImediato) {
        await().atMost(15, TimeUnit.SECONDS)
                .pollDelay(Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    var optAutorizacao = autorizacaoRepository.findByIdInformacaoStatusEnvio(transacao.getIdInformacaoStatus());
                    optAutorizacao.ifPresentOrElse(autorizacao ->
                                    assertAll(
                                            () -> assertEquals(idRecorrencia, autorizacao.getIdRecorrencia()),
                                            () -> assertEquals(transacao.getContrato(), autorizacao.getNumeroContrato()),
                                            () -> assertEquals(TipoStatusAutorizacao.CRIADA, autorizacao.getTipoStatus()),
                                            () -> assertEquals(transacao.getCodigoMunicipioIbge(), autorizacao.getCodigoMunicipioIBGE()),
                                            () -> assertEquals(transacao.getValor(), autorizacao.getValor()),
                                            () -> assertEquals(transacao.getValorMaximo(), autorizacao.getValorMaximo()),
                                            () -> assertEquals(transacao.getPisoValorMaximo(), autorizacao.getPisoValorMaximo()),
                                            () -> assertEquals(transacao.getCpfCnpjConta(), autorizacao.getCpfCnpjPagador()),
                                            () -> assertEquals(transacao.getNomeSolicitante(), autorizacao.getNomePagador()),
                                            () -> assertEquals(transacao.getCooperativa(), autorizacao.getAgenciaPagador()),
                                            () -> assertEquals(transacao.getConta(), autorizacao.getContaPagador()),
                                            () -> assertEquals(transacao.getAgencia(), autorizacao.getPostoPagador()),
                                            () -> assertEquals(transacao.getTipoPessoaConta().name(), autorizacao.getTipoPessoaPagador().name()),
                                            () -> assertEquals(transacao.getCanal(), autorizacao.getTipoCanalPagador().name()),
                                            () -> assertEquals(transacao.getNomeRecebedor(), autorizacao.getNomeRecebedor()),
                                            () -> assertEquals(transacao.getInstituicaoRecebedor(), autorizacao.getInstituicaoRecebedor()),
                                            () -> assertEquals(transacao.getCpfCnpjRecebedor(), autorizacao.getCpfCnpjRecebedor()),
                                            () -> assertEquals(transacao.getNomeDevedor(), autorizacao.getNomeDevedor()),
                                            () -> assertEquals(transacao.getCpfCnpjDevedor(), autorizacao.getCpfCnpjDevedor()),
                                            () -> assertEquals(transacao.getObjeto(), autorizacao.getDescricao()),
                                            () -> assertEquals(transacao.getDataInicialRecorrencia(), autorizacao.getDataInicialRecorrencia()),
                                            () -> assertEquals(transacao.getDataFinalRecorrencia(), autorizacao.getDataFinalRecorrencia()),
                                            () -> assertEquals(transacao.getDataCriacaoRecorrencia().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")), autorizacao.getDataCriacaoRecorrencia().toString()),
                                            () -> assertEquals("01181521", autorizacao.getInstituicaoPagador()),
                                            () -> assertEquals(transacao.getOrigemConta(), autorizacao.getTipoSistemaPagador()),
                                            () -> assertEquals(transacao.getTipoContaPagador().name(), autorizacao.getTipoContaPagador())
                                    ),
                            () -> fail(RECURSO_NAO_ENCONTRADO)
                    );

                    if (comPagamentoImediato) {
                        var optPagamentoImediato = pagamentoImediatoRepository.findById(transacao.getIdFimAFimPagamentoImediato());

                        optPagamentoImediato.ifPresentOrElse(pagamentoImediato ->
                                        assertAll(
                                                () -> assertEquals(transacao.getDataRecebimentoConfirmacaoPacs002PagamentoImediato().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")), pagamentoImediato.getDataRecebimentoConfirmacao().toString()),
                                                () -> assertEquals(transacao.getIdRecorrencia(), pagamentoImediato.getIdRecorrencia())
                                        ),
                                () -> fail(RECURSO_NAO_ENCONTRADO)
                        );
                    }

                    var idempotenteIn = buscarRegistrosIdempotenteIn(idIdempotencia);
                    var idempotenteOut = buscarRegistrosIdempotenteOut(idIdempotencia);

                    assertAll(
                            () -> assertEquals(1, idempotenteIn.size()),
                            () -> assertEquals(2, idempotenteOut.size())
                    );

                    var pain012 = verificarMensagemPain012();

                    assertAll(
                            () -> assertNull(pain012.getMotivoRejeicao()),
                            () -> assertTrue(pain012.getStatus()),
                            () -> assertEquals(CONFIRMADO_USUARIO_PAGADOR.name(), pain012.getStatusRecorrencia()),
                            () -> assertEquals(transacao.getCodigoMunicipioIbge(), pain012.getCodMunIBGE()),
                            () -> assertEquals(3, pain012.getDetalhesRecorrencias().size())
                    );
                });
    }

    private Pain012Dto verificarMensagemPain012() {
        try {
            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);

            for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> iteration : response.records(TOPICO_ICOM_PAIN_ENVIO_V1)) {
                if (iteration.value() != null) {
                    return ObjectMapperUtil.converterStringParaObjeto(iteration.value(), new TypeReference<Pain012Dto>() {
                    });
                }
            }
            return new Pain012Dto();
        } catch (Exception ignore) {
            return new Pain012Dto();
        }
    }

    private CadastroAutorizacaoRecorrenciaTransacaoDTO criarAutorizacaoCadastro() {
        CadastroAutorizacaoRecorrenciaTransacaoDTO dto = new CadastroAutorizacaoRecorrenciaTransacaoDTO();

        dto.setIdInformacaoStatus("IS9158698220250517evxN7JwZRDo");
        dto.setIdRecorrencia("RR9999900420250517G4Jdhm5o9A6");
        dto.setTipoJornada(TipoJornada.JORNADA_2);
        dto.setContrato("123456789012345");
        dto.setObjeto("conta");
        dto.setCodigoMunicipioIbge("3550308");
        dto.setNomeDevedor("João da Silva");
        dto.setCpfCnpjDevedor("12345678901");
        dto.setNomeRecebedor("Empresa XYZ LTDA");
        dto.setCpfCnpjRecebedor("12345678000199");
        dto.setInstituicaoRecebedor("12345678");
        dto.setTipoFrequencia(TipoFrequenciaPixAutomatico.MENSAL);
        dto.setValor(new BigDecimal("150.00"));
        dto.setPisoValorMaximo(new BigDecimal("100.00"));
        dto.setValorMaximo(new BigDecimal("200.00"));
        dto.setPoliticaRetentativa(PoliticaRetentativaRecorrenciaEnum.PERMITE_3R_7D);
        dto.setDataInicialRecorrencia(LocalDate.now());
        dto.setDataFinalRecorrencia(LocalDate.now().plusMonths(6));
        dto.setDataCriacaoRecorrencia(LocalDateTime.now());
        dto.setCpfCnpjConta("98765432100");
        dto.setNomeSolicitante("Maria Oliveira");
        dto.setCooperativa("1234");
        dto.setConta("987654321");
        dto.setAgencia("01");
        dto.setOrigemConta(OrigemEnum.LEGADO);
        dto.setTipoContaPagador(TipoContaEnum.CONTA_CORRENTE);
        dto.setTipoPessoaConta(br.com.sicredi.canaisdigitais.enums.TipoPessoaEnum.PF);
        dto.setCanal("MOBI");
        dto.setDataRecebimentoConfirmacaoPacs002PagamentoImediato(LocalDateTime.now());
        dto.setIdFimAFimPagamentoImediato("E9158698220250325161553Vzd1rbmDM");

        return dto;
    }

    private ProtocoloDTO getProtocoloDTO(String idRecorrencia, String recorrenciaRequestDTO) {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setIdProtocolo(1L);
        protocoloDTO.setCodigoTipoTransacao("codigoTransacao");
        protocoloDTO.setCooperativa(COOPERATIVA);
        protocoloDTO.setConta(CONTA);
        protocoloDTO.setIdentificadorTransacao(idRecorrencia);
        protocoloDTO.setIdentificadorSimulacaoLimite(UUID.randomUUID().toString());
        protocoloDTO.setPayloadTransacao(recorrenciaRequestDTO);
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
                .setHeader(KafkaHeaders.TOPIC, CadastroAutorizacaoIntegrationTest.TOPICO_CADASTRO_AUTORIZACAO)
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }


    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_PROTOCOLO, TOPICO_ICOM_PAIN_ENVIO_V1);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }
}
