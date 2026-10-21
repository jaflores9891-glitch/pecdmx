package mx.pecdmx.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		// Versión fija (la misma que Neon) para que los tests no cambien solos con cada release de PostgreSQL.
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
	}

}
