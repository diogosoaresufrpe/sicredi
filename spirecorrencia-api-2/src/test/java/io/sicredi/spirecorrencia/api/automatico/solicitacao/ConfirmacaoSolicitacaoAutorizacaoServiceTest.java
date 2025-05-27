package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spi.entities.type.TipoRecorrencia;
import br.com.sicredi.spi.entities.type.TipoSituacaoRecorrenciaPain012;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoService;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.ConfirmacaoAutorizacaoRequestDTO;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import jakarta.validation.Validator;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.automatico.autorizacao.ConfirmacaoAutorizacaoRequestDTO.criarDataHoraInicioCanal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmacaoSolicitacaoAutorizacaoServiceTest {

    private static final String CONTA = "conta";
    private static final String CPF_CNPJ_PAGADOR = "cpfCnpjPagador";
    private static final String ID_RECORRENCIA = "RRxxxxxxxxyyyyMMddkkkkkkkkkkk";
    private static final String TOPICO = "topico";
    private static final String COOPERATIVA = "cooperativa";
    private static final String HEADER_OPERACAO_RECORRENCIA_AUTORIZACAO = "RECORRENCIA_AUTORIZACAO";

    @Mock
    private Validator validator;

    @Mock
    private CriaResponseStrategyFactory<ConfirmacaoAutorizacaoRequestDTO> criaResponseStrategyFactory;

    @Mock
    private SolicitacaoAutorizacaoRecorrenciaService solicitacaoAutorizacaoService;

    @Mock
    private AutorizacaoService autorizacaoService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    @Mock
    private EventoResponseFactory eventoResponseFactory;

    @Mock
    private CriaResponseStrategy<ConfirmacaoAutorizacaoRequestDTO> criaResponseStrategy;

    @InjectMocks
    private ConfirmacaoSolicitacaoAutorizacaoService service;

    @Captor
    private ArgumentCaptor<Pain012Dto> captorPain012;

    @Captor
    private ArgumentCaptor<ErroDTO> captorErro;

    @Nested
    class ProcessamentoAceita {

        @Test
        void dadaUmaConfirmacaoAutorizacaoAceitaValida_quandoProcessar_deveCriarResponseIdempotentSucesso() {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setValorMaximo(null);
            confirmacaoAutorizacaoRequestDTO.setIdRecorrencia("RNxxxxxxxxyyyyMMddkkkkkkkkkkk");
            var solicitacao = buildSolicitacao();
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any()))
                    .thenReturn(solicitacao);
            doNothing().when(solicitacaoAutorizacaoService).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            when(autorizacaoService.salvar(any())).thenReturn(new RecorrenciaAutorizacao());
            when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(ConfirmacaoAutorizacaoRequestDTO.class), any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarConfirmacao(request);

            verify(validator, times(1)).validate(any());
            verify(solicitacaoAutorizacaoService, times(1)).buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any());
            verify(solicitacaoAutorizacaoService, times(1)).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            verify(autorizacaoService, times(1)).salvar(any());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_AUTORIZACAO));
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(ConfirmacaoAutorizacaoRequestDTO.class), any(), any(), any());

            var pain012 = captorPain012.getValue();

            assertAll(
                    () -> assertTrue(pain012.getStatus()),
                    () -> assertEquals("RNxxxxxxxxyyyyMMddkkkkkkkkkkk", pain012.getIdRecorrencia()),
                    () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getIdInformacaoStatus(), pain012.getIdInformacaoStatus()),
                    () -> assertEquals(TipoRecorrencia.RECORRENTE.name(), pain012.getTipoRecorrencia()),
                    () -> assertEquals(solicitacao.getTipoFrequencia(), pain012.getTipoFrequencia()),
                    () -> assertEquals(solicitacao.getDataFinalRecorrencia(), pain012.getDataFinalRecorrencia()),
                    () -> assertFalse(pain012.getIndicadorObrigatorioOriginal()),
                    () -> assertEquals(solicitacao.getNomeRecebedor(), pain012.getNomeUsuarioRecebedor()),
                    () -> assertEquals(solicitacao.getCpfCnpjRecebedor(), pain012.getCpfCnpjUsuarioRecebedor()),
                    () -> assertEquals(solicitacao.getInstituicaoRecebedor(), pain012.getParticipanteDoUsuarioRecebedor()),
                    () -> assertEquals(solicitacao.getCodigoMunicipioIBGE(), pain012.getCodMunIBGE()),
                    () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getCpfCnpjPagador(), pain012.getCpfCnpjUsuarioPagador()),
                    () -> assertEquals(solicitacao.getContaPagador(), pain012.getContaUsuarioPagador()),
                    () -> assertEquals(solicitacao.getAgenciaPagador(), pain012.getAgenciaUsuarioPagador()),
                    () -> assertEquals(solicitacao.getInstituicaoPagador(), pain012.getParticipanteDoUsuarioPagador()),
                    () -> assertEquals(solicitacao.getNomeDevedor(), pain012.getNomeDevedor()),
                    () -> assertEquals(solicitacao.getCpfCnpjDevedor(), pain012.getCpfCnpjDevedor()),
                    () -> assertEquals(solicitacao.getNumeroContrato(), pain012.getNumeroContrato()),
                    () -> assertEquals(solicitacao.getDescricao(), pain012.getDescricao()),
                    () -> assertEquals(TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA.name(), pain012.getDetalhesRecorrencias().getFirst().getTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(solicitacao.getDataCriacaoRecorrencia(), pain012.getDetalhesRecorrencias().getFirst().getDataHoraTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(TipoSituacaoRecorrenciaPain012.ATUALIZACAO_STATUS_RECORRENCIA.name(), pain012.getDetalhesRecorrencias().get(1).getTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getDataHoraInicioCanal(), pain012.getDetalhesRecorrencias().get(1).getDataHoraTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(TipoSituacaoRecorrenciaPain012.AUTORIZACAO_PAGAMENTO_JORNADA_1.name(), pain012.getDetalhesRecorrencias().getLast().getTipoSituacaoDaRecorrencia()),
                    () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getDataHoraInicioCanal(), pain012.getDetalhesRecorrencias().getLast().getDataHoraTipoSituacaoDaRecorrencia())
            );
        }
    }

    @Nested
    class ProcessamentoRejeitada {

        private static Stream<Arguments> provideMotivoRejeicao() {
            return Stream.of(
                    Arguments.of(MotivoRejeicaoPain012.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR),
                    Arguments.of(MotivoRejeicaoPain012.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_SEM_INTERESSE_PIX_AUTOMATICO_USUARIO_RECEBEDOR)
            );
        }

        @MethodSource("provideMotivoRejeicao")
        @ParameterizedTest
        void dadaUmaConfirmacaoAutorizacaoRejeitadaValida_quandoProcessar_deveCriarResponseIdempotentSucesso(MotivoRejeicaoPain012 motivoRejeicao) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestComRejeicao(motivoRejeicao);
            SolicitacaoAutorizacaoRecorrencia solicitacao = buildSolicitacao();
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any())).thenReturn(solicitacao);
            doNothing().when(solicitacaoAutorizacaoService).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            when(autorizacaoService.salvar(any())).thenReturn(new RecorrenciaAutorizacao());
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(ConfirmacaoAutorizacaoRequestDTO.class), any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarConfirmacao(request);

            verify(validator, times(1)).validate(any());
            verify(solicitacaoAutorizacaoService, times(1)).buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any());
            verify(solicitacaoAutorizacaoService, times(1)).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            verify(autorizacaoService, times(1)).salvar(any());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_AUTORIZACAO));
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(ConfirmacaoAutorizacaoRequestDTO.class), any(), any(), any());

            var pain012 = captorPain012.getValue();

            assertAll(
                    () -> assertFalse(pain012.getStatus()),
                    () -> assertEquals(motivoRejeicao.name(), pain012.getMotivoRejeicao()),
                    () -> assertEquals(solicitacao.getIdRecorrencia(), pain012.getIdRecorrencia()),
                    () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getIdInformacaoStatus(), pain012.getIdInformacaoStatus()),
                    () -> assertEquals(TipoRecorrencia.RECORRENTE.name(), pain012.getTipoRecorrencia()),
                    () -> assertEquals(solicitacao.getTipoFrequencia(), pain012.getTipoFrequencia()),
                    () -> assertEquals(solicitacao.getDataFinalRecorrencia(), pain012.getDataFinalRecorrencia()),
                    () -> assertFalse(pain012.getIndicadorObrigatorioOriginal()),
                    () -> assertEquals(solicitacao.getNomeRecebedor(), pain012.getNomeUsuarioRecebedor()),
                    () -> assertEquals(solicitacao.getCpfCnpjRecebedor(), pain012.getCpfCnpjUsuarioRecebedor()),
                    () -> assertEquals(solicitacao.getInstituicaoRecebedor(), pain012.getParticipanteDoUsuarioRecebedor()),
                    () -> assertEquals(solicitacao.getCodigoMunicipioIBGE(), pain012.getCodMunIBGE()),
                    () -> assertEquals(confirmacaoAutorizacaoRequestDTO.getCpfCnpjPagador(), pain012.getCpfCnpjUsuarioPagador()),
                    () -> assertEquals(solicitacao.getContaPagador(), pain012.getContaUsuarioPagador()),
                    () -> assertEquals(solicitacao.getAgenciaPagador(), pain012.getAgenciaUsuarioPagador()),
                    () -> assertEquals(solicitacao.getInstituicaoPagador(), pain012.getParticipanteDoUsuarioPagador()),
                    () -> assertEquals(solicitacao.getNomeDevedor(), pain012.getNomeDevedor()),
                    () -> assertEquals(solicitacao.getCpfCnpjDevedor(), pain012.getCpfCnpjDevedor()),
                    () -> assertEquals(solicitacao.getNumeroContrato(), pain012.getNumeroContrato()),
                    () -> assertEquals(solicitacao.getDescricao(), pain012.getDescricao()),
                    () -> assertNull(pain012.getDetalhesRecorrencias())
            );
        }

        @Test
        void dadaUmaConfirmacaoAutorizacaoRejeitadaInvalida_quandoProcessar_deveCriarResponseIdempotentRegraDeNegocio() {
            var confirmacaoAutorizacaoRequestDTO = buildRequestComRejeicao(MotivoRejeicaoPain012.CONTA_TRANSACIONAL_USUARIO_PAGADOR_ENCERRADA);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any()))
                    .thenReturn(buildSolicitacao());
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);

            service.processarConfirmacao(request);

            verify(validator, times(1)).validate(any());
            verify(solicitacaoAutorizacaoService, times(1)).buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any());
            verify(solicitacaoAutorizacaoService, never()).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            verify(autorizacaoService, never()).salvar(any());
            verify(eventoResponseFactory, never()).criarEventoPain012(any(), any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), any());
        }
    }

    @SuppressWarnings("rawtypes")
    @Nested
    class ValidacaoConstraint {

        public static final String TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS = "Teste de erro de validação de constraints";

        private static Stream<Arguments> provideNullAndStrings() {
            return Stream.of(
                    Arguments.of((Object) null),
                    Arguments.of(""),
                    Arguments.of(" ")
            );
        }

        @MethodSource("provideNullAndStrings")
        @ParameterizedTest
        void dadaUmaConfirmacaoComIdSolicitacaoRecorrenciaInvalido_quandoProcessar_deveCriarResponseIdempotentErroValidacaoConstraint(String idSolicitacaoRecorrencia) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setIdSolicitacaoRecorrencia(idSolicitacaoRecorrencia);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @MethodSource("provideNullAndStrings")
        @ParameterizedTest
        void dadaUmaConfirmacaoComIdRecorrenciaInvalido_quandoProcessar_deveCriarResponseIdempotentErroValidacaoConstraint(String idRecorrencia) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setIdRecorrencia(idRecorrencia);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @MethodSource("provideNullAndStrings")
        @ParameterizedTest
        void dadaUmaConfirmacaoComIdInformacaoStatusInvalido_quandoProcessar_deveCriarResponseIdempotentErroValidacaoConstraint(String idInformacaoStatus) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setIdInformacaoStatus(idInformacaoStatus);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @ValueSource(doubles = {12345678901234567.1, 12.1})
        @ParameterizedTest
        void dadaUmaConfirmacaoComValorMaximoInvalido_quandoProcessar_deveCriarResponseIdempotentErroValidacaoConstraint(double valorMaximo) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setValorMaximo(BigDecimal.valueOf(valorMaximo));
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @NullSource
        @ParameterizedTest
        void dadaUmaConfirmacaoComAprovadaNulo_quandoProcessar_deveCriarResponseIdempotentErroValidacaoConstraint(Boolean aprovada) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setAprovada(aprovada);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @NullSource
        @ParameterizedTest
        void dadaUmaConfirmacaoComCpfCnpjPagadorInvalido_quandoProcessar_deveCriarResponseIdempotentErroValidacaoConstraint(String cpfCnpjPagador) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setCpfCnpjPagador(cpfCnpjPagador);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        @NullSource
        @ParameterizedTest
        void dadaUmaConfirmacaoComAprovadaNulo_quandoProcessar_deveCriarResponseIdempotentErroValidacaoConstraint(TipoCanalEnum tipoCanal) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setTipoCanalPagador(tipoCanal);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro, idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroValidacaoConstraint(mensagemErro);
        }

        private void criaStubsErroValidacaoConstraint(ConstraintViolationImpl mockConstraintViolation, String mensagemErro, IdempotentResponse idempotentResponse) {
            when(validator.validate(any())).thenReturn(Set.of(mockConstraintViolation));
            when(mockConstraintViolation.getMessage()).thenReturn(mensagemErro);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);
        }

        private void validarAssercoesErroValidacaoConstraint(String mensagemErro) {
            verify(validator, times(1)).validate(any());
            verify(solicitacaoAutorizacaoService, never()).buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any());
            verify(solicitacaoAutorizacaoService, never()).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            verify(autorizacaoService, never()).salvar(any());
            verify(eventoResponseFactory, never()).criarEventoPain012(any(), any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErro.capture());

            var erroDTO = captorErro.getValue();
            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001, erroDTO.codigoErro()),
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemErro), erroDTO.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_VALIDACAO, erroDTO.tipoRetornoTransacaoEnum())
            );
        }
    }

    @Nested
    class ValidacaoRegraNegocio {

        @Test
        void dadaUmaConfirmacaoNaoEncontradaNoBancoDeDados_quandoProcessar_deveCriarResponseIdempotentErroDeNegocio() {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any())).thenThrow(new NotFoundException());
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroRegraDeNegocio(never(), never(), AppExceptionCode.SOLICITACAO_NAO_ENCONTRADA, AppExceptionCode.SOLICITACAO_NAO_ENCONTRADA.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
        }

        @Test
        void dadaUmaConfirmacaoComValorMaximoInvalido_quandoProcessar_deveCriarResponseIdempotentErroDeNegocio() {
            var requestDTO = buildRequestAprovada();
            requestDTO.setValorMaximo(BigDecimal.ONE);
            SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacaoRecorrencia = buildSolicitacao();
            solicitacaoAutorizacaoRecorrencia.setPisoValorMaximo(BigDecimal.TEN);
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(requestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any()))
                    .thenReturn(solicitacaoAutorizacaoRecorrencia);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroRegraDeNegocio(never(), never(), AppExceptionCode.VALOR_MAXIMO_INVALIDO, AppExceptionCode.VALOR_MAXIMO_INVALIDO.getMensagemFormatada(BigDecimal.TEN.toString()), TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
        }

        @Test
        void dadaUmaConfirmacaoComNumCpfCnpjInvalido_quandoProcessar_deveCriarResponseIdempotentErroDeNegocio() {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.setCpfCnpjPagador("cpfInvalido");
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any()))
                    .thenReturn(buildSolicitacao());
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroRegraDeNegocio(never(), never(), AppExceptionCode.DADOS_PAGADOR_INVALIDO, AppExceptionCode.DADOS_PAGADOR_INVALIDO.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
        }

        @Test
        void dadaUmaConfirmacaoComAgenciaInvalida_quandoProcessar_deveCriarResponseIdempotentErroDeNegocio() {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.getProtocoloDTO().setCooperativa("cooperativaInvalida");
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any()))
                    .thenReturn(buildSolicitacao());
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroRegraDeNegocio(never(), never(), AppExceptionCode.DADOS_PAGADOR_INVALIDO, AppExceptionCode.DADOS_PAGADOR_INVALIDO.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
        }

        @Test
        void dadaUmaConfirmacaoComContaPagadorInvalida_quandoProcessar_deveCriarResponseIdempotentErroDeNegocio() {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            confirmacaoAutorizacaoRequestDTO.getProtocoloDTO().setConta("contaInvalida");
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any()))
                    .thenReturn(buildSolicitacao());
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);

            service.processarConfirmacao(request);

            validarAssercoesErroRegraDeNegocio(never(), never(), AppExceptionCode.DADOS_PAGADOR_INVALIDO, AppExceptionCode.DADOS_PAGADOR_INVALIDO.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
        }

        private static Stream<Arguments> provideExceptions() {
            return Stream.of(
                    Arguments.of(new DataIntegrityViolationException("dataIntegrityViolationExceptionMessage")),
                    Arguments.of(new ConstraintViolationException("dataIntegrityViolationExceptionMessage", new SQLException(), "constraintName")));
        }

        @MethodSource("provideExceptions")
        @ParameterizedTest
        void dadaUmaConfirmacaoComErroNaPersistencia_quandoProcessar_deveCriarResponseIdempotentErroDeNegocio(Exception exception) {
            var confirmacaoAutorizacaoRequestDTO = buildRequestAprovada();
            var request = IdempotentAsyncRequest.<ConfirmacaoAutorizacaoRequestDTO>builder().value(confirmacaoAutorizacaoRequestDTO).build();
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(solicitacaoAutorizacaoService.buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any()))
                    .thenReturn(buildSolicitacao());
            doNothing().when(solicitacaoAutorizacaoService).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            when(autorizacaoService.salvar(any())).thenThrow(exception);

            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);

            service.processarConfirmacao(request);


            validarAssercoesErroRegraDeNegocio(times(1), times(1), AppExceptionCode.ERRO_PERSISTENCIA, AppExceptionCode.ERRO_PERSISTENCIA.getMensagemFormatada(exception.getMessage()), TipoRetornoTransacaoEnum.ERRO_INFRA);
        }

        private void validarAssercoesErroRegraDeNegocio(
                VerificationMode verificaAtualizar, VerificationMode verificaSalvar, AppExceptionCode codErro, String descricaoErro, TipoRetornoTransacaoEnum tipoRetorno) {
            verify(validator, times(1)).validate(any());
            verify(solicitacaoAutorizacaoService, times(1)).buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(any(), any());
            verify(solicitacaoAutorizacaoService, verificaAtualizar).atualizaRecorrenciaAutorizacaoSolicitacao(any(), any(), any(), any(), any());
            verify(autorizacaoService, verificaSalvar).salvar(any());
            verify(eventoResponseFactory, never()).criarEventoPain012(any(), any());
            verify(criaResponseStrategyFactory, times(1)).criar(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErro.capture());

            var erroDTO = captorErro.getValue();
            assertAll(
                    () -> assertEquals(codErro, erroDTO.codigoErro()),
                    () -> assertEquals(descricaoErro, erroDTO.mensagemErro()),
                    () -> assertEquals(tipoRetorno, erroDTO.tipoRetornoTransacaoEnum())
            );
        }
    }

    private static ConfirmacaoAutorizacaoRequestDTO buildRequestAprovada() {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setCooperativa(COOPERATIVA);
        protocoloDTO.setConta(CONTA);

        return ConfirmacaoAutorizacaoRequestDTO.builder()
                .idSolicitacaoRecorrencia("idSolicitacaoRecorrencia")
                .idRecorrencia(ID_RECORRENCIA)
                .idInformacaoStatus("idInformacaoStatus")
                .valorMaximo(BigDecimal.valueOf(100))
                .aprovada(true)
                .motivoRejeicao(null)
                .identificadorTransacao("identificadorTransacao")
                .dataHoraInicioCanal(criarDataHoraInicioCanal("2025-05-05 16:02:30.160-0300"))
                .dataHoraRecepcao(LocalDateTime.now().atZone(ZoneId.of("America/Sao_Paulo")))
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .protocoloDTO(protocoloDTO)
                .cpfCnpjPagador(CPF_CNPJ_PAGADOR)
                .tipoCanalPagador(TipoCanalEnum.MOBI)
                .build();
    }

    private static ConfirmacaoAutorizacaoRequestDTO buildRequestComRejeicao(MotivoRejeicaoPain012 motivoRejeicaoPain012) {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setCooperativa(COOPERATIVA);
        protocoloDTO.setConta(CONTA);

        return ConfirmacaoAutorizacaoRequestDTO.builder()
                .idSolicitacaoRecorrencia("idSolicitacaoRecorrencia")
                .idRecorrencia(ID_RECORRENCIA)
                .idInformacaoStatus("idInformacaoStatus")
                .valorMaximo(BigDecimal.valueOf(100))
                .aprovada(false)
                .motivoRejeicao(motivoRejeicaoPain012)
                .identificadorTransacao("identificadorTransacao")
                .dataHoraInicioCanal(criarDataHoraInicioCanal("2025-05-05 16:02:30.160-0300"))
                .dataHoraRecepcao(LocalDateTime.now().atZone(ZoneId.of("America/Sao_Paulo")))
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .protocoloDTO(protocoloDTO)
                .cpfCnpjPagador(CPF_CNPJ_PAGADOR)
                .tipoCanalPagador(TipoCanalEnum.MOBI)
                .build();
    }

    private static SolicitacaoAutorizacaoRecorrencia buildSolicitacao() {
        return SolicitacaoAutorizacaoRecorrencia.builder()
                .pisoValorMaximo(BigDecimal.valueOf(100))
                .cpfCnpjPagador(CPF_CNPJ_PAGADOR)
                .agenciaPagador(COOPERATIVA)
                .contaPagador(CONTA)
                .idRecorrencia(ID_RECORRENCIA)
                .tipoFrequencia("tipoFrequencia")
                .tipoPessoaPagador(TipoPessoaEnum.PF)
                .tipoSistemaPagador(OrigemEnum.LEGADO)
                .dataInicialRecorrencia(LocalDate.now())
                .dataFinalRecorrencia(LocalDate.now())
                .dataCriacaoRecorrencia(LocalDateTime.now())
                .build();
    }
}