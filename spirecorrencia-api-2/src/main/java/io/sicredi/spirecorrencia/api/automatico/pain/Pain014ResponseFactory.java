package io.sicredi.spirecorrencia.api.automatico.pain;

import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.dto.Pain014Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain014;
import br.com.sicredi.spi.entities.type.SituacaoAgendamentoPain014;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class Pain014ResponseFactory {
    public static Pain014Dto fromPain013Erro(Pain013Dto pain013Dto, MotivoRejeicaoPain014 motivoRejeicaoPain014, LocalDateTime dataConfirmacao) {
        return fromPain013(pain013Dto, dataConfirmacao)
                .situacaoDoAgendamento(SituacaoAgendamentoPain014.REJEITADA_USUARIO_PAGADOR.name())
                .codigoDeErro(motivoRejeicaoPain014.name())
                .build();
    }

    public static Pain014Dto fromPain013Sucesso(Pain013Dto pain013Dto, LocalDateTime dataConfirmacao) {
        return fromPain013(pain013Dto, dataConfirmacao)
                .situacaoDoAgendamento(SituacaoAgendamentoPain014.ACEITA_USUARIO_PAGADOR.name())
                .build();
    }

    private static Pain014Dto.Pain014DtoBuilder fromPain013(Pain013Dto pain013Dto, LocalDateTime dataConfirmacao) {
        return Pain014Dto.builder()
                .idConciliacaoRecebedorOriginal(pain013Dto.getIdConciliacaoRecebedor())
                .idFimAFimOriginal(pain013Dto.getIdFimAFim())
                .dataHoraAceitacaoOuRejeicaoDoAgendamento(dataConfirmacao)
                .participanteDoUsuarioRecebedor(pain013Dto.getParticipanteDoUsuarioRecebedor())
                .cpfCnpjUsuarioRecebedor(pain013Dto.getCpfCnpjUsuarioRecebedor());
    }
}
