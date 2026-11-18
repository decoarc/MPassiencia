# Paciência Triplo: design da versão base (Android)

Data: 2026-11-17 · Status: aprovado

## 1. Por que este projeto existe

Uma pessoa da família do autor joga o Paciência Triplo num site de jogos, quase sempre no celular deitado
e às vezes no tablet. As propagandas do site ficaram cansativas. A **versão base** é um app
Android nativo, sem anúncios, que substitui o site para ela sem que ela sinta falta de nada.

Mais adiante, o app será publicado na Play Store e colocado no portfólio do autor, e talvez
ganhe monetização e recursos novos. Nada disso entra na base, mas as decisões de agora não
podem atrapalhar esse caminho. Um porte para iOS (Swift) está no `BACKLOG.md` e só começa
depois da base pronta.

Referência das regras e da tela: `docs/regras-de-referencia.md`.

## 2. Objetivos, não-objetivos e anti-objetivos

**Objetivos**
- Regras idênticas às do Paciência Triplo clássico, nos modos vira 1 e vira 3.
- Mesa no arranjo tradicional do jogo, para quem já joga se sentir em casa.
- Menu inicial com cara de jogo mobile, no estilo clássico de feltro verde.
- Jogar com toques (toque para mover) e também arrastando.
- Nunca perder a partida: salvamento automático a cada jogada.
- Funcionar offline, sem login, sem anúncios e sem coletar dados.
- Código organizado o bastante para servir de portfólio.

**Não-objetivos da base** (estão no `BACKLOG.md` ou ficam para depois)
- Modo com o celular em pé.
- Temas de baralho, versos e fundos alternativos.
- Estatísticas, conquistas, desafio diário e placares.
- Monetização, anúncios, compras e conta de usuário.
- Porte para iOS e Kotlin Multiplatform.
- Tradução para outros idiomas. Os textos ficam em `strings.xml` em pt-BR, prontos para
  serem traduzidos depois.

**Anti-objetivos** (o app falha se acontecer, mesmo funcionando)
- Ser mais incômodo que o site: cartas pequenas demais para tocar, animações lentas que
  atrasam o jogo, jogadas que "erram" o destino com frequência.
- Perder o progresso de uma partida ao fechar o app, receber uma ligação ou girar a tela.
- Usar arte, nome de marca ou elementos visuais de outros jogos ou sites. O app, o código e
  a documentação não citam nem têm relação pública com nenhum site de jogos.

## 3. Restrições

- Somente Android, nativo, em Kotlin com Jetpack Compose.
- Orientação travada em paisagem (`sensorLandscape`) no celular e no tablet.
- O motor do jogo não pode depender do Android, para virar Kotlin Multiplatform no porte para iOS.
- O desenvolvimento é feito no Windows.
- Toda arte e todo som precisam ser próprios ou ter licença livre compatível com publicação
  comercial (por exemplo, CC0). A licença de cada recurso vai registrada em `docs/CREDITS.md`.

## 4. Abordagens consideradas

| Abordagem | Vantagens | Desvantagens |
|---|---|---|
| **1. Kotlin + Compose, motor em módulo Kotlin puro** ✅ | Nativo, testável, ótimo para portfólio, motor já preparado para KMP | Quase nenhuma |
| 2. Kotlin Multiplatform desde o início | iOS já preparado | Montagem do projeto mais complexa agora; o iOS não compila no Windows; paga hoje por algo que só será usado depois |
| 3. Engine de jogos (Godot, Unity, libGDX) | Animação pronta | Exagero para um jogo de cartas, menus não nativos, porte para Swift vira reescrita total |

O Flutter e o React Native foram descartados porque o pedido é por um app nativo, e com eles
o porte para Swift perderia o sentido.

**Escolhida: abordagem 1**, pelo autor. Com o motor isolado, a conversão para KMP no porte
fica barata, então a abordagem 2 não traz ganho que justifique o custo.

## 5. Regras do jogo

- **Cartas:** 3 baralhos padrão, 156 cartas: 4 naipes × 13 valores × 3 cópias.
- **Distribuição:** 13 colunas com 1, 2, 3, …, 13 cartas, da esquerda para a direita
  (91 cartas). Só a última carta de cada coluna fica virada para cima. As 65 restantes formam
  o **monte** (estoque), virado para baixo.
