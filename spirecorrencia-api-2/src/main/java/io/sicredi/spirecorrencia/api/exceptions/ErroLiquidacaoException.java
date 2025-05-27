package io.sicredi.spirecorrencia.api.exceptions;

import br.com.sicredi.framework.exception.TechnicalException;

public class ErroLiquidacaoException extends TechnicalException {

    public ErroLiquidacaoException(String mensagemErro) {
        super(mensagemErro);
    }

    public ErroLiquidacaoException(String mensagemErro, Throwable cause) {
        super(mensagemErro, cause);
    }
}
