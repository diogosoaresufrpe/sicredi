package io.sicredi.spirecorrencia.api.automatico.autorizacacaocancelamento;

import br.com.sicredi.canaisdigitais.dto.protocolo.ComandoProtocoloDTO;
import br.com.sicredi.canaisdigitais.enums.AcaoProtocoloEnum;
import br.com.sicredi.spi.dto.DetalheRecorrenciaPain011Dto;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.*;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamentoRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSolicitanteCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.FileTestUtils;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.kafka.clients.consumer.Consumer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11.*;
import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.*;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.PATH_IDENTIFICADOR_TRANSACAO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

@SqlGroup(
        value = {
                @Sql(scripts = {
                        "/db/clear.sql",
                        "/db/autorizacao_recorrencia.sql",
                        "/db/consulta_autorizacao.sql",
                        "/db/consulta_solicitacao_autorizacao.sql",
                        "/db/cancelamento_autorizacao.sql"
                }, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class ProcessaSolicitacaoAutorizacaoCancelamentoIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPICO_ICOM_PAIN_ENVIO_V1 = "icom-pain-envio-v1";
    private static final String TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1 = "canaisdigitais-protocolo-comando-v1";
    private static final String TOPICO_ICOM_PAIN_RECEBIDO_V1 = "icom-pain-recebido-v1";
    private static final String CPF_CNPJ_SOLICITANTE_CANCELAMENTO = "3040506070";
    private static final String ID_INFORMACAO_CANCELAMENTO = "IS0000111111111";
    private static final String CPF_CNPJ_USUARIO_RECEBEDOR = "987654321";
    private static final String PARTICIPANTE_DO_USUARIO_RECEBEDOR = "19293949";
    private static final String CONTA_USUARIO_PAGADOR = "102030";
    private static final String NOME_USUARIO = "Nome Usuario";
    private static final String AGENCIA_USUARIO_PAGADOR = "0101";
    private static final String CPF_CNPJ_USUARIO_PAGADOR = "1020304050";
    private static final String PARTICIPANTE_DO_USUARIO_PAGADOR = "405060";
    private static final String CPF_CNPJ_DEVEDOR = "1929394959";
    private static final String NUMERO_CONTRATO = "000000111";
    private static final String NOME_DO_DEVEDOR = "Nome do Devedor";
    private static final String ID_SOLICITACAO_AUTORIZACAO = "SC6118152120250425041bYqAj6ef";

    private static final String CODIGO_IBGE = "4313466";

    private static final String ID_RECORRENCIA_AUTORIZACAO_INEXISTENTE = "1234567890";
    private static final String ID_SOLICITACAO_AUTORIZACAO_INEXISTENTE = "1234567890";
    private static final String ID_RECORRENCIA_AUTORIZACAO_STATUS_APROVADA = "idRecorrencia2";
    private static final String CPF_PAGADOR = "40111443040";
    private static final String CNPJ_RECEBEDOR = "14848398000125";

    @Autowired
    private RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;

    @Nested
    @DisplayName("Teste de pedido de cancelamento Autorização Recebedor")
    class PedidoCancelamentoAutorizacaoRecebedorTest {

        @Nested
        @DisplayName("Teste de integração para Recorrencia Autorização ")
        class RecorrenciaAutorizacaoTest {

            @Test
            @DisplayName("""
                    Dado uma Pain011 mas, sem recorrencia autorização
                    quando processar solicitação de cancelamento
                    Deve salvar recorrencia solicitação com codigo de error MD01 e publicar rejeição com PAIN012
                    """)
            void dadoSolicitacaoAutorizacaoInexisten_quandoProcessarSolicitacao_deveEnviarAcaoRejeitarECriarRecorrenciaAutorizacaoCancelamento() {
                var pain011 = getPain011Dto(
                        SOLICITADO_PELO_USUARIO_PAGADOR,
                        ID_RECORRENCIA_AUTORIZACAO_INEXISTENTE,
                        CPF_CNPJ_USUARIO_PAGADOR,
                        CPF_CNPJ_USUARIO_RECEBEDOR,
                        CPF_CNPJ_SOLICITANTE_CANCELAMENTO,
                        ID_INFORMACAO_CANCELAMENTO
                );

                try (var consumer = configureMessageConsumer(TOPICO_ICOM_PAIN_ENVIO_V1)) {
                    enviaMensagem(pain011, TOPICO_ICOM_PAIN_RECEBIDO_V1);

                    await().atMost(10, TimeUnit.SECONDS)
                            .pollDelay(Duration.ofSeconds(1))
                            .untilAsserted(() -> {
                                var mensagemConsumida = getLastRecord(consumer, TOPICO_ICOM_PAIN_ENVIO_V1);
                                var payload = ObjectMapperUtil.converterStringParaObjeto(mensagemConsumida.value(), Pain012Dto.class);

                                validaCamposPain012ComRejeicao(
                                        payload,
                                        RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_INEXISTENTE,
                                        ID_RECORRENCIA_AUTORIZACAO_INEXISTENTE,
                                        null);
                                vailidaCamposRecorrenciaAutorizacaoCancelamentoComRejeicao(
                                        payload,
                                        pain011,
                                        RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_INEXISTENTE,
                                        TipoCancelamento.RECORRENCIA_AUTORIZACAO);
                            });
                }
            }

            @ParameterizedTest(name = """
                    dado uma PAIN011 com motivo {0} mas, o cpf do pagador não e igual
                    quando processar solicitação de cancelamento
                    Deve salvar Recorrencia Autorização cancelamento com codigo de error {0} e publicar evento Rejeitar com PAIN12
                    """)
            @MethodSource("dadosParaTesteDeValidaDeRegra")
            void dadoUmPain011ComMotivoSolicitadoPeloUsuarioPagadorMasCpfDoPagadorNaoIgual_quandoProcessarSolicitacao_deveSalvarRecorrenciaAutorizacaoCancelamentoEPublicarEVentoRejeitar(
                    MotivoRejeicaoPain012 motivoRejeicaoPain012,
                    String cpfPagador,
                    String cfpRecebedor,
                    String cpfSolicitante,
                    String codigoIbge
            ) {
                var pain011 = getPain011Dto(
                        SOLICITADO_PELO_USUARIO_RECEBEDOR,
                        ID_RECORRENCIA_AUTORIZACAO_STATUS_APROVADA,
                        cpfPagador,
                        cfpRecebedor,
                        cpfSolicitante,
                        ID_INFORMACAO_CANCELAMENTO
                );

                try (var consumer = configureMessageConsumer(TOPICO_ICOM_PAIN_ENVIO_V1)) {
                    enviaMensagem(pain011, TOPICO_ICOM_PAIN_RECEBIDO_V1);

                    await().atMost(10, TimeUnit.SECONDS)
                            .pollDelay(Duration.ofSeconds(1))
                            .untilAsserted(() -> {
                                var mensagemConsumida = getLastRecord(consumer, TOPICO_ICOM_PAIN_ENVIO_V1);
                                var payload = ObjectMapperUtil.converterStringParaObjeto(mensagemConsumida.value(), Pain012Dto.class);

                                validaCamposPain012ComRejeicao(payload, motivoRejeicaoPain012, ID_RECORRENCIA_AUTORIZACAO_STATUS_APROVADA, codigoIbge);
                                vailidaCamposRecorrenciaAutorizacaoCancelamentoComRejeicao(payload, pain011, motivoRejeicaoPain012, TipoCancelamento.RECORRENCIA_AUTORIZACAO);
                            });
                }

            }

            @Test
            @DisplayName("""
                    Dado uma Pain011 com motivo solicitado pelo usuário pagador e com o documento do pagador, ou recebedor igual
                    quando processar solicitação de cancelamento
                    Deve salvar Recorrencia Autorização cancelamento e publicar evento Aprovar com PAIN12
                    """)
            void dadoUmaPain011Valida_quandoProcessarSolicitacao_deveSalvarRecorrenciaAutorizacaoCancelamentoEPublicarEVentoAprovar() {
                var pain011 = getPain011Dto(
                        SOLICITADO_PELO_USUARIO_RECEBEDOR,
                        ID_RECORRENCIA_AUTORIZACAO_STATUS_APROVADA,
                        CPF_PAGADOR,
                        CNPJ_RECEBEDOR,
                        CNPJ_RECEBEDOR,
                        ID_INFORMACAO_CANCELAMENTO
                );

                try  (var consumer = configureMessageConsumer(TOPICO_ICOM_PAIN_ENVIO_V1)) {

                    enviaMensagem(pain011, TOPICO_ICOM_PAIN_RECEBIDO_V1);

                    await().atMost(10, TimeUnit.SECONDS)
                            .pollDelay(Duration.ofSeconds(1))
                            .untilAsserted(() -> {
                                var mensagemConsumida = getLastRecord(consumer, TOPICO_ICOM_PAIN_ENVIO_V1);
                                var payload = ObjectMapperUtil.converterStringParaObjeto(mensagemConsumida.value(), Pain012Dto.class);

                                validaCamposPain012SemRejeicao(payload, ID_RECORRENCIA_AUTORIZACAO_STATUS_APROVADA);
                                vailidaCamposRecorrenciaAutorizacaoCancelamentoSemRejeicao(payload, pain011, TipoCancelamento.RECORRENCIA_AUTORIZACAO);
                            });
                }
            }



            private static Stream<Arguments> dadosParaTesteDeValidaDeRegra() {
                var cpfDiferente = "cpf_diferente";
                return Stream.of(
                        Arguments.of(CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO, cpfDiferente, CNPJ_RECEBEDOR, cpfDiferente, CODIGO_IBGE),
                        Arguments.of(CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE, CPF_PAGADOR, cpfDiferente, cpfDiferente, CODIGO_IBGE),
                        Arguments.of(CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA, CPF_PAGADOR, CNPJ_RECEBEDOR, cpfDiferente, CODIGO_IBGE)
                );
            }
        }

        @Nested
        @DisplayName("Teste de integração para Recorrencia Solicitação")
        class RecorrenciaSolicitacaoTest {

            @ParameterizedTest(name = """
                Dado uma Pain011 com motivo {0} mas, sem Solicitação autorização
                quando processar solicitação de cancelamento
                Deve salvar recorrencia solicitação com codigo de error AP09 e publicar rejeição com PAIN012
                """)
            @EnumSource(
                    value = MotivoCancelamentoPain11.class,
                    mode = EnumSource.Mode.INCLUDE,
                    names = {
                            "ERROR_SOLICITACAO_DE_CONFIRMACAO",
                            "FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR",
                            "CONFIRMADA_POR_OUTRA_JORNADA"
                    })
            void dadoUmaPain011ValidaSemSolicitacaoAutorizacao_quandoProcessarSolicitacao_deveSalvarRecorrenciaSolicitacaoEPublicarEVentoRejeitar(
                    MotivoCancelamentoPain11 motivoCancelamento
            ) {
                var pain011 = getPain011Dto(
                        motivoCancelamento,
                        ID_SOLICITACAO_AUTORIZACAO_INEXISTENTE,
                        CPF_PAGADOR,
                        CNPJ_RECEBEDOR,
                        CNPJ_RECEBEDOR,
                        ID_INFORMACAO_CANCELAMENTO
                );

                try (var consumer = configureMessageConsumer(TOPICO_ICOM_PAIN_ENVIO_V1)) {

                    enviaMensagem(pain011, TOPICO_ICOM_PAIN_RECEBIDO_V1);

                    await().atMost(10, TimeUnit.SECONDS)
                            .pollDelay(Duration.ofSeconds(1))
                            .untilAsserted(() -> {
                                var mensagemConsumida = getLastRecord(consumer, TOPICO_ICOM_PAIN_ENVIO_V1);
                                var payload = ObjectMapperUtil.converterStringParaObjeto(mensagemConsumida.value(), Pain012Dto.class);

                                validaCamposPain012ComRejeicao(
                                        payload,
                                        SOLICITACAO_CONFIRMACAO_NAO_IDENTIFICADA,
                                        ID_SOLICITACAO_AUTORIZACAO_INEXISTENTE,
                                        null);
                                vailidaCamposRecorrenciaAutorizacaoCancelamentoComRejeicao(
                                        payload,
                                        pain011,
                                        SOLICITACAO_CONFIRMACAO_NAO_IDENTIFICADA,
                                        TipoCancelamento.RECORRENCIA_SOLICITACAO);
                            });
                }
            }

            @ParameterizedTest(name = """
                    dado uma PAIN011 com motivo {1} mas, com error {2}
                    quando processar solicitação de cancelamento
                    Deve salvar Recorrencia Autorização cancelamento com codigo de error {2} e publicar evento Rejeitar com PAIN12
                    """)
            @MethodSource("dadosParaTestarValidacaoDeRegrasSolicitacao")
            void dadoUmPain011ComMotivoSolicitadoPeloUsuarioPagadorMasComErroDeRegraDeNegocio_quandoProcessarSolicitacao_deveSalvarRecorrenciaAutorizacaoCancelamentoEPublicarEVentoRejeitar(
                    String idRecorrencia,
                    MotivoCancelamentoPain11 motivoCancelamento,
                    MotivoRejeicaoPain012 motivoRejeicaoPain012,
                    String cpfPagador,
                    String cpfRecebedor,
                    String cpfSolicitante,
                    String codigoIbge
            ) {
                var pain011 = getPain011Dto(
                        motivoCancelamento,
                        idRecorrencia,
                        cpfPagador,
                        cpfRecebedor,
                        cpfSolicitante,
                        ID_INFORMACAO_CANCELAMENTO
                );

                enviaMensagem(pain011, TOPICO_ICOM_PAIN_RECEBIDO_V1);

                try (var consumer = configureMessageConsumer(TOPICO_ICOM_PAIN_ENVIO_V1)) {
                    await().atMost(10, TimeUnit.SECONDS)
                            .pollDelay(Duration.ofSeconds(1))
                            .untilAsserted(() -> {
                                var records = getLastRecord(consumer, TOPICO_ICOM_PAIN_ENVIO_V1);
                                var payload = ObjectMapperUtil.converterStringParaObjeto(records.value(), Pain012Dto.class);

                                validaCamposPain012ComRejeicao(payload, motivoRejeicaoPain012, idRecorrencia, codigoIbge);
                                vailidaCamposRecorrenciaAutorizacaoCancelamentoComRejeicao(payload, pain011, motivoRejeicaoPain012, TipoCancelamento.RECORRENCIA_SOLICITACAO);
                            });
                }
            }

            private static Stream<Arguments> dadosParaTestarValidacaoDeRegrasSolicitacao() {
                var cpfDiferente = "cpf_diferente";

                var solicitacaoCriada = "rec-123456";
                var solicitacaoPendente = "rec-123457";

                var cpfPagadorCriada = "12690422115";
                var cpfPagadorPendente = "12690422115";

                var cpfRecebedorCriada = "12345678901";
                var cpfRecebedorPendente = "12345678901";

                var codigoIBGE = "4305108";

                return Stream.of(
                        Arguments.of(solicitacaoCriada,ERROR_SOLICITACAO_DE_CONFIRMACAO, CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO, cpfDiferente, cpfRecebedorCriada, cpfPagadorCriada, codigoIBGE),
                        Arguments.of(solicitacaoCriada, ERROR_SOLICITACAO_DE_CONFIRMACAO, CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE, cpfPagadorCriada, cpfDiferente, cpfPagadorCriada, codigoIBGE),
                        Arguments.of(solicitacaoCriada, ERROR_SOLICITACAO_DE_CONFIRMACAO, CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA, cpfPagadorCriada, cpfRecebedorCriada, cpfDiferente, codigoIBGE),

                        Arguments.of(solicitacaoCriada, FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR, CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO, cpfDiferente, cpfRecebedorCriada, cpfPagadorCriada, codigoIBGE),
                        Arguments.of(solicitacaoCriada, FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR, CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE, cpfPagadorCriada, cpfDiferente, cpfPagadorCriada, codigoIBGE),
                        Arguments.of(solicitacaoCriada, FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR, CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA, cpfPagadorCriada, cpfRecebedorCriada, cpfDiferente, codigoIBGE),

                        Arguments.of(solicitacaoCriada, CONFIRMADA_POR_OUTRA_JORNADA, CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO, cpfDiferente, cpfRecebedorCriada, cpfPagadorCriada, codigoIBGE),
                        Arguments.of(solicitacaoCriada, CONFIRMADA_POR_OUTRA_JORNADA, CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE, cpfPagadorCriada, cpfDiferente, cpfPagadorCriada, codigoIBGE),
                        Arguments.of(solicitacaoCriada, CONFIRMADA_POR_OUTRA_JORNADA, CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA, cpfPagadorCriada, cpfRecebedorCriada, cpfDiferente, codigoIBGE),

                        Arguments.of(solicitacaoPendente,ERROR_SOLICITACAO_DE_CONFIRMACAO, CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO, cpfDiferente, cpfRecebedorPendente, cpfRecebedorPendente, codigoIBGE),
                        Arguments.of(solicitacaoPendente, ERROR_SOLICITACAO_DE_CONFIRMACAO, CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE, cpfPagadorPendente, cpfDiferente, cpfRecebedorPendente, codigoIBGE),
                        Arguments.of(solicitacaoPendente, ERROR_SOLICITACAO_DE_CONFIRMACAO, CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA, cpfPagadorPendente, cpfRecebedorPendente, cpfDiferente, codigoIBGE),

                        Arguments.of(solicitacaoPendente, FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR, CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO, cpfDiferente, cpfRecebedorPendente, cpfRecebedorPendente, codigoIBGE),
                        Arguments.of(solicitacaoPendente, FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR, CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE, cpfPagadorPendente, cpfDiferente, cpfRecebedorPendente, codigoIBGE),
                        Arguments.of(solicitacaoPendente, FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR, CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA, cpfPagadorPendente, cpfRecebedorPendente, cpfDiferente, codigoIBGE),

                        Arguments.of(solicitacaoPendente, CONFIRMADA_POR_OUTRA_JORNADA, CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO, cpfDiferente, cpfRecebedorPendente, cpfRecebedorPendente, codigoIBGE),
                        Arguments.of(solicitacaoPendente, CONFIRMADA_POR_OUTRA_JORNADA, CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE, cpfPagadorPendente, cpfDiferente, cpfRecebedorPendente, codigoIBGE),
                        Arguments.of(solicitacaoPendente, CONFIRMADA_POR_OUTRA_JORNADA, CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA, cpfPagadorPendente, cpfRecebedorPendente, cpfDiferente, codigoIBGE)
                );
            }
        }

        private void vailidaCamposRecorrenciaAutorizacaoCancelamentoComRejeicao(Pain012Dto payload,
                                                                                Pain011Dto pain011,
                                                                                MotivoRejeicaoPain012 motivoRejeicao,
                                                                                TipoCancelamento recorrenciaAutorizacao) {

            ThrowingConsumer<List<?>> listThrowingConsumer = tuple -> {
                assertThat(tuple.get(0)).isEqualTo(payload.getIdRecorrencia());
                assertThat(tuple.get(1)).isEqualTo(payload.getIdInformacaoStatus());
                assertThat(tuple.get(2)).isEqualTo(motivoRejeicao.name());
                assertThat(tuple.get(3)).isEqualTo(pain011.getMotivoCancelamento());
                assertThat(tuple.get(4)).isNotNull();
                assertThat(tuple.get(5)).isEqualTo(pain011.getCpfCnpjSolicitanteCancelamento());
                assertThat(tuple.get(6)).isEqualTo(TipoSolicitanteCancelamento.RECEBEDOR);
                assertThat(tuple.get(7)).isEqualTo(TipoStatusCancelamentoAutorizacao.CRIADA);
                assertThat(tuple.get(8)).isEqualTo(recorrenciaAutorizacao);
            };
            vailidaCamposRecorrenciaAutorizacaoCancelamento(
                    listThrowingConsumer,
                    RecorrenciaAutorizacaoCancelamento::getIdRecorrencia,
                    RecorrenciaAutorizacaoCancelamento::getIdInformacaoStatus,
                    RecorrenciaAutorizacaoCancelamento::getMotivoRejeicao,
                    RecorrenciaAutorizacaoCancelamento::getMotivoCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getDataCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getCpfCnpjSolicitanteCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getTipoSolicitanteCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getTipoStatus,
                    RecorrenciaAutorizacaoCancelamento::getTipoCancelamento);
        }

        private void vailidaCamposRecorrenciaAutorizacaoCancelamentoSemRejeicao(Pain012Dto payload,
                                                                                Pain011Dto pain011,
                                                                                TipoCancelamento recorrenciaAutorizacao) {

            ThrowingConsumer<List<?>> listThrowingConsumer = tuple -> {
                assertThat(tuple.get(0)).isEqualTo(payload.getIdRecorrencia());
                assertThat(tuple.get(1)).isEqualTo(payload.getIdInformacaoStatus());
                assertThat(tuple.get(2)).isNull();
                assertThat(tuple.get(3)).isEqualTo(pain011.getMotivoCancelamento());
                assertThat(tuple.get(4)).isNotNull();
                assertThat(tuple.get(5)).isEqualTo(pain011.getCpfCnpjSolicitanteCancelamento());
                assertThat(tuple.get(6)).isEqualTo(TipoSolicitanteCancelamento.RECEBEDOR);
                assertThat(tuple.get(7)).isEqualTo(TipoStatusCancelamentoAutorizacao.CRIADA);
                assertThat(tuple.get(8)).isEqualTo(recorrenciaAutorizacao);
            };
            vailidaCamposRecorrenciaAutorizacaoCancelamento(
                    listThrowingConsumer,
                    RecorrenciaAutorizacaoCancelamento::getIdRecorrencia,
                    RecorrenciaAutorizacaoCancelamento::getIdInformacaoStatus,
                    RecorrenciaAutorizacaoCancelamento::getMotivoRejeicao,
                    RecorrenciaAutorizacaoCancelamento::getMotivoCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getDataCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getCpfCnpjSolicitanteCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getTipoSolicitanteCancelamento,
                    RecorrenciaAutorizacaoCancelamento::getTipoStatus,
                    RecorrenciaAutorizacaoCancelamento::getTipoCancelamento);
        }

        @SafeVarargs
        private void validaCampos(Pain012Dto payload,
                                  ThrowingConsumer<List<?>> listThrowingConsumer,
                                  Function<Pain012Dto, ?>... getters) {
            assertThat(payload)
                    .extracting(getters)
                    .satisfies(listThrowingConsumer);

        }

        private void validaCamposPain012SemRejeicao(Pain012Dto payload,
                                                    String idRecorrencia) {
            ThrowingConsumer<List<?>> listThrowingConsumer = tuple -> {
                assertThat(tuple.get(0)).isEqualTo(idRecorrencia);
                assertThat(tuple.get(1)).isNull();
                assertThat(tuple.get(2)).isNotNull();
                assertThat(tuple.get(3)).isEqualTo(CODIGO_IBGE);
            };
            validaCampos(
                    payload,
                    listThrowingConsumer,
                    Pain012Dto::getIdRecorrencia,
                    Pain012Dto::getMotivoRejeicao,
                    Pain012Dto::getIdInformacaoStatus,
                    Pain012Dto::getCodMunIBGE);
        }

        private void validaCamposPain012ComRejeicao(Pain012Dto payload, MotivoRejeicaoPain012 motivoRejeicao, String idRecorrencia, String codigoIbge) {
            validaCampos(payload,
                    tuple -> {
                        assertThat(tuple.get(0)).isEqualTo(idRecorrencia);
                        assertThat(tuple.get(1)).isEqualTo(motivoRejeicao.name());
                        assertThat(tuple.get(2)).isNotNull();
                        assertThat(tuple.get(3)).isEqualTo(codigoIbge);
                    },
                    Pain012Dto::getIdRecorrencia,
                    Pain012Dto::getMotivoRejeicao,
                    Pain012Dto::getIdInformacaoStatus,
                    Pain012Dto::getCodMunIBGE);
        }

        @SafeVarargs
        private void vailidaCamposRecorrenciaAutorizacaoCancelamento(ThrowingConsumer<List<?>> listThrowingConsumer,
                                                                     Function<RecorrenciaAutorizacaoCancelamento, ?>... getters) {
            recorrenciaAutorizacaoCancelamentoRepository.findById(ID_INFORMACAO_CANCELAMENTO)
                    .ifPresentOrElse(
                            recorrenciaAutorizacaoCancelamento ->
                                    assertThat(recorrenciaAutorizacaoCancelamento)
                                            .extracting(getters)
                                            .satisfies(listThrowingConsumer)
                            ,
                            () -> fail("Recorrencia não encontrada")
                    );
        }
    }

    @Nested
    @DisplayName("Teste de pedido de cancelamento Autorização Pagador")
    class PedidoCancelamentoAutorizacaoPagadorTest {

        private static final String CONSULTA_PROTOCOLO_URL = "/v3/%s/%s";
        private static final String CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CANCELAMENTO_DE_AUTORIZACAO = "441";
        private static final String TOPICO_ICOM_PAIN_ENVIADO_V1 = "icom-pain-enviado-v1";
        private static final String SUCESSO_PROTOCOLO_JSON = "__files/mocks/canais-digitais-protocolo-info-internal-api/retorno-sucesso-protocolo.json";

        @Test
        @DisplayName("""
        Dado uma Pain011 valida, mas, sem Recorrencia Solicitação Cancelamento com status CRIADA
        Quando processar solicitação de cancelamento
        Deve publicar comando sucesso, sem errors.
        """)
        void dadoUmaPain011ValidaSemOcorrenciaSolicitacao_quandoProcessarPedidoDeCancelamento_deveRetornarSucessoSemProblemas() {
            var pain011 = getPain011Dto(
                    SOLICITADO_PELO_USUARIO_PAGADOR,
                    ID_RECORRENCIA_AUTORIZACAO_INEXISTENTE,
                    CPF_CNPJ_USUARIO_PAGADOR,
                    CPF_CNPJ_USUARIO_RECEBEDOR,
                    CPF_CNPJ_SOLICITANTE_CANCELAMENTO,
                    ID_INFORMACAO_CANCELAMENTO
            );

            var url = CONSULTA_PROTOCOLO_URL.formatted(CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CANCELAMENTO_DE_AUTORIZACAO, pain011.getIdInformacaoCancelamento());
            var responseConsultaProtocolo = FileTestUtils.asString(new ClassPathResource(SUCESSO_PROTOCOLO_JSON))
                    .replace(PATH_IDENTIFICADOR_TRANSACAO, ID_SOLICITACAO_AUTORIZACAO);

            criarStubMockResponse(
                    url,
                    HttpMethod.GET,
                    HttpStatus.OK,
                    responseConsultaProtocolo
            );

            enviaMensagem(pain011, TOPICO_ICOM_PAIN_ENVIADO_V1);

            try (var consumer = configureMessageConsumer(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1)) {

                await().atMost(10, TimeUnit.SECONDS)
                        .pollDelay(Duration.ofSeconds(1))
                        .untilAsserted(() -> {
                            var mensagemConsumida = getLastRecord(consumer, TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1);
                            var payload = ObjectMapperUtil.converterStringParaObjeto(mensagemConsumida.value(), ComandoProtocoloDTO.class);

                            validaRetornoComandoSucesso(payload);

                            recorrenciaAutorizacaoCancelamentoRepository.findById(ID_INFORMACAO_CANCELAMENTO)
                                    .ifPresentOrElse(recorrenciaAutorizacaoCancelamento -> fail(
                                            "RecorrenciaAutorizacaoCancelamento não deveria ser encontrada"
                                    ), () -> assertThat(Boolean.TRUE).isTrue());
                        });
            }
        }

        @Test
        @DisplayName("""
        Dado uma Pain011 valida com Recorrencia Solicitação Cancelamento com status CRIADA
        Quando processar solicitação de cancelamento
        Deve atualizar Recorrencia Solicitação Cancelamento com o status ENVIADA e publicar comando com sucesso.
        """)
        void dadoUmaPain011Valida_quandoProcessarCancelamento_devePublicarComandoComSucesso() {
            var idInformacaoCancelamento = "IC0118152120250425041bYqAj6ef";
            var pain011 = getPain011Dto(
                    SOLICITADO_PELO_USUARIO_PAGADOR,
                    "RN4118152120250425041bYqAj6ef",
                    CPF_CNPJ_USUARIO_PAGADOR,
                    CPF_CNPJ_USUARIO_RECEBEDOR,
                    CPF_CNPJ_SOLICITANTE_CANCELAMENTO,
                    idInformacaoCancelamento
            );

            var url = CONSULTA_PROTOCOLO_URL.formatted(CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CANCELAMENTO_DE_AUTORIZACAO, pain011.getIdInformacaoCancelamento());
            var responseConsultaProtocolo = FileTestUtils.asString(new ClassPathResource(SUCESSO_PROTOCOLO_JSON))
                    .replace(PATH_IDENTIFICADOR_TRANSACAO, idInformacaoCancelamento);

            criarStubMockResponse(
                    url,
                    HttpMethod.GET,
                    HttpStatus.OK,
                    responseConsultaProtocolo
            );

            try (var consumer = configureMessageConsumer(TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1)) {
                enviaMensagem(pain011, TOPICO_ICOM_PAIN_ENVIADO_V1);

                await().atMost(10, TimeUnit.SECONDS)
                        .pollDelay(Duration.ofSeconds(1))
                        .untilAsserted(() -> {
                            var mensagemConsumida = getLastRecord(consumer, TOPICO_CANAIS_DIGITAIS_PROTOCOLO_COMANDO_V1);
                            var payload = ObjectMapperUtil.converterStringParaObjeto(mensagemConsumida.value(), ComandoProtocoloDTO.class);

                            validaRetornoComandoSucesso(payload);

                            recorrenciaAutorizacaoCancelamentoRepository.findById(idInformacaoCancelamento)
                                    .ifPresentOrElse(recorrenciaAutorizacaoCancelamento ->
                                                    assertThat(recorrenciaAutorizacaoCancelamento.getTipoStatus())
                                                            .isEqualTo(TipoStatusCancelamentoAutorizacao.ENVIADA)
                                    ,
                                    () ->  fail("RecorrenciaAutorizacaoCancelamento deveria ser encontrada")
                            );
                        });


            }
        }

        private void validaRetornoComandoSucesso(ComandoProtocoloDTO payload) {
            assertThat(payload)
                    .extracting(
                            ComandoProtocoloDTO::getAcao,
                            ComandoProtocoloDTO::getPayload
                    )
                    .satisfies(tuple -> {
                        assertThat(tuple.get(0)).isEqualTo(AcaoProtocoloEnum.CONFIRMAR_PROCESSAMENTO);
                        assertThat(tuple.get(1))
                                .isNotNull()
                                .asInstanceOf(InstanceOfAssertFactories.MAP)
                                .extractingByKey("tipoRetorno")
                                .isEqualTo("SUCESSO");
                    });
        }
    }

    private Consumer<String, String> configureMessageConsumer(String topico) {
        return configurarConsumer(
                "latest",
                Boolean.TRUE,
                topico
        );
    }

    private void enviaMensagem(Pain011Dto pain011, String topico) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(pain011);
        var mensagem = buildMensagemTopico(payloadString, topico);

        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private static Message<String> buildMensagemTopico(String payloadString, String topico) {
        return MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, topico)
                .setHeader("TIPO_MENSAGEM", "PAIN011")
                .setHeader(KafkaHeaders.KEY, "RN9118152120250425041bYqAj6ef")
                .setHeader("ID_IDEMPOTENCIA", ID_SOLICITACAO_AUTORIZACAO)
                .setHeader("STATUS_ENVIO", "SUCESSO")
                .setHeader("PSP_EMISSOR", "PAGADOR")
                .setHeader("OPERACAO", "OPERACAO_AUTOMATICO")
                .build();
    }

    private static Pain011Dto getPain011Dto(MotivoCancelamentoPain11 motivoCancelamentoPain11,
                                            String idRecorrencia,
                                            String cpfPagador,
                                            String cnpjRecebedor,
                                            String cpfSolicitante,
                                            String idInformacaoCancelamento) {
        return Pain011Dto.builder()
                .cpfCnpjSolicitanteCancelamento(cpfSolicitante)
                .motivoCancelamento(motivoCancelamentoPain11.name())
                .idRecorrencia(idRecorrencia)
                .idInformacaoCancelamento(idInformacaoCancelamento)
                .tipoRecorrencia(TipoRecorrencia.RECORRENTE.name())
                .tipoFrequencia(TipoFrequencia.MENSAL.name())
                .dataFinalRecorrencia(LocalDate.now())
                .indicadorObrigatorio(Boolean.TRUE)
                .valor(BigDecimal.valueOf(100.00))
                .dataInicialRecorrencia(LocalDate.now().minusMonths(1))
                .cpfCnpjUsuarioRecebedor(cnpjRecebedor)
                .participanteDoUsuarioRecebedor(PARTICIPANTE_DO_USUARIO_RECEBEDOR)
                .contaUsuarioPagador(CONTA_USUARIO_PAGADOR)
                .nomeUsuarioRecebedor(NOME_USUARIO)
                .agenciaUsuarioPagador(AGENCIA_USUARIO_PAGADOR)
                .cpfCnpjUsuarioPagador(cpfPagador)
                .participanteDoUsuarioPagador(PARTICIPANTE_DO_USUARIO_PAGADOR)
                .cpfCnpjDevedor(CPF_CNPJ_DEVEDOR)
                .numeroContrato(NUMERO_CONTRATO)
                .descricao("descricao")
                .nomeDevedor(NOME_DO_DEVEDOR)
                .detalhesRecorrencias(List.of(
                        DetalheRecorrenciaPain011Dto.builder()
                                .dataHoraRecorrencia(LocalDateTime.now())
                                .tipoSituacao(TipoSituacaoPain011.DATA_CRIACAO.name())
                                .build()
                ))
                .build();
    }
}
