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
public class ResultadoTesteIdentificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Relação ResultadoTesteIdentificacao : Identificacao
    @ManyToOne
    @JoinColumn(name = "identificacao")
    private Identificacao identificacao;

    //Relação ResultadoTesteIdentificacao : TesteBioquimico
    @ManyToOne
    @JoinColumn(name = "testeBioquimico")
    private TesteBioquimico testeBioquimico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoTeste resultado;


}
