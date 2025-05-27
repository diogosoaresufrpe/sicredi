package io.sicredi.spirecorrencia.api.idempotente;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentEvent;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentHeaders;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
class CriaResponseAdministrativoStrategy implements CriaResponseStrategy<IdempotenteRequest> {

    @Override
    public TipoResponseIdempotente obterTipoResponse() {
        return TipoResponseIdempotente.ADMINISTRATIVO;
    }

    @Override
    public IdempotentResponse<?> criarResponseIdempotentSucesso(IdempotenteRequest ordemRequest,
                                                                String transactionId,
                                                                Map<String, String> headers,
                                                                List<EventoResponseDTO> listaEventos) {
        log.debug("Sucesso ao realizar a transação. Identificador da transação: {}", transactionId);
        return criarResponseSucesso(ordemRequest, headers, listaEventos);
    }

    @Override
    public IdempotentResponse<?> criarResponseIdempotentErro(IdempotenteRequest ordemRequest,
                                                             String transactionId,
                                                             ErroDTO erro) {
        logTransacao("Falha ao realizar validação das constraints. Identificador da transação: %s | Detalhes do erro: %s", transactionId, erro.mensagemErro());
        return criarResponseErro(transactionId, erro);
    }

    @Override
    public void criarResponseReprocessamentoIdempotentTransactionDuplicated(DeadLetterRequest reprocessamentoRequest, ErroDTO erro) {
        log.warn("[ADMIN] - Reprocessamento de transação duplicada. Detalhes do erro: {}", erro.mensagemErro());
    }

    @Override
    public void criarResponseReprocessamentoOutrasExceptions(DeadLetterRequest reprocessamentoRequest, ErroDTO erro) {
        log.warn("[ADMIN] - Reprocessamento de outras exception. Detalhes do erro: {}", erro.mensagemErro());
    }

    private void logTransacao(String mensagemErro, String identificadorTransacao, String detalhesErro) {
        var mensagemLog = String.format(mensagemErro, identificadorTransacao, detalhesErro);
        log.error(mensagemLog);
    }

    private IdempotentResponse<?> criarResponseSucesso(IdempotenteRequest ordemRequest, Map<String, String> headers, List<EventoResponseDTO> listaEventos) {
        var listaIdempotentEvent = new ArrayList<IdempotentEvent<?>>();

        Stream.ofNullable(listaEventos)
                .flatMap(List::stream)
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .forEach(listaIdempotentEvent::add);

        return IdempotentResponse.builder()
                .value(ordemRequest)
                .events(listaIdempotentEvent)
                .headers(headers)
                .errorResponse(false)
                .build();
    }

    private IdempotentResponse<?> criarResponseErro(String transactionId,
                                                    ErroDTO erro) {

        var headersResponse = new HashMap<String, String>();
        headersResponse.put(IdempotentHeaders.TRANSACTION_ID, transactionId);

        var listaIdempotentEvent = new ArrayList<IdempotentEvent<?>>();

        Stream.ofNullable(erro.listaEventos())
                .flatMap(List::stream)
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .forEach(listaIdempotentEvent::add);

        return IdempotentResponse.builder()
                .headers(headersResponse)
                .value(erro)
                .events(listaIdempotentEvent)
                .errorResponse(true)
                .build();
    }


}
