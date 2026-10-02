package com.prueba.prestamos.infrastructure.web.error;

import com.prueba.prestamos.domain.exception.AccesoDenegadoException;
import com.prueba.prestamos.domain.exception.RecursoNoEncontradoException;
import com.prueba.prestamos.domain.exception.ReglaNegocioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos", errores);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<RespuestaError> peticionMalFormada(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "La petición tiene un formato o un valor inválido", null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespuestaError> credenciales(AuthenticationException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", null);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<RespuestaError> accesoDenegado(AccesoDenegadoException ex) {
        return respuesta(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<RespuestaError> concurrencia(OptimisticLockingFailureException ex) {
        return respuesta(HttpStatus.CONFLICT, "El registro fue modificado por otro usuario. Recargue e intente de nuevo", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> general(Exception ex) {
        if (ex instanceof ErrorResponse errorSpring) {
            HttpStatusCode status = errorSpring.getStatusCode();
            return respuesta(HttpStatus.valueOf(status.value()), errorSpring.getBody().getDetail(), null);
        }
        log.error("Error no controlado", ex);
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<RespuestaError> respuesta(HttpStatus status, String mensaje, Map<String, String> errores) {
        return ResponseEntity.status(status)
                .body(new RespuestaError(LocalDateTime.now(), status.value(), mensaje, errores));
    }
}
