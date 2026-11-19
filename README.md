# Plantão Clima — CitizenTech (FIAP Global Solution 2026/2 · 2ESOA)

Sistema comunitário de coordenação de ligações para proteger **idosos que moram sozinhos** durante **ondas de calor extremo** e **chuvas fortes** agravadas pelo El Niño.

> **Aviso de responsabilidade:** O Plantão Clima complementa a orientação oficial da Defesa Civil e das UBS e **não substitui** os serviços públicos de emergência (**SAMU 192**, **Bombeiros 193** e **Defesa Civil 199**).

---

## 1. O Problema e o Cidadão Beneficiado

- **5,6 milhões de pessoas com 60 anos ou mais moram sozinhas no Brasil** (Censo 2022 / IBGE).
- Ondas de calor extremo causaram cerca de **48 mil mortes em excesso** em 14 regiões metropolitanas brasileiras entre 2000 e 2018 (PLOS ONE), e chuvas intensas representam a maior parte dos alertas da Defesa Civil.
- A orientação oficial já recomenda *"verificar os idosos que moram sozinhos"*, mas ninguém organiza **quem liga para quem**, **quem cobre quando ninguém atende** e **quem ficou sem ser checado**.
- **Quem é protegido:** o idoso que mora sozinho (especialmente com doença crônica, mobilidade reduzida, casa sem ventilação ou residência em área de risco). **Ele não usa aplicativo nenhum — apenas atende o telefone.**
- **Quem usa o sistema:** o coordenador comunitário (síndico, igreja, associação de bairro ou agente de saúde) e os voluntários que o idoso já conhece (vizinhos e familiares).

### Evolução a partir do FloodGuard
Este projeto incorpora e evolui o conceito de **monitoramento territorial por regiões** desenvolvido pela nossa equipe no projeto anterior **FloodGuard** (agora integrado como o *Mapa Territorial de Risco* no Painel do Plantão Clima), ampliando o foco para **ondas de calor e chuvas intensas** com foco direto na proteção ativa de idosos isolados.

---

## 2. Como o Sistema Funciona

1. **Cadastro mínimo e consentimento (LGPD):** o coordenador cadastra os idosos (guardando apenas o **ano de nascimento**, sem data completa nem CPF, com consentimento registrado), os voluntários de confiança e o **vínculo de cuidado** de cada idoso com um voluntário **Titular** e um **Reserva**.
2. **Registro de alerta climático:** ao sair um alerta de **CALOR** ou **CHUVA** (INMET / Defesa Civil / CEMADEN), o coordenador registra o alerta no sistema.
3. **Fila de ligações priorizada por risco:** o backend Java calcula automaticamente a pontuação de risco (0 a 100):
   - No **CALOR**, pesam mais *doença crônica* (+35) e *casa sem ventilação* (+30).
   - Na **CHUVA**, pesam mais *área de risco* (+35) e *mobilidade reduzida* (+30).
   - A faixa etária estimada soma até +25 pontos.
4. **Registro da ligação pelo celular:** o voluntário liga e escolhe:
   - **Está bem**
   - **Precisa de atenção** (abre checklist interativo específico de Calor ou de Chuva)
   - **Precisa de ajuda** (exibe imediatamente os números 192, 193 e 199)
   - **Não atendeu**
5. **Escalonamento automático:**
   - Se o **Titular** marca *Não atendeu*, a ligação passa automaticamente para o **Reserva**.
   - Se o **Reserva** também marca *Não atendeu* (ou se não houver reserva cadastrado), o idoso sobe como **CRÍTICO** no topo do Painel, orientando enviar alguém até a residência e acionar **193, 192 ou 199**.
6. **Inclusão e operação offline:** a lista da rodada possui layout otimizado de impressão (`Imprimir Lista Offline`) para comunidades sem internet estável.
7. **Privacidade para UBS e Defesa Civil:** relatórios externos saem **agregados por região e 100% anonimizados**, sem nomes pessoais.

---

## 3. Arquitetura e Tecnologias (Conteúdo até a Fase 7)

