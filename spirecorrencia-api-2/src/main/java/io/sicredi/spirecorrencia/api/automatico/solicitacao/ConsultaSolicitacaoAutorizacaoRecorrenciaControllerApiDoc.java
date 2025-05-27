package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.framework.web.spring.exception.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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

import static io.sicredi.spirecorrencia.api.automatico.solicitacao.SolicitacaoAutorizacaoRecorrenciaSample.EXEMPLO_RESPONSE_DETALHE_SOLICITACAO_AUTORIZACAO;
import static io.sicredi.spirecorrencia.api.automatico.solicitacao.SolicitacaoAutorizacaoRecorrenciaSample.EXEMPLO_RESPONSE_LISTAGEM_SOLICITACOES_AUTORIZACAO;

@Tag(name = "Consulta de solicitações de autorização do Pix Automático.",
        description = "Reune endpoints destinados a consulta de solicitações de autorização do Pix Automático.")
@Validated
@RequestMapping(value = "/v1/automaticos/autorizacoes/solicitacoes")
interface ConsultaSolicitacaoAutorizacaoRecorrenciaControllerApiDoc {

    @Operation(
            summary = "Consulta de solicitações de autorização do Pix Automático",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Listagem de solicitações de autorização do Pix Automático.", content = {
                            @Content(mediaType = "application/json", schema = @Schema(example = EXEMPLO_RESPONSE_LISTAGEM_SOLICITACOES_AUTORIZACAO))
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
    ResponseEntity<SolicitacaoAutorizacaoRecorrenciaResponseWrapper> consultarSolicitacoesAutorizacao(
            @Valid @ParameterObject ConsultaSolicitacaoAutorizacaoRecorrenciaRequest request
    );

    @Operation(
            summary = "Consulta detalhe solicitação de autorização do Pix Automático",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Detalhe de solicitação de autorização do Pix Automático.", content = {
                            @Content(mediaType = "application/json", schema = @Schema(example = EXEMPLO_RESPONSE_DETALHE_SOLICITACAO_AUTORIZACAO))
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
    @GetMapping(value="/{idSolicitacaoRecorrencia}",produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<SolicitacaoAutorizacaoRecorrenciaResponse> consultarDetalheSolicitacaoAutorizacao(
            @Schema(title = "ID da Recorrência")
            @PathVariable
            @NotBlank(message =  "Preenchimento do identificador da solicitação de autorização é obrigatório")
            @Pattern(regexp = "\\S+", message = "Preenchimento do identificador da solicitação de autorização é obrigatório")
            final String idSolicitacaoRecorrencia
    );
}
