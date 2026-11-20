# Plantão Clima

Global Solution 2026/2 (CitizenTech) · FIAP · Engenharia de Software · 2ESOA

**Vídeo pitch (até 3 min):** [PREENCHER: link público do YouTube, Google Drive ou Vimeo]

## Problema

No Censo 2022, 5,6 milhões de pessoas de 60 anos ou mais moram sozinhas no Brasil (1,3 milhão em SP).
Em dias de calor extremo ou de chuva forte, a orientação oficial é "verifique os idosos que moram sozinhos".
O problema é que ninguém organiza quem liga para quem, quem cobre quando a ligação não é atendida e quem ficou sem ninguém.

## Solução

O Plantão Clima é uma rede de vizinhos, familiares, síndicos, igrejas e agentes de saúde que liga para idosos que moram sozinhos quando sai um alerta de calor ou de chuva.

- **O idoso não usa app.** Ele só atende o telefone (celular ou fixo).
- **Cada idoso tem 1 voluntário titular e 1 reserva**, que são pessoas que ele já conhece.
- **No dia do alerta, o aviso oficial do Inmet entra sozinho**: a cada 30 minutos o servidor consulta a API pública de avisos do Inmet. Se a cidade de uma região cadastrada está num aviso de chuva, tempestade ou calor, o sistema cria o alerta e gera a rodada de ligações. Aviso da Defesa Civil ou de outra fonte, o coordenador cadastra na mão. O sistema também sugere a temperatura pelo Open-Meteo.
- **Na chuva, o alerta pode chegar sozinho pelo FloodGuard** (nosso projeto anterior): o sensor de nível do córrego manda a leitura e, a partir de 70% da cota de transbordo, o sistema cria o alerta e já gera a rodada. Ninguém precisa digitar nada.
- **Cada voluntário vê as suas ligações no celular** e registra o resultado: *Está bem*, *Precisa de atenção*, *Precisa de ajuda* ou *Não atendeu*.
  - *Precisa de atenção* mostra um checklist de calor ou de chuva.
  - *Precisa de ajuda* mostra primeiro o botão do 192, ou do 193/199 na chuva.
- **Escalada automática:**
  - Se o titular não consegue falar com o idoso, a ligação passa para o reserva.
  - Se ninguém consegue, o idoso vira **crítico** e vai para o topo do painel, com a orientação de mandar alguém até a casa.
- **O risco depende do tipo de alerta:**
  - Calor: doença crônica, casa sem ventilação, mobilidade reduzida e idade de 80 anos ou mais.
  - Chuva: área de risco (peso 2), mobilidade reduzida e idade de 80 anos ou mais.
- **Funciona no papel:** dá para imprimir a lista e lançar os resultados depois.
- **LGPD:**
  - Só guardamos os dados mínimos, com consentimento da pessoa ou do responsável.
  - Excluir um idoso anonimiza o cadastro: nome, telefones, endereço e flags de saúde são apagados.
  - O relatório para a UBS e a Defesa Civil é só agregado, sem nomes.

### Braço FloodGuard

O FloodGuard monitorava córregos com sensores de nível de água e mostrava o mapa de risco de alagamento por região. Faltava o último passo: avisar quem não lê alerta no celular. No Plantão Clima ele vira a entrada automática do alerta de chuva:

| Leitura do sensor (% da cota de transbordo) | O que acontece |
|---|---|
| até 69% | Nada. Nível normal. |
| 70% a 89% | Cria o alerta de chuva no nível **Alerta** e gera a rodada de ligações. |
| 90% ou mais | Nível **Máximo**. Se o alerta do dia já existe, só sobe o nível, nunca baixa, e não duplica ligações. |

Rota `POST /api/sensor` (`idRegiao`, `percentual`), regra em `AlertaService.leituraSensor`. Na tela **Alertas**, o card "Sensor FloodGuard" simula a leitura para a demonstração.

### Dados reais e ao vivo (APIs públicas, sem chave)

| API | O que alimenta | Onde |
|---|---|---|
| Inmet, avisos ativos (`apiprevmet3.inmet.gov.br/avisos/ativos`) | Alerta de chuva/tempestade ou calor do dia, com nível pela cor oficial: amarelo = atenção, laranja = alerta, vermelho = máximo. A área do aviso aparece no mapa do Painel | `InmetService`, consulta automática no `App` a cada 30 min, `GET/POST /api/inmet`, tela Alertas |
| Open-Meteo (`api.open-meteo.com`) | Sugestão da temperatura máxima do dia; no Painel, máxima e chuva previstas para hoje em cada região e no mapa (grade de 81 pontos) | `PrevisaoService`, `GET /api/previsao`; Painel do dia (direto no navegador) |
| Mapa base Esri World Light Gray (dados OpenStreetMap e outros) | Fundo do mapa do Painel | Leaflet 1.9.4 local em `web/lib/leaflet` |
| BrasilAPI, CEP (`brasilapi.com.br/api/cep/v2`) | Rua e bairro no cadastro do idoso, a partir do CEP | Tela Idosos (direto no navegador) |

