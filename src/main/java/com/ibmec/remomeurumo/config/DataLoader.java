package com.ibmec.remomeurumo.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.HistoricoOperacaoRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

@Component
public class DataLoader implements CommandLineRunner {

    private static final List<String> NOMES_FICTICIOS = List.of(
            "Ana Souza", "Bruno Lima", "Camila Rocha", "Diego Martins", "Fernanda Alves", "Gabriel Costa",
            "Helena Ramos", "João Pereira", "Larissa Monteiro", "Marcos Vieira", "Natália Azevedo",
            "Otávio Nunes", "Patrícia Cardoso", "Rafael Mendes", "Sabrina Freitas", "Thiago Batista",
            "Aline Moreira", "Caio Teixeira", "Daniela Rezende", "Eduardo Paiva", "Flávia Moraes",
            "Gustavo Barros", "Isabela Campos", "Leandro Farias", "Marina Duarte", "Nelson Ribeiro",
            "Priscila Neves", "Renan Carvalho", "Sofia Tavares", "Vinícius Andrade", "Yasmin Correia",
            "André Pires", "Beatriz Fonseca", "César Lopes", "Débora Sales", "Felipe Araújo");

    private static final List<String> OBSERVACOES_FICTICIAS = List.of(
            "Cadastro fictício para simulação do sistema.",
            "Participante demonstrativo sem dados pessoais reais.",
            "Registro criado exclusivamente para apresentação acadêmica.",
            "Perfil fictício usado para testar frequência e relatórios.");

    private final boolean dadosDemonstracao;
    private final ParticipanteRepository participanteRepository;
    private final AtividadeRepository atividadeRepository;
    private final EncontroRepository encontroRepository;
    private final PresencaRepository presencaRepository;
    private final HistoricoOperacaoRepository historicoRepository;
    private final ParticipanteService participanteService;
    private final AtividadeService atividadeService;
    private final EncontroService encontroService;
    private final PresencaService presencaService;

    public DataLoader(@Value("${app.dados-demonstracao:false}") boolean dadosDemonstracao,
            ParticipanteRepository participanteRepository,
            AtividadeRepository atividadeRepository,
            EncontroRepository encontroRepository,
            PresencaRepository presencaRepository,
            HistoricoOperacaoRepository historicoRepository,
            ParticipanteService participanteService,
            AtividadeService atividadeService,
            EncontroService encontroService,
            PresencaService presencaService) {
        this.dadosDemonstracao = dadosDemonstracao;
        this.participanteRepository = participanteRepository;
        this.atividadeRepository = atividadeRepository;
        this.encontroRepository = encontroRepository;
        this.presencaRepository = presencaRepository;
        this.historicoRepository = historicoRepository;
        this.participanteService = participanteService;
        this.atividadeService = atividadeService;
        this.encontroService = encontroService;
        this.presencaService = presencaService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!dadosDemonstracao || !bancoVazio()) {
            return;
        }

        List<Participante> participantes = criarParticipantes();
        List<Atividade> atividades = criarAtividades();
        List<Encontro> encontros = criarEncontros(atividades);
        criarPresencas(encontros, participantes);

        List.of(8, 17, 28, 34).forEach(indice -> participanteService.desativar(participantes.get(indice).getId()));
        atividadeService.desativar(atividades.get(5).getId());
    }

    private boolean bancoVazio() {
        return participanteRepository.count() == 0
                && atividadeRepository.count() == 0
                && encontroRepository.count() == 0
                && presencaRepository.count() == 0
                && historicoRepository.count() == 0;
    }

    private List<Participante> criarParticipantes() {
        LocalDateTime agora = LocalDateTime.now();
        return java.util.stream.IntStream.range(0, NOMES_FICTICIOS.size())
                .mapToObj(indice -> {
                    Participante participante = new Participante(
                            NOMES_FICTICIOS.get(indice),
                            OBSERVACOES_FICTICIAS.get(indice % OBSERVACOES_FICTICIAS.size()));
                    participante.setDataCadastro(agora.minusDays(3L + indice * 2L));
                    participante.setDataAtualizacao(participante.getDataCadastro());
                    return participanteService.cadastrar(participante);
                })
                .toList();
    }

    private List<Atividade> criarAtividades() {
        return List.of(
                atividadeService.cadastrar(new Atividade("Treino demonstrativo de percurso",
                        "Atividade fictícia criada somente para simular encontros e presença.")),
                atividadeService.cadastrar(new Atividade("Oficina fictícia de técnica",
                        "Atividade demonstrativa sem vínculo com a programação oficial do Instituto.")),
                atividadeService.cadastrar(new Atividade("Condicionamento de exemplo",
                        "Cenário fictício para testar frequência, filtros e relatórios.")),
                atividadeService.cadastrar(new Atividade("Grupo demonstrativo de iniciação",
                        "Grupo inteiramente fictício usado na apresentação acadêmica.")),
                atividadeService.cadastrar(new Atividade("Circuito fictício de integração",
                        "Atividade de exemplo para validar a conexão entre os módulos.")),
                atividadeService.cadastrar(new Atividade("Acompanhamento demonstrativo",
                        "Registro fictício que será desativado para demonstrar preservação histórica.")));
    }

    private List<Encontro> criarEncontros(List<Atividade> atividades) {
        LocalDate hoje = LocalDate.now();
        List<Encontro> encontros = new java.util.ArrayList<>();
        for (int indice = 0; indice < 24; indice++) {
            Atividade atividade = atividades.get(indice % atividades.size());
            LocalDate data;
            StatusEncontro status;
            if (indice == 0) {
                data = hoje.plusDays(2);
                status = StatusEncontro.PLANEJADO;
            } else if (indice == 1) {
                data = hoje.plusDays(5);
                status = StatusEncontro.PLANEJADO;
            } else {
                data = hoje.minusDays((indice - 2L) * 4L + 1L);
                status = indice == 8 || indice == 17 ? StatusEncontro.CANCELADO : StatusEncontro.REALIZADO;
            }
            encontros.add(encontroService.criar(
                    atividade.getId(), data,
                    "Encontro fictício " + (indice + 1) + " de 24 para simulação do sistema.", status));
        }
        return encontros;
    }

    private void criarPresencas(List<Encontro> encontros, List<Participante> participantes) {
        int indiceEncontro = 0;
        for (Encontro encontro : encontros) {
            if (encontro.getStatus() != StatusEncontro.REALIZADO) {
                indiceEncontro++;
                continue;
            }
            Map<Long, StatusPresenca> situacoes = new LinkedHashMap<>();
            Map<Long, String> observacoes = new LinkedHashMap<>();
            for (int indiceParticipante = 0; indiceParticipante < participantes.size(); indiceParticipante++) {
                Participante participante = participantes.get(indiceParticipante);
                StatusPresenca status = statusFicticio(indiceParticipante, indiceEncontro);
                situacoes.put(participante.getId(), status);
                observacoes.put(participante.getId(), status == StatusPresenca.PRESENTE
                        ? ""
                        : "Situação fictícia registrada para demonstrar os indicadores.");
            }
            presencaService.registrarLote(encontro.getId(), situacoes, observacoes);
            indiceEncontro++;
        }
    }

    private StatusPresenca statusFicticio(int participante, int encontro) {
        int ciclo = participante < 12 ? 17 : participante < 28 ? 8 : 4;
        int valor = participante * 3 + encontro * 5;
        if (valor % ciclo == 0) {
            return StatusPresenca.AUSENTE;
        }
        if (valor % (ciclo + 3) == 0) {
            return StatusPresenca.JUSTIFICADA;
        }
        return StatusPresenca.PRESENTE;
    }
}
