package com.example.bacteriaSystem.model;

import lombok.*;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Bacteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nomeCientifico;

    @Column(nullable = false)
    private String genero;

    @Column(nullable = false)
    private String familia = "Enterobacteriaceae";

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String descricaoClinica;

    @Column(nullable = false)
    private Boolean ativo;


    //Relação Bacteria 1:N RankingIdentificacao
    @JsonIgnore
    @OneToMany(mappedBy = "bacteria")
    private List<RankingIdentificacao> rankingIdentificacao = new ArrayList<>();

    //Relação Bacteria : ProbabilidadeTeste
    @JsonIgnore
    @OneToMany(mappedBy = "bacteria")
    private List<ProbabilidadeTeste> probabilidadeTestes = new ArrayList<>();


}
