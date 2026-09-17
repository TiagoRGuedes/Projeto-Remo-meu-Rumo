# Instituto Remo Meu Rumo - Sistema Administrativo

Sistema web acadêmico para a disciplina IBM4024 - Projeto em Ciência de Dados II - IBMEC 2026.2.

A interface utiliza a identidade oficial do Instituto Remo Meu Rumo e foi desenhada para a gestão de presença e frequência. A origem dos assets, a paleta e as regras visuais estão documentadas em [`docs/IDENTIDADE_VISUAL.md`](docs/IDENTIDADE_VISUAL.md).

Integrantes: Bruno Cappellette e Tiago Guedes.

## Requisitos

- Windows com Visual Studio Code.
- Java JDK 25 instalado e configurado no `PATH`.
- Internet na primeira execução do Maven Wrapper, pois ele baixa o Maven localmente.
- Maven instalado é opcional.

## Instalar Java

1. Baixe um JDK 25 de um fornecedor confiável, como Eclipse Temurin, Oracle JDK ou Microsoft Build of OpenJDK.
2. Instale o JDK.
3. Abra um novo terminal PowerShell.
4. Confirme:

```powershell
java -version
```

O resultado deve indicar Java 25.

## VS Code

Extensões recomendadas:

- Extension Pack for Java
- Spring Boot Extension Pack
- Maven for Java
- Thymeleaf

Abra esta pasta no VS Code e aguarde a indexação do Java.

## Executar

No terminal PowerShell, dentro da pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Se o Maven estiver instalado:

```powershell
mvn spring-boot:run
```

Acesse:

```text
http://localhost:8080/
```

Para abrir o ambiente demonstrativo completo, com dados fictícios em um banco separado:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
```

O perfil `demo` não apaga nem substitui o banco padrão. A carga é idempotente: só é criada quando o banco demonstrativo está vazio.

## H2 Console

Com a aplicação em execução:

```text
http://localhost:8080/h2-console
```

Use:

- JDBC URL: `jdbc:h2:file:./dados/remo-meu-rumo`
- User: `sa`
- Password: deixe em branco

O banco fica na pasta `dados/`, que não deve ser versionada.

## Testes

```powershell
.\mvnw.cmd clean test
```

## Parar o servidor

No terminal onde a aplicação está rodando, pressione:

```text
Ctrl + C
```

## Erros comuns

- `java version "1.8"`: instale/configure o JDK 25.
- Porta 8080 ocupada: encerre a outra aplicação ou altere `server.port` em `src/main/resources/application.properties`.
- H2 bloqueado: feche outras execuções da aplicação e tente novamente.
- Maven não encontrado: use `.\mvnw.cmd`, que baixa Maven localmente.

## Funcionalidades

- Cadastro, edição, busca e ativação/desativação de participantes.
- Busca global conectando participantes, atividades e encontros.
- Cadastro, edição, busca e ativação/desativação de atividades.
- Dashboards da Visão geral, participantes, atividades, encontros e frequência.
- Criação, edição, consulta, cancelamento e exclusão segura de encontros.
- Fluxo de formulário, revisão e confirmação final para todas as operações da interface.
- Token de uso único por operação para impedir gravação duplicada por clique ou reenvio.
- Registro de presença em lote com resumo ao vivo, conferência e confirmação antes de salvar.
- Correção e exclusão controlada de registros de presença.
- Histórico de presença com filtros.
- Histórico administrativo somente leitura, com filtros, comparação antes/depois e links aos registros relacionados.
- Exclusão definitiva apenas quando não existem dados relacionados; desativação ou cancelamento nos demais casos.
- Frequência automática.
- Relatórios com impressão e CSV.
- Ajuda para primeiros passos.
- Preferências de acessibilidade por sessão.
- API REST básica.

## Dados demonstrativos

O perfil `demo` cria, somente em um banco vazio, 36 participantes fictícios, 6 atividades demonstrativas, 24 encontros distribuídos no período recente e presenças com diferentes faixas de frequência. Use esses dados para apresentação e testes manuais; eles são claramente identificados como demonstração e não representam pessoas reais.
