package com.example.bacteriaSystem.dto;

import com.example.bacteriaSystem.model.PerfilUsuario;

public record RegisterDTO(String nome, String login, String password, PerfilUsuario role) {
}
