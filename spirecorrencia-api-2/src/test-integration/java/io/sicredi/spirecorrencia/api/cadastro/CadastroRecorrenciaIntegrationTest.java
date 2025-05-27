package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.enums.MarcaEnum;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.ParticipanteRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.CadastroOrdemPagamentoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.RecorrenciaIntegradaRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.*;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"squid:S5960", "SpringJavaInjectionPointsAutowiringInspection", "squid:S6813"})
public class CadastroRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String CPF_RECEBEDOR = "01234567890";
    private static final String CPF_PAGADOR = "12345678900";
    private static final String MSG_RECORRENCIA_NAO_LOCALIZADA = "Recorrencia não localizada";
    private static final String ISPB = "00000000";
    private static final String TOPICO_AGENDADO_RECORRENTE_TRANSACIONAL = "agendado-recorrente-transacional-cadastro-protocolo-v1";
    private static final String TOPICO_AGENDADO_RECORRENTE = "agendado-recorrente-cadastro-protocolo-v1";
    private static final String INFO_USUARIOS_PARCELA = "Informacoes Entre Usuarios Parcela";
    private static final String ID_CONCILIACAO = "idConciliacaoRecebedor";
    private static final String INFO_USUARIOS = "Informacoes entre usuarios";
    private static final String PATH_RECORRENCIA_CADASTRO = "/v1/recorrencias/cadastro";
    private static final String CODIGO_CADASTRO = "358";
    private static final String CODIGO_CADASTRO_INTEGRADO = "410";
    private static final String CONTA = "123456";
    private static final String COOPERATIVA = "1234";

    @Autowired
    protected RecorrenciaRepository recorrenciaRepository;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUpEach() {
        consumer = configurarConsumer();
    }

    @Nested
    class ProcessarAgendamento {

        @Test
        @DisplayName("Deve criar o registro da recorrencia e enviar eventos de protocolo com sucesso quando consumir mensagem de agendado-recorrente")
        void dadoAgendadoUnico_quandoConsumirMensagem_deveCriarRecorrenciaEDispararEventos() {
            var dataAgendado = LocalDateTime.now().plusDays(10);
            var idFimAFimAgendado = SpiUtil.gerarIdFimAFim(TipoId.PAGAMENTO, ISPB, dataAgendado);
            var dadosRecebedor = getRecebedor(CPF_RECEBEDOR);
            var cadastroRecorrenciaRequestDTO = getPayloadListCadastroOrdemPagamentoTransacaoDTO(idFimAFimAgendado, dataAgendado, dadosRecebedor, false);
            var protocoloDTO = getProtocoloDTO(idFimAFimAgendado, CODIGO_CADASTRO, cadastroRecorrenciaRequestDTO, dataAgendado);

            enviarMensagem(protocoloDTO, TOPICO_AGENDADO_RECORRENTE_TRANSACIONAL);

            validarSucessoCadastro(idFimAFimAgendado, COOPERATIVA, CONTA, idFimAFimAgendado , 1, 2);
        }

        @Test
        @DisplayName("Deve criar o registro da recorrencia e enviar eventos de protocolo com sucesso quando consumir mensagem de agendado recorrente")
        void dadoAgendadoComRecorrente_quandoConsumirMensagem_deveCriarRecorrenciaEDispararEventos() {
            criarStubMockResponse(PATH_RECORRENCIA_CADASTRO, HttpMethod.POST, HttpStatus.CREATED, "");
            var dataAgendado = LocalDateTime.now().plusDays(10);
            var idFimAFimAgendado = SpiUtil.gerarIdFimAFim(TipoId.PAGAMENTO, ISPB, dataAgendado);
            var dadosRecebedor = getRecebedor(CPF_RECEBEDOR);
            var cadastroRecorrenciaRequestDTO = getPayloadListCadastroOrdemPagamentoTransacaoDTO(idFimAFimAgendado, dataAgendado, dadosRecebedor, true);
            var protocoloDTO = getProtocoloDTO(idFimAFimAgendado, CODIGO_CADASTRO, cadastroRecorrenciaRequestDTO, dataAgendado);

            enviarMensagem(protocoloDTO, TOPICO_AGENDADO_RECORRENTE_TRANSACIONAL);

            validarSucessoCadastro(idFimAFimAgendado, COOPERATIVA, CONTA, idFimAFimAgendado , 1, 2);

            var requests = wireMockServer.findAll(postRequestedFor(urlPathEqualTo(PATH_RECORRENCIA_CADASTRO)));
            assertEquals(1, requests.size());
            var cadastroRequest = ObjectMapperUtil.converterStringParaObjeto(new String(requests.getFirst().getBody(), StandardCharsets.UTF_8), new TypeReference<CadastroRecorrenciaProtocoloRequest>() {});

            assertAll(
                    () -> assertNotEquals(idFimAFimAgendado, cadastroRequest.getIdentificadorRecorrencia()),
                    () -> assertEquals(TipoRecorrencia.AGENDADO_RECORRENTE, cadastroRequest.getTipoRecorrencia()),
                    () -> assertEquals(2, cadastroRequest.getParcelas().size())
            );
        }

        @Test
        @DisplayName("Deve enviar eventos de erro no protocolo com sucesso sem criar recorrencia quando ocorrer erro de negocio")
        void dadoErroNegocio_quandoConsumirMensagem_deveDispararEventosDeErroSemCriarRecorrencia() {
            var dataAgendado = LocalDateTime.now().plusDays(10);
            var idFimAFimAgendado = SpiUtil.gerarIdFimAFim(TipoId.PAGAMENTO, ISPB, dataAgendado);
            var dadosPagador = getPagador(CPF_PAGADOR);
            var dadosRecebedor = RecorrenteRecebedorDTO.builder()
                    .cpfCnpj(dadosPagador.getCpfCnpj())
                    .nome(dadosPagador.getNome())
                    .agencia(dadosPagador.getAgencia())
                    .conta(dadosPagador.getConta())
                    .instituicao(dadosPagador.getInstituicao())
                    .tipoConta(dadosPagador.getTipoConta())
                    .tipoPessoa(dadosPagador.getTipoPessoa())
                    .tipoChave(TipoChaveEnum.CPF)
                    .chave(CPF_RECEBEDOR)
                    .build();
            var cadastroRecorrenciaRequestDTO = getPayloadListCadastroOrdemPagamentoTransacaoDTO(idFimAFimAgendado, dataAgendado, dadosRecebedor, true);
            var protocoloDTO = getProtocoloDTO(idFimAFimAgendado, CODIGO_CADASTRO, cadastroRecorrenciaRequestDTO, dataAgendado);

            enviarMensagem(protocoloDTO, TOPICO_AGENDADO_RECORRENTE_TRANSACIONAL);

            validarFalhaCadastro(idFimAFimAgendado, COOPERATIVA, CONTA, idFimAFimAgendado , 2);
        }

    }

    @Nested
    class ProcessarRecorrencia {

        @Test
        @DisplayName("Deve criar o registro do agendamento e enviar eventos de protocolo com sucesso quando consumir mensagem de agendado sem recorrencia")
        void dadoRecorrente_quandoConsumirMensagem_deveCriarRecorrenciaEDispararEventos() {
            var idRecorrencia = UUID.randomUUID().toString();
            var idParcela = UUID.randomUUID().toString();
            var dataAgendado = LocalDateTime.now().plusDays(10);
            var pagadorDTO = getPagador(RandomStringUtils.randomNumeric(11));
            var recebedorDTO = getRecebedor(RandomStringUtils.randomNumeric(11));
            var cadastroRecorrenciaRequestDTO = getPayloadListCadastroRecorrenciaTransacaoDTO(idRecorrencia, idParcela, pagadorDTO, recebedorDTO);
            var protocoloDTO = getProtocoloDTO(idRecorrencia, CODIGO_CADASTRO, cadastroRecorrenciaRequestDTO, dataAgendado);

            enviarMensagem(protocoloDTO, TOPICO_AGENDADO_RECORRENTE);

            validarSucessoCadastro(idParcela, pagadorDTO.getAgencia(), pagadorDTO.getConta(), idRecorrencia , 2, 3);
        }

        @Test
        @DisplayName("Deve enviar eventos de erro no protocolo com sucesso sem criar recorrencia quando ocorrer erro de negocio")
        void dadoErroNegocio_quandoConsumirMensagem_deveDispararEventosDeErroSemCriarRecorrencia() {
            var idRecorrencia = UUID.randomUUID().toString();
            var idParcela = UUID.randomUUID().toString();
            var dataAgendado = LocalDateTime.now().plusDays(10);
            var dadosPagador = getPagador(CPF_PAGADOR);
            var dadosRecebedor = RecorrenteRecebedorDTO.builder()
                    .cpfCnpj(dadosPagador.getCpfCnpj())
                    .nome(dadosPagador.getNome())
                    .agencia(dadosPagador.getAgencia())
                    .conta(dadosPagador.getConta())
                    .instituicao(dadosPagador.getInstituicao())
                    .tipoConta(dadosPagador.getTipoConta())
                    .tipoPessoa(dadosPagador.getTipoPessoa())
                    .tipoChave(TipoChaveEnum.CPF)
                    .chave(CPF_RECEBEDOR)
                    .build();
            var cadastroRecorrenciaRequestDTO = getPayloadListCadastroRecorrenciaTransacaoDTO(idRecorrencia, idParcela, dadosPagador, dadosRecebedor);
            var protocoloDTO = getProtocoloDTO(idRecorrencia, CODIGO_CADASTRO, cadastroRecorrenciaRequestDTO, dataAgendado);

            enviarMensagem(protocoloDTO, TOPICO_AGENDADO_RECORRENTE);

            validarFalhaCadastro(idRecorrencia, COOPERATIVA, CONTA, idRecorrencia , 3);
        }

        @Test
        @DisplayName("Deve enviar eventos de erro no protocolo com sucesso sem criar recorrencia quando ocorrer erro de validação")
        void dadoErroConstraint_quandoConsumirMensagem_deveDispararEventosDeErroSemCriarRecorrencia() {
            var idRecorrencia = UUID.randomUUID().toString();
            var idParcela = UUID.randomUUID().toString();
            var pagadorDTO = getPagador(RandomStringUtils.randomNumeric(11));
            pagadorDTO.setInstituicao("9999999999");
            var recebedorDTO = getRecebedor(RandomStringUtils.randomNumeric(11));
            var cadastroRecorrenciaRequestDTO = getPayloadListCadastroRecorrenciaTransacaoDTO(idRecorrencia, idParcela, pagadorDTO, recebedorDTO);
            ProtocoloDTO protocoloDTO = getProtocoloDTO(idRecorrencia, CODIGO_CADASTRO_INTEGRADO, cadastroRecorrenciaRequestDTO, null);

            enviarMensagem(protocoloDTO, TOPICO_AGENDADO_RECORRENTE);

            validarFalhaCadastro(idParcela, COOPERATIVA, CONTA, idRecorrencia , 3);
        }

        private String getPayloadListCadastroRecorrenciaTransacaoDTO(String idRecorrencia, String idParcela, RecorrentePagadorDTO pagador, RecorrenteRecebedorDTO recebedor) {
            var pagadorRequestDTO = PagadorRequestDTO.builder()
                    .cpfCnpj(pagador.getCpfCnpj())
                    .nome(pagador.getNome())
                    .instituicao(pagador.getInstituicao())
                    .agencia(pagador.getAgencia())
                    .conta(pagador.getConta())
                    .posto(pagador.getPosto())
                    .tipoConta(pagador.getTipoConta())
                    .tipoPessoa(pagador.getTipoPessoa())
                    .build();

            var recebedorRequestDTO = RecebedorRequestDTO.builder()
                    .cpfCnpj(recebedor.getCpfCnpj())
                    .nome(recebedor.getNome())
                    .agencia(recebedor.getAgencia())
                    .conta(recebedor.getConta())
                    .instituicao(recebedor.getInstituicao())
                    .tipoConta(recebedor.getTipoConta())
                    .tipoPessoa(recebedor.getTipoPessoa())
                    .tipoChave(recebedor.getTipoChave())
                    .chave(recebedor.getChave())
                    .build();

            var cadastroRecorrenciaRequestDTO1 = CadastroRecorrenciaTransacaoDTO.builder()
                    .pagador(pagadorRequestDTO)
                    .recebedor(recebedorRequestDTO)
                    .nome("Nome")
                    .tipoCanal(TipoCanalEnum.MOBI)
                    .tipoMarca(TipoMarcaEnum.SICREDI)
                    .tipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_BY_KEY)
                    .tipoIniciacaoCanal(TipoIniciacaoCanal.CHAVE)
                    .tipoFrequencia(TipoFrequencia.MENSAL)
                    .numInicCnpj("12345678901234")
                    .identificadorRecorrencia(idRecorrencia)
                    .idFimAFim("E0118152120250206174249JMdlji000")
                    .informacoesEntreUsuarios(INFO_USUARIOS)
                    .idConciliacaoRecebedor(ID_CONCILIACAO)
                    .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                    .build();
            cadastroRecorrenciaRequestDTO1.setOrigemConta(OrigemEnum.LEGADO);
            cadastroRecorrenciaRequestDTO1.setIdentificadorSimulacaoLimite(UUID.randomUUID().toString());
            cadastroRecorrenciaRequestDTO1.setDataTransacao(LocalDateTime.now().plusDays(1).plusMonths(1));
            cadastroRecorrenciaRequestDTO1.setValor(BigDecimal.TEN);
            cadastroRecorrenciaRequestDTO1.setCodigoIdentificadorTransacao(UUID.randomUUID().toString());
            cadastroRecorrenciaRequestDTO1.setIdentificadorParcela(idParcela);

            var cadastroRecorrenciaRequestDTO2 = CadastroRecorrenciaTransacaoDTO.builder()
                    .pagador(cadastroRecorrenciaRequestDTO1.getPagador())
                    .recebedor(cadastroRecorrenciaRequestDTO1.getRecebedor())
                    .nome(cadastroRecorrenciaRequestDTO1.getNome())
                    .tipoCanal(cadastroRecorrenciaRequestDTO1.getTipoCanal())
                    .tipoMarca(cadastroRecorrenciaRequestDTO1.getTipoMarca())
                    .tipoIniciacao(cadastroRecorrenciaRequestDTO1.getTipoIniciacao())
                    .tipoIniciacaoCanal(cadastroRecorrenciaRequestDTO1.getTipoIniciacaoCanal())
                    .tipoFrequencia(cadastroRecorrenciaRequestDTO1.getTipoFrequencia())
                    .numInicCnpj(cadastroRecorrenciaRequestDTO1.getNumInicCnpj())
                    .identificadorRecorrencia(cadastroRecorrenciaRequestDTO1.getIdentificadorRecorrencia())
                    .tipoRecorrencia(cadastroRecorrenciaRequestDTO1.getTipoRecorrencia())
                    .idFimAFim("E0118152120250206174249JMdlji001")
                    .informacoesEntreUsuarios(INFO_USUARIOS)
                    .idConciliacaoRecebedor(ID_CONCILIACAO)
                    .build();
            cadastroRecorrenciaRequestDTO2.setIdentificadorSimulacaoLimite(UUID.randomUUID().toString());
            cadastroRecorrenciaRequestDTO2.setDataTransacao(LocalDateTime.now().plusDays(1).plusMonths(2));
            cadastroRecorrenciaRequestDTO2.setValor(BigDecimal.TEN);
            cadastroRecorrenciaRequestDTO2.setCodigoIdentificadorTransacao(UUID.randomUUID().toString());
            cadastroRecorrenciaRequestDTO2.setIdentificadorParcela(UUID.randomUUID().toString());

            return ObjectMapperUtil.converterObjetoParaString(List.of(cadastroRecorrenciaRequestDTO1, cadastroRecorrenciaRequestDTO2));
        }

    }

    @Nested
    class ProcessarDTL {

        @Test
        @DisplayName("Deve criar o registro do agendamento e enviar eventos de protocolo com sucesso quando consumir mensagem de agendado sem recorrencia")
        void dadoPayloadInvalido_quandoConsumirMensagem_deveEnviarParaDLT() {
            var dataAgendado = LocalDateTime.now().plusDays(10);
            var idFimAFimAgendado = SpiUtil.gerarIdFimAFim(TipoId.PAGAMENTO, ISPB, dataAgendado);
            var dadosRecebedor = getRecebedor(CPF_RECEBEDOR);
            var cadastroRecorrenciaRequestDTO = getPayloadListCadastroOrdemPagamentoTransacaoDTO(idFimAFimAgendado, dataAgendado, dadosRecebedor, false);
            var protocoloDTO = getProtocoloDTO(idFimAFimAgendado, CODIGO_CADASTRO, cadastroRecorrenciaRequestDTO, dataAgendado);

            enviarMensagem(protocoloDTO, TOPICO_AGENDADO_RECORRENTE);

            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 2);
            assertEquals(2, response.count());
        }
    }

    private void validarFalhaCadastro(String idParcela, String agencia, String conta, String transactionId, int qtdEventos) {
        await().timeout(Duration.ofSeconds(10)).untilAsserted(() -> {
            var optRecorrencia = recorrenciaRepository.findRecorrenciaByIdParcelaAndAgenciaAndConta(idParcela, agencia, conta);
            assertTrue(optRecorrencia.isEmpty());

            var idempotenteIn = buscarRegistrosIdempotenteIn(transactionId);
            var idempotenteOut = buscarRegistrosIdempotenteOut(transactionId);
            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(qtdEventos, idempotenteOut.size())
            );

            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), qtdEventos);
            assertEquals(qtdEventos, response.count());
        });
    }

    private void validarSucessoCadastro(String idParcela, String agencia, String conta, String transactionId, int qtdParcelas, int qtdEventos) {
        await().timeout(Duration.ofSeconds(30)).untilAsserted(() -> {
            var optRecorrencia = recorrenciaRepository.findRecorrenciaByIdParcelaAndAgenciaAndConta(idParcela, agencia, conta);
            optRecorrencia.ifPresentOrElse(recorrencia -> assertAll(
                            () -> assertNotNull(recorrencia.getPagador()),
                            () -> assertNotNull(recorrencia.getRecebedor()),
                            () -> assertNotNull(recorrencia.getRecorrencias()),
                            () -> assertEquals(qtdParcelas, recorrencia.getRecorrencias().size())),
                    () -> fail(MSG_RECORRENCIA_NAO_LOCALIZADA)
            );

            var idempotenteIn = buscarRegistrosIdempotenteIn(transactionId);
            var idempotenteOut = buscarRegistrosIdempotenteOut(transactionId);
            assertAll(
                    () -> assertEquals(1, idempotenteIn.size()),
                    () -> assertEquals(qtdEventos, idempotenteOut.size())
            );

            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), qtdEventos);
            assertEquals(qtdEventos, response.count());
        });
    }

    private String getPayloadListCadastroOrdemPagamentoTransacaoDTO(String idFimAFimAgendado, LocalDateTime dataAgendado, RecorrenteRecebedorDTO dadosRecebedor, boolean recorrenciaIntegrada) {
        var dadosPagador = getPagador(CPF_PAGADOR);

        var participantePagador = new ParticipanteRequestDTO();
        participantePagador.setIspb(dadosPagador.getInstituicao());
        participantePagador.setNome("Nome Instituicao Pagador");
        var participanteRecebedor = new ParticipanteRequestDTO();
        participanteRecebedor.setIspb(dadosRecebedor.getInstituicao());
        participanteRecebedor.setNome("Nome Instituicao Recebedor");

        var cadastroOrdemPagamentoTransacaoDTO = new CadastroOrdemPagamentoTransacaoDTO();
        cadastroOrdemPagamentoTransacaoDTO.setChaveDict(dadosRecebedor.getChave());
        cadastroOrdemPagamentoTransacaoDTO.setCpfCnpjUsuarioPagador(dadosPagador.getCpfCnpj());
        cadastroOrdemPagamentoTransacaoDTO.setNomeUsuarioPagador(dadosPagador.getNome());
        cadastroOrdemPagamentoTransacaoDTO.setTipoContaUsuarioPagador(dadosPagador.getTipoConta());
        cadastroOrdemPagamentoTransacaoDTO.setParticipantePagador(participantePagador);
        cadastroOrdemPagamentoTransacaoDTO.setAgenciaUsuarioRecebedor(dadosRecebedor.getAgencia());
        cadastroOrdemPagamentoTransacaoDTO.setContaUsuarioRecebedor(dadosRecebedor.getConta());
        cadastroOrdemPagamentoTransacaoDTO.setCpfCnpjUsuarioRecebedor(dadosRecebedor.getCpfCnpj());
        cadastroOrdemPagamentoTransacaoDTO.setNomeUsuarioRecebedor(dadosRecebedor.getNome());
        cadastroOrdemPagamentoTransacaoDTO.setParticipanteRecebedor(participanteRecebedor);
        cadastroOrdemPagamentoTransacaoDTO.setTipoContaUsuarioRecebedor(dadosRecebedor.getTipoConta());
        cadastroOrdemPagamentoTransacaoDTO.setInformacoesEntreUsuarios("Informacoes Entre Usuarios");
        cadastroOrdemPagamentoTransacaoDTO.setCpfCnpjUsuarioDevedor(dadosPagador.getCpfCnpj());
        cadastroOrdemPagamentoTransacaoDTO.setNomeUsuarioDevedor(dadosPagador.getNome());
        cadastroOrdemPagamentoTransacaoDTO.setTipoPagamento(TipoPagamentoEnum.PIX_COM_CHAVE);
        cadastroOrdemPagamentoTransacaoDTO.setMarca(MarcaEnum.SICREDI);
        cadastroOrdemPagamentoTransacaoDTO.setOrigemConta(OrigemEnum.LEGADO);
        cadastroOrdemPagamentoTransacaoDTO.setFinalidadeTransacao(FinalidadeTransacaoEnum.PAGAMENTO);
        cadastroOrdemPagamentoTransacaoDTO.setSolicitarCadastroFavorecido(Boolean.TRUE);
        cadastroOrdemPagamentoTransacaoDTO.setCpfUsuario(dadosPagador.getCpfCnpj());
        cadastroOrdemPagamentoTransacaoDTO.setTipoProduto("PIX_RECORRENTE");
        cadastroOrdemPagamentoTransacaoDTO.setTipoTransacaoPagamento(TipoTransacaoPagamento.AGENDADO);

        cadastroOrdemPagamentoTransacaoDTO.setOrigemConta(OrigemEnum.LEGADO);
        cadastroOrdemPagamentoTransacaoDTO.setCanal("MOBI");
        cadastroOrdemPagamentoTransacaoDTO.setDataTransacao(dataAgendado);
        cadastroOrdemPagamentoTransacaoDTO.setCodigoIdentificadorTransacao(idFimAFimAgendado);
        cadastroOrdemPagamentoTransacaoDTO.setValor(BigDecimal.TEN);
        cadastroOrdemPagamentoTransacaoDTO.setCodigoTransacao(CODIGO_CADASTRO);
        cadastroOrdemPagamentoTransacaoDTO.setCooperativa(dadosPagador.getAgencia());
        cadastroOrdemPagamentoTransacaoDTO.setAgencia(dadosPagador.getPosto());
        cadastroOrdemPagamentoTransacaoDTO.setConta(dadosPagador.getConta());
        cadastroOrdemPagamentoTransacaoDTO.setTipoPessoaConta(br.com.sicredi.canaisdigitais.enums.TipoPessoaEnum.valueOf(dadosPagador.getTipoPessoa().name()));
        cadastroOrdemPagamentoTransacaoDTO.setNomeSolicitante(dadosPagador.getNome());
        cadastroOrdemPagamentoTransacaoDTO.setIdentificadorSimulacaoLimite(UUID.randomUUID().toString());
        cadastroOrdemPagamentoTransacaoDTO.setCpfCnpjConta(dadosPagador.getCpfCnpj());
        cadastroOrdemPagamentoTransacaoDTO.setCpfCnpjAssociado(dadosPagador.getCpfCnpj());

        if(recorrenciaIntegrada){
            cadastroOrdemPagamentoTransacaoDTO.setTipoTransacaoPagamento(TipoTransacaoPagamento.AGENDADO_COM_RECORRENCIA);
            var parcela1 = RecorrenciaParcelaRequest.builder()
                    .valor(BigDecimal.TEN)
                    .dataTransacao(dataAgendado.plusMonths(1L))
                    .idFimAFim(null)
                    .informacoesEntreUsuarios(INFO_USUARIOS_PARCELA)
                    .idConciliacaoRecebedor(RandomStringUtils.randomAlphanumeric(10))
                    .identificadorParcela(idFimAFimAgendado)
                    .build();
            var parcela2 = RecorrenciaParcelaRequest.builder()
                    .valor(BigDecimal.TEN)
                    .dataTransacao(dataAgendado.plusMonths(2L))
                    .idFimAFim(null)
                    .informacoesEntreUsuarios(INFO_USUARIOS_PARCELA)
                    .idConciliacaoRecebedor(RandomStringUtils.randomAlphanumeric(10))
                    .identificadorParcela(UUID.randomUUID().toString())
                    .build();
            var recorrencia = new RecorrenciaIntegradaRequest();
            recorrencia.setTipoFrequencia(TipoFrequencia.MENSAL);
            recorrencia.setNomeRecorrencia("Nome Recorrencia");
            recorrencia.setTipoIniciacaoCanal(TipoIniciacaoCanal.CHAVE);
            recorrencia.setTipoRecorrencia(TipoRecorrencia.AGENDADO);
            recorrencia.setTipoChaveRecebedor(TipoChaveEnum.CPF);
            recorrencia.setTipoPessoaRecebedor(TipoPessoaEnum.PF);
            recorrencia.setIdentificadorRecorrencia(UUID.randomUUID().toString());
            recorrencia.setParcelas(List.of(parcela1, parcela2));
            cadastroOrdemPagamentoTransacaoDTO.setRecorrencia(recorrencia);
        }


        return ObjectMapperUtil.converterObjetoParaString(cadastroOrdemPagamentoTransacaoDTO);
    }

    private RecorrentePagadorDTO getPagador(String cpfCnpj) {
        return RecorrentePagadorDTO.builder()
                .cpfCnpj(cpfCnpj)
                .nome("Nome Pagador")
                .instituicao(ISPB)
                .agencia(COOPERATIVA)
                .conta(CONTA)
                .posto("12")
                .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                .tipoPessoa(TipoPessoaEnum.PF)
                .build();
    }

    private RecorrenteRecebedorDTO getRecebedor(String cpfCnpj) {
        return RecorrenteRecebedorDTO.builder()
                .cpfCnpj(cpfCnpj)
                .nome("Nome Recebedor")
                .agencia(COOPERATIVA)
                .conta("012345")
                .instituicao("11111111")
                .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                .tipoPessoa(TipoPessoaEnum.PF)
                .tipoChave(TipoChaveEnum.CPF)
                .chave(CPF_RECEBEDOR)
                .build();
    }

    private ProtocoloDTO getProtocoloDTO(String idRecorrencia, String codigoTransacao, String recorrenciaRequestDTO, LocalDateTime dataAgendamento) {
        var protocoloDTO = new ProtocoloDTO();
        protocoloDTO.setIdProtocolo(1L);
        protocoloDTO.setIdentificadorTransacao(idRecorrencia);
        protocoloDTO.setIdentificadorSimulacaoLimite(UUID.randomUUID().toString());
        protocoloDTO.setPayloadTransacao(recorrenciaRequestDTO);
        protocoloDTO.setDataRequisicao(LocalDateTime.now());
        protocoloDTO.setDataAgendamento(dataAgendamento);
        protocoloDTO.setCodigoTipoTransacao(codigoTransacao);
        protocoloDTO.setCooperativa(COOPERATIVA);
        protocoloDTO.setConta(CONTA);
        protocoloDTO.setCodigoCanal(1);
        protocoloDTO.setIdentificadorUsuario(RandomStringUtils.randomNumeric(6));
        return protocoloDTO;
    }

    private void enviarMensagem(Object payload, String topico) {
        var payloadString = ObjectMapperUtil.converterObjetoParaString(payload);
        var mensagem = MessageBuilder
                .withPayload(payloadString)
                .setHeader(KafkaHeaders.TOPIC, topico)
                .build();
        kafkaTemplate.send(mensagem);
        kafkaTemplate.flush();
    }

    private Consumer<String, String> configurarConsumer() {
        var listaTopico = List.of("canaisdigitais-protocolo-comando-v1", "spi-notificacao-recorrencia-v2");

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), Boolean.TRUE.toString(), embeddedKafkaBroker);
        var consumerTest = new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumerTest.subscribe(listaTopico);
        consumerTest.poll(Duration.ofSeconds(2));
        return consumerTest;
    }

}
