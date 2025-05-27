package io.sicredi.spirecorrencia.api.automatico.autorizacao;

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
class ConsultaAutorizacaoController implements ConsultaAutorizacaoControllerApiDoc {

    private final AutorizacaoServiceImpl service;
    private final ObservabilidadeDecorator observabilidadeDecorator;

    @Override
    public ResponseEntity<AutorizacaoResponseWrapper> consultarAutorizacoes(ConsultaAutorizacaoRequest request) {
        var atributos = Map.of(
                RecorrenciaMdc.AGENCIA_PAGADOR, request.getAgenciaPagador(),
                RecorrenciaMdc.CONTA_PAGADOR, request.getContaPagador()
        );
        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta de todas as autorizações para pix automático.");
            var response = service.consultarTodas(request);
            log.debug("Fim da consulta de todas as autorizações para pix automático.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }

    @Override
    public ResponseEntity<AutorizacaoResponse> consultarDetalhesAutorizacao(Long oidRecorrenciaAutorizacao) {
        var atributos = Map.of(
                RecorrenciaMdc.OID_RECORRENCIA_AUTORIZACAO, String.valueOf(oidRecorrenciaAutorizacao)
        );
        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta de detalhes de autorização para pix automático. OID_RECORRENCIA_AUTORIZACAO: {}", oidRecorrenciaAutorizacao);
            var response = service.consultarDetalhes(oidRecorrenciaAutorizacao);
            log.debug("Fim da consulta de detalhes de autorização para pix automático. OID_RECORRENCIA_AUTORIZACAO: {}", oidRecorrenciaAutorizacao);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }
}