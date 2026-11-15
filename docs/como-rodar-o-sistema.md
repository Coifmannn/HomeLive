# Como rodar o HomeLive

## O que precisa ter

- Java 21 ou mais novo (tem que ser o JDK, não só o JRE). Pra conferir: `java -version` e `javac -version`.
- PostgreSQL, só se quiser a opção 2 abaixo.

Os comandos abaixo são pra PowerShell, rodando dentro da pasta do projeto (a que tem o `pom.xml`).

## Opção 1: sem instalar banco (a mais fácil)

Usa um banco em memória (H2). Já vem com dados de exemplo, mas tudo some quando a aplicação é parada.

```powershell
.\mvnw.cmd spring-boot:run
```

A primeira vez demora mais porque o Maven baixa as bibliotecas.

## Opção 2: com PostgreSQL

Aqui os dados ficam salvos mesmo depois de parar a aplicação. Crie um banco novo e vazio, só pro projeto. Pode ser pelo pgAdmin (botão direito em Databases, Create, Database, nome `homelive`) ou pelo terminal:

```powershell
psql -U postgres -h localhost -c "CREATE DATABASE homelive;"
```


Não precisa criar tabela nenhuma: o Hibernate cria sozinho quando a aplicação sobe.

Depois é só subir com o perfil `postgres`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

O projeto espera usuário `postgres` e senha `postgres` no `localhost:5432`. Se o seu Postgres for diferente, defina antes de subir:

```powershell
$env:DB_USER='seu-usuario'
$env:DB_PASSWORD='sua-senha'
```

(também existem `DB_HOST`, `DB_PORT` e `DB_NAME`, se precisar.)

## Abrir o sistema

Quando aparecer `Started HomeLiveApplication` no terminal, espere uns 10 segundos e abra http://localhost:8080. Os dados de exemplo entram logo depois do "Started", então se o painel estiver vazio é só atualizar a página.

## Como entrar

Os dados de exemplo são criados pela própria aplicação quando ela sobe e encontra o banco **vazio**. Por isso, o que dá pra fazer depende do banco.

### Banco vazio (a primeira vez, ou a Opção 1)

A aplicação cria as contas de exemplo sozinha. A senha de todas é `homelive123`:

- Síndica: `helena.vance@homelive2050.io`
- Moradores: `lucas.silva@homelive2050.io`, `sofia.chen@homelive2050.io`, `mateus.oliveira@homelive2050.io` e `beatriz.santos@homelive2050.io`

Pra ver o cadastro de um morador novo: entre como síndica, vá na tela Moradores e copie o código de convite. Depois, numa janela anônima, abra `/cadastro.html` e use o código.

### Se as contas de exemplo não existirem

Isso acontece quando o banco já tinha alguém cadastrado antes da aplicação criar os exemplos (a aplicação só cria os exemplos se não houver nenhum morador). Aí tem dois caminhos:

- Criar do zero: na tela de login, clique em **Criar perfil**. A primeira pessoa a se cadastrar cria a residência e vira síndica. Quem se cadastrar depois entra com o código de convite (tela Moradores).
- Voltar a ter os exemplos: pare a aplicação, apague o banco `homelive`, crie ele vazio de novo e suba outra vez.

### Começar sem os dados de exemplo

Se quiser o sistema totalmente vazio de propósito, defina isto antes de subir:

```powershell
$env:DEMO_DATA='false'
```

Aí o primeiro cadastro cria a residência e a pessoa vira síndica (mesmo caminho do "Criar perfil" acima).

Atenção: essa variável fica valendo enquanto a janela do PowerShell estiver aberta. Pra voltar a ter os exemplos, rode `Remove-Item Env:DEMO_DATA` (ou abra outro PowerShell) e suba com o banco vazio.

## Parar a aplicação

`Ctrl+C` no terminal onde ela está rodando.

## Rodar os testes (opcional)

```powershell
.\mvnw.cmd test
```

Os testes usam o banco em memória, então não mexem no Postgres.

## Se der problema

- `No compiler is provided in this environment. Perhaps you are running on a JRE rather than a JDK?`: o Java instalado é só o JRE. Instale um JDK 21 (por exemplo o Temurin) e, se precisar, aponte o `JAVA_HOME` pra pasta dele.
- `Port 8080 was already in use`: já tem uma cópia rodando. Pare ela com `Ctrl+C` e suba de novo.
- Erro de conexão com o banco (opção 2): o Postgres não está ligado ou o banco `homelive` não foi criado. Pra testar sem ele, use a opção 1.
- `password authentication failed`: a senha do seu Postgres não é `postgres`. Defina `DB_PASSWORD` como mostrado acima.
- Não consigo entrar com a Helena: o banco provavelmente não estava vazio ou o `DEMO_DATA` está como `false`. Veja a seção "Como entrar".
- `NoClassDefFoundError`: o VS Code recompilou a pasta `target/` ao mesmo tempo. Espere um pouco e rode de novo.
- Sem internet: o site abre, mas os ícones e a fonte podem não aparecer. O funcionamento é o mesmo.
