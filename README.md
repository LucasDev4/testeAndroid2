# WeSee (Android / Kotlin / Jetpack Compose)

```
com.wesee.app
├── MainActivity.kt
├── navigation/WeSeeNavigation.kt      # rotas + botão voltar
└── ui
    ├── theme/        Color.kt, Type.kt (Host Grotesk)
    ├── components/   WeSeeText, WeSeeHeader (logo/progresso/olhos), WeSeeButtons, WeSeeTextField, BottomPanel
    └── screens/
        ├── welcome/  WelcomeScreen        (Login / Cadastro)
        ├── profile/  WhoAreYouScreen      (Quem é você?)
        ├── guardian/ GuardianLoginScreen, GuardianRegisterScreen
        ├── pcd/      home/ (PcdHomeScreen, CameraArea, HomeCards, NoteEditor), chat/ (PcdChatScreen)
        └── disabled/ DisabledLoginScreen, DisabledRegisterScreen, VoiceAuthScreen (microfone/biometria)

com.wesee.app.voice  VoiceAssistant (TTS + reconhecimento + bipes, VoiceConfig = voz), VoiceSession (conversa guiada)
com.wesee.app.auth   BiometricAuth
```
Fluxo: Welcome → (Cadastro) WhoAreYou → (Responsável do PCD) GuardianLogin ⇄ GuardianRegister;
(PCD visual) DisabledRegister → DisabledLogin.
Textos das telas do responsável ficam em `res/values/strings.xml`. Imagens em `res/drawable-nodpi`.

com.wesee.app.data    UserStore, Notes (NotesStore)
com.wesee.app.device  Battery (bateria real + estimativa)

## Mapa (Google Maps Platform)
Ative no Google Cloud apenas o **Maps SDK for Android** (o app só desenha o mapa). **Places API e Routes API não são mais usadas** e podem ser desativadas:
pesquisa de endereço (Nominatim) e rotas (OSRM) passam pelo backend. Restrinja a chave ao pacote `com.wesee.app`.
A chave NÃO fica no código: use a variável de ambiente `MAPS_API_KEY` (no GitHub: Settings → Secrets and variables → Actions → New repository secret)
ou `MAPS_API_KEY=...` em `local.properties` (já no .gitignore).

## Conexão com o backend (Backend-atlas)
O app fala com o Spring Boot via `network/AtlasApi.kt` (`/auth/register`, `/auth/login`, `/user/me`). URL em `API_BASE_URL`:
- Emulador: padrão `http://10.0.2.2:8080/` (nada a configurar).
- Celular real: coloque `API_BASE_URL=http://IP-DO-SEU-PC:8080/` em `local.properties` (celular e PC na mesma rede Wi-Fi; libere a porta 8080 no firewall).
- GitHub Actions: crie o secret `API_BASE_URL` (e `MAPS_API_KEY`).
HTTP puro só é permitido no build **debug** (`src/debug`). Em produção use HTTPS.
Regras do backend refletidas no app: senha com **exatamente 6 números** (na voz: "um dois três quatro cinco seis"), CPF 11 dígitos, telefone 10–11 dígitos.
Sessão: o token JWT fica salvo no aparelho; ao abrir o app ele é validado em `/user/me` (401/403 → volta ao login). Logout apaga tudo.

## Área do Responsável (dados demonstrativos)
Telas: Home, painel de Notificações (sino) e Localização — `ui/screens/guardian/home` + `ui/screens/guardian/components`.
A pessoa acompanhada e a **localização** são reais (vínculos + `GET /api/responsavel/pcds/{id}/localizacao`, veja `GuardianTracking.kt`). Status do dispositivo, alertas e notificações
ainda são fictícios em `data/GuardianDemoData.kt` (trocar por API depois). O mapa (`GuardianMap.kt`) usa o Google Maps SDK quando
`MAPS_API_KEY` está configurada (variável de ambiente / GitHub Secret / `local.properties`) e, sem chave, mostra um mapa **ilustrativo**.
Fluxo: Login do responsável → GuardianHome ⇄ GuardianMaps. A aba Account ainda não tem tela.

## Perfil do Responsável e nomes dinâmicos
`ui/screens/profile/ProfileContent.kt` é o Perfil compartilhado (PCD e Responsável). O nome do responsável vem do login (`UserStore.name`);
`data/PersonNames.kt` deriva o nome da pessoa acompanhada ("Gabriel Rodrigues" → "Gabriel Rodrigues Jr.", sem duplicar "Jr.").
## Ícones
Ícones convertidos para VectorDrawable a partir dos SVGs em `design/icons/` (lock, mail, call_end, nomeIcon, Exterior → casa, OlhosIconAzul → olhos).

