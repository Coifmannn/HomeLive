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
./mvnw test                # 177 testes, ~1 min
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
configuráveis**; dashboard corrigido.

**Atenção:** no momento em que este arquivo foi escrito, havia **3 commits locais ainda sem push**
(`bab1262`, `b9f4a70`, `9a8b062`), mais o commit deste arquivo. Rode `git status -sb` e confira.

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
