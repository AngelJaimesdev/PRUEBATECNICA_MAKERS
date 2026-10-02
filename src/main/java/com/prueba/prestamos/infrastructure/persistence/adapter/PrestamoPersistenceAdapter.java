package com.prueba.prestamos.infrastructure.persistence.adapter;

import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.domain.model.Prestamo;
import com.prueba.prestamos.domain.port.PrestamoRepositoryPort;
import com.prueba.prestamos.infrastructure.persistence.entity.PrestamoEntity;
import com.prueba.prestamos.infrastructure.persistence.repository.PrestamoJpaRepository;
import com.prueba.prestamos.infrastructure.persistence.repository.UsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrestamoPersistenceAdapter implements PrestamoRepositoryPort {

    private final PrestamoJpaRepository repository;
    private final UsuarioJpaRepository usuarioRepository;

    @Override
    public Prestamo guardar(Prestamo prestamo) {
        return aDominio(repository.save(aEntidad(prestamo)));
    }

    @Override
    public Optional<Prestamo> buscarPorId(Long id) {
        return repository.findById(id).map(PrestamoPersistenceAdapter::aDominio);
    }

    @Override
    public List<Prestamo> buscarPorUsuarioEmail(String email) {
        log.info("Consultando en BD los préstamos de {}", email);
        return repository.findByUsuarioEmail(email).stream().map(PrestamoPersistenceAdapter::aDominio).toList();
    }

    @Override
    public List<Prestamo> buscarTodos() {
        return repository.findAllConUsuario().stream().map(PrestamoPersistenceAdapter::aDominio).toList();
    }

    @Override
    public List<Prestamo> buscarPorEstado(EstadoPrestamo estado) {
        return repository.findByEstadoConUsuario(estado).stream().map(PrestamoPersistenceAdapter::aDominio).toList();
    }

    @Override
    public boolean existenDelUsuario(Long usuarioId) {
        return repository.existsByUsuarioId(usuarioId);
    }

    private static Prestamo aDominio(PrestamoEntity e) {
        return new Prestamo(e.getId(), e.getUsuario().getId(), e.getUsuario().getEmail(), e.getMonto(),
                e.getPlazoMeses(), e.getEstado(), e.getFechaSolicitud(), e.getFechaRespuesta(), e.getVersion());
    }

    private PrestamoEntity aEntidad(Prestamo p) {
        PrestamoEntity e = new PrestamoEntity();
        e.setId(p.getId());
        e.setUsuario(usuarioRepository.getReferenceById(p.getUsuarioId()));
        e.setMonto(p.getMonto());
        e.setPlazoMeses(p.getPlazoMeses());
        e.setEstado(p.getEstado());
        e.setFechaSolicitud(p.getFechaSolicitud());
        e.setFechaRespuesta(p.getFechaRespuesta());
        e.setVersion(p.getVersion());
        return e;
    }
}
