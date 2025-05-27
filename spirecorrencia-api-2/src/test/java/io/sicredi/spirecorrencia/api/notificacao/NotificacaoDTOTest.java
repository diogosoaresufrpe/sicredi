package io.sicredi.spirecorrencia.api.notificacao;

import br.com.sicredi.framework.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.DOCUMENTO_RECEBEDOR;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;

class NotificacaoDTOTest {

    private final ObjectMapper mapper = new ObjectMapper();

    public static Stream<Arguments> documentoRecebedorProvider() {
        return Stream.of(
                of("02418651150", "###.186.511-##"),
                of("07317858000123", "07.317.858/0001-23"),
                of("12ABC34501DE35", "12.ABC.345/01DE-35")
        );
    }

    @DisplayName("Dado DocumentoRecebedor ao criar informacoes adicionais deve formatar corretamente")
    @ParameterizedTest(name = "{index} => documentoRecebido={0}, documentoEsperado={1}")
    @MethodSource("documentoRecebedorProvider")
    void dadoInformacaoAdicionalDocumentoRecebedor_quandoBuildarNotificacaoDTO_deveFormatarDocumentoCorretamente(String documentoRecebido, String documentoEsperado) throws JsonProcessingException {
        final var variaveisNotificacao = List.of(
                NotificacaoDTO.InformacaoAdicional.of(DOCUMENTO_RECEBEDOR, documentoRecebido)
        );

        NotificacaoDTO notificacaoDTO = NotificacaoDTO.builder()
                .informacoesAdicionais(variaveisNotificacao)
                .build();

        String notificacaoString = mapper.writeValueAsString(notificacaoDTO);

        Assertions.assertTrue(notificacaoString.contains(documentoEsperado));
    }

    @Test
    @DisplayName("Dado InformacaoAdicional nao informada, quando buildar deve construir objeto corretamente")
    void dadoInformacaoAdicionalNaoInformada_quandoBuildarNotificacaoDTO_deveConstruirObjetoCorretamente() {
        assertDoesNotThrow(() -> NotificacaoDTO.builder()
                .canal("canal")
                .build());
    }

    @Test
    @DisplayName("Dado DocumentoRecebedor com tamanho invalido ao criar informacoes adicionais deve formatar corretamente")
    void dadoInformacaoAdicionalDocumentoRecebedorTamanhoInvalido_quandoBuildarNotificacaoDTO_deveFormatarDocumentoCorretamente() {
        final var variaveisNotificacao = List.of(
                NotificacaoDTO.InformacaoAdicional.of(DOCUMENTO_RECEBEDOR, "111")
        );

        BusinessException exceptionRecebida = assertThrows(BusinessException.class, () -> NotificacaoDTO.builder()
                .informacoesAdicionais(variaveisNotificacao)
                .build());

        assertEquals("Documento inválido", exceptionRecebida.getMessage());
    }

}