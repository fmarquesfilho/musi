package br.ufrn.musi.adaptadores.persistencia

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

/**
 * Onde está o banco. Vem do ambiente (fator III do 12-Factor App), nunca do código.
 *
 * Sem `DB_URL`, não há banco: é o caso do deploy no Render até a Sprint 3 (ADR-0004). A
 * API sobe mesmo assim, com a busca funcionando e o CRUD respondendo `503`.
 */
data class ConfigBanco(val url: String, val usuario: String, val senha: String) {
    companion object {
        fun doAmbiente(): ConfigBanco? = System.getenv("DB_URL")?.let { url ->
            ConfigBanco(
                url = url,
                usuario = System.getenv("DB_USER") ?: "musi",
                senha = System.getenv("DB_PASSWORD") ?: "musi",
            )
        }
    }
}

/**
 * Pool de conexões: abrir conexão é caro, e o pool reaproveita.
 *
 * Pool pequeno de propósito: a instância tem 512 MB, e o plano gratuito do Neon (Sprint 3)
 * limita conexões concorrentes.
 */
fun criarDataSource(config: ConfigBanco): HikariDataSource = HikariDataSource(
    HikariConfig().apply {
        jdbcUrl = config.url
        username = config.usuario
        password = config.senha
        maximumPoolSize = 5
        poolName = "musi"
    },
)

/**
 * Aplica as migrações pendentes de `src/main/resources/db/migration`.
 *
 * O esquema é SÓ da migração: o Exposed mapeia tabelas (Tabelas.kt), mas nunca as cria
 * nem altera. Nada de `SchemaUtils.create`.
 */
fun migrar(dataSource: DataSource) {
    Flyway.configure().dataSource(dataSource).load().migrate()
}
