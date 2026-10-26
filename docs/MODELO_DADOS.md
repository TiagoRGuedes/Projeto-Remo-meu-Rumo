# Modelo de dados

## Participante

- `id`
- `nome`
- `sexo` (`FEMININO`, `MASCULINO`, `NAO_INFORMADO`)
- `dataNascimento` (a idade é calculada)
- `tipoEscola` (`PUBLICA`, `PARTICULAR`, `NAO_ESTUDA`, `NAO_INFORMADO`)
- `possuiDeficiencia` (sim, não ou não informado; código CD/SD)
- `condicao` (nome da deficiência ou condição, por exemplo TEA ou Síndrome de Down)
- `ativo`
- `observacoes`
- `dataCadastro`
- `dataAtualizacao`

`possuiDeficiencia`, `condicao` e os anexos são dados sensíveis: aparecem só no cadastro individual e ficam fora da listagem da API, do Histórico e da exportação.

## Atividade

- `id`
- `nome`
- `descricao`
- `diasSemana` (tabela `atividade_dias_semana`)
- `horario`
- `ativo`
- `dataCadastro`

A atividade funciona como a turma do Instituto: tem grade (dias e horário) e recebe matrículas.

## Matricula

- `id`
- `participante`
- `atividade` (turma)
- `status` (`ATIVA`, `ENCERRADA`)
- `dataInicio`
- `dataSaida`
- `motivoSaida` (`DEDICACAO_OUTRA_ATIVIDADE`, `MUDANCA_ROTINA_FAMILIAR`, `AFASTAMENTO_SAUDE`, `OUTRO`)
- `observacaoSaida`
- `dataCadastro`
- `dataAtualizacao`

Relacionamento: um participante pode ter matrículas em várias turmas. Cada matrícula é um período (início e saída). Troca de turma encerra a matrícula atual com o motivo `TROCA_DE_TURMA` e abre outra na nova turma; um retorno abre um novo período. Só pode haver uma matrícula ativa por participante e turma.

## Anexo

- `id`
- `participante`
- `encontro` (opcional; atestado de uma falta avisada)
- `tipo` (`ATESTADO`, `LAUDO`, `OUTRO`)
- `nomeArquivo`, `contentType`, `tamanho`, `conteudo` (PDF ou imagem, até 5 MB)
- `enviadoEm`

## Aula não realizada

`Encontro` ganhou `motivoCancelamento` (`FERIADO`, `GREVE`, `CONDICOES_CLIMATICAS`, `OUTRO`) e `justificativaCancelamento`. Uma aula cancelada não conta como aula prevista para ninguém.

## Frequência mensal

- Aulas previstas: encontros não cancelados da turma no mês, dentro dos períodos de matrícula e até a data de hoje. Quando a pessoa entrou ou saiu no meio do mês, a tela avisa ("Conta só as aulas desde o ingresso em …").
- Percentual: presenças ÷ aulas previstas. Aula sem registro conta como falta; falta justificada continua sendo falta.
- Meta mensal: 60%.
- Mais de 3 faltas no mês: contato com a família.

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
