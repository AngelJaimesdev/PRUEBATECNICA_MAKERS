package com.prueba.prestamos.infrastructure.web.error;

import java.time.LocalDateTime;
import java.util.Map;

public record RespuestaError(
        LocalDateTime timestamp,
        int status,
        String mensaje,
        Map<String, String> errores
) {
}
