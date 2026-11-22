# Política de segurança

Este sistema guarda dados pessoais e de saúde de crianças e jovens atendidos pelo Instituto Remo Meu Rumo. Por isso, problemas de segurança são tratados com prioridade.

## Como relatar uma vulnerabilidade

Não abra uma issue pública. Use a aba **Security → Report a vulnerability** deste repositório no GitHub, que envia o relato de forma privada aos mantenedores.

Inclua, se possível:

- o que você encontrou e onde (tela, endpoint ou arquivo);
- os passos para reproduzir;
- o impacto que você imagina.

## O que este repositório não contém

- Nenhuma senha, chave ou credencial real. A conta principal vem das variáveis de ambiente `ADMIN_USUARIO` e `ADMIN_SENHA`, e o deploy no Azure usa os segredos do repositório.
- Nenhum dado real de participantes. As telas, os testes e o perfil `demo` usam apenas dados fictícios.
