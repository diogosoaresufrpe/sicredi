package io.sicredi.spirecorrencia.api.participante;

import lombok.Data;

import java.util.List;

@Data
class ParticipanteWrapperDTO {
    private List<ParticipanteDTO> content;
}