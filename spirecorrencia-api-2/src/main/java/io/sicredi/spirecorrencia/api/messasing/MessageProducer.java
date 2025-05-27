package io.sicredi.spirecorrencia.api.messasing;

import io.sicredi.spirecorrencia.api.exceptions.MensagemInvalidaException;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public <T> void enviar(final T mensagem,
                           final String topico,
                           final Map<String, String> headers) {
        final var payload = this.parseJson(mensagem);
        final var message = buildMessage(headers, topico, payload);
        kafkaTemplate.send(message)
                .thenAccept(result -> onSuccess(result, topico, payload))
                .exceptionally(ex -> onFailure(ex, topico, payload));
    }

    private Message<String> buildMessage(final Map<String, String> headers,
                                         final String topico,
                                         final String payload) {
        final var messageBuilder = MessageBuilder.withPayload(payload);
        messageBuilder.setHeader(KafkaHeaders.TOPIC, topico);

        for (final var header : headers.entrySet()) {
            messageBuilder.setHeader(header.getKey(), header.getValue());
        }

        return messageBuilder.build();
    }

    private <T> String parseJson(final T object) {
        try {
            return ObjectMapperUtil.converterObjetoParaString(object, true);
        } catch (MensagemInvalidaException ex) {
            log.error("Erro ao serializar o objeto {}", object, ex);
            throw ex;
        }
    }

    private static void onSuccess(SendResult<String, ?> stringStringSendResult, String topico, String jsonMensagem) {
        log.debug("Mensagem enviada com sucesso para o tópico {}. Payload: {}. Result: {}", topico, jsonMensagem, stringStringSendResult);
    }

    private static Void onFailure(Throwable ex, String topico, String jsonMensagemDTO) {
        log.error("Falha ao enviar para o tópico {}. Erro: {}. Payload: {}", topico, ex.getMessage(), jsonMensagemDTO, ex);
        return null;
    }

}
