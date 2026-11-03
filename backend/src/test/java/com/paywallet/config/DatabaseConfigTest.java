package com.paywallet.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseConfigTest {

    @Test
    void shouldParseNeonConnectionUriWithCredentialsAndParams() {
        String neonUri = "jdbc:postgresql://neondb_owner:npg_nH84qARfsCQL@ep-crimson-poetry-bevuitty-pooler.c-9.us-east-2.aws.neon.tech/neondb?sslmode=require&channel_binding=require";

        DatabaseConfig.ParsedDatabaseUrl result = DatabaseConfig.parseDatabaseUrl(neonUri, null, null);

        assertThat(result.jdbcUrl())
                .isEqualTo("jdbc:postgresql://ep-crimson-poetry-bevuitty-pooler.c-9.us-east-2.aws.neon.tech:5432/neondb?sslmode=require");
        assertThat(result.username()).isEqualTo("neondb_owner");
        assertThat(result.password()).isEqualTo("npg_nH84qARfsCQL");
    }

    @Test
    void shouldParseRawPostgresqlSchemeWithoutJdbcPrefix() {
        String rawUri = "postgresql://user123:secret456@myhost.neon.tech/mydb?sslmode=require";

        DatabaseConfig.ParsedDatabaseUrl result = DatabaseConfig.parseDatabaseUrl(rawUri, null, null);

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://myhost.neon.tech:5432/mydb?sslmode=require");
        assertThat(result.username()).isEqualTo("user123");
        assertThat(result.password()).isEqualTo("secret456");
    }

    @Test
    void shouldRetainStandardJdbcUrlWithoutEmbeddedCredentials() {
        String normalJdbc = "jdbc:postgresql://localhost:5434/paywallet";

        DatabaseConfig.ParsedDatabaseUrl result = DatabaseConfig.parseDatabaseUrl(normalJdbc, "devuser", "devpass");

        assertThat(result.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5434/paywallet?sslmode=require");
        assertThat(result.username()).isEqualTo("devuser");
        assertThat(result.password()).isEqualTo("devpass");
    }
}
