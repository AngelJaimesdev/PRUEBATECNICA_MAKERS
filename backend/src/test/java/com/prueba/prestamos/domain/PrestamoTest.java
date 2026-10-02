package com.prueba.prestamos.domain;

import com.prueba.prestamos.domain.exception.ReglaNegocioException;
import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.domain.model.Prestamo;
import com.prueba.prestamos.domain.model.Rol;
import com.prueba.prestamos.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrestamoTest {

    private final Usuario usuario = new Usuario(1L, "Usuario", "usuario@test.com", "hash", Rol.USER);

    @Test
    void solicitudNuevaNacePendiente() {
        Prestamo prestamo = Prestamo.solicitar(usuario, new BigDecimal("5000"), 12);

        assertThat(prestamo.getEstado()).isEqualTo(EstadoPrestamo.PENDIENTE);
        assertThat(prestamo.getUsuarioEmail()).isEqualTo("usuario@test.com");
        assertThat(prestamo.getFechaSolicitud()).isNotNull();
        assertThat(prestamo.getFechaRespuesta()).isNull();
    }

    @ParameterizedTest(name = "monto={0}, plazo={1} es inválido")
    @CsvSource({"999, 12", "0, 12", "-100, 12", "500000001, 12", "5000, 0", "5000, 121"})
    void rechazaMontosYPlazosFueraDeRango(String monto, int plazo) {
        assertThatThrownBy(() -> Prestamo.solicitar(usuario, new BigDecimal(monto), plazo))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void aprobarPendienteCambiaEstadoYRegistraFecha() {
        Prestamo prestamo = Prestamo.solicitar(usuario, new BigDecimal("5000"), 12);

        prestamo.aprobar();

        assertThat(prestamo.getEstado()).isEqualTo(EstadoPrestamo.APROBADO);
        assertThat(prestamo.getFechaRespuesta()).isNotNull();
    }

    @Test
    void noSePuedeAprobarUnPrestamoRechazado() {
        Prestamo prestamo = Prestamo.solicitar(usuario, new BigDecimal("5000"), 12);
        prestamo.rechazar();

        assertThatThrownBy(prestamo::aprobar)
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("rechazado");
        assertThat(prestamo.getEstado()).isEqualTo(EstadoPrestamo.RECHAZADO);
    }

    @Test
    void noSePuedeRechazarUnPrestamoAprobado() {
        Prestamo prestamo = Prestamo.solicitar(usuario, new BigDecimal("5000"), 12);
        prestamo.aprobar();

        assertThatThrownBy(prestamo::rechazar).isInstanceOf(ReglaNegocioException.class);
    }
}
