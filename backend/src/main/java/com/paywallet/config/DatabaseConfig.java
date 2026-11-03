package com.paywallet.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    // Regex to parse URI-style or JDBC-style postgres connection strings:
    // e.g. postgresql://user:pass@host:port/db?params
    // or   jdbc:postgresql://user:pass@host:port/db?params
    // or   jdbc:postgresql://host:port/db?params
    private static final Pattern DB_URL_PATTERN = Pattern.compile(
            "^(?:jdbc:)?(?:postgresql|postgres)://(?:([^:@]+)(?::([^@]+))?@)?([^:/?#]+)(?::(\\d+))?(/[^?#]*)?(?:\\?(.*))?$",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${spring.datasource.url}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setConnectionTimeout(connectionTimeout);
        config.setPoolName("PayWalletHikariCP");

        ParsedDatabaseUrl parsed = parseDatabaseUrl(rawUrl, username, password);
        config.setJdbcUrl(parsed.jdbcUrl());
        if (StringUtils.hasText(parsed.username())) {
            config.setUsername(parsed.username());
        }
        if (StringUtils.hasText(parsed.password())) {
            config.setPassword(parsed.password());
        }

        log.info("Initialized DataSource with sanitized JDBC URL: {}", maskJdbcUrl(parsed.jdbcUrl()));
        return new HikariDataSource(config);
    }

    public static ParsedDatabaseUrl parseDatabaseUrl(String rawUrl, String defaultUser, String defaultPassword) {
        if (!StringUtils.hasText(rawUrl)) {
            return new ParsedDatabaseUrl(rawUrl, defaultUser, defaultPassword);
        }

        Matcher matcher = DB_URL_PATTERN.matcher(rawUrl.trim());
        if (matcher.matches()) {
            String uriUser = matcher.group(1);
            String uriPass = matcher.group(2);
            String host = matcher.group(3);
            String portStr = matcher.group(4);
            String path = matcher.group(5);
            String query = matcher.group(6);

            int port = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 5432;
            String dbPath = (path != null && !path.isEmpty()) ? path : "/postgres";

            // Sanitize query params (remove channel_binding if present, ensure sslmode is safe)
            String cleanQuery = sanitizeQueryParams(query);

            String cleanJdbcUrl = "jdbc:postgresql://" + host + ":" + port + dbPath;
            if (StringUtils.hasText(cleanQuery)) {
                cleanJdbcUrl += "?" + cleanQuery;
            }

            String finalUser = StringUtils.hasText(uriUser) ? uriUser : defaultUser;
            String finalPass = StringUtils.hasText(uriPass) ? uriPass : defaultPassword;

            return new ParsedDatabaseUrl(cleanJdbcUrl, finalUser, finalPass);
        }

        // Fallback: return as-is
        return new ParsedDatabaseUrl(rawUrl, defaultUser, defaultPassword);
    }

    private static String sanitizeQueryParams(String query) {
        if (!StringUtils.hasText(query)) {
            return "sslmode=require";
        }

        StringBuilder sb = new StringBuilder();
        String[] pairs = query.split("&");
        boolean hasSslMode = false;

        for (String pair : pairs) {
            if (pair.startsWith("channel_binding=")) {
                // Skip channel_binding as standard PostgreSQL JDBC driver doesn't support it
                continue;
            }
            if (pair.startsWith("sslmode=")) {
                hasSslMode = true;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(pair);
        }

        if (!hasSslMode) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append("sslmode=require");
        }

        return sb.toString();
    }

    private static String maskJdbcUrl(String url) {
        if (url == null) return null;
        return url.replaceAll("://.*@", "://***:***@");
    }

    public record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {}
}
