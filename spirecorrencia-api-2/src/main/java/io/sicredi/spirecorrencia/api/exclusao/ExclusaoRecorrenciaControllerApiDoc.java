package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.framework.web.spring.exception.ExceptionResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Exclusão de Recorrências do Pix", description = "Reune endpoints destinados a excluir recorrências Pix.")
@Hidden
@Validated
@RequestMapping(value = "/v1/admin/recorrencia/exclusao")
@SuppressWarnings("unused")
interface ExclusaoRecorrenciaControllerApiDoc {


    @Operation(
            summary = "Exclusão de recorrência Pix",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Recorrência excluída com sucesso", content = {
                            @Content()
                    }),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))
                    }),
                    @ApiResponse(responseCode = "422", description = "Entidade não processável", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))
                    }),
                    @ApiResponse(responseCode = "5XX", description = "Erro no Servidor", content = {
                            @Content(mediaType = "application/json", schema = @Schema(implementation = ExceptionResponse.class))
                    })
            }
    )
    @DeleteMapping
    ResponseEntity<?> excluirRecorrenciasTransacao(
                                                      @RequestParam  @NotBlank(message =  "Preenchimento do idempotente é obrigatório") final String idempotente,
                                                      @RequestBody @Valid ExclusaoRequisicaoDTO exclusaoRequisicaoDTO);


}