- **Fundações:** 12 espaços. Cada fundação começa com um Ás e sobe até o Rei, no mesmo
  naipe. Qualquer Ás pode ir para qualquer fundação vazia, então cada naipe ocupa 3 fundações.
- **Colunas:** montadas em ordem decrescente com cores alternadas (vermelho sobre preto e
  vice-versa).
- **Mover:** uma carta sozinha ou um grupo de cartas viradas para cima, a partir de qualquer
  carta da coluna, desde que o grupo esteja em sequência válida. Não há limite de tamanho do grupo.
- **Coluna vazia:** só aceita um Rei, ou um grupo que comece com Rei.
- **Fundação para coluna:** a carta do topo de uma fundação pode voltar para uma coluna.
- **Carta virada para baixo exposta:** vira para cima automaticamente.
- **Monte:** tocar no monte vira 1 carta (modo vira 1) ou 3 cartas (modo vira 3) para o
  descarte. Quando o monte acaba, um toque nele devolve o descarte inteiro ao monte, na ordem
  original. Não há limite de passadas.
- **Descarte:** só a carta do topo pode ser jogada. No vira 3, as 3 cartas viradas aparecem
  em leque, mas só a de cima vale.
- **Vitória:** todas as 156 cartas nas fundações.
- **Modos:** vira 1 (médio) e vira 3 (difícil).

## 6. Decisões tomadas (com o autor)

### 6.1 Número do jogo (seed)
- Cada partida tem um número de 1 a 99999. O mesmo número e o mesmo modo geram sempre a
  mesma distribuição.
- O embaralhamento usa um **gerador de números aleatórios próprio, implementado no `core`**
  (por exemplo, PCG32 ou xorshift, com Fisher–Yates), em vez de `java.util.Random` ou
  `kotlin.random`. Assim, a mesma seed gera a mesma mesa no Android e no futuro app iOS. Um
  teste fixa a distribuição de algumas seeds para detectar mudanças sem querer.
- Os números são próprios do app e não têm compatibilidade com outros jogos.

### 6.2 Tela de jogo (opção A: barra de botões no topo)

```
┌──────────────────────────────────────────────────────────────┐
│ #220 · Pontos 340 · Mov. 41 · 03:12   ↶ Desfazer 💡 Dica ☰ Menu│
│ [monte][descarte] [F][F][F][F][F][F][F][F][F][F][F][F]         │
│  [c1] [c2] [c3] [c4] … [c13]                                  │
│   ⋮    ⋮    ⋮               ← colunas crescem para baixo      │
└──────────────────────────────────────────────────────────────┘
```

- Uma barra fina no topo mostra o número do jogo, os pontos, os movimentos e o tempo, e
  traz os botões Desfazer, Dica e Menu.
- Abaixo dela fica uma fileira com o monte, o descarte e as 12 fundações (14 posições).
- Abaixo, as 13 colunas.
- **Responsivo:** precisa ser jogável no maior número possível de celulares e tablets em
  paisagem. O aparelho de referência é o **Galaxy Note 10** (cerca de 869 × 411 dp
  deitado, tela alongada com furo de câmera). Também é testado num celular pequeno e mais
  largo (por volta de 640 × 360 dp) e num tablet.
- **Tamanho das cartas:** é o maior que permita as 14 posições da fileira de cima na
  largura e uma carta inteira mais algumas sobrepostas na altura. Num tablet, as cartas ficam
  maiores, até um limite, e a mesa fica centralizada.
- **Colunas longas** *(decisão do Claude)*: a sobreposição se ajusta a cada coluna, nesta
  ordem:
  1. Cartas viradas para cima mostram uma tira confortável (valor e naipe com folga).
  2. Se faltar altura, as cartas viradas para baixo se apertam até um mínimo (cerca de 4 dp cada).
  3. Depois, as viradas para cima se apertam até um mínimo que ainda mostra o canto com
     valor e naipe e dá para tocar (cerca de 18 dp, o equivalente a uns 5 mm).
  4. Se mesmo assim não couber (raro, só no fim de partidas difíceis), a área das colunas
     passa a rolar na vertical. A barra e a fileira de cima ficam fixas.
- O jogo usa tela cheia: as barras do sistema ficam escondidas e reaparecem com um gesto.
  Recortes de câmera e cantos arredondados são respeitados.

