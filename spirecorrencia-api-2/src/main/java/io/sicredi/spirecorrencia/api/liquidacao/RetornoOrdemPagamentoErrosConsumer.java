package io.sicredi.spirecorrencia.api.liquidacao;

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
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
class RetornoOrdemPagamentoErrosConsumer {

    private static final String TIPO_PRODUTO_AGENDADO_RECORRENTE = "AGENDADO_RECORRENTE";
    public static final String TIPO_PRODUTO_AGENDADO_RECORRENTE_COBV2 = "AGENDADO_RECORRENTE_COBV2";
    public static final String TIPO_PRODUTO_AGENDADO_RECORRENTE_AGENTE = "AGENDADO_RECORRENTE_AGENTE";

    @SuppressWarnings("unused")
    private final AppConfig appConfig;
    private final ObjectMapper mapper;
    private final LiquidacaoService liquidacaoService;
    private final ObservabilidadeDecorator observabilidadeDecorator;

    private final Set<String> tiposProdutosAgendados = new HashSet<>(Arrays.asList(
            TIPO_PRODUTO_AGENDADO_RECORRENTE,
            TIPO_PRODUTO_AGENDADO_RECORRENTE_COBV2,
            TIPO_PRODUTO_AGENDADO_RECORRENTE_AGENTE));

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.tratamentoErroLiquidacaoOrdemPagamento.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.tratamentoErroLiquidacaoOrdemPagamento.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            timeout = "#{@appConfig.kafka.consumer.tratamentoErroLiquidacaoOrdemPagamento.retry.timeout}",
            retryTopicSuffix = "-spirecorrencia-api-v1-retry",
            dltStrategy = DltStrategy.NO_DLT
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.tratamentoErroLiquidacaoOrdemPagamento.nome}",
            concurrency = "#{@appConfig.kafka.consumer.tratamentoErroLiquidacaoOrdemPagamento.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.tratamentoErroLiquidacaoOrdemPagamento.groupId}",
            containerFactory = "protocoloKafkaListenerContainerFactory"
    )
    public void consumirRetornoOrdemPagamentoErro(
            ConsumerRecord<String, String> consumerRecord,
            @Header(value = "tipoProduto", required = false) String tipoProduto,
            @Header("idFimAFim") String idFimAFim,
            Acknowledgment acknowledgment
    ) {
        var identificadorTransacao = UUID.randomUUID().toString();
        var atributos = Map.of(
                RecorrenciaMdc.ID_FIM_A_FIM, idFimAFim,
                RecorrenciaMdc.IDENTIFICADOR_TRANSACAO, identificadorTransacao
        );
        observabilidadeDecorator.executar(atributos, () -> {
            try {
                if (!tiposProdutosAgendados.contains(tipoProduto)) {
                    log.trace("Ignorando processamento de retorno de erro devido ao tipo de produto não ser aceito. Tipo de produto aceito: {}, Tipo de produto informado: {}", TIPO_PRODUTO_AGENDADO_RECORRENTE, tipoProduto);
                    acknowledgment.acknowledge();
                    return;
                }

                log.debug("Inicio do processamento de retorno de erro.");
                var ordemErroProcessamentoResponse = mapper.readValue(consumerRecord.value(), OrdemErroProcessamentoResponse.class)
                        .adicionarIdFimAFim(idFimAFim);

                liquidacaoService.atualizaRecorrenciaLiquidacaoDaTransacaoComErro(identificadorTransacao, ordemErroProcessamentoResponse);
                log.debug("Fim do processamento de retorno de erro.");
                acknowledgment.acknowledge();
            } catch (JsonProcessingException e) {
                log.error("Erro ao converter mensagem de transações pix em dto, pelo tópico {}", consumerRecord.topic(), e);

                throw new UncheckedIOException(e);
            }
        });
    }
}
