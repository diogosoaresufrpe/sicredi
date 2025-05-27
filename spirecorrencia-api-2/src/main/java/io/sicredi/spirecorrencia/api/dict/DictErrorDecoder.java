package io.sicredi.spirecorrencia.api.dict;

import br.com.sicredi.framework.web.spring.exception.ExceptionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.utils.ProcessamentoRecorrenciaUtils;
import org.springframework.http.HttpStatus;

import java.util.Optional;

public class DictErrorDecoder implements ErrorDecoder {

    private static final String ERROR_PARSE_BODY = "Erro (%d) ao realizar parse do body da mensagem de erro";

    @Override
    public Exception decode(String s, Response response) {
        String responseBody;
        var detalheErro = String.format(ERROR_PARSE_BODY, response.status());
        try {
            responseBody = ProcessamentoRecorrenciaUtils.getResponseBodyAsString(response.body());
            var errorResponse = new ObjectMapper().readValue(responseBody, ExceptionResponse.class);
            var mensagem = String.format(AppExceptionCode.REC_PROC_BU0003.getMessage(), Optional.ofNullable(errorResponse.getCode()).orElse("") + " - " + Optional.ofNullable(errorResponse.getMessage()).orElse(detalheErro));
            return criarDictException(response.status(), AppExceptionCode.REC_PROC_BU0003.getCode(), mensagem);
        } catch (Exception ex) {
            var mensagem = String.format(AppExceptionCode.REC_PROC_BU0003.getMessage(), detalheErro);
            return criarDictException(response.status(), AppExceptionCode.REC_PROC_BU0003.getCode(), mensagem);
        }
    }

    private DictException criarDictException(int statusCode, String code, String mensagem) {
        return new DictException(HttpStatus.valueOf(statusCode), code, mensagem);
    }

}
