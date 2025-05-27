package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.framework.web.spring.exception.ExceptionResponse;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Pagador.Validations.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Validations.IDENTIFICADOR_PARCELA_NOTBLANK;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Regex.*;
import static io.sicredi.spirecorrencia.api.consulta.RecorrenciaSample.*;

@Tag(name = "Consulta de Recorrências do Pix", description = "Reune endpoints destinados a consulta de recorrências Pix.")
@Validated
@RequestMapping(value = "/v1/recorrencias")
interface ConsultaRecorrenciaControllerApiDoc {


    @Operation(
            summary = "Consulta de recorrências Pix",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Listagem de recorrências", content = {
                            @Content(mediaType = "application/json", schema = @Schema(example = EXEMPLO_RESPONSE_LISTAGEM_RECORRENCIAS))
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
    ResponseEntity<RecorrenciaResponseWrapper> consultarRecorrencias(
            @Valid @ParameterObject ConsultaRecorrenciaRequest consultaRecorrenciaRequest
    );

    @Operation(
            summary = "Consulta de detalhes da recorrência Pix",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Consulta de detalhe da recorrência realizada com sucesso", content = @Content(schema = @Schema(example = EXEMPLO_RESPONSE_DETALHE_RECORRENCIA))),
                    @ApiResponse(responseCode = "400", description = "Requisicao Invalida", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
                    @ApiResponse(responseCode = "422", description = "Entidade não processável", content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Erro no Servidor", content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))}
    )
    @GetMapping(value = "/{identificadorRecorrencia}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<RecorrenciaResponse> consultarDetalheRecorrencia(
            @Schema(title = "ID da Recorrência")
            @PathVariable
            @NotBlank(message =  "Preenchimento do identificador único da recorrência é obrigatório")
            @Pattern(regexp = "\\S+", message = "Preenchimento do identificador único da recorrência é obrigatório")
            final String identificadorRecorrencia
    );

    @Operation(
            summary = "Consulta de parcelas das recorrências Pix",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Listagem das parcelas da recorrências", content = {
                            @Content(mediaType = "application/json", schema = @Schema(example = EXEMPLO_RESPONSE_LISTAGEM_PARCELAS_RECORRENCIA))
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
    @GetMapping(value = "/parcelas", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ParcelaRecorrenciaResponseWrapper> consultarParcelasRecorrencias(
            @Valid @ParameterObject ConsultaParcelasRecorrenciaRequest consultaParcelaRecorrenciaRequest
    );


    @Operation(
            summary = "Consulta de detalhes da parcelas Pix",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Listagem de detalhes da parcela",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(name = "Exemplo Criado", value = EXEMPLO_RESPONSE_DETALHES_PARCELAS_RECORRENCIA_CRIADO),
                                            @ExampleObject(name = "Exemplo Cancelado", value = EXEMPLO_RESPONSE_DETALHES_PARCELAS_RECORRENCIA_CANCELADO)
                                    }
                            )
                    ),
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
    @GetMapping(value = "/parcelas/{identificadorParcela}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ConsultaDetalhesParcelasResponse> consultarDetalhesParcelas(
            @Schema(example = RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Exemplos.IDENTIFICADOR_PARCELA, title = RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Titles.IDENTIFICADOR_PARCELA)
            @PathVariable
            @NotBlank(message = IDENTIFICADOR_PARCELA_NOTBLANK)
            @Pattern(regexp = PATTERN_IDENTIFICADOR_PARCELA, message = IDENTIFICADOR_PARCELA_NOTBLANK)
            final String identificadorParcela,
            @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.AGENCIA, description = RecorrenciaConstantes.Pagador.Schemas.Titles.AGENCIA, pattern = PATTERN_AGENCIA, requiredMode = Schema.RequiredMode.REQUIRED)
            @Pattern(regexp = PATTERN_AGENCIA, message = AGENCIA_PATTERN_MESSAGE)
            @NotBlank(message = AGENCIA_PATTERN)
            String agenciaPagador,
            @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.CONTA, description = RecorrenciaConstantes.Pagador.Schemas.Titles.CONTA, pattern = PATTERN_CONTA, requiredMode = Schema.RequiredMode.REQUIRED)
            @Pattern(regexp = PATTERN_CONTA, message = CONTA_PATTERN_MESSAGE)
            @NotBlank(message = CONTA_NOTBLANK)
            String contaPagador
    );

}
