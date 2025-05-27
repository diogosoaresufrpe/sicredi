package io.sicredi.spirecorrencia.api.accountdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record DadosContaResponseDTO(

        @JsonProperty("company")
        String coop,
        @JsonProperty("branch")
        String posto,
        @JsonProperty("number")
        String numeroConta,
        @JsonProperty("source")
        String sistema,
        String status,
        @JsonProperty("product")
        TipoProdutoEnum tipoProduto,
        @JsonProperty("type")
        TipoContaEnum tipoConta,
        @JsonProperty("holder")
        DadosPessoaContaDTO titular,
        @JsonProperty("persons")
        List<DadosPessoaContaDTO> pessoas,
        @JsonProperty("active")
        boolean ativo,
        @JsonProperty("hasCreditLock")
        boolean temCreditoBloqueado,
        @JsonProperty("hasDebitLock")
        boolean temDebitoBloqueado,
        @JsonProperty("migrated")
        boolean migrado

) {
}
