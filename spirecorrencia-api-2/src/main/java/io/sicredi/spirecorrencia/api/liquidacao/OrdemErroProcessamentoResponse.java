package io.sicredi.spirecorrencia.api.liquidacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
class OrdemErroProcessamentoResponse {
    private final String codigoErro;
    private final String mensagemErro;
    private String idFimAFim;

    public OrdemErroProcessamentoResponse(String codigoErro, String mensagemErro) {
        this.codigoErro = codigoErro;
        this.mensagemErro = mensagemErro;
    }

    public OrdemErroProcessamentoResponse adicionarIdFimAFim(String idFimAFim) {
        this.idFimAFim = idFimAFim;
        return this;
    }

}
