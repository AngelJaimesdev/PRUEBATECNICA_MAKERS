package com.prueba.prestamos.domain.port;

import com.prueba.prestamos.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {

    Usuario guardar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorEmail(String email);

    List<Usuario> buscarTodos();

    boolean existeEmail(String email);

    void eliminar(Long id);
}
