# Fluxograma: HomeLive 2050

Fluxo da aplicação do ponto de vista do usuário (front-end HTML/JS → API Spring Boot → PostgreSQL).

## 1. Fluxo geral: acesso e navegação

```mermaid
flowchart TD
    A([Abre o app ou o site]) --> T{Tem token guardado<br/>e ainda válido?<br/>continuar conectado}
    T -- Sim --> G
    T -- Não --> B[Tela de login<br/>e-mail + senha<br/>marca ou não Continuar conectado]
    B --> C{Campos válidos?}
    C -- Não --> B
    C -- Sim --> D[Front chama POST /api/auth/login]
    D --> E{API: senha confere com o hash BCrypt<br/>e a conta está ativa?}
    E -- Não / 401 --> F[Mensagem: e-mail ou senha incorretos] --> B
    E -- Sim --> K[API devolve token JWT + perfil<br/>validade 30 dias se continuar conectado, senão 24h]
    K --> L[Front guarda só o token<br/>e envia Bearer em cada requisição]
    L --> G{Qual o perfil?}
    G -- Síndico --> H[Painel do Síndico]
    G -- Morador --> I[Painel do Morador]

    H --> M[Menu]
    I --> M
    M --> V[Visão geral<br/>métricas, ranking, próximas reservas]
    M --> FI[Finanças<br/>despesas e rateios]
    M --> TA[Tarefas e escalas]
    M --> RE[Reservas de espaços]
    M --> MO[Moradores]
    M --> RG[Regras e penalidades]
    M --> S([Sair: limpa a sessão]) --> B
```

## 2. O que cada perfil pode fazer

```mermaid
flowchart LR
    subgraph Sindico[Síndico]
        S1[Cadastrar moradores e síndicos]
        S2[Aprovar ou rejeitar despesas]
        S3[Criar tarefas e escalas]
        S4[Configurar regras de penalidade]
        S5[Aplicar penalidade manual]
        S6[Registrar não comparecimento]
        S7[Cadastrar e desativar espaços]
    end
    subgraph Morador[Morador]
        M1[Lançar despesa]
        M2[Reservar e cancelar a própria reserva]
        M3[Concluir as próprias tarefas]
        M4[Registrar a própria leitura de consumo]
        M5[Quitar a própria parcela]
        M6[Ver o próprio histórico de penalidades]
    end
    API{{API verifica o perfil<br/>em cada requisição}}
    Sindico --> API
    Morador --> API
    API -- sem permissão --> E403[403 Proibido]
    API -- permitido --> OK[Executa e grava no banco]
```

## 3. Despesa e rateio

```mermaid
flowchart TD
    A([Morador ou síndico lança despesa]) --> B{Dados válidos?<br/>valor > 0, título, vencimento}
    B -- Não / 400 --> A
    B -- Sim --> C[Despesa salva como<br/>PENDENTE_APROVACAO]
    C --> D{Síndico avalia}
    D -- Rejeita --> E[REJEITADA<br/>com motivo opcional]
    D -- Aprova --> F{Tipo de rateio}
    F -- Igualitário --> G[Peso 1 para cada morador ativo]
    F -- Por estadia --> H[Peso = dias de estadia]
    F -- Por consumo --> I{Todos têm leitura<br/>do mês da conta?}
    I -- Não --> J[Recusa e informa quem falta] --> D
    I -- Sim --> K[Peso = kWh do mês]
    G --> L[Divide o valor e fecha os centavos<br/>no último morador]
    H --> L
    K --> L
    L --> M[Cria uma parcela por morador<br/>status PENDENTE]
    M --> N[Despesa RATEADA]
    N --> O[Morador quita a parcela] --> P([Parcela PAGO])
```

## 4. Reserva de espaço comum

```mermaid
flowchart TD
    A([Morador escolhe espaço e horário]) --> B{Início no futuro e<br/>fim depois do início?}
    B -- Não / 400 --> A
    B -- Sim --> C{Espaço e morador ativos?}
    C -- Não / 400 --> A
    C -- Sim --> D{Conflita com outra<br/>reserva confirmada?}
    D -- Sim / 409 --> A
    D -- Não --> E[Reserva CONFIRMADA]
    E --> F{O que acontece?}
    F -- Morador cancela --> G{Faltam menos horas que<br/>o limite da regra?}
    G -- Sim --> H[CANCELADO + penalidade<br/>CANCELAMENTO_TARDIO]
    G -- Não --> I[CANCELADO sem penalidade]
    F -- Síndico cancela --> I
    F -- Síndico registra ausência --> J[NAO_COMPARECEU + penalidade<br/>NAO_COMPARECIMENTO]
    F -- Usa normalmente --> K([Fim])
```

## 5. Tarefas, rodízio e gamificação

```mermaid
flowchart TD
    A([Rotina diária 06:00<br/>ou síndico]) --> B[Marca tarefas vencidas<br/>como ATRASADA]
    B --> C[Evento: tarefa atrasada] --> D[PenalidadeService aplica a regra<br/>FIXA ou PROGRESSIVA]
    A --> E{Há escala recorrente<br/>devida hoje?}
    E -- Não --> Z([Fim])
    E -- Sim --> F{Tipo de rodízio}
    F -- Sequencial --> G[Próximo da fila]
    F -- Menor pontuação --> H[Quem tem menos pontos]
    F -- Menor carga --> I[Quem tem menos tarefas abertas]
    G --> J[Gera tarefa PENDENTE<br/>e avança a próxima data]
    H --> J
    I --> J
    J --> K{Morador conclui}
    K -- No prazo --> L[+100% dos pontos]
    K -- Atrasada --> M[+50% dos pontos]
    L --> N([Ranking atualizado])
    M --> N
```
