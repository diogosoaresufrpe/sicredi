package io.sicredi.spirecorrencia.api.idempotente;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;

import java.util.List;

public record ErroDTO(AppExceptionCode codigoErro, String mensagemErro, TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum, List<EventoResponseDTO> listaEventos) {

    public ErroDTO(AppExceptionCode codigoErro, String mensagemErro, TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum) {
        this(codigoErro, mensagemErro, tipoRetornoTransacaoEnum, List.of());
    }

    public ErroDTO(AppExceptionCode codigoErro, TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum) {
        this(codigoErro, codigoErro.getMessage(), tipoRetornoTransacaoEnum, List.of());
    }

}