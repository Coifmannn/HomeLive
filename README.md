# HomeLive

Trabalho semestral de Programação Orientada a Objetos (Análise e Desenvolvimento de Sistemas).

O HomeLive é um sistema pra gerenciar uma casa compartilhada (co-living / república). Ele resolve três problemas que toda casa com várias pessoas tem:

- dividir as contas (luz, água, internet...) de um jeito justo;
- organizar quem faz cada tarefa da casa, com rodízio;
- reservar os espaços comuns (lavanderia, área gourmet...) sem dois moradores marcarem o mesmo horário.

Pra deixar mais divertido, tem um sistema de pontos: quem cumpre as tarefas ganha pontos e quem atrasa perde.

O back-end é em Java 21 com Spring Boot. O front (HTML, CSS e JS puro) fica na pasta `frontend/` e é servido pela própria API, então é um endereço só.

## Tecnologias

- Java 21
- Spring Boot 4 (Web, Data JPA, Validation, Security)
- PostgreSQL (perfil `postgres`) ou H2 em memória (padrão e nos testes)
- Lombok
- JUnit 5 e MockMvc nos testes
- Login com token JWT (também aceita HTTP Basic)
- Front em HTML/CSS/JS, instalável no celular (PWA)

## Onde aparece a orientação a objetos

Essa parte é a que mais importa pro trabalho, então deixei explicado aqui.

### Herança e polimorfismo

`Usuario` é uma classe abstrata com o que todo mundo tem (nome, e-mail, senha com hash, telefone...). Ela tem o método abstrato `getTipoUsuario()`, e `Morador` e `Sindico` herdam dela e implementam do jeito deles. O Spring Security usa esse retorno pra saber o papel da pessoa.

- `Morador`: número do quarto, dias de estadia e pontos de convivência.
- `Sindico`: aprova ou rejeita despesas, cadastra espaços, cria tarefas.

### Encapsulamento

As entidades não têm setters. O estado só muda por métodos que fazem sentido pro negócio e que recusam mudanças inválidas, por exemplo `Despesa.aprovar()`, `TarefaDomestica.concluir()` e `AgendamentoEspaco.cancelar()`. Tem um teste que falha se alguém colocar um setter público.

A despesa segue um ciclo: `PENDENTE_APROVACAO` vai pra `APROVADA` e depois `RATEADA` (ou vai pra `REJEITADA`). Uma despesa que já foi avaliada não pode ser avaliada de novo.

### Strategy + Template Method (rateio das despesas)

Cada despesa pode ser dividida de um jeito diferente, então o cálculo ficou separado da entidade:

- `CalculoRateioStrategy` é a interface.
- `RateioProporcionalStrategy` é a classe abstrata com o passo a passo do cálculo (validar, somar os pesos, repartir e ajustar os centavos no último morador). O método `calcular()` é `final`; as filhas só dizem qual é o peso de cada morador.
- `RateioIgualitarioStrategy`: todo mundo paga igual.
- `RateioPorEstadiaStrategy`: o peso é a quantidade de dias que a pessoa ficou na casa.
- `RateioPorConsumoStrategy`: o peso é o consumo de energia (kWh) daquele mês.
- `RateioStrategyFactory` escolhe a estratégia certa a partir do enum `TipoRateio`.

As estratégias não acessam o banco. O service monta um `ContextoRateio` com os dados que faltam (as leituras de consumo) e entrega pronto.

Cada despesa tem uma `competencia` (o mês da conta). Uma conta de setembro lançada em outubro é dividida pelas leituras de setembro, não pelas de outubro.

### Rodízio das tarefas

`TarefaRecorrente` é o modelo de uma tarefa que se repete (ex.: tirar o lixo toda semana). Quando chega a data, ela gera uma `TarefaDomestica` de verdade. Quem pega a tarefa é decidido por outra Strategy (`RodizioStrategy`, também com Template Method e Factory):

- `SEQUENCIAL`: vai passando de morador em morador, em ordem.
- `MENOR_PONTUACAO`: fica com quem tem menos pontos.
- `MENOR_CARGA`: fica com quem tem menos tarefas em aberto.

Uma rotina roda todo dia às 6h e gera as tarefas que venceram, então não precisa chamar a API. Se gerar duas vezes no mesmo dia, não duplica. Os períodos (`DIARIA`, `SEMANAL`, `QUINZENAL`, `MENSAL`) estão num enum que calcula a próxima data. Na mensal, a escala lembra o dia em que começou: 31/jan, 28/fev, 31/mar.

