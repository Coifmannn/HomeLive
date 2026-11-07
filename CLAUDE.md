# CLAUDE.md: HomeLive (Co-Living Smart System)

Contexto para o Claude Code continuar este projeto em qualquer máquina. O `README.md` é a documentação voltada a
quem usa a API; este arquivo é o que você precisa saber para **trabalhar no código**.

## O projeto
Trabalho semestral de **POO** (Análise e Desenvolvimento de Sistemas), em grupo. Back-end em **Java 21 + Spring Boot 4.1.1**
(H2 em memória, Spring Data JPA, Spring Security, Lombok). O front-end é de outro integrante do grupo e ainda não existe aqui.
**A nota vem de mostrar POO e padrões de projeto de forma clara e defensável**, então priorize design legível
a infraestrutura sofisticada.

## Sobre o usuário
Estudante e analista **front-end** (JS, PHP, HTML, CSS); estuda Java, Python, C++ e SQL. Fala **português informal**,
responda em pt-BR e descontraído. Explique conceitos de Java/Spring/POO fazendo ponte com o que ele conhece de front-end.
Novo no Claude Code.

## Como rodar
```bash
./mvnw spring-boot:run     # API em http://localhost:8080/api (precisa de JDK 21+)
./mvnw test                # 236 testes, ~1,5 min
# Windows sem JAVA_HOME: use o PowerShell com `$env:JAVA_HOME="C:\Users\<voce>\.jdks\openjdk-26.0.2.1"; .\mvnw.cmd -q test`
# (o Git Bash não acha o JDK). Depois de mudar assinaturas em main, use `mvnw.cmd clean test-compile`: o incremental
# do Maven não recompila os testes e finge que está tudo verde.
```
* Todas as rotas exigem HTTP Basic. Contas de demonstração (`DataInitializer`), **senha `homelive123`**:
  síndico `helena.vance@homelive2050.io`; moradores `lucas.silva@`, `sofia.chen@`, `mateus.oliveira@`, `beatriz.santos@homelive2050.io`.
* `./mvnw -q` esconde a contagem de testes; veja `target/surefire-reports/TEST-*.xml`.
* O servidor responde antes de o `DataInitializer` terminar de semear (ele insere as escalas por último). Espere alguns
  segundos antes de testar à mão.

## Convenções que o usuário pediu
* **Commits:** `feat: descrição em inglês` para algo **novo**; `fix: descrição em inglês` para **ajuste de algo que já existia**
  (refatoração, correção, renomeação). Só esses dois prefixos. Termine a mensagem com a linha `Co-Authored-By` indicada pelo Claude Code.
* **Não dê `git push` sem o usuário pedir.** Ele costuma dar o push sozinho. Pergunte.
* Mantenha o `README.md` atualizado quando mudar a API.

## Arquitetura e regras de design (respeite-as)
Pacotes em `br.com.homelive`: `model` (entidades + `enums`), `repository`, `service`, `controller`, `dto`, `strategy`,
`event`, `security`, `config`, `exception`.

* **Entidades sem `@Setter`.** O estado só muda por métodos de negócio que recusam transições inválidas
  (`Despesa.aprovar`, `AgendamentoEspaco.cancelar`...). Construtor `protected` para o JPA. O teste
  `EncapsulamentoEntidadesTest` **falha** se um setter público aparecer; novas entidades devem entrar na lista dele.
* **Regras ficam na entidade, não no service.** Services orquestram, buscam dados e checam o que depende de outros registros
  (ex.: conflito de horário).
* **Identidade vem do login, não do corpo.** `@AuthenticationPrincipal UsuarioAutenticado`. Regras de "dono":
  `exigirSindicoOuDono(id)` e `moradorAlvo(id)`. Nunca aceite `sindicoId`/`solicitanteId` no DTO.
* **Permissões por papel** ficam todas em `security/SecurityConfig` (matriz numa tela só). Rota nova de síndico entra lá
  **e** na `PermissoesPorPerfilTest`.
* **Padrões em uso:**
  * Strategy + Template Method: rateio (`RateioProporcionalStrategy`, peso por morador, com `ContextoRateio`) e rodízio de
    tarefas (`RodizioBaseStrategy`, `RodizioStrategyFactory`, `ContextoRodizio`).
  * Strategy em enum: `ModoCalculoPenalidade`, `Periodicidade`.
  * Observer: eventos em `event/` (`TarefaMarcadaComoAtrasada`, `ReservaCanceladaPeloMorador`, `MoradorNaoCompareceu`)
    escutados por `PenalidadeService`.
  * Polimorfismo: `Usuario.getTipoUsuario()` abstrato, implementado por `Morador` e `Sindico`; vira a role do Spring Security.
