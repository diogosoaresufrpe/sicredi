package io.sicredi.spirecorrencia.api.commons;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Dados do pagador da recorrência")
public class PagadorResponse {

    @Schema(title = "Nome do pagador", example =  "SUPERMERCADO E ACOUGUE SAO JOSE")
    private String nome;

    @Schema(title = "Instituição Financeira do pagador (ISPB)", example = "91586982")
    private String instituicao;

    @Schema(title = "Agência do pagador", example = "0101")
    private String agencia;

    @Schema(title = "Conta do pagador", example = "052124")
    private String conta;

    @Schema(title = "Tipo de conta do pagador", example = "CONTA_CORRENTE")
    private TipoContaEnum tipoConta;

    @Schema(title = "CPF/CNPJ do pagador", example = "00248158023")
    private String cpfCnpj;

    public static PagadorResponse fromDetalhesParcelas(Pagador pagador) {
        return Optional.ofNullable(pagador)
                .map(pagadorFilter -> PagadorResponse.builder()
                        .nome(pagadorFilter.getNome())
                        .agencia(pagadorFilter.getAgencia())
                        .instituicao(pagadorFilter.getInstituicao())
                        .cpfCnpj(pagadorFilter.getCpfCnpj())
                        .conta(pagadorFilter.getConta())
                        .tipoConta(pagadorFilter.getTipoConta())
                        .build()
                ).orElse(null);
    }

    public static PagadorResponse from(String nome, String cpfCnpj) {
        return PagadorResponse.builder()
                .nome(nome)
                .cpfCnpj(cpfCnpj)
                .build();
    }
}
