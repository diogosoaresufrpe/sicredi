package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spi.entities.type.StatusRecorrenciaPain012;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.*;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.metrica.RegistraMetricaService;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.protocolo.CanaisDigitaisProtocoloInfoInternalApiClient;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.instancio.Instancio;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA;
import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_EXPIRADA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CONFIRMACAO_AUTORIZACAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.RecorrenciaAutorizacao.*;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.ACEITA;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.*;
import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.RecorrenciaConstantes.CPF_PAGADOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutorizacaoServiceTest {

    private static final String TOPICO = "TOPICO";
    private static final String TESTE = "TESTE";
    private static final String CPF_CNPJ = "12690422115";
    private static final String ID_INFORMACAO_STATUS = "IS0071467120250537y331ZVAcRGY";
    private static final int NUMERO_PAGINA_ZERO = 0;
    private static final int TAMANHO_PAGINA_DEZ = 10;

    @InjectMocks
    private AutorizacaoServiceImpl service;
    @Mock
    private RecorrenciaAutorizacaoCancelamentoRepository autorizacaoCancelamentoRepository;
    @Mock
    private RecorrenciaAutorizacaoRepository autorizacaoRepository;
    @Mock
    private SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Mock
    private IdempotentRequest<Pain012Dto> idempotentRequestPain012;
    @Mock
    private CanaisDigitaisProtocoloInfoInternalApiClient canaisDigitaisProtocoloInfoInternalApiClient;
    @Mock
    private CriaResponseStrategyFactory<OperacaoRequest> criaResponseStrategyFactory;
    @Mock
    private CriaResponseStrategy criaResponseStrategy;
    @Captor
    private ArgumentCaptor<NotificacaoDTO> captorNotificacao;
    @Captor
    private ArgumentCaptor<RecorrenciaAutorizacao> captorRecorreciaAutorizacao;
    @Mock
    private RegistraMetricaService registraMetricaService;

    @Test
    void processarPedidoCancelamento() {
        assertThrows(TechnicalException.class, () -> service.processarRetornoPedidoCancelamento(null));
    }

    @Nested
    class ConsultarAutorizacoes {

        @Test
        void consultarTodas() {
            var primeiraAutorizacaoMock = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.APROVADA, null);
            var segundaAutorizacaoMock = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, "AGUARDANDO_RETORNO");

            var pageable = PageRequest.of(
                    NUMERO_PAGINA_ZERO,
                    TAMANHO_PAGINA_DEZ,
                    Sort.by(Sort.Direction.ASC, "dataCriacaoRecorrencia")
            );

            var request = ConsultaAutorizacaoRequest.builder()
                    .agenciaPagador("123")
                    .contaPagador("123456789-0")
                    .status(Set.of("CRIADA", "PENDENTE_CONFIRMACAO"))
                    .tamanhoPagina(TAMANHO_PAGINA_DEZ)
                    .numeroPagina(NUMERO_PAGINA_ZERO)
                    .build();

            when(autorizacaoRepository.findAllByFiltros(any(ConsultaAutorizacaoRequest.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(primeiraAutorizacaoMock, segundaAutorizacaoMock), pageable, 2));

            var response = service.consultarTodas(request);

            assertEquals(2, response.autorizacoes().size());

            var primeiraAutorizacao = response.autorizacoes().getFirst();
            assertEquals(primeiraAutorizacaoMock.getOidRecorrenciaAutorizacao(), primeiraAutorizacao.getOidRecorrenciaAutorizacao());
            assertEquals(primeiraAutorizacaoMock.getNomeRecebedor(), primeiraAutorizacao.getNomeRecebedor());
            assertEquals(primeiraAutorizacaoMock.getDataCriacaoRecorrencia(), primeiraAutorizacao.getDataCriacao());
            assertEquals(primeiraAutorizacaoMock.getNumeroContrato(), primeiraAutorizacao.getContrato());
            assertEquals(primeiraAutorizacaoMock.getDescricao(), primeiraAutorizacao.getDescricao());
            assertEquals(primeiraAutorizacaoMock.getTipoStatus().toString(), primeiraAutorizacao.getTpoStatus());
            assertNull(primeiraAutorizacao.getTpoSubStatus());

            var segundaSolicitacao = response.autorizacoes().getLast();
            assertEquals(segundaAutorizacaoMock.getOidRecorrenciaAutorizacao(), segundaSolicitacao.getOidRecorrenciaAutorizacao());
            assertEquals(segundaAutorizacaoMock.getNomeRecebedor(), segundaSolicitacao.getNomeRecebedor());
            assertEquals(segundaAutorizacaoMock.getDataCriacaoRecorrencia(), segundaSolicitacao.getDataCriacao());
            assertEquals(segundaAutorizacaoMock.getNumeroContrato(), segundaSolicitacao.getContrato());
            assertEquals(segundaAutorizacaoMock.getDescricao(), segundaSolicitacao.getDescricao());
            assertEquals(segundaAutorizacaoMock.getTipoStatus().toString(), segundaSolicitacao.getTpoStatus());
            assertEquals(segundaAutorizacaoMock.getTipoSubStatus(), segundaSolicitacao.getTpoSubStatus());

            verify(autorizacaoRepository).findAllByFiltros(eq(request), any(Pageable.class));
        }

        @Test
        void dadoUmOidValido_quandoBuscarPorOid_deveRetornarAutorizacao() {
            when(autorizacaoRepository.findById(any())).thenReturn(Optional.of(new RecorrenciaAutorizacao()));

            var recorrenciaAutorizacao = service.buscarPorOid(1L);

            assertTrue(recorrenciaAutorizacao.isPresent());
            assertNotNull(recorrenciaAutorizacao.get());
        }
    }

    @Nested
    class ProcessarRespostaBacenConfirmacaoAutorizacao {

        @Test
        void dadoConfirmacaoAprovadaEJornada1_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_RETORNO);

            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));

            service.processarRecebimentoPain012Bacen(idempotentRequestPain012);

            verify(autorizacaoRepository).save(captorRecorreciaAutorizacao.capture());
            verify(solicitacaoRepository).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(eq(respostaConfirmacaoAutorizacao.getIdRecorrencia()), eq(ACEITA), isNull(), any(LocalDateTime.class));
            verify(eventoResponseFactory).criarEventoNotificacao(captorNotificacao.capture());
            verify(registraMetricaService).registrar(any());

            var recorrenciaAutorizacaoValue = captorRecorreciaAutorizacao.getValue();
            var notificacaoValue = captorNotificacao.getValue();

            validarAssertsRespostaAutorizacao(recorrenciaAutorizacaoValue, recorrenciaAutorizacao, respostaConfirmacaoAutorizacao, AUTOMATICO_AUTORIZACAO_CONFIRMADA_SUCESSO, notificacaoValue, TipoStatusAutorizacao.APROVADA);
        }

        @Test
        void dadoConfirmacaoRejeitadaEJornada1_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA.name(), StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_ENVIO);

            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            service.processarRecebimentoPain012Bacen(idempotentRequestPain012);

            verify(autorizacaoRepository).save(captorRecorreciaAutorizacao.capture());
            verify(solicitacaoRepository).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(eq(respostaConfirmacaoAutorizacao.getIdRecorrencia()), eq(PENDENTE_CONFIRMACAO), isNull(), any(LocalDateTime.class));
            verify(eventoResponseFactory).criarEventoNotificacao(captorNotificacao.capture());

            var recorrenciaAutorizacaoValue = captorRecorreciaAutorizacao.getValue();
            var notificacaoValue = captorNotificacao.getValue();

            validarAssertsRespostaAutorizacao(recorrenciaAutorizacaoValue, recorrenciaAutorizacao, respostaConfirmacaoAutorizacao, AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR, notificacaoValue, TipoStatusAutorizacao.REJEITADA);
        }

        @Test
        void dadoConfirmacaoAprovadaENaoJornada1_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_RETORNO);
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            recorrenciaAutorizacao.setTipoJornada(null);

            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            service.processarRecebimentoPain012Bacen(idempotentRequestPain012);

            verify(solicitacaoRepository, never()).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(any(), any(), any(), any());
            verify(autorizacaoRepository).save(captorRecorreciaAutorizacao.capture());
            verify(eventoResponseFactory).criarEventoNotificacao(captorNotificacao.capture());
            verify(registraMetricaService).registrar(any());

            var recorrenciaAutorizacaoValue = captorRecorreciaAutorizacao.getValue();
            var notificacaoValue = captorNotificacao.getValue();

            validarAssertsRespostaAutorizacao(recorrenciaAutorizacaoValue, recorrenciaAutorizacao, respostaConfirmacaoAutorizacao, AUTOMATICO_AUTORIZACAO_CONFIRMADA_SUCESSO, notificacaoValue, TipoStatusAutorizacao.APROVADA);
        }

        @Test
        void dadoConfirmacaoRejeitadaENaoJornada1_quandoProcessarRespostaConfirmacaoAutorizacao_deveAtualizarStatusEEnviarNotificacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA.name(), StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_ENVIO);

            recorrenciaAutorizacao.setTipoJornada(null);
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));

            service.processarRecebimentoPain012Bacen(idempotentRequestPain012);

            verify(solicitacaoRepository, never()).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(any(), any(), any(), any());
            verify(autorizacaoRepository).save(captorRecorreciaAutorizacao.capture());
            verify(eventoResponseFactory).criarEventoNotificacao(captorNotificacao.capture());
            verify(registraMetricaService).registrar(any());

            var recorrenciaAutorizacaoValue = captorRecorreciaAutorizacao.getValue();
            var notificacaoValue = captorNotificacao.getValue();

            validarAssertsRespostaAutorizacao(recorrenciaAutorizacaoValue, recorrenciaAutorizacao, respostaConfirmacaoAutorizacao, AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR, notificacaoValue, TipoStatusAutorizacao.REJEITADA);
        }

        @Test
        void dadoExececaoAoSalvarNoBanco_quandoProcessarRespostaConfirmacaoAutorizacao_deveLancarExececao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA.name(), StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_ENVIO);

            recorrenciaAutorizacao.setTipoJornada(null);

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);

            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));
            when(autorizacaoRepository.save(any())).thenThrow(new DataIntegrityViolationException("Erro ao atualizar dados da autorização."));

            assertThrows(TechnicalException.class,
                    () -> service.processarRecebimentoPain012Bacen(idempotentRequestPain012));

            verify(criaResponseStrategyFactory, never()).criar(TipoResponseIdempotente.OPERACAO);
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any());
            verify(autorizacaoRepository).save(any());
            verify(eventoResponseFactory, never()).criarEventoNotificacao(captorNotificacao.capture());
            verify(solicitacaoRepository, never()).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(any(), any(), any(), any());
        }

        @Test
        void dadoExececaoAoAtualizarSolicitacao_quandoProcessarRespostaConfirmacaoAutorizacao_deveLancarExececao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA.name(), StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_ENVIO);

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));
            when(solicitacaoRepository.atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(any(), any(), any(), any())).thenThrow(new DataIntegrityViolationException("Erro ao atualizar dados da solicitação."));

            assertThrows(TechnicalException.class,
                    () -> service.processarRecebimentoPain012Bacen(idempotentRequestPain012));


            verify(autorizacaoRepository).save(any());
            verify(solicitacaoRepository).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(eq(respostaConfirmacaoAutorizacao.getIdRecorrencia()), eq(PENDENTE_CONFIRMACAO), isNull(), any(LocalDateTime.class));
            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());
        }

        @Test
        void dadoAutorizacaoComStatusDiferenteCriado_quandoProcessarRespostaBacenAutorizacao_deveLancarExececao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA.name(), StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.EXPIRADA, null);

            recorrenciaAutorizacao.setTipoJornada(null);

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));

            assertThrows(TechnicalException.class,
                    () -> service.processarRecebimentoPain012Bacen(idempotentRequestPain012));

            verify(autorizacaoRepository, never()).save(any());
            verify(solicitacaoRepository, never()).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(eq(respostaConfirmacaoAutorizacao.getIdRecorrencia()), eq(ACEITA), isNull(), any(LocalDateTime.class));
            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());
            verify(criaResponseStrategy,never()).criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any());
        }

        @Test
        void dadoAutorizacaoComStatusPendente_quandoProcessarRespostaBacenAutorizacao_deveReceberENaoProcessar() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.PENDENTE_CONFIRMACAO);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.EXPIRADA, null);

            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));
            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);

            service.processarRecebimentoPain012Bacen(idempotentRequestPain012);

            verify(solicitacaoRepository, never()).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(eq(respostaConfirmacaoAutorizacao.getIdRecorrencia()), eq(ACEITA), isNull(), any(LocalDateTime.class));
            verify(autorizacaoRepository, never()).save(any());
            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());
            verify(criaResponseStrategy,never()).criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any());
        }

        @Test
        void dadoAutorizacaoComStatusFalseNaoEncontrada_quandoProcessarRespostaBacenAutorizacao_deveLancarExececao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, null, StatusRecorrenciaPain012.PENDENTE_CONFIRMACAO);

            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of());
            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);

            assertThrows(TechnicalException.class,
                    () -> service.processarRecebimentoPain012Bacen(idempotentRequestPain012));

            verify(criaResponseStrategy,never()).criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any());
            verify(solicitacaoRepository, never()).atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(eq(respostaConfirmacaoAutorizacao.getIdRecorrencia()), eq(ACEITA), isNull(), any(LocalDateTime.class));
            verify(autorizacaoRepository, never()).save(captorRecorreciaAutorizacao.capture());
            verify(eventoResponseFactory, never()).criarEventoNotificacao(captorNotificacao.capture());
        }


        private void validarAssertsRespostaAutorizacao(RecorrenciaAutorizacao recorrenciaAutorizacaoValue, RecorrenciaAutorizacao recorrenciaAutorizacao, Pain012Dto respostaConfirmacaoAutorizacao, NotificacaoDTO.TipoTemplate tipoTemplate, NotificacaoDTO notificacaoValue, TipoStatusAutorizacao statusAutorizacao) {
            assertAll(
                    () -> assertNull(recorrenciaAutorizacaoValue.getTipoSubStatus()),
                    () -> assertEquals(statusAutorizacao, recorrenciaAutorizacaoValue.getTipoStatus()),
                    () -> assertEquals(ID_INFORMACAO_STATUS, recorrenciaAutorizacaoValue.getIdInformacaoStatusRecebimento()),
                    () -> assertEquals(respostaConfirmacaoAutorizacao.getMotivoRejeicao(), recorrenciaAutorizacaoValue.getMotivoRejeicao()),

                    () -> assertEquals(tipoTemplate, notificacaoValue.getOperacao()),
                    () -> assertEquals(converterCanalParaNotificacao(recorrenciaAutorizacao.getTipoCanalPagador(), recorrenciaAutorizacao.getTipoSistemaPagador()), notificacaoValue.getCanal()),
                    () -> assertEquals(recorrenciaAutorizacao.getContaPagador(), notificacaoValue.getConta()),
                    () -> assertEquals(recorrenciaAutorizacao.getAgenciaPagador(), notificacaoValue.getAgencia()),
                    () -> assertEquals(2, notificacaoValue.getInformacoesAdicionais().size())
            );
        }

    }

    @Nested
    class ProcessarRespostaBacenCancelamentoAutorizacao {

        @Test
        void dadoCancelamentoAprovado_quandoProcessarRespostaBacenCancelamentoAutorizacao_deveAtualizarAutorizacaoECancelamentoEEnviarNotificacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CANCELADA);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.APROVADA, AGUARDANDO_RETORNO);

            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));

            service.processarRecebimentoPain012Bacen(idempotentRequestPain012);

            verify(autorizacaoRepository).save(captorRecorreciaAutorizacao.capture());
            verify(autorizacaoCancelamentoRepository).atualizaStatusEIdInformacaoStatus(eq(recorrenciaAutorizacao.getIdRecorrencia()), eq(TipoStatusCancelamentoAutorizacao.ACEITA), eq(respostaConfirmacaoAutorizacao.getIdInformacaoStatus()), any(LocalDateTime.class));
            verify(eventoResponseFactory).criarEventoNotificacao(captorNotificacao.capture());

            var notificacaoValue = captorNotificacao.getValue();
            var recorrenciaAutorizacaoValue = captorRecorreciaAutorizacao.getValue();

            validarDadosNotificacao(AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_SUCESSO, recorrenciaAutorizacao, notificacaoValue);

            assertAll(
                    () -> assertNull(recorrenciaAutorizacaoValue.getTipoSubStatus()),
                    () -> assertEquals(TipoStatusAutorizacao.CANCELADA, recorrenciaAutorizacaoValue.getTipoStatus())
            );
        }

        @Test
        void dadoCancelamentoRejeitadoPeloRecebedor_quandoProcessarRespostaBacenCancelamentoAutorizacao_deveAtualizarAutorizacaoECancelamentoENaoEnviarNotificacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_EXPIRADA.name(), StatusRecorrenciaPain012.CANCELADA);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.APROVADA, AGUARDANDO_CANCELAMENTO);

            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));

            service.processarRecebimentoPain012Bacen(idempotentRequestPain012);

            verify(autorizacaoRepository).save(captorRecorreciaAutorizacao.capture());
            verify(autorizacaoCancelamentoRepository).atualizaStatusIdInformacaoStatusEMotivoRejeicao(eq(recorrenciaAutorizacao.getIdRecorrencia()), eq(TipoStatusCancelamentoAutorizacao.REJEITADA), eq(respostaConfirmacaoAutorizacao.getIdInformacaoStatus()), eq(RECORRENCIA_DA_SOLICITACAO_CANCELAMENTO_EXPIRADA.name()), any(LocalDateTime.class));
            verify(eventoResponseFactory).criarEventoNotificacao(captorNotificacao.capture());

            var notificacaoValue = captorNotificacao.getValue();
            var recorrenciaAutorizacaoValue = captorRecorreciaAutorizacao.getValue();

            validarDadosNotificacao(AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_NEGADO, recorrenciaAutorizacao, notificacaoValue);

            assertAll(
                    () -> assertNull(recorrenciaAutorizacaoValue.getTipoSubStatus()),
                    () -> assertEquals(TipoStatusAutorizacao.APROVADA, recorrenciaAutorizacaoValue.getTipoStatus())
            );
        }

        @Test
        void dadoExececaoAoAtualizarAutorizacao_quandoProcessarRespostaBacenCancelamentoAutorizacao_deveAtualizarAutorizacaoECancelamentoEEnviarNotificacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CANCELADA);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.APROVADA, AGUARDANDO_RETORNO);

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(respostaConfirmacaoAutorizacao.getIdRecorrencia())).thenReturn(List.of(recorrenciaAutorizacao));
            when(autorizacaoRepository.save(any())).thenThrow(new DataIntegrityViolationException("Erro ao atualizar dados da autorização."));

            assertThrows(TechnicalException.class,
                    () -> service.processarRecebimentoPain012Bacen(idempotentRequestPain012));

            verify(autorizacaoRepository).save(captorRecorreciaAutorizacao.capture());

            verify(autorizacaoCancelamentoRepository, never()).atualizaStatusEIdInformacaoStatus(any(), any(), any(), any(LocalDateTime.class));
            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());
            verify(criaResponseStrategyFactory, never()).criar(any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(), any());
        }

        private void validarDadosNotificacao(NotificacaoDTO.TipoTemplate tipoTemplate, RecorrenciaAutorizacao recorrenciaAutorizacao, NotificacaoDTO notificacaoValue) {
            assertAll(
                    () -> assertEquals(tipoTemplate, notificacaoValue.getOperacao()),
                    () -> assertEquals(converterCanalParaNotificacao(recorrenciaAutorizacao.getTipoCanalPagador(), recorrenciaAutorizacao.getTipoSistemaPagador()), notificacaoValue.getCanal()),
                    () -> assertEquals(recorrenciaAutorizacao.getContaPagador(), notificacaoValue.getConta()),
                    () -> assertEquals(recorrenciaAutorizacao.getAgenciaPagador(), notificacaoValue.getAgencia()),
                    () -> assertEquals(2, notificacaoValue.getInformacoesAdicionais().size())
            );
        }

    }

    @Nested
    class RetornoAutorizacaoAposEnvioBacen {

        @Test
        void dadoAutorizacaoAprovada_quandoProcessarRetornoConfirmacaoAutorizacao_deveEnviarProtocoloConfirmacaoENaoAtualizarAutorizacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.APROVADA, null);

            final var protocoloDto = new ProtocoloDTO();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CONFIRMACAO_AUTORIZACAO, respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(protocoloDto);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(autorizacaoRepository, never()).atualizaSubStatusSeCriadaEAguardandoEnvio(any(), any(), any());
            verify(solicitacaoRepository, never()).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
        }

        @Test
        void dadoConfirmacaoAprovadaEJornada1_quandoProcessarRetornoConfirmacaoAutorizacao_deveAtualizarStatusECriarResponseSucesso() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, null);

            final var protocoloDto = new ProtocoloDTO();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CONFIRMACAO_AUTORIZACAO, respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(protocoloDto);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
            verify(autorizacaoRepository).atualizaSubStatusSeCriadaEAguardandoEnvio(eq(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao()), eq(TipoSubStatus.AGUARDANDO_RETORNO.name()), any(LocalDateTime.class));
            verify(solicitacaoRepository).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(eq(respostaConfirmacaoAutorizacao.getIdRecorrencia()), eq(TipoStatusSolicitacaoAutorizacao.CONFIRMADA), eq(TipoSubStatus.AGUARDANDO_RETORNO.name()));
        }

        @Test
        void dadoConfirmacaoAprovadaEJornada2_quandoProcessarRetornoConfirmacaoAutorizacao_deveAtualizarStatusECriarResponseSucesso() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, null);

            recorrenciaAutorizacao.setTipoJornada(TipoJornada.JORNADA_2.name());
            final var protocoloDto = new ProtocoloDTO();

            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador("439", respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(protocoloDto);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
            verify(solicitacaoRepository, never()).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
            verify(autorizacaoRepository).atualizaSubStatusSeCriadaEAguardandoEnvio(eq(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao()), eq(TipoSubStatus.AGUARDANDO_RETORNO.name()), any(LocalDateTime.class));
        }

        @Test
        void dadoConfirmacaoAprovadaEJornada3_quandoProcessarRetornoConfirmacaoAutorizacao_deveAtualizarStatusECriarResponseSucesso() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, null);

            final var protocoloDto = new ProtocoloDTO();
            recorrenciaAutorizacao.setTipoJornada(TipoJornada.JORNADA_3.name());

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador("440", respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(protocoloDto);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(autorizacaoRepository).atualizaSubStatusSeCriadaEAguardandoEnvio(eq(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao()), eq(TipoSubStatus.AGUARDANDO_RETORNO.name()), any(LocalDateTime.class));
            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
            verify(solicitacaoRepository, never()).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
        }

        @Test
        void dadoConfirmacaoAprovadaEJornada4_quandoProcessarRetornoConfirmacaoAutorizacao_deveAtualizarStatusECriarResponseSucesso() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, null);

            recorrenciaAutorizacao.setTipoJornada(TipoJornada.JORNADA_4.name());
            final var protocoloDto = new ProtocoloDTO();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador("439", respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(protocoloDto);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(solicitacaoRepository, never()).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
            verify(autorizacaoRepository).atualizaSubStatusSeCriadaEAguardandoEnvio(eq(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao()), eq(TipoSubStatus.AGUARDANDO_RETORNO.name()), any(LocalDateTime.class));
        }

        @Test
        void dadoConfirmacaoAprovadaENaoJornada1_quandoProcessarRetornoConfirmacaoAutorizacao_deveAtualizarStatusAutorizacaoENaoDeSolicitacao() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, null);

            recorrenciaAutorizacao.setTipoJornada(TipoJornada.JORNADA_2.name());
            final var protocoloDto = new ProtocoloDTO();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador("439", respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(protocoloDto);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(solicitacaoRepository, never()).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
            verify(autorizacaoRepository).atualizaSubStatusSeCriadaEAguardandoEnvio(eq(recorrenciaAutorizacao.getOidRecorrenciaAutorizacao()), eq(TipoSubStatus.AGUARDANDO_RETORNO.name()), any(LocalDateTime.class));
        }

        @Test
        void dadoAutorizacaoNaoEncontrada_quandoProcessarRetornoConfirmacaoAutorizacao_deveLancarTechnicalException() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, null, StatusRecorrenciaPain012.PENDENTE_CONFIRMACAO);

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.empty());

            assertThrows(TechnicalException.class,
                    () -> service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012));

            verify(solicitacaoRepository, never()).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
            verify(autorizacaoRepository, never()).atualizaSubStatusSeCriadaEAguardandoEnvio(any(), any(), any());
        }

        @Test
        void dadoExececaoAoAtualizarSolicitacao_quandoProcessarRetornoConfirmacaoAutorizacao_deveLancarTechnicalException() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_RETORNO);

            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(solicitacaoRepository.atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any())).thenThrow(new TechnicalException());

            assertThrows(TechnicalException.class,
                    () -> service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012));

            verify(autorizacaoRepository).atualizaSubStatusSeCriadaEAguardandoEnvio(any(), any(), any());
            verify(solicitacaoRepository).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
        }

        @Test
        void dadoExececaoAoBuscarDadosProtocolo_quandoProcessarRetornoConfirmacaoAutorizacao_deveLancarNotFoundException() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.TRUE, null, StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, AGUARDANDO_RETORNO);

            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);

            assertThrows(NotFoundException.class,
                    () -> service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012));

            verify(canaisDigitaisProtocoloInfoInternalApiClient).consultaProtocoloPorTipoEIdentificador(CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CONFIRMACAO_AUTORIZACAO, respostaConfirmacaoAutorizacao.getIdInformacaoStatus());
            verify(solicitacaoRepository).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(any(), any(), any());
            verify(autorizacaoRepository).atualizaSubStatusSeCriadaEAguardandoEnvio(any(), any(), any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
        }

        @Test
        void dadoAutorizacaoJornada1NaoAprovada_quandoProcessarRetornoAutorizacaoAposEnvioBacen_deveFinalizarProtocolo() {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, MotivoRejeicaoPain012.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR.name(), null);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, null);

            final var protocoloDto = new ProtocoloDTO();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CONFIRMACAO_AUTORIZACAO, respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(protocoloDto);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(autorizacaoRepository).save(
                    argThat(autorizacao -> {
                        assertEquals(TipoStatusAutorizacao.REJEITADA, autorizacao.getTipoStatus());
                        assertNull(autorizacao.getTipoSubStatus());
                        return true;
                    })
            );
            verify(autorizacaoRepository, never()).atualizaSubStatusSeCriadaEAguardandoEnvio(any(), any(), any());
            verify(solicitacaoRepository).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(
                    any(),
                    argThat(status -> {
                        Assertions.assertEquals(TipoStatusSolicitacaoAutorizacao.REJEITADA, status);
                        return true;
                    }),
                    any()
            );
            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
        }

        @ParameterizedTest
        @EnumSource(value = TipoJornada.class, mode = EnumSource.Mode.EXCLUDE, names = "JORNADA_1")
        void dadoAutorizacaoDiferenteJornada1NaoAprovada_quandoProcessarRetornoAutorizacaoAposEnvioBacen_deveFinalizarProtocolo(TipoJornada tipoJornada) {
            var respostaConfirmacaoAutorizacao = criarRetornoConfirmacaoAutorizacao(Boolean.FALSE, MotivoRejeicaoPain012.SOLICITACAO_CONFIRMACAO_REJEITADA_PAGADOR_NAO_RECONHECE_USUARIO_RECEBEDOR.name(), null);
            var recorrenciaAutorizacao = criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao.CRIADA, null);
            recorrenciaAutorizacao.setTipoJornada(tipoJornada.name());

            final var protocoloDto = new ProtocoloDTO();

            when(idempotentRequestPain012.getValue()).thenReturn(respostaConfirmacaoAutorizacao);
            when(autorizacaoRepository.findByIdInformacaoStatusEnvio(respostaConfirmacaoAutorizacao.getIdInformacaoStatus())).thenReturn(Optional.of(recorrenciaAutorizacao));
            when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(any(), any())).thenReturn(protocoloDto);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);

            service.processarRetornoAutorizacaoAposEnvioBacen(idempotentRequestPain012);

            verify(autorizacaoRepository).save(
                    argThat(autorizacao -> {
                        assertEquals(TipoStatusAutorizacao.REJEITADA, autorizacao.getTipoStatus());
                        assertNull(autorizacao.getTipoSubStatus());
                        return true;
                    })
            );
            verify(autorizacaoRepository, never()).atualizaSubStatusSeCriadaEAguardandoEnvio(any(), any(), any());
            verify(solicitacaoRepository, never()).atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(
                    any(),
                    any(),
                    any()
            );
            verify(criaResponseStrategy).criarResponseIdempotentSucesso(any(ProtocoloDTO.class), any(), any(), any());
        }

    }

    @Nested
    class ConsultaDetalhes {

        private static final Long ID_RECORRENCIA_AUTORIZACAO = 1L;
        private static final Long ID_RECORRENCIA_AUTORIZACAO_LONG = 1L;

        @Test
        void dadoIdentificadorTransacaoExistente_quandoConsultarDetalhes_deveRetornarAutorizacao() {
            var autorizacaoMock = TestFactory.AutorizacaoTestFactory.criarAutorizacao();
            autorizacaoMock.setOidRecorrenciaAutorizacao(ID_RECORRENCIA_AUTORIZACAO_LONG);
            autorizacaoMock.setPermiteLinhaCredito("S");
            autorizacaoMock.setPermiteNotificacaoAgendamento("F");
            autorizacaoMock.setPermiteRetentativa("S");

            when(autorizacaoRepository.consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG))
                    .thenReturn(Optional.of(autorizacaoMock));

            var autorizacaoResponse = service.consultarDetalhes(ID_RECORRENCIA_AUTORIZACAO);

            assertEquals(ID_RECORRENCIA_AUTORIZACAO, autorizacaoResponse.getOidRecorrenciaAutorizacao());

            assertEquals(true, autorizacaoResponse.getPermiteLinhaCredito());
            assertEquals(false, autorizacaoResponse.getPermiteNotificacaoAgendamento());
            assertEquals(true, autorizacaoResponse.getPermiteRetentativa());

            verify(autorizacaoRepository).consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG);
        }

        @Test
        void dadoAutorizacaoCancelada_quandoConsultarDetalhes_deveRetornarAutorizacaoComDadosDeCancelamento() {
            var autorizacaoMock = TestFactory.AutorizacaoTestFactory.criarAutorizacao();
            autorizacaoMock.setOidRecorrenciaAutorizacao(ID_RECORRENCIA_AUTORIZACAO_LONG);
            autorizacaoMock.setPermiteLinhaCredito("S");
            autorizacaoMock.setPermiteRetentativa("S");
            autorizacaoMock.setPermiteNotificacaoAgendamento("F");
            autorizacaoMock.setTipoStatus(TipoStatusAutorizacao.CANCELADA);

            var autorizacaoCancelamento = RecorrenciaAutorizacaoCancelamento.builder()
                    .dataCancelamento(LocalDateTime.now())
                    .motivoCancelamento("SLDB")
                    .tipoSolicitanteCancelamento(TipoSolicitanteCancelamento.PAGADOR).build();

            when(autorizacaoRepository.consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG)).thenReturn(Optional.of(autorizacaoMock));
            when(autorizacaoCancelamentoRepository.findFirstByIdRecorrenciaAndTipoStatusOrderByDataAlteracaoRegistroDesc(autorizacaoMock.getIdRecorrencia(), TipoStatusCancelamentoAutorizacao.ACEITA)).thenReturn(Optional.of(autorizacaoCancelamento));

            var autorizacaoResponse = service.consultarDetalhes(ID_RECORRENCIA_AUTORIZACAO);

            assertEquals(ID_RECORRENCIA_AUTORIZACAO, autorizacaoResponse.getOidRecorrenciaAutorizacao());

            assertEquals(true, autorizacaoResponse.getPermiteLinhaCredito());
            assertEquals(false, autorizacaoResponse.getPermiteNotificacaoAgendamento());
            assertEquals(true, autorizacaoResponse.getPermiteRetentativa());
            assertEquals(autorizacaoCancelamento.getDataCancelamento(), autorizacaoResponse.getDataCancelamento());
            assertEquals(autorizacaoCancelamento.getMotivoCancelamento(), autorizacaoResponse.getCodigoMotivoCancelamento());
            assertEquals(autorizacaoCancelamento.getTipoSolicitanteCancelamento(), autorizacaoResponse.getSolicitanteCancelamento());

            verify(autorizacaoRepository).consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG);
            verify(autorizacaoCancelamentoRepository).findFirstByIdRecorrenciaAndTipoStatusOrderByDataAlteracaoRegistroDesc(any(), any());
        }

        @Test
        void dadoIdentificadorTransacaoExistente_quandoConsultarDetalhesEBoleanosForemNull_deveRetornarAutorizacao() {
            var autorizacaoMock = TestFactory.AutorizacaoTestFactory.criarAutorizacao();
            autorizacaoMock.setOidRecorrenciaAutorizacao(ID_RECORRENCIA_AUTORIZACAO_LONG);
            autorizacaoMock.setPermiteLinhaCredito(null);
            autorizacaoMock.setPermiteNotificacaoAgendamento(null);
            autorizacaoMock.setPermiteRetentativa(null);

            when(autorizacaoRepository.consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG))
                    .thenReturn(Optional.of(autorizacaoMock));

            var autorizacaoResponse = service.consultarDetalhes(ID_RECORRENCIA_AUTORIZACAO);

            assertEquals(ID_RECORRENCIA_AUTORIZACAO, autorizacaoResponse.getOidRecorrenciaAutorizacao());

            verify(autorizacaoRepository).consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG);
        }

        @Test
        void dadoIdentificadorTransacaoInexistente_quandoConsultarDetalhes_deveLancarNotFoundException() {
            when(autorizacaoRepository.consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.consultarDetalhes(ID_RECORRENCIA_AUTORIZACAO));

            verify(autorizacaoRepository).consultarPorIdentificadorRecorrencia(ID_RECORRENCIA_AUTORIZACAO_LONG);
        }
    }

    @Nested
    class SalvarAutorizacoes {

        @Test
        void dadaUmaRecorrenciaAutorizacao_quandoSalvar_deveRetornarRecorrenciaAutorizacao() {
            var recorrenciaAutorizacao = new RecorrenciaAutorizacao();
            when(autorizacaoRepository.save(any())).thenReturn(recorrenciaAutorizacao);
            assertNotNull(service.salvar(recorrenciaAutorizacao));
        }
    }

    @Test
    void dadoIdRecorrenciaENenhumResultado_quandoBuscarComCiclosEInstrucoesPorIdRecorrencia_deveRetornarListaVaziaComSucesso() {
        var idRecorrencia = RandomStringUtils.random(26);
        when(autorizacaoRepository.findWithCiclosByIdRecorrencia(idRecorrencia)).thenReturn(List.of());
        assertThat(service.buscarComCiclosPorIdRecorrencia(idRecorrencia)).isEmpty();
    }

    @Test
    void dadoIdRecorrenciaEResultado_quandoBuscarComCiclosEInstrucoesPorIdRecorrencia_deveRetornarListaComResultadoComSucesso() {
        var recorrenciaAutorizacao = Instancio.create(RecorrenciaAutorizacao.class);
        var idRecorrencia = RandomStringUtils.random(26);
        when(autorizacaoRepository.findWithCiclosByIdRecorrencia(idRecorrencia)).thenReturn(List.of(recorrenciaAutorizacao));
        assertThat(service.buscarComCiclosPorIdRecorrencia(idRecorrencia))
                .isNotEmpty()
                .allSatisfy(r -> assertFalse(r.getCiclos().isEmpty()));
    }

    public static RecorrenciaAutorizacao criarMockAutorizacaoRecorrencia(TipoStatusAutorizacao status, String subStatus) {
        return RecorrenciaAutorizacao.builder()
                .oidRecorrenciaAutorizacao(1L)
                .nomeRecebedor("EMPRESA XYZ LTDA")
                .dataCriacaoRecorrencia(LocalDateTime.of(2025, 5, 6, 18, 0))
                .numeroContrato("21312331-1")
                .descricao("Parcela Plano XYZ")
                .cpfCnpjPagador(CPF_PAGADOR)
                .tipoStatus(status)
                .tipoSubStatus(subStatus)
                .tipoCanalPagador(TipoCanalEnum.SICREDI_X)
                .tipoJornada("JORNADA_1")
                .build();
    }

    private Pain012Dto criarRetornoConfirmacaoAutorizacao(Boolean status, String motivoRejeicao, StatusRecorrenciaPain012 statusRecorrencia) {
        return Pain012Dto.builder()
                .status(status)
                .idInformacaoStatus(ID_INFORMACAO_STATUS)
                .motivoRejeicao(motivoRejeicao)
                .statusRecorrencia(Optional.ofNullable(statusRecorrencia).map(Enum::name).orElse(null))
                .tipoRecorrencia(TESTE)
                .tipoFrequencia(TESTE)
                .dataInicialRecorrencia(LocalDate.parse("2025-05-01"))
                .dataFinalRecorrencia(LocalDate.parse("2025-12-01"))
                .valor(BigDecimal.valueOf(20.75))
                .nomeUsuarioRecebedor("João da Silva")
                .cpfCnpjUsuarioRecebedor("12345678901")
                .participanteDoUsuarioRecebedor("341BANCO")
                .cpfCnpjUsuarioPagador(CPF_CNPJ)
                .contaUsuarioPagador("223190")
                .agenciaUsuarioPagador("0101")
                .participanteDoUsuarioPagador("00714671")
                .nomeDevedor("Empresa XYZ LTDA")
                .cpfCnpjDevedor("98765432000199")
                .numeroContrato("CT-2025-0001")
                .descricao(TESTE)
                .build();
    }

    public String converterCanalParaNotificacao(TipoCanalEnum canal, OrigemEnum sistema) {
        return switch (canal) {
            case SICREDI_INTERNET -> "INTERNET_BANKING";
            case SICREDI_X -> "SICREDI_APP";
            case WEB_OPENBK -> OrigemEnum.FISITAL.equals(sistema) ? "SICREDI_APP" : TipoCanalEnum.MOBI.name();
            default -> canal.name();
        };
    }
}