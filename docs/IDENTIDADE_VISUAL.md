# Identidade visual do sistema administrativo

## Fontes oficiais consultadas

Pesquisa realizada em 28 de agosto de 2026:

- Site oficial: <https://www.remomeurumo.org.br/>
- Instagram oficial: <https://www.instagram.com/remomeurumo/>

O Instagram apresentou restrição de idade para navegação pública sem autenticação. Foram considerados a imagem pública do perfil e os metadados institucionais disponíveis, sem copiar a estrutura de comunicação da rede social.

## Assets oficiais incorporados

Os arquivos são servidos localmente pela aplicação, sem dependência de URL externa em tempo de execução.

| Arquivo local | Uso | Fonte oficial |
| --- | --- | --- |
| `static/img/brand/logo-irmr-branco.png` | Cabeçalho sobre fundo azul | <https://www.remomeurumo.org.br/_imagens/footer-logo-instituto.png> |
| `static/img/brand/logo-irmr-colorido.png` | Cabeçalho de impressão | <https://www.remomeurumo.org.br/_imagens/nav-logo-instituto.png> |
| `static/img/brand/simbolo-irmr.png` | Favicon | <https://www.remomeurumo.org.br/_imagens/nav-aba.png> |

As imagens mantêm formato, proporção, cores e transparência originais. Nenhuma versão foi redesenhada ou gerada por inteligência artificial.

## Paleta

As cores principais foram conferidas nos pixels dos assets oficiais e no CSS do site institucional:

| Token | Valor | Função no sistema |
| --- | --- | --- |
| `--brand-blue` | `#0066B1` | Ações principais, ícones e estados ativos |
| `--brand-blue-hover` | `#00538F` | Interação e contraste de links |
| `--brand-blue-deep` | `#003B67` | Cabeçalho e áreas institucionais |
| `--brand-red` | `#DC0332` | Acento da marca e ação principal de presença |
| `--brand-red-hover` | `#B9002A` | Interação do acento institucional |
| `--brand-sky` | `#E8F3FA` | Seleções e superfícies auxiliares |

Verde, âmbar e vermelho semântico foram definidos separadamente para sucesso, atenção e erro. O vermelho institucional não é usado isoladamente para comunicar erro.

## Direção de interface

- Estrutura administrativa clara, com navegação persistente e texto sempre visível.
- Azul como base estrutural; vermelho usado com parcimônia para movimento, assinatura e prioridade.
- Linhas curvas discretas no destaque da Visão geral evocam percurso e água sem reproduzir o site público.
- Cantos de até 8 px, sombras leves e superfícies neutras mantêm o caráter operacional.
- Tipografia usa a pilha local do sistema para desempenho, legibilidade e privacidade.
- Ícones Lucide são armazenados localmente e sempre acompanham texto nas ações que exigem interpretação.

## Acessibilidade e variações

O design system inclui estados próprios para modo escuro e alto contraste, preserva foco visível, navegação por teclado, aumento de fonte, redução de movimento, textos de status e CSS de impressão. A logo branca permanece sobre fundo escuro na interface; a versão colorida é usada sobre fundo branco na impressão.
