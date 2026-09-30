package com.sportcourt.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

/**
 * DTO para Usuario con validaciones
 *
 * El DTO solo comprueba que el email tenga estructura de correo válida.
 * El dominio permitido según el rol (gmail/hotmail para usuario,
 * _Administrador@sportcourt.com.pe para admin) es regla de negocio y se
 * valida en UsuarioService.
 */
public class UsuarioDTO {

    private Integer id;

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email no es válido")
    private String email;

    @NotBlank(message = "El rol es requerido")
    private String rol;

    // Solo de entrada: se acepta al crear/modificar, nunca se serializa en
    // las respuestas de la API.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Constructores
    public UsuarioDTO() {
    }

    public UsuarioDTO(Integer id, String nombre, String email, String rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
