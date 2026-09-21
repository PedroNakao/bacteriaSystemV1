package com.example.bacteriaSystem.controller;


import com.example.bacteriaSystem.dto.IdentificacaoRequest;
import com.example.bacteriaSystem.dto.IdentificacaoResponse;
import com.example.bacteriaSystem.exceptions.customExceptions.RequisicaoInvalidaException;
import com.example.bacteriaSystem.model.Bacteria;
import com.example.bacteriaSystem.model.RankingIdentificacao;
import com.example.bacteriaSystem.model.ResultadoTesteIdentificacao;
import com.example.bacteriaSystem.model.TesteBioquimico;
import com.example.bacteriaSystem.repository.BacteriaRepository;
import com.example.bacteriaSystem.repository.TesteBioquimicoRepository;
import com.example.bacteriaSystem.service.IdentificacaoService;
import com.example.bacteriaSystem.service.ResultadoTesteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class IdentificacaoController {

    @Autowired
    private IdentificacaoService identificacaoService;

    @Autowired
    private ResultadoTesteService resultadoService;

    @Autowired
    private BacteriaRepository bacteriaRepository;

    @Autowired
    private TesteBioquimicoRepository testeRepository;

    @PostMapping("/identificacao")
    public ResponseEntity<List<IdentificacaoResponse>> retornaRanking( @RequestBody @Valid List<@Valid IdentificacaoRequest> req) {
            if (req == null || req.isEmpty()) {
                throw new RequisicaoInvalidaException("A requisição não pode estar vazia.");
            }

            for (IdentificacaoRequest item : req) {
                if (item == null || item.idTeste() == null || item.resultado() == null) {
                    throw new RequisicaoInvalidaException(
                        "Todos os itens devem informar idTeste e resultado.");
                }
            }

            for (int i = 0; i < req.size(); i++) {
                for (int j = i + 1; j < req.size(); j++) {
                    if (req.get(i).idTeste().equals(req.get(j).idTeste())) {
                        throw new RequisicaoInvalidaException(
                            "Cada teste bioquímico deve aparecer no máximo uma vez. "
                                    + "idTeste duplicado: " + req.get(i).idTeste());
                    }
                }
            }


            List<ResultadoTesteIdentificacao> listaResultado = new ArrayList<>();
            listaResultado = req.stream().map(resultado -> {
                ResultadoTesteIdentificacao newResult = resultadoService.criarResultadoTeste(resultado.idTeste(), resultado.resultado());
                return newResult;
            }).collect(Collectors.toList());

            List<RankingIdentificacao> ranking = identificacaoService.algoritmoBayes(listaResultado);
            List<IdentificacaoResponse> rankResponse = ranking.stream().map(item -> {
                IdentificacaoResponse newRank = new IdentificacaoResponse(item.getBacteria().getId(), item.getProbabilidadeCalculada(), item.getPosicao(), item.getBacteria().getNomeCientifico(), item.getIdentificacao().getStatus());
                return newRank;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(rankResponse);
    }


    @GetMapping("/bacterias")
    public ResponseEntity<List<Bacteria>> carregarBacterias(){
            List<Bacteria> bacterias = bacteriaRepository.findByAtivoTrue();
            return ResponseEntity.ok(bacterias);
    }


    @GetMapping("/testes")
    public ResponseEntity<List<TesteBioquimico>> carregarTestes(){
            List<TesteBioquimico> testes = testeRepository.findByAtivoTrue();
            return ResponseEntity.ok(testes);
    }





}