Sem internet, o sistema continua funcionando: a consulta falha com aviso no log e o coordenador cadastra o alerta à mão. O mapa fica sem fundo e sem previsão, mas os pinos das regiões e os cartões continuam. A leitura do JSON do Inmet é testada com uma amostra offline na `Verificacao`.

### Adoção e divulgação em escala

O sistema não pede que ninguém mude de hábito: o idoso só atende o telefone, o voluntário aperta um botão por ligação e a coordenação usa redes que já existem. A página **Como funciona** (`web/como-funciona.html`) mostra isso para quem vai adotar e tem um cartaz imprimível para recrutar vizinhos.

- **Onde cadastrar:** agentes comunitários de saúde (já visitam os idosos em casa), CRAS, igrejas e associações, síndicos e zeladores.
- **Como o aviso chega:** Inmet, Defesa Civil (o SMS 40199 já chega ao voluntário), Open-Meteo e o sensor FloodGuard.
- **Plano em 3 passos:**
  1. Piloto num bairro: 1 UBS, 1 igreja e 2 prédios, com cerca de 50 idosos, num verão. Meta: todos com notícia em até 2 h depois do aviso.
  2. A cidade adota, com Secretaria de Saúde e Defesa Civil na coordenação.
  3. Rede nacional: código aberto, cada município roda o seu servidor.
- **Como medir:** o relatório R1 dá o % de idosos com contato em até 4 h e os críticos, e o R2 mostra os furos na rede antes do próximo alerta.

### ODS

- **ODS 3** (saúde e bem-estar): evitar mortes por calor e por enchente entre idosos.
- **ODS 10** (redução das desigualdades): quem mora sozinho e não usa smartphone também recebe o aviso.
- **ODS 11** (cidades sustentáveis): rede comunitária de resposta a desastres.
- **ODS 13** (ação climática): adaptação a eventos extremos.

### Design e acessibilidade

- **Painel de controle:** o Painel do dia mostra todos os alertas de uma vez, sem seletor. Um cartão por região, do mais grave para o mais leve, com o nível, quantas pessoas já deram notícia e a previsão do dia. Ao lado, um mapa com os pinos das regiões (número = pessoas sem notícia), a área oficial do aviso do Inmet e as camadas de temperatura máxima e de chuva previstas para hoje. Clicar num cartão ou num pino abre a rodada da região. A lista de cartões tem as mesmas informações do mapa, para quem usa leitor de tela.
- Referências do painel: Painel de Alertas do Cemaden, CGE-SP, MeteoAlarm e NWS (mapa e lista sincronizados, contagem por nível, o alerta mais grave de cada área em destaque).
- Cores do painel na escala de avisos do Inmet: amarelo, laranja e vermelho. O vermelho fica só para o que é crítico.
- Todos os pares de cor passam no contraste AA do WCAG.
- Fonte Atkinson Hyperlegible Next (Braille Institute), feita para baixa visão, servida localmente.
- Botões com no mínimo 44 px (56 px no "Ligar agora"), foco visível pelo teclado e respeito ao "reduzir movimento" do sistema.
- A **fita da rodada** mostra num olhar quem já deu notícia, com um segmento por idoso. O **protocolo** mostra em qual passo da escalada a rodada está.
- O topo indica se o servidor Java + H2 está no ar.
- Barra de acessibilidade em todas as páginas: aumentar e diminuir a letra (até 137,5%) e alto contraste, com a escolha salva no navegador. Link "Pular para o conteúdo" para quem navega pelo teclado.
- Daltonismo: nenhum estado depende só da cor. Na fita e na legenda cada estado tem também textura e símbolo (✓ está bem, ! atenção, !! ajuda, × ninguém atendeu, ↻ com o reserva), e os gráficos trazem o número escrito.
- O painel se atualiza sozinho a cada minuto, sem tirar o foco de quem está usando um botão.

**Pivô:** começamos com um tema de finanças. A validação mostrou que app para o público vulnerável não pega, então mudamos para uma solução em que o idoso só precisa atender o telefone. A primeira versão era só de calor (Plantão Calor); depois incluímos chuva para cobrir o outro evento extremo mais comum.

## Como rodar

Requisito: Java 8 ou mais novo. O banco H2 já está em `lib/` e é criado sozinho na primeira execução, a partir do `sql/script.sql`.

Todos os comandos devem ser executados **na pasta do projeto**.

**Opção 1: terminal, sem Maven** (classes já compiladas em `target/classes`):

```bash
java -cp "target/classes:lib/*" br.com.fiap.plantao.App
# Windows: java -cp "target/classes;lib/*" br.com.fiap.plantao.App
```

