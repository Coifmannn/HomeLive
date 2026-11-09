# 🏢 HomeLive (2050)
> **Sistema Inteligente de Gestão de Convivência, Custos Compartilhados e Escalas**  
> *Projeto Semestral de Programação Orientada a Objetos (POO) em Java com Spring Boot.*

---

## 🚀 1. Visão Geral do Projeto
No ano de **2050**, casas inteligentes compartilhadas (*co-living*) exigem governança automatizada e sem atrito para consumo de recursos sustentáveis (energia solar, água reciclada, conexões quânticas), rateio de despesas, agendamento de espaços coletivos e divisão justa de tarefas domésticas através de gamificação.

---

## 💎 2. Onde Brilha a Orientação a Objetos (POO)

### A. Herança e Polimorfismo
* **Classe Abstrata `Usuario`**:
  * Concentra atributos e comportamentos comuns (`id`, `nome`, `email`, `senhaHash`, `telefone`, `ativo`, `dataCriacao`).
  * Declara o método abstrato `getTipoUsuario()`: cada perfil informa o próprio papel (polimorfismo), e a camada de segurança deriva as permissões daí.
  * Especializada via herança relacional (`JOINED`):
    * **`Morador`**: Adiciona `numeroQuarto`, `diasEstadia`, `pontosConvivencia` (gamificação) e métodos de negócio (`adicionarPontos`, `deduzirPontos`, `atualizarEstadia`). O consumo de energia não fica no morador: vem das leituras mensais (`LeituraConsumo`).
    * **`Sindico`**: Responsável pela governança, aprovação/rejeição de despesas, cadastro de espaços e atribuição de tarefas.

### B. Padrões de Projeto: **Strategy + Template Method (GoF)**
Desacopla os algoritmos de rateio da entidade `Despesa`.
* **Interface `CalculoRateioStrategy`**: contrato de qualquer algoritmo de rateio.
* **Classe abstrata `RateioProporcionalStrategy`** (*Template Method*): contém o esqueleto do cálculo (validar, somar pesos, repartir e fechar os centavos no último morador). O método `calcular()` é `final`; as subclasses só definem o **peso** de cada morador:
  * `RateioIgualitarioStrategy`: peso 1 para todos (divisão homogênea).
  * `RateioPorEstadiaStrategy`: peso = dias de permanência na residência.
  * `RateioPorConsumoStrategy`: peso = **leitura de consumo (kWh) do mês da conta**. Se algum morador não tiver leitura naquele mês, o rateio é recusado e a resposta diz quem falta.
* **`ContextoRateio`**: dados de fora do morador (as leituras do mês) entregues prontos pelo service, para as estratégias continuarem puras e sem acesso a banco.
* **`RateioStrategyFactory`**: Injeção e resolução dinâmica da estratégia correta via enum `TipoRateio`.

**Fidelidade ao período:** cada despesa tem uma `competencia` (mês de referência). Uma conta de setembro lançada em outubro é dividida pelas leituras de setembro, e não pelo consumo atual de cada morador.

### C. Encapsulamento e Invariantes de Domínio
* **Sem setters nas entidades:** o estado só muda por métodos de negócio que recusam transições inválidas (`Despesa.aprovar/rejeitar/marcarComoRateada`, `RateioItem.quitar`, `AgendamentoEspaco.cancelar`, `TarefaDomestica.concluir`). Um teste automatizado falha se alguém adicionar um setter público.
* **Ciclo de vida da despesa:** `PENDENTE_APROVACAO → APROVADA → RATEADA` (ou `→ REJEITADA`). Uma despesa já avaliada não pode ser avaliada de novo.
* **Reserva de Áreas Compartilhadas**:
  * Uma reserva inválida nem chega a existir: o construtor de `AgendamentoEspaco` recusa horários passados, fim anterior ao início, espaço ou morador inativos.
  * Algoritmo de conflito de datas e horários (checado pelo service, pois depende de outras reservas):
    $$\text{Conflito} \iff (\text{inicioNovo} < \text{fimExistente}) \land (\text{fimNovo} > \text{inicioExistente})$$
