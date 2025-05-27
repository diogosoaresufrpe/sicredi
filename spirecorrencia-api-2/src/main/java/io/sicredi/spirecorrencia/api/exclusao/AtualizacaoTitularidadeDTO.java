package io.sicredi.spirecorrencia.api.exclusao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ChavesMdc;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
record AtualizacaoTitularidadeDTO(
        @JsonProperty("operationId")
        String idOperacao,

        @JsonProperty("account")
        AccountDTO dadosConta,

        @JsonProperty("currentHolders")
        List<HolderDTO> titularesAtuais,

        @JsonProperty("holdersToUpdate")
        List<HolderToUpdateDTO> titularesParaAtualizar) {

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccountDTO(
            @JsonProperty("branch")
            String posto,

            @JsonProperty("company")
            String cooperativa,

            @JsonProperty("number")
            String conta,

            @JsonProperty("type")
            String tipo,

            @JsonProperty("status")
            String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HolderDTO(
            @JsonProperty("suid")
            String cpfCnpj,

            @JsonProperty("name")
            String nome,

            @JsonProperty("mainHolder")
            Boolean titularPrincipal
    ) {
    }

    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    record HolderToUpdateDTO(
            @JsonProperty("suid")
            String cpfCnpj,

            @JsonProperty("name")
            String nome,

            @JsonProperty("customerId")
            String idAssociado,

            @JsonProperty("action")
            String acao,

            @JsonProperty("finished")
            boolean concluido
    ) {
    }

    @Getter
    @AllArgsConstructor
    enum HoldersUpdateEventMdc implements ChavesMdc {
        OPERATION_ID("operationId"),
        CUSTOMER_ID("customerId");

        private final String chave;

    }

}

