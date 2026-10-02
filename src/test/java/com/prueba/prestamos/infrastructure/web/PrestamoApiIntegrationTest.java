package com.prueba.prestamos.infrastructure.web;

import com.prueba.prestamos.application.PrestamoService;
import com.prueba.prestamos.domain.port.PrestamoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integración: levanta la aplicación completa (seguridad JWT real, H2, caché) y prueba por HTTP
 * con los usuarios del enunciado. Cada prueba crea su propio préstamo para no depender del orden.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PrestamoApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrestamoService prestamoService;

    @MockitoSpyBean
    private PrestamoRepositoryPort prestamoRepository;

    private String login(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    private long solicitar(String tokenUsuario) throws Exception {
        String json = mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", tokenUsuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":5000,"plazoMeses":12}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(json.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void loginConCredencialesInvalidasRetorna401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"usuario@test.com","password":"incorrecta"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Credenciales inválidas"));
    }

    @Test
    void loginDevuelveTokenYRol() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
    }

    @Test
    void sinTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/prestamos/mios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioNoPuedeAprobarPrestamos() throws Exception {
        String usuario = login("usuario@test.com");
        long id = solicitar(usuario);

        mockMvc.perform(patch("/api/prestamos/{id}/aprobar", id).header("Authorization", usuario))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminApruebaYNoPuedeDecidirDosVeces() throws Exception {
        long id = solicitar(login("usuario@test.com"));
        String admin = login("admin@test.com");

        mockMvc.perform(patch("/api/prestamos/{id}/aprobar", id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADO"));

        mockMvc.perform(patch("/api/prestamos/{id}/rechazar", id).header("Authorization", admin))
                .andExpect(status().isConflict());
    }

    @Test
    void solicitudInvalidaRetorna400ConDetallePorCampo() throws Exception {
        mockMvc.perform(post("/api/prestamos")
                        .header("Authorization", login("usuario@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":-5,"plazoMeses":500}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.monto").exists())
                .andExpect(jsonPath("$.errores.plazoMeses").exists());
    }

    @Test
    void usuarioNoPuedeAdministrarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios").header("Authorization", login("usuario@test.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultaDeMisPrestamosSeSirveDesdeCache() {
        prestamoService.solicitar("usuario@test.com", new BigDecimal("5000"), 12); // invalida la caché
        clearInvocations(prestamoRepository);

        prestamoService.listarDeUsuario("usuario@test.com");
        prestamoService.listarDeUsuario("usuario@test.com");
        prestamoService.listarDeUsuario("usuario@test.com");

        // 3 consultas, 1 sola ida a la base de datos
        verify(prestamoRepository, times(1)).buscarPorUsuarioEmail("usuario@test.com");
    }
}
