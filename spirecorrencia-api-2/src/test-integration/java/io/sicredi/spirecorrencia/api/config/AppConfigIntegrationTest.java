package io.sicredi.spirecorrencia.api.config;

import io.sicredi.spirecorrencia.api.config.AppConfig.JobShedLock;
import io.sicredi.spirecorrencia.api.config.AppConfig.Regras;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.of;

@ActiveProfiles("test")
@SpringBootTest(classes = AppConfig.class)
@ImportAutoConfiguration(RefreshAutoConfiguration.class)
public class AppConfigIntegrationTest {

    private static final String MOST_FOR = "PT15M";
    private static final String LEAST_FOR = "PT5M";

    @Autowired
    private AppConfig appConfig;

    private static Stream<Arguments> fildsProvider() {
        return Arrays.stream(AppConfig.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("get"))
                .map(method -> of(method, method.getName()))
                .toList()
                .stream();
    }

    @Test
    public void validarRegras() {
        Regras regrasRecebidas = appConfig.getRegras();

        assertEquals(36, regrasRecebidas.getParcela().getNumeroMaximoParcelas());
        assertEquals(60, regrasRecebidas.getParcelaOpenFinance().getNumeroMaximoParcelas());
        assertEquals(LocalTime.of(0, 0, 0), regrasRecebidas.getHorario().getInicio());
        assertEquals(LocalTime.of(19, 59, 59), regrasRecebidas.getHorario().getFim());
        assertEquals(1, regrasRecebidas.getHorario().getDiaMinimoCadastroEntreInicioFim());
        assertEquals(2, regrasRecebidas.getHorario().getDiaMinimoCadastroForaInicioFim());
        assertEquals(LocalTime.of(0, 0, 0), regrasRecebidas.getHorarioOpenFinance().getInicio());
        assertEquals(LocalTime.of(23, 59, 59), regrasRecebidas.getHorarioOpenFinance().getFim());
        assertEquals(1, regrasRecebidas.getHorarioOpenFinance().getDiaMinimoCadastroEntreInicioFim());
        assertEquals(2, regrasRecebidas.getHorarioOpenFinance().getDiaMinimoCadastroForaInicioFim());
    }

    @Test
    public void validarKafkaProducer() {
        AppConfig.Kafka.Producer producer = appConfig.getKafka().getProducer();
        assertEquals("spi-notificacao-recorrencia-v2", producer.getNotificacaoRecorrencia().getTopico());
        assertEquals("canaisdigitais-protocolo-comando-v1", producer.getComandoProtocolo().getTopico());
    }

    @Test
    public void validarJobNotificacaoDiaAnterior() {
        JobShedLock jobNotificacaoDiaAnterior = appConfig.getJobNotificacaoDiaAnterior();

        assertEquals("AgendadoRecorrenteNotificacaoDiaAnteriorJob", jobNotificacaoDiaAnterior.getNomeJob());
        assertEquals(100, jobNotificacaoDiaAnterior.getTamanhoDaConsulta());
        assertTrue(jobNotificacaoDiaAnterior.isJobHabilitado());
        assertEquals("0 0 8 * * *", jobNotificacaoDiaAnterior.getCronExpression());
        assertEquals(MOST_FOR, jobNotificacaoDiaAnterior.getLockAtMostFor());
        assertEquals(LEAST_FOR, jobNotificacaoDiaAnterior.getLockAtLeastFor());
    }

    @Test
    public void validarAgendadoRecorrenteProcessamentoLiquidacaoJob() {
        JobShedLock jobProcessamentoLiquidacao = appConfig.getJobProcessamentoLiquidacao();

        assertEquals("AgendadoRecorrenteProcessamentoLiquidacaoJob", jobProcessamentoLiquidacao.getNomeJob());
        assertEquals(100, jobProcessamentoLiquidacao.getTamanhoDaConsulta());
        assertTrue(jobProcessamentoLiquidacao.isJobHabilitado());
        assertEquals("0 0 6,18 * * *", jobProcessamentoLiquidacao.getCronExpression());
        assertEquals(MOST_FOR, jobProcessamentoLiquidacao.getLockAtMostFor());
        assertEquals(LEAST_FOR, jobProcessamentoLiquidacao.getLockAtLeastFor());
    }

    @Test
    public void validarConfigShedlock() {
        AppConfig.ConfigShedlock configShedlock = appConfig.getConfigShedlock();

        assertEquals("America/Sao_Paulo", configShedlock.getTimezone());
    }

    @Test
    public void validarJobExpiracaoSolicitacaoPixAutomatico() {
        JobShedLock jobExpiracao = appConfig.getJobNotificaoExpiracaoPixAutomatico();

        assertEquals("AgendadoExpiracaoSolicitacaoPixAutomaticoJob", jobExpiracao.getNomeJob());
        assertEquals(100, jobExpiracao.getTamanhoDaConsulta());
        assertTrue(jobExpiracao.isJobHabilitado());
        assertEquals("0 1 1 * * *", jobExpiracao.getCronExpression());
        assertEquals(MOST_FOR, jobExpiracao.getLockAtMostFor());
        assertEquals(LEAST_FOR, jobExpiracao.getLockAtLeastFor());
    }

    @DisplayName("Validar que todos os campos estão presentes")
    @ParameterizedTest(name = "validando {1}")
    @MethodSource("fildsProvider")
    public void validarQueTodosOsCamposEstaoPresentes(Method method, String name) throws InvocationTargetException, IllegalAccessException {
        Object invoke = method.invoke(appConfig);

        assertNotNull(invoke);
        assertNotNull(name);
    }


}
