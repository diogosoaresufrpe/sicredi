package io.sicredi.spirecorrencia.api.utils;

import br.com.sicredi.framework.exception.BusinessException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class ChaveUtils {

    private static final String CPF_REGEX = "^\\d{11}$";
    private static final String CNPJ_REGEX = "^\\w{14}$";
    private static final String TELEFONE_REGEX = "^\\+[1-9]\\d{1,14}$";
    private static final String EVP_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9.!#$%&'*+\\/=?^_`{|}~-]{0,77}@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,77}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,77}[a-zA-Z0-9])?){0,77}";

    private static final Pattern CPF_PATTERN = Pattern.compile(CPF_REGEX);
    private static final Pattern CNPJ_PATTERN = Pattern.compile(CNPJ_REGEX);
    private static final Pattern TELEFONE_PATTERN = Pattern.compile(TELEFONE_REGEX);
    private static final Pattern EVP_PATTERN = Pattern.compile(EVP_REGEX);
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    /**
     * Determina o tipo da chave Pix informada.
     *
     * @param chave representação da chave Pix.
     * @return {@link TipoChaveEnum} representando o tipo da chave informada.
     * @throws BusinessException caso a chave não possua um formato válido.
     */
    public TipoChaveEnum determinarTipoChave(String chave) {
        if (chave == null || chave.isBlank()) throw new BusinessException("A chave informada não possui um formato válido");
        if (CPF_PATTERN.matcher(chave).matches()) return TipoChaveEnum.CPF;
        if (CNPJ_PATTERN.matcher(chave).matches()) return TipoChaveEnum.CNPJ;
        if (TELEFONE_PATTERN.matcher(chave).matches()) return TipoChaveEnum.TELEFONE;
        if (EVP_PATTERN.matcher(chave).matches()) return TipoChaveEnum.EVP;
        if (EMAIL_PATTERN.matcher(chave).matches()) return TipoChaveEnum.EMAIL;
        throw new BusinessException("A chave informada não possui um formato válido");
    }

}
