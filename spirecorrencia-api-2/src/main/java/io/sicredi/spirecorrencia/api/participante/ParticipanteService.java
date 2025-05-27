package io.sicredi.spirecorrencia.api.participante;

import br.com.sicredi.framework.exception.BusinessException;
import br.com.sicredi.framework.exception.TechnicalException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipanteService {

    private static final Integer PAGE = 0;
    private static final Integer SIZE = 1;
    private static final String SITUACAO = "ENABLE";

    private final ParticipanteClient participanteClient;

    @Cacheable(cacheNames = "participanteCache", key = "#ispb")
    public String buscarNomeInstituicao(String ispb) {
        try {
            return participanteClient.consultarInstituicao(PAGE, SIZE, SITUACAO, ispb)
                    .getContent()
                    .stream()
                    .findFirst()
                    .map(ParticipanteDTO::nome)
                    .orElse(null);
        } catch (TechnicalException | BusinessException | RetryableException ex) {
            return null;
        }
    }
}