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
