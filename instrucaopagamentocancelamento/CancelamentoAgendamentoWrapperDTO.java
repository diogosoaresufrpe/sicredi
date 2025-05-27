package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class CancelamentoAgendamentoWrapperDTO {
    private final RecorrenciaInstrucaoPagamento instrucaoPagamento;
    private final  String idCancelamentoAgendamento;
    private final String cpfCnpjSolicitante;
    private final MotivoCancelamentoCamt55 motivoCancelamentoCamt55;
    private final LocalDateTime dataHoraSolicitacaoCancelamento;

    public static CancelamentoAgendamentoWrapperDTO fromEmissaoProtocolo(RecorrenciaInstrucaoPagamento instrucaoPagamento, String idCancelamentoAgendamento, String cpfCnpjSolicitante, MotivoCancelamentoCamt55 motivoCancelamentoCamt55, LocalDateTime dataHoraSolicitacaoCancelamento){
        return new CancelamentoAgendamentoWrapperDTO(instrucaoPagamento, idCancelamentoAgendamento, cpfCnpjSolicitante, motivoCancelamentoCamt55, dataHoraSolicitacaoCancelamento);
    }

    public static CancelamentoAgendamentoWrapperDTO fromCancelamentoInterno(RecorrenciaInstrucaoPagamento instrucaoPagamento, String cpfCnpjSolicitante, MotivoCancelamentoCamt55 motivoCancelamentoCamt55){
        var idCancelamento = SpiUtil.gerarIdFimAFim(TipoId.MENSAGEM_CANCELAMENTO_INSTRUCAO_PAGAMENTO);
        var dataHoraSolicitacao = LocalDateTime.now();

        return new CancelamentoAgendamentoWrapperDTO(instrucaoPagamento, idCancelamento, cpfCnpjSolicitante, motivoCancelamentoCamt55, dataHoraSolicitacao);
    }
}
