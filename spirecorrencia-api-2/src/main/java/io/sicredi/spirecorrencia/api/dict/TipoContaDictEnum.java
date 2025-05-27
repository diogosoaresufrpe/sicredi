package io.sicredi.spirecorrencia.api.dict;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoContaDictEnum {
    CORRENTE(TipoContaEnum.CONTA_CORRENTE),
    SALARIO(TipoContaEnum.CONTA_SALARIO),
    POUPANCA(TipoContaEnum.CONTA_POUPANCA),
    PAGAMENTO(TipoContaEnum.CONTA_PAGAMENTO);

    private final TipoContaEnum tipoContaEnum;
}
