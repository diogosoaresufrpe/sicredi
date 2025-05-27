package io.sicredi.spirecorrencia.api.exceptions.decoder;

import br.com.sicredi.framework.web.spring.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class DefaultErrorDecoder implements ErrorDecoder {
    private static final String FORMATO_MSG_LOG = "%s retornou erro (%s)";

    @Override
    public Exception decode(String methodKey, Response response) {
        var httpStatus = HttpStatus.resolve(response.status());
        if (httpStatus != null) {
            return switch (httpStatus) {
                case UNAUTHORIZED, FORBIDDEN -> criarResponseUnauthorized(response);
                case BAD_REQUEST -> criarResponseBadRequest(response);
                case NOT_FOUND -> criarResponseNotFound(response);
                case UNPROCESSABLE_ENTITY -> criarResponseUnprocessableEntity(response);
                case INTERNAL_SERVER_ERROR -> criarResponseInternalServerError(response);
                default -> criarResponseErroGenerico(response);
            };
        }
        return criarResponseErroGenerico(response);
    }

    private UnauthorizedException criarResponseUnauthorized(Response response) {
        var exceptionErro = criarResponseException(response);
        log.error(exceptionErro.getMensagemLog());
        if (StringUtils.isNotBlank(exceptionErro.getMessage())) {
            return new UnauthorizedException(exceptionErro.getMessage(), exceptionErro.getCode());
        }
        return new UnauthorizedException("Não possui permissão para acessar o recurso.");
    }

    private BadRequestException criarResponseBadRequest(Response response) {
        var exceptionErro = criarResponseException(response);
        log.error(exceptionErro.getMensagemLog());
        if (StringUtils.isNotBlank(exceptionErro.getMessage())) {
            return new BadRequestException(exceptionErro.getMessage(), exceptionErro.getCode());
        }
        return new BadRequestException("Dados da requisição inválidos.");
    }

    private NotFoundException criarResponseNotFound(Response response) {
        var exceptionErro = criarResponseException(response);
        log.error(exceptionErro.getMensagemLog());
        if (StringUtils.isNotBlank(exceptionErro.getMessage())) {
            return new NotFoundException(exceptionErro.getMessage(), exceptionErro.getCode());
        }
        return new NotFoundException("Não foi possível localizar o registro.");
    }

    private UnprocessableEntityException criarResponseUnprocessableEntity(Response response) {
        var exceptionErro = criarResponseException(response);
        log.error(exceptionErro.getMensagemLog());
        if (StringUtils.isNotBlank(exceptionErro.getMessage())) {
            return new UnprocessableEntityException(exceptionErro.getMessage(), exceptionErro.getCode());
        }
        return new UnprocessableEntityException("Erro de negócio.");
    }

    private InternalServerException criarResponseInternalServerError(Response response) {
        var exceptionErro = criarResponseException(response);
        log.error(exceptionErro.getMensagemLog());
        if (StringUtils.isNotBlank(exceptionErro.getMessage())) {
            return new InternalServerException(exceptionErro.getMessage(), exceptionErro.getCode());
        }
        return new InternalServerException("Erro interno no servidor.");
    }

    private ServiceUnavailableException criarResponseErroGenerico(Response response) {
        var exceptionErro = criarResponseException(response);
        log.error(exceptionErro.getMensagemLog());
        if (StringUtils.isNotBlank(exceptionErro.getMessage())) {
            return new ServiceUnavailableException(exceptionErro.getMessage(), exceptionErro.getCode());
        }
        return new ServiceUnavailableException("Serviço não disponível.");
    }

    private ExceptionResponse criarResponseException(Response response) {
        var responseBody = criarBodyErro(response);
        try {
            var errorResponse = new ObjectMapper().readValue(responseBody, ExceptionResponse.class);
            if (errorResponse.getMessage().isEmpty()) {
                return ExceptionResponse.builder()
                        .mensagemLog(criarMensagemLog(response, responseBody))
                        .build();
            }
            var mensagem = criarMensagemLog(response, responseBody);
            errorResponse.setMensagemLog(mensagem);
            return errorResponse;
        } catch (Exception ex) {
            return ExceptionResponse.builder()
                    .mensagemLog(criarMensagemLog(response, responseBody))
                    .build();
        }
    }

    private String criarMensagemLog(Response response, String responseBody) {
        return String.format(FORMATO_MSG_LOG, this.criarNomeAPI(response.request().url()), responseBody);
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
}
