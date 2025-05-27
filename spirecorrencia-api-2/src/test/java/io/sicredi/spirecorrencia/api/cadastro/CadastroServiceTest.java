package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.framework.exception.BusinessException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaProtocoloRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.sicredi.spirecorrencia.api.repositorio.Recebedor;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import jakarta.validation.Validator;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.IDENTIFICADOR_TRANSACAO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CadastroServiceTest {

    private static final String ISPB_PAGADOR = "00000000";
    private static final String ISPB_RECEBEDOR = "99999999";
    private static final String NOME_RECORRENCIA = "Nome Recorrencia";
    private static final String NOME_PAGADOR = "Nome Pagador";
    private static final String COOP_PAGADOR = "0000";
    private static final String CONTA_PAGADOR = "000000";
    private static final String POSTO_PAGADOR = "00";
    private static final String NOME_RECEBEDOR = "Nome Recebedor";
    private static final String COOP_RECEBEDOR = "9999";
    private static final String CONTA_RECEBEDOR = "999999";
    private static final String INFORMACOES_ENTRE_USUARIOS = "Informacoes Entre Usuarios";
    private static final String CADASTRO_INTEGRADO = "CADASTRO_INTEGRADO";
    private static final String TOPICO = "topico";

    @InjectMocks
    private CadastroService cadastroService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;
    @Mock
    private Validator validator;
    @Mock
    private RecorrenciaRepository recorrenciaRepository;
    @Mock
    private PagadorRepository pagadorRepository;
    @Mock
    private RecebedorRepository recebedorRepository;
    @Mock
    private SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;
    @Mock
    private CriaResponseStrategyFactory<CadastroRequest> criaResponseStrategyFactory;
    @Mock
    private CriaResponseStrategy<CadastroRequest> criaResponseStrategy;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Mock
    private IdempotentRequest<CadastroRequestWrapper> idempotentRequest;
    @Captor
    private ArgumentCaptor<Recorrencia> captorRecorrencia;
    @Captor
    private ArgumentCaptor<Pagador> captorPagador;
    @Captor
    private ArgumentCaptor<Recebedor> captorRecebedor;
    @Captor
    private ArgumentCaptor<ErroDTO> captorErro;
    @Captor
    private ArgumentCaptor<CadastroRecorrenciaProtocoloRequest> captureCadastroIntegradoRequest;
    @Captor
    private ArgumentCaptor<NotificacaoDTO> captureNotificacaoDTO;

    @Nested
    class ProcessarAgendamento {

        @Test
        @DisplayName("Deve processar protocolo de agendamento unico com sucesso")
        void dadoAgendadoUnico_quandoProcessarProtocolo_deveExecutarCadastroAgendamento() {
            var cadastroRequest = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2))));
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            cadastroRequest.setTipoRecorrencia(TipoRecorrencia.AGENDADO);

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarAgendamento(cadastroRequest);

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            cadastroService.processarAgendamento(idempotentRequest);

            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            verify(eventoResponseFactory, never()).criarEventoNotificacao(captureNotificacaoDTO.capture());
            verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCadastroRecorrenciaIntegrada(eq(cadastroRequest.getTipoCanal()), eq(CADASTRO_INTEGRADO), captureCadastroIntegradoRequest.capture());

            var valueRecorrencia = captorRecorrencia.getValue();
            var valuePagador = valueRecorrencia.getPagador();
            var valueRecebedor = valueRecorrencia.getRecebedor();
            var valueParcelas = valueRecorrencia.getRecorrencias();

            assertAll(
                    //Recorrencia
                    () -> assertEquals(cadastro.getAgendamento().getNome(), valueRecorrencia.getNome()),
                    () -> assertEquals(cadastro.getAgendamento().getTipoCanal(), valueRecorrencia.getTipoCanal()),
                    () -> assertEquals(cadastro.getAgendamento().getTipoOrigemSistema(), valueRecorrencia.getTipoOrigemSistema()),
                    () -> assertEquals(cadastro.getAgendamento().getTipoIniciacao(), valueRecorrencia.getTipoIniciacao()),
                    () -> assertEquals(TipoStatusEnum.CRIADO, valueRecorrencia.getTipoStatus()),
                    () -> assertNotNull(valueRecorrencia.getDataCriacao()),
                    () -> assertEquals(cadastro.getAgendamento().getTipoIniciacaoCanal(), valueRecorrencia.getTipoIniciacaoCanal()),
                    () -> assertEquals(cadastro.getAgendamento().getTipoFrequencia(), valueRecorrencia.getTipoFrequencia()),
                    () -> assertEquals(cadastro.getAgendamento().getNumInicCnpj(), valueRecorrencia.getNumInicCnpj()),
                    () -> assertEquals(cadastro.getAgendamento().getTipoRecorrencia(), valueRecorrencia.getTipoRecorrencia()),
                    //Pagador
                    () -> assertNotNull(valueRecorrencia.getPagador()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getCpfCnpj(), valuePagador.getCpfCnpj()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getNome(), valuePagador.getNome()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getInstituicao(), valuePagador.getInstituicao()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getAgencia(), valuePagador.getAgencia()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getConta(), valuePagador.getConta()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getPosto(), valuePagador.getCodPosto()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getTipoConta(), valuePagador.getTipoConta()),
                    () -> assertEquals(cadastro.getAgendamento().getPagador().getTipoPessoa(), valuePagador.getTipoPessoa()),
                    //Recebedor
                    () -> assertNotNull(valueRecorrencia.getRecebedor()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getCpfCnpj(), valueRecebedor.getCpfCnpj()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getNome(), valueRecebedor.getNome()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getAgencia(), valueRecebedor.getAgencia()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getConta(), valueRecebedor.getConta()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getInstituicao(), valueRecebedor.getInstituicao()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getTipoConta(), valueRecebedor.getTipoConta()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getTipoPessoa(), valueRecebedor.getTipoPessoa()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getTipoChave(), valueRecebedor.getTipoChave()),
                    () -> assertEquals(cadastro.getAgendamento().getRecebedor().getChave(), valueRecebedor.getChave()),
                    //Parcela 1
                    () -> assertNotNull(valueParcelas),
                    () -> assertEquals(1, valueParcelas.size()),
                    () -> assertNotNull(valueParcelas.getFirst().getRecorrencia()),
                    () -> assertNull(valueParcelas.getFirst().getTipoMotivoExclusao()),
                    () -> assertEquals(TipoStatusEnum.CRIADO, valueParcelas.getFirst().getTpoStatus()),
                    () -> assertEquals(cadastro.getAgendamento().getParcelas().getFirst().getIdentificadorParcela(), valueParcelas.getFirst().getIdParcela()),
                    () -> assertEquals(cadastro.getAgendamento().getParcelas().getFirst().getIdFimAFim(), valueParcelas.getFirst().getIdFimAFim()),
                    () -> assertEquals(cadastro.getAgendamento().getParcelas().getFirst().getValor(), valueParcelas.getFirst().getValor()),
                    () -> assertEquals(Boolean.FALSE, valueParcelas.getFirst().getNotificadoDiaAnterior()),
                    () -> assertEquals(cadastro.getAgendamento().getParcelas().getFirst().getDataTransacao().toLocalDate(), valueParcelas.getFirst().getDataTransacao()),
                    () -> assertEquals(cadastro.getAgendamento().getParcelas().getFirst().getInformacoesEntreUsuarios(), valueParcelas.getFirst().getInformacoesEntreUsuarios()),
                    () -> assertEquals(cadastro.getAgendamento().getParcelas().getFirst().getIdConciliacaoRecebedor(), valueParcelas.getFirst().getIdConciliacaoRecebedor()),
                    () -> assertNotNull(valueParcelas.getFirst().getDataCriacaoRegistro())
            );

        }

        @Test
        @DisplayName("Deve processar protocolo de agendamento unico com sucesso e enviar protocolo do cadastro de recorrencia integrada")
        void dadoAgendadoComRecorrencia_quandoProcessarProtocolo_deveExecutarCadastroEEnviarProtocoloIntegradoComSucesso() {
            var agendado = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2))));
            var recorrente = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2))));

            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            agendado.setTipoRecorrencia(TipoRecorrencia.AGENDADO);
            recorrente.setTipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE);

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarAgendamentoComRecorrencia(agendado, recorrente);

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            cadastroService.processarAgendamento(idempotentRequest);

            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            verify(spiCanaisProtocoloApiClient, times(1)).emitirProtocoloCadastroRecorrenciaIntegrada(eq(recorrente.getTipoCanal()), eq(CADASTRO_INTEGRADO), captureCadastroIntegradoRequest.capture());

            var valueParcelas = captorRecorrencia.getValue().getRecorrencias();

            assertAll(
                    () -> assertNotNull(valueParcelas),
                    () -> assertEquals(1, valueParcelas.size())
            );

            var valueCadastroIntegradoRequest = captureCadastroIntegradoRequest.getValue();
            var valueRequestPagador = valueCadastroIntegradoRequest.getPagador();
            var valueRequestRecebedor = valueCadastroIntegradoRequest.getRecebedor();
            var valueRequestParcelas = valueCadastroIntegradoRequest.getParcelas();
            var valueRequestIdentificacaoAssociado = valueCadastroIntegradoRequest.getIdentificacaoAssociado();

            assertAll(
                    // Recorrencia
                    () -> assertNotNull(valueCadastroIntegradoRequest.getPagador()),
                    () -> assertNotNull(valueCadastroIntegradoRequest.getRecebedor()),
                    () -> assertNotNull(valueCadastroIntegradoRequest.getParcelas()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getIdentificadorRecorrencia(), valueCadastroIntegradoRequest.getIdentificadorRecorrencia()),
                    () -> assertEquals(recorrente.getNome(), valueCadastroIntegradoRequest.getNome()),
                    () -> assertEquals(recorrente.getTipoIniciacao(), valueCadastroIntegradoRequest.getTipoIniciacao()),
                    () -> assertEquals(recorrente.getTipoCanal(), valueCadastroIntegradoRequest.getTipoCanal()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getDataTransacao().toLocalDate(), valueCadastroIntegradoRequest.getDataPrimeiraParcela()),
                    () -> assertEquals(recorrente.getTipoIniciacaoCanal(), valueCadastroIntegradoRequest.getTipoIniciacaoCanal()),
                    () -> assertEquals(recorrente.getTipoFrequencia(), valueCadastroIntegradoRequest.getTipoFrequencia()),
                    () -> assertEquals(TipoRecorrencia.AGENDADO_RECORRENTE, valueCadastroIntegradoRequest.getTipoRecorrencia()),
                    // Pagador
                    () -> assertEquals(recorrente.getPagador().getCpfCnpj(), valueRequestPagador.getCpfCnpj()),
                    () -> assertEquals(recorrente.getPagador().getNome(), valueRequestPagador.getNome()),
                    () -> assertEquals(recorrente.getPagador().getInstituicao(), valueRequestPagador.getInstituicao()),
                    () -> assertEquals(recorrente.getPagador().getAgencia(), valueRequestPagador.getAgencia()),
                    () -> assertEquals(recorrente.getPagador().getConta(), valueRequestPagador.getConta()),
                    () -> assertEquals(recorrente.getPagador().getPosto(), valueRequestPagador.getPosto()),
                    () -> assertEquals(recorrente.getPagador().getTipoConta(), valueRequestPagador.getTipoConta()),
                    () -> assertEquals(recorrente.getPagador().getTipoPessoa(), valueRequestPagador.getTipoPessoa()),
                    //Recebedor
                    () -> assertEquals(recorrente.getRecebedor().getCpfCnpj(), valueRequestRecebedor.getCpfCnpj()),
                    () -> assertEquals(recorrente.getRecebedor().getNome(), valueRequestRecebedor.getNome()),
                    () -> assertEquals(recorrente.getRecebedor().getAgencia(), valueRequestRecebedor.getAgencia()),
                    () -> assertEquals(recorrente.getRecebedor().getConta(), valueRequestRecebedor.getConta()),
                    () -> assertEquals(recorrente.getRecebedor().getInstituicao(), valueRequestRecebedor.getInstituicao()),
                    () -> assertEquals(recorrente.getRecebedor().getTipoConta(), valueRequestRecebedor.getTipoConta()),
                    () -> assertEquals(recorrente.getRecebedor().getTipoPessoa(), valueRequestRecebedor.getTipoPessoa()),
                    () -> assertEquals(recorrente.getRecebedor().getTipoChave(), valueRequestRecebedor.getTipoChave()),
                    () -> assertEquals(recorrente.getRecebedor().getChave(), valueRequestRecebedor.getChave()),
                    // Parcela
                    () -> assertEquals(2, valueRequestParcelas.size()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getValor(), valueRequestParcelas.getFirst().getValor()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getDataTransacao(), valueRequestParcelas.getFirst().getDataTransacao()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getIdFimAFim(), valueRequestParcelas.getFirst().getIdFimAFim()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getInformacoesEntreUsuarios(), valueRequestParcelas.getFirst().getInformacoesEntreUsuarios()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getIdConciliacaoRecebedor(), valueRequestParcelas.getFirst().getIdConciliacaoRecebedor()),
                    () -> assertEquals(recorrente.getParcelas().getFirst().getIdentificadorParcela(), valueRequestParcelas.getFirst().getIdentificadorParcela()),
                    //Dados gerais do associado - Comprovante
                    () -> assertEquals(recorrente.getPagador().getTipoConta().getTipoContaCanaisDigitais(), valueRequestIdentificacaoAssociado.getTipoConta()),
                    () -> assertEquals(recorrente.getPagador().getCpfCnpj(), valueRequestIdentificacaoAssociado.getCpfUsuario()),
                    () -> assertEquals(recorrente.getPagador().getCpfCnpj(), valueRequestIdentificacaoAssociado.getCpfCnpjConta()),
                    () -> assertEquals(recorrente.getPagador().getConta(), valueRequestIdentificacaoAssociado.getConta()),
                    () -> assertEquals(recorrente.getPagador().getPosto(), valueRequestIdentificacaoAssociado.getAgencia()),
                    () -> assertEquals(recorrente.getPagador().getNome(), valueRequestIdentificacaoAssociado.getNomeAssociadoConta()),
                    () -> assertEquals(recorrente.getPagador().getNome(), valueRequestIdentificacaoAssociado.getNomeUsuario()),
                    () -> assertEquals(recorrente.getPagador().getAgencia(), valueRequestIdentificacaoAssociado.getCooperativa())
            );

        }

        @Test
        @DisplayName("Deve retornar erro de validação de negocio quando existe ocorrer erro de negocio ao emitir protocolo de recorrencia integrada")
        void dadoProtocoloRecorrenciaIntegradaComErroNegocio_quandoProcessarProtocolo_deveRetornarErroValidacaoNegocio() {
            var recorrente = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2))));
            var agendado = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2))));

            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            agendado.setTipoRecorrencia(TipoRecorrencia.AGENDADO);
            recorrente.setTipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE);

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarAgendamentoComRecorrencia(agendado, recorrente);

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(spiCanaisProtocoloApiClient.emitirProtocoloCadastroRecorrenciaIntegrada(any(), any(), any())).thenThrow(new BusinessException("ERRO"));

            cadastroService.processarAgendamento(idempotentRequest);
            verify(eventoResponseFactory).criarEventoNotificacao(
                    argThat(notificacao -> NotificacaoDTO.TipoTemplate.RECORRENCIA_CADASTRO_FALHA == notificacao.getOperacao())
            );
        }

    }

    @Nested
    class ProcessarRecorrencia {

        @Test
        @DisplayName("Deve processar protocolo com sucesso quando não há erros de validação")
        void dadoRequestValido_quandoProcessarProtocolo_deveExecutarComSucesso() {
            var cadastroRequest = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(3))));
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);
            var recorrencia = cadastro.getRecorrencia();

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(validator, times(1)).validate(any());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(), any());
            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            verify(pagadorRepository, times(1)).findByCpfCnpjAndAgenciaAndContaAndTipoConta(recorrencia.getPagador().getCpfCnpj(), recorrencia.getPagador().getAgencia(), recorrencia.getPagador().getConta(), recorrencia.getPagador().getTipoConta());
            verify(recebedorRepository, times(1)).buscarRecebedor(recorrencia.getRecebedor());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
            verify(eventoResponseFactory, times(1)).criarEventoNotificacao(captureNotificacaoDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
            verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCadastroRecorrenciaIntegrada(any(), any(), any());

            var valueRecorrencia = captorRecorrencia.getValue();
            var valuePagador = valueRecorrencia.getPagador();
            var valueRecebedor = valueRecorrencia.getRecebedor();
            var valueParcelas = valueRecorrencia.getRecorrencias();
            var valueNotificacao = captureNotificacaoDTO.getValue();

            assertAll(
                    //Recorrencia
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getIdentificadorRecorrencia(), valueRecorrencia.getIdRecorrencia()),
                    () -> assertEquals(recorrencia.getNome(), valueRecorrencia.getNome()),
                    () -> assertEquals(recorrencia.getTipoCanal(), valueRecorrencia.getTipoCanal()),
                    () -> assertEquals(recorrencia.getTipoOrigemSistema(), valueRecorrencia.getTipoOrigemSistema()),
                    () -> assertEquals(recorrencia.getTipoIniciacao(), valueRecorrencia.getTipoIniciacao()),
                    () -> assertEquals(TipoStatusEnum.CRIADO, valueRecorrencia.getTipoStatus()),
                    () -> assertNotNull(valueRecorrencia.getDataCriacao()),
                    () -> assertEquals(recorrencia.getTipoIniciacaoCanal(), valueRecorrencia.getTipoIniciacaoCanal()),
                    () -> assertEquals(recorrencia.getTipoFrequencia(), valueRecorrencia.getTipoFrequencia()),
                    () -> assertEquals(recorrencia.getNumInicCnpj(), valueRecorrencia.getNumInicCnpj()),
                    () -> assertEquals(recorrencia.getTipoRecorrencia(), valueRecorrencia.getTipoRecorrencia()),
                    //Pagador
                    () -> assertNotNull(valueRecorrencia.getPagador()),
                    () -> assertEquals(recorrencia.getPagador().getCpfCnpj(), valuePagador.getCpfCnpj()),
                    () -> assertEquals(recorrencia.getPagador().getNome(), valuePagador.getNome()),
                    () -> assertEquals(recorrencia.getPagador().getInstituicao(), valuePagador.getInstituicao()),
                    () -> assertEquals(recorrencia.getPagador().getAgencia(), valuePagador.getAgencia()),
                    () -> assertEquals(recorrencia.getPagador().getConta(), valuePagador.getConta()),
                    () -> assertEquals(recorrencia.getPagador().getPosto(), valuePagador.getCodPosto()),
                    () -> assertEquals(recorrencia.getPagador().getTipoConta(), valuePagador.getTipoConta()),
                    () -> assertEquals(recorrencia.getPagador().getTipoPessoa(), valuePagador.getTipoPessoa()),
                    //Recebedor
                    () -> assertNotNull(valueRecorrencia.getRecebedor()),
                    () -> assertEquals(recorrencia.getRecebedor().getCpfCnpj(), valueRecebedor.getCpfCnpj()),
                    () -> assertEquals(recorrencia.getRecebedor().getNome(), valueRecebedor.getNome()),
                    () -> assertEquals(recorrencia.getRecebedor().getAgencia(), valueRecebedor.getAgencia()),
                    () -> assertEquals(recorrencia.getRecebedor().getConta(), valueRecebedor.getConta()),
                    () -> assertEquals(recorrencia.getRecebedor().getInstituicao(), valueRecebedor.getInstituicao()),
                    () -> assertEquals(recorrencia.getRecebedor().getTipoConta(), valueRecebedor.getTipoConta()),
                    () -> assertEquals(recorrencia.getRecebedor().getTipoPessoa(), valueRecebedor.getTipoPessoa()),
                    () -> assertEquals(recorrencia.getRecebedor().getTipoChave(), valueRecebedor.getTipoChave()),
                    () -> assertEquals(recorrencia.getRecebedor().getChave(), valueRecebedor.getChave()),
                    //Parcela 1
                    () -> assertNotNull(valueParcelas),
                    () -> assertEquals(2, valueParcelas.size()),
                    () -> assertNotNull(valueParcelas.getFirst().getRecorrencia()),
                    () -> assertNull(valueParcelas.getFirst().getTipoMotivoExclusao()),
                    () -> assertEquals(TipoStatusEnum.CRIADO, valueParcelas.getFirst().getTpoStatus()),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getIdentificadorParcela(), valueParcelas.getFirst().getIdParcela()),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getIdFimAFim(), valueParcelas.getFirst().getIdFimAFim()),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getValor(), valueParcelas.getFirst().getValor()),
                    () -> assertEquals(Boolean.FALSE, valueParcelas.getFirst().getNotificadoDiaAnterior()),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getDataTransacao().toLocalDate(), valueParcelas.getFirst().getDataTransacao()),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getInformacoesEntreUsuarios(), valueParcelas.getFirst().getInformacoesEntreUsuarios()),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getIdConciliacaoRecebedor(), valueParcelas.getFirst().getIdConciliacaoRecebedor()),
                    () -> assertNotNull(valueParcelas.getFirst().getDataCriacaoRegistro()),
                    //Parcela 2
                    () -> assertNotNull(valueParcelas.get(1).getRecorrencia()),
                    () -> assertNull(valueParcelas.get(1).getTipoMotivoExclusao()),
                    () -> assertEquals(TipoStatusEnum.CRIADO, valueParcelas.get(1).getTpoStatus()),
                    () -> assertEquals(recorrencia.getParcelas().get(1).getIdentificadorParcela(), valueParcelas.get(1).getIdParcela()),
                    () -> assertEquals(recorrencia.getParcelas().get(1).getIdFimAFim(), valueParcelas.get(1).getIdFimAFim()),
                    () -> assertEquals(recorrencia.getParcelas().get(1).getValor(), valueParcelas.get(1).getValor()),
                    () -> assertEquals(Boolean.FALSE, valueParcelas.get(1).getNotificadoDiaAnterior()),
                    () -> assertEquals(recorrencia.getParcelas().get(1).getDataTransacao().toLocalDate(), valueParcelas.get(1).getDataTransacao()),
                    () -> assertEquals(recorrencia.getParcelas().get(1).getInformacoesEntreUsuarios(), valueParcelas.get(1).getInformacoesEntreUsuarios()),
                    () -> assertEquals(recorrencia.getParcelas().get(1).getIdConciliacaoRecebedor(), valueParcelas.get(1).getIdConciliacaoRecebedor()),
                    () -> assertNotNull(valueParcelas.get(1).getDataCriacaoRegistro()),
                    //Notificacao
                    () -> assertEquals(recorrencia.getPagador().getAgencia(), valueNotificacao.getAgencia()),
                    () -> assertEquals(recorrencia.getPagador().getConta(), valueNotificacao.getConta()),
                    () -> assertEquals(recorrencia.getRecebedor().getChave(), valueNotificacao.getChave()),
                    () -> assertEquals(recorrencia.getRecebedor().getTipoChave().name(), valueNotificacao.getTipoChave()),
                    () -> assertEquals(NotificacaoDTO.TipoTemplate.RECORRENCIA_CADASTRO_SUCESSO, valueNotificacao.getOperacao()),
                    () -> assertEquals(recorrencia.getTipoCanal().getTipoCanalPix().name(), valueNotificacao.getCanal()),
                    () -> assertEquals(6, valueNotificacao.getInformacoesAdicionais().size()),
                    () -> assertEquals(recorrencia.getRecebedor().getNome(), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.NOME_RECEBEDOR.getNomeVariavel())),
                    () -> assertEquals(recorrencia.getTipoFrequencia().getTituloPlural().toLowerCase(Locale.ROOT), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.FREQUENCIA.getNomeVariavel())),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getValor().toString(), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.VALOR.getNomeVariavel())),
                    () -> assertEquals(String.valueOf(recorrencia.getParcelas().size()), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.QUANTIDADE_PARCELAS.getNomeVariavel())),
                    () -> assertEquals(String.valueOf(recorrencia.getPagador().getCpfCnpj()), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR.getNomeVariavel()))
            );
        }

        @Test
        @DisplayName("Deve processar protocolo com sucesso e atualizar o registro do pagador quando os dados do pagador necessitarem atualizacao")
        void dadoPagadorComDadosAtualizado_quandoProcessarProtocolo_deveExecutarComSucessoEAtualizarRegistroPagador() {
            var recorrentePagadorDTO = criarRecorrentePagadorDTO();

            var pagadorEntidade = Pagador.builder()
                    .cpfCnpj(recorrentePagadorDTO.getCpfCnpj())
                    .nome(recorrentePagadorDTO.getNome())
                    .instituicao(recorrentePagadorDTO.getInstituicao())
                    .agencia(recorrentePagadorDTO.getAgencia())
                    .conta(recorrentePagadorDTO.getConta())
                    .codPosto(recorrentePagadorDTO.getPosto())
                    .tipoConta(recorrentePagadorDTO.getTipoConta())
                    .tipoPessoa(recorrentePagadorDTO.getTipoPessoa())
                    .dataCriacaoRegistro(LocalDateTime.now())
                    .build();

            recorrentePagadorDTO.setNome("Nome Atualizado");
            recorrentePagadorDTO.setInstituicao("55555555");
            recorrentePagadorDTO.setPosto("AA");

            var cadastroRequest = criarRecorrenteRequisicaoDTO(recorrentePagadorDTO, criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(3))));

            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);

            mockPagadorRecebedorRepository(pagadorEntidade, Optional.empty());

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(pagadorRepository, times(1)).save(captorPagador.capture());

            var valuePagador = captorPagador.getValue();

            assertAll(
                    //Pagador
                    () -> assertEquals(pagadorEntidade.getCpfCnpj(), valuePagador.getCpfCnpj()),
                    () -> assertEquals(recorrentePagadorDTO.getNome(), valuePagador.getNome()),
                    () -> assertEquals(pagadorEntidade.getInstituicao(), valuePagador.getInstituicao()),
                    () -> assertEquals(pagadorEntidade.getAgencia(), valuePagador.getAgencia()),
                    () -> assertEquals(pagadorEntidade.getConta(), valuePagador.getConta()),
                    () -> assertEquals(recorrentePagadorDTO.getPosto(), valuePagador.getCodPosto()),
                    () -> assertEquals(pagadorEntidade.getTipoConta(), valuePagador.getTipoConta()),
                    () -> assertEquals(pagadorEntidade.getTipoPessoa(), valuePagador.getTipoPessoa())
            );
        }

        @Test
        void dadoRecebedorComDadosAtualizado_quandoProcessarRecorrencia_deveExecutarComSucesso() {
            var recorrenteRecebedorDTO = criarRecorrenteRecebedorDTO();

            var recebedorEntidade = Recebedor.builder()
                    .cpfCnpj(recorrenteRecebedorDTO.getCpfCnpj())
                    .nome(recorrenteRecebedorDTO.getNome())
                    .agencia(recorrenteRecebedorDTO.getAgencia())
                    .conta(recorrenteRecebedorDTO.getConta())
                    .instituicao(recorrenteRecebedorDTO.getInstituicao())
                    .tipoConta(recorrenteRecebedorDTO.getTipoConta())
                    .tipoPessoa(recorrenteRecebedorDTO.getTipoPessoa())
                    .tipoChave(recorrenteRecebedorDTO.getTipoChave())
                    .chave(recorrenteRecebedorDTO.getChave())
                    .dataCriacaoRegistro(LocalDateTime.now())
                    .build();

            recorrenteRecebedorDTO.setNome("Nome Atualizado");
            recorrenteRecebedorDTO.setInstituicao("55555555");
            recorrenteRecebedorDTO.setTipoChave(TipoChaveEnum.EVP);
            recorrenteRecebedorDTO.setChave(UUID.randomUUID().toString());

            var cadastroRequest = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), recorrenteRecebedorDTO, List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(3))));

            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);

            mockPagadorRecebedorRepository(null, Optional.of(recebedorEntidade));

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(recebedorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve processar protocolo com sucesso e ordenar as parcelas por data de liquidação quando as parcelas estiverem desordenadas")
        void dadoParcelasDesordenadas_quandoProcessarProtocolo_deveExecutarComSucessoEOrdenarParcelas() {
            var cadastroRequest = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(5)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(1)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(3))));
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());

            var valueRecorrencia = captorRecorrencia.getValue();
            var valueParcelas = valueRecorrencia.getRecorrencias();

            assertAll(
                    //Recorrencia
                    () -> assertEquals(3, valueParcelas.size()),
                    //Parcela 1
                    () -> assertNotNull(valueParcelas.getFirst()),
                    () -> assertEquals(cadastroRequest.getParcelas().getFirst().getIdentificadorParcela(), valueParcelas.getFirst().getIdParcela()),
                    () -> assertEquals(cadastroRequest.getParcelas().getFirst().getDataTransacao().toLocalDate(), valueParcelas.getFirst().getDataTransacao()),
                    //Parcela 2
                    () -> assertNotNull(valueParcelas.get(1)),
                    () -> assertEquals(cadastroRequest.getParcelas().get(1).getIdentificadorParcela(), valueParcelas.get(1).getIdParcela()),
                    () -> assertEquals(cadastroRequest.getParcelas().get(1).getDataTransacao().toLocalDate(), valueParcelas.get(1).getDataTransacao()),
                    //Parcela 2
                    () -> assertNotNull(valueParcelas.get(2)),
                    () -> assertEquals(cadastroRequest.getParcelas().get(2).getIdentificadorParcela(), valueParcelas.get(2).getIdParcela()),
                    () -> assertEquals(cadastroRequest.getParcelas().get(2).getDataTransacao().toLocalDate(), valueParcelas.get(2).getDataTransacao())
            );
        }

        private void mockPagadorRecebedorRepository(Pagador pagador, Optional<Recebedor> recebedor) {
            var listaPagador = Optional.ofNullable(pagador)
                            .stream().toList();
            when(pagadorRepository.findByCpfCnpjAndAgenciaAndContaAndTipoConta(any(), any(), any(), any())).thenReturn(listaPagador);
            when(recebedorRepository.buscarRecebedor(any())).thenReturn(recebedor);
        }

    }

    @Nested
    class ProcessarCadastroErroNegocio {

        @Test
        @DisplayName("Deve retornar erro de validação de negocio quando a quantidade de parcelas esta acima do permitido")
        void dadoNumeroParcelaAcimaDoPermitido_quandoProcessarProtocolo_deveRetornarErroValidacaoNegocio() {
            var cadastroRequest = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(3))));
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(true).build();


            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);

            var recorrencia = cadastro.getRecorrencia();

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(idempotentRequest.getTransactionId()).thenReturn(IDENTIFICADOR_TRANSACAO);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(1L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any()))
                    .thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(validator, times(1)).validate(any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(), any());
            verify(recorrenciaRepository, never()).save(captorRecorrencia.capture());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(eq(recorrencia), eq(IDENTIFICADOR_TRANSACAO),  captorErro.capture());
            verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCadastroRecorrenciaIntegrada(any(), any(), any());
            verify(eventoResponseFactory, times(1)).criarEventoNotificacao(captureNotificacaoDTO.capture());

            var valueErro = captorErro.getValue();
            var valueNotificacao = captureNotificacaoDTO.getValue();

            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0004, valueErro.codigoErro()),
                    () -> assertEquals("Quantidade total de parcelas inválido. Informe quantidade de parcelas menor que %s".formatted(1L), valueErro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, valueErro.tipoRetornoTransacaoEnum()),

                    () -> assertEquals(recorrencia.getPagador().getAgencia(), valueNotificacao.getAgencia()),
                    () -> assertEquals(recorrencia.getPagador().getConta(), valueNotificacao.getConta()),
                    () -> assertEquals(recorrencia.getRecebedor().getChave(), valueNotificacao.getChave()),
                    () -> assertEquals(recorrencia.getRecebedor().getTipoChave().name(), valueNotificacao.getTipoChave()),
                    () -> assertEquals(NotificacaoDTO.TipoTemplate.RECORRENCIA_CADASTRO_FALHA, valueNotificacao.getOperacao()),
                    () -> assertEquals(recorrencia.getTipoCanal().getTipoCanalPix().name(), valueNotificacao.getCanal()),
                    () -> assertEquals(6, valueNotificacao.getInformacoesAdicionais().size()),
                    () -> assertEquals(recorrencia.getRecebedor().getNome(), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.NOME_RECEBEDOR.getNomeVariavel())),
                    () -> assertEquals(recorrencia.getTipoFrequencia().getTituloPlural().toLowerCase(Locale.ROOT), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.FREQUENCIA.getNomeVariavel())),
                    () -> assertEquals(recorrencia.getParcelas().getFirst().getValor().toString(), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.VALOR.getNomeVariavel())),
                    () -> assertEquals(String.valueOf(recorrencia.getParcelas().size()), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.QUANTIDADE_PARCELAS.getNomeVariavel())),
                    () -> assertEquals(String.valueOf(recorrencia.getPagador().getCpfCnpj()), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR.getNomeVariavel()))
            );
        }

        @Test
        @DisplayName("Deve retornar erro de validação de negocio quando a data de uma parcela estiver no passado")
        void dadoParcelaDataNoPassado_quandoProcessarProtocolo_deveRetornarErroValidacaoNegocio() {
            var cadastroRequest = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().minusMonths(1)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(1))));
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(true).build();

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);

            var recorrencia = cadastro.getRecorrencia();

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(idempotentRequest.getTransactionId()).thenReturn(IDENTIFICADOR_TRANSACAO);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(validator, times(1)).validate(any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(), any());
            verify(recorrenciaRepository, never()).save(captorRecorrencia.capture());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(eq(recorrencia), eq(IDENTIFICADOR_TRANSACAO),  captorErro.capture());
            verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCadastroRecorrenciaIntegrada(any(), any(), any());
            verify(eventoResponseFactory, times(1)).criarEventoNotificacao(captureNotificacaoDTO.capture());

            var valueErro = captorErro.getValue();

            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0003, valueErro.codigoErro()),
                    () -> assertTrue(valueErro.mensagemErro().matches("Data da recorrência inválida. Informe uma data a partir do dia \\d{2}/\\d{2}/\\d{4}")),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, valueErro.tipoRetornoTransacaoEnum())
            );
        }

        @Test
        @DisplayName("Deve retornar erro de validação de negocio quando os dados do pagador serem o mesmo do recebedor")
        void dadoMesmoPagadorERecebedor_quandoProcessarProtocolo_deveRetornarErroValidacaoNegocio() {
            var recorrentePagadorDTO = criarRecorrentePagadorDTO();
            var recorrenteRecebedorDTO = criarRecorrenteRecebedorDTO();

            recorrenteRecebedorDTO.setCpfCnpj(recorrentePagadorDTO.getCpfCnpj());
            recorrenteRecebedorDTO.setAgencia(recorrentePagadorDTO.getAgencia());
            recorrenteRecebedorDTO.setConta(recorrentePagadorDTO.getConta());
            recorrenteRecebedorDTO.setTipoConta(recorrentePagadorDTO.getTipoConta());

            var cadastroRequest = criarRecorrenteRequisicaoDTO(recorrentePagadorDTO, recorrenteRecebedorDTO, List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(3))));
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(true).build();

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(idempotentRequest.getTransactionId()).thenReturn(IDENTIFICADOR_TRANSACAO);
            when(appConfig.getRegras().getParcela().getNumeroMaximoParcelas()).thenReturn(12L);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(validator, times(1)).validate(any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(CadastroRequest.class), any(), any(), any());
            verify(recorrenciaRepository, never()).save(captorRecorrencia.capture());
            verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCadastroRecorrenciaIntegrada(any(), any(), any());
            verify(eventoResponseFactory, times(1)).criarEventoNotificacao(captureNotificacaoDTO.capture());
            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(eq(cadastroRequest), eq(IDENTIFICADOR_TRANSACAO),  captorErro.capture());

            var valueErro = captorErro.getValue();
            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0001, valueErro.codigoErro()),
                    () -> assertEquals("Dados do pagador e do recebedor inválidos. Informe uma conta de origem diferente da conta de destino.", valueErro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, valueErro.tipoRetornoTransacaoEnum())
            );
        }
    }

    @Nested
    class ProcessarCadastroValidacao {

        @Test
        @DisplayName("Deve retornar erro de validação de constraints quando houver erro")
        void dadoViolacaoConstraint_quandoProcessarProtocolo_deveRetornarErroValidacaoConstraint() {
        var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);
            var cadastroRequest = criarRecorrenteRequisicaoDTO(criarRecorrentePagadorDTO(), criarRecorrenteRecebedorDTO(), List.of(criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(2)), criarRecorrenteParcelaRequisicaoDTO(LocalDateTime.now().plusMonths(3))));
            IdempotentResponse idempotentResponse = IdempotentResponse.builder().errorResponse(false).build();

            CadastroRequestWrapper cadastro = CadastroRequestWrapper.criarRecorrencia(cadastroRequest);

            var mensagemErro = "Teste de erro de validação de constraints";

            when(idempotentRequest.getValue()).thenReturn(cadastro);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(validator.validate(any())).thenReturn(Set.of(mockConstraintViolation));
            when(mockConstraintViolation.getMessage()).thenReturn(mensagemErro);
            when(criaResponseStrategy.criarResponseIdempotentErro(any(), any(), any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));

            cadastroService.processarRecorrencia(idempotentRequest);

            verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCadastroRecorrenciaIntegrada(any(), any(), any());

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(eq(cadastroRequest), eq(null), captorErro.capture());
            verify(eventoResponseFactory).criarEventoNotificacao(captureNotificacaoDTO.capture());

            var valueErro = captorErro.getValue();
            var valueNotificacao = captureNotificacaoDTO.getValue();

            assertAll(
                    () -> assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001, valueErro.codigoErro()),
                    () -> assertEquals("Algum dado do payload de cadastro está inválido. Erro -> %s".formatted(mensagemErro), valueErro.mensagemErro()),
                    () -> assertEquals(TipoRetornoTransacaoEnum.ERRO_VALIDACAO, valueErro.tipoRetornoTransacaoEnum()),

                    //Notificacao
                    () -> assertEquals(cadastro.getRecorrencia().getPagador().getAgencia(), valueNotificacao.getAgencia()),
                    () -> assertEquals(cadastro.getRecorrencia().getPagador().getConta(), valueNotificacao.getConta()),
                    () -> assertEquals(cadastro.getRecorrencia().getRecebedor().getChave(), valueNotificacao.getChave()),
                    () -> assertEquals(cadastro.getRecorrencia().getRecebedor().getTipoChave().name(), valueNotificacao.getTipoChave()),
                    () -> assertEquals(NotificacaoDTO.TipoTemplate.RECORRENCIA_CADASTRO_SUCESSO, valueNotificacao.getOperacao())
            );
        }

    }

    private RecorrenteParcelaRequisicaoDTO criarRecorrenteParcelaRequisicaoDTO(LocalDateTime dataParcela) {
        return RecorrenteParcelaRequisicaoDTO.builder()
                .valor(BigDecimal.TEN)
                .dataTransacao(dataParcela)
                .idFimAFim(null)
                .informacoesEntreUsuarios(INFORMACOES_ENTRE_USUARIOS)
                .idConciliacaoRecebedor(UUID.randomUUID().toString())
                .identificadorParcela(UUID.randomUUID().toString())
                .build();
    }

    private RecorrentePagadorDTO criarRecorrentePagadorDTO() {
        return RecorrentePagadorDTO.builder()
                .cpfCnpj("12345678901")
                .nome(NOME_PAGADOR)
                .instituicao(ISPB_PAGADOR)
                .agencia(COOP_PAGADOR)
                .conta(CONTA_PAGADOR)
                .posto(POSTO_PAGADOR)
                .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                .tipoPessoa(TipoPessoaEnum.PF)
                .build();
    }

    private RecorrenteRecebedorDTO criarRecorrenteRecebedorDTO() {
        return RecorrenteRecebedorDTO.builder()
                .cpfCnpj("09876543212")
                .nome(NOME_RECEBEDOR)
                .agencia(COOP_RECEBEDOR)
                .conta(CONTA_RECEBEDOR)
                .instituicao(ISPB_RECEBEDOR)
                .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                .tipoPessoa(TipoPessoaEnum.PF)
                .tipoChave(TipoChaveEnum.CPF)
                .chave("09876543212")
                .build();

    }

    private CadastroRequest criarRecorrenteRequisicaoDTO(RecorrentePagadorDTO recorrentePagadorDTO, RecorrenteRecebedorDTO recorrenteRecebedorDTO, List<RecorrenteParcelaRequisicaoDTO> parcelas) {
        return CadastroRequest.builder()
                .pagador(recorrentePagadorDTO)
                .recebedor(recorrenteRecebedorDTO)
                .nome(NOME_RECORRENCIA)
                .tipoCanal(TipoCanalEnum.MOBI)
                .tipoOrigemSistema(OrigemEnum.LEGADO)
                .tipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_BY_KEY)
                .tipoIniciacaoCanal(TipoIniciacaoCanal.CHAVE)
                .tipoFrequencia(TipoFrequencia.MENSAL)
                .tipoRecorrencia(TipoRecorrencia.AGENDADO)
                .numInicCnpj(null)
                .parcelas(parcelas)
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .build();
    }


}