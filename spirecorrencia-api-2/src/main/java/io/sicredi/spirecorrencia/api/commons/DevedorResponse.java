package io.sicredi.spirecorrencia.api.commons;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Dados do devedor da recorrência")
public class DevedorResponse {
    @Schema(title = "Nome do devedor", example =  "SUPERMERCADO E ACOUGUE SAO JOSE")
    private String nome;

    @Schema(title = "CPF/CNPJ do devedor", example = "00248158023")
    private String cpfCnpj;

    public static DevedorResponse from(String nome, String cpfCnpj) {
        return DevedorResponse.builder()
                .nome(nome)
                .cpfCnpj(cpfCnpj)
                .build();
    }
}


