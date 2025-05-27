package io.sicredi.spirecorrencia.api.shedlock;

import io.sicredi.spirecorrencia.api.config.AppConfig;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.util.TimeZone;


@Configuration
@EnableScheduling
@RequiredArgsConstructor
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
@ConditionalOnProperty(value = "app.scheduling.enable", havingValue = "true", matchIfMissing = true)
class ShedlockConfig {

    private final AppConfig propertiesLoader;

    @Bean
    LockProvider configLockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTimeZone(TimeZone.getTimeZone(propertiesLoader.getConfigShedlock().getTimezone()))
                        .withTableName("SPI_OWNER.SHEDLOCK")
                        .build()
        );
    }

}