package com.example.bacteriaSystem.service;

import com.example.bacteriaSystem.exceptions.customExceptions.TesteNaoEncontradoException;
import com.example.bacteriaSystem.model.ResultadoTeste;
import com.example.bacteriaSystem.model.ResultadoTesteIdentificacao;
import com.example.bacteriaSystem.model.TesteBioquimico;
import com.example.bacteriaSystem.repository.ResultadoTesteIdentificacaoRepository;
import com.example.bacteriaSystem.repository.TesteBioquimicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResultadoTesteService {

    @Autowired
    TesteBioquimicoRepository testeRepository;

    public ResultadoTesteIdentificacao criarResultadoTeste(Long idTeste, ResultadoTeste resultado) {
        ResultadoTesteIdentificacao novoResultado = new ResultadoTesteIdentificacao();
        novoResultado.setResultado(resultado);
        TesteBioquimico teste = testeRepository.findById(idTeste).orElseThrow(() -> new TesteNaoEncontradoException("Teste bioquímico não encontrado: id " + idTeste));

        novoResultado.setTesteBioquimico(teste);

        return novoResultado;
    }


}


//Teste não encontrado linha 19
//Bacterias não encontradas Identificação Service linha 35