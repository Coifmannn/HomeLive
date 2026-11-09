# Modelagem de Dados: HomeLive 2050

Banco relacional **PostgreSQL** (perfil `postgres`). As tabelas são geradas pelo Hibernate a partir das classes `@Entity`
do pacote `br.com.homelive.model`, então este diagrama espelha o código.

## Diagrama Entidade-Relacionamento (DER)

```mermaid
erDiagram
    usuarios ||--o| moradores : "é um (herança)"
    usuarios ||--o| sindicos : "é um (herança)"

    moradores ||--o{ leituras_consumo : "registra"
    moradores ||--o{ despesas : "solicita"
    sindicos  ||--o{ despesas : "aprova/rejeita"
    despesas  ||--|{ rateio_itens : "é dividida em"
    moradores ||--o{ rateio_itens : "deve pagar"

    espacos_comuns ||--o{ agendamentos_espacos : "é reservado em"
    moradores      ||--o{ agendamentos_espacos : "reserva"

    moradores ||--o{ tarefas_domesticas : "é responsável por"
    sindicos  ||--o{ tarefas_domesticas : "atribui"
    moradores ||--o{ tarefas_recorrentes : "último responsável"

    moradores         ||--o{ penalidades : "recebe"
    regras_penalidade ||--o{ penalidades : "origina"
    sindicos          ||--o{ penalidades : "aplica"

    usuarios {
        bigint id PK
        varchar nome
        varchar email UK
        varchar telefone
        varchar senha_hash "BCrypt"
        boolean ativo
        timestamp data_criacao
    }
    moradores {
        bigint id PK,FK "= usuarios.id"
        varchar numero_quarto
        int dias_estadia
        int pontos_convivencia
        date data_entrada
    }
    sindicos {
        bigint id PK,FK "= usuarios.id"
        varchar departamento
        date inicio_mandato
        date fim_mandato
    }
    leituras_consumo {
        bigint id PK
        bigint morador_id FK
        date competencia "UK com morador_id"
        numeric kwh
        timestamp registrada_em
    }
    despesas {
        bigint id PK
        varchar titulo
        varchar descricao
        numeric valor_total
        date data_vencimento
        date data_lancamento
        date competencia
        varchar categoria
        varchar tipo_despesa
        varchar tipo_rateio
        varchar status
        bigint solicitante_id FK
        bigint aprovador_id FK
        varchar motivo_rejeicao
    }
    rateio_itens {
        bigint id PK
        bigint despesa_id FK
        bigint morador_id FK
        numeric valor_calculado
        varchar status_pagamento
        date data_pagamento
    }
    espacos_comuns {
        bigint id PK
        varchar nome
        varchar descricao
        int capacidade_maxima
        varchar regras_uso
        boolean ativo
    }
    agendamentos_espacos {
        bigint id PK
        bigint espaco_id FK
        bigint morador_id FK
        timestamp data_hora_inicio
        timestamp data_hora_fim
        varchar motivo_reserva
        varchar status
        timestamp data_criacao
    }
    tarefas_domesticas {
        bigint id PK
        varchar titulo
        varchar descricao
        date data_prazo
        int pontos_recompensa
        int pontos_penalidade
        varchar status
        bigint responsavel_id FK
        bigint atribuida_por_id FK
        timestamp data_conclusao
    }
    tarefas_recorrentes {
        bigint id PK
        varchar titulo
        varchar descricao
        varchar periodicidade
        int prazo_em_dias
        int pontos_recompensa
        int pontos_penalidade
        varchar tipo_rodizio
        date proxima_geracao
        boolean ativa
        bigint ultimo_responsavel_id FK
    }
    regras_penalidade {
        bigint id PK
        varchar nome
        varchar tipo_infracao
        varchar modo_calculo
        int pontos_base
        int incremento_por_reincidencia
        int teto_pontos
        int janela_reincidencia_dias
        int limite_horas_antecedencia
        boolean ativa
    }
    penalidades {
        bigint id PK
        bigint morador_id FK
        bigint regra_id FK "nulo = manual"
        varchar tipo_infracao
        int pontos
        varchar motivo
        timestamp data_aplicacao
        bigint aplicada_por_id FK "nulo = automática"
    }
```

