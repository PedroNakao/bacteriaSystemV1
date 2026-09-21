package com.example.bacteriaSystem.dto;

import com.example.bacteriaSystem.model.ResultadoTeste;
import jakarta.validation.constraints.NotNull;

public record IdentificacaoRequest(

        @NotNull(message = "idTeste é obrigatório")
        Long idTeste,

        @NotNull(message = "resultado é obrigatório (POSITIVO, NEGATIVO ou VARIAVEL)")
        ResultadoTeste resultado

) {
}
