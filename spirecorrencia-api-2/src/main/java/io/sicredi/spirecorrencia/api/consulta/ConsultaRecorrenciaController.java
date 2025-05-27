package io.sicredi.spirecorrencia.api.consulta;

import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
class ConsultaRecorrenciaController implements ConsultaRecorrenciaControllerApiDoc {

    private final ConsultaRecorrenciaService consultaRecorrenciaService;
    private final ObservabilidadeDecorator observabilidadeDecorator;

    @Override
    public ResponseEntity<RecorrenciaResponseWrapper> consultarRecorrencias(ConsultaRecorrenciaRequest consultaRecorrenciaRequest) {
        var atributos = Map.of(
                RecorrenciaMdc.AGENCIA_PAGADOR, consultaRecorrenciaRequest.getAgenciaPagador(),
                RecorrenciaMdc.CONTA_PAGADOR, consultaRecorrenciaRequest.getContaPagador()
        );
        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta de todas as recorrências.");
            var response = consultaRecorrenciaService.consultarTodas(consultaRecorrenciaRequest);
            log.debug("Fim da consulta de todas as recorrências.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }

    @Override
    public ResponseEntity<RecorrenciaResponse> consultarDetalheRecorrencia(String identificadorRecorrencia) {
        var atributos = Map.of(
                RecorrenciaMdc.IDENTIFICADOR_RECORRENCIA, identificadorRecorrencia
        );
        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta de detalhes da recorrência.");
            var response = consultaRecorrenciaService.consultarDetalhes(identificadorRecorrencia);
            log.debug("Fim da consulta de detalhes da recorrência.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }

    @Override
    public ResponseEntity<ParcelaRecorrenciaResponseWrapper> consultarParcelasRecorrencias(ConsultaParcelasRecorrenciaRequest consultaRecorrenciaRequest) {
        var atributos = Map.of(
                RecorrenciaMdc.AGENCIA_PAGADOR, consultaRecorrenciaRequest.getAgenciaPagador(),
                RecorrenciaMdc.CONTA_PAGADOR, consultaRecorrenciaRequest.getContaPagador()
        );
        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta de parcelas de todas as recorrências.");
            var response = consultaRecorrenciaService.consultarParcelas(consultaRecorrenciaRequest);
            log.debug("Fim da consulta de parcelas de todas as recorrências.");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }

    @Override
    public ResponseEntity<ConsultaDetalhesParcelasResponse> consultarDetalhesParcelas(String identificadorParcela, String agenciaPagador, String contaPagador) {
        var atributos = Map.of(
                RecorrenciaMdc.IDENTIFICADOR_PARCELA, identificadorParcela,
                RecorrenciaMdc.AGENCIA_PAGADOR, agenciaPagador,
                RecorrenciaMdc.CONTA_PAGADOR, contaPagador
        );

        return observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início da consulta dos detalhes da parcela.");
            var response = consultaRecorrenciaService.consultarDetalhesParcelas(identificadorParcela, agenciaPagador, contaPagador);
            log.debug("Fim da consulta dos detalhes da parcela");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        });
    }
}
