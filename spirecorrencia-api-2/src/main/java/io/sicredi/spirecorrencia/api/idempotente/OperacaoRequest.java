package io.sicredi.spirecorrencia.api.idempotente;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoMensagem;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OperacaoRequest implements IdempotenteRequest {
    private String operacao;
    private String motivoRejeicao;

    public static OperacaoRequest criarOperacaoRequest(TipoMensagem tipoMensagem, String motivoRejeicao) {
        return OperacaoRequest.builder()
                .operacao(tipoMensagem.name())
                .motivoRejeicao(motivoRejeicao)
                .build();
    }

}
