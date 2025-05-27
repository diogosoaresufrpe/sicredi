package io.sicredi.spirecorrencia.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalTime;

@Data
@Primary
@RefreshScope
@Configuration
@ConfigurationProperties(prefix = "config")
public class AppConfig {
    private Kafka kafka;
    private Regras regras;
    private HoldersMaintenance holdersMaintenance;
    private JobShedLock jobNotificacaoDiaAnterior;
    private JobShedLock jobProcessamentoLiquidacao;
    private JobShedLock jobNotificaoExpiracaoPixAutomatico;
    private JobShedLock jobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico;

    private JobShedLock jobConfirmacaoCancelamentoAposExpiracaoPixAutomatico;

    private ConfigShedlock configShedlock;
    private Automatico automatico = new Automatico();

    @Data
    public static class HoldersMaintenance {
        private int tamanhoDaConsulta;
    }

    @Data
    public static class JobShedLock {
        private String nomeJob;
        private int tamanhoDaConsulta;
        private boolean jobHabilitado;
        private String cronExpression;
        private String lockAtMostFor;
        private String lockAtLeastFor;
    }

    @Data
    public static class ConfigShedlock {
        private String timezone;
    }

    @Data
    public static class Regras {
        private LocalTime exclusaoHorarioLimite;
        private Parcela parcela;
        private Parcela parcelaOpenFinance;
        private Horario horario;
        private Horario horarioOpenFinance;
        private Processamento processamento;
        private CancelamentoAgendamento cancelamentoAgendamento;

        @Data
        public static class Parcela {
            private Long numeroMinimoParcelas;
            private Long numeroMaximoParcelas;
        }

        @Data
        public static class Horario {
            private LocalTime inicio;
            private LocalTime fim;
            private Long diaMinimoCadastroEntreInicioFim;
            private Long diaMinimoCadastroForaInicioFim;
        }
    }

    @Data
    public static class CancelamentoAgendamento {
        private LocalTime horarioLimiteCancelamento;
        private Integer diasMinimosAntecedencia;
    }

    @Data
    public static class Processamento {
        private LocalTime horarioLimiteLiquidacao;
        private Integer minutosExpiracao;
        private Long limiteExpiracaoHoras;
        private boolean reenvioOperacaoHabilitado;
    }

    @Data
    public static class Kafka {
        private Consumer consumer;
        private Producer producer;

        @Data
        public static class Consumer {
            private ConsumerProps retornoTransacao;
            private ConsumerProps cadastroRecorrencia;
            private ConsumerProps cadastroAgendado;
            private ConsumerProps exclusaoRecorrencia;
            private ConsumerProps cadastroAutorizacao;
            private ConsumerProps cancelamentoDebito;
            private ConsumerProps retentativaTratamentoErroLiquidacao;
            private ConsumerProps tratamentoErroLiquidacaoOrdemPagamento;
            private ConsumerProps holdersMaintenance;
            private ConsumerProps icomPainRecebido;
            private ConsumerProps icomPainEnviado;
            private ConsumerProps icomPainEnviadoFalha;
            private ConsumerProps confirmacaoSolicitacaoAutorizacao;
            private ConsumerProps icomCamtRecebido;
            private ConsumerProps autorizacaoCancelamento;

            @Data
            public static class ConsumerProps {
                private String groupId;
                private String nome;
                private Long concurrency = 1L;
                private Retentativa retry;
            }

            @Data
            public static class Retentativa {
                private Long tentativas = 1L;
                private Long delay = 0L;
                private Long timeout = 60000L;
            }

        }

        @Data
        public static class Producer {
            private ProducerProps comandoProtocolo;
            private ProducerProps notificacaoRecorrencia;
            private ProducerProps exclusaoRecorrencia;
            private ProducerProps icomPainEnvio;
            private ProducerProps icomCamtEnvio;

            @Data
            public static class ProducerProps {
                private String topico;
            }
        }
    }

    @Data
    public static class Automatico {
        private InstrucaoPagamento instrucaoPagamento = new InstrucaoPagamento();

        @Data
        public static class InstrucaoPagamento{
            private LocalTime horarioLimiteProcessamento = LocalTime.of(21, 0);
        }
    }
}