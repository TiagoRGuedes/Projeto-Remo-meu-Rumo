# Endpoints

## Rotas web

- `GET /`
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

## CSV

- `GET /relatorios/frequencia-participantes.csv`
- `GET /relatorios/encontro/{id}.csv`
