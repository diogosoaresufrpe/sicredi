package io.sicredi.spirecorrencia.api.idempotente;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransactionService;
import io.sicredi.spirecorrencia.api.messasing.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class IdempotenteServiceImpl implements IdempotenteService {

    private final IdempotentTransactionService idempotentTransactionService;
    private final MessageProducer messageProducer;

    @Override
    public void reenviarOperacao(final String topico, final String chaveIdempotencia) {
        log.info("Reenviando operacao para o topico: [{}], chaveIdempotencia: [{}].", topico, chaveIdempotencia);

        var response = idempotentTransactionService.findTransactionResponse(chaveIdempotencia);
        var eventResponse = response.getEvents().stream()
                .filter(idempotentEvent -> idempotentEvent.getTopic().equalsIgnoreCase(topico))
                .findFirst();

        if (eventResponse.isEmpty()) {
            log.warn("Nao foi encontrado 'eventoResponse' para a chaveIdempotencia: [{}], topico: [{}].",
                    chaveIdempotencia, topico);
            return;
        }

        messageProducer.enviar(eventResponse.get().getValue(), topico, eventResponse.get().getHeaders());
    }
}