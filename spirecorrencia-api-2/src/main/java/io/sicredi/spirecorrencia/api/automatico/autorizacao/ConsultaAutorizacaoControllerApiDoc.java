package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.framework.web.spring.exception.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoResponseSample.EXEMPLO_RESPONSE_LISTAGEM_AUTORIZACOES;


@Tag(name = "Consulta autorizações do Pix Automático.",
        description = "Reune endpoints destinados a consulta de autorizações do Pix Automático.")
@Validated
@RequestMapping(value = "/v1/automaticos/autorizacoes")
interface ConsultaAutorizacaoControllerApiDoc {

    @Operation(
            summary = "Consulta de autorizações do Pix Automático",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Listagem de autorizações do Pix Automático.", content = {
                            @Content(mediaType = "application/json", schema = @Schema(example = EXEMPLO_RESPONSE_LISTAGEM_AUTORIZACOES))
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
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AutorizacaoResponseWrapper> consultarAutorizacoes(
            @Valid @ParameterObject ConsultaAutorizacaoRequest request
    );

    @Operation(
            summary = "Detalhamento de autorização do Pix Automático.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Detalhamento de autorização do Pix Automático.", content = {
                            @Content(mediaType = "application/json", schema = @Schema(example = EXEMPLO_RESPONSE_LISTAGEM_AUTORIZACOES))
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
    @GetMapping(value = "/{oidRecorrenciaAutorizacao}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AutorizacaoResponse> consultarDetalhesAutorizacao(
            @NotNull(message =  "Preenchimento do identificador da autorização é obrigatório")
            @Valid
            @PathVariable Long oidRecorrenciaAutorizacao
    );

}