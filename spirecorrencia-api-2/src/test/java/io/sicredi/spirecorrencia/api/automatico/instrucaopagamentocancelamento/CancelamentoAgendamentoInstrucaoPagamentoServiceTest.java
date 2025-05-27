package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.messasing.MessageProducer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CancelamentoAgendamentoInstrucaoPagamentoServiceTest {

    private static final String TOPICO = "topico";
    private static final String ID_FIM_A_FIM = "E8785320620250506210800n2f1PQijN";
    private static final String CPF_SOLICITANTE = "12345678901";

    @Mock
    private CriaResponseStrategyFactory<CancelamentoAgendamentoDebitoRequest> criaResponseStrategyFactory;
    @Mock
    private MessageProducer messageProducer;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;
    @Mock
    private IdempotentRequest<CancelamentoAgendamentoDebitoRequest> idempotentRequest;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Mock
    private CriaResponseStrategy<CancelamentoAgendamentoDebitoRequest> criaResponseStrategy;
    @Mock
    private ProcessaCancelamentoAgendamentoService processaCancelamento;
    @InjectMocks
    private CancelamentoAgendamentoInstrucaoPagamentoServiceImpl service;

    @Nested
    class ProcessarCancelamentoDebito {

        @Test
        void dadoPedidoCancelamentoValido_quandoProcessarCancelamentoDebito_deveEnviarEventoCamtEProtocolo() {
            var request = criarCancelamentoRequest();
            var camt55 = criarCamt55();

            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            when(idempotentRequest.getValue()).thenReturn(request);
            when(processaCancelamento.processarCancelamento(request)).thenReturn(new ErroWrapperDTO<>(camt55));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CancelamentoAgendamentoDebitoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoCamt055(any())).thenReturn(new EventoResponseDTO(Camt055Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarCancelamentoDebito(idempotentRequest);

            verify(eventoResponseFactory).criarEventoCamt055(camt55);

            verify(criaResponseStrategy).criarResponseIdempotentSucesso(
                    eq(request),
                    any(),
                    any(),
                    argThat(argument -> argument.size() == 1 &&
                                    argument.getFirst().topic().equals(TOPICO))
            );

            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
        }

        @Test
        void dadoPedidoCancelamentoComErro_quandoProcessarCancelamentoDebito_deveCriarErroNegocioENaoCriarEventoCamt() {
            var request = criarCancelamentoRequest();

            var erro = new ErroDTO(
                    INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA,
                    INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA.getMessage(),
                    TipoRetornoTransacaoEnum.ERRO_NEGOCIO
            );

            ErroWrapperDTO<Camt055Dto> wrapperComErro = new ErroWrapperDTO<>(erro);
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(true).build();

            when(idempotentRequest.getValue()).thenReturn(request);
            when(processaCancelamento.processarCancelamento(request)).thenReturn(wrapperComErro);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            when(criaResponseStrategy.criarResponseIdempotentErro(eq(request), any(), eq(erro))).thenReturn(idempotentResponse);

            service.processarCancelamentoDebito(idempotentRequest);

            verify(criaResponseStrategy).criarResponseIdempotentErro(eq(request), any(), eq(erro));
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(CancelamentoAgendamentoDebitoRequest.class), any(), any(), any());
            verify(eventoResponseFactory, never()).criarEventoCamt055(any());
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

    @Nested
    class ProcessarCancelamentoDebitoComInstrucao {

        @Test
        void dadoPedidoCancelamentoValido_quandoProcessarCancelamentoDebitoComInstrucao_deveRetornarEventoCamt55() {
            var instrucao = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);
            var wrapper = CancelamentoAgendamentoWrapperDTO.fromCancelamentoInterno(instrucao, CPF_SOLICITANTE, MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR);

            var camt55 = criarCamt55();
            var eventoEsperado = new EventoResponseDTO(camt55, new HashMap<>(), TOPICO);

            when(processaCancelamento.processarCancelamentoComInstrucao(wrapper)).thenReturn(new ErroWrapperDTO<>(camt55));
            when(eventoResponseFactory.criarEventoCamt055(camt55)).thenReturn(eventoEsperado);

            var resultado = service.processarCancelamentoDebito(wrapper);

            assertTrue(resultado.getErro().isEmpty());
            assertEquals(eventoEsperado, resultado.getObjeto());

            verify(eventoResponseFactory).criarEventoCamt055(camt55);
        }

        @Test
        void dadoPedidoCancelamentoComErro_quandoProcessarCancelamentoDebitoComInstrucao_deveCriarErroNegocioENaoCriarEventoCamt() {
            var instrucao = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);
            var wrapper = CancelamentoAgendamentoWrapperDTO.fromCancelamentoInterno(instrucao, CPF_SOLICITANTE, MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR);

            var erro = new ErroDTO(INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA,
                    INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA.getMessage(),
                    TipoRetornoTransacaoEnum.ERRO_NEGOCIO
            );

            when(processaCancelamento.processarCancelamentoComInstrucao(wrapper)).thenReturn(new ErroWrapperDTO<>(erro));

            var resultado = service.processarCancelamentoDebito(wrapper);

            assertTrue(resultado.getErro().isPresent());
            assertEquals(erro, resultado.getErro().get());
            assertNull(resultado.getObjeto());

            verify(eventoResponseFactory, never()).criarEventoCamt055(any());
        }
    }

    @Nested
    class ProcessarCancelamentoDebitoSemIdempotencia {

        @Test
        void dadoInstrucaoValida_quandoProcessarCancelamentoDebitoSemIdempotencia_deveEnviarCamt55() {
            var instrucao = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);
            var wrapper = CancelamentoAgendamentoWrapperDTO.fromCancelamentoInterno(instrucao, CPF_SOLICITANTE, MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR);

            var camt55 = criarCamt55();
            var eventoEsperado = new EventoResponseDTO(camt55, new HashMap<>(), TOPICO);

            when(processaCancelamento.processarCancelamentoComInstrucao(wrapper)).thenReturn(new ErroWrapperDTO<>(camt55));
            when(eventoResponseFactory.criarEventoCamt055(camt55)).thenReturn(eventoEsperado);

            service.processarCancelamentoDebitoSemIdempotencia(wrapper);

            verify(messageProducer).enviar(any(), any(), any());
        }

        @Test
        void dadoErroNoProcessamento_quandoProcessarCancelamentoDebitoSemIdempotencia_naoDeveEnviarCamt55() {
            var instrucao = criarInstrucaoPagamento(TipoStatusInstrucaoPagamento.ATIVA);
            var wrapper = CancelamentoAgendamentoWrapperDTO.fromCancelamentoInterno(instrucao, CPF_SOLICITANTE, MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR);

            var erro = new ErroDTO(
                    INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA,
                    INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA.getMessage(),
                    TipoRetornoTransacaoEnum.ERRO_NEGOCIO
            );

            when(processaCancelamento.processarCancelamentoComInstrucao(wrapper)).thenReturn(new ErroWrapperDTO<>(erro));

            service.processarCancelamentoDebitoSemIdempotencia(wrapper);

            verify(messageProducer, never()).enviar(any(), any(), any());
        }
    }


    private Camt055Dto criarCamt55() {
        return Camt055Dto.builder()
                .idCancelamentoAgendamento("CANCEL123456789")
                .idConciliacaoRecebedorOriginal("CR8785320620250506210800n2f1PQij")
                .cpfCnpjUsuarioSolicitanteCancelamento(CPF_SOLICITANTE)
                .motivoCancelamento(MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR.name())
                .idFimAFimOriginal(ID_FIM_A_FIM)
                .tipoSolicitacaoOuInformacao(MotivoCancelamentoCamt55.SOLICITADO_PELO_PAGADOR.name())
                .dataHoraSolicitacaoOuInformacao(LocalDateTime.now())
                .dataHoraCriacaoParaEmissao(LocalDateTime.now())
                .participanteSolicitanteDoCancelamento("01181521")
                .participanteDestinatarioDoCancelamento("23423123")
                .build();
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
}