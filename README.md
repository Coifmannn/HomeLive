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
  * Concentra atributos e comportamentos comuns (`id`, `nome`, `email`, `telefone`, `ativo`, `dataCriacao`).
  * Especializada via herança relacional (`JOINED`):
    * **`Morador`**: Adiciona `numeroQuarto`, `diasEstadia`, `consumoDeclaradoKwh`, `pontosConvivencia` (gamificação) e métodos de negócio (`adicionarPontos`, `deduzirPontos`).
    * **`Sindico`**: Responsável pela governança, aprovação/rejeição de despesas, cadastro de espaços e penalidades manuais.

### B. Padrão de Projeto: **Strategy (GoF)**
Desacopla os algoritmos de rateio da entidade `Despesa`.
* **Interface `CalculoRateioStrategy`**:
  * `RateioIgualitarioStrategy`: Divisão homogênea entre todos os moradores ativos (com ajuste de centavos de arredondamento no último morador).
  * `RateioPorEstadiaStrategy`: Divisão proporcional aos dias de permanência de cada morador na residência.
  * `RateioPorConsumoStrategy`: Divisão proporcional ao consumo inteligente aferido (ex: kWh por quarto).
* **`RateioStrategyFactory`**: Injeção e resolução dinâmica da estratégia correta via enum `TipoRateio`.

### C. Encapsulamento e Invariantes de Domínio
* **Reserva de Áreas Compartilhadas**:
  * Algoritmo rigoroso de validação de conflito de datas e horários:
    $$\text{Conflito} \iff (\text{inicioNovo} < \text{fimExistente}) \land (\text{fimNovo} > \text{inicioExistente})$$
  * Impede reservas com horários passados, início posterior ao fim e colisão de agendas no mesmo espaço.
* **Gamificação de Tarefas**:
  * Conclusão no prazo bonifica o morador (`pontosRecompensa`).
  * Processamento de tarefas atrasadas aplica dedução de pontos (`pontosPenalidade`).

---

## 🛠️ 3. Tecnologias Utilizadas
* **Java 21 / 25 (LTS)**
* **Spring Boot 4.x** (Spring Web, Spring Data JPA, Hibernate Validator)
* **Banco de Dados H2** (Em memória, com console visual em `/h2-console`)
* **Lombok** (Produtividade e encapsulamento limpo)
* **JUnit 5 / Mockito** (Testes automatizados das regras e do Strategy)

---

## 🔌 4. Guia Rápido de Execução

No terminal, dentro do diretório do projeto:

```bash
# Executar a aplicação
./mvnw spring-boot:run

# Rodar os testes unitários de POO
./mvnw test
```

* **API Base URL:** `http://localhost:8080/api`
* **H2 Console:** `http://localhost:8080/h2-console`
  * **JDBC URL:** `jdbc:h2:mem:homelivedb`
  * **Usuário:** `sa`
  * **Senha:** *(deixar em branco)*

---

## 🌐 5. Endpoints REST (Prontos para Integração com o Front-end)

> **CORS habilitado:** Todas as rotas possuem suporte a `Access-Control-Allow-Origin: *` com suporte a credenciais, permitindo conexão imediata com Vite, React, Vue, Angular ou HTML puro.

| Módulo | Método | Rota | Descrição |
| :--- | :--- | :--- | :--- |
| **Dashboard** | `GET` | `/api/dashboard` | Resumo consolidado com métricas, ranking e agendamentos |
| **Moradores** | `GET` | `/api/moradores` | Lista todos os moradores |
| | `POST` | `/api/moradores` | Cadastra novo morador |
| | `GET` | `/api/moradores/ranking` | Ranking ordenado por pontos de convivência (gamificação) |
| | `PATCH` | `/api/moradores/{id}/consumo` | Atualiza consumo em kWh do morador |
| | `PATCH` | `/api/moradores/{id}/estadia` | Atualiza dias de estadia no período |
| **Espaços** | `GET` | `/api/espacos` | Lista espaços comuns inteligentes |
| | `POST` | `/api/espacos` | Cadastra novo espaço comum |
| | `PATCH` | `/api/espacos/{id}/status` | Ativa/desativa disponibilidade do espaço |
| **Agendamentos**| `POST` | `/api/agendamentos` | Cria reserva com validação rigorosa anti-conflito |
| | `GET` | `/api/agendamentos` | Lista todas as reservas |
| | `PATCH` | `/api/agendamentos/{id}/cancelar` | Cancela um agendamento |
| **Despesas** | `POST` | `/api/despesas` | Lança nova despesa (solicitação) |
| | `POST` | `/api/despesas/{id}/avaliar` | Síndico aprova (e dispara Strategy de rateio) ou rejeita |
| | `PATCH` | `/api/despesas/rateio/{itemId}/quitar` | Morador quita sua parcela do rateio |
| | `GET` | `/api/despesas` | Lista despesas (filtro por status opcional) |
| **Tarefas** | `POST` | `/api/tarefas` | Cria tarefa doméstica da residência |
| | `PATCH` | `/api/tarefas/{id}/concluir` | Morador conclui tarefa (+pontos) |
| | `POST` | `/api/tarefas/processar-atrasos` | Processa penalidades para tarefas vencidas |

---

## 👥 6. Dados Pré-Carregados (`DataInitializer`)
Ao iniciar, o sistema carrega automaticamente:
* **Síndico:** Dra. Helena Vance (`helena.vance@homelive2050.io`)
* **Moradores:** Lucas Silva, Sofia Chen, Mateus Oliveira e Beatriz Santos.
* **Espaços:** *Escritório Acústico VR*, *Lavanderia Inteligente IoT* e *Espaço Gourmet e Rooftop Hidropônico*.
* **Despesas & Rateios:** Demonstração prática do rateio por consumo, igualitário e por estadia já calculados!
