package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.CadastroOrdemRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoLiquidacao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.participante.ParticipanteService;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.RecorrenciaTestFactory.criarRecorrencia;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessamentoLiquidacaoServiceTest {

    @Mock
    private SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;
    @Mock
    private RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;
    @Mock
    private ParticipanteService participanteService;
    @InjectMocks
    private ProcessamentoLiquidacaoService processamentoLiquidacaoService;
    @Captor
    private ArgumentCaptor<CadastroOrdemRequest> captureCadastroOrdemRequest;

    private RecorrenciaTransacao recorrenciaTransacaoMock;

    private TipoProcessamentoWrapperDTO tipoProcessamentoMock;

    private static final String TIPO_PRODUTO = "AGENDADO_RECORRENTE";
    private static final String PRIORIDADE = "NORMAL";
    private static final String ID_FIM_A_FIM = "E91586982202208151245099rD6AIAa7";

    @BeforeEach
    void beforeEach() {
        var recorrenciaMock = criarRecorrencia(LocalDateTime.now());
        recorrenciaMock.getRecebedor().setInstituicao(RandomStringUtils.randomNumeric(8,8));
        recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        tipoProcessamentoMock = TestFactory.TipoProcessamentoWrapperDTOTestFactory.liquidacao(recorrenciaTransacaoMock);
    }

    @Test
    void dadoProcessamentoValido_quandoProcessarLiquidacaoLiquidacao_deveRealizarComSucesso() {
        processamentoLiquidacaoService.processarLiquidacao(tipoProcessamentoMock);

        verify(recorrenciaTransacaoRepository).updateStateAndIdFimAFim(eq(recorrenciaTransacaoMock.getOidRecorrenciaTransacao()), eq(TipoStatusEnum.PENDENTE), eq(ID_FIM_A_FIM), any(LocalDateTime.class));
        verify(spiCanaisProtocoloApiClient).emitirProtocoloLiquidacaoRecorrencia(eq(recorrenciaTransacaoMock.getRecorrencia().getTipoCanal()), eq(TipoLiquidacao.AGENDADO_RECORRENTE), captureCadastroOrdemRequest.capture());

        var valueCadastroOrdemRequest = captureCadastroOrdemRequest.getValue();

        validarDadosRequest(valueCadastroOrdemRequest, recorrenciaTransacaoMock);
    }

    @Test
    void dadoExcecao_quandoProcessarLiquidacaoLiquidacao_deveLancarTechnicalException() {
        doThrow(RuntimeException.class).when(spiCanaisProtocoloApiClient).emitirProtocoloLiquidacaoRecorrencia(any(), any(), any());

        assertThrows(TechnicalException.class, () -> processamentoLiquidacaoService.processarLiquidacao(tipoProcessamentoMock));

        verify(recorrenciaTransacaoRepository).updateStateAndIdFimAFim(eq(recorrenciaTransacaoMock.getOidRecorrenciaTransacao()), eq(TipoStatusEnum.PENDENTE), eq(ID_FIM_A_FIM), any(LocalDateTime.class));
        verify(spiCanaisProtocoloApiClient).emitirProtocoloLiquidacaoRecorrencia(eq(recorrenciaTransacaoMock.getRecorrencia().getTipoCanal()), eq(TipoLiquidacao.AGENDADO_RECORRENTE), captureCadastroOrdemRequest.capture());

        var valueCadastroOrdemRequest = captureCadastroOrdemRequest.getValue();

        validarDadosRequest(valueCadastroOrdemRequest, recorrenciaTransacaoMock);

    }

    private void validarDadosRequest(CadastroOrdemRequest valueCadastroOrdemRequest, RecorrenciaTransacao recorrenciaTransacaoMock) {
        var pagador = recorrenciaTransacaoMock.getRecorrencia().getPagador();
        var recebedor = recorrenciaTransacaoMock.getRecorrencia().getRecebedor();

        verify(participanteService, times(1)).buscarNomeInstituicao(pagador.getInstituicao());
        verify(participanteService, times(1)).buscarNomeInstituicao(recebedor.getInstituicao());

        assertAll(
                //IdentificadorAssociado
                () -> assertEquals(pagador.getTipoConta().getTipoContaCanaisDigitais(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getTipoConta()),
                () -> assertEquals(pagador.getCpfCnpj(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getCpfUsuario()),
                () -> assertEquals(pagador.getCpfCnpj(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getCpfCnpjConta()),
                () -> assertEquals(pagador.getConta(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getConta()),
                () -> assertEquals(pagador.getCodPosto(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getAgencia()),
                () -> assertEquals(pagador.getNome(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getNomeAssociadoConta()),
                () -> assertEquals(pagador.getNome(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getNomeUsuario()),
                () -> assertEquals(pagador.getAgencia(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getCooperativa()),
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getTipoOrigemSistema(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getOrigemConta()),

                //Participante
                () -> assertEquals(pagador.getInstituicao(), valueCadastroOrdemRequest.getParticipantePagador().getIspb()),
                () -> assertEquals(recebedor.getInstituicao(), valueCadastroOrdemRequest.getParticipanteRecebedor().getIspb()),

                //CadastroOrdemRequest
                () -> assertEquals(PRIORIDADE, valueCadastroOrdemRequest.getPrioridadePagamento()),
                () -> assertEquals(recebedor.getChave(), valueCadastroOrdemRequest.getChaveDict()),
                () -> assertEquals(pagador.getCpfCnpj(), valueCadastroOrdemRequest.getCpfCnpjAssociado()),
                () -> assertEquals(recebedor.getAgencia(), valueCadastroOrdemRequest.getAgenciaUsuarioRecebedor()),
                () -> assertEquals(recebedor.getConta(), valueCadastroOrdemRequest.getContaUsuarioRecebedor()),
                () -> assertEquals(recebedor.getCpfCnpj(), valueCadastroOrdemRequest.getCpfCnpjUsuarioRecebedor()),
                () -> assertEquals(recebedor.getNome(), valueCadastroOrdemRequest.getNomeUsuarioRecebedor()),
                () -> assertEquals(recebedor.getTipoConta(), valueCadastroOrdemRequest.getTipoContaUsuarioRecebedor()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdConciliacaoRecebedor(), valueCadastroOrdemRequest.getIdConciliacaoRecebedor()),
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getNumInicCnpj(), valueCadastroOrdemRequest.getNumInicCnpj()),
                () -> assertEquals(recorrenciaTransacaoMock.getValor(), valueCadastroOrdemRequest.getValor()),
                () -> assertEquals(TIPO_PRODUTO, valueCadastroOrdemRequest.getTipoProduto()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdFimAFim(), valueCadastroOrdemRequest.getIdentificadorTransacao())
        );
    }

}