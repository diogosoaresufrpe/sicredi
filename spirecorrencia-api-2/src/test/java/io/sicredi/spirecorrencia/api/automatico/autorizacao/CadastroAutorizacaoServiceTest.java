package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.TipoRecorrencia;
import br.com.sicredi.spi.entities.type.TipoSituacaoRecorrenciaPain012;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import jakarta.validation.Validator;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.AUTORIZACAO_JA_APROVADA_ANTERIORMENTE;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.PERMISSAO_RETENTATIVA_INVALIDA;
import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.ID_FIM_A_FIM;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CadastroAutorizacaoServiceTest {

    private static final String TOPICO = "topico";

    @Mock
    private Validator validator;
    @Mock
    private CriaResponseStrategyFactory<CadastroAutorizacaoRequest> criaResponseStrategyFactory;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private RecorrenciaAutorizacaoPagamentoImediatoRepository pagamentoImediatoRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Mock
    private CriaResponseStrategy<CadastroAutorizacaoRequest> criaResponseStrategy;
    @InjectMocks
    private CadastroAutorizacaoService service;
    @Captor
    private ArgumentCaptor<Pain012Dto> captorPain012;
    @Captor
    private ArgumentCaptor<RecorrenciaAutorizacaoPagamentoImediato> pagamentoImediatoArgumentCaptor;
    @Captor
    private ArgumentCaptor<ErroDTO> captorErro;

    @Nested
    class ProcessarCadastroAutorizacao{

        @Test
        void dadaDadosValidos_quandoProcessarCadastroAutorizacao_deveCriarAutorizacaoEEnviarPain012() {
            var cadastroAutorizacaoRequest = criarRequestAutorizacaoCadastro();

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(cadastroAutorizacaoRequest).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(autorizacaoService.consultarAutorizacaoPorIdEStatus(cadastroAutorizacaoRequest.getIdRecorrencia(), TipoStatusAutorizacao.APROVADA)).thenReturn(Optional.empty());
            when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroAutorizacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarCadastroAutorizacao(request);

            verify(pagamentoImediatoRepository, never()).save(any());
            verify(validator, times(1)).validate(any());
            verify(autorizacaoService, times(1)).consultarAutorizacaoPorIdEStatus(any(), any());
            verify(autorizacaoService, times(1)).salvar(any());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(CadastroAutorizacaoRequest.class), any(), any(), any());

            var pain012 = captorPain012.getValue();

            validarDadosPain012(pain012, cadastroAutorizacaoRequest, TipoSituacaoRecorrenciaPain012.AUTORIZACAO_PAGAMENTO_JORNADA_2, cadastroAutorizacaoRequest.getDataHoraInicioCanal());
        }

        @Test
        void dadoTipoJornada3_quandoProcessarCadastroAutorizacao_deveCriarAutorizacaoEPagamentoImediatoEEnviarPain012() {
            var cadastroAutorizacaoRequest = criarRequestAutorizacaoCadastro();

            cadastroAutorizacaoRequest.setTipoJornada(TipoJornada.JORNADA_3);

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(cadastroAutorizacaoRequest).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(autorizacaoService.consultarAutorizacaoPorIdEStatus(cadastroAutorizacaoRequest.getIdRecorrencia(), TipoStatusAutorizacao.APROVADA)).thenReturn(Optional.empty());
            when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroAutorizacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarCadastroAutorizacao(request);

            verify(validator, times(1)).validate(any());
            verify(autorizacaoService, times(1)).consultarAutorizacaoPorIdEStatus(any(), any());
            verify(autorizacaoService, times(1)).salvar(any());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(CadastroAutorizacaoRequest.class), any(), any(), any());
            verify(pagamentoImediatoRepository, times(1)).save(pagamentoImediatoArgumentCaptor.capture());

            var pain012 = captorPain012.getValue();
            var pagamentoImediatoValue = pagamentoImediatoArgumentCaptor.getValue();

            validarDadosPain012(pain012, cadastroAutorizacaoRequest, TipoSituacaoRecorrenciaPain012.AUTORIZACAO_PAGAMENTO_JORNADA_3, cadastroAutorizacaoRequest.getDataRecebimentoConfirmacaoPacs002PagamentoImediato());

            assertAll(
                    () -> assertEquals(cadastroAutorizacaoRequest.getIdFimAFimPagamentoImediato(), pagamentoImediatoValue.getIdFimAFim()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getDataRecebimentoConfirmacaoPacs002PagamentoImediato(), pagamentoImediatoValue.getDataRecebimentoConfirmacao())
            );
        }

        private void validarDadosPain012(Pain012Dto pain012, CadastroAutorizacaoRequest cadastroAutorizacaoRequest, TipoSituacaoRecorrenciaPain012 tipoSituacaoRecorrenciaPain012, LocalDateTime dataAtualizacaoAutorizacao) {
            assertAll(
                    () -> assertTrue(pain012.getStatus()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getIdInformacaoStatus(), pain012.getIdInformacaoStatus()),
                    () -> assertEquals(TipoRecorrencia.RECORRENTE.name(), pain012.getTipoRecorrencia()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getTipoFrequencia().name(), pain012.getTipoFrequencia()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getDataFinalRecorrencia(), pain012.getDataFinalRecorrencia()),
                    () -> assertFalse(pain012.getIndicadorObrigatorioOriginal()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getNomeRecebedor(), pain012.getNomeUsuarioRecebedor()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getCpfCnpjRecebedor(), pain012.getCpfCnpjUsuarioRecebedor()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getInstituicaoRecebedor(), pain012.getParticipanteDoUsuarioRecebedor()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getCodigoMunicipioIbge(), pain012.getCodMunIBGE()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getCpfCnpjPagador(), pain012.getCpfCnpjUsuarioPagador()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getContaPagador(), pain012.getContaUsuarioPagador()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getAgenciaPagador(), pain012.getAgenciaUsuarioPagador()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getInstituicaoPagador(), pain012.getParticipanteDoUsuarioPagador()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getNomeDevedor(), pain012.getNomeDevedor()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getCpfCnpjDevedor(), pain012.getCpfCnpjDevedor()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getContrato(), pain012.getNumeroContrato()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getObjeto(), pain012.getDescricao()),
                    () -> assertEquals(TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA.name(), pain012.getDetalhesRecorrencias().getFirst().getTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getDataCriacaoRecorrencia(), pain012.getDetalhesRecorrencias().getFirst().getDataHoraTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(TipoSituacaoRecorrenciaPain012.ATUALIZACAO_STATUS_RECORRENCIA.name(), pain012.getDetalhesRecorrencias().get(1).getTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(dataAtualizacaoAutorizacao, pain012.getDetalhesRecorrencias().get(1).getDataHoraTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(tipoSituacaoRecorrenciaPain012.name(), pain012.getDetalhesRecorrencias().getLast().getTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(cadastroAutorizacaoRequest.getDataHoraInicioCanal(), pain012.getDetalhesRecorrencias().getLast().getDataHoraTipoSituacaoDaRecorrencia())
            );
        }

    }

    @Nested
    class ProcessarCadastroAutorizacaoErroNegocio{

        @Test
        void dadaRecorrenciaJaAprovada_quandoProcessarCadastroAutorizacao_deveRetornarErroNegocio() {
            var cadastroAutorizacaoRequest = criarRequestAutorizacaoCadastro();

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(cadastroAutorizacaoRequest).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(autorizacaoService.consultarAutorizacaoPorIdEStatus(cadastroAutorizacaoRequest.getIdRecorrencia(), TipoStatusAutorizacao.APROVADA)).thenReturn(Optional.of(RecorrenciaAutorizacao.builder().build()));
            when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(CadastroAutorizacaoRequest.class), any(), any())).thenReturn(idempotentResponse);

            service.processarCadastroAutorizacao(request);

            verify(validator, times(1)).validate(any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(autorizacaoService, times(1)).consultarAutorizacaoPorIdEStatus(any(), any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(CadastroAutorizacaoRequest.class), any(), captorErro.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(CadastroAutorizacaoRequest.class), any(), any(), any());
            verify(pagamentoImediatoRepository, never()).save(any());
            verify(autorizacaoService, never()).salvar(any());
            verify(eventoResponseFactory, never()).criarEventoPain012(captorPain012.capture(), any());

            var erro = captorErro.getValue();

            assertAll(
                    () -> assertEquals(AUTORIZACAO_JA_APROVADA_ANTERIORMENTE, erro.codigoErro()),
                    () -> assertEquals(AUTORIZACAO_JA_APROVADA_ANTERIORMENTE.getMessage(), erro.mensagemErro())
            );
        }

        @Test
        void dadaPermissaoRetentativaIncosistente_quandoProcessarCadastroAutorizacao_deveRetornarErroNegocio() {
            var cadastroAutorizacaoRequest = criarRequestAutorizacaoCadastro();

            cadastroAutorizacaoRequest.setIdRecorrencia("RN9999900420250517G4Jdhm5o9A6");

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(cadastroAutorizacaoRequest).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(autorizacaoService.consultarAutorizacaoPorIdEStatus(cadastroAutorizacaoRequest.getIdRecorrencia(), TipoStatusAutorizacao.APROVADA)).thenReturn(Optional.empty());
            when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(CadastroAutorizacaoRequest.class), any(), any())).thenReturn(idempotentResponse);

            service.processarCadastroAutorizacao(request);

            verify(validator, times(1)).validate(any());
            verify(pagamentoImediatoRepository, never()).save(any());
            verify(autorizacaoService, never()).salvar(any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(autorizacaoService, times(1)).consultarAutorizacaoPorIdEStatus(any(), any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(CadastroAutorizacaoRequest.class), any(), captorErro.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(CadastroAutorizacaoRequest.class), any(), any(), any());

            verify(eventoResponseFactory, never()).criarEventoPain012(captorPain012.capture(), any());

            var erro = captorErro.getValue();

            assertAll(
                    () -> assertEquals(PERMISSAO_RETENTATIVA_INVALIDA, erro.codigoErro()),
                    () -> assertEquals(PERMISSAO_RETENTATIVA_INVALIDA.getMessage(), erro.mensagemErro())
            );
        }
    }

    @SuppressWarnings("rawtypes")
    @Nested
    class ProcessarCadastroAutorizacaoErroValidacao {

        private static final String TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS = "Teste de erro de validação de constraints";

        private static Stream<Arguments> provideNullOrInvalidStrings() {
            return Stream.of(
                    Arguments.of((Object) null),
                    Arguments.of(""),
                    Arguments.of(" ")
            );
        }

        @MethodSource("provideNullOrInvalidStrings")
        @ParameterizedTest
        void dadoIdRecorrenciaInvalido_quandoProcessarCadastroAutorizacao_deveRetornarErroValidacaoConstraint(String idRecorrencia) {
            var requestDTO = criarRequestAutorizacaoCadastro();
            requestDTO.setIdRecorrencia(idRecorrencia);

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(requestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarCadastroAutorizacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @MethodSource("provideNullOrInvalidStrings")
        @ParameterizedTest
        void dadoNomeDevedorInvalido_quandoProcessarCadastroAutorizacao_deveRetornarErroValidacaoConstraint(String nomeDevedor) {
            var requestDTO = criarRequestAutorizacaoCadastro();
            requestDTO.setNomePagador(nomeDevedor);

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(requestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarCadastroAutorizacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @ValueSource(doubles = {12345678901234567.89, -10.5})
        @ParameterizedTest
        void dadoValorMaximoInvalido_quandoProcessarCadastroAutorizacao_deveRetornarErroValidacaoConstraint(double valorMaximo) {
            var requestDTO = criarRequestAutorizacaoCadastro();
            requestDTO.setValor(BigDecimal.valueOf(valorMaximo));

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(requestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarCadastroAutorizacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @Test
        void dadoDataInicialRecorrenciaNula_quandoProcessarCadastroAutorizacao_deveRetornarErroValidacaoConstraint() {
            var requestDTO = criarRequestAutorizacaoCadastro();
            requestDTO.setContrato(null);

            var request = IdempotentAsyncRequest.<CadastroAutorizacaoRequest>builder().value(requestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarCadastroAutorizacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        private void criaStubsErroValidacaoConstraint(ConstraintViolationImpl mockConstraintViolation, String mensagemErro, IdempotentResponse idempotentResponse) {
            when(validator.validate(any())).thenReturn(Set.of(mockConstraintViolation));
            when(mockConstraintViolation.getMessage()).thenReturn(mensagemErro);
            when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);
        }

        private void validarAssercoesErroValidacaoConstraint(String mensagemEsperada) {
            verify(validator, times(1)).validate(any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErro.capture());

            var erroDTO = captorErro.getValue();
            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001, erroDTO.codigoErro()),
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemEsperada), erroDTO.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_VALIDACAO, erroDTO.tipoRetornoTransacaoEnum())
            );
        }

    }

    private CadastroAutorizacaoRequest criarRequestAutorizacaoCadastro() {
        return CadastroAutorizacaoRequest.builder()
                .idInformacaoStatus("status-123")
                .idRecorrencia("RR9999900420250517G4Jdhm5o9A6")
                .tipoJornada(TipoJornada.JORNADA_2)
                .tipoCanal(TipoCanalEnum.MOBI)
                .contrato("123456789012345")
                .objeto("mensalidade plano")
                .codigoMunicipioIbge("3550308")
                .nomeDevedor("João da Silva")
                .cpfCnpjDevedor("12345678901")
                .nomeRecebedor("Empresa XYZ LTDA")
                .cpfCnpjRecebedor("12345678000199")
                .instituicaoRecebedor("12345678")
                .tipoFrequencia(TipoFrequenciaPixAutomatico.MENSAL)
                .valor(new BigDecimal("150.00"))
                .pisoValorMaximo(new BigDecimal("100.00"))
                .valorMaximo(new BigDecimal("200.00"))
                .politicaRetentativa(PoliticaRetentativaRecorrenciaEnum.PERMITE_3R_7D)
                .dataInicialRecorrencia(LocalDate.now())
                .dataFinalRecorrencia(LocalDate.now().plusMonths(6))
                .dataCriacaoRecorrencia(LocalDateTime.now())
                .cpfCnpjPagador("98765432100")
                .nomePagador("Maria Oliveira")
                .instituicaoPagador("87654321")
                .agenciaPagador("1234")
                .contaPagador("987654321")
                .postoPagador("01")
                .tipoContaPagador(TipoContaEnum.CONTA_CORRENTE)
                .tipoPessoaPagador(TipoPessoaEnum.PF)
                .tipoOrigemSistema(OrigemEnum.LEGADO)
                .dataRecebimentoConfirmacaoPacs002PagamentoImediato(LocalDateTime.now())
                .idFimAFimPagamentoImediato(ID_FIM_A_FIM)
                .build();
    }
}