package io.sicredi.spirecorrencia.api.consulta;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Consulta de todas recorrências Pix")
record RecorrenciaResponseWrapper(List<RecorrenciaResponse> recorrencias, PaginacaoDTO paginacao) {

}
