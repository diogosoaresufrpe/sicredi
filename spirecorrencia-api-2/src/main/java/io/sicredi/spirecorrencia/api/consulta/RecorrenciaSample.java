package io.sicredi.spirecorrencia.api.consulta;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RecorrenciaSample {

    public static final String EXEMPLO_RESPONSE_LISTAGEM_RECORRENCIAS = """
        {
            "recorrencias": [
                {
                    "identificadorRecorrencia": "2E1A553EBB9461EDE063C50A17AC6551",
                    "tipoRecorrencia": "AUTOMATICO_RECORRENTE",
                    "nome": "Recorrência Fictícia",
                    "tipoStatus": "CRIADO",
                    "valorProximoPagamento": 10,
                    "dataCriacao": "2025-02-14 10:23:01",
                    "dataProximoPagamento": "2025-02-15",
                    "recebedor": {
                        "nome": "Nome Fictício"
                    }
                },
                {
                    "identificadorRecorrencia": "2E1A553EBBB961EDE063C50A17AC6551",
                    "tipoRecorrencia": "AUTOMATICO_RECORRENTE",
                    "nome": "Recorrência Fictícia",
                    "tipoStatus": "CRIADO",
                    "valorProximoPagamento": 10,
                    "dataCriacao": "2025-02-14 10:23:26",
                    "dataProximoPagamento": "2025-02-15",
                    "recebedor": {
                        "nome": "Nome Fictício"
                    }
                }
            ],
            "paginacao": {
                "totalPaginas": 46,
                "tamanhoPagina": 2,
                "numeroPagina": 0,
                "numeroElementos": 2,
                "numeroTotalElementos": 92,
                "ultimaPagina": false,
                "primeiraPagina": true,
                "existeProximaPagina": true,
                "existePaginaAnterior": false,
                "paginaVazia": false
            }
        }
        """;

    public static final String EXEMPLO_RESPONSE_DETALHE_RECORRENCIA = """
        {
            "identificadorRecorrencia": "2E1A553EBB9461EDE063C50A17AC6551",
            "tipoRecorrencia": "AGENDADO_RECORRENTE",
            "nome": "Recorrência Fictícia",
            "tipoFrequencia": "MENSAL",
            "tipoStatus": "CRIADO",
            "valorProximoPagamento": 10,
            "numeroTotalParcelas": 3,
            "dataCriacao": "2025-02-14 10:23:01",
            "dataProximoPagamento": "2025-02-15",
            "dataPrimeiraParcela": "2025-02-15",
            "dataUltimaParcela": "2025-04-16",
            "recebedor": {
                "nome": "Nome Fictício",
                "chave": "email@exemplo.com",
                "agencia": "1234",
                "conta": "567890"
            },
            "pagador": {
                "nome": "JOAO CARLOS",
                "instituicao": "01181521",
                "agencia": "0101",
                "conta": "00027",
                "tipoConta": "CONTA_CORRENTE",
                "cpfCnpj": "00248158023"
            },
            "parcelas": [
                {
                  "identificadorParcela": "2E1A553EBB9561EDE063C50A17AC6551",
                  "idFimAFim": "E9158698220250325161553Vzd1rbmDM",
                  "valor": 10,
                  "numeroParcela": 1,
                  "status": "CRIADO",
                  "dataTransacao": "2025-02-15"
                },
                {
                  "identificadorParcela": "2E1A553EBB9661EDE063C50A17AC6551",
                  "idFimAFim": "E9158698220250325161553Vzd1rbmDM",
                  "valor": 10,
                  "numeroParcela": 2,
                  "status": "CRIADO",
                  "dataTransacao": "2025-03-17"
                },
                {
                  "identificadorParcela": "2E1A553EBB9761EDE063C50A17AC6551",
                  "idFimAFim": "E9158698220250325161553Vzd1rbmDM",
                  "valor": 10,
                  "numeroParcela": 3,
                  "status": "CRIADO",
                  "dataTransacao": "2025-04-16"
                }
            ]
        }
        """;

    public static final String EXEMPLO_RESPONSE_LISTAGEM_PARCELAS_RECORRENCIA = """
        {
            "parcelas": [
                {
                    "identificadorParcela": "2E83EC2A134CB3FAE063C50A17AC3074",
                    "identificadorRecorrencia": "2E83EC2A134CB3FAE063C50A17AC3074",
                    "valor": 10,
                    "dataTransacao": "2025-02-15",
                    "dataExclusao": "2025-02-20 10:54:18",
                    "status": "CRIADO",
                    "tipoRecorrencia": "AUTOMATICO_RECORRENTE",
                    "recebedor": {
                        "nome": "Nome Fictício"
                    }
                },
                {
                    "identificadorParcela": "2E1F4427DF7137BBE063C50A17AC8F1F",
                    "identificadorRecorrencia": "2E1F4427DF7137BBE063C50A17AC8F1F",
                    "valor": 10,
                    "dataTransacao": "2025-02-15",
                    "status": "CRIADO",
                    "tipoRecorrencia": "AUTOMATICO_RECORRENTE",
                    "recebedor": {
                        "nome": "Nome Fictício"
                    }
                }
            ],
            "paginacao": {
                "totalPaginas": 1746,
                "tamanhoPagina": 2,
                "numeroPagina": 0,
                "numeroElementos": 2,
                "numeroTotalElementos": 3492,
                "ultimaPagina": false,
                "primeiraPagina": true,
                "existeProximaPagina": true,
                "existePaginaAnterior": false,
                "paginaVazia": false
            }
        };
    """;

    public static final String EXEMPLO_RESPONSE_DETALHES_PARCELAS_RECORRENCIA_CRIADO = """
        {
            "identificadorRecorrencia": "2E1F4427E73E37BBE063C50A17AC8F1F",
            "nome": "Recorrência Fictícia",
            "tipoRecorrencia": "AUTOMATICO_RECORRENTE",
            "tipoFrequencia": "MENSAL",
            "tipoIniciacaoCanal": "CHAVE",
            "tipoCanal": "MOBI",
            "numeroTotalParcelas": 36,
            "recebedor": {
              "cpfCnpj": "12345678901",
              "nome": "Nome Fictício",
              "instituicao": "0001",
              "tipoChave": "EMAIL",
              "chave": "email@exemplo.com",
              "agencia": "1234",
              "conta": "567890"
            },
            "parcela": {
              "identificadorParcela": "2E1F4427E75B37BBE063C50A17AC8F1F",
              "idFimAFim": "E9999900420250214152328fEbHlgpAT",
              "valor": 10,
              "numeroParcela": 29,
              "status": "CRIADO",
              "dataTransacao": "2027-06-05",
              "informacoesEntreUsuarios": "Informações Complementares",
              "idConciliacaoRecebedor": "identificador",
              "dataCriacaoRegistro": "2025-02-14 15:23:28"
            },
            "pagador": {
              "nome": "Adelia Maria Moschem Colorio",
              "instituicao": "99999004",
              "agencia": "0101",
              "conta": "000023",
              "tipoConta": "CONTA_CORRENTE",
              "cpfCnpj": "24110287000170"
            }
          }
    """;

    public static final String EXEMPLO_RESPONSE_DETALHES_PARCELAS_RECORRENCIA_CANCELADO = """
        {
             "identificadorRecorrencia": "2E1F4427E73E37BBE063C50A17AC8F1F",
             "nome": "Recorrência Fictícia",
             "tipoRecorrencia": "AUTOMATICO_RECORRENTE",
             "tipoFrequencia": "MENSAL",
             "tipoIniciacaoCanal": "CHAVE",
             "tipoCanal": "MOBI",
             "numeroTotalParcelas": 36,
             "recebedor": {
               "cpfCnpj": "12345678901",
               "nome": "Nome Fictício",
               "instituicao": "0001",
               "tipoChave": "EMAIL",
               "chave": "email@exemplo.com",
               "agencia": "1234",
               "conta": "567890"
             },
             "parcela": {
               "identificadorParcela": "2E1F4427E75C37BBE063C50A17AC8F1F",
               "idFimAFim": "E9999900420250214152328cDZlMyKRS",
               "valor": 10,
               "numeroParcela": 30,
               "status": "EXCLUIDO",
               "dataTransacao": "2027-07-05",
               "dataExclusao": "2025-03-13 09:52:21",
               "informacoesEntreUsuarios": "Informações Complementares",
               "idConciliacaoRecebedor": "identificador",
               "dataCriacaoRegistro": "2025-02-14 15:23:28"
             },
             "pagador": {
               "nome": "Adelia Maria Moschem Colorio",
               "instituicao": "99999004",
               "agencia": "0101",
               "conta": "000023",
               "tipoConta": "CONTA_CORRENTE",
               "cpfCnpj": "24110287000170"
             }
           }
    """;
}
