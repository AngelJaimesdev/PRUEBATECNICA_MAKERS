package com.prueba.prestamos.domain.port;

import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.domain.model.Prestamo;

import java.util.List;
import java.util.Optional;


public interface PrestamoRepositoryPort {

    Prestamo guardar(Prestamo prestamo);

    Optional<Prestamo> buscarPorId(Long id);

    List<Prestamo> buscarPorUsuarioEmail(String email);

    List<Prestamo> buscarTodos();

    List<Prestamo> buscarPorEstado(EstadoPrestamo estado);

    boolean existenDelUsuario(Long usuarioId);

    void eliminar(Long id);
}