* **Gamificação de Tarefas**:
  * Conclusão no prazo bonifica o morador com `pontosRecompensa`.
  * Conclusão **fora do prazo** rende só **50%** da recompensa.
  * Tarefas vencidas viram `ATRASADA`, e a dedução de pontos segue as **regras de penalidade** (seção E). Sem regra ativa, vale o `pontosPenalidade` da própria tarefa.

### D. Escala Automática de Tarefas com Rodízio
A `TarefaRecorrente` é o *modelo* de uma tarefa que se repete ("tirar o lixo, toda semana"). Quando fica devida, gera uma `TarefaDomestica` concreta e avança a própria data, e a escolha de **quem** pega a tarefa é uma **Strategy** plugável (`RodizioStrategy`, com *Template Method* e `RodizioStrategyFactory`, como no rateio):

| `tipoRodizio` | Quem recebe a tarefa |
| :--- | :--- |
| `SEQUENCIAL` | Os moradores em ordem circular. Na primeira vez parte de quem tem menos tarefas em aberto, para que escalas criadas juntas não girem em sincronia |
| `MENOR_PONTUACAO` | Quem tem menos pontos de convivência (ajuda a equilibrar o ranking) |
| `MENOR_CARGA` | Quem tem menos tarefas em aberto (pendentes ou atrasadas) |

* **Automático:** uma rotina diária (06:00, horário de Brasília) penaliza os atrasos e gera as tarefas devidas. Nenhuma chamada à API é necessária.
* **Idempotente:** gerar duas vezes no mesmo dia não duplica tarefas. Se o sistema ficou parado, gera uma só e salta para a próxima data futura.
* Só participam moradores **ativos**. Tarefas geradas aparecem como atribuídas pelo "Sistema Automático".
* `Periodicidade` (`DIARIA`, `SEMANAL`, `QUINZENAL`, `MENSAL`) é um enum que sabe calcular a própria data seguinte. Na **mensal**, a escala guarda o dia em que começou (`diaAncora`): ajusta nos meses curtos e volta ao dia certo (31/jan → 28/fev → 31/mar → 30/abr), sem ficar presa no 28.

### E. Regras de Penalidade Configuráveis (Observer + Strategy)
O síndico define as regras; o sistema as aplica sozinho quando a infração acontece. Tarefas e reservas **não sabem que penalidades existem**: elas publicam *eventos* (`TarefaMarcadaComoAtrasada`, `ReservaCanceladaPeloMorador`, `MoradorNaoCompareceu`) e o `PenalidadeService` reage a eles (padrão **Observer**).

| Infração (`tipoInfracao`) | Quando acontece |
| :--- | :--- |
| `TAREFA_ATRASADA` | A rotina diária (ou o síndico) marca uma tarefa vencida como atrasada |
| `CANCELAMENTO_TARDIO` | O próprio morador cancela a reserva com menos de `limiteHorasAntecedencia` horas para o início. Cancelamento feito pelo síndico não penaliza |
| `NAO_COMPARECIMENTO` | O síndico registra que o morador não usou o espaço (`PATCH /api/agendamentos/{id}/nao-compareceu`, só depois do início) |
| `MANUAL` | O síndico aplica diretamente, informando pontos e motivo. Não usa regra |

**Como calcular** (`modoCalculo`, uma *Strategy* implementada no próprio enum):
* `FIXA`: sempre `pontosBase`.
* `PROGRESSIVA`: `pontosBase + incrementoPorReincidencia × reincidências`, limitado por `tetoPontos` (opcional). Reincidência é uma penalidade do mesmo tipo recebida nos últimos `janelaReincidenciaDias` dias (padrão 30).

