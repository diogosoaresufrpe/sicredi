package io.sicredi.spirecorrencia.api.notificacao;

import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class NotificacaoProducerIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPIC = "spi-notificacao-recorrencia-v2";

    @Autowired
    private NotificacaoProducer producer;

    @Test
    void deveEnviarNotificacao() {
        Consumer<String, String> consumer = configurarConsumer("latest", Boolean.TRUE, TOPIC);
        NotificacaoDTO notificacaoDTO = NotificacaoDTO
                .builder()
                .agencia("0101")
                .chave("06037755000")
                .conta("001287")
                .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_VENCIMENTO_PROXIMO_DIA)
                .informacoesAdicionais(List.of())
                .build();

        producer.enviarNotificacao(notificacaoDTO);

        ConsumerRecord<String, String> mensagemConsumida = getLastRecord(consumer, TOPIC);

        String esperado = "{\"chave\":\"06037755000\",\"conta\":\"001287\",\"agencia\":\"0101\",\"operacao\":\"RECORRENCIA_VENCIMENTO_PROXIMO_DIA\",\"informacoesAdicionais\":{}}";

        assertEquals(TOPIC, mensagemConsumida.topic());
        assertEquals(esperado, mensagemConsumida.value());
    }

}
