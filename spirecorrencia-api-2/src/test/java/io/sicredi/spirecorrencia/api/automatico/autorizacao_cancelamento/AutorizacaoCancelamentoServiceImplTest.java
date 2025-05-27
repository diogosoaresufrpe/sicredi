package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.DetalheRecorrenciaPain011Dto;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11;
import br.com.sicredi.spi.entities.type.TipoFrequencia;
import br.com.sicredi.spi.entities.type.TipoRecorrencia;
import br.com.sicredi.spi.entities.type.TipoSituacaoPain011;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamentoRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSolicitanteCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.protocolo.CanaisDigitaisProtocoloInfoInternalApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.*;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao.APROVADA;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoSubStatus.AGUARDANDO_CANCELAMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutorizacaoCancelamentoServiceImplTest {

    private static final String CODIGO_IBGE = "123456";
    private static final String ID_INFORMACAO_CANCELAMENTO = "IS0000111111111";
    private static final String PARTICIPANTE_DO_USUARIO_RECEBEDOR = "19293949";
    private static final String CONTA_USUARIO_PAGADOR = "102030";
    private static final String NOME_USUARIO = "Nome Usuario";
    private static final String AGENCIA_USUARIO_PAGADOR = "12690422115";
    private static final String PARTICIPANTE_DO_USUARIO_PAGADOR = "405060";
    private static final String CPF_CNPJ_DEVEDOR = "1929394959";
    private static final String NUMERO_CONTRATO = "000000111";
    private static final String NOME_DO_DEVEDOR = "Nome do Devedor";
    private static final String CPF_DO_SOLICITANTE = "12690422115";
    private static final String ID_RECORRENCIA = "123456789";
    private static final String CPF_DIFERENTE_DO_EXPERADO = "0000000000";
    private static final String CPF_DO_SOLICITANTE_DIFERENTE = "CPF_DO_SOLICITANTE_DIFERENTE";
    private static final String HEADER_OPERACAO_CANCELAMENTO_RECEBEDOR = "CANCELAMENTO_RECEBEDOR";
    private static final String CPF_PAGADOR = "12690422115";
    private static final String CPF_RECEBEDOR = "12690422115";

    private static final String DEFAULT_JSON = "{\"foo\":\"bar\"}";

    @Mock
    private RecorrenciaAutorizacaoRepository autorizacaoRepository;
    @Mock
    private SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;
    @Mock
    private RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Mock
    private CriaResponseStrategyFactory<IdempotenteRequest> operacaoRequestCriaResponseStrategyFactory;
    @Mock
    private CanaisDigitaisProtocoloInfoInternalApiClient canaisDigitaisProtocoloInfoInternalApiClient;

    @InjectMocks
    private AutorizacaoCancelamentoServiceImpl service;

    @Captor
    private ArgumentCaptor<RecorrenciaAutorizacaoCancelamento> recorrenciaAutorizacaoCancelamentoArgumentCaptor;

    @Captor
    private ArgumentCaptor<Pain012Dto> pain012DtoArgumentCaptor;

    @Nested
    @DisplayName("Teste de processamento de pedido de cancelamento Recebedor")
    class ProcessaPedidoCancelamentoRecebedorTest {

        @Nested
        @DisplayName("Teste de Processamento de Recorrencia Autorização")
        class ProcessaAutorizacaoTest {
            private static final String TOPICO = "topico-teste";

            @Nested
            @DisplayName("Valida retorno")
            class ValidaRetornoTest {

                private final IdempotentResponse<?> response = IdempotentResponse.builder().build();

                @SuppressWarnings("unchecked")
                @BeforeEach
                void setup() {
                    var responseStrategy = Mockito.mock(CriaResponseStrategy.class);

                    when(eventoResponseFactory.criarEventoPain012(any(Pain012Dto.class), eq(HEADER_OPERACAO_CANCELAMENTO_RECEBEDOR)))
                            .thenReturn(new EventoResponseDTO(DEFAULT_JSON, Map.of("foo", "bar"), TOPICO));

                    when(operacaoRequestCriaResponseStrategyFactory.criar(any()))
                            .thenReturn(responseStrategy);

                    when(responseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any()))
                            .thenReturn(response);
                }

                @Test
                @DisplayName("""
                    Dado uma Pain011 valida,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar recorrencia solicitação RecorrenciaAutorizacao e retornar IdempontenciaResponse com suceso
                    """)
                void dadoUmaPain011Valida_processarPedidoCancelamentoRecebedor_deveSalvarRecorrenciaAutorizacaoERetornarIdempontenciaResponseComSucesso() {
                    var recorrenciaAutorizacao = RecorrenciaAutorizacao.builder()
                            .cpfCnpjPagador(CPF_PAGADOR)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR)
                            .codigoMunicipioIBGE(CODIGO_IBGE)
                            .build();
                    when(autorizacaoRepository.findByIdRecorrenciaAndTipoStatusIn(any(), any()))
                            .thenReturn(Optional.of(recorrenciaAutorizacao));
                    when(recorrenciaAutorizacaoCancelamentoRepository.save(any()))
                            .thenReturn(RecorrenciaAutorizacaoCancelamento.builder()
                                    .dataAlteracaoRegistro(LocalDateTime.now())
                                    .build());

                    var pain011 = criarPain011(
                            CPF_DO_SOLICITANTE,
                            CPF_PAGADOR,
                            CPF_RECEBEDOR,
                            ID_RECORRENCIA,
                            MotivoCancelamentoPain11.ENCERRAMENTO_DE_EMPRESA_RECEBEDORA
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository).atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(autorizacaoRepository).findByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, List.of(APROVADA));
                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            null,
                            TipoCancelamento.RECORRENCIA_AUTORIZACAO,
                            2,
                            CODIGO_IBGE);
                }

                @Test
                @DisplayName("""
                    Dado uma Pain011 com cpf do pagador diferente da recorrencia,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar Recorrencia Autorização cancelamento com erro AP02 e retornar IdempontenciaResponse com erro
                    """)
                void dadoUmaPain011Valida_quandoProcessarPedidoCancelamentoRecebedor_deveSalvarRecorrenciaAutorizacaoCancelamentoComErrorAP02ERetornarIdempontenciaResponseComErro() {
                    var recorrenciaAutorizacao = RecorrenciaAutorizacao.builder()
                            .cpfCnpjPagador(CPF_PAGADOR)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR)
                            .codigoMunicipioIBGE(CODIGO_IBGE)
                            .build();

                    when(autorizacaoRepository.findByIdRecorrenciaAndTipoStatusIn(any(), any()))
                            .thenReturn(Optional.of(recorrenciaAutorizacao));

                    var pain011 = criarPain011(
                            CPF_DO_SOLICITANTE,
                            "documento_diferente",
                            CPF_RECEBEDOR,
                            ID_RECORRENCIA,
                            MotivoCancelamentoPain11.ENCERRAMENTO_DE_EMPRESA_RECEBEDORA
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository).atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(autorizacaoRepository).findByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, List.of(APROVADA));

                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO.name(),
                            TipoCancelamento.RECORRENCIA_AUTORIZACAO,
                            0,
                            CODIGO_IBGE);
                }

                @Test
                @DisplayName("""
                    Dado uma Pain011 com cpf do recebedor diferente da recorrencia,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar Recorrencia Autorização cancelamento com error AP06 e retornar IdempontenciaResponse com erro
                    """)
                void dadoUmaPain011Valida_quandoProcessarPedidoCancelamentoRecebedor_deveSalvarRecorrenciaAutorizacaoCancelamentoComErrorAP06ERetornarIdempontenciaResponseComErro() {
                    var recorrenciaAutorizacao = RecorrenciaAutorizacao.builder()
                            .cpfCnpjPagador(CPF_PAGADOR)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR)
                            .codigoMunicipioIBGE(CODIGO_IBGE)
                            .build();

                    when(autorizacaoRepository.findByIdRecorrenciaAndTipoStatusIn(any(), any()))
                            .thenReturn(Optional.of(recorrenciaAutorizacao));

                    var pain011 = criarPain011(
                            CPF_DO_SOLICITANTE,
                            CPF_PAGADOR,
                            "documento_diferente",
                            ID_RECORRENCIA,
                            MotivoCancelamentoPain11.ENCERRAMENTO_DE_EMPRESA_RECEBEDORA
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository).atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(autorizacaoRepository).findByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, List.of(APROVADA));
                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE.name(),
                            TipoCancelamento.RECORRENCIA_AUTORIZACAO,
                            0,
                            CODIGO_IBGE);
                }

                @Test
                @DisplayName("""
                    Dado uma Pain011 com cpf do solicitante igual ao cpf do recebedor ou recebedor,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar Recorrencia Autorização cancelamento com error AP10 e retornar IdempontenciaResponse com erro
                    """)
                void dadoUmaPain011Valida_quandoProcessarPedidoCancelamentoRecebedor_deveSalvarRecorrenciaAutorizacaoCancelamentoComErrorAP10ERetornarIdempontenciaResponseComErro() {
                    var recorrenciaAutorizacao = RecorrenciaAutorizacao.builder()
                            .cpfCnpjPagador(CPF_PAGADOR)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR)
                            .codigoMunicipioIBGE(CODIGO_IBGE)
                            .build();

                    when(autorizacaoRepository.findByIdRecorrenciaAndTipoStatusIn(any(), any()))
                            .thenReturn(Optional.of(recorrenciaAutorizacao));

                    var pain011 = criarPain011(
                            CPF_DO_SOLICITANTE_DIFERENTE,
                            CPF_PAGADOR,
                            CPF_RECEBEDOR,
                            ID_RECORRENCIA,
                            MotivoCancelamentoPain11.SUPEITA_DE_FRAUDE
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository).atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(autorizacaoRepository).findByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, List.of(APROVADA));

                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA.name(),
                            TipoCancelamento.RECORRENCIA_AUTORIZACAO,
                            0,
                            CODIGO_IBGE);
                }

                @Test
                @DisplayName("""
                    Dado uma Pain011 que não corresponde a uma Recorrencia Autorização
                    Quando processarPedidoCancelamentoRecebedor
                    Deve salvar Recorrencia Autorização cancelamento e retornar IdempontenciaResponse com erro
                    """)
                void dadoPain011Valida_quandoProcessaPedidoCancelamentoRecebedor_deveSalvarRecorrenciaAutorizacaoCancelamentoComErrorMD01ERetornarIdempontenciaResponseComErro() {
                    when(autorizacaoRepository.findByIdRecorrenciaAndTipoStatusIn(any(), any()))
                            .thenReturn(Optional.empty());

                    var pain011 = criarPain011(
                            CPF_DO_SOLICITANTE_DIFERENTE,
                            CPF_PAGADOR,
                            CPF_RECEBEDOR,
                            ID_RECORRENCIA,
                            MotivoCancelamentoPain11.SUPEITA_DE_FRAUDE
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository, never()).atualizaSubStatusPorIdRecorrencia(eq(ID_RECORRENCIA), any());
                    verify(autorizacaoRepository).findByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, List.of(APROVADA));

                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_INEXISTENTE.name(),
                            TipoCancelamento.RECORRENCIA_AUTORIZACAO,
                            0,
                            null);
                }
            }

            @Nested
            @DisplayName("Valida casos de exceptions")
            class CasoDeExceptions {

                @Test
                @DisplayName("""
                    Dado uma Pain011 com dados validos, mas ocorrer um error na chama do eventoResponseFactory
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve lançar um exception
                    """)
                void dadoUmPain011ValidaOcorrerErrorEventoResponseFactory_quandoProcessarPedidoCancelamentoRecebedor_DeveLancarException() {
                    var recorrenciaAutorizacao = RecorrenciaAutorizacao.builder()
                            .cpfCnpjPagador(CPF_PAGADOR)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR)
                            .build();
                    when(autorizacaoRepository.findByIdRecorrenciaAndTipoStatusIn(any(), any()))
                            .thenReturn(Optional.of(recorrenciaAutorizacao));

                    var pain011 = criarPain011(
                            CPF_DO_SOLICITANTE_DIFERENTE,
                            CPF_PAGADOR,
                            CPF_RECEBEDOR,
                            ID_RECORRENCIA,
                            MotivoCancelamentoPain11.SUPEITA_DE_FRAUDE
                    );

                    when(eventoResponseFactory.criarEventoPain012(any(), any()))
                            .thenThrow(new RuntimeException("Erro ao criar evento Pain12IcomRetorno"));

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    assertThrows(RuntimeException.class, () -> service.processarPedidoCancelamentoRecebedor(build));

                    verify(autorizacaoRepository).findByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, List.of(APROVADA));
                    verify(eventoResponseFactory).criarEventoPain012(any(Pain012Dto.class), eq(HEADER_OPERACAO_CANCELAMENTO_RECEBEDOR));
                    verify(operacaoRequestCriaResponseStrategyFactory, never()).criar(TipoResponseIdempotente.OPERACAO);
                }
            }
        }

        @Nested
        @DisplayName("Teste de Processamento de Solicitação Autorização")
        class ProcessaSolicitacaoAutorizacaoTest {

            private static final String CPF_PAGADOR_SOLICITACAO = "674778388383";
            private static final String CPF_SOLICITANTE_SOLICITACAO = "674778388383";
            private static final String CPF_RECEBEDOR_SOLICITACAO = "2883642388234";

            private static final String DEFAULT_JSON_SOLICITACAO = "{\"bar\":\"bar\"}";
            private static final Map<String, String> DEFAULT_HEADERS_SOLICITACAO = Map.of("bar", "bar");
            private static final String TOPICO_SOLICITACAO = "topico-teste-enviado";

            @Nested
            @DisplayName("Valida retorno")
            class CasoDeRetornoValido {

                private final IdempotentResponse<?> response = IdempotentResponse.builder().build();

                private final List<TipoStatusSolicitacaoAutorizacao> statusRecorrenciaAutorizacaoSolicitacao = List.of(
                        TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO,
                        TipoStatusSolicitacaoAutorizacao.CRIADA
                );

                @SuppressWarnings("unchecked")
                @BeforeEach
                void setup() {
                    var responseStrategy = Mockito.mock(CriaResponseStrategy.class);
                    when(eventoResponseFactory.criarEventoPain012(any(), any()))
                            .thenReturn(new EventoResponseDTO(DEFAULT_JSON_SOLICITACAO, DEFAULT_HEADERS_SOLICITACAO, TOPICO_SOLICITACAO));

                    when(operacaoRequestCriaResponseStrategyFactory.criar(any()))
                            .thenReturn(responseStrategy);

                    when(responseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any()))
                            .thenReturn(response);
                }

                @ParameterizedTest(name = """
                Dado uma Pain011 com dados validos, com motivo {0},
                Quando processarPedidoAutorizacaoRecebedor,
                Deve salvar Recorrencia Autorização com status APROVADA
                """)
                @EnumSource(
                        value = MotivoCancelamentoPain11.class,
                        mode = EnumSource.Mode.INCLUDE,
                        names = {
                                "ERROR_SOLICITACAO_DE_CONFIRMACAO",
                                "FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR",
                                "CONFIRMADA_POR_OUTRA_JORNADA"
                        })
                void dadoUmaPain011Valida_quandoProcessarPedidoAutorizacaoRecebedor_DeveSalvarRecorrenciaAutorizacaoComStatusAprovada(MotivoCancelamentoPain11 motivoCancelamento) {
                    var recorrenciaAutorizacaoSolicitacao = SolicitacaoAutorizacaoRecorrencia.builder()
                            .idRecorrencia(ID_RECORRENCIA)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR_SOLICITACAO)
                            .cpfCnpjPagador(CPF_PAGADOR_SOLICITACAO)
                            .codigoMunicipioIBGE(CODIGO_IBGE)
                            .build();

                    when(solicitacaoRepository.findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao))
                            .thenReturn(Optional.of(recorrenciaAutorizacaoSolicitacao));
                    when(recorrenciaAutorizacaoCancelamentoRepository.save(any()))
                            .thenReturn(RecorrenciaAutorizacaoCancelamento.builder()
                                    .dataAlteracaoRegistro(LocalDateTime.now())
                                    .build());

                    var pain011 = criarPain011(
                            CPF_SOLICITANTE_SOLICITACAO,
                            CPF_PAGADOR_SOLICITACAO,
                            CPF_RECEBEDOR_SOLICITACAO,
                            ID_RECORRENCIA,
                            motivoCancelamento
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository, never())
                            .atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(solicitacaoRepository)
                            .findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao);
                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            null,
                            TipoCancelamento.RECORRENCIA_SOLICITACAO,
                            2,
                            CODIGO_IBGE);

                }

                @ParameterizedTest(name = """
                    Dado uma Pain011 com cpf do pagador diferente da recorrencia,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar Recorrencia Autorização cancelamento com erro AP02 e retornar IdempontenciaResponse com erro
                    """)
                @EnumSource(
                        value = MotivoCancelamentoPain11.class,
                        mode = EnumSource.Mode.INCLUDE,
                        names = {
                                "ERROR_SOLICITACAO_DE_CONFIRMACAO",
                                "FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR",
                                "CONFIRMADA_POR_OUTRA_JORNADA"
                        })
                void dadoUmaPain011ComCpfDoPagadorDiferenteDaRecorrencia_quandoProcessarPedidoCancelamentoRecebedor_DeveSalvarRecorrenciaAutorizacaoCancelamentoComErroAP02ERetornarIdempontenciaResponseComErro(
                        MotivoCancelamentoPain11 motivoCancelamento
                ) {
                    var recorrenciaAutorizacaoSolicitacao = SolicitacaoAutorizacaoRecorrencia.builder()
                            .idRecorrencia(ID_RECORRENCIA)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR_SOLICITACAO)
                            .cpfCnpjPagador(CPF_PAGADOR_SOLICITACAO)
                            .build();

                    when(solicitacaoRepository.findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao))
                            .thenReturn(Optional.of(recorrenciaAutorizacaoSolicitacao));

                    when(recorrenciaAutorizacaoCancelamentoRepository.save(any()))
                            .thenReturn(RecorrenciaAutorizacaoCancelamento.builder()
                                    .dataAlteracaoRegistro(LocalDateTime.now())
                                    .build());

                    var pain011 = criarPain011(
                            CPF_SOLICITANTE_SOLICITACAO,
                            CPF_DIFERENTE_DO_EXPERADO,
                            CPF_RECEBEDOR_SOLICITACAO,
                            ID_RECORRENCIA,
                            motivoCancelamento
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository, never())
                            .atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(solicitacaoRepository)
                            .findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao);

                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO.name(),
                            TipoCancelamento.RECORRENCIA_SOLICITACAO,
                            0,
                            null);

                }

                @ParameterizedTest(name = """
                    Dado uma Pain011 com cpf do recebedor diferente da recorrencia,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar Recorrencia Autorização cancelamento com erro AP06 e retornar IdempontenciaResponse com erro
                    """)
                @EnumSource(
                        value = MotivoCancelamentoPain11.class,
                        mode = EnumSource.Mode.INCLUDE,
                        names = {
                                "ERROR_SOLICITACAO_DE_CONFIRMACAO",
                                "FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR",
                                "CONFIRMADA_POR_OUTRA_JORNADA"
                        })
                void dadoUmaPain011ComCpfDoRecebedorDiferenteDaRecorrencia_quandoProcessarPedidoCancelamentoRecebedor_DeveSalvarRecorrenciaAutorizacaoCancelamentoComErroAP06ERetornarIdempontenciaResponseComErro(
                        MotivoCancelamentoPain11 motivoCancelamento
                ) {
                    var recorrenciaAutorizacaoSolicitacao = SolicitacaoAutorizacaoRecorrencia.builder()
                            .idRecorrencia(ID_RECORRENCIA)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR_SOLICITACAO)
                            .cpfCnpjPagador(CPF_PAGADOR_SOLICITACAO)
                            .codigoMunicipioIBGE(CODIGO_IBGE)
                            .build();

                    when(solicitacaoRepository.findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao))
                            .thenReturn(Optional.of(recorrenciaAutorizacaoSolicitacao));

                    when(recorrenciaAutorizacaoCancelamentoRepository.save(any()))
                            .thenReturn(RecorrenciaAutorizacaoCancelamento.builder()
                                    .dataAlteracaoRegistro(LocalDateTime.now())
                                    .build());

                    var pain011 = criarPain011(
                            CPF_SOLICITANTE_SOLICITACAO,
                            CPF_PAGADOR_SOLICITACAO,
                            CPF_DIFERENTE_DO_EXPERADO,
                            ID_RECORRENCIA,
                            motivoCancelamento
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository, never())
                            .atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(solicitacaoRepository)
                            .findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao);

                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            CPF_CNPJ_USUARIO_RECEBEDOR_DIVERGENTE.name(),
                            TipoCancelamento.RECORRENCIA_SOLICITACAO,
                            0,
                            CODIGO_IBGE);
                }

                @ParameterizedTest(name = """
                    Dado uma Pain011 com cpf do recebedor diferente da recorrencia,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar Recorrencia Autorização cancelamento com erro AP10 e retornar IdempontenciaResponse com erro
                    """)
                @EnumSource(
                        value = MotivoCancelamentoPain11.class,
                        mode = EnumSource.Mode.INCLUDE,
                        names = {
                                "ERROR_SOLICITACAO_DE_CONFIRMACAO",
                                "FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR",
                                "CONFIRMADA_POR_OUTRA_JORNADA"
                        })
                void dadoUmaPain011ComCpfDoRecebedorDiferenteDaRecorrencia_quandoProcessarPedidoCancelamentoRecebedor_DeveSalvarRecorrenciaAutorizacaoCancelamentoComErroAP10ERetornarIdempontenciaResponseComErro(
                        MotivoCancelamentoPain11 motivoCancelamento
                ) {
                    var recorrenciaAutorizacaoSolicitacao = SolicitacaoAutorizacaoRecorrencia.builder()
                            .idRecorrencia(ID_RECORRENCIA)
                            .cpfCnpjRecebedor(CPF_RECEBEDOR_SOLICITACAO)
                            .cpfCnpjPagador(CPF_PAGADOR_SOLICITACAO)
                            .codigoMunicipioIBGE(CODIGO_IBGE)
                            .build();

                    when(solicitacaoRepository.findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao))
                            .thenReturn(Optional.of(recorrenciaAutorizacaoSolicitacao));

                    when(recorrenciaAutorizacaoCancelamentoRepository.save(any()))
                            .thenReturn(RecorrenciaAutorizacaoCancelamento.builder()
                                    .dataAlteracaoRegistro(LocalDateTime.now())
                                    .build());

                    var pain011 = criarPain011(
                            CPF_DIFERENTE_DO_EXPERADO,
                            CPF_PAGADOR_SOLICITACAO,
                            CPF_RECEBEDOR_SOLICITACAO,
                            ID_RECORRENCIA,
                            motivoCancelamento
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);

                    verify(autorizacaoRepository, never())
                            .atualizaSubStatusPorIdRecorrencia(ID_RECORRENCIA, AGUARDANDO_CANCELAMENTO.name());
                    verify(solicitacaoRepository)
                            .findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao);

                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            CPF_CNPJ_SOLICITANTE_CANCELAMENTO_NAO_CORRESPONDE_AO_DA_RECORRENCIA.name(),
                            TipoCancelamento.RECORRENCIA_SOLICITACAO,
                            0,
                            CODIGO_IBGE);
                }

                @ParameterizedTest(name = """
                    Dado uma Pain011 que não corresponde a uma solicitação recorrencia,
                    Quando processarPedidoCancelamentoRecebedor,
                    Deve salvar Recorrencia Autorização cancelamento com erro AP09 e retornar IdempontenciaResponse com erro
                    """)
                @EnumSource(
                        value = MotivoCancelamentoPain11.class,
                        mode = EnumSource.Mode.INCLUDE,
                        names = {
                                "ERROR_SOLICITACAO_DE_CONFIRMACAO",
                                "FALTA_DE_CONFIRMACAO_RECEBIMENTO_PELO_PSP_PAGADOR",
                                "CONFIRMADA_POR_OUTRA_JORNADA"
                        })
                void dadoUmaPain011QueNaoCorrespondeAUmasolicitacaoRecorrencia_quandoProcessarPedidoCancelamentoRecebedor_DeveSalvarRecorrenciaAutorizacaoCancelamentoComErroAP09ERetornarIdempontenciaResponseComErro(
                        MotivoCancelamentoPain11 motivoCancelamento
                ) {
                    when(solicitacaoRepository.findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao))
                            .thenReturn(Optional.empty());

                    when(recorrenciaAutorizacaoCancelamentoRepository.save(any()))
                            .thenReturn(RecorrenciaAutorizacaoCancelamento.builder()
                                    .dataAlteracaoRegistro(LocalDateTime.now())
                                    .build());

                    var pain011 = criarPain011(
                            CPF_SOLICITANTE_SOLICITACAO,
                            CPF_PAGADOR_SOLICITACAO,
                            CPF_RECEBEDOR_SOLICITACAO,
                            ID_RECORRENCIA,
                            motivoCancelamento
                    );

                    var build = IdempotentRequest.<Pain011Dto>builder()
                            .value(pain011)
                            .build();

                    var resultado = service.processarPedidoCancelamentoRecebedor(build);

                    assertEquals(response, resultado);
                    verify(autorizacaoRepository, never()).atualizaSubStatusPorIdRecorrencia(any(), any());

                    verify(solicitacaoRepository)
                            .findFirstByIdRecorrenciaAndTipoStatusIn(ID_RECORRENCIA, statusRecorrenciaAutorizacaoSolicitacao);

                    verificacaoDeStubsBasicos();
                    validaCamposDaPainERecorrenciaAutorizacaoCancelamento(
                            pain011,
                            SOLICITACAO_CONFIRMACAO_NAO_IDENTIFICADA.name(),
                            TipoCancelamento.RECORRENCIA_SOLICITACAO,
                            0,
                            null);
                }
            }
        }

        private void validaCamposDaPainERecorrenciaAutorizacaoCancelamento(Pain011Dto pain011,
                                                                           String motivoRejeicao,
                                                                           TipoCancelamento tipoCancelamento,
                                                                           int qtdDetalhes,
                                                                           String codigoIbge) {
            var pain012 = pain012DtoArgumentCaptor.getValue();
            validaCamposPain012(pain012, pain011, ID_RECORRENCIA, motivoRejeicao, qtdDetalhes, codigoIbge);

            var recorrenciaAutorizacaoCancelamento = recorrenciaAutorizacaoCancelamentoArgumentCaptor.getValue();
            validaCamposRecorrenciaAutorizacaoCancelamento(
                    recorrenciaAutorizacaoCancelamento,
                    pain011,
                    pain012.getIdInformacaoStatus(),
                    tipoCancelamento,
                    motivoRejeicao
            );
        }

        private void verificacaoDeStubsBasicos() {
            verify(eventoResponseFactory).criarEventoPain012(pain012DtoArgumentCaptor.capture(), eq(HEADER_OPERACAO_CANCELAMENTO_RECEBEDOR));
            verify(operacaoRequestCriaResponseStrategyFactory).criar(TipoResponseIdempotente.OPERACAO);
            verify(recorrenciaAutorizacaoCancelamentoRepository).save(recorrenciaAutorizacaoCancelamentoArgumentCaptor.capture());
        }

        private void validaCamposPain012(Pain012Dto pain012,
                                         Pain011Dto pain011,
                                         String idRecorrenca,
                                         String motivoRejeicao,
                                         int qtdDetalhes,
                                         String codigoIbge) {
            assertThat(pain012)
                    .extracting(
                            Pain012Dto::getIdRecorrencia,
                            Pain012Dto::getMotivoRejeicao,
                            Pain012Dto::getTipoFrequencia,
                            Pain012Dto::getTipoRecorrencia,
                            Pain012Dto::getDataInicialRecorrencia,
                            Pain012Dto::getDataFinalRecorrencia,
                            Pain012Dto::getNomeUsuarioRecebedor,
                            Pain012Dto::getCpfCnpjUsuarioRecebedor,
                            Pain012Dto::getParticipanteDoUsuarioRecebedor,
                            Pain012Dto::getCodMunIBGE,
                            Pain012Dto::getCpfCnpjUsuarioPagador,
                            Pain012Dto::getContaUsuarioPagador,
                            Pain012Dto::getAgenciaUsuarioPagador,
                            Pain012Dto::getParticipanteDoUsuarioPagador,
                            Pain012Dto::getNomeDevedor,
                            Pain012Dto::getCpfCnpjDevedor,
                            Pain012Dto::getNumeroContrato,
                            Pain012Dto::getDescricao,
                            Pain012Dto::getIndicadorObrigatorioOriginal,
                            Pain012Dto::getValor,
                            pain012Dto -> Optional.ofNullable(pain012Dto.getDetalhesRecorrencias())
                                    .map(List::size)
                                    .orElse(0)
                    )
                    .satisfies(
                            tuple -> {
                                assertThat(tuple.get(0)).isEqualTo(idRecorrenca);
                                assertThat(tuple.get(1)).isEqualTo(motivoRejeicao);
                                assertThat(tuple.get(2)).isEqualTo(TipoFrequencia.MENSAL.name());
                                assertThat(tuple.get(3)).isEqualTo(pain011.getTipoRecorrencia());
                                assertThat(tuple.get(4)).isEqualTo(pain011.getDataInicialRecorrencia());
                                assertThat(tuple.get(5)).isEqualTo(pain011.getDataFinalRecorrencia());
                                assertThat(tuple.get(6)).isEqualTo(pain011.getNomeUsuarioRecebedor());
                                assertThat(tuple.get(7)).isEqualTo(pain011.getCpfCnpjUsuarioRecebedor());
                                assertThat(tuple.get(8)).isEqualTo(pain011.getParticipanteDoUsuarioRecebedor());
                                assertThat(tuple.get(9)).isEqualTo(codigoIbge);
                                assertThat(tuple.get(10)).isEqualTo(pain011.getCpfCnpjUsuarioPagador());
                                assertThat(tuple.get(11)).isEqualTo(pain011.getContaUsuarioPagador());
                                assertThat(tuple.get(12)).isEqualTo(pain011.getAgenciaUsuarioPagador());
                                assertThat(tuple.get(13)).isEqualTo(pain011.getParticipanteDoUsuarioPagador());
                                assertThat(tuple.get(14)).isEqualTo(pain011.getNomeDevedor());
                                assertThat(tuple.get(15)).isEqualTo(pain011.getCpfCnpjDevedor());
                                assertThat(tuple.get(16)).isEqualTo(pain011.getNumeroContrato());
                                assertThat(tuple.get(17)).isEqualTo(pain011.getDescricao());
                                assertThat(tuple.get(18)).isEqualTo(pain011.getIndicadorObrigatorio());
                                assertThat(tuple.get(19)).isEqualTo(pain011.getValor());
                                assertThat(tuple.get(20)).isEqualTo(qtdDetalhes);
                            }
                    );
        }

        private void validaCamposRecorrenciaAutorizacaoCancelamento(RecorrenciaAutorizacaoCancelamento recorrencia,
                                                                    Pain011Dto dto,
                                                                    String idInformacaoStatus,
                                                                    TipoCancelamento tipoCancelamento,
                                                                    String motivaRejeicao) {


            var dataCancelamento = Optional.ofNullable(dto.getDetalhesRecorrencias())
                    .stream()
                    .flatMap(Collection::stream)
                    .filter(detalheRecorrencia -> {
                        var situacao = TipoSituacaoPain011.of(detalheRecorrencia.getTipoSituacao());
                        return TipoSituacaoPain011.DATA_CANCELAMENTO == situacao;
                    })
                    .map(DetalheRecorrenciaPain011Dto::getDataHoraRecorrencia)
                    .findFirst()
                    .orElse(null);

            assertThat(recorrencia)
                    .extracting(
                            RecorrenciaAutorizacaoCancelamento::getIdInformacaoCancelamento,
                            RecorrenciaAutorizacaoCancelamento::getIdRecorrencia,
                            RecorrenciaAutorizacaoCancelamento::getIdInformacaoStatus,
                            RecorrenciaAutorizacaoCancelamento::getTipoCancelamento,
                            RecorrenciaAutorizacaoCancelamento::getTipoSolicitanteCancelamento,
                            RecorrenciaAutorizacaoCancelamento::getTipoStatus,
                            RecorrenciaAutorizacaoCancelamento::getCpfCnpjSolicitanteCancelamento,
                            RecorrenciaAutorizacaoCancelamento::getMotivoCancelamento,
                            RecorrenciaAutorizacaoCancelamento::getDataAlteracaoRegistro,
                            RecorrenciaAutorizacaoCancelamento::getMotivoRejeicao,
                            RecorrenciaAutorizacaoCancelamento::getDataCancelamento
                    )
                    .satisfies(
                            tuple -> {
                                assertThat(tuple.get(0)).isEqualTo(dto.getIdInformacaoCancelamento());
                                assertThat(tuple.get(1)).isEqualTo(dto.getIdRecorrencia());
                                assertThat(tuple.get(2)).isEqualTo(idInformacaoStatus);
                                assertThat(tuple.get(3)).isEqualTo(tipoCancelamento);
                                assertThat(tuple.get(4)).isEqualTo(TipoSolicitanteCancelamento.RECEBEDOR);
                                assertThat(tuple.get(5)).isEqualTo(TipoStatusCancelamentoAutorizacao.CRIADA);
                                assertThat(tuple.get(6)).isEqualTo(dto.getCpfCnpjSolicitanteCancelamento());
                                assertThat(tuple.get(7)).isEqualTo(dto.getMotivoCancelamento());
                                assertThat(tuple.get(8)).isEqualTo(recorrencia.getDataAlteracaoRegistro());
                                assertThat(tuple.get(9)).isEqualTo(motivaRejeicao);
                                assertThat(tuple.get(10)).isEqualTo(dataCancelamento);
                            }
                    );
        }
    }

    @Nested
    @DisplayName("Teste de processamento de pedido de cancelamento Pagador")
    class ProcessamentoPedidoCancelamentoPagador {

        private static final String ID_RECORRENCIA = "ID_RECORRENCIA";
        private static final String CODIGO_TIPO_TRANSACAO = "441";

        private final IdempotentResponse<?> response = IdempotentResponse.builder().build();

        @Nested
        @DisplayName("Valida retorno")
        class CasoDeRetornoValido {

            private CriaResponseStrategy responseStrategy = Mockito.mock(CriaResponseStrategy.class);

            @SuppressWarnings("unchecked")
            @BeforeEach
            void setup() {
                when(responseStrategy.criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), eq(Collections.emptyList())))
                        .thenReturn(response);
            }

            @Test
            @DisplayName("""
            Dado uma Pain011 valida
            Quando processarPedidoCancelamentoPagador,
            Deve retornar Idempotencia com Sucesso e Atualizar a entidade RecorrenciaAutorizacaoCancelamento
            """)
            void dadoUmaPain011Valida_quandoProcessarPedidoCancelamentoPagador_DeveRetornarIdempotenciaComSucessoEAtualizarAEntidadeRecorrenciaAutorizacaoCancelamento() {
                var pain011 = criarPain011(
                        CPF_DO_SOLICITANTE,
                        CPF_PAGADOR,
                        CPF_RECEBEDOR,
                        ID_RECORRENCIA,
                        MotivoCancelamentoPain11.CONFIRMADA_POR_OUTRA_JORNADA
                );

                when(operacaoRequestCriaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                        .thenReturn(responseStrategy);

                doNothing()
                        .when(recorrenciaAutorizacaoCancelamentoRepository)
                        .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(
                                ID_INFORMACAO_CANCELAMENTO,
                                ID_RECORRENCIA,
                                TipoStatusCancelamentoAutorizacao.ENVIADA
                        );
                when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO))
                        .thenReturn(new ProtocoloDTO());

                var request = IdempotentAsyncRequest.<Pain011Dto>builder()
                        .value(pain011)
                        .build();

                var resultado = service.processarPedidoCancelamentoPagador(request);

                assertEquals(response, resultado);

                verify(recorrenciaAutorizacaoCancelamentoRepository)
                        .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(
                                ID_INFORMACAO_CANCELAMENTO,
                                ID_RECORRENCIA,
                                TipoStatusCancelamentoAutorizacao.ENVIADA
                        );
                verify(canaisDigitaisProtocoloInfoInternalApiClient)
                        .consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO);
            }

            @Test
            @DisplayName("""
            Dado uma Pain011 valida, mas não retornar o protocolo
            Quando processarPedidoCancelamentoPagador,
            Deve retornar Idempotencia com Sucesso e Atualizar a entidade RecorrenciaAutorizacaoCancelamento
            """)
            void dadoUmaPain011ValidaSemRetornoDoProtocolo_quandoProcessarPedidoCancelamentoPagador_DeveRetornarIdempotenciaComSucessoEAtualizarAEntidadeRecorrenciaAutorizacaoCancelamento() {
                var pain011 = criarPain011(
                        CPF_DO_SOLICITANTE,
                        CPF_PAGADOR,
                        CPF_RECEBEDOR,
                        ID_RECORRENCIA,
                        MotivoCancelamentoPain11.CONFIRMADA_POR_OUTRA_JORNADA
                );

                when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO))
                        .thenReturn(null);

                var request = IdempotentAsyncRequest.<Pain011Dto>builder()
                        .value(pain011)
                        .build();

                assertNull(service.processarPedidoCancelamentoPagador(request));

                verify(recorrenciaAutorizacaoCancelamentoRepository, never())
                        .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(any(), any(), any());
                verify(canaisDigitaisProtocoloInfoInternalApiClient)
                        .consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO);
            }
        }

        @Nested
        @DisplayName("Caso de Execeptions")
        class CasoDeExeceptions {

            @Test
            @DisplayName("""
            Dado uma Pain011 valida, mas, ocorre error na chamada da busca do protocolo
            Quando processarPedidoCancelamentoPagador
            Deve retornar TechnicalException
            """)
            void dadoUmaPain011ValidaMasOcorreErrorNaChamadaDaBuscaDoProtocolo_quandoProcessarPedidoCancelamentoPagador_DeveRetornarTechnicalException() {
                var pain011 = criarPain011(
                        CPF_DO_SOLICITANTE,
                        CPF_PAGADOR,
                        CPF_RECEBEDOR,
                        ID_RECORRENCIA,
                        MotivoCancelamentoPain11.CONFIRMADA_POR_OUTRA_JORNADA
                );

                when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO))
                        .thenThrow(new TechnicalException("Erro"));

                var request = IdempotentAsyncRequest.<Pain011Dto>builder()
                        .value(pain011)
                        .build();

                assertThrows(TechnicalException.class, () -> service.processarPedidoCancelamentoPagador(request));

                verify(recorrenciaAutorizacaoCancelamentoRepository, never())
                        .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(any(), any(), any());

                verify(canaisDigitaisProtocoloInfoInternalApiClient)
                        .consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO);
            }

            @Test
            @DisplayName("""
            Dado uma Pain011 valida, mas, ocorre error na chamada do update do recorrencia
            Quando processarPedidoCancelamentoPagador
            Deve retornar TechnicalException
            """)
            void dadoUmaPain011ValidaMasOcorreErrorNaChamadaDoUpdateDoRecorrencia_quandoProcessarPedidoCancelamentoPagador_DeveRetornarTechnicalException() {
                var responseStrategy = Mockito.mock(CriaResponseStrategy.class);

                when(operacaoRequestCriaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                        .thenReturn(responseStrategy);

                when(responseStrategy.criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), eq(Collections.emptyList())))
                        .thenReturn(response);
                var pain011 = criarPain011(
                        CPF_DO_SOLICITANTE,
                        CPF_PAGADOR,
                        CPF_RECEBEDOR,
                        ID_RECORRENCIA,
                        MotivoCancelamentoPain11.CONFIRMADA_POR_OUTRA_JORNADA
                );

                when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO))
                        .thenReturn(new ProtocoloDTO());

                doThrow(new TechnicalException("Erro"))
                        .when(recorrenciaAutorizacaoCancelamentoRepository)
                        .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(
                                ID_INFORMACAO_CANCELAMENTO,
                                ID_RECORRENCIA,
                                TipoStatusCancelamentoAutorizacao.ENVIADA
                        );
                var request = IdempotentAsyncRequest.<Pain011Dto>builder()
                        .value(pain011)
                        .build();

                assertThrows(TechnicalException.class, () -> service.processarPedidoCancelamentoPagador(request));

                verify(recorrenciaAutorizacaoCancelamentoRepository)
                        .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(
                                ID_INFORMACAO_CANCELAMENTO,
                                ID_RECORRENCIA,
                                TipoStatusCancelamentoAutorizacao.ENVIADA
                        );
                verify(canaisDigitaisProtocoloInfoInternalApiClient)
                        .consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO);
            }

            @Test
            @DisplayName("""
            Dado uma Pain011 valida mas, ocorre error na construção do IdempotenciaResponse
            Quando processaPedidoCancelamentoPagador
            Deve retornar TechnicalExceptiono
            """)
            void dadoUmaPain011ValidaMasOcorreErrorNaConstrucaoDoIdempotenciaResponse_quandoProcessarPedidoCancelamentoPagador_DeveRetornarTechnicalException() {
                var pain011 = criarPain011(
                        CPF_DO_SOLICITANTE,
                        CPF_PAGADOR,
                        CPF_RECEBEDOR,
                        ID_RECORRENCIA,
                        MotivoCancelamentoPain11.CONFIRMADA_POR_OUTRA_JORNADA
                );

                when(operacaoRequestCriaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                        .thenThrow(new RuntimeException());

                when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_TIPO_TRANSACAO, ID_INFORMACAO_CANCELAMENTO))
                        .thenReturn(new ProtocoloDTO());

                var request = IdempotentAsyncRequest.<Pain011Dto>builder()
                        .value(pain011)
                        .build();

                assertThrows(RuntimeException.class, () -> service.processarPedidoCancelamentoPagador(request));
                verify(recorrenciaAutorizacaoCancelamentoRepository, never())
                        .atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(any(), any(), any());
            }
        }
    }


    private static Pain011Dto criarPain011(String cpfSolicitante,
                                           String cpfPagador,
                                           String cnpjRecebedor,
                                           String idRecorrenca,
                                           MotivoCancelamentoPain11 motivoCancelamentoPain11) {
        return Pain011Dto.builder()
                .cpfCnpjSolicitanteCancelamento(cpfSolicitante)
                .motivoCancelamento(motivoCancelamentoPain11.name())
                .idRecorrencia(idRecorrenca)
                .idInformacaoCancelamento(ID_INFORMACAO_CANCELAMENTO)
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
                                .build(),
                        DetalheRecorrenciaPain011Dto.builder()
                                .dataHoraRecorrencia(LocalDateTime.now())
                                .tipoSituacao(TipoSituacaoPain011.DATA_CANCELAMENTO.name())
                                .build()
                ))
                .build();
    }
}