* **Estratégias são puras:** dados externos entram por um objeto de contexto montado pelo service (sem acesso a banco).
* Erros de negócio = `RegraNegocioException` (400), `RecursoNaoEncontradoException` (404), `ConflitoHorarioException` (409);
  `GlobalExceptionHandler` formata tudo em JSON.

## Como o usuário gosta de trabalhar comigo
* **Teste de verdade, não só "compila".** Para cada funcionalidade: testes unitários + de integração + **suba a API e use `curl`**.
  O teste HTTP real já achou bugs que os testes automáticos não viam (CORS que dava 500, rotas inexistentes dando 500).
* **Teste de mutação manual:** sabote a lógica de propósito, confirme que os testes falham, restaure. Já pegou um teste que não testava nada.
* Mostre um **plano antes de mudanças grandes** e deixe o usuário decidir o que é decisão de negócio.
* Commits pequenos e separados por assunto. `fix:` (bug que já existia) separado de `feat:`.

## Armadilhas
* **`pkill -f` / `pgrep -f` com texto que aparece no próprio comando mata o seu shell** (exit 144). Para encerrar a API:
  `kill $(ps -eo pid,args | grep -E "[j]ava.*HomeLive" | awk '{print $1}')` em comando separado.
* Pacotes de teste do Spring Boot 4 mudaram: `AutoConfigureMockMvc` está em `org.springframework.boot.webmvc.test.autoconfigure`.
* Encadear `python script && git commit` numa linha só, senão o commit roda mesmo se o script falhar (já aconteceu).
* Testes mexem em campos privados com `ReflectionTestUtils.setField` (não há setters). Use `@Transactional` nos testes de
  integração para desfazer dados.

## Estado atual
Concluído e testado: autenticação e perfis; encapsulamento; rateio com Strategy+Template; **leitura mensal de consumo**
com `competencia` na despesa; **rodízio de tarefas** (escalas recorrentes + agendador diário às 6h); **regras de penalidade
configuráveis**; dashboard corrigido; **casas (multi-tenant)**: cadastro público, criar casa (vira síndico), entrar com código
+ aprovação, e isolamento total por casa.

### Casas (feito, na branch `pc-casa`)
* `Casa` (nome + `codigoConvite` tipo `HL-7K2Q`). `Usuario.casa` é nula logo após o cadastro. `StatusMembro`: `SEM_CASA`,
  `PENDENTE`, `ATIVO` (`Usuario.getStatusMembro()` é polimórfico; o síndico responde `ATIVO` quando tem casa).
* Cadastro (`POST /api/auth/cadastro`, público) cria um `Morador` sem casa. **Fundar a casa troca esse `Morador` por um `Sindico`**
  com os mesmos dados (o JPA não muda a subclasse de uma linha): o `id` muda, o login (e-mail+senha) não. Ver `CasaService.criar`.
* **Porta de entrada:** `ExigeMembroAtivoFilter` (instanciado no `SecurityConfig`, sem `@Component` de propósito) devolve 403
  "Sem Casa Ativa" a quem não é membro ativo, fora de `/api/auth/**` e `/api/casas/**`.
* **Isolamento:** as 5 entidades-raiz (`EspacoComum`, `Despesa`, `TarefaDomestica`, `TarefaRecorrente`, `RegraPenalidade`) têm
  `casa` obrigatória no construtor; as demais herdam pelo dono (agendamento→espaço, rateio→despesa, leitura/penalidade→morador).
  Todo service recebe o `casaId` do login (`logado.getCasaId()`), nunca do corpo. **Recurso de outra casa = 404**, não 403.
  Ao criar um service/rota nova, filtre por casa **e** acrescente o caso ao `IsolamentoEntreCasasTest`.
* Casa nova nasce com as 3 regras de penalidade padrão (`RegraPenalidade.padroesDa(casa)`).
* Agendador das 6h roda casa por casa (`AgendadorConfig`), cada uma em transação própria.
* Removidos: `POST /api/moradores`, `POST /api/sindicos`, `GET /api/sindicos` (use `GET /api/auth/eu`).
* O `frontend/` do colega ainda usa o contrato antigo (descobre o perfil via `GET /sindicos` e `GET /moradores`) e não tem as
  telas "criar casa / entrar com código / aguardando aprovação". **Precisa ser combinado com ele.**
