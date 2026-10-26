# Guia do usuário

## Cadastrar participantes

1. Abra Participantes.
2. Clique em Cadastrar participante.
3. Informe o nome e, se souber, sexo, data de nascimento, escola e deficiência.
4. Em "Referência do laudo", informe só o código ou a pasta onde o laudo está guardado.
5. Revise e confirme.

## Matricular em uma turma

1. Abra a página do participante.
2. Em Matrículas, escolha a turma e a data de início.
3. Clique em Matricular, revise e confirme.
4. Uma pessoa pode estar em várias turmas.

## Registrar a saída e o retorno

1. Na matrícula, clique em Registrar saída.
2. Escolha o motivo (dedicação a outra atividade, mudança na rotina familiar, afastamento de saúde ou outro).
3. Revise e confirme. O histórico de presença continua guardado.
4. Se a pessoa voltar, clique em Reativar matrícula.

## Cadastrar turmas

1. Abra Turmas.
2. Clique em Cadastrar turma.
3. Informe o nome, os dias da semana e o horário.
4. Revise e confirme.

## Criar aula

1. Abra Encontros.
2. Clique em Cadastrar encontro.
3. Escolha a atividade.
4. Informe a data.
5. Salve.

## Trocar de turma

1. Abra o participante.
2. Na matrícula ativa, clique em Trocar turma e escolha a nova turma.
3. Revise e confirme. As presenças anteriores continuam no histórico; a frequência do mês conta cada turma só no período em que a pessoa esteve nela.

## Anexar atestado ou laudo

1. Abra o participante e vá até Atestados e laudos.
2. Escolha o tipo e o arquivo (PDF ou foto, até 5 MB) e clique em Anexar arquivo.
3. Na chamada, ao marcar Falta avisada, use Anexar atestado na linha da pessoa; o arquivo é enviado ao salvar.

## Aula que não aconteceu

1. Abra a chamada da aula.
2. Toque em Aula não aconteceu e escolha o motivo (feriado, greve, condições climáticas ou outro).
3. Confirme. A aula não conta na frequência de ninguém. Use Desfazer se foi engano.

## Fazer chamada

1. Toque em Fazer chamada (botão vermelho no topo). O sistema abre a turma do horário atual; se não houver aula agora, mostra as aulas de hoje e as anteriores sem chamada.
2. Toque no nome para alternar entre Presente, Falta e Falta avisada, ou use os botões da linha.
3. Use Marcar vazios como presentes para completar quem ainda não foi marcado.
4. Clique em Salvar chamada. O botão mostra quantas pessoas ainda faltam marcar.
5. Se a internet cair, as marcações ficam guardadas no aparelho até você salvar.

Quem já tem 3 faltas no mês aparece destacado na lista. A chamada completa, com busca e observação por pessoa, continua disponível em Chamada > Outra data.

## Corrigir chamada

1. Abra Chamada e escolha a aula (ou a turma e a data em Outra data).
2. Clique em Corrigir chamada.
3. Ajuste as marcações e salve.

## Início

A tela Início mostra a frequência do mês contra a meta de 60%, quem precisa de contato (mais de 3 faltas no mês), as chamadas pendentes, as chamadas de hoje e a frequência por turma. Use as setas para ver meses anteriores.

## Desativar ou excluir

1. Abra o participante, a turma ou a aula.
2. Escolha Excluir ou Remover/cancelar.
3. Leia a página de confirmação.
4. Se houver histórico relacionado, use Desativar ou Cancelar para preservar os dados.

## Consultar o histórico do sistema

1. Abra Histórico no menu.
2. Filtre por período, módulo, ação ou texto.
3. Consulte a linha do tempo. Os registros do histórico não podem ser editados nem excluídos.

## Consultar frequência

1. Abra Frequência.
2. Use os filtros desejados.
3. Consulte o resumo e o histórico.
4. Em "Frequência mensal por turma", escolha o mês para ver aulas previstas, percentual e quem precisa de contato.
5. Clique em uma turma para ver cada participante no mês. Mais de 3 faltas no mês aparece como "Contatar família".

## Relatórios

1. Abra Relatórios.
2. Filtre por atividade ou período.
3. Imprima pelo botão da tela ou exporte CSV.

## Acessibilidade

1. Abra Acessibilidade.
2. Escolha tamanho de fonte, contraste, modo escuro ou redução de animações.
3. Salve as preferências.

## Pedidos da coordenação (turmas reais, várias turmas, total anual, celular)

- **Turmas**: a grade oficial fica em um único arquivo, `src/main/java/com/ibmec/remomeurumo/config/TurmasOficiais.java`. Para mudar um horário, edite só essa lista; ao iniciar, o sistema cria as turmas que faltarem. No sábado há duas turmas: SÁB 09h e SÁB 10h30.
- **Várias turmas**: no cadastro, marque todas as turmas da pessoa. Na página dela, use **Adicionar turma** ou **Remover** em cada turma. A última turma só sai por **Registrar saída**. Cada adição ou remoção aparece em "Entradas e saídas" e no Histórico (filtro "Turma adicionada/removida").
- **Frequência**: as aulas previstas somam todas as turmas, cada uma contada a partir da entrada. A coluna **Presenças no ano** fica sempre visível, e o seletor **Mês / Ano** muda o período. Os meses aparecem sozinhos até o mês atual. A planilha (CSV) e a exportação para o REDCap trazem as presenças e as aulas previstas do ano.
- **Aula não aconteceu**: funciona igual no computador e no celular, com motivo, observação, Confirmar/Voltar e Desfazer. A aula de hoje só entra na conta depois que a chamada é feita.
- **Celular**: o sistema pode ser instalado como app. Veja Ajuda → Instalar no celular.