### Penalidades (Observer)

O síndico cria as regras de penalidade e o sistema aplica sozinho. Tarefa e reserva não sabem que existe penalidade: elas só publicam eventos (`TarefaMarcadaComoAtrasada`, `ReservaCanceladaPeloMorador`, `MoradorNaoCompareceu`) e o `PenalidadeService` escuta e reage.

A regra pode ser `FIXA` (sempre os mesmos pontos) ou `PROGRESSIVA` (aumenta a cada reincidência nos últimos 30 dias, com um teto). Só pode ter uma regra ativa por tipo de infração, e cada penalidade fica registrada no histórico.

### Reservas

A reserva só existe se for válida: o construtor recusa horário no passado, fim antes do início e espaço ou morador inativo. O conflito de horário é checado no service, porque depende das outras reservas. Dois horários conflitam quando o início do novo é antes do fim do que já existe e o fim do novo é depois do início do que já existe.

### Pontos

Concluir uma tarefa no prazo dá os pontos da tarefa. Concluir atrasado dá só metade. Tarefa vencida vira `ATRASADA` e perde pontos conforme a regra de penalidade.

## Como rodar

Precisa de Java 21 ou mais novo. Os comandos abaixo são pra PowerShell, dentro da pasta do projeto.

Primeiro clone o projeto (ou dê `git pull` se já tem):

```powershell
git clone https://github.com/Coifmannn/HomeLive.git
cd HomeLive
```

### Subir com o banco em memória (mais fácil)

```powershell
.\mvnw.cmd spring-boot:run
```

Já vem com dados de exemplo, mas tudo some quando você para a aplicação.

### Subir com PostgreSQL

Precisa ter o PostgreSQL instalado e rodando, com um banco chamado `homelive`. As tabelas o Hibernate cria sozinho.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Se quiser começar sem dados de exemplo, antes de subir:

```powershell
$env:DEMO_DATA='false'
```

Aí o primeiro cadastro cria a residência e a pessoa vira síndica.

Depois que aparecer `Started HomeLiveApplication`, espere uns 10 segundos (os dados de exemplo entram depois disso) e abra http://localhost:8080.

### Contas de exemplo

A senha de todas é `homelive123` (só pra desenvolvimento).

- Síndica: `helena.vance@homelive2050.io`
- Moradores: `lucas.silva@homelive2050.io`, `sofia.chen@homelive2050.io`, `mateus.oliveira@homelive2050.io` e `beatriz.santos@homelive2050.io`

### Variáveis de ambiente (opcionais)

| Variável | Padrão |
| :--- | :--- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `homelive` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `DEMO_DATA` | `true` |
| `JWT_SECRET` | valor de desenvolvimento (mínimo 32 caracteres) |

### Testes

```powershell
.\mvnw.cmd test
```

Os testes usam o H2, então não mexem no Postgres.

### Usar no celular

Com o celular no mesmo Wi-Fi do computador, abra `http://IP-DO-PC:8080` (o IP aparece no `ipconfig`) e use "Adicionar à tela inicial". Se não abrir, o firewall do Windows pode estar bloqueando a porta 8080.

### Se der problema

- `Port 8080 was already in use`: tem outra cópia rodando. Feche ela (Ctrl+C no terminal) e suba de novo.
- Erro de conexão com o banco: o Postgres não está ligado, ou o banco `homelive` não existe. Pra testar sem ele, suba sem o perfil `postgres`.
- `NoClassDefFoundError`: o VS Code recompilou a pasta `target/` junto. Espere um pouco e rode de novo.
- Painel vazio logo depois de subir: os dados de exemplo ainda estão entrando, atualize a página.
- Morador esqueceu a senha: o síndico clica em "Redefinir senha" no cartão do morador, na tela Moradores, e passa a senha temporária pra ele.

Tem um guia mais detalhado, pensado pra quem vai testar na própria máquina, em [docs/como-rodar-o-sistema.md](docs/como-rodar-o-sistema.md).

## Login e permissões

O login é em `POST /api/auth/login` com e-mail e senha e devolve um token JWT. Esse token vai no header `Authorization: Bearer <token>` nas outras requisições. Marcando "continuar conectado" o token dura 30 dias; senão, 24 horas. Também dá pra usar HTTP Basic (os testes e o `curl` usam):

```bash
curl -u lucas.silva@homelive2050.io:homelive123 http://localhost:8080/api/dashboard
```

Quem está fazendo a ação vem sempre do login, nunca do corpo da requisição. Por isso não existem campos como `solicitanteId` ou `sindicoId`.