Detalhes:
* Só **uma regra ativa por tipo** de infração. Para trocar, desative a atual e crie a nova.
* Uma regra ativa de `TAREFA_ATRASADA` **substitui** o `pontosPenalidade` de cada tarefa. Sem regra, vale o valor da tarefa. Para as demais infrações, sem regra ativa não há penalidade automática.
* Cada punição fica registrada (`Penalidade`: morador, regra, pontos, motivo, data, quem aplicou ou "Sistema Automático"), o que dá trilha de auditoria e base para contar reincidências.
* O morador consulta o próprio histórico; o síndico vê todos.

---

## 🛠️ 3. Tecnologias Utilizadas
* **Java 21 / 25 (LTS)**
* **Spring Boot 4.x** (Spring Web, Spring Data JPA, Hibernate Validator)
* **Spring Security** (login com token JWT e HTTP Basic, senhas com BCrypt, perfis Síndico e Morador)
* **PWA** (front instalável no celular, servido pela própria API)
* **PostgreSQL** (persistência relacional, perfil `postgres`)
* **Banco de Dados H2** (padrão de desenvolvimento e dos testes; em memória, com console visual em `/h2-console`, restrito ao síndico)
* **Lombok** (Produtividade e encapsulamento limpo)
* **JUnit 5 / MockMvc** (Testes das regras de domínio, do Strategy e da segurança)

---

## 🔌 4. Passo a passo para rodar (apresentação)

> Tudo em **PowerShell**, dentro da pasta do projeto. Use exatamente os mesmos passos no ensaio e na apresentação.
> Roteiro do que mostrar: [docs/roteiro-demonstracao.md](docs/roteiro-demonstracao.md). Cola da defesa: [docs/Cola-da-defesa.pdf](docs/Cola-da-defesa.pdf).

### 4.1 Antes de começar (confira uma vez)

| O que | Como conferir | Esperado |
| :--- | :--- | :--- |
| Java 21 ou mais novo | `java -version` | versão 21+ (na máquina do grupo: 23) |
| PostgreSQL 17 ligado | `Get-Service *postgres*` | `Status: Running` |
| Git | `git --version` | qualquer versão |
| Internet | na 1ª vez o Maven baixa as bibliotecas; o site usa fontes e ícones da internet | sem internet o sistema funciona, só os ícones somem |

### 4.2 Pegar a versão mais nova do projeto
```powershell
cd C:\Users\Lube\HomeLive
git pull
```
Em outra máquina, na primeira vez: `git clone https://github.com/Coifmannn/HomeLive.git` e depois `cd HomeLive`.

### 4.3 Deixar o banco zerado (faça antes de apresentar)
Os dados de exemplo usam a **data de agora** (reservas, tarefas atrasadas, escalas devidas). Zerar o banco faz tudo nascer de hoje. **Pare a API antes** (passo 4.7).
```powershell
$env:PGPASSWORD='postgres'
& 'C:\Program Files\PostgreSQL\17\bin\psql.exe' -U postgres -h localhost -c "DROP DATABASE IF EXISTS homelive;" -c "CREATE DATABASE homelive;"
```
Na primeira vez numa máquina nova, o `DROP` não faz nada e o `CREATE` cria o banco. Não precisa criar tabelas: o Hibernate cria sozinho.

### 4.4 Subir a API
Escolha **uma** das duas formas:

**A) Com dados de exemplo** (painel já cheio: moradores, despesas, tarefas, reservas):
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

