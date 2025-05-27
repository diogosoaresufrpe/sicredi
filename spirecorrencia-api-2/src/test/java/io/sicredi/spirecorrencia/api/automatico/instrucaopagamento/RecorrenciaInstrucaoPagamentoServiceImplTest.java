package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.dto.Pain014Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain014;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.idempotente.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static br.com.sicredi.spi.entities.type.SituacaoAgendamentoPain014.ACEITA_USUARIO_PAGADOR;
import static br.com.sicredi.spi.entities.type.SituacaoAgendamentoPain014.REJEITADA_USUARIO_PAGADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
@ExtendWith(MockitoExtension.class)
class RecorrenciaInstrucaoPagamentoServiceImplTest {
    @Mock
    private EventoResponseFactory eventoResponseFactory;

    @Mock
    private ProcessaRecorrenciaInstrucaoPagamentoService processaRecorrenciaInstrucaoPagamentoService;

    @Mock
    private CriaResponseStrategyFactory criaResponseStrategyFactory;

    @Mock
    private CriaResponseStrategy criaResponseStrategy;

    @InjectMocks
    private RecorrenciaInstrucaoPagamentoServiceImpl recorrenciaInstrucaoPagamentoService;

    @Captor
    private ArgumentCaptor<Pain014Dto> pain014Captor;

    private Pain013Dto pain013;
    private IdempotentAsyncRequest<Pain013Dto> request;
    private RecorrenciaInstrucaoPagamento recorrenciaInstrucaoPagamento;

    @BeforeEach
    void setup() {
        var idempotentResponse = IdempotentResponse.<Pain014Dto>builder().errorResponse(false).build();
        var evento = Instancio.create(EventoResponseDTO.class);
        pain013 = Instancio.create(Pain013Dto.class);
        recorrenciaInstrucaoPagamento = Instancio.create(RecorrenciaInstrucaoPagamento.class);
        request = IdempotentAsyncRequest
                .<Pain013Dto>builder()
                .transactionId(RandomStringUtils.randomAlphanumeric(5))
                .value(pain013)
                .build();

        when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
        when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class),
                any(), any(), any())).thenReturn(idempotentResponse);
        when(eventoResponseFactory.criarEventoPain14IcomEnvio(any())).thenReturn(evento);
    }

    @Test
    void dadoPain013EInstrucaoPagamentoAceita_quandoProcessarPedidoAgendamentoDebito_deveEmitirPain014ComSituacaoAceita() {
        recorrenciaInstrucaoPagamento.setCodMotivoRejeicao(null);

        when(processaRecorrenciaInstrucaoPagamentoService.processar(pain013)).thenReturn(recorrenciaInstrucaoPagamento);

        var response = recorrenciaInstrucaoPagamentoService.processarPedidoAgendamentoDebito(request);

        assertNotNull(response);
        verify(processaRecorrenciaInstrucaoPagamentoService).processar(pain013);
        verify(eventoResponseFactory).criarEventoPain14IcomEnvio(pain014Captor.capture());
        assertThat(pain014Captor.getValue())
                .satisfies(pain014 -> {
                    assertThat(pain014.getSituacaoDoAgendamento()).isEqualTo(ACEITA_USUARIO_PAGADOR.name());
                    assertThat(pain014.getCodigoDeErro()).isNull();
                    verificarCamposBase(pain014, pain013);
                });
    }

    @Test
    void dadoPain013EInstrucaoPagamentoRejeitada_quandoProcessarPedidoAgendamentoDebito_deveEmitirPain014ComSituacaoRejeitada() {
        var erro = MotivoRejeicaoPain014.ID_RECORRENCIA_INEXISTENTE_OU_INCORRETO;
        recorrenciaInstrucaoPagamento.setCodMotivoRejeicao(erro.name());

        when(processaRecorrenciaInstrucaoPagamentoService.processar(pain013)).thenReturn(recorrenciaInstrucaoPagamento);

        var response = recorrenciaInstrucaoPagamentoService.processarPedidoAgendamentoDebito(request);

        assertNotNull(response);

        verify(processaRecorrenciaInstrucaoPagamentoService).processar(pain013);
        verify(eventoResponseFactory).criarEventoPain14IcomEnvio(pain014Captor.capture());
        assertThat(pain014Captor.getValue())
                .satisfies(pain014 -> {
                    assertThat(pain014.getSituacaoDoAgendamento()).isEqualTo(REJEITADA_USUARIO_PAGADOR.name());
                    assertThat(pain014.getCodigoDeErro()).isEqualTo(erro.name());
                    verificarCamposBase(pain014, pain013);
                });
    }

    private static void verificarCamposBase(Pain014Dto pain014, Pain013Dto pain013) {
        assertThat(pain014.getIdFimAFimOriginal()).isEqualTo(pain013.getIdFimAFim());
        assertThat(pain014.getDataHoraAceitacaoOuRejeicaoDoAgendamento()).isNotNull();
        assertThat(pain014.getParticipanteDoUsuarioRecebedor()).isEqualTo(pain013.getParticipanteDoUsuarioRecebedor());
        assertThat(pain014.getCpfCnpjUsuarioRecebedor()).isEqualTo(pain013.getCpfCnpjUsuarioRecebedor());
    }

}