## Vínculos PCD ⇄ Responsável (modais no Perfil)
Opção **"Meus responsáveis"** (PCD) / **"Pessoas vinculadas"** (Responsável) no Perfil → modal (`ui/screens/links/`):
`LinksController` (estado + chamadas) e `LinksModal` (UI). Endpoints usados (token do login): `GET /api/vinculos`,
`POST /api/vinculos/codigos` (PCD) e `POST /api/vinculos/solicitar` (Responsável). O tamanho do código é `VINCULO_CODE_LENGTH` (6)
e deve ser igual a `vinculo.codigo-tamanho` do backend. Ícone da opção: `design/icons/family_restroom.svg`.

## Conectar o app ao backend (Backend-atlas)
1. **Suba o MySQL e o backend** (veja o README do Backend-atlas) e confira com `python scripts/smoke_test.py` — se der "verificações passaram", o contrato com o app está ok.
2. **Diga ao app onde está o backend** (copie `local.properties.example` para `local.properties`):
   - *Emulador:* nada a fazer (`http://10.0.2.2:8080/` é o padrão).
   - *Celular por USB (mais fácil, sem firewall/Wi-Fi):* `adb reverse tcp:8080 tcp:8080` e `API_BASE_URL=http://localhost:8080/`.
   - *Celular no Wi-Fi:* `API_BASE_URL=http://IP-DO-PC:8080/`, mesma rede e porta 8080 liberada no firewall (teste abrindo `http://IP-DO-PC:8080/swagger-ui/index.html` no navegador do celular).
   - *APK do GitHub Actions:* crie o secret `API_BASE_URL` (mesmo formato) e rode o workflow de novo — o valor entra no APK na hora da compilação.
3. **Instale o APK novo.** Se o servidor não responder, o app (build debug) diz por voz/texto qual endereço tentou usar.

Endpoints usados pelo app: `POST /auth/register`, `POST /auth/login`, `GET /user/me`, `GET /api/vinculos`, `POST /api/vinculos/codigos`, `POST /api/vinculos/solicitar`.
O app só usa HTTP puro no build **debug** (`src/debug`); em produção use HTTPS.

## Câmera com IA na Home do PCD
O card grande do início da Home agora mostra a **câmera real** (CameraX). A cada ~1,5 s um quadro JPEG vai para `POST /detect` do backend (token do login);
o backend chama a IA (YOLO) e devolve os objetos e as frases em português, que o app **fala** (TextToSpeech):
- perigo (prioridade alta e muito próximo): "Atenção! Pessoa muito próxima à sua frente" — interrompe e repete a cada 5 s;
- cena mudou: "Vejo duas pessoas à sua frente e um carro à sua direita." (no máx. a cada 3 s); cena igual: lembrete a cada 20 s; vazio: "Caminho livre."
Código: `ui/screens/pcd/home/camera/` (`LiveCamera.kt` = câmera e permissão; `SceneDetection.kt` = envio dos quadros + regras de fala).
O topo da Home mostra "Sistema ativo" (verde) enquanto a IA responde. Pede a permissão da câmera ao abrir a Home.
Para funcionar: IA (`WeSeeIa`) + backend + MySQL rodando e o app com `API_BASE_URL` apontando para o backend (veja "Conectar o app ao backend").


## Backend no Render (padrão) e navegação
- O app já aponta para **https://backend-wesee-final.onrender.com/** (padrão em `app/build.gradle.kts`). `API_BASE_URL` (variável de ambiente, GitHub Secret ou `local.properties`) tem prioridade: **se o seu secret `API_BASE_URL` ainda tiver o IP do PC, apague-o ou troque pelo endereço do Render**, senão ele vence o padrão.
- Plano gratuito do Render dorme após ~15 min parado: a 1ª chamada pode levar ~1 min. O app "acorda" o servidor ao abrir e usa timeouts longos.
- **Câmera com IA no Render:** o backend precisa alcançar a IA (`WeSeeIa`). No painel do Render, defina `ATLAS_AI_BASE_URL` no backend com o endereço público da IA (veja o README da WeSeeIa). Sem isso o app mostra "A IA está indisponível agora".
- **Navegação:** pilha de telas (`navigation/WeSeeNavigation.kt`). Tela inicial → *Login* ou *Cadastro* → "Quem é você?" → PCD (voz) ou Responsável, já na tela de login ou de cadastro conforme o botão tocado. Voltar desempilha; após o login a pilha é limpa.
- **Câmera em tela cheia:** na Home do PCD, toque na câmera → botão "Tela cheia"; na tela cheia (celular em pé, barras escondidas) há o botão "Sair da tela cheia" (ou o botão voltar). O app inteiro fica em modo retrato.

