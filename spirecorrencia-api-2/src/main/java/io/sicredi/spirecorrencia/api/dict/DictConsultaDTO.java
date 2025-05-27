package io.sicredi.spirecorrencia.api.dict;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import lombok.Data;

@Data
public class DictConsultaDTO {

    private String agencia;
    private String conta;
    private String chave;
    private String cpfCnpj;
    private String endToEndBacen;
    private String instituicao;
    private String nome;
    private String status;
    private String tipoChave;
    private TipoContaDictEnum tipoConta;
    private TipoPessoaEnum tipoPessoa;
}
