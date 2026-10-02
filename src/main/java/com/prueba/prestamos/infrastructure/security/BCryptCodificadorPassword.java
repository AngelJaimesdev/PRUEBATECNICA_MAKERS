package com.prueba.prestamos.infrastructure.security;

import com.prueba.prestamos.domain.port.CodificadorPasswordPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BCryptCodificadorPassword implements CodificadorPasswordPort {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String codificar(String passwordPlano) {
        return passwordEncoder.encode(passwordPlano);
    }
}
