# Modelo de dados

## Participante

- `id`
- `nome`
- `ativo`
- `observacoes`
- `dataCadastro`
- `dataAtualizacao`

## Atividade

- `id`
- `nome`
- `descricao`
- `ativo`
- `dataCadastro`

## Encontro

- `id`
- `atividade`
- `data`
- `observacoes`
- `status`
- `dataCadastro`

Relacionamento: muitos encontros pertencem a uma atividade.

## Presenca

- `id`
- `participante`
- `encontro`
- `status`
- `observacoes`
- `registradoEm`
- `atualizadoEm`

Relacionamentos:

- muitas presenças pertencem a um participante;
- muitas presenças pertencem a um encontro;
- `participante + encontro` é único.

## Enums

`StatusPresenca`:

- `PRESENTE`
- `AUSENTE`
- `JUSTIFICADA`

`StatusEncontro`:

- `PLANEJADO`
- `REALIZADO`
- `CANCELADO`

## HistoricoOperacao

- `id`
- `modulo`
- `tipoEntidade`
- `entidadeId`
- `nomeEntidade`
- `acao`
- `descricao`
- `dataHora`

O histórico administrativo não possui relacionamento físico com os cadastros. Assim, uma exclusão segura continua registrada sem impedir a remoção do cadastro. Os registros são somente leitura na interface.

`AcaoHistorico`:

- `CRIACAO`
- `EDICAO`
- `ATIVACAO`
- `DESATIVACAO`
- `EXCLUSAO`
- `REGISTRO`
- `CORRECAO`
- `CANCELAMENTO`
