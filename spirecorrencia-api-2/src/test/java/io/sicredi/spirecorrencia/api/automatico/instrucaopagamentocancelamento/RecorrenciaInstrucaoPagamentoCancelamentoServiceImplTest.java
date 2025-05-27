package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.spi.dto.Camt029Dto;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoCamt029;
import br.com.sicredi.spi.entities.type.StatusCancelamentoCamt029;
import br.com.sicredi.spi.entities.type.TipoAceitacaoRejeicaoCamt029;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoService;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoService;
import io.sicredi.spirecorrencia.api.idempotente.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecorrenciaInstrucaoPagamentoCancelamentoServiceImplTest {

    private static final String TOPICO = "topico";

    @Mock
    private AutorizacaoService autorizacaoService;

    @Mock
    private EventoResponseFactory eventoResponseFactory;

    @Mock
    private RecorrenciaInstrucaoPagamentoService instrucaoPagamentoService;

    @Mock
    private RecorrenciaInstrucaoPagamentoCancelamentoRepository repository;

    @Mock
    private CriaResponseStrategyFactory<OperacaoRequest> criaResponseStrategyFactory;

    @Mock
    private CriaResponseStrategy<OperacaoRequest> criaResponseStrategy;

    @Captor
    private ArgumentCaptor<Camt029Dto> camt029Captor;

    @InjectMocks
    private RecorrenciaInstrucaoPagamentoCancelamentoServiceImpl service;

    @Test
    void dadaUmaSolicitacaoCancelamentoValida_quandoProcessar_deveEnviarCamt029DeSucesso() {
        var camt055Dto = buildCamt055Dto();
        var request = IdempotentAsyncRequest.<Camt055Dto>builder().value(camt055Dto).build();
        IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

        when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(buildRecorrenciaInstrucaoPagamento()));
        when(repository.save(any())).thenReturn(new RecorrenciaInstrucaoPagamentoCancelamento());
        doNothing().when(instrucaoPagamentoService).atualizaTpoStatusETpoSubStatus(any(), any(), any());
        when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
        when(criaResponseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
        when(eventoResponseFactory.criarEventoCamt029IcomEnvio(any())).thenReturn(new EventoResponseDTO(Camt029Dto.builder().build(), new HashMap<>(), TOPICO));

        service.processarSolicitacaoCancelamento(request);

        executarVerificacoes(times(1));

        var camt029 = camt029Captor.getValue();

        assertAll(
                () -> assertNotNull(camt029),
                () -> assertEquals(camt055Dto.getIdCancelamentoAgendamento(), camt029.getIdCancelamentoAgendamentoOriginal()),
                () -> assertEquals(camt055Dto.getIdConciliacaoRecebedorOriginal(), camt029.getIdConciliacaoRecebedorOriginal()),
                () -> assertEquals(camt055Dto.getIdFimAFimOriginal(), camt029.getIdFimAFimOriginal()),
                () -> assertEquals(camt055Dto.getParticipanteSolicitanteDoCancelamento(), camt029.getParticipanteAtualizaSolicitacaoDoCancelamento()),
                () -> assertEquals(camt055Dto.getParticipanteDestinatarioDoCancelamento(), camt029.getParticipanteRecebeAtualizacaoDoCancelamento()),
                () -> assertEquals(StatusCancelamentoCamt029.ACEITO.name(), camt029.getStatusDoCancelamento()),
                () -> assertEquals(TipoAceitacaoRejeicaoCamt029.DATA_HORA_ACEITACAO_CANCELAMENTO.name(), camt029.getTipoAceitacaoOuRejeicao()),
                () -> assertNotNull(camt029.getDataHoraAceitacaoOuRejeicaoDoCancelamento()),
                () -> assertNull(camt029.getCodigoDeRejeicaoDoCancelamento())
        );
    }

    @Test
    void dadaUmaSolicitacaoCancelamentoSemInstrucaoDePagamento_quandoProcessar_deveEnviarCamt029ComCodRejeicao() {
        var camt055Dto = buildCamt055Dto();
        var request = IdempotentAsyncRequest.<Camt055Dto>builder().value(camt055Dto).build();
        IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

        when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(new RecorrenciaInstrucaoPagamentoCancelamento());
        when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
        when(criaResponseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
        when(eventoResponseFactory.criarEventoCamt029IcomEnvio(any())).thenReturn(new EventoResponseDTO(Camt029Dto.builder().build(), new HashMap<>(), TOPICO));

        service.processarSolicitacaoCancelamento(request);

        executarVerificacoes(never());

        var camt029 = camt029Captor.getValue();

        validarCamt029Enviada(camt029, camt055Dto, MotivoRejeicaoCamt029.ID_FIM_A_FIM_NAO_CORRESPONDE_AO_ORIGINAL_INFORMADO);
    }

    @Test
    void dadaUmaSolicitacaoCancelamentoComInstrucaoDePagamentoComStatusInvalido_quandoProcessar_deveEnviarCamt029ComCodRejeicao() {
        var camt055Dto = buildCamt055Dto();
        var request = IdempotentAsyncRequest.<Camt055Dto>builder().value(camt055Dto).build();
        IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
        var instrucaoPagamento = buildRecorrenciaInstrucaoPagamento();
        instrucaoPagamento.setTpoStatus("ATIVA");

        when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));
        when(repository.save(any())).thenReturn(new RecorrenciaInstrucaoPagamentoCancelamento());
        when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
        when(criaResponseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
        when(eventoResponseFactory.criarEventoCamt029IcomEnvio(any())).thenReturn(new EventoResponseDTO(Camt029Dto.builder().build(), new HashMap<>(), TOPICO));

        service.processarSolicitacaoCancelamento(request);

        executarVerificacoes(never());

        var camt029 = camt029Captor.getValue();

        validarCamt029Enviada(camt029, camt055Dto, MotivoRejeicaoCamt029.PAGAMENTO_JA_CONCLUIDO_COM_SUCESSO);
    }

    @Test
    void dadaUmaSolicitacaoCancelamentoRecebidoEmHorarioInvalido_quandoProcessar_deveEnviarCamt029ComCodRejeicao() {
        var camt055Dto = buildCamt055Dto();
        var request = IdempotentAsyncRequest.<Camt055Dto>builder().value(camt055Dto).build();
        IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
        var instrucaoPagamento = buildRecorrenciaInstrucaoPagamento();
        instrucaoPagamento.setCodFimAFim("XXxxxxxxx202310011030kkkkkkk");

        when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));
        when(repository.save(any())).thenReturn(new RecorrenciaInstrucaoPagamentoCancelamento());
        when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
        when(criaResponseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
        when(eventoResponseFactory.criarEventoCamt029IcomEnvio(any())).thenReturn(new EventoResponseDTO(Camt029Dto.builder().build(), new HashMap<>(), TOPICO));

        service.processarSolicitacaoCancelamento(request);

        executarVerificacoes(never());

        var camt029 = camt029Captor.getValue();

        validarCamt029Enviada(camt029, camt055Dto, MotivoRejeicaoCamt029.SOLICITACAO_CANCELAMENTO_NAO_RECEBIDA_NO_PRAZO);
    }

    @Test
    void dadaUmaSolicitacaoCancelamentoComIdConciliacaoInvalido_quandoProcessar_devenEnviarCamt029ComCodRejeicao() {
        var camt055Dto = buildCamt055Dto();
        var request = IdempotentAsyncRequest.<Camt055Dto>builder().value(camt055Dto).build();
        IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
        var instrucaoPagamento = buildRecorrenciaInstrucaoPagamento();
        instrucaoPagamento.setIdConciliacaoRecebedor("idConciliacaoRecebedorDiferente");

        when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));
        when(repository.save(any())).thenReturn(new RecorrenciaInstrucaoPagamentoCancelamento());
        when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
        when(criaResponseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
        when(eventoResponseFactory.criarEventoCamt029IcomEnvio(any())).thenReturn(new EventoResponseDTO(Camt029Dto.builder().build(), new HashMap<>(), TOPICO));

        service.processarSolicitacaoCancelamento(request);

        executarVerificacoes(never());

        var camt029 = camt029Captor.getValue();

        validarCamt029Enviada(camt029, camt055Dto, MotivoRejeicaoCamt029.ID_CONCILICAO_RECEBEDOR_NAO_CORRESPONDE_AO_ORIGINALMENTE_INFORMADO);
    }

    @Test
    void cpfCnpjRecebedorDiferente() {
        var camt055Dto = buildCamt055Dto();
        var request = IdempotentAsyncRequest.<Camt055Dto>builder().value(camt055Dto).build();
        IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();
        var instrucaoPagamento = buildRecorrenciaInstrucaoPagamento();
        instrucaoPagamento.setNumCpfCnpjRecebedor("12312123");

        when(instrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(any())).thenReturn(Optional.of(instrucaoPagamento));
        when(repository.save(any())).thenReturn(new RecorrenciaInstrucaoPagamentoCancelamento());
        when(criaResponseStrategyFactory.criar(any())).thenReturn(criaResponseStrategy);
        when(criaResponseStrategy.criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any())).thenReturn(idempotentResponse);
        when(eventoResponseFactory.criarEventoCamt029IcomEnvio(any())).thenReturn(new EventoResponseDTO(Camt029Dto.builder().build(), new HashMap<>(), TOPICO));

        service.processarSolicitacaoCancelamento(request);

        executarVerificacoes(never());

        var camt029 = camt029Captor.getValue();

        validarCamt029Enviada(camt029, camt055Dto, MotivoRejeicaoCamt029.CPF_CNPJ_USUARIO_RECEBEDOR_NAO_CORRESPONDENTE_RECORRENCIA_OU_AUTORIZACAO);
    }

    private static void validarCamt029Enviada(Camt029Dto camt029, Camt055Dto camt055Dto, MotivoRejeicaoCamt029 motivoRejeicao) {
        assertAll(
                () -> assertNotNull(camt029),
                () -> assertEquals(camt055Dto.getIdCancelamentoAgendamento(), camt029.getIdCancelamentoAgendamentoOriginal()),
                () -> assertEquals(camt055Dto.getIdConciliacaoRecebedorOriginal(), camt029.getIdConciliacaoRecebedorOriginal()),
                () -> assertEquals(StatusCancelamentoCamt029.REJEITADO.name(), camt029.getStatusDoCancelamento()),
                () -> assertEquals(motivoRejeicao.name(), camt029.getCodigoDeRejeicaoDoCancelamento()),
                () -> assertEquals(camt055Dto.getIdFimAFimOriginal(), camt029.getIdFimAFimOriginal()),
                () -> assertEquals(TipoAceitacaoRejeicaoCamt029.DATA_HORA_REJEICAO_CANCELAMENTO.name(), camt029.getTipoAceitacaoOuRejeicao()),
                () -> assertNotNull(camt029.getDataHoraAceitacaoOuRejeicaoDoCancelamento()),
                () -> assertEquals(camt055Dto.getParticipanteDestinatarioDoCancelamento(), camt029.getParticipanteRecebeAtualizacaoDoCancelamento()),
                () -> assertEquals(camt055Dto.getParticipanteSolicitanteDoCancelamento(), camt029.getParticipanteAtualizaSolicitacaoDoCancelamento())
        );
    }

    private void executarVerificacoes(VerificationMode verificaAtualizaInstrucaoPagamento) {
        verify(instrucaoPagamentoService, times(1)).buscarPorCodFimAFimComAutorizacao(any());
        verify(repository, times(1)).save(any());
        verify(instrucaoPagamentoService, verificaAtualizaInstrucaoPagamento).atualizaTpoStatusETpoSubStatus(any(), any(), any());
        verify(eventoResponseFactory, times(1)).criarEventoCamt029IcomEnvio(camt029Captor.capture());
        verify(criaResponseStrategyFactory, times(1)).criar(any());
        verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(OperacaoRequest.class), any(), any(), any());
    }

    private static RecorrenciaAutorizacao buildRecorrenciaAutorizacao() {
        return RecorrenciaAutorizacao.builder()
                .cpfCnpjRecebedor("1")
                .build();
    }

    private static RecorrenciaInstrucaoPagamento buildRecorrenciaInstrucaoPagamento() {
        return RecorrenciaInstrucaoPagamento.builder()
                .codFimAFim("XXxxxxxxx202610011030kkkkkkk")
                .tpoStatus("CRIADA")
                .numCpfCnpjRecebedor("1")
                .numCpfCnpjPagador("cpfCnpjUsuarioSolicitanteCancelamento")
                .idConciliacaoRecebedor("idConciliacaoRecebedorOriginal")
                .autorizacao(buildRecorrenciaAutorizacao())
                .build();
    }

    private Camt055Dto buildCamt055Dto() {
        return Camt055Dto.builder()
                .idCancelamentoAgendamento("idCancelamentoAgendamento")
                .idConciliacaoRecebedorOriginal("idConciliacaoRecebedorOriginal")
                .cpfCnpjUsuarioSolicitanteCancelamento("cpfCnpjUsuarioSolicitanteCancelamento")
                .motivoCancelamento("motivoCancelamento")
                .idFimAFimOriginal("idFimAFimOriginal")
                .tipoSolicitacaoOuInformacao("tipoSolicitacaoOuInformacao")
                .dataHoraSolicitacaoOuInformacao(LocalDateTime.of(2025, 10, 1, 10, 20))
                .participanteSolicitanteDoCancelamento("123")
                .participanteDestinatarioDoCancelamento("321")
                .dataHoraCriacaoParaEmissao(LocalDateTime.of(2024,10,10,0,0))
                .build();
    }
}