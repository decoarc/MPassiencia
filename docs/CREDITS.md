# Créditos e licenças

## Arte e som do jogo

- **Cartas, verso e mesa:** desenhados no próprio código do app (`app/.../ui/cards/CardFace.kt`, `ui/theme/Felt.kt`). Arte própria do projeto.
- **Efeitos sonoros** (`app/src/main/res/raw/*.wav`): gerados pelo script `tools/gen_sounds.py`, que sintetiza ondas e ruído com a biblioteca padrão do Python. São sons próprios do projeto.
- **Regras:** o Paciência Triplo é uma variação do Klondike com três baralhos, criada por Thomas Warfield. As regras de um jogo não têm direito autoral. O texto de "Como Jogar" foi escrito para este app.

## Bibliotecas

Todas sob a licença Apache 2.0:

- Kotlin, `kotlinx.serialization` e `kotlinx.coroutines` (JetBrains)
- AndroidX: Compose (UI, Foundation, Material 3), Activity, Lifecycle, Navigation 3, DataStore e Core
- Somente nos testes: Robolectric (MIT), Roborazzi (Apache 2.0) e JUnit (EPL 1.0)
