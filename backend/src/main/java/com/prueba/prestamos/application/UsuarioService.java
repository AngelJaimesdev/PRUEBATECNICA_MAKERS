package com.prueba.prestamos.application;

import com.prueba.prestamos.domain.exception.RecursoNoEncontradoException;
import com.prueba.prestamos.domain.exception.ReglaNegocioException;
import com.prueba.prestamos.domain.model.Rol;
import com.prueba.prestamos.domain.model.Usuario;
import com.prueba.prestamos.domain.port.CodificadorPasswordPort;
import com.prueba.prestamos.domain.port.PrestamoRepositoryPort;
import com.prueba.prestamos.domain.port.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepositoryPort usuarios;
    private final PrestamoRepositoryPort prestamos;
    private final CodificadorPasswordPort codificador;

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.buscarTodos();
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return buscar(id);
    }

    @Transactional(readOnly = true)
    public Usuario obtenerPorEmail(String email) {
        return usuarios.buscarPorEmail(normalizar(email))
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + email + " no encontrado"));
    }

    @Transactional
    public Usuario crear(String nombre, String email, String password, Rol rol) {
        String emailNormalizado = normalizar(email);
        if (usuarios.existeEmail(emailNormalizado)) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + emailNormalizado);
        }
        if (password == null || password.isBlank()) {
            throw new ReglaNegocioException("La contraseña es obligatoria al crear un usuario");
        }
        return usuarios.guardar(new Usuario(null, nombre, emailNormalizado, codificador.codificar(password), rol));
    }

    @Transactional
    public Usuario actualizar(Long id, String nombre, String email, String password, Rol rol) {
        Usuario usuario = buscar(id);
        String emailNormalizado = normalizar(email);
        if (!usuario.getEmail().equals(emailNormalizado) && usuarios.existeEmail(emailNormalizado)) {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + emailNormalizado);
        }
        usuario.actualizarDatos(nombre, emailNormalizado, rol);
        if (password != null && !password.isBlank()) {
            usuario.cambiarPassword(codificador.codificar(password));
        }
        return usuarios.guardar(usuario);
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        if (prestamos.existenDelUsuario(id)) {
            throw new ReglaNegocioException("No se puede eliminar un usuario que tiene préstamos registrados");
        }
        usuarios.eliminar(id);
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no encontrado"));
    }

    private String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
