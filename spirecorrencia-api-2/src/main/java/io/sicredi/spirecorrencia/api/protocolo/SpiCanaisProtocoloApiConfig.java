package io.sicredi.spirecorrencia.api.protocolo;

import br.com.sicredi.framework.exception.CoreException;
import br.com.sicredi.framework.exception.ExceptionError;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
class SpiCanaisProtocoloApiConfig implements ErrorDecoder {

    @Override
    public Exception decode(String s, Response response) {
        var httpStatus = HttpStatus.resolve(response.status());
        if (httpStatus != null) {
            return switch (httpStatus) {
                case UNAUTHORIZED ->
                        criarResponse(response, UnauthorizedException.class, AppExceptionCode.SPIRECORRENCIA_PROT001);
                case BAD_REQUEST ->
                        criarResponse(response, BadRequestException.class, AppExceptionCode.SPIRECORRENCIA_PROT002);
                case NOT_FOUND ->
                        criarResponse(response, NotFoundException.class, AppExceptionCode.SPIRECORRENCIA_PROT003);
                case METHOD_FAILURE, UNPROCESSABLE_ENTITY ->
                        criarResponse(response, UnprocessableEntityException.class, AppExceptionCode.SPIRECORRENCIA_PROT004);
                case INTERNAL_SERVER_ERROR ->
                        criarResponse(response, InternalServerException.class, AppExceptionCode.SPIRECORRENCIA_PROT005);
                default ->
                        criarResponse(response, ServiceUnavailableException.class, AppExceptionCode.SPIRECORRENCIA_PROT006);
            };
        }
        return criarResponse(response, ServiceUnavailableException.class, AppExceptionCode.SPIRECORRENCIA_PROT999);
    }

    private <T extends CoreException> T criarResponse(Response response, Class<T> exceptionClass, AppExceptionCode appExceptionCode) {
        var exceptionErro = criarExceptionResponse(response);
        log.error(exceptionErro.getMensagemLog());
        try {
            if (StringUtils.isNotBlank(exceptionErro.getMessage())) {
                return exceptionClass.getConstructor(String.class, String.class, String.class)
                        .newInstance(exceptionErro.getMessage(), exceptionErro.getCode(), appExceptionCode.getCode());
            }
            return exceptionClass.getConstructor(ExceptionError.class).newInstance(appExceptionCode);
        } catch (ReflectiveOperationException e) {
            throw new TechnicalException("Erro ao criar a exceção", e);
        }
    }

    private ExceptionResponse criarExceptionResponse(Response response) {
        var responseBody = criarBodyErro(response);
        try {
            var errorResponse = new ObjectMapper().readValue(responseBody, ExceptionResponse.class);
            if (StringUtils.isBlank(errorResponse.getMessage())) {
                return ExceptionResponse.builder()
                        .mensagemLog(criarMensagemLog(response, responseBody))
                        .build();
            }
            var mensagemLog = criarMensagemLog(response, responseBody);
            errorResponse.setMensagemLog(mensagemLog);
            return errorResponse;
        } catch (Exception ex) {
            return ExceptionResponse.builder()
                    .mensagemLog(criarMensagemLog(response, responseBody))
                    .build();
        }
    }

    private String criarMensagemLog(Response response, String responseBody) {
        return String.format("%s retornou erro (%s)",
                this.criarNomeAPI(response.request().url()), responseBody);
    }

    private String criarBodyErro(Response response) {
        var erro = StringUtils.EMPTY;

        try {
            erro = converterBodyErro(response.body());
        } catch (IOException e) {
            erro = response.toString();
        }

        return erro;
    }

    private String converterBodyErro(final Response.Body body) throws IOException {
        return body == null ?
                StringUtils.EMPTY :
                IOUtils.toString(body.asReader(StandardCharsets.UTF_8));
    }

    private String criarNomeAPI(String url) {
        try {
            return new URI(url).getHost();
        } catch (URISyntaxException var3) {
            return url;
        }
    }

    @Data
    @Builder
    static class ExceptionResponse {
        private String code;
        private String message;
        private String mensagemLog;
    }
}
