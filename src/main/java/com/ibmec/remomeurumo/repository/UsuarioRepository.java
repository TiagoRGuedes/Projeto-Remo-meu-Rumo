package com.ibmec.remomeurumo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ibmec.remomeurumo.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLoginAndAtivoTrue(String login);

    boolean existsByLogin(String login);

    List<Usuario> findAllByOrderByNomeAsc();
}
