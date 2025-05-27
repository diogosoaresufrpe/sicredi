package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.consulta.PaginacaoDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Consulta de todas as autorizações do Pix Automático.")
public record AutorizacaoResponseWrapper(
        List<AutorizacaoResponse> autorizacoes, PaginacaoDTO paginacao) {
}