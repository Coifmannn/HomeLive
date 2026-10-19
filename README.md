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
* `Periodicidade` (`DIARIA`, `SEMANAL`, `QUINZENAL`, `MENSAL`) é um enum que sabe calcular a própria data seguinte.

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
* **Spring Security** (HTTP Basic, senhas com BCrypt, perfis Síndico e Morador)
* **Banco de Dados H2** (Em memória, com console visual em `/h2-console`, restrito ao síndico)
* **Lombok** (Produtividade e encapsulamento limpo)
* **JUnit 5 / MockMvc** (Testes das regras de domínio, do Strategy e da segurança)

---

## 🔌 4. Guia Rápido de Execução

No terminal, dentro do diretório do projeto:

```bash
# Executar a aplicação
./mvnw spring-boot:run

# Rodar os testes automatizados
./mvnw test
```

* **API Base URL:** `http://localhost:8080/api`
* **H2 Console:** `http://localhost:8080/h2-console` (exige login de síndico)
  * **JDBC URL:** `jdbc:h2:mem:homelivedb`
  * **Usuário:** `sa`
  * **Senha:** *(deixar em branco)*

---

## 🔐 5. Autenticação e Perfis

A API usa **HTTP Basic**: toda requisição leva o header `Authorization: Basic base64(email:senha)`. Sem ele, a resposta é `401`; com perfil sem permissão, `403`. Ambos em JSON, no mesmo formato dos demais erros.

```js
// Exemplo no front-end
const auth = 'Basic ' + btoa('lucas.silva@homelive2050.io:homelive123');
fetch('http://localhost:8080/api/dashboard', { headers: { Authorization: auth } });
```

**Quem está agindo vem do login, não do corpo da requisição.** Por isso os campos `solicitanteId` (despesa), `sindicoId` (avaliação) e `atribuidaPorId` (tarefa) não existem mais. Se o front enviá-los, são ignorados.

| Ação | Síndico | Morador |
| :--- | :---: | :---: |
| Aprovar/rejeitar despesa | ✅ | ❌ |
| Cadastrar moradores e síndicos | ✅ | ❌ |
| Cadastrar espaço / ativar-desativar | ✅ | ❌ |
| Criar tarefa / processar atrasos | ✅ | ❌ |
| Criar, ativar/desativar escalas recorrentes e gerar tarefas | ✅ | ❌ |
| Configurar regras de penalidade e aplicar penalidade manual | ✅ | ❌ |
| Registrar não comparecimento a uma reserva | ✅ | ❌ |
| Ver penalidades | ✅ todas | só as próprias |
| Desativar morador / atualizar estadia | ✅ | ❌ |
| Lançar despesa | ✅ (administrativa) | ✅ (em nome próprio) |
| Reservar espaço | ✅ (informa `moradorId`) | ✅ (em nome próprio) |
| Cancelar reserva | ✅ qualquer | só a própria |
| Concluir tarefa | ✅ qualquer | só a própria |
| Quitar item de rateio | ✅ qualquer | só o próprio |
| Registrar leitura de consumo | ✅ de qualquer morador | só a própria |
| Consultas (dashboard, ranking, listagens) | ✅ | ✅ |

---

## 🌐 6. Endpoints REST

> **CORS habilitado** para qualquer origem (Vite, React, Vue, Angular ou HTML puro). O header `Authorization` é aceito.

| Módulo | Método | Rota | Acesso | Descrição |
| :--- | :--- | :--- | :---: | :--- |
| **Dashboard** | `GET` | `/api/dashboard` | logado | Resumo consolidado com métricas, ranking e agendamentos |
| **Moradores** | `GET` | `/api/moradores` | logado | Lista todos os moradores |
| | `POST` | `/api/moradores` | síndico | Cadastra morador (corpo inclui `senha`, mín. 6 caracteres) |
| | `GET` | `/api/moradores/ranking` | logado | Ranking por pontos de convivência |
| | `PATCH` | `/api/moradores/{id}/estadia?dias=` | síndico | Atualiza dias de estadia |
| | `DELETE` | `/api/moradores/{id}` | síndico | Desativa o morador |
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
