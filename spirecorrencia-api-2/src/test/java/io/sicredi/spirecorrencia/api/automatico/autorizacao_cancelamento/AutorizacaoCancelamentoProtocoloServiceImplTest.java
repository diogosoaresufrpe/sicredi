package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11;
import br.com.sicredi.spi.entities.type.TipoRecorrencia;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoCancelamentoRepository;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacaoRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.*;
import io.sicredi.spirecorrencia.api.idempotente.*;
import jakarta.validation.Validator;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoCancelamentoProtocoloServiceImplTest {

    public static final String ID_RECORRENCIA = "id_recorrencia";
    @Mock
    private RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;
    @Mock
    private RecorrenciaAutorizacaoRepository autorizacaoRepository;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Mock
    private CriaResponseStrategyFactory<CancelamentoAutorizacaoRequest> criaResponseStrategyFactory;
    @Mock
    private Validator validator;
    @Mock
    private CriaResponseStrategy<CancelamentoAutorizacaoRequest> criaResponseStrategy;

    @InjectMocks
    private AutorizacaoCancelamentoProtocoloServiceImpl service;

    private final IdempotentResponse<?> response = IdempotentResponse.builder().build();
    private static final String DEFAULT_JSON = "{\"foo\":\"bar\"}";
    private static final String HEADER_OPERACAO_CANCELAMENTO_PAGADOR = "CANCELAMENTO_PAGADOR";
    private static final String TOPICO_ENVIO = "topico-envio";
    private static final String CPF_CNPJ_SOLICITANTE_CANCELAMENTO = "63262164004";
    private static final String CPF_CNPJ_PAGADOR = "63262164004";
    private static final String CPF_CNPJ_RECEBEDOR = "63262164005";

    @Test
    @DisplayName("""
            Dado uma request, mas, Recorrencia Autorização não e encontrada
            Quando processaCancelamentoRecorrenciaAutorizacao
            Deve retonar uma IdempotenciaResponse com error
            """)
    void dadoUmaRequestMasRecorrenciaAutorizacaoNaoEncontrada_quandoProcessaCancelamentoRecorrenciaAutorizacao_DeveRetornarUmaIdempotenciaResponseComError() {
        var protocoloRequest = criaCancelamentoAutorizacaoRequest(CPF_CNPJ_SOLICITANTE_CANCELAMENTO);
        configuraMockFabricaIdempotenciaResponseComError();

        var idempotentRequest = IdempotentRequest.<CancelamentoAutorizacaoRequest>builder()
                .value(protocoloRequest)
                .build();

        when(autorizacaoRepository.findById(protocoloRequest.getOidRecorrenciaAutorizacao()))
                .thenReturn(Optional.empty());

        var resultado = service.processaCancelamentoRecorrenciaAutorizacao(idempotentRequest);

        assertEquals(response, resultado);

        verify(autorizacaoRepository).findById(protocoloRequest.getOidRecorrenciaAutorizacao());
        verify(validator).validate(protocoloRequest);
        verify(criaResponseStrategyFactory).criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO);
        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
        verify(eventoResponseFactory, never()).criaEventoPain011(any(Pain011Dto.class), eq(HEADER_OPERACAO_CANCELAMENTO_PAGADOR));
    }

    @Test
    @DisplayName("""
            Dado uma request, mas, RecorrenciaAutorização não esta com status Aprovado
            Quando processaCancelamentoRecorrenciaAutorizacao
            Deve retonar uma IdempotenciaResponse com error
            """)
    void dadoUmaRequestDiferenteDeAprovado_quandoProcessaCancelamentoRecorrenciaAutorizacao_DeveRetornarUmaIdempotenciaResponseComError() {
        var protocoloRequest = criaCancelamentoAutorizacaoRequest(CPF_CNPJ_SOLICITANTE_CANCELAMENTO);

        configuraMockFabricaIdempotenciaResponseComError();
        var idempotentRequest = IdempotentRequest.<CancelamentoAutorizacaoRequest>builder()
                .value(protocoloRequest)
                .build();

        when(autorizacaoRepository.findById(protocoloRequest.getOidRecorrenciaAutorizacao()))
                .thenReturn(Optional.of(
                        RecorrenciaAutorizacao.builder()
                                .tipoStatus(TipoStatusAutorizacao.CANCELADA)
                                .build()
                ));

        var resultado = service.processaCancelamentoRecorrenciaAutorizacao(idempotentRequest);

        assertEquals(response, resultado);

        verify(autorizacaoRepository).findById(protocoloRequest.getOidRecorrenciaAutorizacao());
        verify(criaResponseStrategyFactory).criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO);
        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
        verify(validator).validate(protocoloRequest);
        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
        verify(eventoResponseFactory, never()).criaEventoPain011(any(Pain011Dto.class), eq(HEADER_OPERACAO_CANCELAMENTO_PAGADOR));
    }

    @Test
    @DisplayName("""
            Dado uma request, mas, ocorre um erro de validação de request
            Quando processaCancelamentoRecorrenciaAutorizacao
            Deve retonar uma IdempotenciaResponse com error
            """)
    void dadoUmaRequestMasOcorreUmErroDeValidacaoDeRequest_quandoProcessaCancelamentoRecorrenciaAutorizacao_DeveRetornarUmaIdempotenciaResponseComError() {
        var protocoloRequest = criaCancelamentoAutorizacaoRequest(CPF_CNPJ_SOLICITANTE_CANCELAMENTO);

        var mockConstraintViolation = mock(ConstraintViolationImpl.class);
        when(validator.validate(any())).thenReturn(Set.of(mockConstraintViolation));
        when(mockConstraintViolation.getMessage()).thenReturn("mensagem de error");

        configuraMockFabricaIdempotenciaResponseComError();
        var idempotentRequest = IdempotentRequest.<CancelamentoAutorizacaoRequest>builder()
                .value(protocoloRequest)
                .build();

        var resultado = service.processaCancelamentoRecorrenciaAutorizacao(idempotentRequest);

        assertEquals(response, resultado);

        verify(validator).validate(protocoloRequest);
        verify(autorizacaoRepository, never()).findById(protocoloRequest.getOidRecorrenciaAutorizacao());
        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
        verify(eventoResponseFactory, never()).criaEventoPain011(any(Pain011Dto.class), eq(HEADER_OPERACAO_CANCELAMENTO_PAGADOR));
        verify(criaResponseStrategyFactory).criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO);
        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("""
            Dado uma request, mas, cpf/cnpj do solicitante e diferente do pagador ou recebedor
            Quando processaCancelamentoRecorrenciaAutorizacao
            Deve retonar uma IdempotenciaResponse com error
            """)
    void dadoUmaRequestSolicitanteDiferente_quandoProcessaCancelamentoRecorrenciaAutorizacao_DeveRetornarUmaIdempotenciaResponseComError() {
        var protocoloRequest = criaCancelamentoAutorizacaoRequest("solicitante");
        when(validator.validate(any())).thenReturn(Set.of());

        configuraMockFabricaIdempotenciaResponseComError();
        var idempotentRequest = IdempotentRequest.<CancelamentoAutorizacaoRequest>builder()
                .value(protocoloRequest)
                .build();

        when(autorizacaoRepository.findById(protocoloRequest.getOidRecorrenciaAutorizacao()))
                .thenReturn(Optional.of(criaRecorrenciaAutorizacao()));

        var resultado = service.processaCancelamentoRecorrenciaAutorizacao(idempotentRequest);

        assertEquals(response, resultado);

        verify(autorizacaoRepository).findById(protocoloRequest.getOidRecorrenciaAutorizacao());
        verify(criaResponseStrategyFactory).criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO);
        verify(validator).validate(protocoloRequest);

        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
        verify(eventoResponseFactory, never()).criaEventoPain011(any(Pain011Dto.class), eq(HEADER_OPERACAO_CANCELAMENTO_PAGADOR));
        verify(recorrenciaAutorizacaoCancelamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("""
            Dado uma request valida
            Quando processaCancelamentoRecorrenciaAutorizacao
            Deve retonar uma IdempotenciaResponse com sucesso
            """)
    void dadoUmaRequestValida_quandoProcessaCancelamentoRecorrenciaAutorizacao_DeveRetornarUmaIdempotenciaResponseComSucesso() {
        when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                .thenReturn(criaResponseStrategy);
        var protocoloRequest = criaCancelamentoAutorizacaoRequest(CPF_CNPJ_SOLICITANTE_CANCELAMENTO);

        when(validator.validate(any())).thenReturn(Set.of());

        var idempotentRequest = IdempotentRequest.<CancelamentoAutorizacaoRequest>builder()
                .value(protocoloRequest)
                .build();

        var recorrenciaAutorizacao = criaRecorrenciaAutorizacao();
        when(autorizacaoRepository.findById(protocoloRequest.getOidRecorrenciaAutorizacao()))
                .thenReturn(Optional.of(recorrenciaAutorizacao));
        var recorrenciaAutorizacaoCancelamento = criaRecorrenciaAutorizacaoCancelamento(protocoloRequest);
        when(recorrenciaAutorizacaoCancelamentoRepository.save(any(RecorrenciaAutorizacaoCancelamento.class)))
                .thenReturn(recorrenciaAutorizacaoCancelamento);
        when(eventoResponseFactory.criaEventoPain011(any(Pain011Dto.class), eq(HEADER_OPERACAO_CANCELAMENTO_PAGADOR)))
                .thenReturn(new EventoResponseDTO(DEFAULT_JSON, Map.of("foo", "bar"), TOPICO_ENVIO));

        service.processaCancelamentoRecorrenciaAutorizacao(idempotentRequest);

        verify(criaResponseStrategyFactory).criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO);
        verify(autorizacaoRepository).findById(protocoloRequest.getOidRecorrenciaAutorizacao());
        verify(validator).validate(protocoloRequest);

        var recorreciaAutorizacaoCancelamentoCaptor = ArgumentCaptor.forClass(RecorrenciaAutorizacaoCancelamento.class);
        verify(recorrenciaAutorizacaoCancelamentoRepository).save(recorreciaAutorizacaoCancelamentoCaptor.capture());

        var cancelamento = recorreciaAutorizacaoCancelamentoCaptor.getValue();
        validaRecorrenciaAutorizacaoCancelamento(protocoloRequest, cancelamento, recorrenciaAutorizacao);

        var pain011Captor = ArgumentCaptor.forClass(Pain011Dto.class);
        verify(eventoResponseFactory).criaEventoPain011(pain011Captor.capture(), eq(HEADER_OPERACAO_CANCELAMENTO_PAGADOR));

        var pain011 = pain011Captor.getValue();
        validaPain011(recorrenciaAutorizacao, pain011, protocoloRequest);

        verify(recorrenciaAutorizacaoCancelamentoRepository).save(recorreciaAutorizacaoCancelamentoCaptor.capture());
        verify(autorizacaoRepository).atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(
                protocoloRequest.getOidRecorrenciaAutorizacao(),
                TipoStatusAutorizacao.APROVADA,
                TipoSubStatus.AGUARDANDO_CANCELAMENTO.name()
        );
    }

    private static RecorrenciaAutorizacao criaRecorrenciaAutorizacao() {
        return RecorrenciaAutorizacao.builder()
                .idRecorrencia(ID_RECORRENCIA)
                .tipoStatus(TipoStatusAutorizacao.APROVADA)
                .cpfCnpjRecebedor(CPF_CNPJ_RECEBEDOR)
                .cpfCnpjPagador(CPF_CNPJ_PAGADOR)
                .build();
    }

    private static void validaRecorrenciaAutorizacaoCancelamento(CancelamentoAutorizacaoRequest protocoloRequest, RecorrenciaAutorizacaoCancelamento cancelamento, RecorrenciaAutorizacao recorrenciaAutorizacao) {
        assertEquals(protocoloRequest.getMotivoCancelamento(), cancelamento.getMotivoCancelamento());
        assertEquals(protocoloRequest.getCpfCnpjSolicitanteCancelamento(), cancelamento.getCpfCnpjSolicitanteCancelamento());
        assertEquals(TipoCancelamento.RECORRENCIA_AUTORIZACAO, cancelamento.getTipoCancelamento());
        assertEquals(TipoSolicitanteCancelamento.PAGADOR, cancelamento.getTipoSolicitanteCancelamento());
        assertEquals(protocoloRequest.getCpfCnpjSolicitanteCancelamento(), cancelamento.getCpfCnpjSolicitanteCancelamento());
        assertEquals(protocoloRequest.getDataHoraInicioCanal(), cancelamento.getDataCancelamento());
        assertEquals(recorrenciaAutorizacao.getIdRecorrencia(), cancelamento.getIdRecorrencia());
    }

    private static void validaPain011(RecorrenciaAutorizacao recorrenciaAutorizacao, Pain011Dto pain011, CancelamentoAutorizacaoRequest protocoloRequest) {
        assertEquals(recorrenciaAutorizacao.getIdRecorrencia(), pain011.getIdRecorrencia());
        assertEquals(protocoloRequest.getIdInformacaoCancelamento(), pain011.getIdInformacaoCancelamento());
        assertEquals(TipoRecorrencia.RECORRENTE.name(), pain011.getTipoRecorrencia());
        assertEquals(recorrenciaAutorizacao.getTipoFrequencia(), pain011.getTipoFrequencia());
        assertEquals(recorrenciaAutorizacao.getDataFinalRecorrencia(), pain011.getDataFinalRecorrencia());
        assertFalse(pain011.getIndicadorObrigatorio());
        assertEquals(recorrenciaAutorizacao.getValor(), pain011.getValor());
        assertEquals(recorrenciaAutorizacao.getDataInicialRecorrencia(), pain011.getDataInicialRecorrencia());
        assertEquals(CPF_CNPJ_RECEBEDOR, pain011.getCpfCnpjUsuarioRecebedor());
        assertEquals(recorrenciaAutorizacao.getInstituicaoRecebedor(), pain011.getParticipanteDoUsuarioRecebedor());
        assertEquals(recorrenciaAutorizacao.getContaPagador(), pain011.getContaUsuarioPagador());
        assertEquals(recorrenciaAutorizacao.getNomeRecebedor(), pain011.getNomeUsuarioRecebedor());
        assertEquals(recorrenciaAutorizacao.getAgenciaPagador(), pain011.getAgenciaUsuarioPagador());
        assertEquals(CPF_CNPJ_PAGADOR, pain011.getCpfCnpjUsuarioPagador());
        assertEquals(recorrenciaAutorizacao.getInstituicaoPagador(), pain011.getParticipanteDoUsuarioPagador());
        assertEquals(recorrenciaAutorizacao.getCpfCnpjDevedor(), pain011.getCpfCnpjDevedor());
        assertEquals(recorrenciaAutorizacao.getNumeroContrato(), pain011.getNumeroContrato());
        assertEquals(recorrenciaAutorizacao.getNomeDevedor(), pain011.getNomeDevedor());
    }

    private static RecorrenciaAutorizacaoCancelamento criaRecorrenciaAutorizacaoCancelamento(CancelamentoAutorizacaoRequest protocoloRequest) {
        return RecorrenciaAutorizacaoCancelamento.builder()
                .idInformacaoCancelamento(protocoloRequest.getIdInformacaoCancelamento())
                .idRecorrencia(ID_RECORRENCIA)
                .tipoCancelamento(TipoCancelamento.RECORRENCIA_AUTORIZACAO)
                .tipoSolicitanteCancelamento(TipoSolicitanteCancelamento.PAGADOR)
                .tipoStatus(TipoStatusCancelamentoAutorizacao.CRIADA)
                .cpfCnpjSolicitanteCancelamento(protocoloRequest.getCpfCnpjSolicitanteCancelamento())
                .motivoCancelamento(MotivoCancelamentoPain11.of(protocoloRequest.getMotivoCancelamento()).name())
                .dataCancelamento(protocoloRequest.getDataHoraInicioCanal())
                .build();
    }

    private static CancelamentoAutorizacaoRequest criaCancelamentoAutorizacaoRequest(String cpfCnpjSolicitanteCancelamento) {
        return CancelamentoAutorizacaoRequest.builder()
                .idInformacaoCancelamento("ID_CANCELAMENTO")
                .motivoCancelamento("CONFIRMADA_POR_OUTRA_JORNADA")
                .cpfCnpjSolicitanteCancelamento(cpfCnpjSolicitanteCancelamento)
                .build();
    }

    private void configuraMockFabricaIdempotenciaResponseComError() {
        var responseStrategy = mock(CriaResponseStrategy.class);
        when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                .thenReturn(responseStrategy);
        when(responseStrategy.criarResponseIdempotentErro(any(), any(), any()))
                .thenReturn(response);
    }
}