**B) Totalmente vazio** (o primeiro cadastro cria a residência e vira síndico):
```powershell
$env:DEMO_DATA='false'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Espere aparecer `Started HomeLiveApplication` e **mais uns 10 segundos** (no modo A os dados de exemplo terminam de entrar depois do "Started"). A primeira subida é mais lenta porque compila o projeto.

### 4.5 Abrir o sistema
Abra **http://localhost:8080** no navegador.

| Modo | Como entrar |
| :--- | :--- |
| **A (exemplo)** | Contas abaixo, todas com a senha `homelive123` |
| **B (vazio)** | Clique em **Criar perfil**: a tela pede o nome da residência e você vira o síndico. Quem se cadastrar depois usa o código de convite (tela **Moradores**) |

| Perfil | E-mail |
| :--- | :--- |
| Síndica | `helena.vance@homelive2050.io` |
| Morador | `lucas.silva@homelive2050.io` |
| Morador | `sofia.chen@homelive2050.io` |
| Morador | `mateus.oliveira@homelive2050.io` |
| Morador | `beatriz.santos@homelive2050.io` |

### 4.6 Usar no celular
Celular no **mesmo Wi-Fi** do computador. Descubra o IP (`ipconfig`, campo "Endereço IPv4") e abra `http://IP-DO-COMPUTADOR:8080` no Chrome do celular, depois **Adicionar à tela inicial** (detalhes na seção 5.1).
Se o celular não abrir o site, o firewall do Windows está bloqueando a porta. Libere uma vez, em um PowerShell **como administrador**:
```powershell
New-NetFirewallRule -DisplayName "HomeLive 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow
```

### 4.7 Parar a API
Aperte **Ctrl+C** no terminal onde ela está rodando. Se o terminal foi fechado sem parar e a porta ficou presa:
```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

### 4.8 Plano B: sem o Postgres
Se o Postgres não ligar na hora, suba só com o banco em memória (já vem com os dados de exemplo; tudo some ao parar):
```powershell
.\mvnw.cmd spring-boot:run
```

### 4.9 Rodar os testes (opcional, uns 2 minutos)
```powershell
.\mvnw.cmd test
```
Deve terminar com `Tests run: 251, Failures: 0, Errors: 0` e `BUILD SUCCESS`. Os testes usam o banco em memória e **não mexem** no Postgres.

### 4.10 Se algo der errado

| O que aparece | Causa provável | O que fazer |
| :--- | :--- | :--- |
| `Port 8080 was already in use` | Outra cópia da API rodando | Comando do passo 4.7 e suba de novo |
| Erro de conexão com o banco ao subir | Postgres parado ou banco inexistente | `Start-Service postgresql-x64-17` (PowerShell como administrador) e/ou recriar o banco (passo 4.3) |
| `password authentication failed` | A senha do Postgres não é `postgres` | `$env:DB_PASSWORD='sua-senha'` antes de subir (variáveis na tabela abaixo) |
| `NoClassDefFoundError` ao subir ou testar | O VS Code recompilou a pasta `target/` ao mesmo tempo | Espere alguns segundos e rode de novo (ou feche o VS Code) |
| Painel vazio logo depois de subir (modo A) | Os dados de exemplo ainda estão entrando | Espere ~10 segundos e atualize a página |
| Site sem ícones e com outra fonte | Sem internet | Funciona igual; só a aparência muda |
| "E-mail ou senha incorretos" | Conta ou senha errada | Use uma conta do passo 4.5 (modo A) ou cadastre-se (modo B) |
| Esqueceu a senha de um morador | Não há e-mail no sistema | O síndico clica em **Redefinir senha** no cartão do morador (tela Moradores) |

### Variáveis de ambiente (opcionais)

| Variável | Padrão | Para que serve |
| :--- | :--- | :--- |
| `DB_HOST` | `localhost` | Endereço do Postgres |
| `DB_PORT` | `5432` | Porta do Postgres |
| `DB_NAME` | `homelive` | Nome do banco |
| `DB_USER` | `postgres` | Usuário do banco |
| `DB_PASSWORD` | `postgres` | Senha do banco |
| `DEMO_DATA` | `true` | `false` abre o sistema vazio |
| `JWT_SECRET` | valor de desenvolvimento | Segredo do token de login (mínimo 32 caracteres) |

Sem o perfil `postgres`, a API usa o H2 em memória (os testes também).

* **API Base URL:** `http://localhost:8080/api`
* **H2 Console** (só no modo em memória): `http://localhost:8080/h2-console` (exige login de síndico). JDBC URL `jdbc:h2:mem:homelivedb`, usuário `sa`, senha em branco.

---

## 🔐 5. Autenticação e Perfis

