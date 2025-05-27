package io.sicredi.spirecorrencia.api.automatico.camt;

import br.com.sicredi.spi.dto.Camt029Dto;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoCamt029;
import br.com.sicredi.spi.entities.type.StatusCancelamentoCamt029;
import br.com.sicredi.spi.entities.type.TipoAceitacaoRejeicaoCamt029;

import java.time.LocalDateTime;

public class Camt029ResponseFactory {

    private Camt029ResponseFactory() {
    }

    public static Camt029Dto fromCamt055Aceita(Camt055Dto camt055) {
        return buildCamt029Dto(camt055, StatusCancelamentoCamt029.ACEITO, null, TipoAceitacaoRejeicaoCamt029.DATA_HORA_ACEITACAO_CANCELAMENTO.name());
    }

    public static Camt029Dto fromCamt055Rejeitada(Camt055Dto camt055, MotivoRejeicaoCamt029 motivoRejeicao) {
        return buildCamt029Dto(camt055, StatusCancelamentoCamt029.REJEITADO, motivoRejeicao, TipoAceitacaoRejeicaoCamt029.DATA_HORA_REJEICAO_CANCELAMENTO.name());
    }

    private static Camt029Dto buildCamt029Dto(Camt055Dto camt055, StatusCancelamentoCamt029 statusCancelamento,
                                              MotivoRejeicaoCamt029 motivoRejeicao, String tipoAceiteRejeicao) {
        return Camt029Dto.builder()
                .idCancelamentoAgendamentoOriginal(camt055.getIdCancelamentoAgendamento())
                .idConciliacaoRecebedorOriginal(camt055.getIdConciliacaoRecebedorOriginal())
                .statusDoCancelamento(statusCancelamento.name())
                .codigoDeRejeicaoDoCancelamento(motivoRejeicao != null ? motivoRejeicao.name() : null)
                .idFimAFimOriginal(camt055.getIdFimAFimOriginal())
                .tipoAceitacaoOuRejeicao(tipoAceiteRejeicao)
                .dataHoraAceitacaoOuRejeicaoDoCancelamento(LocalDateTime.now())
                .participanteAtualizaSolicitacaoDoCancelamento(camt055.getParticipanteSolicitanteDoCancelamento())
                .participanteRecebeAtualizacaoDoCancelamento(camt055.getParticipanteDestinatarioDoCancelamento())
                .build();
    }
}