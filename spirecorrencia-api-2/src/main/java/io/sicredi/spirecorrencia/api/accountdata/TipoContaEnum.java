package io.sicredi.spirecorrencia.api.accountdata;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;

@Getter
@RequiredArgsConstructor
public enum TipoContaEnum {

    CHECKING_ACCOUNT("CORRENTE", "CONTA_CORRENTE", "CORRENTE"),
    SAVINGS_ACCOUNT("POUPANCA", "CONTA_POUPANCA", "POUPANCA"),
    SALARY_ACCOUNT("SALARIO", "CONTA_SALARIO", "SALARIO"),
    PAYMENT_ACCOUNT("PAGAMENTO", "CONTA_PAGAMENTO", "PGTO"),

    @JsonEnumDefaultValue
    UNKNOWN(Strings.EMPTY, Strings.EMPTY, Strings.EMPTY);

    private final String asCanaisPixNomeSimples;
    private final String asCanaisPixNomeCompleto;
    private final String asSpiExtrato;
}