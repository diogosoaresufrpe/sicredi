package io.sicredi.spirecorrencia.api.accountdata;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TipoRelacaoEnum {

    UNDERAGE_HOLDER,
    PERMISSIONLESS_UNDERAGE_UPHOLDER,

    @JsonEnumDefaultValue
    UNKNOWN

}
