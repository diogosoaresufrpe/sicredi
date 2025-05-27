package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;

public interface CancelamentoAgendamentoRequest {
    String idCancelamentoAgendamento();
    String cpfCnpjSolicitanteCancelamento();
    MotivoCancelamentoCamt55 motivoCancelamento();
}
