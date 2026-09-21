package com.example.bacteriaSystem.service;


import com.example.bacteriaSystem.exceptions.customExceptions.UsuarioNaoEncontradoException;
import com.example.bacteriaSystem.model.Usuario;
import com.example.bacteriaSystem.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails usuario = usuarioRepository.findByEmail(username);
        if(usuario == null) {
            throw new UsuarioNaoEncontradoException("Usuario nao encontrado");
        }
        return usuario;
    }
}
