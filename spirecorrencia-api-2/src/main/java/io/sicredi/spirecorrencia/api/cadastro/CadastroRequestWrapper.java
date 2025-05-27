package io.sicredi.spirecorrencia.api.cadastro;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class CadastroRequestWrapper {
    private final CadastroRequest agendamento;
    private final CadastroRequest recorrencia;

    public static CadastroRequestWrapper criarAgendamento(CadastroRequest agendamento) {
        return new CadastroRequestWrapper(agendamento, null);
    }

    public static CadastroRequestWrapper criarAgendamentoComRecorrencia(CadastroRequest agendamento, CadastroRequest recorrencia) {
        return new CadastroRequestWrapper(agendamento, recorrencia);
    }

    public static CadastroRequestWrapper criarRecorrencia(CadastroRequest recorrencia) {
        return new CadastroRequestWrapper(null, recorrencia);
    }

    public boolean possuiRecorrencia() {
        return this.recorrencia != null;
    }

    public List<String> listaIdentificadoresParcelas() {
        return Stream.of(agendamento, recorrencia)
                .filter(Objects::nonNull)
                .flatMap(dto -> dto.getParcelas().stream())
                .map(RecorrenteParcelaRequisicaoDTO::getIdentificadorParcela)
                .toList();
    }
}