- **Linguagem:** Java 17 (POO pura, sem frameworks externos)
- **Persistência:** JDBC puro + **H2 Database 2.3 em modo Oracle** (`MODE=Oracle`), driver incluído em `lib/h2-2.3.232.jar`
- **Camadas separadas por pacote:**
  - `br.com.plantaoclima.model`: superclasse abstrata `Pessoa`, interface `AvaliavelRisco`, entidades `Regiao`, `Idoso`, `Voluntario`, `VinculoCuidado`, `Alerta`, `Checagem`, enums e DTOs de relatórios.
  - `br.com.plantaoclima.exception`: `ValidacaoException`, `RecursoNaoEncontradoException`, `PersistenciaException`.
  - `br.com.plantaoclima.dao`: `DatabaseConfig`, interface `CrudDao<T>`, 6 DAOs com CRUD completo via `PreparedStatement` + `RelatorioDao` com consultas SQL (`JOIN`, `LEFT JOIN`, `GROUP BY`, `HAVING`).
  - `br.com.plantaoclima.service`: regras de negócio, validações LGPD, geração de rodada priorizada e motor de escalonamento automático.
  - `br.com.plantaoclima.http`: `PlantaoClimaServer` (JDK `HttpServer`), `ApiHandler` (API REST JSON conectando a web diretamente ao banco H2), `StaticFileHandler` e `JsonUtil`.
  - `br.com.plantaoclima.cli`: `ConsoleApp` opcional para inspeção via terminal (`--cli`).
- **Banco de Dados (6 tabelas):** `REGIAO`, `IDOSO`, `VOLUNTARIO`, `VINCULO_CUIDADO` (associativa N:N), `ALERTA` e `CHECAGEM`.
- **Interface Web (5 telas responsivas em HTML5 semântico + Bootstrap 5 + JS):**
  1. `web/index.html` — **Painel** (Casos Críticos no topo, KPIs, Alertas Ativos e Mapa Territorial FloodGuard)
  2. `web/idosos.html` — **Idosos** (CRUD completo com validação em JS e consentimento LGPD)
  3. `web/voluntarios.html` — **Voluntários e Vínculos** (CRUD de voluntários + vínculos N:N Titular/Reserva)
  4. `web/alertas.html` — **Alertas e Rodada** (CRUD de alertas, fila ordenada por risco, checklists, escalonamento e impressão offline)
  5. `web/relatorios.html` — **Relatórios** (Consultas SQL ao vivo + relatório anonimizado para UBS/Defesa Civil)

---

## 4. Como Compilar e Executar

### Pré-requisitos
- **JDK 17 ou superior** (`java` e `javac`).
- Nenhuma instalação de banco de dados externa é necessária — o driver H2 já está incluso em `lib/h2-2.3.232.jar` e cria/popula o banco automaticamente a partir de `sql/plantao_clima.sql` na primeira execução.

### Opção A — Execução rápida (Windows)
Abra o terminal dentro da pasta `plantao-clima` e execute:
```bat
run.bat
```

### Opção B — Execução rápida (Linux / macOS)
```bash
chmod +x run.sh
./run.sh
```

### Opção C — Compilação e execução manual (qualquer sistema)
1. **Compilar:**
   ```bash
   mkdir -p build/classes
   javac -encoding UTF-8 -cp "lib/h2-2.3.232.jar" -d build/classes src/main/java/br/com/plantaoclima/*.java src/main/java/br/com/plantaoclima/*/*.java
   ```
   *(No Windows PowerShell, use `Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object { '"' + $_.FullName + '"' } | Set-Content -Encoding ASCII build\sources.txt` e depois `javac -encoding UTF-8 -cp "lib\h2-2.3.232.jar" -d build\classes @build\sources.txt`.)*

2. **Executar o servidor Java + H2 + Interface Web:**
   - **Windows:**
     ```bat
     java -cp "build\classes;lib\h2-2.3.232.jar" br.com.plantaoclima.Main
     ```
   - **Linux / macOS:**
     ```bash
     java -cp "build/classes:lib/h2-2.3.232.jar" br.com.plantaoclima.Main
     ```

