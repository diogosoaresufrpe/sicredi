-- ************************************************************************************************************************************************************************************************************
-- Inicio Cenário 1: Solicitação com status 'PENDENTE_CONFIRMACAO' e 'ACEITA' com o mesmo ID RECORRENCIA
-- ************************************************************************************************************************************************************************************************************
INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO (ID_SOLICITACAO_RECORRENCIA, ID_RECORRENCIA,
                                                           ID_INFORMACAO_STATUS, TPO_STATUS,
                                                           TPO_FREQUENCIA, NUM_CPF_CNPJ_PAGADOR, TXT_NOME_PAGADOR,
                                                           NUM_AGENCIA_PAGADOR,
                                                           NUM_CONTA_PAGADOR, NUM_INSTITUICAO_PAGADOR,
                                                           COD_POSTO_PAGADOR, TPO_CONTA_PAGADOR,
                                                           TPO_PESSOA_PAGADOR, TXT_NOME_RECEBEDOR,
                                                           NUM_CPF_CNPJ_RECEBEDOR, NUM_INSTITUICAO_RECEBEDOR,
                                                           NUM_CONTRATO, COD_MUN_IBGE, DAT_INICIAL_RECORRENCIA,
                                                           DAT_CRIACAO_RECORRENCIA,
                                                           DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO, TXT_NOME_DEVEDOR, NUM_VALOR, TPO_SISTEMA_PAGADOR)
VALUES ('SC0118152120250425041bYqAj6ef', 'RN0118152120250425041bYqAj6ef', 'IS0118152120250425041bYqAj6ef',
        'PENDENTE_CONFIRMACAO',
        'MENSAL', '00248158023', 'Nair Hoffmann Jung', '0101',
        '003039', '12345678', '9999', 'CONTA_CORRENTE',
        'PF', 'Recebedor Teste', '99887766554433', '87654321',
        'CONTRATO123', '123456', SYSDATE, SYSTIMESTAMP,
        SYSTIMESTAMP + INTERVAL '2' DAY, 'Nome Devedor', 2, 'LEGADO');

-- Cenário 1: ACEITA
INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO (ID_SOLICITACAO_RECORRENCIA, ID_RECORRENCIA,
                                                           ID_INFORMACAO_STATUS, TPO_STATUS,
                                                           TPO_FREQUENCIA, NUM_CPF_CNPJ_PAGADOR, TXT_NOME_PAGADOR,
                                                           NUM_AGENCIA_PAGADOR,
                                                           NUM_CONTA_PAGADOR, NUM_INSTITUICAO_PAGADOR,
                                                           COD_POSTO_PAGADOR, TPO_CONTA_PAGADOR,
                                                           TPO_PESSOA_PAGADOR, TXT_NOME_RECEBEDOR,
                                                           NUM_CPF_CNPJ_RECEBEDOR, NUM_INSTITUICAO_RECEBEDOR,
                                                           NUM_CONTRATO, COD_MUN_IBGE, DAT_INICIAL_RECORRENCIA,
                                                           DAT_CRIACAO_RECORRENCIA,
                                                           DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO, TXT_NOME_DEVEDOR, NUM_VALOR, TPO_SISTEMA_PAGADOR)
VALUES ('SC4118152120250425041bYqAj6ef', 'RN0118152120250425041bYqAj6ef', 'IS0218152120250425041bYqAj6ef', 'ACEITA',
        'MENSAL', '00248158023', 'Nair Hoffmann Jung', '02',
        '003039', '12345678', '9999', 'CONTA_CORRENTE',
        'PF', 'Recebedor Teste', '99887766554433', '87654321',
        'CONTRATO123', '123456', SYSDATE, SYSTIMESTAMP,
        SYSTIMESTAMP + INTERVAL '2' DAY, 'Nome Devedor', 2, 'LEGADO');

