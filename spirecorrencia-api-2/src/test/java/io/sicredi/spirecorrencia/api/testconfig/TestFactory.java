package io.sicredi.spirecorrencia.api.testconfig;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.entities.type.FinalidadeAgendamento;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import io.sicredi.spirecorrencia.api.accountdata.DadosContaResponseDTO;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCicloEnum;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaAutorizacaoCiclo;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.commons.DevedorResponse;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.liquidacao.TipoProcessamentoEnum;
import io.sicredi.spirecorrencia.api.liquidacao.TipoProcessamentoWrapperDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.sicredi.spirecorrencia.api.repositorio.Recebedor;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static br.com.sicredi.spi.entities.type.FinalidadeAgendamento.AGENDADO_NOVA_TENTATIVA;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.REC_PROC_BU0001;
import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.IdentificadorTransacaoTestFactory.gerarIdFimAFim;
import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.RecebedorTestFactory.criarRecebedor;
import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.IDENTIFICADOR_TRANSACAO;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestFactory {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RecorrenciaTestFactory {

        public static final long OID_RECORRENCIA = 1L;
        public static final String ID_RECORRENCIA = "d02aefe4-91be-404e-a5c6-7dff4e8b05cb";

        public static Recorrencia criarRecorrencia(LocalDateTime dataCriacao) {

            return Recorrencia.builder()
                    .oidRecorrencia(OID_RECORRENCIA)
                    .idRecorrencia(ID_RECORRENCIA)
                    .pagador(PagadorTestFactory.criarPagador())
                    .recebedor(criarRecebedor())
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.NOME)
                    .tipoCanal(TipoCanalEnum.MOBI)
                    .tipoOrigemSistema(OrigemEnum.LEGADO)
                    .tipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_BY_KEY)
                    .tipoStatus(TipoStatusEnum.CRIADO)
                    .dataCriacao(dataCriacao)
                    .tipoIniciacaoCanal(TipoIniciacaoCanal.CHAVE)
                    .tipoFrequencia(TipoFrequencia.MENSAL)
                    .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                    .build();
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class AutorizacaoTestFactory {

        public static final long OID_RECORRENCIA_AUTORIZACAO = 1L;
        public static final String ID_RECORRENCIA_AUTORIZACAO = "d02aefe4-91be-404e-a5c6-7dff4e8b05cb";
        public static final Pagador PAGADOR = PagadorTestFactory.criarPagador();
        public static final DevedorResponse DEVEDOR = DevedorTestFactory.criarDevedor();
        public static final Recebedor RECEBEDOR = RecebedorTestFactory.criarRecebedor();

        public static RecorrenciaAutorizacao criarAutorizacao() {

            return RecorrenciaAutorizacao.builder()
                    .oidRecorrenciaAutorizacao(OID_RECORRENCIA_AUTORIZACAO)
                    .idRecorrencia(ID_RECORRENCIA_AUTORIZACAO)
                    .tipoJornada("")
                    .tipoStatus(TipoStatusAutorizacao.APROVADA)
                    .tipoSubStatus(null)
                    .tipoFrequencia(TipoFrequencia.MENSAL.getTitulo())
                    .valor(new BigDecimal(100))
                    .pisoValorMaximo(new BigDecimal(90))
                    .valor(new BigDecimal(110))
                    .permiteLinhaCredito("S")
                    .permiteNotificacaoAgendamento("S")
                    .permiteRetentativa("S")
                    .dataInicialRecorrencia(LocalDate.now())
                    .dataFinalRecorrencia(LocalDate.now().plusDays(2))
                    .numeroContrato("0123456789")
                    .descricao("descricao")
                    .cpfCnpjPagador(PAGADOR.getCpfCnpj())
                    .agenciaPagador(PAGADOR.getAgencia())
                    .cpfCnpjDevedor(DEVEDOR.getCpfCnpj())
                    .cpfCnpjRecebedor(RECEBEDOR.getCpfCnpj())
                    .build();
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SolicitacaoTestFactory {

        public static final String ID_SOLICITACAO_AUTORIZACAO = "1L";
        public static final String ID_RECORRENCIA_AUTORIZACAO = "d02aefe4-91be-404e-a5c6-7dff4e8b05cb";
        public static final Pagador PAGADOR = PagadorTestFactory.criarPagador();
        public static final DevedorResponse DEVEDOR = DevedorTestFactory.criarDevedor();
        public static final Recebedor RECEBEDOR = RecebedorTestFactory.criarRecebedor();

        public static SolicitacaoAutorizacaoRecorrencia criarAutorizacao() {

            return SolicitacaoAutorizacaoRecorrencia.builder()
                    .idSolicitacaoRecorrencia(ID_SOLICITACAO_AUTORIZACAO)
                    .idRecorrencia(ID_RECORRENCIA_AUTORIZACAO)
                    .tipoStatus(TipoStatusSolicitacaoAutorizacao.ACEITA)
                    .tipoSubStatus(null)
                    .tipoFrequencia(TipoFrequencia.MENSAL.getTitulo())
                    .valor(new BigDecimal(100))
                    .pisoValorMaximo(new BigDecimal(90))
                    .dataInicialRecorrencia(LocalDate.now())
                    .dataFinalRecorrencia(LocalDate.now().plusMonths(1))
                    .numeroContrato("0123456789")
                    .descricao("descricao")
                    .cpfCnpjPagador(PAGADOR.getCpfCnpj())
                    .nomePagador(PAGADOR.getNome())
                    .cpfCnpjDevedor(DEVEDOR.getCpfCnpj())
                    .nomeDevedor(DEVEDOR.getNome())
                    .cpfCnpjRecebedor(RECEBEDOR.getCpfCnpj())
                    .nomeRecebedor(RECEBEDOR.getNome())
                    .build();
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class DevedorTestFactory {
        public static DevedorResponse criarDevedor() {
            return DevedorResponse.builder()
                    .cpfCnpj(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ)
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.NOME)
                    .build();
        }
    }


    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class PagadorTestFactory {
        public static Pagador criarPagador() {
            return Pagador.builder()
                    .cpfCnpj(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ)
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.NOME)
                    .instituicao(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.INSTITUICAO)
                    .agencia(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.AGENCIA)
                    .conta(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CONTA)
                    .codPosto(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.UA)
                    .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                    .tipoPessoa(TipoPessoaEnum.PF)
                    .build();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RecebedorTestFactory {
        public static Recebedor criarRecebedor() {
            return Recebedor.builder()
                    .cpfCnpj(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CPF_CNPJ)
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.NOME)
                    .agencia(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.AGENCIA)
                    .conta(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CONTA)
                    .instituicao(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.INSTITUICAO)
                    .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                    .tipoPessoa(TipoPessoaEnum.PJ)
                    .tipoChave(TipoChaveEnum.EMAIL)
                    .chave(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CHAVE_PIX)
                    .build();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RecorrenciaTransacaoTestFactory {

        public static RecorrenciaTransacao criarRecorrenciaTransacao(Recorrencia recorrencia,
                                                                     LocalDate dataParcela,
                                                                     BigDecimal valorParcela) {
            return RecorrenciaTransacao.builder()
                    .oidRecorrenciaTransacao(1L)
                    .recorrencia(recorrencia)
                    .tpoStatus(TipoStatusEnum.CRIADO)
                    .idParcela("17b1caba-a6ab-4d25-ad8a-2b30bc17218a")
                    .idFimAFim("E91586982202208151245099rD6AIAa7")
                    .valor(valorParcela)
                    .dataTransacao(dataParcela)
                    .build();
        }
    }

    @UtilityClass
    public static class TipoProcessamentoWrapperDTOTestFactory {

        public static TipoProcessamentoWrapperDTO liquidacao(RecorrenciaTransacao recorrenciaTransacao) {
            return TipoProcessamentoWrapperDTO.builder()
                    .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                    .tipoProcessamentoEnum(TipoProcessamentoEnum.LIQUIDACAO)
                    .recorrenciaTransacao(recorrenciaTransacao)
                    .build();
        }

        public static TipoProcessamentoWrapperDTO exclusaoTotal(RecorrenciaTransacao parcela) {
            var tipoProcessamentoErroDTO = TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                    .codigoErro(REC_PROC_BU0001.getCode())
                    .mensagemErro(REC_PROC_BU0001.getMessage())
                    .tipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA)
                    .build();
            return TipoProcessamentoWrapperDTO.builder()
                    .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                    .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_TOTAL)
                    .recorrenciaTransacao(parcela)
                    .tipoProcessamentoErro(tipoProcessamentoErroDTO)
                    .templateNotificacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE)
                    .build();
        }

        public static TipoProcessamentoWrapperDTO exclusaoParcial(RecorrenciaTransacao parcela) {
            var tipoProcessamentoErroDTO = TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                    .codigoErro(REC_PROC_BU0001.getCode())
                    .mensagemErro(REC_PROC_BU0001.getMessage())
                    .tipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA)
                    .build();
            return TipoProcessamentoWrapperDTO.builder()
                    .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                    .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_PARCIAL)
                    .recorrenciaTransacao(parcela)
                    .tipoProcessamentoErro(tipoProcessamentoErroDTO)
                    .templateNotificacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE)
                    .build();
        }

        public static TipoProcessamentoWrapperDTO ignorada(RecorrenciaTransacao parcela) {
            var tipoProcessamentoErroDTO = TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                    .codigoErro(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                    .mensagemErro("Erro ao consultar chave DICT")
                    .build();

            return TipoProcessamentoWrapperDTO.builder()
                    .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                    .tipoProcessamentoEnum(TipoProcessamentoEnum.IGNORADA)
                    .recorrenciaTransacao(parcela)
                    .tipoProcessamentoErro(tipoProcessamentoErroDTO)
                    .build();
        }
    }

    @UtilityClass
    public static class RecorrenciaInstrucaoPagamentoTestFactory {
        private static final String ID_RECORRENCIA = "67890";
        private static final String CPF_CNPJ_USUARIO_PAGADOR = "12345678901";
        private static final String PARTICIPANTE_USUARIO_PAGADOR = "001";
        private static final String NOME_DEVEDOR = "Devedor Teste";
        private static final String CPF_CNPJ_DEVEDOR = "98765432100";
        private static final BigDecimal VALOR = BigDecimal.valueOf(100.00);
        private static final String CPF_CNPJ_USUARIO_RECEBEDOR = "11223344556";
        private static final String AGENCIA_USUARIO_RECEBEDOR = "1234";
        private static final String CONTA_USUARIO_RECEBEDOR = "56789";
        private static final String AGENCIA_USUARIO_PAGADOR = "0101";
        private static final String CONTA_USUARIO_PAGADOR = "123456";
        private static final String PARTICIPANTE_USUARIO_RECEBEDOR = "002";
        private static final String TIPO_CONTA_USUARIO_RECEBEDOR = "CONTA_CORRENTE";
        private static final String ID_CONCILIACAO_RECEBEDOR = "99999";
        private static final String FINALIDADE_AGENDAMENTO = FinalidadeAgendamento.AGENDADO.name();
        private static final String INFORMACOES_ENTRE_USUARIOS = "Informações Teste";
        private static final LocalDate DATA_VENCIMENTO = LocalDate.now().plusDays(5);
        private static final String ID_FIM_A_FIM = gerarIdFimAFim(LocalDateTime.now().plusDays(3));
        public static final RecorrenciaAutorizacao AUTORIZACAO_SEM_CICLOS = autorizacao().ciclos(null).build();
        public static final RecorrenciaAutorizacao AUTORIZACAO_CICLOS_VAZIOS = autorizacao().ciclos(List.of()).build();
        public static final RecorrenciaInstrucaoPagamento INSTRUCAO_AGENDAMENTO_CANCELADA = instrucao().build();
        public static final RecorrenciaInstrucaoPagamento INSTRUCAO_NOVA_TENTATIVA_CANCELADA = instrucao().tpoFinalidadeAgendamento(AGENDADO_NOVA_TENTATIVA.name()).build();
        public static final List<RecorrenciaAutorizacaoCiclo> CICLO_COM_MAXIMO_DE_RETENTATIVAS = List.of(ciclo().instrucoesPagamento(List.of(INSTRUCAO_NOVA_TENTATIVA_CANCELADA, INSTRUCAO_NOVA_TENTATIVA_CANCELADA, INSTRUCAO_NOVA_TENTATIVA_CANCELADA, INSTRUCAO_AGENDAMENTO_CANCELADA)).build());
        public static final RecorrenciaInstrucaoPagamento INSTRUCAO_CONCLUIDA = instrucao().tpoStatus(TipoStatusInstrucaoPagamento.CONCLUIDA.name()).build();
        public static final RecorrenciaInstrucaoPagamento INSTRUCAO_ATIVA = instrucao().tpoStatus(TipoStatusInstrucaoPagamento.ATIVA.name()).build();
        public static final Pain013Dto PAIN013_AGENDAMENTO = pain013().build();
        public static final Pain013Dto PAIN013_REENVIO_DEVIDO_ERRO = pain013().finalidadeDoAgendamento(FinalidadeAgendamento.REENVIO_INST_PAG_DEVIDO_ERRO.name()).build();
        public static final Pain013Dto PAIN013_NOVA_TENTATIVA_APOS_VENC = pain013().finalidadeDoAgendamento(AGENDADO_NOVA_TENTATIVA.name()).build();

        public static AppConfig.Automatico automaticoConfig() {
            var automaticoConfig = new AppConfig.Automatico();
            var instrucaoConfig = new AppConfig.Automatico.InstrucaoPagamento();
            instrucaoConfig.setHorarioLimiteProcessamento(LocalTime.MAX);
            automaticoConfig.setInstrucaoPagamento(instrucaoConfig);
            return automaticoConfig;
        }

        public static Pain013Dto.Pain013DtoBuilder pain013() {
            return Pain013Dto.builder()
                    .idFimAFim(ID_FIM_A_FIM)
                    .idRecorrencia(ID_RECORRENCIA)
                    .cpfCnpjUsuarioPagador(CPF_CNPJ_USUARIO_PAGADOR)
                    .participanteDoUsuarioPagador(PARTICIPANTE_USUARIO_PAGADOR)
                    .nomeDevedor(NOME_DEVEDOR)
                    .cpfCnpjDevedor(CPF_CNPJ_DEVEDOR)
                    .valor(VALOR)
                    .cpfCnpjUsuarioRecebedor(CPF_CNPJ_USUARIO_RECEBEDOR)
                    .agenciaUsuarioRecebedor(AGENCIA_USUARIO_RECEBEDOR)
                    .contaUsuarioRecebedor(CONTA_USUARIO_RECEBEDOR)
                    .participanteDoUsuarioRecebedor(PARTICIPANTE_USUARIO_RECEBEDOR)
                    .tipoContaUsuarioRecebedor(TIPO_CONTA_USUARIO_RECEBEDOR)
                    .idConciliacaoRecebedor(ID_CONCILIACAO_RECEBEDOR)
                    .finalidadeDoAgendamento(FINALIDADE_AGENDAMENTO)
                    .informacoesEntreUsuarios(INFORMACOES_ENTRE_USUARIOS)
                    .dataHoraCriacaoParaEmissao(LocalDateTime.now())
                    .dataVencimento(DATA_VENCIMENTO);
        }

        public static RecorrenciaAutorizacao.RecorrenciaAutorizacaoBuilder autorizacao() {
            return RecorrenciaAutorizacao.builder()
                    .oidRecorrenciaAutorizacao(1L)
                    .ciclos(List.of(ciclo().build()))
                    .idRecorrencia(ID_RECORRENCIA)
                    .tipoStatus(TipoStatusAutorizacao.APROVADA)
                    .cpfCnpjPagador(CPF_CNPJ_USUARIO_PAGADOR)
                    .cpfCnpjDevedor(CPF_CNPJ_DEVEDOR)
                    .cpfCnpjRecebedor(CPF_CNPJ_USUARIO_RECEBEDOR)
                    .contaPagador(CONTA_USUARIO_PAGADOR)
                    .agenciaPagador(AGENCIA_USUARIO_PAGADOR)
                    .permiteRetentativa("S");
        }

        public static RecorrenciaAutorizacaoCiclo.RecorrenciaAutorizacaoCicloBuilder ciclo() {
            return RecorrenciaAutorizacaoCiclo.builder()
                    .tipoStatus(TipoStatusCicloEnum.ABERTO)
                    .dataInicial(LocalDate.now().withDayOfMonth(1))
                    .dataFinal(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).minusDays(1));
        }

        public static RecorrenciaInstrucaoPagamento.RecorrenciaInstrucaoPagamentoBuilder instrucao() {
            return RecorrenciaInstrucaoPagamento.builder()
                    .idRecorrencia(ID_RECORRENCIA)
                    .codFimAFim(ID_FIM_A_FIM)
                    .idConciliacaoRecebedor(ID_CONCILIACAO_RECEBEDOR)
                    .tpoFinalidadeAgendamento(FINALIDADE_AGENDAMENTO)
                    .tpoStatus(TipoStatusInstrucaoPagamento.CANCELADA.name())
                    .datVencimento(DATA_VENCIMENTO)
                    .numValor(VALOR);
        }

        public static DadosContaResponseDTO.DadosContaResponseDTOBuilder contaPagador() {
            return DadosContaResponseDTO.builder()
                    .status("ACTIVE")
                    .temCreditoBloqueado(false);
        }
    }

    @UtilityClass
    public class IdentificadorTransacaoTestFactory {
        private static final String ISPB_RECEBEDOR = "12345678";

        public static String gerarIdFimAFim(LocalDateTime dataAgendada) {
            var dataHora = dataAgendada.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));

            var sequencial = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 11);

            return "E" + ISPB_RECEBEDOR + dataHora + sequencial;
        }
    }

}