## Chat do Navi (respostas pré-programadas, sem IA)
`ui/screens/pcd/chat/`: `ChatScreen.kt` (tela e componentes) e `NaviChatData.kt` (as 6 sugestões, **30 respostas** — 5 por card — e o `ReplyRotator`).
100% local: nenhuma chamada de rede e nenhuma IA/API. Estado 1: microfone + grid assimétrico de sugestões. Estado 2 (ao tocar num card): o microfone some e a
área central vira a conversa (balão azul do usuário à direita, balão claro do Navi à esquerda, histórico até "Nova conversa"). O campo de mensagem fica
desativado (o Navi só responde pelas sugestões). A resposta é lida em voz alta, exceto com o TalkBack ligado (ele já lê o balão). Olhos do Navi: `design/icons/OlhosNaviNovo.svg`.
Para editar textos/tamanhos dos cards: só `NaviChatData.kt`.

## Pesquisa de destino, rota e localização (via backend)
O app **não** fala com Nominatim/OSRM: tudo passa pelo backend (`AtlasApi.searchPlaces / reverseGeocode / route / publishLocation / pcdLocation`).
- **Mapas do PCD** (`MapsScreen.kt` + `MapsState.kt`): digite o destino e toque em *Buscar* (ou na tecla de busca). Não há autocomplete a cada tecla (o Nominatim público não permite).
  Escolha um resultado → a rota é pedida com a posição real (Fused Location) → o Google Maps desenha a `Polyline`; *Direção* mostra as etapas.
- **Rota a pé**: enquanto o backend não confirmar um servidor OSRM com perfil de pedestre (`OSRM_WALKING_CONFIRMED`), o app mostra a mensagem do backend em vez de uma rota. Rota de carro nunca é exibida como caminhada. A rota não garante calçada acessível nem travessia segura.
- **Home do PCD**: endereço aproximado real (geocodificação reversa). **Compartilhamento**: com o app aberto, o PCD envia a posição a cada ~60 s (`PcdLocationSharing`); o backend só guarda se houver responsável vinculado com permissão de localização. Não há localização em segundo plano.
- Exibe a atribuição "Dados © colaboradores do OpenStreetMap".

## Idioma, tema e opções do Perfil
As opções do **Perfil** (PCD e Responsável compartilham `ProfileContent` + `ProfilePages.kt`) abrem páginas por cima do Perfil; o botão voltar do sistema fecha a página.
- **Idioma** (Português Brasil / English): troca na hora, em todo o app, sem reiniciar e sem mudar o idioma do aparelho. Textos em `res/values/strings.xml` (PT) e `res/values-en/strings.xml` (EN);
  telas usam `stringResource(...)` e código fora de telas usa `tr(R.string...)`. `WeSeeTheme` fornece um `Context` com o idioma escolhido. Voz (TTS) e reconhecimento de voz seguem o idioma
  (EN = `Locale.US`; sem a voz instalada, cai para o idioma padrão do aparelho, sem erro). Em inglês, a descrição da cena e as instruções de rota são montadas no app a partir dos dados do backend
  (`SceneDescriber`, `RouteInstructions`); mensagens de erro/validação enviadas pelo servidor continuam em português.
- **Aparência** (Claro / Escuro): global e salva. As cores são papéis em `ui/theme/Color.kt` (`LightPalette` = cores originais, `DarkPalette` = paleta escura); os nomes antigos (`WeSeeBlue`...) seguem o tema.
  Para texto/ícones sobre o fundo do tema use `WeSeeInk`; `WeSeeBlue` é a superfície azul (texto branco por cima). Ícones de uma cor usam `ThemedImage`.
- **Notificações**: apenas a preferência (liga/desliga, salva). O app ainda NÃO envia notificações reais nem pede permissão de notificações.
- **Histórico, Termos de uso, Política de privacidade, FAQ**: conteúdo demonstrativo (identificado na tela). **Apagar conta**: só informa que a exclusão ainda não existe (o backend não tem esse recurso).
- Preferências ficam em `wesee_settings` (`AppSettings`), que não é apagado no logout.
