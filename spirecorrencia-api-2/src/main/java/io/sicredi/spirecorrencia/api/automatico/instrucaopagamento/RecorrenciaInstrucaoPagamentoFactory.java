package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import br.com.sicredi.spi.dto.Pain013Dto;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusInstrucaoPagamento;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
class RecorrenciaInstrucaoPagamentoFactory {

    static RecorrenciaInstrucaoPagamento criar(Pain013Dto pain013Dto) {
        var recorrenciaInstrucaoPagamento = new RecorrenciaInstrucaoPagamento();
        recorrenciaInstrucaoPagamento.setCodFimAFim(pain013Dto.getIdFimAFim());
        recorrenciaInstrucaoPagamento.setIdRecorrencia(pain013Dto.getIdRecorrencia());
        recorrenciaInstrucaoPagamento.setNumCpfCnpjPagador(pain013Dto.getCpfCnpjUsuarioPagador());
        recorrenciaInstrucaoPagamento.setNumInstituicaoPagador(pain013Dto.getParticipanteDoUsuarioPagador());
        recorrenciaInstrucaoPagamento.setTxtNomeDevedor(pain013Dto.getNomeDevedor());
        recorrenciaInstrucaoPagamento.setNumCpfCnpjDevedor(pain013Dto.getCpfCnpjDevedor());
        recorrenciaInstrucaoPagamento.setNumValor(pain013Dto.getValor());
        recorrenciaInstrucaoPagamento.setNumCpfCnpjRecebedor(pain013Dto.getCpfCnpjUsuarioRecebedor());
        recorrenciaInstrucaoPagamento.setNumAgenciaRecebedor(pain013Dto.getAgenciaUsuarioRecebedor());
        recorrenciaInstrucaoPagamento.setNumContaRecebedor(pain013Dto.getContaUsuarioRecebedor());
        recorrenciaInstrucaoPagamento.setNumInstituicaoRecebedor(pain013Dto.getParticipanteDoUsuarioRecebedor());
        recorrenciaInstrucaoPagamento.setTpoContaRecebedor(pain013Dto.getTipoContaUsuarioRecebedor());
        recorrenciaInstrucaoPagamento.setIdConciliacaoRecebedor(pain013Dto.getIdConciliacaoRecebedor());
        recorrenciaInstrucaoPagamento.setTpoStatus(TipoStatusInstrucaoPagamento.CRIADA.name());
        recorrenciaInstrucaoPagamento.setTpoFinalidadeAgendamento(pain013Dto.getFinalidadeDoAgendamento());
        recorrenciaInstrucaoPagamento.setTxtInformacoesEntreUsuarios(pain013Dto.getInformacoesEntreUsuarios());
        recorrenciaInstrucaoPagamento.setDatVencimento(pain013Dto.getDataVencimento());
        recorrenciaInstrucaoPagamento.setDatEmissao(pain013Dto.getDataHoraCriacaoParaEmissao());
        recorrenciaInstrucaoPagamento.setDatCriacaoRegistro(LocalDateTime.now());
        recorrenciaInstrucaoPagamento.setDatAlteracaoRegistro(LocalDateTime.now());
        recorrenciaInstrucaoPagamento.setDatConfirmacao(LocalDateTime.now());
        return recorrenciaInstrucaoPagamento;
    }
}