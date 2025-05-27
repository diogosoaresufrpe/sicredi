package io.sicredi.spirecorrencia.api.liquidacao;

public enum TipoProcessamentoEnum {
    /**
     * Indica que deve ser processada a liquidação da recorrência.
     */
    LIQUIDACAO,

    /**
     * Indica que deve ser processada a exclusão da recorrência e de todas as parcelas vinculadas à mesma.
     */
    EXCLUSAO_TOTAL,

    /**
     * Indica que deve ser processada a exclusão da recorrência e de apenas algumas das parcelas vinculadas à mesma.
     */
    EXCLUSAO_PARCIAL,

    /**
     * Indica que o processamento deve ser ignorado.
     */
    IGNORADA
}

