package com.example.bacteriaSystem.model;

public enum PerfilUsuario {

    ADMIN("admin"),
    PESQUISADOR("pesquisador");

    private String role;


    PerfilUsuario(String role) {
        this.role = role;
    }

    public String getRole(){
        return role;
    }
}
