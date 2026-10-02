package com.prueba.prestamos.infrastructure.config;

import com.prueba.prestamos.domain.model.Prestamo;
import com.prueba.prestamos.domain.model.Rol;
import com.prueba.prestamos.domain.model.Usuario;
import com.prueba.prestamos.domain.port.CodificadorPasswordPort;
import com.prueba.prestamos.domain.port.PrestamoRepositoryPort;
import com.prueba.prestamos.domain.port.UsuarioRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepositoryPort usuarios;
    private final PrestamoRepositoryPort prestamos;
    private final CodificadorPasswordPort codificador;

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.existeEmail("usuario@test.com")) {
            return;
        }
        Usuario usuario = usuarios.guardar(
                new Usuario(null, "Usuario", "usuario@test.com", codificador.codificar("123"), Rol.USER));
        usuarios.guardar(new Usuario(null, "Admin Admin", "admin@test.com", codificador.codificar("123"), Rol.ADMIN));

        prestamos.guardar(Prestamo.solicitar(usuario, new BigDecimal("1000"), 12));

        log.info("Datos iniciales cargados: usuario@test.com / admin@test.com (password 123)");
    }
}
