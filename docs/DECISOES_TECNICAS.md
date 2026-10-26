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

## Atividade como turma

A atividade passou a funcionar como a turma do Instituto, com dias da semana e horário. Assim, encontros, presenças e relatórios existentes continuam válidos, sem uma migração de dados.

## Matrícula e frequência mensal

A matrícula liga a pessoa à turma e define quem é esperado em cada aula. A frequência mensal usa as aulas previstas dentro do período da matrícula e até hoje; aula sem registro conta como falta.

## Vocabulário da ONG

A interface usa Turma, Aula, Presente, Falta e Falta avisada. O código, as rotas e o banco mantêm os nomes `Atividade`, `Encontro`, `AUSENTE` e `JUSTIFICADA`, para não exigir migração.

## Chamada em uma etapa

A chamada rápida dispensa a tela de conferência: o botão Salvar só libera quando todos estão marcados e mostra quantos faltam. A aula é criada automaticamente na primeira chamada do dia, e uma aula planejada passa a realizada. As marcações ficam no `localStorage` do aparelho até o envio.

## Idioma fixo

O sistema usa `pt-BR` fixo para datas e números, independentemente do idioma do navegador.

## Dados sensíveis

Deficiência e referência de laudo ficam só no cadastro individual. Não aparecem na listagem da API, no Histórico nem na exportação.

## Total esperado no dashboard

Como o modelo atual não possui uma lista histórica de inscritos por encontro, o total esperado usa o maior valor entre participantes atualmente ativos e registros existentes no encontro. Essa regra deve ser revisada se a ONG adotar turmas ou inscrições por atividade.
