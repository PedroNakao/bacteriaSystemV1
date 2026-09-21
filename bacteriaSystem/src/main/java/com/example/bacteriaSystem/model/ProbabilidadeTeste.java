package com.example.bacteriaSystem.model;

import lombok.*;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
//ALTERAÇÃO DO CLAUDE PREMIUM
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"bacteria", "testeBioquimico"}))
public class ProbabilidadeTeste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal percentualPositividade;

    @Column
    private String fonteBibliografica;

    //Relação ProbabilidadeTeste : Bacteria
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bacteria", nullable = false)
    private Bacteria bacteria;


    //Relação ProbabilidadeTeste : TesteBioquimico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "testeBioquimico", nullable = false)
    private TesteBioquimico testeBioquimico;

}
