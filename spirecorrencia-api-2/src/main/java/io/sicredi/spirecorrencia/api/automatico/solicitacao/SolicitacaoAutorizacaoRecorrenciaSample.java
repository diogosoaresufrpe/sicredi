package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import lombok.experimental.UtilityClass;

@UtilityClass
class SolicitacaoAutorizacaoRecorrenciaSample {

    public static final String EXEMPLO_RESPONSE_LISTAGEM_SOLICITACOES_AUTORIZACAO = """
            {
              "solicitacoes": [
                {
                  "idSolicitacaoRecorrencia": "AS12123AsdaaSDASD",
                  "nomeRecebedor": "Xis do Rafa",
                  "dataExpiracao": "2025-05-10T12:15:46",
                  "contrato": "21312331-1",
                  "descricao": "Parcela Academia",
                  "tpoStatus":"CONFIRMADA",
                  "tpoSubStatus":"AGUARDANDO_ENVIO"
                }
              ],
              "paginacao": {
                "totalPaginas": 1,
                "tamanhoPagina": 1,
                "numeroPagina": 0,
                "numeroElementos": 1,
                "numeroTotalElementos": 1,
                "ultimaPagina": true,
                "primeiraPagina": true,
                "existeProximaPagina": false,
                "existePaginaAnterior": false,
                "paginaVazia": false
              }
            }
            """;
    public static final String EXEMPLO_RESPONSE_DETALHE_SOLICITACAO_AUTORIZACAO = """
            {
                "idSolicitacaoRecorrencia" : "ABC123321ABC",
                "idRecorrencia" : "CBA321123CBA",
                "tpoStatus " : "CONFIRMADA",
                "tpoSubStatus " : "AGUARDANDO_ENVIO",
                "tpoFrequencia" : "MENSAL",
                "valor " : 0,
                "pisoValorMaximo " : 0,
                "dataPrimeiroPagamento" : "2025-05-10T12:15:46",
                "dataUltimoPagamento" : "2026-05-10T12:15:46",
                "contrato " : "123321",
                "descricao" : "Lorem ipsum",
                "recebedor" : {
                    "nome" : "João",
                    "cpfCnpj" : "000.000.000-00"
                },
                "devedor" : {
                    "nome" : "Maria",
                    "cpfCnpj" : "999.999.999-99"
                },
                "pagador" : {
                    "nome" : "Chico",
                    "cpfCnpj" : "123.123.123-12"
                }
            }
            """;
}