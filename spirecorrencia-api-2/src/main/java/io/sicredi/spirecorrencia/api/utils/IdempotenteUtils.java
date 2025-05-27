package io.sicredi.spirecorrencia.api.utils;

import br.com.sicredi.spi.dto.*;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.StringJoiner;

@UtilityClass
public class IdempotenteUtils {

    public byte[] criarChecksumPain009(Pain009Dto pain009) {
        var joiner = new StringJoiner(StringUtils.EMPTY);

        var valor = Optional.ofNullable(pain009.getValor()).orElse(BigDecimal.ZERO);

        joiner.add(pain009.getIdSolicitacaoRecorrencia());
        joiner.add(pain009.getIdRecorrencia());
        joiner.add(valor.toString());
        joiner.add(pain009.getCpfCnpjUsuarioRecebedor());
        joiner.add(pain009.getCpfCnpjUsuarioPagador());
        joiner.add(pain009.getNumeroContrato());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] criarChecksumPain011(Pain011Dto pain011) {
        var joiner = new StringJoiner(StringUtils.EMPTY);
        joiner.add(pain011.getIdInformacaoCancelamento());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] criarChecksumPain012(Pain012Dto pain012) {
        var joiner = new StringJoiner(StringUtils.EMPTY);

        var valor = Optional.ofNullable(pain012.getValor()).orElse(BigDecimal.ZERO);

        joiner.add(pain012.getIdInformacaoStatus());
        joiner.add(pain012.getCpfCnpjUsuarioRecebedor());
        joiner.add(pain012.getCpfCnpjUsuarioPagador());
        joiner.add(pain012.getIdRecorrencia());
        joiner.add(valor.toString());
        joiner.add(pain012.getNumeroContrato());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] criarChecksumPain013(Pain013Dto pain013) {
        var joiner = new StringJoiner(StringUtils.EMPTY);

        var valor = Optional.ofNullable(pain013.getValor()).orElse(BigDecimal.ZERO);

        joiner.add(pain013.getIdFimAFim());
        joiner.add(pain013.getCpfCnpjUsuarioRecebedor());
        joiner.add(pain013.getCpfCnpjUsuarioPagador());
        joiner.add(pain013.getIdRecorrencia());
        joiner.add(valor.toString());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] criarChecksumPain014(Pain014Dto pain014) {
        var joiner = new StringJoiner(StringUtils.EMPTY);
        joiner.add(pain014.getIdFimAFimOriginal());
        joiner.add(pain014.getCpfCnpjUsuarioRecebedor());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] criarCheckSumCamt055(Camt055Dto camt055Dto) {
        var joiner = new StringJoiner(StringUtils.EMPTY);
        joiner.add(camt055Dto.getIdCancelamentoAgendamento());
        joiner.add(camt055Dto.getIdFimAFimOriginal());
        joiner.add(camt055Dto.getCpfCnpjUsuarioSolicitanteCancelamento());
        joiner.add(camt055Dto.getTipoSolicitacaoOuInformacao());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }
}
