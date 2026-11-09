# Roteiro de demonstração: HomeLive 2050 (5 minutos)

## Antes de começar (fora do tempo)

1. **Feche a API**, se estiver rodando, e **zere o banco** para os dados de exemplo nascerem de hoje (reservas, tarefas atrasadas e escalas usam a data de agora):
   ```powershell
   $env:PGPASSWORD='postgres'
   & 'C:\Program Files\PostgreSQL\17\bin\psql.exe' -U postgres -h localhost -c "DROP DATABASE IF EXISTS homelive;" -c "CREATE DATABASE homelive;"
   ```
2. **Suba a API:** `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"`. Espere `Started HomeLiveApplication` e mais uns 10 segundos (os dados de exemplo terminam de entrar depois).
3. **Duas janelas do navegador** em `http://localhost:8080`: uma normal (síndica) e uma **anônima** (cadastro do morador).
4. **Abertos:** `docs/DER-e-Fluxograma.pdf` e `docs/Cola-da-defesa.pdf`. Celular com o atalho já na tela inicial.
5. **Contas de exemplo** (senha `homelive123`): síndica `helena.vance@homelive2050.io`, morador `lucas.silva@homelive2050.io`.
6. Tenha um e-mail novo pronto para o cadastro, por exemplo `visitante@homelive2050.io`.

## Roteiro

| Tempo | O que fazer na tela | O que falar | Conceito que aparece |
| :--- | :--- | :--- | :--- |
| **0:00** (30s) | Tela de login. | "O HomeLive é um sistema de gestão de uma casa compartilhada: rateio de despesas, reserva de espaços e tarefas com pontos. Java com Spring Boot, PostgreSQL, e funciona como app no celular." | Visão geral |
| **0:30** (40s) | Digitar uma **senha errada** e mostrar o aviso. Entrar como **Helena** marcando **Continuar conectado**. Depois abrir `login.html` de novo: ele entra direto. | "O navegador guarda um token assinado, nunca a senha. Quem marcou continuar conectado fica 30 dias." | Autenticação, BCrypt, JWT, validação |
| **1:10** (60s) | Menu **Moradores**: mostrar o card do **código de convite** e copiar. Na janela anônima, abrir `/cadastro.html` ("Entre na sua residência"): tentar um **código errado** (erro), depois o **certo**, com o e-mail novo e um quarto. Cai no painel como morador. | "O primeiro cadastro cria a residência e vira síndico. Os outros entram com o código e viram moradores. Repare: o morador não vê o código nem os botões de síndico." | Perfis e permissões, herança (`Usuario` → `Morador`/`Sindico`) |
| **2:10** (60s) | Como Helena: **Finanças & Rateio** → despesa pendente *Revisão Técnica dos Robôs Aspiradores* → **Aprovar**. Mostrar as parcelas por morador. | "Ao aprovar, o sistema divide o valor. Aqui o critério é estadia, mas pode ser igualitário ou por consumo. A soma sempre fecha: o último morador absorve os centavos." | Strategy e Template Method |
| **3:10** (40s) | Menu **Reservas**: reservar um espaço. Tentar **o mesmo horário de novo** e mostrar o erro de conflito. | "Não deixa duas reservas no mesmo espaço e horário: há conflito se o início novo é antes do fim existente e o fim novo é depois do início existente." | Regra de negócio, validação, erro 409 |
| **3:50** (40s) | Menu **Escalas & Tarefas**: **Gerar Tarefas Devidas** (rodízio) e **Processar Atrasos**. Depois **Regras & Penalidades**: mostrar a penalidade que o Mateus recebeu. | "O rodízio escolhe quem pega a tarefa. Tarefa atrasada gera um evento, e quem aplica a penalidade é outra classe. A tarefa não sabe que existem penalidades." | Strategy no rodízio e Observer |
| **4:30** (30s) | Mostrar o **celular** com o app. Mostrar o **PDF do DER e do fluxograma**. | "Está também como app no celular. A modelagem tem 13 tabelas. Ficou para a próxima versão: várias residências e recuperação de senha." | PWA, DER, limitações assumidas |

## Se algo der errado
- **Postgres não sobe:** pare a API e suba sem o perfil (`.\mvnw.cmd spring-boot:run`). Usa o banco em memória e funciona igual.
- **Sem internet na sala:** o site abre, mas os ícones e a fonte (carregados de fora) podem sumir. Não afeta o funcionamento.
- **Celular não abre o site:** mostre o app no computador e diga que o celular usa o mesmo endereço pelo IP do computador. Se tiver, mostre uma foto da tela do celular.
- **Erro no meio da demo:** respire, volte ao menu e siga para a próxima parte. Nada aqui depende de uma tela só.

## Frases de segurança (se perguntarem)
- "Por que token e não a senha?" → A senha não fica no navegador; o token é assinado e expira.
- "Por que uma residência só?" → Decisão de escopo: várias residências exigiriam filtrar todos os dados por casa. Ficou para a próxima versão.
- "Quem aplica a penalidade?" → O `PenalidadeService`, que reage a eventos publicados pelas tarefas e reservas (Observer).