### 6.3 Interação
- **Toque para mover (padrão):** tocar numa carta virada para cima a envia ao melhor destino
  válido. Se for uma carta no meio da coluna, vai o grupo inteiro a partir dela. A
  prioridade é: fundação (se for uma carta só), depois uma coluna com cartas, depois uma
  coluna vazia (só para Rei). Se não houver destino válido, a carta dá uma leve tremida.
- **Arrastar:** também funciona. Ao soltar num destino inválido, a carta volta animada.
- **Monte:** um toque vira cartas. Com o monte vazio, um toque recicla o descarte.
- **Desfazer:** sem limite, até o início da partida. Desfaz também viradas do monte e cartas
  viradas automaticamente.
- **Dica:** destaca com contorno amarelo uma jogada útil (a origem e o destino). Tocar de
  novo mostra a próxima dica. Sem jogadas na mesa, a dica aponta o monte. Sem nenhuma
  jogada possível, mostra "Sem jogadas disponíveis".
- **Jogo travado** *(decisão do Claude)*: o `core` considera a partida travada quando
  nenhuma carta do monte e do descarte pode ser jogada, em nenhuma posição de uma passada
  completa, e também não há jogada útil na mesa. Uma jogada útil é a que muda o estado sem
  só desfazer a anterior. Nesse caso, a dica mostra "Sem jogadas disponíveis" com os botões
  Desfazer, Reiniciar este jogo e Novo Jogo. A verificação roda só quando o jogador pede
  dica, então o app nunca interrompe o jogo sozinho.
- **Fim automático:** quando o monte e o descarte estão vazios e todas as cartas das colunas
  estão viradas para cima, aparece o botão **"Terminar"**, que leva tudo para as fundações
  com animação.

### 6.4 Pontuação
Adaptada da pontuação clássica de Klondike:

| Ação | Pontos |
|---|---|
| Carta para a fundação | +10 |
| Carta virada para cima numa coluna (pela exposição) | +5 |
| Carta do descarte para uma coluna | +5 |
| Carta da fundação de volta para uma coluna | −15 |

- A pontuação nunca fica negativa (mínimo 0).
- Desfazer reverte os pontos da jogada desfeita.
- "Movimentos" conta cada jogada, inclusive virar o monte. Desfazer não soma movimento.

### 6.5 Tempo
- O cronômetro começa na primeira jogada e para quando o app vai para segundo plano ou o
  menu está aberto.
- Aparece na barra e na tela de vitória.

### 6.6 Tela de vitória
- Uma animação curta de cartas.
- Um painel com pontos, movimentos, tempo, número do jogo e modo.
- Os botões "Novo Jogo" e "Menu".

### 6.7 Menu inicial (estilo clássico de feltro verde, aprovado no rascunho)
- Fundo de feltro verde com degradê radial, um leque de cartas e o título.
- O botão principal é **▶ Continuar**, em amarelo, e só aparece se houver uma partida
  salva. Os botões secundários são **Novo Jogo**, **Como Jogar** e **Opções**, em creme.
- Atalhos para o som e as opções ficam no canto superior direito.
- **Novo Jogo** abre um diálogo com o número do jogo (já preenchido com um número aleatório
  e editável) e o modo (vira 1 ou vira 3), com os botões Cancelar e Jogar. Se houver partida
  em andamento, o app pede confirmação antes de descartá-la.
- **Como Jogar** é uma tela com as regras da seção 5 em linguagem simples.
- **Menu dentro da partida (☰):** Continuar, Novo Jogo, Reiniciar este jogo (mesmo número e
  modo), Opções, Como Jogar e Sair para o menu inicial.

### 6.8 Opções da base
- Modo padrão para novos jogos: vira 1 ou vira 3.
- Som ligado ou desligado.
- Velocidade das animações: normal, rápida ou desligada.

### 6.9 Cartas (arte própria e simples)
- Desenhadas no código (Compose Canvas ou vetores próprios), sem imagens de terceiros.
- **Face:** fundo branco, borda arredondada, valor e naipe grandes no canto superior
  esquerdo (legíveis em cartas pequenas) e um naipe grande no centro. As figuras (J, Q, K)
  levam a letra grande no centro, sem desenho de personagem. Vermelho para copas e ouros,
  preto para espadas e paus.
