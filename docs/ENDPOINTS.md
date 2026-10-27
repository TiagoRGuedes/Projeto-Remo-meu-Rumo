# Endpoints

## Rotas web

- `GET /?mes=AAAA-MM`
- `GET /chamada` (abre a turma do horário atual ou a lista de chamadas)
- `GET /chamada?lista=1`
- `GET /chamada/escolher?turmaId={id}&data=AAAA-MM-DD`
- `GET /chamada/{turmaId}?data=AAAA-MM-DD`
- `POST /chamada/{turmaId}`
- `GET /presencas/registrar?completa=1` (chamada completa; sem parâmetros redireciona para `/chamada`)
- `GET /participantes`
- `GET /participantes/novo`
- `POST /participantes/novo`
- `GET /participantes/{id}`
- `GET /participantes/{id}/editar`
- `POST /participantes/{id}/editar`
- `POST /participantes/{id}/ativar`
- `POST /participantes/{id}/desativar`
- `GET /participantes/{id}/excluir` (confirmação)
- `POST /participantes/{id}/excluir`
- `GET /atividades`
- `GET /atividades/nova`
- `POST /atividades/nova`
- `GET /atividades/{id}`
- `GET /atividades/{id}/editar`
- `POST /atividades/{id}/editar`
- `POST /atividades/{id}/ativar`
- `POST /atividades/{id}/desativar`
- `GET /atividades/{id}/excluir` (confirmação)
- `POST /atividades/{id}/excluir`
- `GET /encontros`
- `GET /encontros/novo`
- `POST /encontros/novo`
- `GET /encontros/{id}`
- `GET /encontros/{id}/editar`
- `POST /encontros/{id}/editar`
- `GET /encontros/{id}/excluir` (confirmação)
- `POST /encontros/{id}/excluir`
- `POST /encontros/{id}/cancelar`
- `GET /presencas/registrar`
- `POST /presencas/registrar`
- `POST /presencas/conferir`
- `POST /presencas/revisar`
- `GET /presencas/{id}/excluir` (confirmação)
- `POST /presencas/{id}/excluir`
- `GET /frequencia`
- `GET /relatorios`
- `GET /historico`
- `GET /ajuda`
- `GET /acessibilidade`
- `POST /acessibilidade`

- `POST /matriculas/nova`
- `POST /matriculas/{id}/encerrar`
- `POST /matriculas/{id}/reativar`
- `POST /matriculas/{id}/trocar`
- `POST /participantes/{id}/anexos` (multipart)
- `GET /anexos/{id}`
- `POST /chamada/{turmaId}/nao-aconteceu`
- `POST /chamada/{turmaId}/desfazer-nao-aconteceu`
- `GET /historico?acao=INGRESSO|SAIDA|TROCA_TURMA|ARQUIVO` e `GET /historico?busca=15/09`

## API REST

- `GET /api/participantes`
- `POST /api/participantes`
- `GET /api/participantes/{id}`
- `PUT /api/participantes/{id}`
- `DELETE /api/participantes/{id}`
- `GET /api/atividades`
- `POST /api/atividades`
- `GET /api/atividades/{id}`
- `PUT /api/atividades/{id}`
- `DELETE /api/atividades/{id}`
- `GET /api/encontros`
- `POST /api/encontros`
- `GET /api/encontros/{id}`
- `PUT /api/encontros/{id}`
- `GET /api/presencas`
- `POST /api/presencas`
- `GET /api/participantes/{id}/matriculas`
- `GET /api/participantes/{id}/frequencia?mes=AAAA-MM`
- `GET /api/turmas`
- `POST /api/turmas`
- `GET /api/turmas/{id}`
- `PUT /api/turmas/{id}`
- `DELETE /api/turmas/{id}`
- `GET /api/turmas/{id}/matriculas`
- `GET /api/turmas/{id}/frequencia?mes=AAAA-MM`
- `GET /api/turmas/{id}/frequencia/participantes?mes=AAAA-MM`
- `GET /api/turmas/{id}/faltosos?mes=AAAA-MM`
- `GET /api/matriculas`
- `POST /api/matriculas`
- `GET /api/matriculas/{id}`
- `POST /api/matriculas/{id}/encerrar`
- `POST /api/matriculas/{id}/reativar`
- `GET /api/exportacao/frequencia-mensal?mes=AAAA-MM` (um registro por matrícula e mês, em `snake_case`, pronto para Power BI ou planilha; sem dados sensíveis)

Sem `mes`, as rotas usam o mês atual.

## CSV

- `GET /relatorios/frequencia-participantes.csv`
- `GET /relatorios/encontro/{id}.csv`
