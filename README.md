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
    * **`Morador`**: Adiciona `numeroQuarto`, `diasEstadia`, `consumoDeclaradoKwh`, `pontosConvivencia` (gamificação) e métodos de negócio (`adicionarPontos`, `deduzirPontos`, `atualizarConsumo`, `atualizarEstadia`).
    * **`Sindico`**: Responsável pela governança, aprovação/rejeição de despesas, cadastro de espaços e atribuição de tarefas.

### B. Padrões de Projeto: **Strategy + Template Method (GoF)**
Desacopla os algoritmos de rateio da entidade `Despesa`.
* **Interface `CalculoRateioStrategy`**: contrato de qualquer algoritmo de rateio.
* **Classe abstrata `RateioProporcionalStrategy`** (*Template Method*): contém o esqueleto do cálculo (validar, somar pesos, repartir e fechar os centavos no último morador). O método `calcular()` é `final`; as subclasses só definem o **peso** de cada morador:
  * `RateioIgualitarioStrategy`: peso 1 para todos (divisão homogênea).
  * `RateioPorEstadiaStrategy`: peso = dias de permanência na residência.
  * `RateioPorConsumoStrategy`: peso = consumo declarado em kWh.
* **`RateioStrategyFactory`**: Injeção e resolução dinâmica da estratégia correta via enum `TipoRateio`.

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
  * Processamento de tarefas atrasadas aplica dedução de pontos (`pontosPenalidade`).

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
| Desativar morador / atualizar estadia | ✅ | ❌ |
| Lançar despesa | ✅ (administrativa) | ✅ (em nome próprio) |
| Reservar espaço | ✅ (informa `moradorId`) | ✅ (em nome próprio) |
| Cancelar reserva | ✅ qualquer | só a própria |
| Concluir tarefa | ✅ qualquer | só a própria |
| Quitar item de rateio | ✅ qualquer | só o próprio |
| Atualizar consumo | ✅ qualquer | só o próprio |
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
| | `PATCH` | `/api/moradores/{id}/consumo?consumoKwh=` | próprio ou síndico | Atualiza consumo em kWh |
| | `PATCH` | `/api/moradores/{id}/estadia?dias=` | síndico | Atualiza dias de estadia |
| | `DELETE` | `/api/moradores/{id}` | síndico | Desativa o morador |
| **Síndicos** | `GET` | `/api/sindicos` | logado | Lista síndicos |
| | `POST` | `/api/sindicos` | síndico | Cadastra síndico (corpo inclui `senha`) |
| **Espaços** | `GET` | `/api/espacos` | logado | Lista espaços comuns |
| | `POST` | `/api/espacos` | síndico | Cadastra espaço comum |
| | `PATCH` | `/api/espacos/{id}/status` | síndico | Ativa/desativa o espaço |
| **Agendamentos** | `POST` | `/api/agendamentos` | logado | Cria reserva. `moradorId` só é necessário para o síndico |
| | `GET` | `/api/agendamentos` | logado | Lista reservas |
| | `PATCH` | `/api/agendamentos/{id}/cancelar` | dono ou síndico | Cancela a reserva |
| **Despesas** | `POST` | `/api/despesas` | logado | Lança despesa (solicitante = quem está logado) |
| | `POST` | `/api/despesas/{id}/avaliar` | síndico | Corpo: `{ "aprovado": true, "motivoRejeicao": null }`. Aprova (dispara o rateio) ou rejeita |
| | `PATCH` | `/api/despesas/rateio/{itemId}/quitar` | dono ou síndico | Quita a parcela do rateio |
| | `GET` | `/api/despesas` | logado | Lista despesas (filtro opcional por status) |
| **Tarefas** | `POST` | `/api/tarefas` | síndico | Cria tarefa (atribuída pelo síndico logado) |
| | `PATCH` | `/api/tarefas/{id}/concluir` | responsável ou síndico | Conclui a tarefa (+pontos; 50% se atrasada) |
| | `POST` | `/api/tarefas/processar-atrasos` | síndico | Processa penalidades das tarefas vencidas |

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
* **Despesas & Rateios:** demonstração prática do rateio por consumo e igualitário já calculados, e uma despesa pendente de aprovação.
