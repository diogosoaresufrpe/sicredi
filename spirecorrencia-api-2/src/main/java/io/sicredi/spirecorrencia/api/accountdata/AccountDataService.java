package io.sicredi.spirecorrencia.api.accountdata;

import br.com.sicredi.framework.exception.TechnicalException;
import feign.RetryableException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class AccountDataService {

    private final AccountDataClient accountDataClient;
    public static final String DIGITAL = "DIGITAL";
    public static final String ALL = "ALL";
    public static final String ORIGINATING = "ORIGINATING";
    public static final String LEGACY = "LEGACY";
    public static final String ACTIVE = "ACTIVE";

    /**
     * Consultar e selecionar uma conta com seguinte ordem de precedência:
     * 1 - Conta Digital Ativa;
     * 2 - Conta Legado Ativa;
     * 3 - Conta Digital Não Ativa;
     * 4 - Conta Legado Não Ativa;
     * Contas Digital com numero de conta de mais de 6 digitos possuem dois registros no account-data (DIGITAL e LEGACY).
     *
     * @param cpfCnpj
     * @param coop
     * @param conta
     * @return ConsultaDadosResponse
     */
    public DadosContaResponseDTO consultarConta(String cpfCnpj, String coop, String conta) {
       try {
           log.debug("(Account-Data) Realizando consulta de conta. Cooperativa: {}, Conta:{}", coop, conta);
           var contas = accountDataClient.consultarConta(cpfCnpj, coop, conta, ALL);

           List<DadosContaResponseDTO> contasFiltradas = contas.stream()
                   .filter(this::isNotContaEmAndamento)
                   .filter(c -> this.isContaExistente(cpfCnpj, coop, Long.valueOf(conta), c))
                   .toList();

           return selecionarContaComOrdemPrecedencia(contasFiltradas);
       } catch (RetryableException ex) {
           log.error("(Account-Data) Ocorreu um erro ao realizar a consulta dos dados da conta. Cooperativa: {}, Conta:{}, Motivo: Timeout | Detalhes: {} ", coop, conta, ex.getMessage());
           throw new TechnicalException(ex);
       }
    }

    private DadosContaResponseDTO selecionarContaComOrdemPrecedencia(List<DadosContaResponseDTO> contas) {
        Map<String, List<DadosContaResponseDTO>> contasPorOrigem = contas.stream()
                .collect(Collectors.groupingBy(this::criarMapKeyPorOrigem));

        List<DadosContaResponseDTO> digitais = contasPorOrigem.getOrDefault(DIGITAL, List.of());
        List<DadosContaResponseDTO> legados = contasPorOrigem.getOrDefault(LEGACY, List.of());

        return obterPrimeiraContaAtiva(digitais)
                .or(() -> obterPrimeiraContaAtiva(legados))
                .or(() -> digitais.stream().findFirst())
                .or(() -> contas.stream().findFirst()).orElse(null);
    }

    private Optional<DadosContaResponseDTO> obterPrimeiraContaAtiva(Collection<DadosContaResponseDTO> contas) {
        return contas.stream().filter(c -> ACTIVE.equals(c.status())).findFirst();
    }

    private String criarMapKeyPorOrigem(DadosContaResponseDTO contaCarregada) {
        return contaCarregada.sistema();
    }

    public boolean isNotContaEmAndamento(DadosContaResponseDTO c) {
        return !ORIGINATING.equals(c.status());
    }

    public boolean isContaExistente(String cpfCnpj, String agencia, Long conta, DadosContaResponseDTO consultaDadosResponse) {
        boolean retorno = false;

        if (Objects.nonNull(consultaDadosResponse)) {
            Long contaNumericaCarregada = Long.valueOf(consultaDadosResponse.numeroConta());

            // Caso a conta seja migrada, remove o prefixo de 3 digitos, caso o pagador tenha informado o numero de conta antigo
            retorno = (consultaDadosResponse.coop().equals(agencia) && isContaOriginalOuMigrada(conta, consultaDadosResponse, contaNumericaCarregada))
                      || isOrdemCpfCnpjCorrepondeDocumentoAssociadoConta(cpfCnpj, consultaDadosResponse);
        }

        return retorno;
    }

    private boolean isOrdemCpfCnpjCorrepondeDocumentoAssociadoConta(String cpfCnpj, DadosContaResponseDTO consultaDadosResponse) {
        return consultaDadosResponse.pessoas().stream()
                .anyMatch(personResponse -> personResponse.documento().equals(cpfCnpj));
    }

    private boolean isContaOriginalOuMigrada(Long conta, DadosContaResponseDTO consultaDadosResponse, Long contaNumericaCarregada) {
        return contaNumericaCarregada.equals(conta) || (consultaDadosResponse.migrado() && Long.valueOf(consultaDadosResponse.numeroConta().substring(3)).equals(conta));
    }

}