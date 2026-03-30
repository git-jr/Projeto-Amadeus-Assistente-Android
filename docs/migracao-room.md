# Plano de Migração: SQLite → Room

**Projeto:** Amadeus Android
**Banco atual:** `Amadeus.db` — versão 4 (SQLite via `SQLiteOpenHelper`)
**Destino:** Room Persistence Library
**Data do plano:** 2026-03-29

---

## 1. Visão Geral

O banco atual possui 5 tabelas gerenciadas manualmente via cursores e Gson. A migração para Room traz type-safety em queries, geração de código em tempo de compilação, integração com Kotlin Coroutines/Flow e eliminação de boilerplate de cursor.

### Tabelas existentes

| Tabela | Versão criada |
|--------|--------------|
| `autor` | v1 |
| `mensagem` | v1 |
| `sentenca` | v2 |
| `historico_sentenca` | v2 |
| `entidade` | v3 |

---

## 2. Instalar a Lib

### 2.1 `app/build.gradle` — adicionar dependências

```groovy
dependencies {
    // Room
    def room_version = "2.7.0"
    implementation "androidx.room:room-runtime:$room_version"
    implementation "androidx.room:room-ktx:$room_version"          // suporte a coroutines
    kapt "androidx.room:room-compiler:$room_version"

    // ... dependências existentes mantidas
}
```

### 2.2 Habilitar kapt no topo do mesmo arquivo

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'                  // ← adicionar esta linha
    id 'com.google.gms.google-services'
    id 'com.google.android.gms.oss-licenses-plugin'
}
```

> **Alternativa KSP (mais rápido):** substitua `kapt` por `ksp` e adicione `id 'com.google.devtools.ksp'` nos plugins, além de `ksp "androidx.room:room-compiler:$room_version"`. Requer `ksp` no `build.gradle` raiz.

---

## 3. TypeConverters (dados JSON)

As colunas `respostas`, `significados`, `sinonimos`, `acao` armazenam JSON como `VARCHAR`. No Room elas ficam como `String` na entidade e são convertidas para/de listas usando `TypeConverter`.

### `app/src/main/java/com/paradoxo/amadeus/dao/room/Converters.kt`

```kotlin
package com.paradoxo.amadeus.dao.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paradoxo.amadeus.modelo.AcaoEnum

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): MutableList<String> {
        if (value == null) return mutableListOf()
        val type = object : TypeToken<MutableList<String>>() {}.type
        return gson.fromJson(value, type) ?: mutableListOf()
    }

    @TypeConverter
    fun toStringList(list: MutableList<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromAcaoEnum(value: String?): AcaoEnum? {
        return value?.let { gson.fromJson(it, AcaoEnum::class.java) }
    }

    @TypeConverter
    fun toAcaoEnum(acao: AcaoEnum?): String? {
        return gson.toJson(acao)
    }
}
```

---

## 4. Entidades Room

Criar em `dao/room/entities/`. As entidades mapeiam exatamente as tabelas existentes para preservar dados na migração.

### 4.1 `AutorEntity.kt`

```kotlin
package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "autor")
data class AutorEntity(
    @PrimaryKey val id: Int,
    val nome: String?
)
```

### 4.2 `MensagemEntity.kt`

```kotlin
package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "mensagem",
    foreignKeys = [
        ForeignKey(
            entity = AutorEntity::class,
            parentColumns = ["id"],
            childColumns = ["fk_autor"]
        ),
        ForeignKey(
            entity = MensagemEntity::class,
            parentColumns = ["id"],
            childColumns = ["fk_resposta"]
        )
    ]
)
data class MensagemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val conteudo: String?,
    val fk_autor: Int?,
    val dt: Double?,           // REAL no SQLite original
    val fk_resposta: Int?
)
```

### 4.3 `SentencaEntity.kt`

```kotlin
package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sentenca")
data class SentencaEntity(
    @PrimaryKey val id: Int,
    val chave: String?,
    val respostas: String?,    // JSON — convertido via Converters
    val acao: String?,         // JSON — convertido via Converters
    val tipo_item: Int?,
    val idBanco: String?
)
```

### 4.4 `HistoricoSentencaEntity.kt`

```kotlin
package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historico_sentenca")
data class HistoricoSentencaEntity(
    @PrimaryKey val id: Int,
    val chave: String?,
    val respostas: String?,
    val acao: String?,
    val tipo_item: Int?
)
```

### 4.5 `EntidadeEntity.kt`

```kotlin
package com.paradoxo.amadeus.dao.room.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entidade")
data class EntidadeEntity(
    @PrimaryKey val id: Int,
    val nome: String?,
    val significados: String?,   // JSON
    val sinonimos: String?,      // JSON
    val atributos: Int?,
    val idBanco: String?
)
```

---

## 5. DAOs Room

Criar em `dao/room/`. Preservam a mesma assinatura funcional dos DAOs atuais.

### 5.1 `AutorRoomDAO.kt`

```kotlin
package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.AutorEntity

