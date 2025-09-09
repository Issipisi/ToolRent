package com.toolrent.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test Controller", description = "Endpoints para pruebas del sistema")
public class TestController {

    @GetMapping
    @Operation(summary = "Endpoint de prueba",
            description = "Retorna un mensaje de confirmación que el sistema está funcionando")
    public String test() {
        return "✅ Spring Boot is working!";
    }


    @GetMapping("/private")
    public Map<String,String> privado(Authentication auth) {
        String nombre;
        if (auth instanceof JwtAuthenticationToken jwt) {
            nombre = jwt.getToken().getClaimAsString("preferred_username");
        } else if (auth instanceof OAuth2AuthenticationToken oauth) {
            nombre = oauth.getPrincipal().getAttribute("preferred_username");
        } else {
            nombre = auth.getName();
        }
        return Map.of("usuario", nombre);
    }
}