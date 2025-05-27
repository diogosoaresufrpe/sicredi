package io.sicredi.spirecorrencia.api.idempotente;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentEvent;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransactionService;
import io.sicredi.spirecorrencia.api.messasing.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotenteServiceTest {

    private static final String CHAVE_IDEMPOTENCIA = "chave-123";
    private static final String TOPICO = "meu-topico";
    private static final String OUTRO_TOPICO = "outro-topico";
    private static final String MENSAGEM_DE_TESTE = "mensagem-de-teste";
    private static final String HEADER_KEY = "key1";
    private static final String HEADER_VALUE = "valor1";

    @Mock
    IdempotentTransactionService idempotentTransactionService;

    @Mock
    MessageProducer messageProducer;

    @InjectMocks
    IdempotenteServiceImpl idempotenteService;

    @Captor
    ArgumentCaptor<String> valueCaptor;

    @Captor
    ArgumentCaptor<String> topicCaptor;

    @Captor
    ArgumentCaptor<Map<String, String>> headersCaptor;

    @Test
    @DisplayName("Enviando um tópico e uma chave de idempotência com evento existente, deve reenviar a operação com sucesso.")
    void dadoUmTopicoEUmaChaveIdempotenciaExistente_quandoReenviarOperacao_deveEnviarAOperacaoComSucesso() {
        // Arrange
        IdempotentEvent<String> evento = criarEvento(TOPICO, MENSAGEM_DE_TESTE, Map.of(HEADER_KEY, HEADER_VALUE));

        IdempotentResponse<String> idempotentResponse = IdempotentResponse.<String>builder()
                .value(null)
                .events(List.of(evento))
                .headers(Map.of())
                .cdc(false)
                .errorResponse(false)
                .build();

        // Usando thenAnswer para evitar problemas com wildcard
        when(idempotentTransactionService.findTransactionResponse(CHAVE_IDEMPOTENCIA))
                .thenAnswer(invocation -> idempotentResponse);

        // Act
        idempotenteService.reenviarOperacao(TOPICO, CHAVE_IDEMPOTENCIA);

        // Assert
        verify(messageProducer).enviar(valueCaptor.capture(), topicCaptor.capture(), headersCaptor.capture());
        assertEquals(MENSAGEM_DE_TESTE, valueCaptor.getValue());
        assertEquals(TOPICO, topicCaptor.getValue());
        assertEquals(Map.of(HEADER_KEY, HEADER_VALUE), headersCaptor.getValue());
    }

    @Test
    @DisplayName("Quando buscar um evento de um topico divergente e uma chave de idempotencia existente, não deve reenviar a operacao.")
    void dadoUmTopicoNaoExistenteEUmaChaveIdempotenciaExistente_quandoReenviarOperacao_deveNaoEnviarAOperacao() {

        // Arrange
        IdempotentEvent<String> evento = criarEvento(OUTRO_TOPICO, MENSAGEM_DE_TESTE, Map.of(HEADER_KEY, HEADER_VALUE));

        IdempotentResponse<String> idempotentResponse = IdempotentResponse.<String>builder()
                .value(null)
                .events(List.of(evento))
                .headers(Map.of())
                .cdc(false)
                .errorResponse(false)
                .build();

        when(idempotentTransactionService.findTransactionResponse(CHAVE_IDEMPOTENCIA))
                .thenAnswer(invocation -> idempotentResponse);

        // Act
        idempotenteService.reenviarOperacao(TOPICO, CHAVE_IDEMPOTENCIA);

        // Assert
        verify(messageProducer, never()).enviar(any(), any(), any());
    }

    @Test
    @DisplayName("Dado uma lista de eventos com apenas um evento com topico e chave de idempotencia existente, deve reenviar apenas uma operacao.")
    void dadoMultiplosEventosPoremComUmTopicoEUmaChaveIdempotenciaExistente_quandoReenviarOperacao_deveEnviarApenasUmaOperacao() {
        // Arrange
        IdempotentEvent<String> evento1 = criarEvento(OUTRO_TOPICO, "mensagem-1", Map.of("key1", "valor1"));
        IdempotentEvent<String> evento2 = criarEvento(TOPICO, MENSAGEM_DE_TESTE, Map.of(HEADER_KEY, HEADER_VALUE));
        IdempotentEvent<String> evento3 = criarEvento(OUTRO_TOPICO, "mensagem-3", Map.of("key3", "valor3"));

        IdempotentResponse<String> idempotentResponse = IdempotentResponse.<String>builder()
                .value(null)
                .events(List.of(evento1, evento2, evento3))
                .headers(Map.of())
                .cdc(false)
                .errorResponse(false)
                .build();

        when(idempotentTransactionService.findTransactionResponse(CHAVE_IDEMPOTENCIA))
                .thenAnswer(invocation -> idempotentResponse);

        // Act
        idempotenteService.reenviarOperacao(TOPICO, CHAVE_IDEMPOTENCIA);

        // Assert
        verify(messageProducer).enviar(valueCaptor.capture(), topicCaptor.capture(), headersCaptor.capture());
        assertEquals(MENSAGEM_DE_TESTE, valueCaptor.getValue());
        assertEquals(TOPICO, topicCaptor.getValue());
        assertEquals(Map.of(HEADER_KEY, HEADER_VALUE), headersCaptor.getValue());
    }

    @Test
    @DisplayName("Dado um tópico e chave de idempotência inexistente, não deve reenviar nenhuma operação.")
    void dadoUmTopicoEUmaChaveIdempotenciaInexistente_quandoReenviarOperacao_deveNaoEnviarNenhumaOperacao() {
        // Arrange
        IdempotentResponse<String> idempotentResponse = IdempotentResponse.<String>builder()
                .value(null)
                .events(List.of())
                .headers(Map.of())
                .cdc(false)
                .errorResponse(false)
                .build();

        when(idempotentTransactionService.findTransactionResponse(CHAVE_IDEMPOTENCIA))
                .thenAnswer(invocation -> idempotentResponse);

        // Act
        idempotenteService.reenviarOperacao(TOPICO, CHAVE_IDEMPOTENCIA);

        // Assert
        verify(messageProducer, never()).enviar(any(), any(), any());
    }

    private IdempotentEvent<String> criarEvento(String topico, String value, Map<String, String> headers) {
        return IdempotentEvent.<String>builder()
                .topic(topico)
                .value(value)
                .headers(headers)
                .build();
    }
}

