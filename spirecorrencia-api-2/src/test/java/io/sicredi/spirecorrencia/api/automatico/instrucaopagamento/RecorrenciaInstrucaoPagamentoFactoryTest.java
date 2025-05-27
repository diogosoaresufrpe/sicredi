package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusInstrucaoPagamento;
import org.junit.jupiter.api.Test;

import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.RecorrenciaInstrucaoPagamentoTestFactory.PAIN013_AGENDAMENTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RecorrenciaInstrucaoPagamentoFactoryTest {

    @Test
    void dadoPain013DTO_quandoCriar_deveRetornarRecorrenciaInstrucaoPagamentoComSucesso() {
        var pain013Dto = PAIN013_AGENDAMENTO;

        var instrucao = RecorrenciaInstrucaoPagamentoFactory.criar(pain013Dto);

        assertNotNull(instrucao);
        assertEquals(pain013Dto.getIdFimAFim(), instrucao.getCodFimAFim());
        assertEquals(pain013Dto.getIdRecorrencia(), instrucao.getIdRecorrencia());
        assertEquals(pain013Dto.getCpfCnpjUsuarioPagador(), instrucao.getNumCpfCnpjPagador());
        assertEquals(pain013Dto.getParticipanteDoUsuarioPagador(), instrucao.getNumInstituicaoPagador());
        assertEquals(pain013Dto.getNomeDevedor(), instrucao.getTxtNomeDevedor());
        assertEquals(pain013Dto.getCpfCnpjDevedor(), instrucao.getNumCpfCnpjDevedor());
        assertEquals(pain013Dto.getValor(), instrucao.getNumValor());
        assertEquals(pain013Dto.getCpfCnpjUsuarioRecebedor(), instrucao.getNumCpfCnpjRecebedor());
        assertEquals(pain013Dto.getAgenciaUsuarioRecebedor(), instrucao.getNumAgenciaRecebedor());
        assertEquals(pain013Dto.getContaUsuarioRecebedor(), instrucao.getNumContaRecebedor());
        assertEquals(pain013Dto.getParticipanteDoUsuarioRecebedor(), instrucao.getNumInstituicaoRecebedor());
        assertEquals(pain013Dto.getTipoContaUsuarioRecebedor(), instrucao.getTpoContaRecebedor());
        assertEquals(pain013Dto.getIdConciliacaoRecebedor(), instrucao.getIdConciliacaoRecebedor());
        assertEquals(TipoStatusInstrucaoPagamento.CRIADA.name(), instrucao.getTpoStatus());
        assertEquals(pain013Dto.getFinalidadeDoAgendamento(), instrucao.getTpoFinalidadeAgendamento());
        assertEquals(pain013Dto.getInformacoesEntreUsuarios(), instrucao.getTxtInformacoesEntreUsuarios());
        assertEquals(pain013Dto.getDataVencimento(), instrucao.getDatVencimento());
        assertEquals(pain013Dto.getDataHoraCriacaoParaEmissao(), instrucao.getDatEmissao());
        assertNotNull(instrucao.getDatCriacaoRegistro());
        assertNotNull(instrucao.getDatAlteracaoRegistro());
    }
}