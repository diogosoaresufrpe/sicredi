package io.sicredi.spirecorrencia.api.notificacao;

import br.com.sicredi.framework.web.spring.exception.InternalServerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoProducer {

    private final AppConfig appConfig;
    private final ObjectMapper mapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void enviarNotificacao(NotificacaoDTO dto) {
        var topico = appConfig.getKafka().getProducer().getNotificacaoRecorrencia().getTopico();

        Message<String> message = this.construirMensagem(dto, topico);

        log.debug("Tentando enviar mensagem: {}. para o topico: {}", message.getPayload(), topico);
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(message);

        future.whenComplete((result, throwable) -> logarSucesso(result)).exceptionally(throwable -> {
            log.error("Falha no envio da mensagem {}. Erro: {}", message.getPayload(), throwable.getMessage(), throwable);
            throw new InternalServerException("Falha no envio da mensagem", throwable);
        });
    }

    private Message<String> construirMensagem(NotificacaoDTO dto, String topico) {
        var mensagem = this.escreverObjetoComoString(dto);
        var messageBuilder = MessageBuilder.withPayload(mensagem);
        messageBuilder.setHeader("kafka_topic", topico);
        return messageBuilder.build();
    }

    private String escreverObjetoComoString(NotificacaoDTO dto) {
        try {
            return mapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("Erro no writeValueAsString do objeto {}", dto, e);
            throw new InternalServerException("Não foi possível serializar a mensagem", e);
        }
    }

    private void logarSucesso(SendResult<String, String> result) {
        log.info("Sucesso no envio da mensagem {} para o topico {} ", result.getProducerRecord().value(), result.getProducerRecord().topic());
    }

}