### Login com token ("continuar conectado")
O front troca e-mail + senha por um **token** (JWT assinado) em `POST /api/auth/login` e daí em diante só mostra o token. A senha nunca fica guardada no navegador.

```js
// 1. Login (rota pública)
const r = await fetch('/api/auth/login', {
  method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'lucas.silva@homelive2050.io', senha: 'homelive123', lembrar: true })
}).then(r => r.json());
// r = { token, expiraEm, papel: "MORADOR" | "SINDICO", usuario: { ...dados do morador/síndico } }

// 2. Demais rotas
fetch('/api/dashboard', { headers: { Authorization: 'Bearer ' + r.token } });
```

| `lembrar` | Validade do token | Onde o front guarda |
| :--- | :--- | :--- |
| `true` ("Continuar conectado") | 30 dias | `localStorage` (sobrevive ao fechar o app) |
| `false` ou ausente | 24 horas | `sessionStorage` (some ao fechar a aba) |

* Login errado (e-mail inexistente, senha errada ou conta desativada) responde sempre `401` com a mesma mensagem.
* Token adulterado, expirado ou de usuário desativado depois do login: `401`.
* O segredo de assinatura vem da variável `JWT_SECRET` (mín. 32 caracteres). O padrão do `application.properties` é só para desenvolvimento.
* Quem já tem token válido entra direto: a tela de login redireciona para o painel sozinha.

### Cadastro: criar a residência ou entrar com convite
A tela `cadastro.html` pergunta à API (`GET /api/casa/estado`) se a residência já existe e mostra o formulário certo:

| Situação | Formulário | Resultado |
| :--- | :--- | :--- |
| Ainda **não existe** residência | **Criar residência**: nome da casa e (opcional) "começar com sugestões prontas" | A pessoa vira **síndico**, a casa é criada com um código de convite `HL-XXXXXX` e, se marcado, nasce com 3 espaços, 3 regras de penalidade e 3 escalas de tarefas |
| **Já existe** | **Entrar**: código de convite e quarto | A pessoa vira **morador** |

* Nos dois casos o cadastro já devolve o token: a pessoa sai logada e cai no painel (o síndico novo, direto na tela de Moradores, onde está o código).
* O síndico copia o código ou gera um novo na tela de Moradores. O código aceita minúsculas e espaços; trocá-lo invalida o anterior.
* Quem tenta criar uma segunda residência (sem o código) é barrado: com a casa criada, só dá para entrar com convite.
* Existe **uma residência por instalação**. Várias residências, aprovação do síndico antes da entrada e recuperação de senha por e-mail ficam para a próxima versão.

### Esqueci a senha (sem e-mail)
O sistema não envia e-mail, então a recuperação passa pelo síndico: na tela **Moradores** ele clica em **Redefinir senha** no cartão do morador e recebe uma **senha temporária**, que aparece uma única vez (no banco fica só o hash). O morador entra com ela e usa **Trocar minha senha** (menu lateral) para escolher a dele. A página `esqueceu-senha.html` explica esses passos. Tokens já emitidos continuam valendo até expirar; ao trocar a senha, vale a nova só nos próximos logins.

### HTTP Basic (continua funcionando)
Toda requisição também pode levar `Authorization: Basic base64(email:senha)` (usado pelos testes e pelo `curl`). Sem credencial válida, a resposta é `401`; com perfil sem permissão, `403`. Ambos em JSON, no mesmo formato dos demais erros.

```bash
curl -u lucas.silva@homelive2050.io:homelive123 http://localhost:8080/api/dashboard
```

### Permissões por perfil

**Quem está agindo vem do login, não do corpo da requisição.** Por isso os campos `solicitanteId` (despesa), `sindicoId` (avaliação) e `atribuidaPorId` (tarefa) não existem mais. Se o front enviá-los, são ignorados.

