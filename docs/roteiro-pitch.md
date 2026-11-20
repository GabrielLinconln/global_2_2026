# Roteiro do pitch (3 minutos)

Antes de gravar: apague a pasta `data/` e inicie o sistema, com internet. O terminal mostra `Inmet ...: N alerta(s)` quando a consulta automática terminou. Abra http://localhost:8080 e confira se o topo mostra "Servidor Java + H2 no ar" e se o mapa do Painel do dia carregou. Deixe aberta outra aba com o cadastro de idoso e um CEP já digitado.

| Tempo | Tela | Fala |
|---|---|---|
| 0:00 a 0:20 | Como funciona, gráfico "O risco é conhecido" | "Na onda de calor de Chicago, quem não saía de casa teve quase 7 vezes mais risco de morrer. No Brasil, o calor matou 48 mil pessoas em 14 metrópoles em 18 anos, e 5,6 milhões de idosos moram sozinhos. O que protege não é tecnologia: é alguém que liga e pergunta como você está." |
| 0:20 a 0:40 | Como funciona, Quem faz o quê | "O Plantão Clima organiza essa ligação sem dar trabalho novo a ninguém. O idoso não instala nada, só atende o telefone, inclusive o fixo. Quem liga é um vizinho que ele já conhece, com titular e reserva." |
| 0:40 a 1:05 | Painel do dia, mapa em "Avisos" | "Isto é dado real de hoje. O sistema consulta o Inmet a cada 30 minutos. A área no mapa é o aviso oficial que cobre São Paulo agora: os alertas e as rodadas de ligações foram criados sozinhos. Cada cartão mostra quantas pessoas da região já deram notícia." Clicar em "Temperatura": "E esta é a máxima prevista para hoje, ponto a ponto, pela Open-Meteo." |
| 1:05 a 1:20 | Alertas, Sensor FloodGuard | "E na rua, o sensor do FloodGuard, nosso projeto anterior: o córrego do Taboão chegou a 92%." Digitar 92, enviar, clicar em "Abrir no painel". "O Taboão subiu para o nível máximo e foi para o topo da lista." |
| 1:20 a 1:55 | Painel do dia, rodada do Taboão | Clicar no cartão do Taboão. "Faixa vermelha, nas cores do Inmet. A fita mostra quem já deu notícia, com símbolo e textura, não só cor, por causa do daltonismo." Clicar em "Não atendeu" num cartão. "Não atendeu: passa para o reserva. Se o reserva também não consegue, vira caso crítico no topo, com 192 e 193 a um toque, e alguém vai até a casa." Ligar o Alto contraste e desligar em seguida. |
| 1:55 a 2:10 | Idosos (CEP já digitado), Voluntários | "Java com JDBC, CRUD completo, validação no navegador e no servidor. O CEP preenche a rua pela BrasilAPI. Cadastro com consentimento, e excluir anonimiza, por causa da LGPD." |
| 2:10 a 2:25 | Relatórios | "Para a UBS e a Defesa Civil: quem teve notícia em até 4 horas, onde falta reserva e qual voluntário está acima do limite. SQL com JOIN e GROUP BY, sem nomes." |
| 2:25 a 2:45 | Como funciona, linha do tempo e três passos | "A França liga para idosos isolados desde 2004, mas eles raramente se inscrevem sozinhos. Aqui quem cadastra é o agente de saúde, que já visita essas casas, o CRAS, a igreja e o síndico. Piloto num bairro, depois a cidade, depois o país. Plantão Clima: nenhum idoso fica sem uma ligação." |

Se o Inmet não tiver aviso para São Paulo no dia da gravação, o mapa mostra só os pinos das regiões e a nota embaixo dele diz que não há aviso sobre elas. Nesse caso fale "hoje não tem aviso aqui, então o sistema não incomoda ninguém", mostre a camada Temperatura e siga para o sensor, que cria o alerta do zero.

Perguntas da banca: as respostas curtas estão em Como funciona, "Perguntas que sempre aparecem" (por que não aplicativo, o que acrescenta à Defesa Civil, se o voluntário falhar, LGPD, custo, como medir).

Dicas:
- Grave a tela em 1280 px ou mais e mostre o celular por alguns segundos no painel. O terminal mostra o endereço para abrir na mesma rede Wi-Fi.
- O roteiro fecha em 2:45, com 15 segundos de folga. Se o tempo apertar, corte a parte de Idosos e Voluntários, que repete o que a banca já vê no código.
