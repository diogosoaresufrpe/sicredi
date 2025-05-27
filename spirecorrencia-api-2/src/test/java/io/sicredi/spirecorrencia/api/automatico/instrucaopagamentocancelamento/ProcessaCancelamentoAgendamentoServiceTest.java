package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;
import br.com.sicredi.spi.entities.type.TipoSolicitacaoCamt55;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoService;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import jakarta.validation.Validator;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.*;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.HORARIO_CANCELAMENTO_FORA_DO_PERMITIDO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessaCancelamentoAgendamentoServiceTest {

    private static final String CPF_SOLICITANTE = "12345678901";
    private static final String ID_FIM_A_FIM = "E8785320620250506210800n2f1PQijN";

    @Mock
    private Validator validator;
    @Mock
    private RecorrenciaInstrucaoPagamentoCancelamentoRepository instrucaoPagamentoCancelamentoRepository;
    @Mock
    private RecorrenciaInstrucaoPagamentoService instrucaoPagamentoService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;
    @InjectMocks
    private ProcessaCancelamentoAgendamentoService service;
    @Captor
    private ArgumentCaptor<RecorrenciaInstrucaoPagamento> instrucaoPagamentoCaptor;
    @Captor
    private ArgumentCaptor<RecorrenciaInstrucaoPagamentoCancelamento> instrucaoPagamentoCancelamentoCaptor;

    @Nested
    class ProcessarCancelamentoDebito {

        @BeforeEach
        void beforeEach(){
            when(appConfig.getRegras().getCancelamentoAgendamento().getHorarioLimiteCancelamento()).thenReturn(LocalTime.of(23,59,59));
            when(appConfig.getRegras().getCancelamentoAgendamento().getDiasMinimosAntecedencia()).thenReturn(1);
        }

        @Test
        void dadoPedidoCancelamentoSolicitadoPagador_quandoProcessarCancelamento_deveRetornarCamt055() {
            var request = criarCancelamentoRequest();
            var instrucaoPagamento = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);
            var instrucaoPagamentoCancelamento = criarInstrucaoPagamentoCancelamento(MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR);

            when(instrucaoPagamentoService.salvarInstrucaoPagamento(any())).thenReturn(instrucaoPagamento);
            when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));
            when(instrucaoPagamentoCancelamentoRepository.save(any())).thenReturn(instrucaoPagamentoCancelamento);

            var wrapperDTO = service.processarCancelamento(request);

            verify(instrucaoPagamentoCancelamentoRepository).save(instrucaoPagamentoCancelamentoCaptor.capture());
            verify(instrucaoPagamentoService).salvarInstrucaoPagamento(instrucaoPagamentoCaptor.capture());

            var instrucaoPagamentoValue = instrucaoPagamentoCaptor.getValue();
            var instrucaoPagamentoCancelmentoValue = instrucaoPagamentoCancelamentoCaptor.getValue();

            var camt055 = wrapperDTO.getObjeto();

            validarDadosSucesso(instrucaoPagamentoValue, request, instrucaoPagamentoCancelmentoValue, camt055, instrucaoPagamento);
        }


        @Test
        void dadoPedidoCancelamentoSolicitadoSicredi_quandoProcessarCancelamento_deveRetornarCamt055() {
            var request = criarCancelamentoRequest();
            var instrucaoPagamento = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);
            var instrucaoPagamentoCancelamento = criarInstrucaoPagamentoCancelamento(MotivoCancelamentoCamt55.FALHA_LIQUIDACAO);

            request.setCpfCnpjSolicitanteCancelamento("01181521000155");
            request.setMotivoCancelamento(MotivoCancelamentoCamt55.FALHA_LIQUIDACAO);

            when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));
            when(instrucaoPagamentoService.salvarInstrucaoPagamento(any())).thenReturn(instrucaoPagamento);
            when(instrucaoPagamentoCancelamentoRepository.save(any())).thenReturn(instrucaoPagamentoCancelamento);

            var wrapperDTO = service.processarCancelamento(request);

            verify(instrucaoPagamentoService).salvarInstrucaoPagamento(instrucaoPagamentoCaptor.capture());
            verify(instrucaoPagamentoCancelamentoRepository).save(instrucaoPagamentoCancelamentoCaptor.capture());

            var instrucaoPagamentoValue = instrucaoPagamentoCaptor.getValue();
            var instrucaoPagamentoCancelmentoValue = instrucaoPagamentoCancelamentoCaptor.getValue();

            var camt055 = wrapperDTO.getObjeto();

            validarDadosSucesso(instrucaoPagamentoValue, request, instrucaoPagamentoCancelmentoValue, camt055, instrucaoPagamento);
        }

        private static void validarDadosSucesso(RecorrenciaInstrucaoPagamento instrucaoPagamentoValue, CancelamentoAgendamentoDebitoRequest request, RecorrenciaInstrucaoPagamentoCancelamento instrucaoPagamentoCancelmentoValue, Camt055Dto camt055, RecorrenciaInstrucaoPagamento instrucaoPagamento) {
            assertAll(
                    () -> assertEquals("AGUARDANDO_CANCELAMENTO", instrucaoPagamentoValue.getTpoSubStatus()),

                    () -> assertEquals(request.getIdCancelamentoAgendamento(), instrucaoPagamentoCancelmentoValue.getIdCancelamentoAgendamento()),
                    () -> assertEquals(request.getCpfCnpjSolicitanteCancelamento(), instrucaoPagamentoCancelmentoValue.getNumCpfCnpjSolicitanteCancelamento()),
                    () -> assertEquals(request.getMotivoCancelamento().name(), instrucaoPagamentoCancelmentoValue.getCodMotivoCancelamento()),
                    () -> assertEquals(request.getIdFimAFim(), instrucaoPagamentoCancelmentoValue.getCodFimAFim()),

                    () -> assertEquals(request.getIdCancelamentoAgendamento(), camt055.getIdCancelamentoAgendamento()),
                    () -> assertEquals(request.getMotivoCancelamento().name(), camt055.getMotivoCancelamento()),
                    () -> assertEquals(request.getIdFimAFim(), camt055.getIdFimAFimOriginal()),
                    () -> assertEquals(instrucaoPagamento.getIdConciliacaoRecebedor(), camt055.getIdConciliacaoRecebedorOriginal()),
                    () -> assertEquals(TipoSolicitacaoCamt55.SOLICITADO_PELO_PAGADOR.name(), camt055.getTipoSolicitacaoOuInformacao()),
                    () -> assertEquals(instrucaoPagamento.getNumInstituicaoRecebedor(), camt055.getParticipanteDestinatarioDoCancelamento()),
                    () -> assertEquals(instrucaoPagamento.getNumInstituicaoPagador(), camt055.getParticipanteSolicitanteDoCancelamento())
            );
        }

        private RecorrenciaInstrucaoPagamentoCancelamento criarInstrucaoPagamentoCancelamento(MotivoCancelamentoCamt55 motivoCancelamentoCamt55) {
            return RecorrenciaInstrucaoPagamentoCancelamento.builder()
                    .codFimAFim(ID_FIM_A_FIM)
                    .idCancelamentoAgendamento("CANCEL123456789")
                    .numCpfCnpjSolicitanteCancelamento(CPF_SOLICITANTE)
                    .codMotivoCancelamento(motivoCancelamentoCamt55.name())
                    .build();
        }
    }

    @Nested
    class ProcessarCancelamentoDebitoErroNegocio {
        @ParameterizedTest
        @DisplayName("Dado instrução de pamento com status diferente de ATIVA, quando processar cancelamento de débito, deve criar erro de negocio e não enviar evento")
        @EnumSource(
                value = TipoStatusInstrucaoPagamento.class,
                mode = EnumSource.Mode.EXCLUDE,
                names = {
                        "ATIVA"
                })
        void dadoStatusDiferenteAtiva_quandoProcessarCancelamentoDebito_deveLancarErroNegocio(TipoStatusInstrucaoPagamento status) {
            var request = criarCancelamentoRequest();
            var instrucaoPagamento = criarInstrucaoPagamento(status);

            when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var objeto = wrapperDTO.getObjeto();
            var erro = wrapperDTO.getErro().get();

            verify(instrucaoPagamentoService, never()).salvarInstrucaoPagamento(any());
            verify(instrucaoPagamentoCancelamentoRepository, never()).save(any());

            assertAll(
                    () -> assertNull(objeto),
                    () -> assertEquals(INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA, erro.codigoErro()),
                    () -> assertEquals(INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA.getMessage(), erro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, erro.tipoRetornoTransacaoEnum())
            );
        }

        @Test
        void dadoCpfDiferente_quandoProcessarCancelamentoDebito_deveLancarErroNegocio() {
            var request = criarCancelamentoRequest();
            var instrucaoPagamento = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);

            request.setCpfCnpjSolicitanteCancelamento("11111111111");

            when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var objeto = wrapperDTO.getObjeto();
            var erro = wrapperDTO.getErro().get();

            verify(instrucaoPagamentoService, never()).salvarInstrucaoPagamento(any());
            verify(instrucaoPagamentoCancelamentoRepository, never()).save(any());

            assertAll(
                    () -> assertNull(objeto),
                    () -> assertEquals(DADOS_SOLICITANTE_DIFERENTE_DA_INSTRUCAO_PAGAMENTO, erro.codigoErro()),
                    () -> assertEquals(DADOS_SOLICITANTE_DIFERENTE_DA_INSTRUCAO_PAGAMENTO.getMessage(), erro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, erro.tipoRetornoTransacaoEnum())
            );
        }

        @Test
        void dadoAgendamentoForaDoHorarioPermitido_quandoProcessarCancelamentoDebito_deveLancarErroNegocio() {
            var request = criarCancelamentoRequest();
            var instrucaoPagamento = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);

            instrucaoPagamento.setDatVencimento(LocalDate.now());

            when(appConfig.getRegras().getCancelamentoAgendamento().getHorarioLimiteCancelamento()).thenReturn(LocalTime.of(23,59,59));
            when(appConfig.getRegras().getCancelamentoAgendamento().getDiasMinimosAntecedencia()).thenReturn(1);

            when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var objeto = wrapperDTO.getObjeto();
            var erro = wrapperDTO.getErro().get();

            verify(instrucaoPagamentoService, never()).salvarInstrucaoPagamento(any());
            verify(instrucaoPagamentoCancelamentoRepository, never()).save(any());

            assertAll(
                    () -> assertNull(objeto),
                    () -> assertEquals(HORARIO_CANCELAMENTO_FORA_DO_PERMITIDO, erro.codigoErro()),
                    () -> assertEquals(HORARIO_CANCELAMENTO_FORA_DO_PERMITIDO.getMessage(), erro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, erro.tipoRetornoTransacaoEnum())
            );
        }

        @Test
        void dadoInstrucaoNaoEncontrada_quandoProcessarCancelamentoDebito_deveLancarErroNegocio() {
            var request = criarCancelamentoRequest();
            var instrucaoPagamento = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);

            instrucaoPagamento.setDatVencimento(LocalDate.now());

            when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.empty());

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var objeto = wrapperDTO.getObjeto();
            var erro = wrapperDTO.getErro().get();

            verify(instrucaoPagamentoService, never()).salvarInstrucaoPagamento(any());
            verify(instrucaoPagamentoCancelamentoRepository, never()).save(any());

            assertAll(
                    () -> assertNull(objeto),
                    () -> assertEquals(INSTRUCAO_PAGAMENTO_NAO_ENCONTRADA, erro.codigoErro()),
                    () -> assertEquals(INSTRUCAO_PAGAMENTO_NAO_ENCONTRADA.getMessage(), erro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, erro.tipoRetornoTransacaoEnum())
            );
        }

        @Test
        void dadoExececaoAoSalvarDados_quandoProcessarCancelamentoDebito_deveLancarErroInfra() {
            var request = criarCancelamentoRequest();
            var instrucaoPagamento = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);

            when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));
            when(instrucaoPagamentoCancelamentoRepository.save(any())).thenThrow(new DataIntegrityViolationException("Erro ao salvar"));

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var objeto = wrapperDTO.getObjeto();
            var erro = wrapperDTO.getErro().get();

            verify(instrucaoPagamentoService, times(1)).salvarInstrucaoPagamento(any());

            assertAll(
                    () -> assertNull(objeto),
                    () -> assertEquals(ERRO_PERSISTENCIA, erro.codigoErro()),
                    () -> assertEquals(ERRO_PERSISTENCIA.getMensagemFormatada("Erro ao salvar"), erro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_INFRA, erro.tipoRetornoTransacaoEnum())
            );
        }
    }

    @Nested
    class ProcessarCancelamentoDebitoErroValidacao {

        public static final String TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS = "Teste de erro de validação de constraints";

        private static Stream<Arguments> provideNullAndStrings() {
            return Stream.of(
                    Arguments.of((Object) null),
                    Arguments.of(""),
                    Arguments.of(" ")
            );
        }

        @ParameterizedTest
        @MethodSource("provideNullAndStrings")
        void dadoIdCancelamentoAgendamentoInvalido_quandoProcessarCancelamentoDebito_deveCriarResponseIdempotentErro(String idCancelamentoAgendamento) {
            var request = criarCancelamentoRequest();
            request.setIdCancelamentoAgendamento(idCancelamentoAgendamento);

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro);

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var erro = wrapperDTO.getErro().get();

            validarAssercoesErroValidacaoConstraint(mensagemErro, erro);
        }

        @ParameterizedTest
        @MethodSource("provideNullAndStrings")
        void dadoidFimAFim_quandoProcessarCancelamentoDebito_deveCriarResponseIdempotentErro(String idFimAFim) {
            var request = criarCancelamentoRequest();

            request.setIdFimAFim(idFimAFim);

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro);

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var erro = wrapperDTO.getErro().get();

            validarAssercoesErroValidacaoConstraint(mensagemErro, erro);
        }

        @ParameterizedTest
        @NullSource
        void dadoMotivoCancelamentoInvalido_quandoProcessarCancelamentoDebito_deveCriarResponseIdempotentErro(MotivoCancelamentoCamt55 motivoCancelamento) {
            var request = criarCancelamentoRequest();

            request.setMotivoCancelamento(motivoCancelamento);

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro);

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var erro = wrapperDTO.getErro().get();

            validarAssercoesErroValidacaoConstraint(mensagemErro, erro);
        }

        @ParameterizedTest
        @MethodSource("provideNullAndStrings")
        void dadoMotivoCancelamentoInvalido_quandoProcessarCancelamentoDebito_deveCriarResponseIdempotentErro(String cpfSolicitante) {
            var request = criarCancelamentoRequest();

            request.setCpfCnpjSolicitanteCancelamento(cpfSolicitante);

            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var mensagemErro = TESTE_DE_ERRO_DE_VALIDACAO_DE_CONSTRAINTS;

            criaStubsErroValidacaoConstraint(mockConstraintViolation, mensagemErro);

            var wrapperDTO = service.processarCancelamento(request);

            assertTrue(wrapperDTO.getErro().isPresent());

            var erro = wrapperDTO.getErro().get();

            validarAssercoesErroValidacaoConstraint(mensagemErro, erro);
        }

        private void validarAssercoesErroValidacaoConstraint(String mensagemErro, ErroDTO erroDTO) {
            verify(validator, times(1)).validate(any());
            verify(instrucaoPagamentoService, never()).buscarPorCodFimAFimComAutorizacao(any());
            verify(instrucaoPagamentoService, never()).salvarInstrucaoPagamento(any());
            verify(instrucaoPagamentoCancelamentoRepository, never()).save(any());

            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001, erroDTO.codigoErro()),
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemErro), erroDTO.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_VALIDACAO, erroDTO.tipoRetornoTransacaoEnum())
            );
        }

        private void criaStubsErroValidacaoConstraint(ConstraintViolationImpl mockConstraintViolation, String mensagemErro) {
            when(validator.validate(any())).thenReturn(Set.of(mockConstraintViolation));
            when(mockConstraintViolation.getMessage()).thenReturn(mensagemErro);
        }
    }

    private RecorrenciaInstrucaoPagamento criarInstrucaoPagamento(TipoStatusInstrucaoPagamento  status) {
        return RecorrenciaInstrucaoPagamento.builder()
                .txtNomeDevedor("João da Silva")
                .numCpfCnpjDevedor(CPF_SOLICITANTE)
                .numCpfCnpjPagador(CPF_SOLICITANTE)
                .tpoStatus(status.name())
                .codFimAFim(ID_FIM_A_FIM)
                .datVencimento(LocalDate.now().plusDays(5))
                .idConciliacaoRecebedor("CR8785320620250506210800n2f1PQij")
                .numInstituicaoPagador("01181521")
                .numInstituicaoRecebedor("23423123")
                .build();
    }

    private CancelamentoAgendamentoDebitoRequest criarCancelamentoRequest() {
        return CancelamentoAgendamentoDebitoRequest.builder()
                .oidRecorrenciaAutorizacao(123L)
                .idFimAFim(ID_FIM_A_FIM)
                .idCancelamentoAgendamento("CANCEL123456789")
                .cpfCnpjSolicitanteCancelamento(CPF_SOLICITANTE)
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .motivoCancelamento(MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR)
                .build();
    }
}