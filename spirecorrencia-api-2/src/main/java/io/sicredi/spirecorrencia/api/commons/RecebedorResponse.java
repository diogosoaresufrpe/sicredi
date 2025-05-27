package io.sicredi.spirecorrencia.api.commons;


import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.repositorio.Recebedor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Dados do recebedor da recorrência")
public class RecebedorResponse {

    @Schema(title = "CPF/CNPJ do recebedor", example = "00248158023")
    private String cpfCnpj;

    @Schema(title = "Nome do recebedor", example =  "SUPERMERCADO E ACOUGUE SAO JOSE")
    private String nome;

    @Schema(title = "Instituição Financeira do recebedor (ISPB)", example = "91586982")
    private String instituicao;

    @Schema(title =  "Tipo de Pessoa", example = "PJ", implementation = TipoPessoaEnum.class, enumAsRef = true)
    private TipoPessoaEnum tipoPessoa;

    @Schema(title = "Tipo de Chave", example = "EMAIL", implementation = TipoChaveEnum.class, enumAsRef = true)
    private TipoChaveEnum tipoChave;

    @Schema(title = "Chave Pix", example = "pix@sicredi.com.br")
    private String chave;

    @Schema(title = "Agência do recebedor", example = "0101")
    private String agencia;

    @Schema(title = "Conta do recebedor", example = "052124")
    private String conta;

    @Schema(title = "Tipo de conta do recebedor", example = "CONTA_CORRENTE")
    private TipoContaEnum tipoConta;

    public static RecebedorResponse fromDetalhesRecorrencia(Recebedor recebedor) {
        return Optional.ofNullable(recebedor)
                .map(recebedorFilter -> RecebedorResponse.builder()
                        .nome(recebedorFilter.getNome())
                        .cpfCnpj(recebedorFilter.getCpfCnpj())
                        .instituicao(recebedorFilter.getInstituicao())
                        .chave(recebedorFilter.getChave())
                        .agencia(recebedorFilter.getAgencia())
                        .conta(recebedorFilter.getConta())
                        .tipoChave(recebedorFilter.getTipoChave())
                        .build()
                ).orElse(null);
    }

    public static RecebedorResponse fromDetalhesParcelas(Recebedor recebedor) {
        return Optional.ofNullable(recebedor)
                .map(recebedorFilter -> RecebedorResponse.builder()
                        .nome(recebedorFilter.getNome())
                        .chave(recebedorFilter.getChave())
                        .agencia(recebedorFilter.getAgencia())
                        .cpfCnpj(recebedorFilter.getCpfCnpj())
                        .conta(recebedorFilter.getConta())
                        .instituicao(recebedorFilter.getInstituicao())
                        .tipoChave(recebedorFilter.getTipoChave())
                        .build()
                ).orElse(null);
    }

    public static RecebedorResponse fromNomeRecebedor(String nomeRecebedor) {
        return Optional.ofNullable(nomeRecebedor)
                .map(nome -> RecebedorResponse.builder()
                        .nome(nome)
                        .build()
                ).orElse(null);
    }

    public static RecebedorResponse from(String nome, String cpfCnpj) {
        return RecebedorResponse.builder()
                .nome(nome)
                .cpfCnpj(cpfCnpj)
                .build();
    }
}
