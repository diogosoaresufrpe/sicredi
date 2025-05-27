package io.sicredi.spirecorrencia.api.exclusao;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@SuppressWarnings("unused")
class ExclusaoRecorrenciaController implements ExclusaoRecorrenciaControllerApiDoc {

    private final ExclusaoService exclusaoService;
    private final ObservabilidadeDecorator observabilidadeDecorator;

    public ResponseEntity<Object> excluirRecorrenciasTransacao(String idempotente, ExclusaoRequisicaoDTO exclusaoRequisicaoDTO) {
        var idRecorrencia = exclusaoRequisicaoDTO.getIdentificadorRecorrencia();

        var atributos = Map.of(
                RecorrenciaMdc.IDENTIFICADOR_TRANSACAO, idempotente,
                RecorrenciaMdc.IDENTIFICADOR_RECORRENCIA, idRecorrencia
        );

        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("[ADMIN] Inicio processamento protocolo");

            exclusaoRequisicaoDTO.setFluxoLiquidacao(false);
            exclusaoRequisicaoDTO.setTipoResponse(TipoResponseIdempotente.ADMINISTRATIVO);

            var request = IdempotentRequest
                    .<ExclusaoRequisicaoDTO>builder()
                    .value(exclusaoRequisicaoDTO)
                    .transactionId(idempotente)
                    .build();

            var retorno = exclusaoService.processarProtocolo(request);

            if (retorno.isErrorResponse()) {
                if (retorno.getValue() instanceof String valueString && valueString.contains("ERRO_VALIDACAO")) {
                    return ResponseEntity.status(400).body(retorno.getValue());
                }
                return ResponseEntity.status(422).body(retorno.getValue());
            }

            log.debug("[ADMIN] Mensagem consumida com sucesso.");
            return ResponseEntity.noContent().build();
        });
    }

}
