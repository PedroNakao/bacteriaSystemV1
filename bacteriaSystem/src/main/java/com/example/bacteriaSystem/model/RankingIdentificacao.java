package com.example.bacteriaSystem.model;

import lombok.*;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RankingIdentificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Relação RankingIdentificacao : Identificacao
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="identificacao")
    private Identificacao identificacao;

    // Relação RankingIdentificacao : Bacteria
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="bacteria")
    private Bacteria bacteria;

    @Column(nullable = false)
    private Double probabilidadeCalculada;

    @Column(nullable = false)
    private Integer posicao;


}
