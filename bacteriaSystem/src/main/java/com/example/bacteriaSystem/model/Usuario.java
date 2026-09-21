package com.example.bacteriaSystem.model;

import lombok.*;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(nullable = false)
    private LocalDateTime dataCadastro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PerfilUsuario perfil;

    //Relação Usuario: Identificacao
    @OneToMany(mappedBy = "usuario")
    private List<Identificacao> identificacoes = new ArrayList<>();

    public Usuario(String nome, String login, String password, PerfilUsuario perfil) {
        this.nome = nome;
        this.email = login;
        this.senha = password;
        this.perfil = perfil;
        this.dataCadastro = LocalDateTime.now();
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(this.perfil == PerfilUsuario.ADMIN){
            return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }else{
            return List.of(new SimpleGrantedAuthority("ROLE_PESQUISADOR"));
        }
    }

    @Override
    public String getPassword() {
        return this.getSenha();
    }

    @Override
    public String getUsername() {
        return this.email;
    }
}
