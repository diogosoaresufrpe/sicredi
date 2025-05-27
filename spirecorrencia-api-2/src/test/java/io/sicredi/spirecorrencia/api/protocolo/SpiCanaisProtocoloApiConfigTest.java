package io.sicredi.spirecorrencia.api.protocolo;

import br.com.sicredi.framework.exception.CoreException;
import br.com.sicredi.framework.web.spring.exception.*;
import feign.Request;
import feign.Response;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class SpiCanaisProtocoloApiConfigTest {

    private SpiCanaisProtocoloApiConfig spiCanaisProtocoloApiConfig = new SpiCanaisProtocoloApiConfig();

    @Mock
    private Response response;

    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("statusMapeados")
    @DisplayName("Dado response com status code mapeado, deve retornar exception tratada")
    void dadoStatusMapeado_quandoDecode_deveRetornarTratamento(int status, Class<? extends CoreException> clazz, AppExceptionCode appExceptionCode) {
        var request = mock(Request.class);
        when(response.status()).thenReturn(status);
        when(response.request()).thenReturn(request);
        when(request.url()).thenReturn("teste.unitario");

        var retorno = (CoreException) spiCanaisProtocoloApiConfig.decode("", response);

        assertAll(
                () -> assertEquals(clazz, retorno.getClass()),
                () -> assertEquals(appExceptionCode.getCode(), retorno.getCode().get()),
                () -> assertEquals(appExceptionCode.getError(), retorno.getError().get()),
                () -> assertEquals(appExceptionCode.getMessage(), retorno.getMessage())
        );
    }


    @ParameterizedTest(name = "{0} - {2}")
    @MethodSource("statusNaoMapeados")
    @DisplayName("Dado response com status code não mapeado, deve retornar exception generica")
    void dadoStatusNaoMapeado_quandoDecode_deveRetornarGenerico(int status, Class<? extends CoreException> clazz, AppExceptionCode appExceptionCode) {
        var request = mock(Request.class);
        when(response.status()).thenReturn(status);
        when(response.request()).thenReturn(request);
        when(request.url()).thenReturn("teste.unitario");

        var retorno = (CoreException) spiCanaisProtocoloApiConfig.decode("", response);

        assertAll(
                () -> assertEquals(clazz, retorno.getClass()),
                () -> assertEquals(appExceptionCode.getCode(), retorno.getCode().get()),
                () -> assertEquals(appExceptionCode.getError(), retorno.getError().get()),
                () -> assertEquals(appExceptionCode.getMessage(), retorno.getMessage())
        );
    }

    static Stream<Arguments> statusNaoMapeados(){
        var errors = Set.of(Series.CLIENT_ERROR, Series.SERVER_ERROR);
        var mapeado = Set.of(UNAUTHORIZED, BAD_REQUEST, NOT_FOUND, UNPROCESSABLE_ENTITY, METHOD_FAILURE, INTERNAL_SERVER_ERROR);
        return Stream.of(HttpStatus.values())
                .filter(httpStatus ->  errors.contains(httpStatus.series()))
                .filter(httpStatus ->  !mapeado.contains(httpStatus))
                .map(httpStatus -> Arguments.of(httpStatus.value(), ServiceUnavailableException.class, AppExceptionCode.SPIRECORRENCIA_PROT006));
    }

    static Stream<Arguments> statusMapeados(){
        return Stream.of(
                Arguments.of(UNAUTHORIZED.value(), UnauthorizedException.class, AppExceptionCode.SPIRECORRENCIA_PROT001),
                Arguments.of(BAD_REQUEST.value(), BadRequestException.class, AppExceptionCode.SPIRECORRENCIA_PROT002),
                Arguments.of(NOT_FOUND.value(), NotFoundException.class, AppExceptionCode.SPIRECORRENCIA_PROT003),
                Arguments.of(UNPROCESSABLE_ENTITY.value(), UnprocessableEntityException.class, AppExceptionCode.SPIRECORRENCIA_PROT004),
                Arguments.of(METHOD_FAILURE.value(), UnprocessableEntityException.class, AppExceptionCode.SPIRECORRENCIA_PROT004),
                Arguments.of(INTERNAL_SERVER_ERROR.value(), InternalServerException.class, AppExceptionCode.SPIRECORRENCIA_PROT005)
        );
    }
}