package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.consulta.PaginacaoDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Consulta de todas as solicitações de autorização do Pix Automático.")
public record SolicitacaoAutorizacaoRecorrenciaResponseWrapper(
        List<SolicitacaoAutorizacaoRecorrenciaResponse> solicitacoes, PaginacaoDTO paginacao) {
}