@Dao
interface AutorRoomDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(autor: AutorEntity): Long

    @Update
    suspend fun alterar(autor: AutorEntity)

    @Query("SELECT * FROM autor")
    suspend fun listar(): List<AutorEntity>

    @Query("SELECT * FROM autor WHERE id = :id")
    suspend fun buscarPorId(id: Int): AutorEntity?

    @Query("DELETE FROM autor")
    suspend fun deletarTodos()
}
```

### 5.2 `MensagemRoomDAO.kt`

```kotlin
package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.MensagemEntity

@Dao
interface MensagemRoomDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(mensagem: MensagemEntity): Long

    @Query("""
        SELECT m.*, r.conteudo as conteudo_resposta
        FROM mensagem m
        LEFT JOIN mensagem r ON m.fk_resposta = r.id
        ORDER BY m.id ASC
    """)
    suspend fun listarRespostasCompleto(): List<MensagemEntity>

    @Query("SELECT * FROM mensagem WHERE conteudo = :conteudo AND fk_autor = :autorId LIMIT 1")
    suspend fun verificarExistencia(conteudo: String, autorId: Int): MensagemEntity?

    @Query("DELETE FROM mensagem")
    suspend fun deletarTodas()
}
```

### 5.3 `SentencaRoomDAO.kt`

```kotlin
package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.SentencaEntity
import com.paradoxo.amadeus.dao.room.entities.HistoricoSentencaEntity

@Dao
interface SentencaRoomDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(sentenca: SentencaEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserirHistorico(sentenca: HistoricoSentencaEntity): Long

    @Query("SELECT * FROM sentenca WHERE chave LIKE :chave || '%' LIMIT 1")
    suspend fun buscaPorChave(chave: String): SentencaEntity?

    @Query("SELECT * FROM sentenca WHERE id = :id LIMIT 1")
    suspend fun buscaPorId(id: Int): SentencaEntity?

    @Query("SELECT * FROM sentenca WHERE chave LIKE :chave || '%' LIMIT :limite")
    suspend fun buscaPorChaveLista(chave: String, limite: Long): List<SentencaEntity>

    @Query("SELECT * FROM sentenca")
    suspend fun listar(): List<SentencaEntity>

    @Query("SELECT * FROM sentenca LIMIT :limite")
    suspend fun listarComLimite(limite: Long): List<SentencaEntity>

    @Query("SELECT * FROM sentenca WHERE id >= :idInicio")
    suspend fun listarAPartirDe(idInicio: Long): List<SentencaEntity>

    @Update
    suspend fun alterar(sentenca: SentencaEntity)

    @Delete
    suspend fun excluir(sentenca: SentencaEntity)

    @Query("SELECT COUNT(*) FROM sentenca")
    suspend fun getQuantidadeTotal(): Long

    // Histórico
    @Query("SELECT * FROM historico_sentenca")
    suspend fun listarHistorico(): List<HistoricoSentencaEntity>

    @Query("SELECT * FROM historico_sentenca WHERE chave LIKE :chave || '%' LIMIT 1")
    suspend fun buscaHistoricoPorChave(chave: String): HistoricoSentencaEntity?
}
```

### 5.4 `EntidadeRoomDAO.kt`

```kotlin
package com.paradoxo.amadeus.dao.room

import androidx.room.*
import com.paradoxo.amadeus.dao.room.entities.EntidadeEntity

