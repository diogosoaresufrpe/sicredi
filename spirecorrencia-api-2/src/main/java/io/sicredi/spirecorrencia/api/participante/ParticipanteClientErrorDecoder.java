package io.sicredi.spirecorrencia.api.participante;

import br.com.sicredi.framework.exception.BusinessException;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.*;
import feign.Response;
import feign.codec.ErrorDecoder;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.*;

@Slf4j
class ParticipanteClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        var httpStatus = HttpStatus.resolve(response.status());
        var errorResponse = criarExceptionResponse(response);

        if (httpStatus != null) {
            return switch (httpStatus) {
                case BAD_REQUEST -> criarBusinessException(errorResponse, SPIRECORRENCIA_PART0001);
                case NOT_FOUND -> criarBusinessException(errorResponse, SPIRECORRENCIA_PART0002);
                case UNPROCESSABLE_ENTITY -> criarBusinessException(errorResponse, SPIRECORRENCIA_PART0003);
                case INTERNAL_SERVER_ERROR -> criarBusinessException(errorResponse, SPIRECORRENCIA_PART0004);
                default -> criarBusinessException(errorResponse, SPIRECORRENCIA_PART0005);
            };
        }
        return criarBusinessException(errorResponse, SPIRECORRENCIA_PART0006);
    }

    private Exception criarBusinessException(ExceptionResponse errorResponse, AppExceptionCode exceptionCode) {
        log.error("'spi-participantes' retornou erro ({})", errorResponse);
        if (StringUtils.isNotBlank(errorResponse.getMessage())) {
            return new BusinessException(errorResponse.getMessage(), errorResponse.getCode(), exceptionCode.getError());
        }
        return new BusinessException(exceptionCode);
    }

    private ExceptionResponse criarExceptionResponse(Response response)  {
        try {
            var responseBody = response.body() == null ?
                StringUtils.EMPTY :
                IOUtils.toString(response.body().asReader(StandardCharsets.UTF_8));
            return ObjectMapperUtil.converterStringParaObjeto(responseBody, ExceptionResponse.class);
        } catch (Exception ex) {
            throw new TechnicalException(SPIRECORRENCIA_PART0006);
        }
    }
}