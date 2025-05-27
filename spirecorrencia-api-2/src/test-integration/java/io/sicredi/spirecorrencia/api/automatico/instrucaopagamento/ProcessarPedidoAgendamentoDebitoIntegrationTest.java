package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.dto.Pain014Dto;
import br.com.sicredi.spi.entities.type.FinalidadeAgendamento;
import br.com.sicredi.spi.entities.type.SituacaoAgendamentoPain014;
import io.sicredi.spirecorrencia.api.accountdata.DadosContaResponseDTO;
import io.sicredi.spirecorrencia.api.accountdata.DadosPessoaContaDTO;
import io.sicredi.spirecorrencia.api.accountdata.TipoContaEnum;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.IdentificadorTransacaoTestFactory.gerarIdFimAFim;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SqlGroup(
        value = {
                @Sql(scripts = {
                        "/db/clear.sql",
                        "/db/insert_agendamento_recorrencia_autorizacao.sql",
                        "/db/insert_agendamento_recorrencia_autorizacao_ciclo.sql",
                        "/db/insert_agendamento_recorrencia_instrucao_pagamento.sql"
                }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
class ProcessarPedidoAgendamentoDebitoIntegrationTest extends AbstractIntegrationTest {
    private static final String TOPICO_ICOM_PAIN_ENVIO_V1 = "icom-pain-envio-v1";
    private static final String TOPICO_ICOM_PAIN_RECEBIDO_V1 = "icom-pain-recebido-v1";
    private static final String PARAMS_CONSULTAR_DADOS_CONTA = "/accounts?document=12345678901&company=1234&number=567890&source=ALL";

    @ParameterizedTest
    @MethodSource("proverPain013")
    void dadoPain013_quandoProcessarPedidoAgendamentoDebito_deveEmitirPain014(Pain013Dto pain013, String situacaoEsperada) {
        criarStubDadosContaPagador();

        enviarMensagem(pain013, TOPICO_ICOM_PAIN_RECEBIDO_V1, criarHeaders(pain013));

        try (Consumer<String, String> consumer = configurarConsumer("latest", Boolean.TRUE, TOPICO_ICOM_PAIN_ENVIO_V1)) {
            await().atMost(10, TimeUnit.SECONDS)
                    .pollDelay(Duration.ofSeconds(1))
                    .untilAsserted(() -> {
                        var mensagemConsumida = getLastRecord(consumer, TOPICO_ICOM_PAIN_ENVIO_V1);
                        var pain014 = ObjectMapperUtil.converterStringParaObjeto(mensagemConsumida.value(), Pain014Dto.class);
                        assertNotNull(pain014);
                        assertThat(pain014.getSituacaoDoAgendamento()).isEqualTo(situacaoEsperada);
                    });
        }
    }

    private void criarStubDadosContaPagador() {
        criarStubMockResponse(PARAMS_CONSULTAR_DADOS_CONTA,
                HttpMethod.GET,
                HttpStatus.OK,
                ObjectMapperUtil.converterObjetoParaString(List.of(
                        DadosContaResponseDTO.builder()
                                .posto("12")
                                .coop("1234")
                                .status("ACTIVE")
                                .numeroConta("567890")
                                .sistema("DIGITAL")
                                .temCreditoBloqueado(false)
                                .titular(DadosPessoaContaDTO.builder().documento("12345678901").build())
                                .tipoConta(TipoContaEnum.CHECKING_ACCOUNT)
                                .build()
                ))
        );
    }

    private Stream<Arguments> proverPain013() {
        var pain13InstrucaoValida = criarPain013("RN1234567890123456789012345", LocalDate.of(2025, 5, 8), FinalidadeAgendamento.AGENDADO);
        var pain13IntrucaoInvalida = criarPain013("RR1234567890123456789012345", LocalDate.of(2025, 5, 15), FinalidadeAgendamento.AGENDADO_NOVA_TENTATIVA);

        return Stream.of(
                Arguments.of(pain13InstrucaoValida, SituacaoAgendamentoPain014.ACEITA_USUARIO_PAGADOR.name()),
                Arguments.of(pain13IntrucaoInvalida, SituacaoAgendamentoPain014.REJEITADA_USUARIO_PAGADOR.name())
        );
    }

    private static Map<String, String> criarHeaders(Pain013Dto pain013) {
        return Map.of(
                "TIPO_MENSAGEM", "PAIN013",
                "ID_IDEMPOTENCIA", pain013.getIdFimAFim(),
                KafkaHeaders.KEY, pain013.getIdRecorrencia()
        );
    }

    private static Pain013Dto criarPain013(String idRecorrencia, LocalDate dataVencimento, FinalidadeAgendamento finalidadeAgendamento) {
        return Pain013Dto.builder()
                .idRecorrencia(idRecorrencia)
                .idFimAFim(gerarIdFimAFim(dataVencimento.atStartOfDay()))
                .idConciliacaoRecebedor("CONTRATO123")
                .finalidadeDoAgendamento(finalidadeAgendamento.name())
                .dataVencimento(dataVencimento)
                .dataHoraCriacaoParaEmissao(dataVencimento.minusDays(3).atStartOfDay())
                .cpfCnpjUsuarioPagador("12345678901")
                .participanteDoUsuarioPagador("0001")
                .nomeDevedor("Maria Oliveira")
                .cpfCnpjDevedor("12345678909")
                .valor(new BigDecimal("1000.00"))
                .participanteDoUsuarioRecebedor("0002")
                .cpfCnpjUsuarioRecebedor("98765432100")
                .contaUsuarioRecebedor("567890")
                .agenciaUsuarioRecebedor("1234")
                .tipoContaUsuarioRecebedor("CORRENTE")
                .build();
    }
}
