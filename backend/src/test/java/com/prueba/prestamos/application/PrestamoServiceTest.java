package com.prueba.prestamos.application;

import com.prueba.prestamos.domain.exception.AccesoDenegadoException;
import com.prueba.prestamos.domain.exception.RecursoNoEncontradoException;
import com.prueba.prestamos.domain.exception.ReglaNegocioException;
import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.domain.model.Prestamo;
import com.prueba.prestamos.domain.model.Rol;
import com.prueba.prestamos.domain.model.Usuario;
import com.prueba.prestamos.domain.port.PrestamoRepositoryPort;
import com.prueba.prestamos.domain.port.UsuarioRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock
    private PrestamoRepositoryPort prestamos;

    @Mock
    private UsuarioRepositoryPort usuarios;

    @InjectMocks
    private PrestamoService service;

    private Prestamo prestamo(EstadoPrestamo estado) {
        return new Prestamo(1L, 10L, "usuario@test.com", new BigDecimal("5000"), 12,
                estado, LocalDateTime.now(), null, 0L);
    }

    @Test
    void solicitarGuardaPrestamoPendienteDelUsuario() {
        Usuario usuario = new Usuario(10L, "Usuario", "usuario@test.com", "hash", Rol.USER);
        when(usuarios.buscarPorEmail("usuario@test.com")).thenReturn(Optional.of(usuario));
        when(prestamos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Prestamo resultado = service.solicitar("usuario@test.com", new BigDecimal("5000"), 12);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPrestamo.PENDIENTE);
        assertThat(resultado.getUsuarioId()).isEqualTo(10L);
    }

    @Test
    void solicitarConUsuarioInexistenteLanza404() {
        when(usuarios.buscarPorEmail("x@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitar("x@test.com", new BigDecimal("5000"), 12))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(prestamos, never()).guardar(any());
    }

    @Test
    void aprobarPendienteGuardaEstadoAprobado() {
        when(prestamos.buscarPorId(1L)).thenReturn(Optional.of(prestamo(EstadoPrestamo.PENDIENTE)));
        when(prestamos.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Prestamo resultado = service.aprobar(1L);

        assertThat(resultado.getEstado()).isEqualTo(EstadoPrestamo.APROBADO);
        verify(prestamos).guardar(resultado);
    }

    @Test
    void aprobarPrestamoYaRechazadoNoGuardaNada() {
        when(prestamos.buscarPorId(1L)).thenReturn(Optional.of(prestamo(EstadoPrestamo.RECHAZADO)));

        assertThatThrownBy(() -> service.aprobar(1L)).isInstanceOf(ReglaNegocioException.class);
        verify(prestamos, never()).guardar(any());
    }

    @Test
    void rechazarPrestamoInexistenteLanza404() {
        when(prestamos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rechazar(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    void usuarioNoPuedeVerPrestamoDeOtro() {
        when(prestamos.buscarPorId(1L)).thenReturn(Optional.of(prestamo(EstadoPrestamo.PENDIENTE)));

        assertThatThrownBy(() -> service.obtener(1L, "otro@test.com", false))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void adminPuedeVerCualquierPrestamo() {
        when(prestamos.buscarPorId(1L)).thenReturn(Optional.of(prestamo(EstadoPrestamo.PENDIENTE)));

        assertThat(service.obtener(1L, "admin@test.com", true).getId()).isEqualTo(1L);
    }
}
