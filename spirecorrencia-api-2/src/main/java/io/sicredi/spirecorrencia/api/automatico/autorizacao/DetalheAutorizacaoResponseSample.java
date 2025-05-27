package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import lombok.experimental.UtilityClass;

@UtilityClass
class DetalheAutorizacaoResponseSample {

    public static final String EXEMPLO_RESPONSE_LISTAGEM_AUTORIZACOES = """
            {
                 "oidRecorrenciaAutorizacao": 1,
                 "idRecorrencia" : "RR9999900420250513EdaacDKLONY",
                 "nomeRecebedor": "Empresa XYZ Ltda",
                 "dataCriacao": "2025-07-18T13:30:00",
                 "contrato": "12331223-1",
                 "descricao": "Parcela Psiquiatra",
                 "tpoStatus": "APROVADA",
                 "tpoJornada": "JORNADA_2",
                 "tpoFrequencia": "MENSAL",
                 "valor": 0,
                 "pisoValorMaximo": 50,
                 "valorMaximo": 100,
                 "permiteLinhaCredito": true,
                 "permiteNotificacaoAgendamento": true,
                 "permiteRetentativa": true,
                 "dataInicialRecorrenica": "2025-07-18",
                 "dataFinalRecorrencia": "2025-12-18",
                 "recebedor": {
                     "nome": "Empresa XYZ Ltda",
                     "cpfCnpj": "14848398000125"
                 },
                 "devedor": {
                     "nome": "Nome Devedor",
                     "cpfCnpj": "17098313084"
                 },
                 "pagador": {
                     "nome": "Fernando Marinho",
                     "cpfCnpj": "40111443040"
                 }
             }
            """;
}