| Ação | Síndico | Morador |
| :--- | :---: | :---: |
| Aprovar/rejeitar despesa | ✅ | ❌ |
| Cadastrar moradores e síndicos | ✅ | ❌ |
| Ver e renovar o código de convite da residência | ✅ | ❌ |
| Redefinir a senha de um morador (senha temporária) | ✅ | ❌ |
| Trocar a própria senha | ✅ | ✅ |
| Cadastrar espaço e ativar/desativar (tela Reservas) | ✅ | ❌ |
| Cadastrar espaço / ativar-desativar | ✅ | ❌ |
| Criar tarefa / processar atrasos | ✅ | ❌ |
| Criar, ativar/desativar escalas recorrentes e gerar tarefas | ✅ | ❌ |
| Configurar regras de penalidade e aplicar penalidade manual | ✅ | ❌ |
| Registrar não comparecimento a uma reserva | ✅ | ❌ |
| Ver penalidades | ✅ todas | só as próprias |
| Desativar, reativar morador e editar dias de estadia | ✅ | ❌ |
| Lançar despesa | ✅ (administrativa) | ✅ (em nome próprio) |
| Reservar espaço | ✅ (informa `moradorId`) | ✅ (em nome próprio) |
| Cancelar reserva | ✅ qualquer | só a própria |
| Concluir tarefa | ✅ qualquer | só a própria |
| Quitar item de rateio | ✅ qualquer | só o próprio |
| Registrar leitura de consumo | ✅ de qualquer morador | só a própria |
| Consultas (dashboard, ranking, listagens) | ✅ | ✅ |

## 📱 5.1 App no celular (PWA)
O front fica na pasta `frontend/` e é **servido pela própria API**: uma URL só para site e dados. Ele é um **PWA**: no celular dá para instalar na tela inicial, com ícone e tela cheia, sem loja de apps.

1. Suba a API (passo 4.4) e deixe o celular **no mesmo Wi-Fi** do computador. Se não abrir, libere a porta no firewall (passo 4.6).
2. Descubra o IP do computador (`ipconfig`, campo "Endereço IPv4"), por exemplo `192.168.0.10`.
3. No Chrome do celular, abra `http://192.168.0.10:8080`.
4. Menu ⋮ → **Adicionar à tela inicial**.

> `localhost` no celular é o próprio celular, por isso o IP. Sem HTTPS o Chrome cria um atalho (abre o site); a instalação completa ("Instalar app", funcionamento offline da casca) exige HTTPS ou `localhost`. Os dados nunca ficam em cache: tudo que é `/api` vai sempre ao servidor.

---

## 🌐 6. Endpoints REST

> **CORS habilitado** para qualquer origem (Vite, React, Vue, Angular ou HTML puro). O header `Authorization` é aceito.

