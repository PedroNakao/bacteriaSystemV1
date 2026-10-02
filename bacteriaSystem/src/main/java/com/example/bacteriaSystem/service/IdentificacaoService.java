package com.example.bacteriaSystem.service;

import com.example.bacteriaSystem.exceptions.customExceptions.DadosInsuficientesException;
import com.example.bacteriaSystem.exceptions.customExceptions.RequisicaoInvalidaException;
import com.example.bacteriaSystem.exceptions.customExceptions.TestesInsuficientesExceptions;
import com.example.bacteriaSystem.model.*;
import com.example.bacteriaSystem.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IdentificacaoService {
    private static final double PROBABILIDADE_MINIMA = 1.0;
    private static final double PROBABILIDADE_MAXIMA = 99.0;

    @Autowired

    BacteriaRepository  bacteriaRepository;

    @Autowired
    TesteBioquimicoRepository testeRepository;

    @Autowired
    ProbabilidadeTesteRepository probabilidadeRepository;

    @Autowired
    IdentificacaoRepository identificacaoRepository;

    @Autowired
    RankingRepository rankingRepository;

    @Autowired
    ResultadoTesteIdentificacaoRepository resultadoTesteIdentificacaoRepository;

    @Transactional
    public List<RankingIdentificacao> algoritmoBayes(
            List<ResultadoTesteIdentificacao> resultadoTestesAplicados,
            Usuario usuario) {

        validaQtdTestes(resultadoTestesAplicados);

        List<Bacteria> bacterias = bacteriaRepository.findAll();
        if (bacterias.isEmpty()){
            throw new DadosInsuficientesException("Tabela de bactérias não preenchidas");
        }

        double pbPriori = 1.0/ bacterias.size();

        Double denominador =  bacterias.stream()
                .mapToDouble(bacteria -> {
                    Double probPorBacteria = resultadoTestesAplicados.stream()
                            .mapToDouble(teste ->{
                                BigDecimal probBD = probabilidadeRepository
                                        .pegarProbabilidade(bacteria.getId(), teste.getTesteBioquimico().getId());
                                if(probBD == null){
                                    return 1.0;
                                }

                                double prob = probBD.doubleValue();

                                double ajustada = Math.max(PROBABILIDADE_MINIMA, Math.min(PROBABILIDADE_MAXIMA, prob));
                                if (teste.getResultado() == ResultadoTeste.NEGATIVO){
                                    ajustada = 100 - ajustada;
                                }
                                if (teste.getResultado() == ResultadoTeste.VARIAVEL){
                                    ajustada = 50;
                                }

                                return ajustada/100;
                            }).reduce(1.0, (somaProb, prob) ->  prob * somaProb);

                    return probPorBacteria * pbPriori;
                }).sum();


        //Calcula probabilidade de cada bacteria
        List<Bacteria> bacteriasList = bacteriaRepository.findAll();
        if(bacteriasList.isEmpty() ){
            throw new DadosInsuficientesException("Tabela de bacterias não preenchidas");
        }
        RankingIdentificacao[] rankingBacteria = new RankingIdentificacao[bacteriasList.size()];
        int i = 0;
        for (i = 0; i < bacterias.size(); i++){
            Bacteria bac = bacterias.get(i);
            Double probPorBacteria1 = resultadoTestesAplicados.stream()
                    .mapToDouble(teste ->{
                        BigDecimal probBD = probabilidadeRepository
                                .pegarProbabilidade(bac.getId(), teste.getTesteBioquimico().getId());

                        if(probBD == null){
                            return 1.0;
                        }

                        double prob= probBD.doubleValue();

                        double ajustada = Math.max(PROBABILIDADE_MINIMA, Math.min(PROBABILIDADE_MAXIMA, prob));

                        if (teste.getResultado() == ResultadoTeste.NEGATIVO){
                            ajustada = 100 - ajustada;
                        }
                        if (teste.getResultado() == ResultadoTeste.VARIAVEL){
                            ajustada = 50;
                        }

                        return ajustada/100;
                    }).reduce(1.0, (somaProb, prob) ->  prob * somaProb);
            Double numerador = probPorBacteria1 * pbPriori;
            Double probBac = numerador / denominador;
            rankingBacteria[i] = new RankingIdentificacao();
            rankingBacteria[i].setBacteria(bac);
            rankingBacteria[i].setProbabilidadeCalculada(probBac);
        }




        // Ordenar ranking do maior para o menor
        i = 0;
        int j = 0;
        for (i = 0; i <rankingBacteria.length; i++){
            for(j = i+1; j < rankingBacteria.length; j++){
                if (rankingBacteria[i].getProbabilidadeCalculada() < rankingBacteria[j].getProbabilidadeCalculada()){
                    RankingIdentificacao buffer = new  RankingIdentificacao();
                    buffer = rankingBacteria[i];
                    rankingBacteria[i] = rankingBacteria[j];
                    rankingBacteria[j] = buffer;
                }
            }
        }

        for (i = 0; i <rankingBacteria.length; i++){
            rankingBacteria[i].setPosicao(i+1);
        }



        Identificacao novaIdentificacao = new Identificacao();
        novaIdentificacao.setDataHora(LocalDateTime.now());
        novaIdentificacao.setStatus(
                rankingBacteria[0].getProbabilidadeCalculada() >= 0.9
                        ? StatusIdentificacao.CONCLUIDA
                        : StatusIdentificacao.INCONCLUSIVA);

        // O usuario vem do controller, que e quem tem acesso ao contexto
        // de seguranca da requisicao. Manter o SecurityContextHolder fora
        // do service deixa a regra de negocio testavel sem subir o Spring.
        novaIdentificacao.setUsuario(usuario);

        identificacaoRepository.save(novaIdentificacao);

        for (ResultadoTesteIdentificacao resultado : resultadoTestesAplicados) {
            resultado.setIdentificacao(novaIdentificacao);
        }
        resultadoTesteIdentificacaoRepository.saveAll(resultadoTestesAplicados);

        List<RankingIdentificacao> ranking = List.of(rankingBacteria);
        for (RankingIdentificacao item : ranking) {
            item.setIdentificacao(novaIdentificacao);
        }
        rankingRepository.saveAll(ranking);

        return List.of(rankingBacteria);
    }



    public Boolean validaQtdTestes(List<ResultadoTesteIdentificacao> resultadoTestesAplicados){

        if (resultadoTestesAplicados == null || resultadoTestesAplicados.isEmpty()) {
            throw new RequisicaoInvalidaException("Nenhum resultado de teste foi informado.");
        }

        long testesDistintos = resultadoTestesAplicados.stream()
                .map(r -> r.getTesteBioquimico().getId())
                .distinct()
                .count();

        if (testesDistintos != resultadoTestesAplicados.size()) {
            throw new RequisicaoInvalidaException(
                    "Há testes repetidos entre os resultados enviados.");
        }

        long testesInformativos = resultadoTestesAplicados.stream()
                .filter(r -> r.getResultado() != ResultadoTeste.VARIAVEL)
                .count();

        if (testesInformativos < 5) {
            throw new TestesInsuficientesExceptions(
                    "São necessários ao menos 5"
                            + " testes com resultado positivo ou negativo. Informados: "
                            + testesInformativos
                            + " (resultados variáveis não alteram o cálculo).");
        }

        return true;
    }


}






//SELECT pt.percentualPositividade
//FROM probabilidadeTeste pt, testeBioquimico tb, bacteria b
//WHERE b.id = (:id_bacteria) AND t.id = (:id_teste)

//IF (r.resultadoPositivo =
