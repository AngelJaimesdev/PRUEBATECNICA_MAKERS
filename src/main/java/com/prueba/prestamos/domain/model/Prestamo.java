package com.prueba.prestamos.domain.model;

import com.prueba.prestamos.domain.exception.ReglaNegocioException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Getter
@AllArgsConstructor
public class Prestamo implements Serializable {

    public static final BigDecimal MONTO_MINIMO = new BigDecimal("1000");
    public static final BigDecimal MONTO_MAXIMO = new BigDecimal("500000000");
    public static final int PLAZO_MINIMO_MESES = 1;
    public static final int PLAZO_MAXIMO_MESES = 120;

    private Long id;
    private Long usuarioId;
    private String usuarioEmail;
    private BigDecimal monto;
    private Integer plazoMeses;
    private EstadoPrestamo estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaRespuesta;
    private Long version;

    public static Prestamo solicitar(Usuario usuario, BigDecimal monto, int plazoMeses) {
        if (monto == null || monto.compareTo(MONTO_MINIMO) < 0 || monto.compareTo(MONTO_MAXIMO) > 0) {
            throw new ReglaNegocioException("El monto debe estar entre " + MONTO_MINIMO + " y " + MONTO_MAXIMO);
        }
        if (plazoMeses < PLAZO_MINIMO_MESES || plazoMeses > PLAZO_MAXIMO_MESES) {
            throw new ReglaNegocioException("El plazo debe estar entre " + PLAZO_MINIMO_MESES + " y " + PLAZO_MAXIMO_MESES + " meses");
        }
        return new Prestamo(null, usuario.getId(), usuario.getEmail(), monto, plazoMeses,
                EstadoPrestamo.PENDIENTE, LocalDateTime.now(), null, null);
    }

    public void aprobar() {
        decidir(EstadoPrestamo.APROBADO);
    }

    public void rechazar() {
        decidir(EstadoPrestamo.RECHAZADO);
    }

    public void validarCancelable() {
        if (estado != EstadoPrestamo.PENDIENTE) {
            throw new ReglaNegocioException("Solo se pueden cancelar préstamos pendientes. Estado actual: " + estado);
        }
    }

    public boolean perteneceA(String email) {
        return usuarioEmail != null && usuarioEmail.equalsIgnoreCase(email);
    }

    private void decidir(EstadoPrestamo nuevoEstado) {
        if (estado != EstadoPrestamo.PENDIENTE) {
            throw new ReglaNegocioException("El préstamo " + id + " ya fue " + estado.name().toLowerCase()
                    + "; solo se pueden aprobar o rechazar préstamos pendientes");
        }
        this.estado = nuevoEstado;
        this.fechaRespuesta = LocalDateTime.now();
    }
}
