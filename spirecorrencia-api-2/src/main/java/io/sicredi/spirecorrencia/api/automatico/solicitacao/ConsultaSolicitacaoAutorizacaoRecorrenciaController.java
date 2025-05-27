package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
class ConsultaSolicitacaoAutorizacaoRecorrenciaController implements ConsultaSolicitacaoAutorizacaoRecorrenciaControllerApiDoc {

    private final SolicitacaoAutorizacaoRecorrenciaService service;
    private final ObservabilidadeDecorator observabilidadeDecorator;

    @Override
    public ResponseEntity<SolicitacaoAutorizacaoRecorrenciaResponseWrapper> consultarSolicitacoesAutorizacao(ConsultaSolicitacaoAutorizacaoRecorrenciaRequest request) {
        var atributos = Map.of(
                RecorrenciaMdc.AGENCIA_PAGADOR, request.getAgenciaPagador(),
                RecorrenciaMdc.CONTA_PAGADOR, request.getContaPagador()
        );
        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta de todas as solicitacoes de autorizacao para pix automático.");
            var response = service.consultarTodas(request);
            log.debug("Fim da consulta de todas as solicitacoes de autorizacao para pix automático.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }

    @Override
    public ResponseEntity<SolicitacaoAutorizacaoRecorrenciaResponse> consultarDetalheSolicitacaoAutorizacao(String idSolicitacaoRecorrencia) {
        var atributos = Map.of(
                RecorrenciaMdc.ID_SOLICITACAO_RECORRENCIA, idSolicitacaoRecorrencia
        );
        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta de detalhes solicitação de  recorrência.");
            var response = service.consultarDetalhes(idSolicitacaoRecorrencia);
            log.debug("Fim da consulta detalhes solicitação de  recorrência.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }
}