package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.spi.dto.TransacaoDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
class RetornoPagamentoConsumer {

    private static final String TIPO_PRODUTO_PAGAMENTO_COM_RECORRENCIA = "PAGAMENTO_COM_RECORRENCIA";
    private static final String TIPO_PRODUTO_PAGAMENTO_COM_AUTOMATICO = "PAGAMENTO_COM_AUTOMATICO";
    private static final String TIPO_PRODUTO_AGENDADO_RECORRENTE = "AGENDADO_RECORRENTE";
    private static final String TIPO_PRODUTO_AGENDADO_RECORRENTE_COBV2 = "AGENDADO_RECORRENTE_COBV2";
    private static final String TIPO_PRODUTO_AGENDADO_RECORRENTE_AGENTE = "AGENDADO_RECORRENTE_AGENTE";

    @SuppressWarnings("unused")
    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final LiquidacaoService liquidacaoService;
    private final ObservabilidadeDecorator observabilidadeDecorator;

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.retornoTransacao.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.retornoTransacao.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            timeout = "#{@appConfig.kafka.consumer.retornoTransacao.retry.timeout}",
            retryTopicSuffix = "-spirecorrencia-api-v1-retry",
            dltStrategy = DltStrategy.NO_DLT
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.retornoTransacao.nome}",
            concurrency = "#{@appConfig.kafka.consumer.retornoTransacao.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.retornoTransacao.groupId}",
            containerFactory = "protocoloKafkaListenerContainerFactory"
    )
    public void consumir(ConsumerRecord<String, String> consumerRecord,
                         @Header("tipoProduto") String tipoProduto,
                         @Header("idFimAFim") String idFimAFim,
                         Acknowledgment acknowledgment) {
        var identificadorTransacao = UUID.randomUUID().toString();
        var atributos = Map.of(
                RecorrenciaMdc.ID_FIM_A_FIM, idFimAFim,
                RecorrenciaMdc.IDENTIFICADOR_TRANSACAO, identificadorTransacao
        );
        observabilidadeDecorator.executar(atributos, () -> {
            try {
                log.info("Consumindo retorno de transação de tipoProduto: {}", tipoProduto);
                final var transacaoDTO = objectMapper.readValue(consumerRecord.value(), TransacaoDto.class);
                switch (tipoProduto) {
                    case TIPO_PRODUTO_AGENDADO_RECORRENTE, TIPO_PRODUTO_AGENDADO_RECORRENTE_COBV2, TIPO_PRODUTO_AGENDADO_RECORRENTE_AGENTE ->
                            liquidacaoService.processarRetornoPagamentoAgendadoRecorrente(identificadorTransacao, transacaoDTO);
                    case TIPO_PRODUTO_PAGAMENTO_COM_RECORRENCIA ->
                            liquidacaoService.processarRetornoPagamentoComRecorrencia(identificadorTransacao, transacaoDTO);
                    case TIPO_PRODUTO_PAGAMENTO_COM_AUTOMATICO ->
                            liquidacaoService.processarRetornoPagamentoComAutomatico(transacaoDTO);
                    default ->
                            log.debug("Descartando evento do tópico {} pois mensagem não contém o header tipoProduto com valor esperado. TipoProduto recebido = {}", consumerRecord.topic(), tipoProduto);
                }
                log.info("Consumo concluído para retorno de transação de tipoProduto: {}", tipoProduto);
                acknowledgment.acknowledge();
            } catch (JsonProcessingException e) {
                log.error("Erro ao processar retorno de transação pelo tópico {} para cadastro de recorrência", consumerRecord.topic(), e);
                throw new UncheckedIOException(e);
            }
        });
    }

}
