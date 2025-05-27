package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.CadastroOrdemRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaProtocoloRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.dict.DictConsultaDTO;
import io.sicredi.spirecorrencia.api.dict.TipoContaDictEnum;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.RecorrenciaTransacaoTentativa;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.RecorrenciaTransacaoTentativaRepository;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_OPERACIONAL;
import static io.sicredi.spirecorrencia.api.utils.TestConstants.PATH_EMITIR_CANCELAMENTO;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/data_liquidacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
        }
)
@TestPropertySource(properties = "app.scheduling.enable=false")
public class ProcessamentoLiquidacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String TOPICO_NOTIFICACAO = "spi-notificacao-recorrencia-v2";
    private static final String PATH_CONSULTA_CHAVE = "/v2/chaves/{key}?incluirEstatisticas=false";
    private static final String PATH_EMITIR_LIQUIDACAO = "/v1/recorrencias/liquidacao";
    private static final String PATH_PARTICIPANTE = "/v2/participantes/pagging?page=0&size=1&situacao=ENABLE&ispb={ispb}";
    private static final String TIPO_PRODUTO = "AGENDADO_RECORRENTE";
    private static final String PATH_KEY = "{key}";
    private static final String PATH_ISPB = "{ispb}";
    private static final String CPF = "12345678901";
    public static final String ISPB = "99999004";
    @Autowired
    private RecorrenciaRepository recorrenciaRepository;
    @Autowired
    private RecorrenciaTransacaoTentativaRepository tentativaRepository;
    @Autowired
    private ProcessamentoLiquidacaoJob processamentoLiquidacaoJob;
    @Autowired
    private ConfigurableApplicationContext context;
    @Autowired
    private CacheManager cacheManager;
    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
        cacheManager.getCache("participanteCache").clear();
    }

    @Test
    void dadoParcelasParaLiquidar_quandoExecutarJob_deveEmitirProtocoloLiquidacao() {
        var urlConsultaChaveDict = PATH_CONSULTA_CHAVE.replace(PATH_KEY, CPF);
        var urlConsultaParticipante = PATH_PARTICIPANTE.replace(PATH_ISPB, ISPB);

        criarStubMockResponse(urlConsultaChaveDict, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(criarDictConsultaDTO()));
        criarStubMockResponse(urlConsultaParticipante, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(consultaNomeInstituicao(ISPB)));
        criarStubMockResponse(PATH_EMITIR_LIQUIDACAO, HttpMethod.POST, HttpStatus.OK, "");

        processamentoLiquidacaoJob.executar();

        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(PATH_EMITIR_LIQUIDACAO)));
            wireMockServer.verify(1, getRequestedFor(urlEqualTo(urlConsultaParticipante)));

            var cadastroOrdemRequest = obterCadastroOrdemRequest(PATH_EMITIR_LIQUIDACAO);

            var recorrencia = recorrenciaRepository.findByIdRecorrencia("2f335153-4d6a-4af1-92a0-c52c5c827af9").orElseThrow();
            var parcelaCriada = obterParcelaTransacao(recorrencia);

            validarDadosRequest(cadastroOrdemRequest, parcelaCriada, OrigemEnum.LEGADO);
        });
    }

    @Test
    void dadoChaveDictNaoEncontrada_quandoExecutarJob_deveEmitirEventoExclusao() {
        var urlConsultaProtocolo = PATH_CONSULTA_CHAVE.replace(PATH_KEY, CPF);

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.NOT_FOUND, "");
        criarStubMockResponse(PATH_EMITIR_CANCELAMENTO, HttpMethod.POST, HttpStatus.OK, "");

        processamentoLiquidacaoJob.executar();

        validarDadosErro("REC_PROC_BU0002", "Dados da chave não localizado no DICT.", 2, RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE);
    }

    @Test
    void dadoErroAoEmitirProtocoloLiquidacao_quandoExecutarJob_deveNaoEmitirEventoExclusao() {
        var urlConsultaChaveDict = PATH_CONSULTA_CHAVE.replace(PATH_KEY, CPF);
        var urlConsultaParticipante = PATH_PARTICIPANTE.replace(PATH_ISPB, ISPB);

        criarStubMockResponse(urlConsultaChaveDict, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(criarDictConsultaDTO()));
        criarStubMockResponse(urlConsultaParticipante, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(consultaNomeInstituicao(ISPB)));
        criarStubMockResponse(PATH_EMITIR_LIQUIDACAO, HttpMethod.POST, HttpStatus.BAD_REQUEST, "");

        processamentoLiquidacaoJob.executar();

        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(PATH_EMITIR_LIQUIDACAO)));
            wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(PATH_EMITIR_CANCELAMENTO)));
        });
    }

    @Test
    @SqlGroup(
            value = {
                    @Sql(scripts = {"/db/clear.sql", "/db/data_liquidacao_open_finance.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                    @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
            }
    )
    void dadoErroAoEmitirProtocoloLiquidacao_quandoExecutarJob_deveEmitirEventoExclusao() {
        var urlConsultaChaveDict = PATH_CONSULTA_CHAVE.replace(PATH_KEY, CPF);
        var urlConsultaParticipante = PATH_PARTICIPANTE.replace(PATH_ISPB, ISPB);

        criarStubMockResponse(urlConsultaChaveDict, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(criarDictConsultaDTO()));
        criarStubMockResponse(urlConsultaParticipante, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(consultaNomeInstituicao(ISPB)));
        criarStubMockResponse(PATH_EMITIR_LIQUIDACAO, HttpMethod.POST, HttpStatus.BAD_REQUEST, "");
        criarStubMockResponse(PATH_EMITIR_CANCELAMENTO, HttpMethod.POST, HttpStatus.OK, "");

        processamentoLiquidacaoJob.executar();

        validarDadosErro("SPIRECORRENCIA_BU0027", "Houve um erro durante a emissão de protocolo de liquidação", 1, RECORRENCIA_FALHA_OPERACIONAL);
    }

    @Test
    @SqlGroup(
            value = {
                    @Sql(scripts = {"/db/clear.sql", "/db/data_liquidacao_open_finance.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                    @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
            }
    )
    void dadoErroAoEmitirProtocoloLiquidacao_quandoExecutarJob_deveEnviarExcecaoAoExcluirParcela() {
        var urlConsultaChaveDict = PATH_CONSULTA_CHAVE.replace(PATH_KEY, CPF);
        var urlConsultaParticipante = PATH_PARTICIPANTE.replace(PATH_ISPB, ISPB);

        criarStubMockResponse(urlConsultaChaveDict, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(criarDictConsultaDTO()));
        criarStubMockResponse(urlConsultaParticipante, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(consultaNomeInstituicao(ISPB)));

        criarStubMockResponse(PATH_EMITIR_LIQUIDACAO, HttpMethod.POST, HttpStatus.BAD_REQUEST, "");
        criarStubMockResponse(PATH_EMITIR_CANCELAMENTO, HttpMethod.POST, HttpStatus.BAD_REQUEST, "");

        processamentoLiquidacaoJob.executar();

        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(PATH_EMITIR_CANCELAMENTO)));

            var exclusaoRecorrenciaProtocoloRequest = obterExclusaoRecorrenciaRequest(PATH_EMITIR_CANCELAMENTO);

            var tentativas = tentativaRepository.findAll();
            var recorrencia = recorrenciaRepository.findByIdRecorrencia(exclusaoRecorrenciaProtocoloRequest.getIdentificadorRecorrencia()).orElseThrow();

            validarIdentificacaoAssociado(recorrencia, exclusaoRecorrenciaProtocoloRequest);
            validarDetalhesExclusao(exclusaoRecorrenciaProtocoloRequest, recorrencia, 1);
            validarTentativaProcessamento(tentativas, "SPIRECORRENCIA_BU0027", "Houve um erro durante a emissão de protocolo de liquidação");
        });
    }

    @Test
    void dadoChaveRecebedorNaoPertencenteAtitular_quandoExecutarJob_deveExcluirTodasParcelas() {
        var urlConsultaProtocolo = PATH_CONSULTA_CHAVE.replace(PATH_KEY, CPF);

        DictConsultaDTO consultaDTO = criarDictConsultaDTO();
        consultaDTO.setCpfCnpj("15248672225");

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.OK, ObjectMapperUtil.converterObjetoParaString(consultaDTO));
        criarStubMockResponse(PATH_EMITIR_CANCELAMENTO, HttpMethod.POST, HttpStatus.OK, "");

        processamentoLiquidacaoJob.executar();

        validarDadosErro("REC_PROC_BU0001", "Dados do recebedor difere dos dados no DICT.", 2, RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE);
    }

    @Test
    void dadoErroGenerico_quandoExecutarJob_deveFinalizarProcessoIgnorandoProcessamento() {
        var urlConsultaProtocolo = PATH_CONSULTA_CHAVE.replace(PATH_KEY, CPF);

        criarStubMockResponse(urlConsultaProtocolo, HttpMethod.GET, HttpStatus.BAD_REQUEST, "");

        processamentoLiquidacaoJob.executar();

        wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(PATH_EMITIR_CANCELAMENTO)));
    }

    private void validarDadosErro(String codigo, String mensagemErro, int quantidadeParcelas, NotificacaoDTO.TipoTemplate tipoTemplate) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(PATH_EMITIR_CANCELAMENTO)));

            var exclusaoRecorrenciaProtocoloRequest = obterExclusaoRecorrenciaRequest(PATH_EMITIR_CANCELAMENTO);

            var recorrencia = recorrenciaRepository.findByIdRecorrencia(exclusaoRecorrenciaProtocoloRequest.getIdentificadorRecorrencia()).orElseThrow();
            var tentativas = tentativaRepository.findAll();

            validarIdentificacaoAssociado(recorrencia, exclusaoRecorrenciaProtocoloRequest);
            validarDetalhesExclusao(exclusaoRecorrenciaProtocoloRequest, recorrencia, quantidadeParcelas);
            validarTentativaProcessamento(tentativas, codigo, mensagemErro);

            ConsumerRecord<String, String> mensagemConsumida = getLastRecord(consumer, TOPICO_NOTIFICACAO);
            assertTrue(mensagemConsumida.value().contains(tipoTemplate.toString()));
        });
    }

    private CadastroOrdemRequest obterCadastroOrdemRequest(String path) {
        var requests = wireMockServer.findAll(postRequestedFor(urlPathEqualTo(path)));
        return ObjectMapperUtil.converterStringParaObjeto(new String(requests.getFirst().getBody(), StandardCharsets.UTF_8), new TypeReference<CadastroOrdemRequest>() {
        });
    }

    private ExclusaoRecorrenciaProtocoloRequest obterExclusaoRecorrenciaRequest(String path) {
        var requests = wireMockServer.findAll(postRequestedFor(urlPathEqualTo(path)));
        return ObjectMapperUtil.converterStringParaObjeto(new String(requests.getFirst().getBody(), StandardCharsets.UTF_8), new TypeReference<ExclusaoRecorrenciaProtocoloRequest>() {
        });
    }

    private RecorrenciaTransacao obterParcelaTransacao(Recorrencia recorrencia) {
        return recorrencia.getRecorrencias()
                .stream()
                .filter(parcela -> "2DDF2A2BCF9FE8B2E063C50A17ACB3A4".equals(parcela.getIdParcela()))
                .findFirst()
                .orElseThrow();
    }

    private void validarIdentificacaoAssociado(Recorrencia recorrencia, ExclusaoRecorrenciaProtocoloRequest request) {
        var pagador = recorrencia.getPagador();

        assertAll("Identificação do Associado",
                () -> assertEquals(pagador.getTipoConta().getTipoContaCanaisDigitais(), request.getIdentificacaoAssociado().getTipoConta()),
                () -> assertEquals(pagador.getCpfCnpj(), request.getIdentificacaoAssociado().getCpfUsuario()),
                () -> assertEquals(pagador.getCpfCnpj(), request.getIdentificacaoAssociado().getCpfCnpjConta()),
                () -> assertEquals(pagador.getConta(), request.getIdentificacaoAssociado().getConta()),
                () -> assertEquals(pagador.getCodPosto(), request.getIdentificacaoAssociado().getAgencia()),
                () -> assertEquals(pagador.getNome(), request.getIdentificacaoAssociado().getNomeAssociadoConta()),
                () -> assertEquals(pagador.getAgencia(), request.getIdentificacaoAssociado().getCooperativa()),
                () -> assertEquals(recorrencia.getTipoOrigemSistema(), request.getIdentificacaoAssociado().getOrigemConta())
        );
    }

    private void validarDetalhesExclusao(ExclusaoRecorrenciaProtocoloRequest request, Recorrencia recorrencia, int quantidadeParcelas) {
        assertAll("Detalhes da Exclusão",
                () -> assertEquals(recorrencia.getIdRecorrencia(), request.getIdentificadorRecorrencia()),
                () -> assertEquals(TipoMotivoExclusao.SOLICITADO_SISTEMA, request.getTipoMotivoExclusao()),
                () -> assertEquals(quantidadeParcelas, request.getParcelas().size()),
                () -> assertEquals("2DDF2A2BCF9FE8B2E063C50A17ACB3A4", request.getParcelas().getFirst().getIdentificadorParcela())
        );
    }

    private void validarDadosRequest(CadastroOrdemRequest valueCadastroOrdemRequest, RecorrenciaTransacao recorrenciaTransacaoMock, OrigemEnum origemConta) {
        var pagador = recorrenciaTransacaoMock.getRecorrencia().getPagador();
        var recebedor = recorrenciaTransacaoMock.getRecorrencia().getRecebedor();

        assertAll(
                () -> assertEquals(TipoStatusEnum.PENDENTE, recorrenciaTransacaoMock.getTpoStatus()),

                () -> assertEquals(pagador.getTipoConta().getTipoContaCanaisDigitais(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getTipoConta()),
                () -> assertEquals(pagador.getCpfCnpj(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getCpfUsuario()),
                () -> assertEquals(pagador.getCpfCnpj(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getCpfCnpjConta()),
                () -> assertEquals(pagador.getConta(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getConta()),
                () -> assertEquals(pagador.getCodPosto(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getAgencia()),
                () -> assertEquals(pagador.getNome(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getNomeAssociadoConta()),
                () -> assertEquals(pagador.getAgencia(), valueCadastroOrdemRequest.getIdentificacaoAssociado().getCooperativa()),
                () -> assertEquals(origemConta, valueCadastroOrdemRequest.getIdentificacaoAssociado().getOrigemConta()),

                () -> assertEquals(pagador.getInstituicao(), valueCadastroOrdemRequest.getParticipantePagador().getIspb()),
                () -> assertNotNull(valueCadastroOrdemRequest.getParticipantePagador().getNome()),
                () -> assertEquals(recebedor.getInstituicao(), valueCadastroOrdemRequest.getParticipanteRecebedor().getIspb()),
                () -> assertNotNull(valueCadastroOrdemRequest.getParticipanteRecebedor().getNome()),

                () -> assertEquals("NORMAL", valueCadastroOrdemRequest.getPrioridadePagamento()),
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
                () -> assertEquals("E9158698220250319183724pIpVq2Wqk", valueCadastroOrdemRequest.getIdentificadorTransacao())
        );
    }

    private void validarTentativaProcessamento(List<RecorrenciaTransacaoTentativa> transacaoTentativa, String codigo, String mensagem) {
        assertAll("Tentativas de Processamento",
                () -> assertEquals(codigo, transacaoTentativa.getFirst().getCodigo()),
                () -> assertEquals(mensagem, transacaoTentativa.getFirst().getMotivo()),
                () -> assertEquals(1, transacaoTentativa.size())
        );
    }

    private DictConsultaDTO criarDictConsultaDTO() {
        DictConsultaDTO dictConsultaDTO = new DictConsultaDTO();
        dictConsultaDTO.setCpfCnpj(CPF);
        dictConsultaDTO.setAgencia("1234");
        dictConsultaDTO.setConta("567890");
        dictConsultaDTO.setTipoConta(TipoContaDictEnum.CORRENTE);
        dictConsultaDTO.setTipoPessoa(TipoPessoaEnum.PF);
        dictConsultaDTO.setEndToEndBacen("E9158698220250319183724pIpVq2Wqk");
        return dictConsultaDTO;
    }

    private Map<String, Object> consultaNomeInstituicao(String ispb) {
        var contentMap = Map.of(
                "ispb", ispb,
                "nome", "Nome Instituicao ".concat(RandomStringUtils.randomAlphabetic(3,3)),
                "tipo", "DIRETO",
                "situacao", "ENABLE"
        );

        var paginationMap = Map.of(
                "size", "-1",
                "total", "1",
                "page", "0",
                "offset", "0",
                "pageTotal", "1"
        );

        return Map.of(
                "content", List.of(contentMap),
                "pagination", paginationMap
        );
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of(TOPICO_NOTIFICACAO);

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

}