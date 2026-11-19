# Roteiro do Vídeo Pitch — Plantão Clima (Máximo 3 minutos)

> **Dicas da banca (Slide 24 e 25):** Não passe de 3:00! Nada de slides estáticos ou leitura mecânica — mostre o **sistema rodando de verdade em `http://localhost:8080`**, gravando e lendo do banco H2!

---

### `0:00 – 0:30` · O Problema e o Cidadão (30s)
- **Tela:** Mostrar o Painel inicial (`index.html`) ou começar com câmera + tela do Painel.
- **Fala sugerida:**
  > "No Brasil, **5,6 milhões de pessoas com 60 anos ou mais moram sozinhas**, segundo o Censo 2022. Com o **El Niño** intensificando ondas de calor no país todo e chuvas fortes no Sul, a orientação oficial já diz: *'verifique os idosos que moram sozinhos'*. Mas na prática, ninguém organiza quem liga para quem, quem cobre quando ninguém atende e quem ficou sem resposta."

### `0:30 – 0:50` · A Solução: Plantão Clima (20s)
- **Tela:** Mostrar rapidamente o topo do Painel e o Mapa Territorial de Risco (módulo herdado do FloodGuard).
- **Fala sugerida:**
  > "Por isso criamos o **Plantão Clima**, evoluindo o monitoramento territorial do nosso projeto FloodGuard. O diferencial é simples e inclusivo: **o idoso não usa app nenhum — só atende o telefone**. Síndicos, igrejas, associações de bairro e agentes de saúde cadastram os idosos com dados mínimos e vinculam voluntários de confiança — um **Titular** e um **Reserva**."

### `0:50 – 2:10` · Demonstração Real com Banco H2 (1min 20s)
*(Mostrar clicando de verdade no navegador com o indicador verde **"Java + H2 Online"** visível!)*
1. **Tela `Idosos` (15s):** Cadastrar um novo idoso marcando doença crônica + casa sem ventilação + consentimento LGPD (apenas ano de nascimento) -> salvar -> mostrar ele aparecendo na lista com a pontuação de risco calculada pelo Java e persistida no banco H2!
2. **Tela `Voluntários e Vínculos` (15s):** Vincular um Titular e um Reserva para esse idoso (mostrar a tabela associativa N:N `VINCULO_CUIDADO` atualizando a cobertura de *Incompleta* para *Completa*).
3. **Tela `Alertas e Rodada` (35s):**
   - Emitir um alerta de **CALOR** ou **CHUVA** (mostrar a rodada gerada automaticamente ordenada por risco).
   - Clicar em **"Precisa de atenção"** -> mostrar o checklist específico de calor/chuva.
   - Clicar em **"Não atendeu"** no Titular -> mostrar o aviso de **escalonamento automático para o Reserva**!
   - Clicar em **"Não atendeu"** no Reserva -> voltar ao **Painel** e mostrar o idoso aparecendo no banner **CRÍTICO** no topo com os botões 192 / 193 / 199!
   - Mencionar rapidamente o botão **"Imprimir Lista Offline"** para locais sem internet.
4. **Tela `Relatórios` (15s):** Mostrar as 4 consultas SQL ao vivo: % checados em até 4 horas, idosos com rede incompleta, taxa de "não atendeu" por voluntário e o **relatório agregado sem nomes para UBS e Defesa Civil**.

### `2:10 – 2:40` · Impacto e ODS (30s)
- **Tela:** Seção de Relatórios / Impacto e ODS.
- **Fala sugerida:**
  > "Ondas de calor causaram **48 mil mortes em excesso** em 14 regiões metropolitanas brasileiras entre 2000 e 2018. O Plantão Clima mede seu próprio impacto em tempo real — cobertura em até 4 horas, casos críticos e furos na rede — conectando-se diretamente aos **ODS 3 (Saúde)**, **ODS 11 (Cidades Resilientes)** e **ODS 13 (Ação Climática)**, sempre complementando o 192, o 193 e o 199."

### `2:40 – 3:00` · Fechamento (20s)
- **Tela:** Painel principal do Plantão Clima.
- **Fala sugerida:**
  > "Construído em Java 17 com JDBC, H2 em modo Oracle e interface web responsiva ligada ao backend real, o Plantão Clima prova que a melhor CitizenTech não exige que o cidadão vulnerável aprenda um app novo — ela organiza a comunidade ao redor dele para salvar vidas. Obrigado!"
