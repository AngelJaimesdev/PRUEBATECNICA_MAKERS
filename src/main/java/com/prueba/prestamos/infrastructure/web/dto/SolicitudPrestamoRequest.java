package com.prueba.prestamos.infrastructure.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SolicitudPrestamoRequest(
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "1000", message = "El monto mínimo es 1.000")
        @DecimalMax(value = "500000000", message = "El monto máximo es 500.000.000")
        @Digits(integer = 13, fraction = 2, message = "El monto admite máximo 2 decimales")
        BigDecimal monto,

        @NotNull(message = "El plazo es obligatorio")
        @Min(value = 1, message = "El plazo mínimo es 1 mes")
        @Max(value = 120, message = "El plazo máximo es 120 meses")
        Integer plazoMeses
) {
}
