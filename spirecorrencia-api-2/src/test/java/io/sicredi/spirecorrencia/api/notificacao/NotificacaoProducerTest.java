package io.sicredi.spirecorrencia.api.notificacao;

import br.com.sicredi.framework.web.spring.exception.InternalServerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoProducerTest {

    public static final String TOPICO = "TOPICO";

    public static final NotificacaoDTO NOTIFICACAO_DTO = NotificacaoDTO
            .builder()
            .agencia("0101")
            .chave("06037755000")
            .conta("001287")
            .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_VENCIMENTO_PROXIMO_DIA)
            .informacoesAdicionais(
                    List.of(
                            of(NotificacaoInformacaoAdicional.NOME_RECORRENCIA, "value")
                    )
            )
            .build();

    @Mock
    private AppConfig appConfig;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificacaoProducer notificacaoProducer;

    @BeforeEach
    void setup() {
        AppConfig.Kafka kafka = Mockito.mock(AppConfig.Kafka.class);
        AppConfig.Kafka.Producer appConfigProducer = Mockito.mock(AppConfig.Kafka.Producer.class);
        AppConfig.Kafka.Producer.ProducerProps producerProps = Mockito.mock(AppConfig.Kafka.Producer.ProducerProps.class);
        when(appConfig.getKafka()).thenReturn(kafka);
        when(kafka.getProducer()).thenReturn(appConfigProducer);
        when(appConfigProducer.getNotificacaoRecorrencia()).thenReturn(producerProps);
        when(producerProps.getTopico()).thenReturn(TOPICO);
    }

    @Test
    void deveLancarInternalServerExceptionQuandoWriteValueAsStringLancarExcecao() throws JsonProcessingException {
        doThrow(JsonProcessingException.class).when(objectMapper).writeValueAsString(any());

        final var exception = assertThrows(
                InternalServerException.class,
                () -> notificacaoProducer.enviarNotificacao(NOTIFICACAO_DTO)
        );

        assertEquals(InternalServerException.class, exception.getClass());

        verify(objectMapper).writeValueAsString(NOTIFICACAO_DTO);
        verify(kafkaTemplate, never()).send(any(Message.class));
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void deveLancarInternalServerExceptionQuandoKafkaTemplateEnviarComFalha(CapturedOutput captureOutput) throws JsonProcessingException {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();

        when(kafkaTemplate.send(any(Message.class))).thenReturn(future);
        when(objectMapper.writeValueAsString(any())).thenReturn(NOTIFICACAO_DTO.toString());

        notificacaoProducer.enviarNotificacao(NOTIFICACAO_DTO);

        future.completeExceptionally(new InternalServerException("erro ao enviar mensagem"));

        assertTrue(captureOutput.getAll().contains("br.com.sicredi.framework.web.spring.exception.InternalServerException: erro ao enviar mensagem"));

        verify(objectMapper).writeValueAsString(NOTIFICACAO_DTO);
        verify(kafkaTemplate).send(any(Message.class));
    }

}