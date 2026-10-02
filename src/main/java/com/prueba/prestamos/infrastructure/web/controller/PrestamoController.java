package com.prueba.prestamos.infrastructure.web.controller;

import com.prueba.prestamos.application.PrestamoService;
import com.prueba.prestamos.domain.model.EstadoPrestamo;
import com.prueba.prestamos.infrastructure.web.dto.PrestamoResponse;
import com.prueba.prestamos.infrastructure.web.dto.SolicitudPrestamoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/prestamos")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService service;

    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrestamoResponse solicitar(@Valid @RequestBody SolicitudPrestamoRequest request, Authentication auth) {
        return PrestamoResponse.from(service.solicitar(auth.getName(), request.monto(), request.plazoMeses()));
    }

    @GetMapping("/mios")
    public List<PrestamoResponse> misPrestamos(Authentication auth) {
        return service.listarDeUsuario(auth.getName()).stream().map(PrestamoResponse::from).toList();
    }

    @GetMapping
    public List<PrestamoResponse> listar(@RequestParam(required = false) EstadoPrestamo estado) {
        return service.listarTodos(estado).stream().map(PrestamoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PrestamoResponse obtener(@PathVariable Long id, Authentication auth) {
        return PrestamoResponse.from(service.obtener(id, auth.getName(), esAdmin(auth)));
    }

    @PatchMapping("/{id}/aprobar")
    public PrestamoResponse aprobar(@PathVariable Long id) {
        return PrestamoResponse.from(service.aprobar(id));
    }

    @PatchMapping("/{id}/rechazar")
    public PrestamoResponse rechazar(@PathVariable Long id) {
        return PrestamoResponse.from(service.rechazar(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelar(@PathVariable Long id, Authentication auth) {
        service.cancelar(id, auth.getName());
    }

    private static boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
