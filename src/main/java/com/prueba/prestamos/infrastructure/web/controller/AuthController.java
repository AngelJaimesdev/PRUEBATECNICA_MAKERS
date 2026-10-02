package com.prueba.prestamos.infrastructure.web.controller;

import com.prueba.prestamos.application.UsuarioService;
import com.prueba.prestamos.domain.model.Usuario;
import com.prueba.prestamos.infrastructure.security.TokenService;
import com.prueba.prestamos.infrastructure.web.dto.LoginRequest;
import com.prueba.prestamos.infrastructure.web.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final TokenService tokenService;

 
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));

        Usuario usuario = usuarioService.obtenerPorEmail(request.email());
        return new LoginResponse(tokenService.generar(usuario), "Bearer", tokenService.segundosDeVigencia(),
                usuario.getNombre(), usuario.getEmail(), usuario.getRol());
    }
}
