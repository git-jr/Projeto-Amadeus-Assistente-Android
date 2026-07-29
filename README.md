# Projeto Amadeus: Assistente Virtual Android

Amadeus é um assistente virtual para celulares Android. Ele conversa por texto
e voz, aprende com o usuário e, no modo agente, consegue usar outros apps a
partir de um pedido em linguagem natural.

<a href='https://play.google.com/store/apps/details?id=com.paradoxo.amadeus&pcampaignid=MKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img alt='Disponível no Google Play' src='https://play.google.com/intl/pt-BR/badges/images/generic/pt-br_badge_web_generic.png' width="250px"/></a>

## Um pouco de história

O projeto nasceu ao longo de uma [série de vídeos no YouTube](https://www.youtube.com/watch?v=dyXFa1ymL-M&list=PLgkr0bcGnH6YtPQO1774MBPhJB4tVBk8L&index=1).

O primeiro protótipo foi feito em Python, codificado direto em um celular
Android, com a ajuda de dicas e sugestões de quem acompanhava a série. Como
havia limites para o que dava para fazer só com essas ferramentas, ele foi
reescrito como um app nativo em Java. O app herdou os recursos que o protótipo
já tinha, como reconhecimento de voz e comandos para tocar músicas, e ganhou
outros que não eram possíveis por lá. Hoje o código está sendo migrado para
Kotlin e o app ganhou um modo agente com IA.

## O que o app faz

- Conversa por texto e voz, usando o reconhecimento de fala e a síntese de voz
  do Android. As respostas podem ser ensinadas e editadas pelo próprio usuário.
- Responde à palavra de ativação "Amadeus", reconhecida offline com
  [Vosk](https://alphacephei.com/vosk/).
- Pode ser definido como assistente padrão do Android.
- No modo agente, usa o serviço de acessibilidade para ler a tela e executar
  tarefas em outros apps, como tocar, digitar, abrir apps, rolar e voltar. Esse
  modo é acionado por um botão flutuante.
- Grava macros, que são sequências de ações reutilizáveis. As macros
  confirmadas servem de exemplo para o modelo em tarefas parecidas.
- Permite escolher a IA usada: OpenAI (via Cloud Function), Google Gemini (via
  Firebase AI Logic) ou modelos que rodam no próprio aparelho com
  [LiteRT-LM](https://github.com/google-ai-edge/LiteRT-LM), como Gemma 3 1B,
  Gemma 4 E2B e Gemma 4 E4B.

## Tecnologias

Kotlin (com partes ainda em Java), Jetpack Compose e Views, Room, Firebase
(Auth, Realtime Database, Remote Config, Cloud Messaging, Functions, AI Logic e
Analytics), Google Play Billing, Vosk, LiteRT-LM, EventBus e Lottie.

## Como compilar

Você vai precisar de uma versão recente do Android Studio, do JDK 17 e do
Android SDK 36. O app roda a partir do Android 7.0 (API 24).

1. Clone o repositório e abra no Android Studio.
2. Crie um projeto no [Firebase](https://console.firebase.google.com/) e
   adicione um app Android com o pacote `com.paradoxo.amadeus`. Se preferir
   outro pacote, troque o `applicationId` em `app/build.gradle`.
3. Baixe o `google-services.json` do seu projeto e coloque na pasta `app/`.
   Esse arquivo não é versionado.
4. No Firebase, ative:
   - Authentication, com os provedores Anônimo e Google. Para o login com
     Google funcionar, cadastre o SHA-1 da sua chave de debug nas
     configurações do app.
   - Realtime Database e Cloud Messaging.
   - Firebase AI Logic (Gemini Developer API), se quiser usar o Gemini.

   O Remote Config é opcional, porque o app já traz valores padrão no código.
5. Compile e instale:
   ```bash
   ./gradlew installDebug
   ```
   Essa tarefa também concede a permissão de acessibilidade via `adb`, o mesmo
   que o script `./grant_accessibility.sh` faz.

### O que depende do backend

As Cloud Functions usadas pelo app não estão neste repositório. Sem elas:

- O provedor OpenAI não funciona, porque depende da função `chatCompletion`.
  No lugar dele, use o Gemini ou um modelo local: no painel do agente, escolha
  "Google Gemini" em "Inteligência" ou ative um modelo em "Modelo de IA". Também
  dá para definir o parâmetro `ai_provider` como `GEMINI` no Remote Config.
- A assinatura premium não é validada, já que é o backend que grava
  `entitlements/{uid}`. Para facilitar o desenvolvimento, os builds de debug
  têm `FORCE_PREMIUM_FOR_TESTS` ativado, o que libera os recursos premium.

Alguns modelos locais pedem um token do Hugging Face, que é informado na
própria tela de modelos locais do app.

## Estrutura do projeto

| Pacote | Conteúdo |
|---|---|
| `activity/`, `ui/` | Telas. As mais novas usam Compose com ViewModel e UiState |
| `cognicao/` | Processamento das mensagens (`Processadora`, `Senteciadora` e `Acionadora`) e os interpretadores do agente para OpenAI, Gemini e LiteRT |
| `service/` | Acessibilidade, botão flutuante, gravador de macros e palavra de ativação |
| `util/voz/` | Voz para texto e texto para voz |
| `dao/` | Banco de dados local (Room) |
| `payments/`, `auth/` | Assinatura pelo Google Play e login com Google |
| `firebase/` | Notificações via Firebase Cloud Messaging |

Boa parte da comunicação entre os componentes passa pelo EventBus. Para seguir
o fluxo, procure os métodos anotados com `@Subscribe`. Os arquivos
`*Java.java` são as versões antigas em Java, mantidas como referência durante a
migração e excluídas do build.

Comandos úteis: `./gradlew assembleDebug`, `./gradlew test` e `./gradlew lint`.

## Licença

O projeto é distribuído sob a licença MIT, disponível em [LICENSE](LICENSE). O
modelo de voz do Vosk que acompanha o app usa a licença Apache 2.0, descrita em
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
