# Backlog

## Melhorias (depois da base pronta)

- [ ] **Modo com o celular em pé.** 13 colunas estreitas em que cada carta mostra só o canto (valor e naipe em tamanho grande), jogadas com o toque para mover. A base é só com o celular deitado.
  - Descartados: duas fileiras de colunas (a de cima invade a de baixo quando as colunas crescem) e mesa que desliza para o lado (não dá para arrastar para uma coluna fora da tela).
- [ ] Monetização e recursos novos, a decidir depois da base pronta.

## Antes da primeira publicação

- [ ] **Escolher o nome comercial** e verificar se já existe na Play Store e se há marca registrada.
- [ ] **Definir o identificador do pacote** (`applicationId`), que não muda depois de publicado. O provisório é `io.github.decarc.mpaciencia`.

## Depois que a versão Android estiver finalizada

- [ ] **Porte para iOS (Swift).** Só começa quando o app Android estiver pronto.
  - O motor do jogo (regras, estado, pontuação) fica em um módulo Kotlin puro, sem dependências de Android, para ser reaproveitado via Kotlin Multiplatform ou servir de referência para reescrever em Swift.
  - Precisa de um Mac com Xcode para compilar para iOS.