| Módulo | Método | Rota | Acesso | Descrição |
| :--- | :--- | :--- | :---: | :--- |
| **Autenticação** | `POST` | `/api/auth/login` | pública | `{ "email", "senha", "lembrar" }` → `{ token, expiraEm, papel, usuario }` |
| | `PATCH` | `/api/auth/senha` | logado | A própria pessoa troca a senha: `{ "senhaAtual", "novaSenha" }` → `204`. Senha atual errada ou nova igual à atual: `400` (não `401`, para não derrubar a sessão) |
| | `POST` | `/api/auth/cadastro` | pública | Cria a conta e já devolve o token (`201`). Sem residência: cria a casa e a pessoa vira **síndico**. Com residência: exige `codigoConvite` e `numeroQuarto` e a pessoa vira **morador** |
| **Residência** | `GET` | `/api/casa/estado` | pública | `{ casaCriada, nomeCasa }`: a tela de cadastro usa para escolher entre criar e entrar |
| | `GET` | `/api/casa` | logado | Nome da residência. O `codigoConvite` só vem para o síndico |
| | `POST` | `/api/casa/convite/renovar` | síndico | Gera um novo código de convite (o anterior deixa de valer) |
| **Dashboard** | `GET` | `/api/dashboard` | logado | Resumo consolidado com métricas, ranking e agendamentos |
| **Moradores** | `GET` | `/api/moradores` | logado | Lista todos os moradores |
| | `POST` | `/api/moradores` | síndico | Cadastra morador (corpo inclui `senha`, mín. 6 caracteres) |
| | `GET` | `/api/moradores/ranking` | logado | Ranking por pontos de convivência |
| | `PATCH` | `/api/moradores/{id}/estadia?dias=` | síndico | Atualiza dias de estadia |
| | `DELETE` | `/api/moradores/{id}` | síndico | Desativa o morador (ele deixa de entrar e de contar nos totais e rateios) |
| | `PATCH` | `/api/moradores/{id}/reativar` | síndico | Traz de volta um morador desativado (`204`; reativar quem já está ativo também dá `204`) |
| | `POST` | `/api/moradores/{id}/redefinir-senha` | síndico | Gera uma **senha temporária** (8 caracteres) para o morador que esqueceu a dele: `{ "senhaTemporaria": "..." }`. Ela só aparece nesta resposta; a senha antiga deixa de valer |
| **Leituras de consumo** | `POST` | `/api/leituras-consumo` | logado | Registra (ou corrige) a leitura do mês: `{ "kwh": 120.5, "competencia": "2026-09" }`. `competencia` é opcional (padrão: mês corrente). O síndico também informa `moradorId` |
| | `GET` | `/api/leituras-consumo?competencia=2026-09` | logado | Leituras de um mês (sem parâmetro, o mês corrente) |
| | `GET` | `/api/leituras-consumo/morador/{id}` | logado | Histórico de leituras de um morador |
| **Síndicos** | `GET` | `/api/sindicos` | logado | Lista síndicos |
| | `POST` | `/api/sindicos` | síndico | Cadastra síndico (corpo inclui `senha`) |
| **Espaços** | `GET` | `/api/espacos` | logado | Lista espaços comuns |
| | `POST` | `/api/espacos` | síndico | Cadastra espaço comum |
| | `PATCH` | `/api/espacos/{id}/status` | síndico | Ativa/desativa o espaço |
| **Agendamentos** | `POST` | `/api/agendamentos` | logado | Cria reserva. `moradorId` só é necessário para o síndico |
| | `GET` | `/api/agendamentos` | logado | Lista reservas |
| | `PATCH` | `/api/agendamentos/{id}/cancelar` | dono ou síndico | Cancela a reserva. Pode gerar penalidade de cancelamento tardio, se o morador cancelar |
| | `PATCH` | `/api/agendamentos/{id}/nao-compareceu` | síndico | Registra que o morador não usou o espaço (só depois do início) |
| **Despesas** | `POST` | `/api/despesas` | logado | Lança despesa (solicitante = quem está logado). `competencia` (`aaaa-MM`, opcional) é o mês a que a conta se refere e define quais leituras valem no rateio por consumo |
| | `POST` | `/api/despesas/{id}/avaliar` | síndico | Corpo: `{ "aprovado": true, "motivoRejeicao": null }`. Aprova (dispara o rateio) ou rejeita |
| | `PATCH` | `/api/despesas/rateio/{itemId}/quitar` | dono ou síndico | Quita a parcela do rateio |
| | `GET` | `/api/despesas` | logado | Lista despesas (filtro opcional por status) |
| **Tarefas** | `POST` | `/api/tarefas` | síndico | Cria tarefa (atribuída pelo síndico logado) |
| | `PATCH` | `/api/tarefas/{id}/concluir` | responsável ou síndico | Conclui a tarefa (+pontos; 50% se atrasada) |
| | `POST` | `/api/tarefas/processar-atrasos` | síndico | Marca tarefas vencidas como atrasadas e dispara as penalidades (a rotina diária já faz isso) |
| **Regras de penalidade** | `GET` | `/api/regras-penalidade` | logado | Lista as regras (todos podem consultar as regras da casa) |
| | `POST` | `/api/regras-penalidade` | síndico | Cria regra: `nome`, `tipoInfracao`, `modoCalculo`, `pontosBase` (opcionais: `incrementoPorReincidencia`, `tetoPontos`, `janelaReincidenciaDias`, `limiteHorasAntecedencia`) |
| | `PATCH` | `/api/regras-penalidade/{id}/ativar` e `/desativar` | síndico | Liga ou desliga a regra (só uma ativa por tipo) |
| **Penalidades** | `GET` | `/api/penalidades` | síndico | Histórico geral |
| | `GET` | `/api/penalidades/morador/{id}` | dono ou síndico | Histórico de um morador |
| | `POST` | `/api/penalidades` | síndico | Penalidade manual: `{ "moradorId": 3, "pontos": 10, "motivo": "..." }` |
| **Tarefas recorrentes** | `GET` | `/api/tarefas-recorrentes` | logado | Lista as escalas com a próxima data e o último responsável |
| | `POST` | `/api/tarefas-recorrentes` | síndico | Cria escala: `titulo`, `periodicidade`, `prazoEmDias`, `tipoRodizio` (opcionais: `descricao`, `pontosRecompensa`, `pontosPenalidade`, `primeiraGeracao`) |
| | `PATCH` | `/api/tarefas-recorrentes/{id}/ativar` e `/desativar` | síndico | Liga ou pausa a escala |
| | `POST` | `/api/tarefas-recorrentes/gerar` | síndico | Gera agora as tarefas devidas (a rotina diária já faz isso sozinha) |

