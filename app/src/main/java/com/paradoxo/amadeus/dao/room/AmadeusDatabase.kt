package com.paradoxo.amadeus.dao.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.paradoxo.amadeus.dao.room.entities.*

@Database(
    entities = [
        AutorEntity::class,
        MensagemEntity::class,
        SentencaEntity::class,
        HistoricoSentencaEntity::class,
        EntidadeEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AmadeusDatabase : RoomDatabase() {

    abstract fun autorDAO(): AutorRoomDAO
    abstract fun mensagemDAO(): MensagemRoomDAO
    abstract fun sentencaDAO(): SentencaRoomDAO
    abstract fun entidadeDAO(): EntidadeRoomDAO

    companion object {
        @Volatile
        private var INSTANCE: AmadeusDatabase? = null

        /**
         * Migration 4 → 5: Migração completa para usuários que já possuem o app instalado.
         *
         * Problema raiz: o Room valida o schema por PRAGMA table_info / foreign_key_list.
         * As tabelas originais foram criadas sem NOT NULL explícito nos PKs e com tipos
         * VARCHAR (vs TEXT que o Room gera). Para garantir que a validação não falhe,
         * todas as tabelas são recriadas com o schema exato que o Room espera, preservando
         * 100% dos dados via INSERT ... SELECT antes do DROP.
         *
         * Tabelas afetadas:
         *   - autor             → NOT NULL no PK, VARCHAR → TEXT
         *   - mensagem          → NOT NULL no PK, VARCHAR → TEXT, FK format
         *   - sentenca          → NOT NULL no PK, VARCHAR → TEXT
         *   - historico_sentenca→ NOT NULL no PK, VARCHAR → TEXT
         *   - entidade          → NOT NULL no PK, VARCHAR → TEXT, atributos INTEGER → TEXT
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = OFF")

                migrarAutor(db)
                migrarMensagem(db)
                migrarSentenca(db)
                migrarHistoricoSentenca(db)
                migrarEntidade(db)

                db.execSQL("PRAGMA foreign_keys = ON")
            }

            // ── autor ────────────────────────────────────────────────────────
            private fun migrarAutor(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE autor_new (
                        id   INTEGER PRIMARY KEY NOT NULL,
                        nome TEXT
                    )
                """.trimIndent())
                db.execSQL("INSERT INTO autor_new (id, nome) SELECT id, nome FROM autor")
                db.execSQL("DROP TABLE autor")
                db.execSQL("ALTER TABLE autor_new RENAME TO autor")
            }

            // ── mensagem ─────────────────────────────────────────────────────
            private fun migrarMensagem(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE mensagem_new (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        conteudo    TEXT,
                        fk_autor    INTEGER,
                        dt          REAL,
                        fk_resposta INTEGER,
                        FOREIGN KEY(fk_autor)    REFERENCES autor(id),
                        FOREIGN KEY(fk_resposta) REFERENCES mensagem(id)
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO mensagem_new (id, conteudo, fk_autor, dt, fk_resposta)
                    SELECT id, conteudo, fk_autor, dt, fk_resposta FROM mensagem
                """.trimIndent())

                // Preserva o contador AUTOINCREMENT para que novos IDs não colidam
                db.execSQL("""
                    INSERT OR REPLACE INTO sqlite_sequence (name, seq)
                    VALUES ('mensagem_new', (SELECT COALESCE(MAX(id), 0) FROM mensagem_new))
                """.trimIndent())

                db.execSQL("DROP TABLE mensagem")
                db.execSQL("ALTER TABLE mensagem_new RENAME TO mensagem")

                // Corrige o nome na tabela de sequências após o rename
                db.execSQL("UPDATE sqlite_sequence SET name = 'mensagem' WHERE name = 'mensagem_new'")
            }

            // ── sentenca ─────────────────────────────────────────────────────
            private fun migrarSentenca(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE sentenca_new (
                        id        INTEGER PRIMARY KEY NOT NULL,
                        chave     TEXT,
                        respostas TEXT,
                        acao      TEXT,
                        tipo_item INTEGER,
                        idBanco   TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO sentenca_new (id, chave, respostas, acao, tipo_item, idBanco)
                    SELECT id, chave, respostas, acao, tipo_item, idBanco FROM sentenca
                """.trimIndent())
                db.execSQL("DROP TABLE sentenca")
                db.execSQL("ALTER TABLE sentenca_new RENAME TO sentenca")
            }

            // ── historico_sentenca ───────────────────────────────────────────
            private fun migrarHistoricoSentenca(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE historico_sentenca_new (
                        id        INTEGER PRIMARY KEY NOT NULL,
                        chave     TEXT,
                        respostas TEXT,
                        acao      TEXT,
                        tipo_item INTEGER
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO historico_sentenca_new (id, chave, respostas, acao, tipo_item)
                    SELECT id, chave, respostas, acao, tipo_item FROM historico_sentenca
                """.trimIndent())
                db.execSQL("DROP TABLE historico_sentenca")
                db.execSQL("ALTER TABLE historico_sentenca_new RENAME TO historico_sentenca")
            }

            // ── entidade ─────────────────────────────────────────────────────
            // atributos era INTEGER no schema original mas armazenava JSON — corrigido para TEXT
            private fun migrarEntidade(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE entidade_new (
                        id          INTEGER PRIMARY KEY NOT NULL,
                        nome        TEXT,
                        significados TEXT,
                        sinonimos   TEXT,
                        atributos   TEXT,
                        idBanco     TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO entidade_new (id, nome, significados, sinonimos, atributos, idBanco)
                    SELECT id, nome, significados, sinonimos, CAST(atributos AS TEXT), idBanco
                    FROM entidade
                """.trimIndent())
                db.execSQL("DROP TABLE entidade")
                db.execSQL("ALTER TABLE entidade_new RENAME TO entidade")
            }
        }

        fun getInstance(context: Context): AmadeusDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AmadeusDatabase::class.java,
                    "Amadeus.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    // Versões 1, 2, 3 eram esquemas muito antigos já cobertos pelo
                    // onUpgrade do SQLiteOpenHelper. Na prática todos os usuários
                    // chegam na v4, mas caso alguém nunca tenha aberto o app o banco
                    // é recriado do zero (dados padrão recarregados pelo SplashScreen).
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
