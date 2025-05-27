package io.sicredi.spirecorrencia.api.exceptions;

import br.com.sicredi.framework.web.spring.exception.BadRequestException;

public class MensagemInvalidaException extends BadRequestException {
    public MensagemInvalidaException(String mensagemErro) {
        super(mensagemErro);
    }

    public MensagemInvalidaException(String mensagemErro, Throwable cause) {
        super(mensagemErro, cause);
    }

}
