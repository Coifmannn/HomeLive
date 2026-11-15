# Como rodar o HomeLive

Esse guia é pra quem vai testar o projeto na própria máquina (ex.: o professor). Não precisa usar o banco da gente: tem um jeito sem instalar nada e outro com um PostgreSQL novo.

## O que precisa ter

- Java 21 ou mais novo (tem que ser o JDK, não só o JRE). Pra conferir: `java -version` e `javac -version`.
- Git, se for baixar o projeto pelo GitHub.
- PostgreSQL, só se quiser a opção 2 abaixo.

## Baixar o projeto

```powershell
git clone https://github.com/Coifmannn/HomeLive.git
cd HomeLive
```

Ou descompacte o zip e abra o PowerShell dentro da pasta.

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

Não precisa criar tabela nenhuma, o Hibernate cria sozinho quando a aplicação sobe.

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

Se quiser começar sem os dados de exemplo, defina também `$env:DEMO_DATA='false'`. Aí o primeiro cadastro cria a residência e a pessoa vira síndica.

## Abrir o sistema

Quando aparecer `Started HomeLiveApplication` no terminal, espere uns 10 segundos (os dados de exemplo entram logo depois) e abra http://localhost:8080.

Contas de exemplo (a senha de todas é `homelive123`):

- Síndica: `helena.vance@homelive2050.io`
- Moradores: `lucas.silva@homelive2050.io`, `sofia.chen@homelive2050.io`, `mateus.oliveira@homelive2050.io` e `beatriz.santos@homelive2050.io`

Pra ver o cadastro de um morador novo, abra uma janela anônima, entre como síndica na outra, copie o código de convite na tela Moradores e use ele em `/cadastro.html`.

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
- `NoClassDefFoundError`: o VS Code recompilou a pasta `target/` ao mesmo tempo. Espere um pouco e rode de novo.
- Painel vazio logo depois de subir: os dados de exemplo ainda estão entrando, atualize a página.
- Sem internet: o site abre, mas os ícones e a fonte podem não aparecer. O funcionamento é o mesmo.
