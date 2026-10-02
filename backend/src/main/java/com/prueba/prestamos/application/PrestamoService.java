package com.prueba.prestamos.application;

import com.prueba.prestamos.domain.exception.AccesoDenegadoException;
import com.prueba.prestamos.domain.exception.RecursoNoEncontradoException;
import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.domain.model.Prestamo;
import com.prueba.prestamos.domain.model.Usuario;
import com.prueba.prestamos.domain.port.PrestamoRepositoryPort;
import com.prueba.prestamos.domain.port.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    public static final String CACHE_PRESTAMOS_USUARIO = "prestamosUsuario";

    private final PrestamoRepositoryPort prestamos;
    private final UsuarioRepositoryPort usuarios;

    @Transactional
    @CacheEvict(cacheNames = CACHE_PRESTAMOS_USUARIO, key = "#email")
    public Prestamo solicitar(String email, BigDecimal monto, int plazoMeses) {
        Usuario usuario = usuarios.buscarPorEmail(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + email + " no encontrado"));
        return prestamos.guardar(Prestamo.solicitar(usuario, monto, plazoMeses));
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_PRESTAMOS_USUARIO, key = "#email")
    public List<Prestamo> listarDeUsuario(String email) {
        return prestamos.buscarPorUsuarioEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Prestamo> listarTodos(EstadoPrestamo estado) {
        return estado == null ? prestamos.buscarTodos() : prestamos.buscarPorEstado(estado);
    }

    @Transactional(readOnly = true)
    public Prestamo obtener(Long id, String email, boolean esAdmin) {
        Prestamo prestamo = buscar(id);
        if (!esAdmin && !prestamo.perteneceA(email)) {
            throw new AccesoDenegadoException("No tiene permiso para consultar este préstamo");
        }
        return prestamo;
    }

  
    @Transactional
    @CacheEvict(cacheNames = CACHE_PRESTAMOS_USUARIO, allEntries = true)
    public Prestamo aprobar(Long id) {
        Prestamo prestamo = buscar(id);
        prestamo.aprobar();
        return prestamos.guardar(prestamo);
    }

    @Transactional
    @CacheEvict(cacheNames = CACHE_PRESTAMOS_USUARIO, allEntries = true)
    public Prestamo rechazar(Long id) {
        Prestamo prestamo = buscar(id);
        prestamo.rechazar();
        return prestamos.guardar(prestamo);
    }

    private Prestamo buscar(Long id) {
        return prestamos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Préstamo " + id + " no encontrado"));
    }
}