* Fora da 1ª versão: o que acontece quando o síndico sai (mais simples: transferir o cargo a outro morador).

## Direção nova (ainda NÃO implementada)

### Postgres
O usuário quer migrar do H2 para PostgreSQL. Há um `schema.sql` do grupo (com `tb_morador`, `tb_reserva`, `tb_rateio`,
`tb_votacao`, `tb_voto`...), mas ele **descreve outro modelo** e **não deve ser executado**: o Hibernate gera as tabelas das
entidades (`ddl-auto=update`), o script teria tabelas duplicadas e traz senha em texto puro (incompatível com o Spring Security).
Plano:
* Criar o banco vazio `homelive`, adicionar `org.postgresql:postgresql` no `pom.xml`.
* Perfil `postgres` em `application-postgres.properties` (credenciais por variável de ambiente, dialeto Postgres,
  `ddl-auto=update`). **H2 continua padrão e nos testes**, então os 236 testes não dependem de Postgres.
* `DataInitializer` precisa de guarda para não duplicar a semente a cada boot (dados agora persistem).
* Atualizar o `README.md`. O `schema.sql` vira referência antiga (mover para `docs/` ou apagar, a critério do usuário).
* `tb_votacao`/`tb_voto` (votações) **não existem** no back-end; seria feature nova, se estiver na proposta.
* No ambiente do usuário não havia `psql` nem `docker` no PATH: perguntar se Postgres já está instalado.

### App desktop
O usuário quer **app de desktop** (não celular, não web). Caminho recomendado: **Electron** empacotando o front HTML/JS, falando
com a API em `localhost:8080`. O mais simples é o Electron subir o JAR do Spring junto (exige Java instalado na máquina).
Descartados: PWA/Capacitor (são para celular) e reescrever o front em Flutter/React Native.

### Ordem sugerida (commits separados, `feat:`)
1. ~~Casa + cadastro/entrada + isolamento por casa~~ (feito).
2. Perfil Postgres.
3. Electron.

## Pendências (em ordem de recomendação)
1. **Pontos com consequência nas reservas.** Hoje a gamificação pune, mas quase não recompensa. Ideia: quem tem mais pontos
   reserva com mais antecedência ou tem mais reservas por semana. Fecha o ciclo e rende outra Strategy (regra de antecedência
   por faixa de pontuação).
2. **Confirmação de tarefa por outro morador** antes de os pontos caírem, para evitar fraude. Encaixa no "Morador confirma
   tarefas" da proposta. Pede um novo estado (`AGUARDANDO_CONFIRMACAO`).
3. **Extrato do morador:** "quanto devo neste mês", somando os rateios pendentes dele. Simples e bom para o front.
4. **Mensagem para o integrante do grupo (front-end)** resumindo as mudanças de contrato da API (o README tem tudo, mas um
   resumo direto ajuda): header `Authorization: Basic`; sem `solicitanteId`/`sindicoId`/`atribuidaPorId`; `senha` no cadastro;
   `consumoDeclaradoKwh` removido (agora `/api/leituras-consumo`); `competencia` na despesa; `moradorId` só para o síndico
   em reservas e leituras; novas rotas de escalas, regras de penalidade e penalidades.
5. **Preparar a apresentação:** montar o mapa "conceito de POO → onde está no código" (herança/polimorfismo, encapsulamento,
   os padrões acima, Observer). Posso gerar isso a partir deste arquivo.

## Pontos em aberto (decisões de negócio, perguntar ao usuário)
* `diasEstadia` ainda é **um valor único atual** no morador, o mesmo defeito que o consumo tinha. Poderia virar histórico mensal.
* O total de despesas do dashboard **inclui despesas rejeitadas**, e `totalMoradores` conta moradores inativos.
* `Periodicidade.MENSAL` acumula deriva de dia (31/jan → 28/fev → 28/mar).
* Quem confirma o que, e se o síndico também pode ser "morador" (hoje não: `Sindico` e `Morador` são entidades irmãs).

## Limites conhecidos (aceitáveis para o trabalho, não para produção)
H2 em memória (dados somem ao reiniciar; `ddl-auto=update`, sem migrations); senha de demonstração fixa no `DataInitializer`;
HTTP Basic sem HTTPS; sem paginação nas listagens.
