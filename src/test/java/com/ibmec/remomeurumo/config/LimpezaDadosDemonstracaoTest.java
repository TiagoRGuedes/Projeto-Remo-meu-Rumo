package com.ibmec.remomeurumo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Perfil;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.HistoricoOperacaoRepository;
import com.ibmec.remomeurumo.repository.MatriculaRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;
import com.ibmec.remomeurumo.repository.UsuarioRepository;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ChamadaService;
import com.ibmec.remomeurumo.service.HistoricoOperacaoService;
import com.ibmec.remomeurumo.service.MatriculaService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.UsuarioService;

@SpringBootTest
@Transactional
class LimpezaDadosDemonstracaoTest {

    @Autowired private LimpezaDadosDemonstracao limpeza;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private MatriculaService matriculaService;
    @Autowired private ChamadaService chamadaService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private HistoricoOperacaoService historicoService;
    @Autowired private ParticipanteRepository participanteRepository;
    @Autowired private MatriculaRepository matriculaRepository;
    @Autowired private EncontroRepository encontroRepository;
    @Autowired private PresencaRepository presencaRepository;
    @Autowired private AtividadeRepository atividadeRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private HistoricoOperacaoRepository historicoRepository;

    @Test
    void removeParticipantesEDependentesMantendoTurmasEUsuarios() {
        Atividade turma = atividadeService.cadastrar(new Atividade("Limpeza Turma", "",
                EnumSet.allOf(DiaSemana.class), LocalTime.of(9, 0)));
        Participante ana = participanteService.cadastrar(new Participante("Limpeza Ana", ""));
        LocalDate ontem = LocalDate.now().minusDays(1);
        matriculaService.matricular(ana.getId(), turma.getId(), ontem.minusDays(10));
        chamadaService.salvar(turma.getId(), ontem, Map.of(ana.getId(), StatusPresenca.PRESENTE));
        usuarioService.cadastrar("Prof Limpeza", "prof.limpeza", "Rumo-teste-91", Perfil.PROFESSOR);
        long turmas = atividadeRepository.count();
        long usuarios = usuarioRepository.count();

        int removidos = limpeza.executar();

        assertThat(removidos).isGreaterThanOrEqualTo(1);
        assertThat(participanteRepository.count()).isZero();
        assertThat(matriculaRepository.count()).isZero();
        assertThat(encontroRepository.count()).isZero();
        assertThat(presencaRepository.count()).isZero();
        assertThat(atividadeRepository.count()).isEqualTo(turmas);
        assertThat(usuarioRepository.count()).isEqualTo(usuarios);
        var historico = historicoService.listar(null, null, null, null, null);
        assertThat(historico).noneMatch(h -> h.getModulo() == ModuloHistorico.MATRICULAS
                || h.getModulo() == ModuloHistorico.PRESENCAS || h.getModulo() == ModuloHistorico.ENCONTROS);
        assertThat(historico).anyMatch(h -> h.getModulo() == ModuloHistorico.USUARIOS);
        assertThat(historico).anyMatch(h -> LimpezaDadosDemonstracao.MARCA.equals(h.getTipoEntidade())
                && h.getAcao() == AcaoHistorico.EXCLUSAO);
    }

    @Test
    void rodaUmaUnicaVez() {
        limpeza.executar();
        participanteService.cadastrar(new Participante("Limpeza Participante Real", ""));
        long registros = historicoRepository.count();

        assertThat(limpeza.executar()).isZero();
        assertThat(participanteRepository.count()).isEqualTo(1);
        assertThat(historicoRepository.count()).isEqualTo(registros);
    }
}
