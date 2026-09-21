package com.example.bacteriaSystem.dto;

import com.example.bacteriaSystem.model.StatusIdentificacao;

public record IdentificacaoResponse(
        Long idBacteria, Double probCalculada,
        Integer posicao, String nomeCientifico,
        StatusIdentificacao status) {
}
