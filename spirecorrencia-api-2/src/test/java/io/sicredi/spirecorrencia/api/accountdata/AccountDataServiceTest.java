package io.sicredi.spirecorrencia.api.accountdata;

import br.com.sicredi.framework.exception.TechnicalException;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static io.sicredi.spirecorrencia.api.accountdata.AccountDataService.*;
import static io.sicredi.spirecorrencia.api.accountdata.TipoContaEnum.CHECKING_ACCOUNT;
import static io.sicredi.spirecorrencia.api.accountdata.TipoContaEnum.SAVINGS_ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDataServiceTest {

    private static final String AGENCIA = "0101";
    private static final String CONTA = "123456";
    private static final String CPF_CNPJ = "123123123";
    private static final String CANCELED = "CANCELED";
    private static final String CAPITALIZING = "CAPITALIZING";
    private static final String CLOSED = "CLOSED";

    @Mock
    private AccountDataClient accountDataClient;

    @InjectMocks
    private AccountDataService accountDataService;

    @Test
    void dadoContaDigitalEmAndamento_quandoConsultarConta_deveDeveRetornarNull() {
        var dadosConta = List.of(getDadosConta(CPF_CNPJ, AGENCIA, CONTA, DIGITAL, CHECKING_ACCOUNT, "ORIGINATING"));

        when(accountDataClient.consultarConta(CPF_CNPJ, AGENCIA, CONTA, AccountDataService.ALL)).thenReturn(dadosConta);

        var contaSelecionada = accountDataService.consultarConta(CPF_CNPJ, AGENCIA, CONTA);

        assertNull(contaSelecionada);
    }

    @Test
    void dadoTimeOutAoConsultar_quandoConsultarConta_deveRetornarTechnicalException() {
        RetryableException timeout = mock(RetryableException.class);

        when(accountDataClient.consultarConta(CPF_CNPJ, AGENCIA, CONTA, AccountDataService.ALL)).thenThrow(timeout);

        assertThatThrownBy(() -> accountDataService.consultarConta(CPF_CNPJ, AGENCIA, CONTA))
                .isInstanceOf(TechnicalException.class)
                .hasCause(timeout);
    }

    @Nested
    class OrdemPrecedenciaTest {

        private DadosContaResponseDTO contaPoupancaDigitalAtiva;
        private DadosContaResponseDTO contaCorrenteDigitalAtiva;
        private DadosContaResponseDTO contaPoupancaLegadoAtiva;
        private DadosContaResponseDTO contaCorrenteLegadoAtiva;
        private DadosContaResponseDTO contaPoupancaLegadoCancelada;
        private DadosContaResponseDTO contaCorrenteLegadoCancelada;
        private DadosContaResponseDTO contaPoupancaDigitalCancelada;
        private DadosContaResponseDTO contaCorrenteDigitalInativa;
        private DadosContaResponseDTO contaPoupancaDigitalEncerrada;

        @BeforeEach
        void setUp() {
            contaPoupancaDigitalAtiva = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, DIGITAL, SAVINGS_ACCOUNT, ACTIVE);
            contaCorrenteDigitalAtiva = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, DIGITAL, CHECKING_ACCOUNT, ACTIVE);
            contaPoupancaLegadoAtiva = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, LEGACY, SAVINGS_ACCOUNT, ACTIVE);
            contaCorrenteLegadoAtiva = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, LEGACY, CHECKING_ACCOUNT, ACTIVE);
            contaPoupancaLegadoCancelada = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, LEGACY, SAVINGS_ACCOUNT, CANCELED);
            contaCorrenteLegadoCancelada = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, LEGACY, CHECKING_ACCOUNT, CANCELED);
            contaPoupancaDigitalCancelada = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, DIGITAL, SAVINGS_ACCOUNT, CANCELED);
            contaCorrenteDigitalInativa = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, DIGITAL, CHECKING_ACCOUNT, CAPITALIZING);
            contaPoupancaDigitalEncerrada = getDadosConta(CPF_CNPJ, AGENCIA, CONTA, DIGITAL, SAVINGS_ACCOUNT, CLOSED);

        }

        @Test
        void dadoListaContasComContaDigitalAtiva_quandoConsultarConta_deveDeveRetornarContaDigital() {
            List<DadosContaResponseDTO> contasCarregadas = List.of(contaPoupancaDigitalAtiva, contaCorrenteDigitalAtiva, contaPoupancaLegadoAtiva, contaCorrenteLegadoAtiva, contaPoupancaLegadoCancelada, contaPoupancaDigitalCancelada);

            when(accountDataClient.consultarConta(CPF_CNPJ, AGENCIA, CONTA, AccountDataService.ALL)).thenReturn(contasCarregadas);

            var contaSelecionada = accountDataService.consultarConta(CPF_CNPJ, AGENCIA, CONTA);

            assertThat(contaSelecionada).usingRecursiveComparison().isEqualTo(contaPoupancaDigitalAtiva);
        }

        @Test
        void dadoListaContasComContaLegadoAtiva_quandoConsultarConta_deveDeveRetornarContaLegado() {
            List<DadosContaResponseDTO> contasCarregadas = List.of(contaCorrenteDigitalInativa, contaCorrenteLegadoAtiva, contaPoupancaLegadoAtiva);

            when(accountDataClient.consultarConta(CPF_CNPJ, AGENCIA, CONTA, AccountDataService.ALL)).thenReturn(contasCarregadas);

            var contaSelecionada = accountDataService.consultarConta(CPF_CNPJ, AGENCIA, CONTA);

            assertThat(contaSelecionada).usingRecursiveComparison().isEqualTo(contaCorrenteLegadoAtiva);
        }

        @Test
        void dadoListaContasComContaDigitalCancelada_quandoConsultarConta_deveDeveRetornarContaDigital() {
            List<DadosContaResponseDTO> contasCarregadas = List.of(contaPoupancaLegadoCancelada, contaPoupancaDigitalEncerrada, contaPoupancaDigitalCancelada);

            when(accountDataClient.consultarConta(CPF_CNPJ, AGENCIA, CONTA, AccountDataService.ALL)).thenReturn(contasCarregadas);

            var contaSelecionada = accountDataService.consultarConta(CPF_CNPJ, AGENCIA, CONTA);

            assertThat(contaSelecionada).usingRecursiveComparison().isEqualTo(contaPoupancaDigitalEncerrada);
        }

        @Test
        void dadoListaContasComContaDigitalEncerrada_quandoConsultarConta_deveDeveRetornarContaDigital() {
            List<DadosContaResponseDTO> contasCarregadas = List.of(contaPoupancaLegadoCancelada, contaPoupancaDigitalEncerrada);

            when(accountDataClient.consultarConta(CPF_CNPJ, AGENCIA, CONTA, AccountDataService.ALL)).thenReturn(contasCarregadas);

            var contaSelecionada = accountDataService.consultarConta(CPF_CNPJ, AGENCIA, CONTA);

            assertThat(contaSelecionada).usingRecursiveComparison().isEqualTo(contaPoupancaDigitalEncerrada);
        }

        @Test
        void dadoListaContasComContaLegadoCancelada_quandoConsultarConta_deveDeveRetornarContaLegadoNaoAtiva() {
            List<DadosContaResponseDTO> contasCarregadas = List.of(contaPoupancaLegadoCancelada, contaCorrenteLegadoCancelada);

            when(accountDataClient.consultarConta(CPF_CNPJ, AGENCIA, CONTA, AccountDataService.ALL)).thenReturn(contasCarregadas);

            var contaSelecionada = accountDataService.consultarConta(CPF_CNPJ, AGENCIA, CONTA);

            assertThat(contaSelecionada).usingRecursiveComparison().isEqualTo(contaPoupancaLegadoCancelada);
        }

    }

    public static DadosContaResponseDTO getDadosConta(String cpfCnpj, String agencia, String conta, String sistema, TipoContaEnum tipoContaEnum, String status) {
        return DadosContaResponseDTO.builder()
                .coop(agencia)
                .numeroConta(conta)
                .sistema(sistema)
                .status(status)
                .tipoConta(tipoContaEnum)
                .titular(DadosPessoaContaDTO.builder()
                        .documento(cpfCnpj)
                        .build())
                .tipoProduto(TipoProdutoEnum.INDIVIDUAL_CHECKING_ACCOUNT)
                .build();
    }
}