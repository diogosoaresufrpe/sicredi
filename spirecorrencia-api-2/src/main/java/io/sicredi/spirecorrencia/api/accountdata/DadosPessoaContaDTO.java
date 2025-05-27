package io.sicredi.spirecorrencia.api.accountdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record DadosPessoaContaDTO(

        @JsonProperty("name")
        String nome,

        @JsonProperty("socialName")
        String nomeSocial,

        @JsonProperty("documentNumber")
        String documento,

        @JsonProperty("relationType")
        TipoRelacaoEnum tipoRelacionamento
) {
}