-- ************************************************************************************************************************************************************************************************************
-- Inicio Cenário 2: Solicitação com status diferente de 'PENDENTE_CONFIRMACAO' e 'ACEITA' com o mesmo ID RECORRENCIA
-- ************************************************************************************************************************************************************************************************************

INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO (
    ID_SOLICITACAO_RECORRENCIA, ID_RECORRENCIA, ID_INFORMACAO_STATUS, TPO_STATUS,
    TPO_FREQUENCIA, NUM_CPF_CNPJ_PAGADOR, TXT_NOME_PAGADOR, NUM_AGENCIA_PAGADOR,
    NUM_CONTA_PAGADOR, NUM_INSTITUICAO_PAGADOR, COD_POSTO_PAGADOR, TPO_CONTA_PAGADOR,
    TPO_PESSOA_PAGADOR, TXT_NOME_RECEBEDOR, NUM_CPF_CNPJ_RECEBEDOR, NUM_INSTITUICAO_RECEBEDOR,
    NUM_CONTRATO, COD_MUN_IBGE, DAT_INICIAL_RECORRENCIA, DAT_CRIACAO_RECORRENCIA,
    DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO, TXT_NOME_DEVEDOR, NUM_VALOR, TPO_SISTEMA_PAGADOR, TPO_SUB_STATUS
) VALUES (
             'SC5118152120250425041bYqAj6ef', 'RN2118152120250425041bYqAj6ef', 'IS01181521202504250416YqAj6ef', 'CRIADA',
             'MENSAL', '00248158023', 'Nair Hoffmann Jung', '0101',
             '003039', '12345678', '9999', 'CONTA_CORRENTE',
             'PF', 'Recebedor Teste', '99887766554433', '87654321',
             'CONTRATO123', '123456', SYSDATE, SYSTIMESTAMP,
             SYSTIMESTAMP + INTERVAL '2' DAY, 'Nome Devedor', 2, 'LEGADO', 'AGUARDANDO_ENVIO');

-- ************************************************************************************************************************************************************************************************************
-- Inicio Cenário 3: Solicitação com status confirmada com ID_RECORRENCIA diferente
-- ************************************************************************************************************************************************************************************************************

INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO (
    ID_SOLICITACAO_RECORRENCIA, ID_RECORRENCIA, ID_INFORMACAO_STATUS, TPO_STATUS,
    TPO_FREQUENCIA, NUM_CPF_CNPJ_PAGADOR, TXT_NOME_PAGADOR, NUM_AGENCIA_PAGADOR,
    NUM_CONTA_PAGADOR, NUM_INSTITUICAO_PAGADOR, COD_POSTO_PAGADOR, TPO_CONTA_PAGADOR,
    TPO_PESSOA_PAGADOR, TXT_NOME_RECEBEDOR, NUM_CPF_CNPJ_RECEBEDOR, NUM_INSTITUICAO_RECEBEDOR,
    NUM_CONTRATO, COD_MUN_IBGE, DAT_INICIAL_RECORRENCIA, DAT_CRIACAO_RECORRENCIA,
    DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO, TXT_NOME_DEVEDOR, NUM_VALOR, TPO_SISTEMA_PAGADOR, TPO_SUB_STATUS
) VALUES (
             'SC8118152120250425041bYqAj6ef', 'RN0118152120250425041bYqAj6ef', 'IS11181521202504250416YqAj6ef', 'CONFIRMADA',
             'MENSAL', '00248158023', 'Nair Hoffmann Jung', '0101',
             '003039', '12345678', '9999', 'CONTA_CORRENTE',
             'PF', 'Recebedor Teste', '99887766554433', '87654321',
             'CONTRATO123', '123456', SYSDATE, SYSTIMESTAMP,
             SYSTIMESTAMP + INTERVAL '2' DAY, 'Nome Devedor', 2, 'LEGADO', 'AGUARDANDO_ENVIO');

INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO (
    ID_SOLICITACAO_RECORRENCIA, ID_RECORRENCIA, ID_INFORMACAO_STATUS, TPO_STATUS,
    TPO_FREQUENCIA, NUM_CPF_CNPJ_PAGADOR, TXT_NOME_PAGADOR, NUM_AGENCIA_PAGADOR,
    NUM_CONTA_PAGADOR, NUM_INSTITUICAO_PAGADOR, COD_POSTO_PAGADOR, TPO_CONTA_PAGADOR,
    TPO_PESSOA_PAGADOR, TXT_NOME_RECEBEDOR, NUM_CPF_CNPJ_RECEBEDOR, NUM_INSTITUICAO_RECEBEDOR,
    NUM_CONTRATO, COD_MUN_IBGE, DAT_INICIAL_RECORRENCIA, DAT_CRIACAO_RECORRENCIA,
    DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO, TXT_NOME_DEVEDOR, NUM_VALOR, TPO_SISTEMA_PAGADOR, TPO_SUB_STATUS
) VALUES (
             'SC7118152120250425041bYqAj6ef', 'RN3118152120250425041bYqAj6ef', 'IS11181521202504250416YqAj6ef', 'CONFIRMADA',
             'MENSAL', '00248158023', 'Nair Hoffmann Jung', '0101',
             '003039', '12345678', '9999', 'CONTA_CORRENTE',
             'PF', 'Recebedor Teste', '99887766554433', '87654321',
             'CONTRATO123', '123456', SYSDATE, SYSTIMESTAMP,
             SYSTIMESTAMP + INTERVAL '2' DAY, 'Nome Devedor', 2, 'LEGADO', 'AGUARDANDO_ENVIO');

INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO (
    ID_SOLICITACAO_RECORRENCIA, ID_RECORRENCIA, ID_INFORMACAO_STATUS, TPO_STATUS,
    TPO_FREQUENCIA, NUM_CPF_CNPJ_PAGADOR, TXT_NOME_PAGADOR, NUM_AGENCIA_PAGADOR,
    NUM_CONTA_PAGADOR, NUM_INSTITUICAO_PAGADOR, COD_POSTO_PAGADOR, TPO_CONTA_PAGADOR,
    TPO_PESSOA_PAGADOR, TXT_NOME_RECEBEDOR, NUM_CPF_CNPJ_RECEBEDOR, NUM_INSTITUICAO_RECEBEDOR,
    NUM_CONTRATO, COD_MUN_IBGE, DAT_INICIAL_RECORRENCIA, DAT_CRIACAO_RECORRENCIA,
    DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO, TXT_NOME_DEVEDOR, NUM_VALOR, TPO_SISTEMA_PAGADOR, TPO_SUB_STATUS
) VALUES (
             'SC9118152120250425041bYqAj6ef', 'RN1118152120250425041bYqAj6ef', 'IS11181521202504250416YqAj6ef', 'CONFIRMADA',
             'MENSAL', '00248158023', 'Nair Hoffmann Jung', '0101',
             '003039', '12345678', '9999', 'CONTA_CORRENTE',
             'PF', 'Recebedor Teste', '99887766554433', '87654321',
             'CONTRATO123', '123456', SYSDATE, SYSTIMESTAMP,
             SYSTIMESTAMP + INTERVAL '2' DAY, 'Nome Devedor', 2, 'LEGADO','AGUARDANDO_RETORNO');

-- ************************************************************************************************************************************************************************************************************
-- Inicio Cenário 4: Confirmação de solicitação de autorização
-- ************************************************************************************************************************************************************************************************************

INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO ( ID_SOLICITACAO_RECORRENCIA, ID_RECORRENCIA, ID_INFORMACAO_STATUS,
    TPO_STATUS, TPO_SUB_STATUS, TPO_FREQUENCIA, NUM_VALOR, NUM_PISO_VALOR_MAXIMO, NUM_CPF_CNPJ_PAGADOR, TXT_NOME_PAGADOR,
    NUM_AGENCIA_PAGADOR, NUM_CONTA_PAGADOR, NUM_INSTITUICAO_PAGADOR, COD_POSTO_PAGADOR, TPO_CONTA_PAGADOR, TPO_PESSOA_PAGADOR,
    TPO_SISTEMA_PAGADOR, TXT_NOME_RECEBEDOR, NUM_CPF_CNPJ_RECEBEDOR, NUM_INSTITUICAO_RECEBEDOR, TXT_NOME_DEVEDOR,
    NUM_CPF_CNPJ_DEVEDOR, NUM_CONTRATO, TXT_DESCRICAO, COD_MOTIVO_REJEICAO, COD_MUN_IBGE, DAT_INICIAL_RECORRENCIA, DAT_FINAL_RECORRENCIA,
    DAT_CRIACAO_RECORRENCIA, DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO, DAT_INICIO_CONFIRMACAO, DAT_CRIACAO_REGISTRO, DAT_ALTERACAO_REGISTRO
) VALUES (
             'SC0118152120250425041bYqAj6gg', 'idRecorrencia1', 'teste1', 'PENDENTE_CONFIRMACAO', NULL, 'TESTE', null, 20, '12690422115',
             'Empresa XYZ LTDA', '0101', '223190', '00714671', '12', 'CHECKING_ACCOUNT', 'PF', 'LEGADO', 'João da Silva', '12345678901',
             '341BANCO', 'Empresa XYZ LTDA', '98765432000199', 'CT-2025-0001', 'teste', NULL, '4305108', TIMESTAMP'2025-05-01 00:00:00',
             TIMESTAMP'2025-12-01 00:00:00', TIMESTAMP'2025-07-18 13:00:00', TIMESTAMP'2025-07-18 15:00:00', TIMESTAMP'2025-07-18 12:00:00',
             TIMESTAMP'2025-05-05 17:43:43.5242', TIMESTAMP'2025-05-05 17:43:43.522687'
         ),
         (
            'SC0118152120250425041bYqAj6ff', 'idRecorrencia2', 'teste2', 'PENDENTE_CONFIRMACAO', NULL, 'TESTE', null, 20, '12690422115',
            'Empresa XYZ LTDA', '0101', '223190', '00714671', '12', 'CHECKING_ACCOUNT', 'PF', 'LEGADO', 'João da Silva', '12345678901',
            '341BANCO', 'Empresa XYZ LTDA', '98765432000199', 'CT-2025-0001', 'teste', NULL, '4305108', TIMESTAMP'2025-05-01 00:00:00',
            TIMESTAMP'2025-12-01 00:00:00', TIMESTAMP'2025-07-18 13:00:00', TIMESTAMP'2025-07-18 15:00:00', TIMESTAMP'2025-07-18 12:00:00',
            TIMESTAMP'2025-05-05 17:43:43.5242', TIMESTAMP'2025-05-05 17:43:43.522687'
         ),
         (
            'SC0118152120250425041bYqAj6hh', 'rec-123456', 'teste123', 'PENDENTE_CONFIRMACAO', NULL, 'TESTE', 20.75, 20, '12690422115',
            'Empresa XYZ LTDA', '0101', '223190', '00714671', '12', 'CHECKING_ACCOUNT', 'PF', 'LEGADO', 'João da Silva', '12345678901',
            '341BANCO', 'Empresa XYZ LTDA', '98765432000199', 'CT-2025-0001', 'teste', NULL, '4305108', TIMESTAMP'2025-05-01 00:00:00',
            TIMESTAMP'2025-12-01 00:00:00', TIMESTAMP'2025-07-18 13:00:00', TIMESTAMP'2025-07-18 15:00:00', TIMESTAMP'2025-07-18 12:00:00',
            TIMESTAMP'2025-05-05 17:43:43.5242', TIMESTAMP'2025-05-05 17:43:43.522687'
   )
;