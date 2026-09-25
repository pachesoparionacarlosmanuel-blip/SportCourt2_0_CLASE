package com.sportcourt.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/csrf")
@Tag(name = "CSRF", description = "Obtención del token CSRF para peticiones que modifican estado")
public class CsrfController {

    @GetMapping
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }
}