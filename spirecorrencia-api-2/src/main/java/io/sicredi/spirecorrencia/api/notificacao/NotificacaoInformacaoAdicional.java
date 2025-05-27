package io.sicredi.spirecorrencia.api.notificacao;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificacaoInformacaoAdicional {

    NOME_RECORRENCIA("nomeRecorrencia"),
    NOME_RECEBEDOR("nomeRecebedor"),
    DATA_EXPIRACAO_AUTORIZACAO("dataExpiracaoAutorizacao"),
    VALOR("valor"),
    DATA_PARCELA("dataParcela"),
    DOCUMENTO_PAGADOR("documento"),
    DOCUMENTO_RECEBEDOR("documentoRecebedor"),
    FREQUENCIA("frequencia"),
    QUANTIDADE_PARCELAS("quantidadeParcelas"),
    NOME_DEVEDOR("nomeDevedor"),
    OBJETO_PAGAMENTO("objetoDoPagamento"),;

    /**
     * Nome da variável definida no template da notificação.
     */
    private final String nomeVariavel;

}
