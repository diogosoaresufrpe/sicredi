package io.sicredi.spirecorrencia.api.liquidacao;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetornoPagamentoConsumerTest {

    public static final String ID_FIM_A_FIM = "idFimAFim";
    public static final String PAGAMENTO_COM_RECORRENCIA = "PAGAMENTO_COM_RECORRENCIA";
    public static final String PAGAMENTO_COM_AUTOMATICO = "PAGAMENTO_COM_AUTOMATICO";
    public static final String TIPO_PRODUTO = "TIPO_PRODUTO";
    @Mock
    private AppConfig appConfig;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private LiquidacaoService liquidacaoService;

    @Mock
    private ObservabilidadeDecorator observabilidadeDecorator;

    @Captor
    private ArgumentCaptor<Runnable> functionCaptor;

    @Mock
    private ConsumerRecord<String,String> consumerRecord;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private RetornoPagamentoConsumer retornoPagamentoConsumer;

    @ParameterizedTest
    @ValueSource(strings = {"AGENDADO_RECORRENTE", "AGENDADO_RECORRENTE_COBV2", "AGENDADO_RECORRENTE_AGENTE"})
    void deveProcessarRetornoPagamentoComRecorrenciaQuandoEAgendadoComRecorrente(String tipoProduto) {
        retornoPagamentoConsumer.consumir(consumerRecord, tipoProduto, ID_FIM_A_FIM, acknowledgment);

        verify(observabilidadeDecorator).executar(any(), functionCaptor.capture());
        functionCaptor.getValue().run();
        verify(liquidacaoService, never()).processarRetornoPagamentoComRecorrencia(any(), any());
        verify(liquidacaoService).processarRetornoPagamentoAgendadoRecorrente(any(), any());
        verifyNoMoreInteractions(liquidacaoService);

    }

    @Test
    void deveProcessarRetornoPagamentoComRecorrenciaQuandoEPagamentoComRecorrente() {
        retornoPagamentoConsumer.consumir(consumerRecord, PAGAMENTO_COM_RECORRENCIA, ID_FIM_A_FIM, acknowledgment);

        verify(observabilidadeDecorator).executar(any(), functionCaptor.capture());
        functionCaptor.getValue().run();
        verify(liquidacaoService).processarRetornoPagamentoComRecorrencia(any(), any());
        verify(liquidacaoService, never()).processarRetornoPagamentoAgendadoRecorrente(any(), any());
        verifyNoMoreInteractions(liquidacaoService);

    }

    @Test
    void dadoPagamentoComAutomatico_quandoConsumir_entaoProcessaComSucesso() {
        retornoPagamentoConsumer.consumir(consumerRecord, PAGAMENTO_COM_AUTOMATICO, ID_FIM_A_FIM, acknowledgment);

        verify(observabilidadeDecorator).executar(any(), functionCaptor.capture());
        functionCaptor.getValue().run();
        verify(liquidacaoService, never()).processarRetornoPagamentoComRecorrencia(any(), any());
        verify(liquidacaoService, never()).processarRetornoPagamentoAgendadoRecorrente(any(), any());
        verify(liquidacaoService).processarRetornoPagamentoComAutomatico(any());
        verifyNoMoreInteractions(liquidacaoService);

    }

    @Test
    void naoDeveProcessarQuandoForTipoProdutoNaoMapeado() {
        retornoPagamentoConsumer.consumir(consumerRecord, TIPO_PRODUTO, ID_FIM_A_FIM, acknowledgment);

        verify(observabilidadeDecorator).executar(any(), functionCaptor.capture());
        functionCaptor.getValue().run();
        verify(liquidacaoService, never()).processarRetornoPagamentoComRecorrencia(any(), any());
        verify(liquidacaoService, never()).processarRetornoPagamentoAgendadoRecorrente(any(), any());

    }

}