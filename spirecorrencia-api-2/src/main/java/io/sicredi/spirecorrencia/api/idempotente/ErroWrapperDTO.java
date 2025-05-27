package io.sicredi.spirecorrencia.api.idempotente;

import lombok.Getter;

import java.util.Optional;

@Getter
public class ErroWrapperDTO<T>  {

    private final T objeto;
    private final Optional<ErroDTO> erro;

    public ErroWrapperDTO(T objeto) {
        this.objeto = objeto;
        this.erro = Optional.empty();
    }

    public ErroWrapperDTO(ErroDTO erro) {
        this.objeto = null;
        this.erro = Optional.of(erro);
    }
}