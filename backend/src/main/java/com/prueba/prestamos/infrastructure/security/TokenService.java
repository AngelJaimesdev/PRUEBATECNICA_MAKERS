package com.prueba.prestamos.infrastructure.security;

import com.prueba.prestamos.domain.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration duracion;

    public TokenService(JwtEncoder encoder, @Value("${app.jwt.expiracion-minutos}") long minutos) {
        this.encoder = encoder;
        this.duracion = Duration.ofMinutes(minutos);
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("prestamos-api")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(duracion))
                .subject(usuario.getEmail())
                .claim(JwtConfig.CLAIM_ROL, usuario.getRol().name())
                .claim(JwtConfig.CLAIM_NOMBRE, usuario.getNombre())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long segundosDeVigencia() {
        return duracion.toSeconds();
    }
}
