package io.sicredi.spirecorrencia.api.automatico.camt;

import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.TipoSolicitacaoCamt55;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Camt055ResponseFactory {

    public static Camt055Dto fromInstrucaoPagamento(RecorrenciaInstrucaoPagamento instrucaoPagamento,
                                                    String idCancelamentoAgendamento,
                                                    String cpfCnpjSolicitante,
                                                    String motivoCancelamento,
                                                    LocalDateTime dataHoraSolicitacaoOuInformacao){

        return Camt055Dto.builder()
                .idCancelamentoAgendamento(idCancelamentoAgendamento)
                .idConciliacaoRecebedorOriginal(instrucaoPagamento.getIdConciliacaoRecebedor())
                .cpfCnpjUsuarioSolicitanteCancelamento(cpfCnpjSolicitante)
                .motivoCancelamento(motivoCancelamento)
                .idFimAFimOriginal(instrucaoPagamento.getCodFimAFim())
                .participanteDestinatarioDoCancelamento(instrucaoPagamento.getNumInstituicaoRecebedor())
                .participanteSolicitanteDoCancelamento(instrucaoPagamento.getNumInstituicaoPagador())
                .tipoSolicitacaoOuInformacao(TipoSolicitacaoCamt55.SOLICITADO_PELO_PAGADOR.name())
                .dataHoraSolicitacaoOuInformacao(dataHoraSolicitacaoOuInformacao)
                .build();
    }

}
