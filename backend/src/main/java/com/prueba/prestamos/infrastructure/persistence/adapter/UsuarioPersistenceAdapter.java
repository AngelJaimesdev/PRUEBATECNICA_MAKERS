package com.prueba.prestamos.infrastructure.persistence.adapter;

import com.prueba.prestamos.domain.model.Usuario;
import com.prueba.prestamos.domain.port.UsuarioRepositoryPort;
import com.prueba.prestamos.infrastructure.persistence.entity.UsuarioEntity;
import com.prueba.prestamos.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repository;

    @Override
    public Usuario guardar(Usuario usuario) {
        return aDominio(repository.save(aEntidad(usuario)));
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id).map(UsuarioPersistenceAdapter::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return repository.findByEmail(email).map(UsuarioPersistenceAdapter::aDominio);
    }

    @Override
    public List<Usuario> buscarTodos() {
        return repository.findAll().stream().map(UsuarioPersistenceAdapter::aDominio).toList();
    }

    @Override
    public boolean existeEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public void eliminar(Long id) {
        repository.deleteById(id);
    }

    static Usuario aDominio(UsuarioEntity e) {
        return new Usuario(e.getId(), e.getNombre(), e.getEmail(), e.getPasswordHash(), e.getRol());
    }

    private static UsuarioEntity aEntidad(Usuario u) {
        UsuarioEntity e = new UsuarioEntity();
        e.setId(u.getId());
        e.setNombre(u.getNombre());
        e.setEmail(u.getEmail());
        e.setPasswordHash(u.getPasswordHash());
        e.setRol(u.getRol());
        return e;
    }
}
