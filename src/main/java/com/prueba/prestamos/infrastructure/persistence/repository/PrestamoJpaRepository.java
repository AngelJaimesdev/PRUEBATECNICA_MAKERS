package com.prueba.prestamos.infrastructure.persistence.repository;

import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.infrastructure.persistence.entity.PrestamoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface PrestamoJpaRepository extends JpaRepository<PrestamoEntity, Long> {

    @Query("select p from PrestamoEntity p join fetch p.usuario u where u.email = :email order by p.id desc")
    List<PrestamoEntity> findByUsuarioEmail(String email);

    @Query("select p from PrestamoEntity p join fetch p.usuario order by p.id desc")
    List<PrestamoEntity> findAllConUsuario();

    @Query("select p from PrestamoEntity p join fetch p.usuario where p.estado = :estado order by p.id desc")
    List<PrestamoEntity> findByEstadoConUsuario(EstadoPrestamo estado);

    boolean existsByUsuarioId(Long usuarioId);
}
