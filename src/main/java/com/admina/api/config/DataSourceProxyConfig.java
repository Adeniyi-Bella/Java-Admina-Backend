
package com.admina.api.config;

import lombok.extern.slf4j.Slf4j;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("dev")
@Slf4j
public class DataSourceProxyConfig {

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        DataSource original = properties.initializeDataSourceBuilder().build();
        return ProxyDataSourceBuilder
                .create(original)
                .name("dataSource")
                .afterQuery((execInfo, queryInfoList) -> {
                    queryInfoList.forEach(queryInfo -> {
                        long timeMs = execInfo.getElapsedTime();
                        boolean success = execInfo.isSuccess();
                        String connectionId = execInfo.getConnectionId();
                        String type = execInfo.getStatementType().name();
                        String query = queryInfo.getQuery()
                                .trim()
                                .replaceAll("\\s+", " ");
                        String shortened = query.substring(0, Math.min(query.length(), 80));

                        if (!success) {
                            log.error("DB_FAILURE conn={} time={}ms type={} query={}",
                                    connectionId, timeMs, type, query);
                            return;
                        }

                        if (timeMs > 100) {
                            log.warn("SLOW_QUERY conn={} time={}ms type={} query={}",
                                    connectionId, timeMs, type, shortened);
                            return;
                        }

                        if (log.isDebugEnabled()) {
                            log.debug("DB conn={} time={}ms type={} query={}",
                                    connectionId, timeMs, type, shortened);
                        }
                    });
                })
                .build();
    }
}