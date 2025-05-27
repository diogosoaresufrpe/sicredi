-- ************************************************************************************************************************************************************************************************************
-- Inicio Cenário 1: Cancelamento com status e 'ACEITA'
-- ************************************************************************************************************************************************************************************************************
INSERT INTO SPI_OWNER.RECORRENCIA_AUTORIZACAO_CANCELAMENTO (
    ID_INFORMACAO_CANCELAMENTO, ID_RECORRENCIA, ID_INFORMACAO_STATUS, TPO_CANCELAMENTO,
    TPO_PSP_SOLICITANTE_CANCELAMENTO, TPO_STATUS, NUM_CPF_CNPJ_SOLICITANTE_CANCELAMENTO,
    COD_MOTIVO_CANCELAMENTO, DAT_CANCELAMENTO
) VALUES (
     'IC0118152120250425041bYqAj6ef', 'RN4118152120250425041bYqAj6ef', 'IS0118152120250425041bYqAj6ef',
     'RECORRENCIA_AUTORIZACAO', 'PAGADOR', 'CRIADA', '12345678901', 'AP01', CURRENT_TIMESTAMP
);
