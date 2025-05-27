package io.sicredi.spirecorrencia.api.accountdata;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoProdutoEnum {

    INDIVIDUAL_CHECKING_ACCOUNT(TipoContaEnum.CHECKING_ACCOUNT),
    INDIVIDUAL_SAVINGS_ACCOUNT(TipoContaEnum.SAVINGS_ACCOUNT),
    JOINT_CHECKING_ACCOUNT(TipoContaEnum.CHECKING_ACCOUNT),
    JOINT_SAVINGS_ACCOUNT(TipoContaEnum.SAVINGS_ACCOUNT),
    SALARY_ACCOUNT(TipoContaEnum.SALARY_ACCOUNT),
    PAYMENT_ACCOUNT(TipoContaEnum.PAYMENT_ACCOUNT),

    @JsonEnumDefaultValue
    UNKNOWN(TipoContaEnum.UNKNOWN);

    final TipoContaEnum asTipoConta;

}