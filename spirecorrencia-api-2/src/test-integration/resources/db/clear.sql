delete from SPI_OWNER.SHEDLOCK;
delete from SPI_OWNER.RECORRENCIA_TRANSACAO_TENT;
delete from SPI_OWNER.RECORRENCIA_TRANSACAO;
delete from SPI_OWNER.RECORRENCIA;
delete from SPI_OWNER.RECORRENCIA_RECEBEDOR;
delete from SPI_OWNER.RECORRENCIA_PAGADOR;
delete from SPI_OWNER.RECORRENCIA_AUTORIZACAO;
delete from SPI_OWNER.RECORRENCIA_AUTORIZACAO_SOLICITACAO;
delete from SPI_OWNER.IDEMPOTENT_TRANSACTION_OUTBOX_RECORRENCIA;
delete from SPI_OWNER.IDEMPOTENT_TRANSACTION_RECORRENCIA;
delete from SPI_OWNER.RECORRENCIA_AUTORIZACAO_CANCELAMENTO;
delete from SPI_OWNER.RECORRENCIA_AUTORIZACAO_CICLO;
delete from SPI_OWNER.RECORRENCIA_INSTRUCAO_PAGAMENTO;
delete from SPI_OWNER.RECORRENCIA_INSTRUCAO_PAGAMENTO_CANCELAMENTO;
ALTER TABLE SPI_OWNER.RECORRENCIA_AUTORIZACAO MODIFY OID_RECORRENCIA_AUTORIZACAO NUMBER(19) GENERATED BY DEFAULT AS IDENTITY (START WITH 1);