@Dao
interface EntidadeRoomDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(entidade: EntidadeEntity): Long

    @Query("SELECT * FROM entidade WHERE nome LIKE :nome || '%' LIMIT 1")
    suspend fun buscaPorChave(nome: String): EntidadeEntity?

    @Query("SELECT * FROM entidade WHERE id = :id LIMIT 1")
    suspend fun buscaPorId(id: Int): EntidadeEntity?

    @Query("SELECT * FROM entidade WHERE nome LIKE :chave || '%' LIMIT :limite")
    suspend fun buscaPorChaveLista(chave: String, limite: Long): List<EntidadeEntity>

    @Query("SELECT * FROM entidade")
    suspend fun listar(): List<EntidadeEntity>

    @Query("SELECT * FROM entidade LIMIT :limite")
    suspend fun listarComLimite(limite: Long): List<EntidadeEntity>

    @Query("SELECT * FROM entidade WHERE id >= :idInicio")
    suspend fun listarAPartirDe(idInicio: Long): List<EntidadeEntity>

    @Update
    suspend fun alterar(entidade: EntidadeEntity)

    @Delete
    suspend fun excluir(entidade: EntidadeEntity)

    @Query("SELECT COUNT(*) FROM entidade")
    suspend fun getQuantidadeTotal(): Long
}
```

---

## 6. Database Room (+ Migração)

### `AmadeusDatabase.kt`

```kotlin
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
    version = 5,           // versão atual era 4 — Room assume controle a partir da 5
    exportSchema = true    // gera JSON do schema em app/schemas/ para auditoria
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
         * Migration 4 → 5: sem alteração de schema.
         * O banco existente (Amadeus.db v4) tem exatamente as mesmas tabelas
         * que o Room vai mapear. Nenhuma DDL é necessária — apenas informa
         * ao Room que o schema está correto.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Sem alterações de schema nesta migração.
                // O Room valida as tabelas existentes contra as entidades anotadas.
            }
        }

        fun getInstance(context: Context): AmadeusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AmadeusDatabase::class.java,
                    "Amadeus.db"            // mesmo nome — preserva os dados!
                )
                    .addMigrations(MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

> **Por que versão 5?** O Room precisa de uma versão maior que a atual (4) para executar o bloco de migração ao abrir o banco existente. O `MIGRATION_4_5` não precisa executar nenhum SQL porque o schema é idêntico — ele só serve para o Room saber que a transição é intencional e que os dados devem ser preservados.

---

## 7. Exportar Schema (recomendado)

Adicionar ao `app/build.gradle` para o Room gerar o JSON do schema, útil para auditar migrações futuras:

```groovy
android {
    defaultConfig {
        // ...
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }
}
```

---

## 8. Roteiro de Execução

Execute as etapas em ordem. Cada etapa é independente e pode ser commitada separadamente.

| # | Etapa | Arquivo(s) |
|---|-------|------------|
| 1 | Adicionar dependências Room + kapt | `app/build.gradle` |
| 2 | Criar `Converters.kt` | `dao/room/Converters.kt` |
| 3 | Criar 5 entidades | `dao/room/entities/*.kt` |
| 4 | Criar 4 DAOs Room | `dao/room/*RoomDAO.kt` |
| 5 | Criar `AmadeusDatabase.kt` | `dao/room/AmadeusDatabase.kt` |
| 6 | Configurar export de schema | `app/build.gradle` |
| 7 | Build e verificar compilação | `./gradlew assembleDebug` |
| 8 | Substituir usos dos DAOs antigos | Activities, Services, Cognição |
| 9 | Remover DAOs antigos e `BDHelper`/`BDGateway` | `dao/*.kt` antigos |

---

## 9. Substituição de Uso (passo 8 em detalhe)

Cada lugar que instancia um DAO antigo deve passar a usar `AmadeusDatabase`:

**Antes (padrão atual):**
```kotlin
val dao = SentencaDAO(context, usarTabelaHistorico = false)
val lista = dao.listar()
```

**Depois (Room + coroutines):**
```kotlin
val db = AmadeusDatabase.getInstance(context)
val lista = withContext(Dispatchers.IO) {
    db.sentencaDAO().listar()
}
```

### Arquivos que precisam de atualização

| Arquivo | DAOs usados atualmente |
|---------|----------------------|
| `SplashScreenActivityJava.java` | SentencaDAO, EntidadeDAO, AutorDAO, MensagemDAO |
| `ListaSentencaActivityJava.java` | SentencaDAO |
| `ListaEntidadeActivityJava.java` | EntidadeDAO |
| `EditarSentencaActivityJava.java` | SentencaDAO |
| `EditarItemEntidadeNovoActivityJava.java` | EntidadeDAO |
| `AprendizActivityJava.java` | SentencaDAO, EntidadeDAO |
| `RetroCompatibilidadeActivityJava.java` | múltiplos |
| `Processadora.kt` | SentencaDAO, EntidadeDAO |
| `Acionadora.kt` | SentencaDAO, EntidadeDAO |
| `GravaHistoricoService` | MensagemDAO, AutorDAO |

---

## 10. Validação pós-migração

```bash
# 1. Build limpo
./gradlew clean assembleDebug

# 2. Lint (verifica queries Room em tempo de compilação)
./gradlew lint

# 3. Instalar no dispositivo com banco v4 existente e verificar
#    se os dados aparecem (migração 4→5 funcionou)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verificar no Logcat que não aparece:
- `Room cannot verify the data integrity` → indica schema divergente
- `IllegalStateException: Migration didn't properly handle` → migração incompleta

---

## 11. Notas e Riscos

| Risco | Mitigação |
|-------|-----------|
| Schema divergente entre Room e banco existente | Usar `fallbackToDestructiveMigration()` apenas em dev; nunca em release |
| Coluna `atributos` em `entidade` é `INTEGER` no banco mas armazena dados JSON em `Entidade.kt` | Manter como `Int?` no Room; ajustar o modelo em refactor futuro |
| DAOs atuais em Java (`*Java.java`) estão excluídos do build | Podem ser removidos com segurança após o passo 9 |
| `AcaoDAO` lê de asset JSON, não do banco | Não migrar — manter como está |

---

## Estrutura de pastas esperada ao final

```
dao/
├── room/
│   ├── AmadeusDatabase.kt
│   ├── Converters.kt
│   ├── AutorRoomDAO.kt
│   ├── MensagemRoomDAO.kt
│   ├── SentencaRoomDAO.kt
│   ├── EntidadeRoomDAO.kt
│   └── entities/
│       ├── AutorEntity.kt
│       ├── MensagemEntity.kt
│       ├── SentencaEntity.kt
│       ├── HistoricoSentencaEntity.kt
│       └── EntidadeEntity.kt
├── AcaoDAO.kt          ← mantido (lê asset JSON)
└── [demais arquivos antigos removidos no passo 9]
```
