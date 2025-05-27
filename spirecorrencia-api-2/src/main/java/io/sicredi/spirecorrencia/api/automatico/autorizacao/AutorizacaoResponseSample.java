package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import lombok.experimental.UtilityClass;

@UtilityClass
class AutorizacaoResponseSample {

    public static final String EXEMPLO_RESPONSE_LISTAGEM_AUTORIZACOES = """
            {
              "autorizacoes": [
                {
                  "oidRecorrenciaAutorizacao": "1",
                  "nomeRecebedor": "Enzo Gabriel",
                  "dataCriacao": "2025-07-18T13:30:00",
                  "contrato": "12331223-1",
                  "descricao": "Parcela Psiquiatra",
                  "tpoStatus": "CRIADA",
                  "tpoSubStatus": "AGUARDANDO_RETORNO"
                }
              ],
              "paginacao": {
                "totalPaginas": 1,
                "tamanhoPagina": 10,
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
}