**Opção 2: Maven ou IntelliJ** (abrir o `pom.xml`):

```bash
mvn compile exec:java
```

Depois abra **http://localhost:8080**. Também dá para abrir o `web/index.html` direto no navegador com o servidor ligado.
O terminal mostra o endereço para acessar pelo celular na mesma rede Wi-Fi.

Para outra porta, use `-Dporta=9090`. Para apagar o banco e recriar com os dados de exemplo, apague a pasta `data/`.

**Oracle (opcional):**

1. Rode `sql/script.sql` no Oracle SQL Developer.
2. Adicione o driver `ojdbc` no classpath.
3. Inicie a aplicação com `-Ddb.url=jdbc:oracle:thin:@host:1521/servico -Ddb.user=RM -Ddb.pass=senha`.

**Testes das regras** (39 verificações num banco em memória):

```bash
java -cp "target/classes:lib/*" br.com.fiap.plantao.Verificacao
```

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `sql/script.sql` | DDL das 6 tabelas, constraints, dados de exemplo e as consultas dos relatórios |
| `src/main/java/br/com/fiap/plantao/model` | Entidades. `Pessoa` (abstrata) é a base de `Idoso` e `Voluntario`, cada uma com o próprio `validar()`. `Idoso` implementa a interface `AvaliavelRisco` |
| `.../dao` | JDBC com `PreparedStatement`. A interface `Dao<T>` define o CRUD; a escalada roda em transação (commit/rollback) |
| `.../service` | Regras: gerar a rodada, registrar a ligação, escalar para o reserva ou crítico, sugestão do Open-Meteo, avisos do Inmet ao vivo (`InmetService`), leitura do sensor FloodGuard |
| `.../http` | Servidor HTTP (JDK, sem framework) com a API JSON e os arquivos de `web/` |
| `.../exception` | `RegraNegocioException` (erro do usuário, HTTP 400), `RecursoNaoEncontradoException` (registro inexistente, HTTP 404) e `PersistenciaException` (erro do sistema, HTTP 500) |
| `web/` | 6 páginas com Bootstrap e Leaflet locais: Painel do dia, Idosos, Voluntários e vínculos, Alertas e regiões, Relatórios, Como funciona |
| `docs/` | DER e UML (PNG, mais o fonte Mermaid `.mmd`) e o roteiro do pitch |

**Banco:**

- Tabelas: `regiao`, `idoso`, `voluntario`, `vinculo_cuidado`, `alerta` e `checagem`.
- O relacionamento N:N é idoso ↔ voluntário, pela tabela `vinculo_cuidado`. Ela tem PK composta e papel TITULAR/RESERVA único por idoso.

**Relatórios:**

- R1: cobertura do alerta por região, com % de checados em até 4 h e críticos.
- R2: furos na rede, ou seja, idosos com menos de 2 voluntários.
- R3: carga por voluntário.

## Fontes

- Censo 2022 (IBGE): pessoas de 60+ que moram sozinhas.
- SES-SP, Alerta Nº 01/2025: orientações e sinais de alerta do calor, usados no checklist.
- Inmet, API de avisos ativos (apiprevmet3.inmet.gov.br/avisos/ativos): fonte principal do alerta, consultada automaticamente.
- Defesa Civil Alerta: aviso cadastrado pelo coordenador.
- BrasilAPI (brasilapi.com.br): busca de endereço pelo CEP.
- Evidências da página "Como funciona": Semenza et al., NEJM 1996 (Chicago 1995); Naughton et al., AJPM 2002 (Chicago 1999); Monteiro dos Santos et al., PLOS ONE 2024 (48 mil mortes por calor no Brasil); Cetic.br, TIC Domicílios 2024; IBGE, pesquisa PEERS sobre as enchentes do RS; registro de vulneráveis da França (lei de 30/06/2004) e avaliação da Santé publique France.
- Open-Meteo: previsão da máxima do dia (sugestão no cadastro do alerta) e grade de máxima e chuva do mapa do Painel.
- Leaflet 1.9.4 (licença BSD-2) e mapa base Esri World Light Gray Canvas (Esri, HERE, Garmin, OpenStreetMap).
- Inmet, escala de cores dos avisos (amarelo, laranja, vermelho): base das cores do painel.
- WCAG 2.2 (W3C) e gov.br Design System: alvos de toque, contraste e foco.
- Atkinson Hyperlegible Next (Braille Institute, licença OFL).
- FloodGuard: projeto anterior do grupo, base do braço de sensores.
- A régua 30/32/35/37 °C foi definida pelo grupo, inspirada em Campinas e no Rio. Não é protocolo oficial.

## Uso de IA generativa

O grupo usou IA generativa (Claude, da Anthropic) como apoio na pesquisa, na revisão da proposta e na escrita de parte do código e dos diagramas.
Todo o conteúdo foi revisado, testado e é de responsabilidade do grupo.
