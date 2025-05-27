package io.sicredi.spirecorrencia.api.dict;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class DictException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String code;
    private final String mensagem;
}

