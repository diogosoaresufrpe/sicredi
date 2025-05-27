package io.sicredi.spirecorrencia.api.participante;

import br.com.sicredi.framework.exception.BusinessException;
import br.com.sicredi.framework.exception.TechnicalException;
import feign.Response;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.Reader;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipanteClientErrorDecoderTest {

    private final ParticipanteClientErrorDecoder participanteClientErrorDecoder = new ParticipanteClientErrorDecoder();

    @Mock
    private Response.Body body;
    @Mock
    private Response response;

    @ParameterizedTest(name = "Status {0} -> Resultado =  {1}")
    @MethodSource("exceptionResponseProvider")
    void dadoStatusEExceptionCode_quandoDecode_deveRetornarBusinessException(HttpStatus status, AppExceptionCode exceptionCode) throws IOException {
        when(body.asReader(any())).thenReturn(mock(Reader.class));
        when(response.status()).thenReturn(status.value());
        when(response.body()).thenReturn(body);

        try (var mockedStatic = mockStatic(IOUtils.class)) {
            mockedStatic.when(() -> IOUtils.toString(any(Reader.class))).thenReturn("{\"code\": \"ERR001\",\"message\": \"The provided input is invalid.\"}");

            var exception = (BusinessException) participanteClientErrorDecoder.decode("", response);

            assertAll(
                    () -> assertEquals("The provided input is invalid.", exception.getMessage()),
                    () -> assertEquals("ERR001", exception.getCode().get()),
                    () -> assertEquals(exceptionCode.getCode(), exception.getError().get())
            );
        }
    }

    @Test
    void dadoPayloadInvalido_quandoDecode_deveRetornarTechnicalException() throws IOException {
        when(body.asReader(any())).thenReturn(mock(Reader.class));
        when(response.status()).thenReturn(500);
        when(response.body()).thenReturn(body);

        try (var mockedStatic = mockStatic(IOUtils.class)) {
            mockedStatic.when(() -> IOUtils.toString(any(Reader.class))).thenReturn(null);

            var exception = assertThrows(TechnicalException.class, () -> participanteClientErrorDecoder.decode("", response));

            assertEquals(AppExceptionCode.SPIRECORRENCIA_PART0006.getCode(), exception.getError().get());
        }
    }

    private static Stream<Arguments> exceptionResponseProvider() {
        return Stream.of(
                Arguments.of(HttpStatus.BAD_REQUEST, AppExceptionCode.SPIRECORRENCIA_PART0001),
                Arguments.of(HttpStatus.NOT_FOUND, AppExceptionCode.SPIRECORRENCIA_PART0002),
                Arguments.of(HttpStatus.UNPROCESSABLE_ENTITY, AppExceptionCode.SPIRECORRENCIA_PART0003),
                Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, AppExceptionCode.SPIRECORRENCIA_PART0004),
                Arguments.of(HttpStatus.NOT_IMPLEMENTED, AppExceptionCode.SPIRECORRENCIA_PART0005)
        );
    }
}