- **Verso:** um padrão próprio, vermelho, com moldura branca.
- **Espaço vazio:** contorno claro e translúcido sobre o feltro, com "A" nas fundações e "K"
  nas colunas, discretos.

### 6.10 Salvamento
- O estado completo da partida, incluindo o histórico para desfazer, é salvo depois de cada
  jogada e quando o app vai para segundo plano.
- A serialização é feita com `kotlinx.serialization` (multiplataforma), para o formato servir
  ao porte para iOS.
- As opções ficam no DataStore.
- Ao abrir o app com partida salva, ele vai para o menu inicial com "Continuar" à mostra.

### 6.11 Som
- Efeitos curtos: virar carta, colocar carta, carta na fundação e vitória.
- Usar arquivos CC0 ou gerados, com a licença registrada em `docs/CREDITS.md`. Sem música.

## 7. Arquitetura

```
MPassiencia/
├── core/                 módulo Kotlin/JVM puro (sem dependências de Android)
│   ├── model/            Card, Suit, Rank, Pile (Stock, Waste, Foundation, Tableau), GameState
│   ├── rules/            validação de jogadas e escolha do melhor destino (toque para mover)
│   ├── engine/           aplicar jogada → novo estado imutável, desfazer, fim automático, vitória
│   ├── hint/             lista de jogadas úteis em ordem de prioridade
│   ├── scoring/          pontos e movimentos
│   └── random/           PRNG próprio + embaralhamento por seed
└── app/                  Android (Compose)
    ├── ui/menu/          menu inicial e diálogo de Novo Jogo
    ├── ui/game/          mesa, cartas, animações, arrastar e toque, barra superior
    ├── ui/rules/         Como Jogar
    ├── ui/settings/      Opções
    ├── ui/win/           tela de vitória
    ├── data/             salvamento da partida e DataStore das opções
    └── audio/            efeitos sonoros
```

- O estado do jogo é **imutável**. Cada jogada produz um novo `GameState`, e o histórico
  serve de pilha para desfazer.
- Um `GameViewModel` no `app` guarda o estado atual, chama o `core` e dispara o
  salvamento e os sons. A interface só observa o estado.
- O `core` usa apenas Kotlin padrão e `kotlinx.serialization`, para a conversão em módulo
  KMP no porte ser mecânica.
- Navegação entre telas com Navigation 3 (menu → jogo → vitória; Opções e Como Jogar).

## 8. Testes

- **`core`:** testes unitários escritos antes do código (TDD) para as regras de cada pilha,
  movimentos em grupo, coluna vazia, monte nos dois modos e reciclagem, distribuição
  determinística por seed, desfazer, pontuação, dica, fim automático e vitória. Inclui uma
  partida completa simulada por um jogador automático simples em várias seeds, para checar
  que o motor nunca chega a um estado inválido. Não é preciso garantir que todo jogo tenha
  solução.
- **`app`:** testes de interface em Compose para o fluxo menu → novo jogo → jogada →
  desfazer → voltar ao menu → continuar. Um teste de salvamento e restauração. Screenshots da
  mesa em pelo menos um celular e um tablet, em paisagem.
- **Verificação manual:** rodar no emulador (celular e tablet) e, se possível, num aparelho real.

## 9. Nome do app (ainda em aberto)

- O nome comercial ainda não foi escolhido. O autor quer um nome bem comercial e vai
  verificar se já existe na loja antes de decidir.
- Na base, o app usa o nome provisório **"Paciência Triplo"**, guardado só em `strings.xml`
  (`app_name`) e no título do menu, para trocar num lugar só.
- O identificador do pacote também é provisório: `io.github.decarc.mpaciencia`. Ele fica
  num lugar só (`applicationId` no Gradle). Antes da **primeira publicação**, os dois
  precisam estar definidos, porque o identificador não muda depois de publicado. O
  `namespace` dos pacotes de código não depende do nome comercial.
- O item "definir nome e identificador antes de publicar" está no `BACKLOG.md`.

## 10. Fica a critério de quem constrói

- A versão mínima do Android (sugestão: API 26) e as versões das bibliotecas.
- O algoritmo exato do PRNG.
- Os critérios internos de prioridade da dica e do melhor destino, respeitando a seção 6.3.
- Os detalhes de animação (curvas e durações), desde que a velocidade "normal" seja ágil.
- As cores exatas, a tipografia e os ícones, dentro do estilo de feltro verde aprovado.
