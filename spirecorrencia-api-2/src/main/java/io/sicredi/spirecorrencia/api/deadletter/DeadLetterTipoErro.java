package io.sicredi.spirecorrencia.api.deadletter;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransactionDuplicatedException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@AllArgsConstructor
@Getter
enum DeadLetterTipoErro {
    IDEMPOTENT_TRANSACTION_DUPLICATED(IdempotentTransactionDuplicatedException.class),
    OUTRAS(RuntimeException.class);


    private final Class<?> tipoException;

    public static DeadLetterTipoErro of(String causeException) {
        return criarClassException(causeException)
                .map(classException -> Arrays.stream(values())
                    .filter(tipoException -> tipoException.tipoException.isAssignableFrom(classException))
                    .findFirst()
                    .orElse(DeadLetterTipoErro.OUTRAS))
                .orElse(DeadLetterTipoErro.OUTRAS);
    }

    private static Optional<Class<?>> criarClassException(String causeException) {
        try {
            return Optional.of(Class.forName(causeException));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }

}
