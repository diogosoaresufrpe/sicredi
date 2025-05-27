package io.sicredi.spirecorrencia.api.exceptions.decoder;

import br.com.sicredi.framework.web.spring.exception.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DefaultErrorDecoderTest {

    private static final String CHAVE_METODO = "chaveMetodo";
    private final DefaultErrorDecoder decoder = new DefaultErrorDecoder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void dadoStatus401_quandoDecodificarErro_deveLancarUnauthorizedException() throws Exception {
        var response = criarResposta(401, jsonExcecao("Acesso não autorizado", "401X"));
        var excecao = decoder.decode(CHAVE_METODO, response);
        assertEquals(UnauthorizedException.class, excecao.getClass());
        assertEquals("Acesso não autorizado", excecao.getMessage());
    }

    @Test
    void dadoStatus403_quandoDecodificarErro_deveLancarUnauthorizedException() throws Exception {
        var response = criarResposta(403, jsonExcecao("Acesso proibido", "403X"));
        var excecao = decoder.decode(CHAVE_METODO, response);
        assertEquals(UnauthorizedException.class, excecao.getClass());
        assertEquals("Acesso proibido", excecao.getMessage());
    }

    @Test
    void dadoStatus400_quandoDecodificarErro_deveLancarBadRequestException() throws Exception {
        var response = criarResposta(400, jsonExcecao("Requisição inválida", "400X"));
        var excecao = decoder.decode(CHAVE_METODO, response);
        assertEquals(BadRequestException.class, excecao.getClass());
        assertEquals("Requisição inválida", excecao.getMessage());
    }

    @Test
    void dadoStatus404_quandoDecodificarErro_deveLancarNotFoundException() throws Exception {
        var response = criarResposta(404, jsonExcecao("Recurso não encontrado", "404X"));
        var excecao = decoder.decode(CHAVE_METODO, response);
        assertEquals(NotFoundException.class, excecao.getClass());
        assertEquals("Recurso não encontrado", excecao.getMessage());
    }

    @Test
    void dadoStatus422_quandoDecodificarErro_deveLancarUnprocessableEntityException() throws Exception {
        var response = criarResposta(422, jsonExcecao("Dados inválidos", "422X"));
        var excecao = decoder.decode(CHAVE_METODO, response);
        assertEquals(UnprocessableEntityException.class, excecao.getClass());
        assertEquals("Dados inválidos", excecao.getMessage());
    }

    @Test
    void dadoStatus500_quandoDecodificarErro_deveLancarInternalServerException() throws Exception {
        var response = criarResposta(500, jsonExcecao("Erro interno no servidor", "500X"));
        var excecao = decoder.decode(CHAVE_METODO, response);
        assertEquals(InternalServerException.class, excecao.getClass());
        assertEquals("Erro interno no servidor", excecao.getMessage());
    }

    @Test
    void dadoStatusDesconhecido_quandoDecodificarErro_deveLancarServiceUnavailableException() {
        var response = criarResposta(599, "{\"message\":\"Erro desconhecido\",\"code\":\"599X\"}");
        var excecao = decoder.decode(CHAVE_METODO, response);
        assertEquals(ServiceUnavailableException.class, excecao.getClass());
        assertEquals("Erro desconhecido", excecao.getMessage());
    }

    private Response criarResposta(int status, String corpoJson) {
        return Response.builder()
                .status(status)
                .reason("Erro Simulado")
                .request(Request.create(Request.HttpMethod.GET, "teste", Map.of(), null, null, null))
                .body(corpoJson, StandardCharsets.UTF_8)
                .build();
    }

    private String jsonExcecao(String mensagem, String codigo) throws JsonProcessingException {
        return objectMapper.writeValueAsString(
                ExceptionResponse.builder()
                        .message(mensagem)
                        .code(codigo)
                        .build()
        );
    }
}
