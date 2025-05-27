package io.sicredi.spirecorrencia.api.gestentconector;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class GestentConectorService {

    private final GestentConectorClient gestentConectorClient;

    public static final String TIPO_ENTIDADE_AGENCIA = "AGENCIA";
    public static final String TIPO_ENTIDADE_COOPERATIVA = "COOPERATIVA";
    public static final String AGENCIA_01 = "01";

    public String consultarCodigoMunicipio(String agencia, String cooperativa) {
        String tipoEntidade = AGENCIA_01.equals(agencia) ? TIPO_ENTIDADE_COOPERATIVA : TIPO_ENTIDADE_AGENCIA;

        try {
            Page<InformacoesCooperativaDTO> resultado = gestentConectorClient.consultarDadosCooperativa(tipoEntidade, agencia, cooperativa, PageRequest.of(0, 1));

            return resultado.getContent().stream()
                    .map(InformacoesCooperativaDTO::getCodigoIbge)
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(null);

        } catch (NotFoundException ex) {
            log.debug("Código de município não encontrado: tipoEntidade = {}, agencia = {}, cooperativa = {}", tipoEntidade, agencia, cooperativa);
            return null;
        } catch (RetryableException retryableException) {
            log.error("Erro ao consultar código de município. Motivo: TimeOut | Detalhes: {}", retryableException.getMessage());
            throw new TechnicalException(retryableException);
        }
    }

}
