package com.example.bacteriaSystem.model;

import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Identificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Relação Identificacao : Usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario")
    private Usuario usuario;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusIdentificacao status;


    //Relação Identificacao : ResultadoIdentificacao
    @OneToMany(mappedBy = "identificacao")
    @Column
    private List<ResultadoTesteIdentificacao> resultadoTesteIdentificacaos = new ArrayList<>();

    //Relação Identificacao:RankingIdentificacao
    @OneToMany(mappedBy = "identificacao")
    @Column(nullable = false)
    private List<RankingIdentificacao> rankingIdentificacao = new ArrayList<>();



}
