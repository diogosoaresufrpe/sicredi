package io.sicredi.spirecorrencia.api.exceptions.decoder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExceptionResponse {
    private String code;
    private String message;
    private String mensagemLog;
}