## Tabelas

| Tabela | Classe Java | O que guarda |
| :--- | :--- | :--- |
| `usuarios` | `Usuario` (abstrata) | Dados comuns de login de qualquer pessoa |
| `moradores` | `Morador extends Usuario` | Quarto, estadia e pontos de convivência |
| `sindicos` | `Sindico extends Usuario` | Mandato e departamento do administrador |
| `leituras_consumo` | `LeituraConsumo` | kWh de cada morador por mês (base do rateio por consumo) |
| `despesas` | `Despesa` | Contas da casa e seu ciclo de aprovação |
| `rateio_itens` | `RateioItem` | Parcela de cada morador numa despesa aprovada |
| `espacos_comuns` | `EspacoComum` | Áreas compartilhadas (escritório, lavanderia, gourmet) |
| `agendamentos_espacos` | `AgendamentoEspaco` | Reservas de espaços |
| `tarefas_domesticas` | `TarefaDomestica` | Tarefas concretas com prazo e pontos |
| `tarefas_recorrentes` | `TarefaRecorrente` | Escalas que geram tarefas automaticamente (rodízio) |
| `regras_penalidade` | `RegraPenalidade` | Regras configuradas pelo síndico |
| `penalidades` | `Penalidade` | Histórico de punições aplicadas (auditoria) |

**Herança `JOINED`:** `usuarios` tem as colunas comuns; `moradores` e `sindicos` têm só as colunas próprias e usam o
**mesmo `id`** como chave primária e estrangeira. Um morador é a junção `usuarios ⨝ moradores`.

## Regras de negócio

**Usuários e perfis**
* E-mail é único (`UNIQUE`). Senha guardada só como hash BCrypt.
* Dois perfis com permissões distintas: **Síndico** (administra) e **Morador** (usa).
* Morador não é apagado: é desativado (`ativo = false`), para preservar o histórico.
* Todo morador começa com 100 pontos de convivência.

**Despesas e rateio**
* Ciclo de vida: `PENDENTE_APROVACAO → APROVADA → RATEADA`, ou `PENDENTE_APROVACAO → REJEITADA`. Uma despesa avaliada não
  volta a ser avaliada.
* Só o síndico aprova ou rejeita (a rejeição pode registrar um motivo).
* Ao aprovar, a despesa é dividida entre os moradores ativos (`rateio_itens`) conforme `tipo_rateio`: igualitário, por
  dias de estadia ou por consumo (kWh). A soma das parcelas é sempre igual ao valor total (os centavos fecham no último).
* O rateio por consumo usa as leituras do **mês de competência** da conta. Falta leitura de algum morador → o rateio é recusado.
* Uma leitura por morador por mês (`UNIQUE (morador_id, competencia)`); registrar de novo corrige a existente.

**Reservas de espaços**
* Não se reserva no passado, com fim antes do início, nem espaço ou morador inativos.
* Não pode haver duas reservas confirmadas do mesmo espaço com horários sobrepostos:
  `inicioNovo < fimExistente AND fimNovo > inicioExistente`.
* Morador só cancela a própria reserva. Cancelar em cima da hora pode gerar penalidade.

**Tarefas e gamificação**
* Concluir no prazo dá `pontos_recompensa`; fora do prazo, 50%.
* Tarefa vencida vira `ATRASADA` e gera penalidade.
* Escalas recorrentes geram tarefas sozinhas (rotina diária às 06:00), escolhendo o responsável por rodízio
  (`SEQUENCIAL`, `MENOR_PONTUACAO` ou `MENOR_CARGA`). Gerar duas vezes no mesmo dia não duplica.

**Penalidades**
* Só uma regra ativa por tipo de infração.
* Cálculo `FIXA` (sempre `pontos_base`) ou `PROGRESSIVA` (`pontos_base + incremento × reincidências`, limitado ao teto).
* Toda penalidade fica registrada com morador, regra, pontos, motivo, data e quem aplicou.