---

## 👥 7. Dados Pré-Carregados (`DataInitializer`)
Ao iniciar, o sistema carrega automaticamente as contas abaixo. **A senha de todas é `homelive123`** (apenas para desenvolvimento).
A residência de exemplo se chama *Residência HomeLive 2050*; o código de convite dela aparece para a Dra. Helena na tela de Moradores.

> **Quer começar do zero?** Suba com `DEMO_DATA=false` num banco vazio (ex.: `$env:DEMO_DATA='false'` no PowerShell antes do `./mvnw spring-boot:run`). O sistema abre sem nenhum usuário e o **primeiro cadastro cria a residência** e vira o síndico. Banco que já tinha usuários e nenhuma residência (criado antes desta versão) ganha uma casa automaticamente.

| Perfil | Nome | E-mail |
| :--- | :--- | :--- |
| Síndico | Dra. Helena Vance | `helena.vance@homelive2050.io` |
| Morador | Lucas Silva | `lucas.silva@homelive2050.io` |
| Morador | Sofia Chen | `sofia.chen@homelive2050.io` |
| Morador | Mateus Oliveira | `mateus.oliveira@homelive2050.io` |
| Morador | Beatriz Santos | `beatriz.santos@homelive2050.io` |

* **Espaços:** *Escritório Acústico VR*, *Lavanderia Inteligente IoT* e *Espaço Gourmet e Rooftop Hidropônico*.
* **Regras de penalidade:** tarefa atrasada (progressiva: 10 pts +5 por reincidência, teto 30), cancelamento com menos de 2h de antecedência (fixa, 10 pts) e reserva não utilizada (fixa, 20 pts). Dá para testar processando os atrasos (`POST /api/tarefas/processar-atrasos`): a tarefa vencida do Mateus gera a primeira penalidade.
* **Escalas recorrentes:** *Retirada do lixo orgânico* (semanal, `SEQUENCIAL`), *Limpeza do Espaço Gourmet* (semanal, `MENOR_CARGA`) e *Troca dos filtros de água* (mensal, `MENOR_PONTUACAO`). As duas primeiras ficam devidas hoje: faça `POST /api/tarefas-recorrentes/gerar` como síndico para ver o rodízio distribuí-las.
* **Leituras de consumo:** dos dois últimos meses para os quatro moradores.
* **Despesas & Rateios:** a conta de energia de demonstração é do **mês anterior** e foi rateada com as leituras daquele mês. Há também um rateio igualitário já calculado e uma despesa pendente de aprovação.
