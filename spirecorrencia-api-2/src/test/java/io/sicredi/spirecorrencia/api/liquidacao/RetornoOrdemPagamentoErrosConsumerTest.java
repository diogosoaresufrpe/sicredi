package io.sicredi.spirecorrencia.api.liquidacao;

import com.fasterxml.jackson.core.JsonParser;
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

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RetornoOrdemPagamentoErrosConsumerTest {

    @Mock
    private ObjectMapper objectMapperError;

    @Mock
    private LiquidacaoService liquidacaoServiceError;

    @Mock
    private ObservabilidadeDecorator observabilidadeDecoratorError;

    @Captor
    private ArgumentCaptor<Runnable> functionCaptorError;

    @Mock
    private ConsumerRecord<String, String> consumerRecordError;

    @Mock
    private Acknowledgment acknowledgmentError;

    @Mock
    OrdemErroProcessamentoResponse ordemErroProcessamentoResponseError;

    @InjectMocks
    private RetornoOrdemPagamentoErrosConsumer retornoOrdemPagamentoErrosConsumerError;

    @ParameterizedTest
    @ValueSource(strings = {"AGENDADO_RECORRENTE", "AGENDADO_RECORRENTE_COBV2", "AGENDADO_RECORRENTE_AGENTE"})
    public void deveAtualizarErroNaLiquidacaoDaRecorrenciaQuandoForTipoAgendadoRecorrente(String tipoProduto) throws IOException {
        when(objectMapperError.readValue(consumerRecordError.value(), OrdemErroProcessamentoResponse.class)).thenReturn(ordemErroProcessamentoResponseError);
        retornoOrdemPagamentoErrosConsumerError.consumirRetornoOrdemPagamentoErro(consumerRecordError, tipoProduto, "idFimAFim", acknowledgmentError);

        verify(observabilidadeDecoratorError).executar(any(), functionCaptorError.capture());
        functionCaptorError.getValue().run();

        verify(liquidacaoServiceError).atualizaRecorrenciaLiquidacaoDaTransacaoComErro(any(), any());
        verifyNoMoreInteractions(liquidacaoServiceError);
    }

    @Test
    void naoDeveProcessarQuandoForTipoProdutoNaoMapeado() {
        retornoOrdemPagamentoErrosConsumerError.consumirRetornoOrdemPagamentoErro(consumerRecordError, "TIPO_PRODUTO", "idFimAFim", acknowledgmentError);

        verify(observabilidadeDecoratorError).executar(any(), functionCaptorError.capture());
        functionCaptorError.getValue().run();
        verify(liquidacaoServiceError, never()).atualizaRecorrenciaLiquidacaoDaTransacaoComErro(any(), any());

    }

}