3. **Abrir no navegador:**
   Acesse **[http://localhost:8080](http://localhost:8080)**

   - *Modo console opcional:* execute adicionando `--cli` ao final do comando `java`.
   - *Abertura direta do HTML:* também é possível abrir `web/index.html` diretamente no navegador (se o servidor Java estiver rodando em `localhost:8080`, a página conecta automaticamente ao banco H2; se estiver desligado, ativa um modo demonstração local com aviso no topo).

### Executar o script SQL isoladamente (verificação do banco)
```bash
java -cp "lib/h2-2.3.232.jar" org.h2.tools.RunScript -url "jdbc:h2:mem:teste;MODE=Oracle;DATABASE_TO_UPPER=TRUE" -user sa -script "sql/plantao_clima.sql"
```

---

## 5. Relatórios SQL Implementados

Todos os relatórios estão disponíveis na tela **Relatórios** (`/relatorios.html`) e no endpoint `GET /api/relatorios`:

1. **Percentual de idosos checados em até 4 horas** (`ALERTA LEFT JOIN CHECAGEM` com cálculo temporal `INTERVAL '4' HOUR`).
2. **Idosos com rede incompleta** (`IDOSO LEFT JOIN VINCULO_CUIDADO GROUP BY ... HAVING` identificando falta de Titular e/ou Reserva).
3. **Taxa de "não atendeu" por voluntário** (`VOLUNTARIO LEFT JOIN CHECAGEM` com agregação condicional `SUM(CASE WHEN ...)`).
4. **Relatório agregado e anonimizado por região para UBS e Defesa Civil** (sem nomes ou telefones individuais).

---

## 6. Diagramas e Documentos da Entrega

- **Script SQL completo (DDL + Carga Inicial):** [`sql/plantao_clima.sql`](sql/plantao_clima.sql)
- **Diagrama Entidade-Relacionamento (DER):** [`docs/diagrama-er.pdf`](docs/diagrama-er.pdf) e [`docs/diagrama-er.png`](docs/diagrama-er.png)
- **Diagrama de Classes (UML):** [`docs/diagrama-classes-uml.pdf`](docs/diagrama-classes-uml.pdf) e [`docs/diagrama-classes-uml.png`](docs/diagrama-classes-uml.png)
- **Dados do grupo e link do vídeo pitch:** [`entrega-grupo.txt`](entrega-grupo.txt)
- **Roteiro de apoio do Vídeo Pitch (3 min):** [`docs/roteiro-pitch-3min.md`](docs/roteiro-pitch-3min.md)

---

## 7. Conexão com os ODS da ONU

- **ODS 3 — Saúde e Bem-Estar:** reduz mortes evitáveis e agravamento de doenças crônicas em idosos durante ondas de calor extremo e enchentes por meio de acompanhamento preventivo rápido.
- **ODS 11 — Cidades e Comunidades Sustentáveis:** fortalece redes comunitárias resilientes e inclusivas que funcionam mesmo em cenários de baixa conectividade e sem exigir alfabetização digital do idoso.
- **ODS 13 — Ação contra a Mudança Global do Clima:** transforma alertas meteorológicos (INMET/Defesa Civil) em ações locais concretas de adaptação climática.

---

## 8. Vídeo Pitch

- **Link público do Vídeo Pitch (até 3 minutos):** `[PREENCHER_LINK_DO_VIDEO_PITCH]`
  *(Lembre-se de testar o link em aba anônima e colocá-lo também em `entrega-grupo.txt`.)*

---

## 9. Declaração de Uso de IA Generativa

Em conformidade com as diretrizes da Global Solution FIAP 2026/2:
- **Ferramentas utilizadas:** Assistente de IA (Google Gemini / Antigravity).
- **Finalidade:** Apoio na estruturação inicial das camadas Java + JDBC + H2, revisão das consultas SQL em modo Oracle, criação do layout responsivo com suporte a impressão offline e revisão de documentação e diagramas.
- **Revisão pela equipe:** Todo o código-fonte, regras de negócio (cálculo de risco Calor/Chuva e escalonamento Titular → Reserva → Crítico), scripts SQL e interface web foram revisados, executados e validados pela equipe antes da entrega.
