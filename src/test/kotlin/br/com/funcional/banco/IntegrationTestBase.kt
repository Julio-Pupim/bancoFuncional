package br.com.funcional.banco

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
abstract class IntegrationTestBase {

    companion object {

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")

        @JvmStatic
        @DynamicPropertySource
        fun configure(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") {
                postgres.jdbcUrl
            }

            registry.add("spring.datasource.username") {
                postgres.username
            }

            registry.add("spring.datasource.password") {
                postgres.password
            }
        }
    }
}