Resumo do que cada perfil pode:

- Só o síndico: aprovar ou rejeitar despesas, cadastrar moradores, ver e renovar o código de convite, redefinir senha de morador, desativar e reativar moradores, criar espaços, tarefas, escalas e regras de penalidade, aplicar penalidade manual e registrar não comparecimento.
- Os dois: lançar despesa, reservar espaço, concluir tarefa, quitar rateio, registrar leitura de consumo, trocar a própria senha e consultar dashboard e ranking.
- Morador só mexe no que é dele (a própria reserva, tarefa, parcela e leitura). O síndico mexe em qualquer uma.

### Cadastro

A tela de cadastro pergunta à API se a residência já existe:

- Se ainda não existe, a pessoa cria a residência e vira síndica. A casa ganha um código de convite (`HL-XXXXXX`) e dá pra começar com espaços, regras e escalas de sugestão.
- Se já existe, a pessoa entra com o código de convite e o quarto, e vira moradora.

Por enquanto é uma residência por instalação. Várias residências, aprovação do síndico pra entrar e recuperação de senha por e-mail ficaram pra uma próxima versão.

## Endpoints

Base: `http://localhost:8080/api`

**Autenticação e residência**
- `POST /auth/login` (pública)
- `POST /auth/cadastro` (pública)
- `PATCH /auth/senha`: troca a própria senha
- `GET /casa/estado` (pública)
- `GET /casa`
- `POST /casa/convite/renovar` (síndico)

**Moradores e síndicos**
- `GET /moradores`, `GET /moradores/ranking`
- `POST /moradores` (síndico)
- `PATCH /moradores/{id}/estadia?dias=` (síndico)
- `PATCH /moradores/{id}/reativar` (síndico)
- `POST /moradores/{id}/redefinir-senha` (síndico)
- `DELETE /moradores/{id}`: desativa (síndico)
- `GET /sindicos`, `POST /sindicos` (síndico)

**Despesas e consumo**
- `GET /despesas`, `POST /despesas`
- `POST /despesas/{id}/avaliar` (síndico): `{ "aprovado": true, "motivoRejeicao": null }`
- `PATCH /despesas/rateio/{itemId}/quitar`
- `POST /leituras-consumo`, `GET /leituras-consumo?competencia=2026-09`
- `GET /leituras-consumo/morador/{id}`

**Espaços e reservas**
- `GET /espacos`, `POST /espacos` (síndico), `PATCH /espacos/{id}/status` (síndico)
- `GET /agendamentos`, `POST /agendamentos`
- `PATCH /agendamentos/{id}/cancelar`
- `PATCH /agendamentos/{id}/nao-compareceu` (síndico)

**Tarefas e escalas**
- `POST /tarefas` (síndico), `PATCH /tarefas/{id}/concluir`
- `POST /tarefas/processar-atrasos` (síndico)
- `GET /tarefas-recorrentes`, `POST /tarefas-recorrentes` (síndico)
- `PATCH /tarefas-recorrentes/{id}/ativar` e `/desativar` (síndico)
- `POST /tarefas-recorrentes/gerar` (síndico)

**Penalidades**
- `GET /regras-penalidade`, `POST /regras-penalidade` (síndico)
- `PATCH /regras-penalidade/{id}/ativar` e `/desativar` (síndico)
- `GET /penalidades` (síndico), `GET /penalidades/morador/{id}`
- `POST /penalidades`: penalidade manual (síndico)

**Outros**
- `GET /dashboard`: resumo com métricas, ranking e próximas reservas

## Dados de exemplo

Quando sobe com `DEMO_DATA=true` (o padrão) em um banco vazio, o `DataInitializer` cria:

- a residência "Residência HomeLive 2050", com a síndica e os quatro moradores;
- três espaços: Escritório Acústico VR, Lavanderia Inteligente IoT e Espaço Gourmet e Rooftop Hidropônico;
- três regras de penalidade (tarefa atrasada, cancelamento em cima da hora e reserva não utilizada);
- seis escalas recorrentes, algumas já devidas hoje (faça `POST /tarefas-recorrentes/gerar` como síndico pra ver o rodízio funcionando);
- leituras de consumo dos dois últimos meses;
- despesas em vários estados: aprovadas e rateadas (internet, energia, água, compras), pendentes de aprovação e uma rejeitada.

A tarefa vencida do Mateus serve pra testar as penalidades: chame `POST /tarefas/processar-atrasos` e ele perde pontos.
