package io.sicredi.spirecorrencia.api.utils;

import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ChavesMdc;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum RecorrenciaMdc implements ChavesMdc {

    ID_FIM_A_FIM("idFimAFim"),
    ID_PARCELA("idParcela"),
    AGENCIA_PAGADOR("agenciaPagador"),
    CONTA_PAGADOR("contaPagador"),
    IDENTIFICADOR_RECORRENCIA("identificadorRecorrencia"),
    ID_PROTOCOLO("idProtocolo"),
    ADMIN_IDEMPOTENTE("adminIdempotente"),
    IDENTIFICADOR_PARCELA("identificadorParcela"),
    NOME_JOB("nomeJob"),
    OID_RECORRENCIA_PAGADOR("oidRecorrenciaPagador"),
    IDENTIFICADOR_TRANSACAO("identificadorTransacao"),
    IDENTIFICADOR_TRANSACAO_INTEGRADO("identificadorTransacaoIntegrado"),
    ID_RECORRENCIA("idRecorrencia"),
    OID_RECORRENCIA_AUTORIZACAO("oidRecorrenciaAutorizacao"),
    ID_SOLICITACAO_RECORRENCIA("idSolicitacaoRecorrencia"),
    ID_INFORMACAO_CANCELAMENTO("idInformacaoCancelamento"),
    ID_IDEMPOTENCIA("idIdempotencia"),
    OPERACAO_AUTOMATICO("operacao"),
    TIPO_MENSAGEM("tipoMensagem"),
    ID_CANCELAMENTO_AGENDAMENTO("idCancelamentoAgendamento");

    private final String chave;

    @Override
    public String getChave() {
        return this.chave;
    }
}
