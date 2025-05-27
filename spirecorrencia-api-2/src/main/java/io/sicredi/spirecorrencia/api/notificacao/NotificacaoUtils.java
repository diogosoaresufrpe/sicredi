package io.sicredi.spirecorrencia.api.notificacao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMarcaEnum;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificacaoUtils {

    public static final String INTERNET_BANKING = "INTERNET_BANKING";
    public static final String SICREDI_APP = "SICREDI_APP";
    public static final String MOBI = "MOBI";

    public String converterCanalParaNotificacao(TipoCanalEnum canal, OrigemEnum sistema) {
        return switch (canal) {
            case SICREDI_INTERNET -> INTERNET_BANKING;
            case SICREDI_X -> SICREDI_APP;
            case WEB_OPENBK -> formatarCanalOpenBankingPorSistema(sistema);
            default -> canal.name();
        };
    }

    public static String converterCanalParaNotificacao(TipoCanalEnum canal, TipoMarcaEnum marca) {
        return switch (canal) {
            case SICREDI_INTERNET -> INTERNET_BANKING;
            case SICREDI_X -> SICREDI_APP;
            case WEB_OPENBK -> formatarCanalOpenBankingPorMarca(marca);
            default -> canal.name();
        };
    }

    public String converterCanalParaNotificacao(OrigemEnum sistema) {
        return switch (sistema) {
            case LEGADO -> MOBI;
            case FISITAL -> SICREDI_APP;
        };
    }

    private static String formatarCanalOpenBankingPorSistema(OrigemEnum sistema) {
        if (OrigemEnum.FISITAL.equals(sistema)) {
            return SICREDI_APP;
        }
        return TipoCanalEnum.MOBI.name();
    }

    private static String formatarCanalOpenBankingPorMarca(TipoMarcaEnum marca) {
        if (TipoMarcaEnum.SICREDI.equals(marca)) {
            return TipoCanalEnum.MOBI.name();
        }
        return SICREDI_APP;
    }

}
