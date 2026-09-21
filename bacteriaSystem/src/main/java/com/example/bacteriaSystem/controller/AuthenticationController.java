package com.example.bacteriaSystem.controller;

import com.example.bacteriaSystem.config.security.TokenService;
import com.example.bacteriaSystem.dto.AuthenticationDTO;
import com.example.bacteriaSystem.dto.LoginResponseDTO;
import com.example.bacteriaSystem.dto.RegisterDTO;
import com.example.bacteriaSystem.exceptions.customExceptions.EmailJaExisteException;
import com.example.bacteriaSystem.model.Usuario;
import com.example.bacteriaSystem.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenService tokenService;


    @PostMapping("/login")
    public ResponseEntity login(@RequestBody AuthenticationDTO usuario) {
        var auth = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken
                        (usuario.login(), usuario.password()));

        var token = tokenService.generateToken((Usuario) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponseDTO(token));
    }


    @PostMapping("/register")
    public ResponseEntity register(@RequestBody RegisterDTO data){
        if (usuarioRepository.findByEmail(data.login()) != null) {
            throw new EmailJaExisteException("E-mail já cadastrado");
        }

        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        Usuario usuario = new Usuario(data.nome(), data.login(), encryptedPassword, data.role());

        this.usuarioRepository.save(usuario);

        return ResponseEntity.ok().build();

    }


}
