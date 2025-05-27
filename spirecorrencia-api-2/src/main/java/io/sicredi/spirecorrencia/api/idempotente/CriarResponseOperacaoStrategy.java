package io.sicredi.spirecorrencia.api.idempotente;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentEvent;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.metrica.MetricaCounter;
import io.sicredi.spirecorrencia.api.metrica.RegistraMetricaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
final class CriarResponseOperacaoStrategy implements CriaResponseStrategy<OperacaoRequest> {

    private final RegistraMetricaService registraMetricaService;

    @Override
    public TipoResponseIdempotente obterTipoResponse() {
        return TipoResponseIdempotente.OPERACAO;
    }


    @Override
    public IdempotentResponse<?> criarResponseIdempotentSucesso(OperacaoRequest operacaoRequest,
                                                                String transactionId,
                                                                Map<String, String> headers,
                                                                List<EventoResponseDTO> listaEventos) {
        log.debug("Sucesso ao realizar a transação. Identificador da transação: {}", transactionId);
        return criarResponseSucesso(operacaoRequest, headers, listaEventos);
    }

    private IdempotentResponse<?> criarResponseSucesso(OperacaoRequest operacaoRequest, Map<String, String> headers, List<EventoResponseDTO> listaEventos) {
        var listaIdempotentEvent = new ArrayList<IdempotentEvent<?>>();

        Stream.ofNullable(listaEventos)
                .flatMap(List::stream)
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .forEach(listaIdempotentEvent::add);

        registrarMetrica(operacaoRequest);

        return IdempotentResponse.builder()
                .events(listaIdempotentEvent)
                .headers(headers)
                .errorResponse(false)
                .build();
    }

    private void registrarMetrica(OperacaoRequest ordemRequest) {
        var metrica = new MetricaCounter(
                "pix_automatico_resultado_processamento",
                "Resultado do processamento de fluxos do Pix Automático."
        ).adicionarTag("operacao", ordemRequest.getOperacao())
         .adicionarTag("motivo_rejeicao", ordemRequest.getMotivoRejeicao());

        registraMetricaService.registrar(metrica);
    }

}
