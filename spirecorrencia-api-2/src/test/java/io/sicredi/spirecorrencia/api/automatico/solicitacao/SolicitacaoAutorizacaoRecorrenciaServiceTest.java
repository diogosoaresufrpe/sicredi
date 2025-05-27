package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.DetalheRecorrenciaPain009Dto;
import br.com.sicredi.spi.dto.Pain009Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.accountdata.AccountDataService;
import io.sicredi.spirecorrencia.api.accountdata.DadosContaResponseDTO;
import io.sicredi.spirecorrencia.api.accountdata.DadosPessoaContaDTO;
import io.sicredi.spirecorrencia.api.accountdata.TipoContaEnum;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.gestentconector.GestentConectorService;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
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

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.*;
import static br.com.sicredi.spi.entities.type.TipoSituacaoPain009.*;
import static br.com.sicredi.spi.entities.type.TipoSituacaoRecorrenciaPain012.ATUALIZACAO_STATUS_RECORRENCIA;
import static br.com.sicredi.spi.entities.type.TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.*;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_PENDENTE_DE_APROVACAO;
import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.RecorrenciaConstantes.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitacaoAutorizacaoRecorrenciaServiceTest {

    private static final String TOPICO = "TOPICO";
    private static final String CPF_CNPJ = "12690422115";
    private static final String STATUS_CONTA = "ACTIVE";
    private static final String CODIGO_IBGE = "123456";
    private static final String TESTE = "TESTE";
    private static final int NUMERO_PAGINA_ZERO = 0;
    private static final int TAMANHO_PAGINA_DEZ = 10;
    private static final String HEADER_OPERACAO_RECORRENCIA_SOLICITACAO = "RECORRENCIA_SOLICITACAO";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;
    @Mock
    private AccountDataService accountDataService;
    @Mock
    private GestentConectorService gestentConectorService;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Mock
    private IdempotentRequest<Pain009Dto> idempotentRequestPain009;
    @Mock
    private IdempotentRequest<Pain012Dto> idempotentRequestPain012;
    @Mock
    private CriaResponseStrategyFactory criaResponseStrategyFactory;
    @Mock
    private CriaResponseStrategy criaResponseStrategy;
    @Captor
    private ArgumentCaptor<SolicitacaoAutorizacaoRecorrencia> captorRecorrenciaAutorizacaoSolicitacao;
    @Captor
    private ArgumentCaptor<Pain012Dto> captorPain012;
    @Captor
    private ArgumentCaptor<NotificacaoDTO> captorNotificacao;
    @Mock
    private SolicitacaoAutorizacaoRecorrenciaRepository repository;
    @InjectMocks
    private SolicitacaoAutorizacaoRecorrenciaServiceImpl service;

    @Nested
    class ConsultarTodasSolicitacoes {
        @Test
        void dadosParametrosValidos_quandoBuscarTodas_deveRetornarSolicitacoesAutorizacao() {
            var primeiraSolicitacaoAutorizacao = criarMockSolicitacaoAutorizacaoRecorrencia(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO, null);
            var segundaSolicitacaoAutorizacao = criarMockSolicitacaoAutorizacaoRecorrencia(TipoStatusSolicitacaoAutorizacao.CONFIRMADA, "AGUARDANDO_RETORNO");

            var pageable = PageRequest.of(
                    NUMERO_PAGINA_ZERO,
                    TAMANHO_PAGINA_DEZ,
                    Sort.by(Sort.Direction.ASC, "dataCriacaoRecorrencia")
            );

            var request = ConsultaSolicitacaoAutorizacaoRecorrenciaRequest.builder()
                    .agenciaPagador("123")
                    .contaPagador("123456789-0")
                    .status(Set.of("CRIADA", "PENDENTE_CONFIRMACAO"))
                    .tamanhoPagina(TAMANHO_PAGINA_DEZ)
                    .numeroPagina(NUMERO_PAGINA_ZERO)
                    .build();

            when(repository.findAllByFiltros(any(ConsultaSolicitacaoAutorizacaoRecorrenciaRequest.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(primeiraSolicitacaoAutorizacao, segundaSolicitacaoAutorizacao), pageable, 2));

            var response = service.consultarTodas(request);

            assertEquals(2, response.solicitacoes().size());

            var primeiraSolicitacao = response.solicitacoes().getFirst();
            assertEquals(primeiraSolicitacaoAutorizacao.getIdSolicitacaoRecorrencia(), primeiraSolicitacao.getIdSolicitacaoRecorrencia());
            assertEquals(primeiraSolicitacaoAutorizacao.getNomeRecebedor(), primeiraSolicitacao.getNomeRecebedor());
            assertEquals(primeiraSolicitacaoAutorizacao.getDataExpiracaoConfirmacaoSolicitacao(), primeiraSolicitacao.getDataExpiracao());
            assertEquals(primeiraSolicitacaoAutorizacao.getDataCriacaoRecorrencia(), primeiraSolicitacao.getDataCriacao());
            assertEquals(primeiraSolicitacaoAutorizacao.getNumeroContrato(), primeiraSolicitacao.getContrato());
            assertEquals(primeiraSolicitacaoAutorizacao.getDescricao(), primeiraSolicitacao.getDescricao());
            assertEquals(primeiraSolicitacaoAutorizacao.getTipoStatus().toString(), primeiraSolicitacao.getTpoStatus());
            assertNull(primeiraSolicitacao.getTpoSubStatus());

            var segundaSolicitacao = response.solicitacoes().getLast();
            assertEquals(segundaSolicitacaoAutorizacao.getIdSolicitacaoRecorrencia(), segundaSolicitacao.getIdSolicitacaoRecorrencia());
            assertEquals(segundaSolicitacaoAutorizacao.getNomeRecebedor(), segundaSolicitacao.getNomeRecebedor());
            assertEquals(segundaSolicitacaoAutorizacao.getDataExpiracaoConfirmacaoSolicitacao(), segundaSolicitacao.getDataExpiracao());
            assertEquals(segundaSolicitacaoAutorizacao.getDataCriacaoRecorrencia(), segundaSolicitacao.getDataCriacao());
            assertEquals(segundaSolicitacaoAutorizacao.getNumeroContrato(), segundaSolicitacao.getContrato());
            assertEquals(segundaSolicitacaoAutorizacao.getDescricao(), segundaSolicitacao.getDescricao());
            assertEquals(segundaSolicitacaoAutorizacao.getTipoStatus().toString(), segundaSolicitacao.getTpoStatus());
            assertEquals(segundaSolicitacaoAutorizacao.getTipoSubStatus(), segundaSolicitacao.getTpoSubStatus());

            verify(repository, times(1)).findAllByFiltros(eq(request), any(Pageable.class));
        }

        public static SolicitacaoAutorizacaoRecorrencia criarMockSolicitacaoAutorizacaoRecorrencia(TipoStatusSolicitacaoAutorizacao status, String subStatus) {
            return SolicitacaoAutorizacaoRecorrencia.builder()
                    .idSolicitacaoRecorrencia("SC0118152120250425041bYqAj6ef")
                    .nomeRecebedor("EMPRESA XYZ LTDA")
                    .dataExpiracaoConfirmacaoSolicitacao(LocalDateTime.of(2025, 5, 6, 18, 0))
                    .numeroContrato("21312331-1")
                    .descricao("Parcela Plano XYZ")
                    .tipoStatus(status)
                    .tipoSubStatus(subStatus)
                    .build();
        }
    }

    @Nested
    class ProcessarRetornoSolicitacaoAutorizacao {

        @Test
        void dadoPedidoAprovado_quandoProcessarRetornoSolicitacao_deveAtualizarStatusEEnviarPush() {
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();
            var retornoSolicitacao = criarRetornoSolicitacaoAutorizacao(Boolean.TRUE);
            var solicitacaoAutorizacaoRecorrencia = criarSolicitacaoAutorizacaoRecorrencia();

            when(idempotentRequestPain012.getValue()).thenReturn(retornoSolicitacao);
            when(eventoResponseFactory.criarEventoNotificacao(any())).thenReturn(new EventoResponseDTO(NotificacaoDTO.builder().build(), new HashMap<>(), TOPICO));
            when(repository.findByIdInformacaoStatusAndIdRecorrencia(any(), any())).thenReturn(Optional.of(solicitacaoAutorizacaoRecorrencia));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            service.processarRetornoBacenSolicitacaoAutorizacao(idempotentRequestPain012);

            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());
            verify(eventoResponseFactory, times(1)).criarEventoNotificacao(captorNotificacao.capture());

            var valueRecorrencia = captorRecorrenciaAutorizacaoSolicitacao.getValue();
            var valueNotificacao = captorNotificacao.getValue();

            assertAll(
                    () -> assertEquals(PENDENTE_CONFIRMACAO, valueRecorrencia.getTipoStatus()),

                    () -> assertEquals(AUTOMATICO_AUTORIZACAO_PENDENTE_DE_APROVACAO, valueNotificacao.getOperacao()),
                    () -> assertEquals("SICREDI_APP", valueNotificacao.getCanal()),
                    () -> assertEquals(solicitacaoAutorizacaoRecorrencia.getContaPagador(), valueNotificacao.getConta()),
                    () -> assertEquals(solicitacaoAutorizacaoRecorrencia.getAgenciaPagador(), valueNotificacao.getAgencia()),
                    () -> assertEquals(5, valueNotificacao.getInformacoesAdicionais().size())
            );
        }

        @Test
        void dadoPedidoRejeitado_quandoProcessarRetornoSolicitacao_deveAtualizarStatusEEnviarPush() {
            var retornoSolicitacao = criarRetornoSolicitacaoAutorizacao(Boolean.FALSE);
            var solicitacaoAutorizacaoRecorrencia = criarSolicitacaoAutorizacaoRecorrencia();
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain012.getValue()).thenReturn(retornoSolicitacao);
            when(repository.findByIdInformacaoStatusAndIdRecorrencia(any(), any())).thenReturn(Optional.of(solicitacaoAutorizacaoRecorrencia));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            service.processarRetornoBacenSolicitacaoAutorizacao(idempotentRequestPain012);

            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());
            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());

            var valueRecorrencia = captorRecorrenciaAutorizacaoSolicitacao.getValue();

            assertEquals(REJEITADA, valueRecorrencia.getTipoStatus());
        }

        @Test
        void dadoSolicitacaoNaoEncontrada_quandoProcessarRetornoSolicitacao_deveEnviarExececao() {
            var retornoSolicitacao = criarRetornoSolicitacaoAutorizacao(Boolean.FALSE);

            when(idempotentRequestPain012.getValue()).thenReturn(retornoSolicitacao);
            when(repository.findByIdInformacaoStatusAndIdRecorrencia(any(), any())).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> service.processarRetornoBacenSolicitacaoAutorizacao(idempotentRequestPain012));

            verify(repository, never()).save(captorRecorrenciaAutorizacaoSolicitacao.capture());
            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());
        }

        @Test
        void dadoExcecaoAoSalvarNoBanco_quandoProcessarRetornoSolicitacao_deveEnviarTechnicalException() {
            var retornoSolicitacao = criarRetornoSolicitacaoAutorizacao(Boolean.FALSE);
            var solicitacaoAutorizacaoRecorrencia = criarSolicitacaoAutorizacaoRecorrencia();

            when(idempotentRequestPain012.getValue()).thenReturn(retornoSolicitacao);
            when(repository.findByIdInformacaoStatusAndIdRecorrencia(any(), any())).thenReturn(Optional.of(solicitacaoAutorizacaoRecorrencia));
            when(repository.save(any())).thenThrow(new DataIntegrityViolationException("Erro ao salvar"));

            assertThrows(TechnicalException.class,
                    () -> service.processarRetornoBacenSolicitacaoAutorizacao(idempotentRequestPain012));

            verify(repository, times(1)).save(any());
            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());
        }

        private SolicitacaoAutorizacaoRecorrencia criarSolicitacaoAutorizacaoRecorrencia() {
            return SolicitacaoAutorizacaoRecorrencia.builder()
                    .nomeRecebedor(NOME_RECEBEDOR)
                    .nomeDevedor(NOME_RECEBEDOR)
                    .cpfCnpjPagador(CPF_PAGADOR)
                    .descricao("DESCRICAO")
                    .valor(new BigDecimal(1))
                    .agenciaPagador(AGENCIA)
                    .contaPagador(CONTA)
                    .tipoSistemaPagador(OrigemEnum.FISITAL)
                    .dataExpiracaoConfirmacaoSolicitacao(LocalDateTime.now())
                    .build();
        }

        private Pain012Dto criarRetornoSolicitacaoAutorizacao(Boolean status) {
            return Pain012Dto.builder()
                    .status(status)
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
    }

    @Nested
    class ProcessarSolicitacaoAutorizacao {

        @Test
        void dadoPedidoAutorizacaoValido_quandoProcessarPedidoAutorizacao_deveCriarSolicitacaoEPain012() {
            var pedidoAutorizacao = criarSolicitacaoAutorizacao();
            var dadosConta = criarDadosContaPagador(CPF_CNPJ, STATUS_CONTA, false);
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain009.getValue()).thenReturn(pedidoAutorizacao);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(accountDataService.consultarConta(any(), any(), any())).thenReturn(dadosConta);
            when(gestentConectorService.consultarCodigoMunicipio(dadosConta.posto(), dadosConta.coop())).thenReturn(CODIGO_IBGE);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarSolicitacaoAutorizacao(idempotentRequestPain009);

            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_SOLICITACAO));

            var valueRecorrencia = captorRecorrenciaAutorizacaoSolicitacao.getValue();
            var valuePain012 = captorPain012.getValue();

            assertAll("Valida campos da RecorrenciaAutorizacaoSolicitacao persistida",
                    () -> assertNotNull(valueRecorrencia),
                    () -> assertNotNull(valueRecorrencia.getIdInformacaoStatus()),
                    () -> assertEquals(pedidoAutorizacao.getIdRecorrencia(), valueRecorrencia.getIdRecorrencia()),
                    () -> assertEquals(OrigemEnum.FISITAL, valueRecorrencia.getTipoSistemaPagador()),
                    () -> assertEquals(pedidoAutorizacao.getIdSolicitacaoRecorrencia(), valueRecorrencia.getIdSolicitacaoRecorrencia()),
                    () -> assertEquals(pedidoAutorizacao.getCpfCnpjUsuarioPagador(), valueRecorrencia.getCpfCnpjPagador()),
                    () -> assertEquals(dadosConta.titular().nome(), valueRecorrencia.getNomePagador()),
                    () -> assertEquals(pedidoAutorizacao.getValor(), valueRecorrencia.getValor()),
                    () -> assertEquals(pedidoAutorizacao.getPisoValorMaximo(), valueRecorrencia.getPisoValorMaximo()),
                    () -> assertEquals(pedidoAutorizacao.getParticipanteDoUsuarioPagador(), valueRecorrencia.getInstituicaoPagador()),
                    () -> assertEquals(dadosConta.posto(), valueRecorrencia.getPostoPagador()),
                    () -> assertEquals(dadosConta.tipoConta().getAsCanaisPixNomeSimples(), valueRecorrencia.getTipoContaPagador()),
                    () -> assertEquals(pedidoAutorizacao.getCpfCnpjUsuarioRecebedor(), valueRecorrencia.getCpfCnpjRecebedor()),
                    () -> assertEquals(pedidoAutorizacao.getNomeUsuarioRecebedor(), valueRecorrencia.getNomeRecebedor()),
                    () -> assertEquals(pedidoAutorizacao.getTipoFrequencia(), valueRecorrencia.getTipoFrequencia()),
                    () -> assertEquals(TipoStatusSolicitacaoAutorizacao.CRIADA, valueRecorrencia.getTipoStatus()),
                    () -> assertEquals(CODIGO_IBGE, valueRecorrencia.getCodigoMunicipioIBGE())
            );

            assertAll("Valida dados da PAIN012 criada",
                    () -> assertNotNull(valuePain012),

                    () -> assertTrue(valuePain012.getStatus()),
                    () -> assertEquals(PENDENTE_CONFIRMACAO.name(), valuePain012.getStatusRecorrencia()),
                    () -> assertEquals(pedidoAutorizacao.getIdRecorrencia(), valuePain012.getIdRecorrencia()),
                    () -> assertEquals(pedidoAutorizacao.getCpfCnpjUsuarioPagador(), valuePain012.getCpfCnpjUsuarioPagador()),
                    () -> assertEquals(pedidoAutorizacao.getNomeUsuarioRecebedor(), valuePain012.getNomeUsuarioRecebedor()),
                    () -> assertEquals(pedidoAutorizacao.getDescricao(), valuePain012.getDescricao()),
                    () -> assertEquals(CODIGO_IBGE, valuePain012.getCodMunIBGE()),
                    () -> assertNotNull(valuePain012.getDetalhesRecorrencias()),
                    () -> assertEquals(2, valuePain012.getDetalhesRecorrencias().size()),

                    // Detalhe "CRTN"
                    () -> assertEquals(CRIACAO_RECORRENCIA.name(), valuePain012.getDetalhesRecorrencias().getFirst().getTipoSituacaoDaRecorrencia()),

                    // Detalhe "UPDT"
                    () -> assertEquals(ATUALIZACAO_STATUS_RECORRENCIA.name(), valuePain012.getDetalhesRecorrencias().get(1).getTipoSituacaoDaRecorrencia()),
                    () -> assertNotNull(valuePain012.getDetalhesRecorrencias().get(1).getDataHoraTipoSituacaoDaRecorrencia())
            );
        }
    }

    @Nested
    class ProcessarSolicitacaoAutorizacaoErroNegocio {

        @Test
        void dadoPedidoAutorizacaoJaExistente_quandoProcessarPedidoAutorizacao_deveRetornarErroAM05() {
            var pedidoAutorizacao = criarSolicitacaoAutorizacao();
            var conta = criarDadosContaPagador(CPF_CNPJ, STATUS_CONTA, false);

            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            var statusSolicitacaoAutorizacao = List.of(PENDENTE_CONFIRMACAO, ACEITA);

            when(accountDataService.consultarConta(any(), any(), any())).thenReturn(conta);
            when(gestentConectorService.consultarCodigoMunicipio(any(), any())).thenReturn(CODIGO_IBGE);
            when(idempotentRequestPain009.getValue()).thenReturn(pedidoAutorizacao);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(repository.findFirstByIdRecorrenciaAndTipoStatusIn(pedidoAutorizacao.getIdRecorrencia(), statusSolicitacaoAutorizacao))
                    .thenReturn(Optional.of(new SolicitacaoAutorizacaoRecorrencia()));
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarSolicitacaoAutorizacao(idempotentRequestPain009);

            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_SOLICITACAO));

            var valueSolicitacao = captorRecorrenciaAutorizacaoSolicitacao.getValue();
            var valuePain012 = captorPain012.getValue();

            validarDadosRecorrenciaAutorizacaoSolicitacaoComErro(pedidoAutorizacao, CODIGO_IBGE, valueSolicitacao, JA_CONFIRMADA_PREVIAMENTE_OU_STATUS_PENDENTE.name());
            validarDadosPain012ComErro(valuePain012, pedidoAutorizacao, CODIGO_IBGE, JA_CONFIRMADA_PREVIAMENTE_OU_STATUS_PENDENTE.name());
        }

        @Test
        void dadoContaNaoExistente_quandoProcessarPedidoAutorizacao_deveRetornarErroAC01() {
            var pedidoAutorizacao = criarSolicitacaoAutorizacao();
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(accountDataService.consultarConta(any(), any(), any())).thenReturn(null);
            when(idempotentRequestPain009.getValue()).thenReturn(pedidoAutorizacao);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            service.processarSolicitacaoAutorizacao(idempotentRequestPain009);

            verify(gestentConectorService, never()).consultarCodigoMunicipio(any(), any());
            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_SOLICITACAO));

            var valueSolicitacao = captorRecorrenciaAutorizacaoSolicitacao.getValue();
            var valuePain012 = captorPain012.getValue();

            validarDadosRecorrenciaAutorizacaoSolicitacaoComErro(pedidoAutorizacao, null, valueSolicitacao, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA.name());
            validarDadosPain012ComErro(valuePain012, pedidoAutorizacao, null, CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA.name());
        }

        @Test
        void dadoCpfTitularDiferente_quandoProcessarPedidoAutorizacao_deveRetornarErroAP02() {
            var pedidoAutorizacao = criarSolicitacaoAutorizacao();
            var conta = criarDadosContaPagador("12345678541", STATUS_CONTA, false);
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain009.getValue()).thenReturn(pedidoAutorizacao);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));
            when(accountDataService.consultarConta(any(), any(), any())).thenReturn(conta);
            when(gestentConectorService.consultarCodigoMunicipio(any(), any())).thenReturn(CODIGO_IBGE);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            service.processarSolicitacaoAutorizacao(idempotentRequestPain009);

            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_SOLICITACAO));
            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());

            var valueSolicitacao = captorRecorrenciaAutorizacaoSolicitacao.getValue();
            var valuePain012 = captorPain012.getValue();

            validarDadosRecorrenciaAutorizacaoSolicitacaoComErro(pedidoAutorizacao, CODIGO_IBGE, valueSolicitacao, CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO.name());
            validarDadosPain012ComErro(valuePain012, pedidoAutorizacao, CODIGO_IBGE,  CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO.name());
        }

        @Test
        void dadoContaCanceladaOuEncerrada_quandoProcessarPedidoAutorizacao_deveRetornarErroAC04() {
            var pedidoAutorizacao = criarSolicitacaoAutorizacao();
            var conta = criarDadosContaPagador(CPF_CNPJ, "CLOSED", false);
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(idempotentRequestPain009.getValue()).thenReturn(pedidoAutorizacao);
            when(accountDataService.consultarConta(any(), any(), any())).thenReturn(conta);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);
            when(gestentConectorService.consultarCodigoMunicipio(any(), any())).thenReturn(CODIGO_IBGE);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));

            service.processarSolicitacaoAutorizacao(idempotentRequestPain009);

            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());
            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_SOLICITACAO));

            var valueSolicitacao = captorRecorrenciaAutorizacaoSolicitacao.getValue();
            var valuePain012 = captorPain012.getValue();

            validarDadosRecorrenciaAutorizacaoSolicitacaoComErro(pedidoAutorizacao, CODIGO_IBGE, valueSolicitacao, CONTA_TRANSACIONAL_USUARIO_PAGADOR_ENCERRADA.name());
            validarDadosPain012ComErro(valuePain012, pedidoAutorizacao, CODIGO_IBGE, CONTA_TRANSACIONAL_USUARIO_PAGADOR_ENCERRADA.name());
        }

        @Test
        void dadoContaComBloqueioDeCredito_quandoProcessarPedidoAutorizacao_deveRetornarErroAC06() {
            var pedidoAutorizacao = criarSolicitacaoAutorizacao();
            var conta = criarDadosContaPagador(CPF_CNPJ, STATUS_CONTA, true);
            IdempotentResponse<Pain012Dto> idempotentResponse = IdempotentResponse.<Pain012Dto>builder().errorResponse(false).build();

            when(accountDataService.consultarConta(any(), any(), any())).thenReturn(conta);
            when(gestentConectorService.consultarCodigoMunicipio(any(), any())).thenReturn(CODIGO_IBGE);
            when(idempotentRequestPain009.getValue()).thenReturn(pedidoAutorizacao);
            when(eventoResponseFactory.criarEventoPain012(any(), any())).thenReturn(new EventoResponseDTO(Pain012Dto.builder().build(), new HashMap<>(), TOPICO));
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO)).thenReturn(criaResponseStrategy);
            when(criaResponseStrategy.criarResponseIdempotentSucesso(any(IdempotenteRequest.class), any(), any(),  any())).thenReturn(idempotentResponse);

            service.processarSolicitacaoAutorizacao(idempotentRequestPain009);

            verify(eventoResponseFactory, times(1)).criarEventoPain012(captorPain012.capture(), eq(HEADER_OPERACAO_RECORRENCIA_SOLICITACAO));
            verify(repository, times(1)).save(captorRecorrenciaAutorizacaoSolicitacao.capture());

            var valueSolicitacao = captorRecorrenciaAutorizacaoSolicitacao.getValue();
            var valuePain012 = captorPain012.getValue();

            validarDadosRecorrenciaAutorizacaoSolicitacaoComErro(pedidoAutorizacao, CODIGO_IBGE, valueSolicitacao, CONTA_TRANSACIONAL_USUARIO_PAGADOR_BLOQUEADA.name());
            validarDadosPain012ComErro(valuePain012, pedidoAutorizacao, CODIGO_IBGE, CONTA_TRANSACIONAL_USUARIO_PAGADOR_BLOQUEADA.name());
        }

        private void validarDadosPain012ComErro(Pain012Dto pain012Dto, Pain009Dto pain009Dto, String codigoIbge, String codigoErro) {
            assertAll("Valida PAIN012 com erro de negócio",
                    () -> assertFalse(pain012Dto.getStatus()),
                    () -> assertEquals(codigoErro, pain012Dto.getMotivoRejeicao()),
                    () -> assertEquals(pain009Dto.getIdRecorrencia(), pain012Dto.getIdRecorrencia()),
                    () -> assertEquals(pain009Dto.getCpfCnpjUsuarioPagador(), pain012Dto.getCpfCnpjUsuarioPagador()),
                    () -> assertEquals(pain009Dto.getNomeUsuarioRecebedor(), pain012Dto.getNomeUsuarioRecebedor()),
                    () -> assertEquals(pain009Dto.getDescricao(), pain012Dto.getDescricao()),
                    () -> assertEquals(codigoIbge, pain012Dto.getCodMunIBGE()),
                    () -> assertNull(pain012Dto.getStatusRecorrencia(), "statusRecorrencia deve ser null quando erro"),
                    () -> assertNull(pain012Dto.getDetalhesRecorrencias(), "detalhesRecorrencias deve ser null quando erro")
            );
        }

        private void validarDadosRecorrenciaAutorizacaoSolicitacaoComErro(Pain009Dto pedido, String codigoIbge, SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacao, String codigoErro) {
            assertAll("Valida dados básicos da recorrência",
                    () -> assertEquals(codigoErro, solicitacaoAutorizacao.getMotivoRejeicao()),
                    () -> assertEquals(pedido.getIdRecorrencia(), solicitacaoAutorizacao.getIdRecorrencia()),
                    () -> assertEquals(pedido.getIdRecorrencia(), solicitacaoAutorizacao.getIdRecorrencia()),
                    () -> assertEquals(pedido.getIdSolicitacaoRecorrencia(), solicitacaoAutorizacao.getIdSolicitacaoRecorrencia()),
                    () -> assertEquals(pedido.getContaUsuarioPagador(), solicitacaoAutorizacao.getContaPagador()),
                    () -> assertEquals(codigoIbge, solicitacaoAutorizacao.getCodigoMunicipioIBGE())
            );
        }

    }

    private DadosContaResponseDTO criarDadosContaPagador(String documento, String status, boolean temBloqueioCredito) {
        return DadosContaResponseDTO.builder()
                .posto("12")
                .coop("0101")
                .status(status)
                .sistema("DIGITAL")
                .temCreditoBloqueado(temBloqueioCredito)
                .titular(DadosPessoaContaDTO.builder().documento(documento).nome(TESTE).build())
                .tipoConta(TipoContaEnum.CHECKING_ACCOUNT)
                .build();
    }

    private Pain009Dto criarSolicitacaoAutorizacao() {
        return Pain009Dto.builder()
                .idRecorrencia("rec-123456")
                .idSolicitacaoRecorrencia("SC0118152120250425041bYqAj6ef")
                .tipoRecorrencia(TESTE)
                .tipoFrequencia(TESTE)
                .dataInicialRecorrencia(LocalDate.parse("2025-05-01"))
                .dataFinalRecorrencia(LocalDate.parse("2025-12-01"))
                .indicadorObrigatorio(true)
                .valor(BigDecimal.valueOf(20.75))
                .indicadorPisoValorMaximo(false)
                .pisoValorMaximo(BigDecimal.valueOf(20.00))
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
                .detalhesRecorrenciaPain009Dto(List.of(
                        new DetalheRecorrenciaPain009Dto(DATA_CRIACAO_SOLICITACAO_CONFIRMACAO.name(), LocalDateTime.parse("2025-07-18T12:00:00")),
                        new DetalheRecorrenciaPain009Dto(DATA_CRIACAO_RECORRENCIA.name(), LocalDateTime.parse("2025-07-18T13:00:00")),
                        new DetalheRecorrenciaPain009Dto(DATA_EXPIRACAO_SOLICITACAO_CONFIRMACAO.name(), LocalDateTime.parse("2025-07-18T15:00:00"))
                ))
                .build();
    }


    @Nested
    class ConsultaDetalhes {

        private static final String ID_RECORRENCIA_AUTORIZACAO= "1";
        private static final String ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO = "1";

        @Test
        void dadoIdentificadorSolicitacao_quandoConsultarDetalhes_deveRetornarSolicitacao() {
            var solicitacaoMock = TestFactory.SolicitacaoTestFactory.criarAutorizacao();
            solicitacaoMock.setIdSolicitacaoRecorrencia(ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO);


            when(repository.findById(ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO))
                    .thenReturn(Optional.of(solicitacaoMock));

            var autorizacaoResponse = service.consultarDetalhes(ID_RECORRENCIA_AUTORIZACAO);

            assertEquals(ID_RECORRENCIA_AUTORIZACAO, autorizacaoResponse.getIdSolicitacaoRecorrencia());


            verify(repository).findById(ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO);
        }

        @Test
        void dadoIdentificadorSolicitacao_quandoConsultarDetalhes_deveLancarNotFoundException() {
            when(repository.findById(ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> service.consultarDetalhes(ID_RECORRENCIA_AUTORIZACAO));

            verify(repository).findById(ID_SOLICITACAO_RECORRENCIA_AUTORIZACAO);
        }

    }

}