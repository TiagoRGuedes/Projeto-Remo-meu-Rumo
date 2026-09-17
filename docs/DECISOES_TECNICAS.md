# Decisões técnicas

## Java e Spring Boot

O projeto usa Java 25 e Spring Boot 4.1.x para atender ao requisito acadêmico.

## MVC simples

Controllers cuidam de requisições, services cuidam de regras, repositories cuidam do banco e models representam o domínio.

## H2 em arquivo

O banco usa `./dados/remo-meu-rumo` para persistir dados localmente durante desenvolvimento. A pasta `dados/` fica no `.gitignore`.

## Thymeleaf e CSS próprio

A interface usa Thymeleaf, HTML semântico e CSS próprio. JavaScript foi evitado, exceto uma ação simples de impressão no relatório.

## Presença em lote

A tela de presença atualiza ou cria registros para evitar duplicidade. A API individual rejeita duplicidade explicitamente.

## Acessibilidade

Preferências visuais ficam na sessão do servidor e são aplicadas por classes CSS.

## Autenticação futura

A aplicação ainda não usa Spring Security. A arquitetura está preparada para adicionar perfis como ADMIN e OPERADOR depois da confirmação de requisitos.

## Histórico administrativo explícito

Os services registram operações importantes por meio de `HistoricoOperacaoService`, dentro da mesma transação da ação principal. Não foi usado AOP para manter o código acadêmico direto e explicável. Como ainda não existe autenticação, o histórico não inventa nomes de administradores; uma associação de usuário poderá ser adicionada futuramente.

## Exclusão segura

Participantes com presenças e atividades com encontros não podem ser apagados fisicamente. Encontros com presença devem ser cancelados. A exclusão definitiva usa POST na interface e só é permitida após uma página de confirmação quando não há dependências.

## Total esperado no dashboard

Como o modelo atual não possui uma lista histórica de inscritos por encontro, o total esperado usa o maior valor entre participantes atualmente ativos e registros existentes no encontro. Essa regra deve ser revisada se a ONG adotar turmas ou inscrições por atividade.
