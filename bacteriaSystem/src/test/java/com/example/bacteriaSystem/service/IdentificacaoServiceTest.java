package com.example.bacteriaSystem.service;

import com.example.bacteriaSystem.exceptions.customExceptions.RequisicaoInvalidaException;
import com.example.bacteriaSystem.exceptions.customExceptions.TestesInsuficientesExceptions;
import com.example.bacteriaSystem.model.Bacteria;
import com.example.bacteriaSystem.model.Identificacao;
import com.example.bacteriaSystem.model.RankingIdentificacao;
import com.example.bacteriaSystem.model.ResultadoTeste;
import com.example.bacteriaSystem.model.ResultadoTesteIdentificacao;
import com.example.bacteriaSystem.model.StatusIdentificacao;
import com.example.bacteriaSystem.model.TesteBioquimico;
import com.example.bacteriaSystem.repository.BacteriaRepository;
import com.example.bacteriaSystem.repository.IdentificacaoRepository;
import com.example.bacteriaSystem.repository.ProbabilidadeTesteRepository;
import com.example.bacteriaSystem.repository.RankingRepository;
import com.example.bacteriaSystem.repository.ResultadoTesteIdentificacaoRepository;
import com.example.bacteriaSystem.repository.TesteBioquimicoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes do algoritmoBayes com mocks — nao sobe Spring nem banco.
 *
 * Formula verificada em cada caso:
 *   P(bacteria | evidencias) = [ produto das verossimilhancas x priori ]
 *                              / [ soma dos numeradores de todas as especies ]
 *
 * Verossimilhanca de cada teste, conforme o IdentificacaoService:
 *   POSITIVO -> p            (p = percentual limitado ao intervalo 1..99)
 *   NEGATIVO -> 100 - p
 *   VARIAVEL -> 50 fixo      (constante, some na normalizacao)
 *   sem registro -> 1.0      (neutro, nao entra no produto)
 *
 * Todos os casos de sucesso usam no minimo 5 testes informativos, porque a
 * RN06 rejeita identificacoes abaixo desse limite.
 */
@ExtendWith(MockitoExtension.class)
class IdentificacaoServiceTest {

    private static final double TOLERANCIA = 0.001;

    @Mock
    BacteriaRepository bacteriaRepository;

    @Mock
    TesteBioquimicoRepository testeRepository;

    @Mock
    ProbabilidadeTesteRepository probabilidadeRepository;

    @Mock
    IdentificacaoRepository identificacaoRepository;

    @Mock
    RankingRepository rankingRepository;

    @Mock
    ResultadoTesteIdentificacaoRepository resultadoTesteIdentificacaoRepository;

    @InjectMocks
    IdentificacaoService service;

    @Captor
    ArgumentCaptor<Identificacao> capturaIdentificacao;

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private Bacteria bacteria(long id, String nome) {
        Bacteria b = new Bacteria();
        b.setId(id);
        b.setNomeCientifico(nome);
        return b;
    }

    private TesteBioquimico teste(long id, String nome) {
        TesteBioquimico t = new TesteBioquimico();
        t.setId(id);
        t.setNome(nome);
        return t;
    }

    private ResultadoTesteIdentificacao resultado(TesteBioquimico teste, ResultadoTeste valor) {
        ResultadoTesteIdentificacao r = new ResultadoTesteIdentificacao();
        r.setTesteBioquimico(teste);
        r.setResultado(valor);
        return r;
    }

    private void stub(long idBacteria, long idTeste, double percentual) {
        when(probabilidadeRepository.pegarProbabilidade(idBacteria, idTeste))
                .thenReturn(BigDecimal.valueOf(percentual));
    }

    private void imprime(String titulo, List<RankingIdentificacao> ranking) {
        System.out.println("=== " + titulo + " ===");
        for (RankingIdentificacao r : ranking) {
            System.out.printf("%d\u00ba - %s: %.4f (%.2f%%)%n",
                    r.getPosicao(),
                    r.getBacteria().getNomeCientifico(),
                    r.getProbabilidadeCalculada(),
                    r.getProbabilidadeCalculada() * 100);
        }
        System.out.println();
    }

    /** Cinco testes com ids 1..5, todos com o mesmo resultado. */
    private List<ResultadoTesteIdentificacao> cincoTestes(ResultadoTeste valor) {
        List<ResultadoTesteIdentificacao> lista = new ArrayList<>();
        for (long id = 1; id <= 5; id++) {
            lista.add(resultado(teste(id, "Teste" + id), valor));
        }
        return lista;
    }

    // ------------------------------------------------------------------
    // Caso 1 — cinco testes positivos
    // L(A) = .90*.80*.70*.60*.50 = 0.1512
    // L(B) = .20*.30*.10*.40*.50 = 0.0012
    // P(A) = 0.9921   P(B) = 0.0079
    // ------------------------------------------------------------------
    @Test
    void cincoTestesPositivos_especieComMaiorPositividadeVence() {
        Bacteria a = bacteria(1L, "Bacteria A");
        Bacteria b = bacteria(2L, "Bacteria B");
        when(bacteriaRepository.findAll()).thenReturn(List.of(a, b));

        double[] pctA = {90, 80, 70, 60, 50};
        double[] pctB = {20, 30, 10, 40, 50};
        for (int i = 0; i < 5; i++) {
            stub(1L, i + 1, pctA[i]);
            stub(2L, i + 1, pctB[i]);
        }

        List<RankingIdentificacao> ranking =
                service.algoritmoBayes(cincoTestes(ResultadoTeste.POSITIVO));
        imprime("Caso 1: cinco testes positivos", ranking);

        assertEquals(2, ranking.size());
        assertEquals("Bacteria A", ranking.get(0).getBacteria().getNomeCientifico());
        assertEquals(1, ranking.get(0).getPosicao());
        assertEquals(0.9921, ranking.get(0).getProbabilidadeCalculada(), TOLERANCIA);
        assertEquals("Bacteria B", ranking.get(1).getBacteria().getNomeCientifico());
        assertEquals(2, ranking.get(1).getPosicao());
        assertEquals(0.0079, ranking.get(1).getProbabilidadeCalculada(), TOLERANCIA);
    }

    // ------------------------------------------------------------------
    // Caso 2 — seis testes misturando positivo e negativo, tres especies
    // Marcados: T1+ T2+ T3- T4+ T5- T6+
    // L(P) = .80*.60*.10*.70*.60*.20 = 0.004032
    // L(Q) = .30*.50*.80*.60*.20*.70 = 0.010080
    // L(R) = .10*.10*.50*.20*.70*.90 = 0.000630
    // P(Q) = 0.6838   P(P) = 0.2735   P(R) = 0.0427
    // ------------------------------------------------------------------
    @Test
    void testesPositivosENegativosMisturados_ordenaCorretamente() {
        Bacteria p = bacteria(11L, "Bacteria P");
        Bacteria q = bacteria(12L, "Bacteria Q");
        Bacteria r = bacteria(13L, "Bacteria R");
        when(bacteriaRepository.findAll()).thenReturn(List.of(p, q, r));

        double[] pctP = {80, 60, 90, 70, 40, 20};
        double[] pctQ = {30, 50, 20, 60, 80, 70};
        double[] pctR = {10, 10, 50, 20, 30, 90};
        for (int i = 0; i < 6; i++) {
            stub(11L, i + 1, pctP[i]);
            stub(12L, i + 1, pctQ[i]);
            stub(13L, i + 1, pctR[i]);
        }

        ResultadoTeste[] marcacoes = {
                ResultadoTeste.POSITIVO, ResultadoTeste.POSITIVO, ResultadoTeste.NEGATIVO,
                ResultadoTeste.POSITIVO, ResultadoTeste.NEGATIVO, ResultadoTeste.POSITIVO
        };
        List<ResultadoTesteIdentificacao> resultados = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            resultados.add(resultado(teste(i + 1, "Teste" + (i + 1)), marcacoes[i]));
        }

        List<RankingIdentificacao> ranking = service.algoritmoBayes(resultados);
        imprime("Caso 2: testes mistos (+/-), tres especies", ranking);

        assertEquals("Bacteria Q", ranking.get(0).getBacteria().getNomeCientifico());
        assertEquals(0.6838, ranking.get(0).getProbabilidadeCalculada(), TOLERANCIA);
        assertEquals("Bacteria P", ranking.get(1).getBacteria().getNomeCientifico());
        assertEquals(0.2735, ranking.get(1).getProbabilidadeCalculada(), TOLERANCIA);
        assertEquals("Bacteria R", ranking.get(2).getBacteria().getNomeCientifico());
        assertEquals(0.0427, ranking.get(2).getProbabilidadeCalculada(), TOLERANCIA);
    }

    // ------------------------------------------------------------------
    // Caso 3 — documenta a decisao de projeto sobre o VARIAVEL.
    // Acrescentar um teste marcado como VARIAVEL nao altera a posterior,
    // mesmo quando as especies tem percentuais muito diferentes nele
    // (10% x 95%), porque a verossimilhanca fixa em 0,5 e um fator
    // constante que desaparece na normalizacao.
    // ------------------------------------------------------------------
    @Test
    void resultadoVariavel_naoAlteraAsProbabilidades() {
        Bacteria a = bacteria(1L, "Bacteria A");
        Bacteria b = bacteria(2L, "Bacteria B");
        when(bacteriaRepository.findAll()).thenReturn(List.of(a, b));

        double[] pctA = {90, 80, 70, 60, 50};
        double[] pctB = {20, 30, 10, 40, 50};
        for (int i = 0; i < 5; i++) {
            stub(1L, i + 1, pctA[i]);
            stub(2L, i + 1, pctB[i]);
        }
        stub(1L, 9L, 10);
        stub(2L, 9L, 95);

        List<ResultadoTesteIdentificacao> comVariavel = cincoTestes(ResultadoTeste.POSITIVO);
        comVariavel.add(resultado(teste(9L, "TesteVariavel"), ResultadoTeste.VARIAVEL));

        List<RankingIdentificacao> ranking = service.algoritmoBayes(comVariavel);
        imprime("Caso 3: cinco positivos + um variavel", ranking);

        // Valores identicos aos do Caso 1, que nao tinha o teste variavel.
        assertEquals("Bacteria A", ranking.get(0).getBacteria().getNomeCientifico());
        assertEquals(0.9921, ranking.get(0).getProbabilidadeCalculada(), TOLERANCIA);
        assertEquals(0.0079, ranking.get(1).getProbabilidadeCalculada(), TOLERANCIA);
    }

    // ------------------------------------------------------------------
    // Caso 4 — par (especie, teste) sem registro na tabela de
    // probabilidades. A verossimilhanca vira 1.0 e o teste simplesmente
    // nao participa do calculo, em vez de quebrar com NullPointerException.
    // ------------------------------------------------------------------
    @Test
    void probabilidadeNaoCadastrada_naoQuebraOCalculo() {
        Bacteria a = bacteria(1L, "Bacteria A");
        Bacteria b = bacteria(2L, "Bacteria B");
        when(bacteriaRepository.findAll()).thenReturn(List.of(a, b));

        double[] pctA = {90, 80, 70, 60};
        double[] pctB = {20, 30, 10, 40};
        for (int i = 0; i < 4; i++) {
            stub(1L, i + 1, pctA[i]);
            stub(2L, i + 1, pctB[i]);
        }
        // O teste 5 nao existe para nenhuma das duas especies.
        when(probabilidadeRepository.pegarProbabilidade(1L, 5L)).thenReturn(null);
        when(probabilidadeRepository.pegarProbabilidade(2L, 5L)).thenReturn(null);

        List<RankingIdentificacao> ranking =
                service.algoritmoBayes(cincoTestes(ResultadoTeste.POSITIVO));
        imprime("Caso 4: par sem probabilidade cadastrada", ranking);

        assertEquals(0.9921, ranking.get(0).getProbabilidadeCalculada(), TOLERANCIA);
        assertEquals(0.0079, ranking.get(1).getProbabilidadeCalculada(), TOLERANCIA);
    }

    // ------------------------------------------------------------------
    // Caso 5 — o clamp de 1..99 impede que um percentual 0 elimine a
    // especie de forma irreversivel. Bacteria A tem 0% no primeiro teste
    // e mesmo assim permanece no ranking com probabilidade nao nula.
    // ------------------------------------------------------------------
    @Test
    void percentualZero_naoEliminaEspecieDoRanking() {
        Bacteria a = bacteria(1L, "Bacteria A");
        Bacteria b = bacteria(2L, "Bacteria B");
        when(bacteriaRepository.findAll()).thenReturn(List.of(a, b));

        double[] pctA = {0, 80, 70, 60, 50};
        double[] pctB = {100, 30, 10, 40, 50};
        for (int i = 0; i < 5; i++) {
            stub(1L, i + 1, pctA[i]);
            stub(2L, i + 1, pctB[i]);
        }

        List<RankingIdentificacao> ranking =
                service.algoritmoBayes(cincoTestes(ResultadoTeste.POSITIVO));
        imprime("Caso 5: clamp de 1 a 99 por cento", ranking);

        assertEquals("Bacteria B", ranking.get(0).getBacteria().getNomeCientifico());
        assertEquals(0.7795, ranking.get(0).getProbabilidadeCalculada(), TOLERANCIA);
        assertEquals("Bacteria A", ranking.get(1).getBacteria().getNomeCientifico());
        assertEquals(0.2205, ranking.get(1).getProbabilidadeCalculada(), TOLERANCIA);
        assertTrue(ranking.get(1).getProbabilidadeCalculada() > 0.0,
                "o clamp deve impedir probabilidade zero");
    }

    // ------------------------------------------------------------------
    // Caso 6 — RN08: acima de 90 por cento a identificacao e CONCLUIDA.
    // ------------------------------------------------------------------
    @Test
    void probabilidadeAcimaDoLimiar_marcaIdentificacaoComoConcluida() {
        Bacteria a = bacteria(1L, "Bacteria A");
        Bacteria b = bacteria(2L, "Bacteria B");
        when(bacteriaRepository.findAll()).thenReturn(List.of(a, b));

        double[] pctA = {90, 80, 70, 60, 50};
        double[] pctB = {20, 30, 10, 40, 50};
        for (int i = 0; i < 5; i++) {
            stub(1L, i + 1, pctA[i]);
            stub(2L, i + 1, pctB[i]);
        }

        service.algoritmoBayes(cincoTestes(ResultadoTeste.POSITIVO));

        verify(identificacaoRepository).save(capturaIdentificacao.capture());
        assertEquals(StatusIdentificacao.CONCLUIDA, capturaIdentificacao.getValue().getStatus());
    }

    // ------------------------------------------------------------------
    // Caso 7 — RN06: menos de cinco testes informativos e rejeitado.
    // Cinco resultados VARIAVEL nao valem como evidencia.
    // ------------------------------------------------------------------
    @Test
    void cincoResultadosVariaveis_saoRejeitadosPelaRN06() {
        assertThrows(TestesInsuficientesExceptions.class,
                () -> service.algoritmoBayes(cincoTestes(ResultadoTeste.VARIAVEL)));
    }

    @Test
    void menosDeCincoTestes_saoRejeitados() {
        List<ResultadoTesteIdentificacao> apenasTres = List.of(
                resultado(teste(1L, "Teste1"), ResultadoTeste.POSITIVO),
                resultado(teste(2L, "Teste2"), ResultadoTeste.POSITIVO),
                resultado(teste(3L, "Teste3"), ResultadoTeste.NEGATIVO));

        assertThrows(TestesInsuficientesExceptions.class,
                () -> service.algoritmoBayes(apenasTres));
    }

    // ------------------------------------------------------------------
    // Caso 8 — o mesmo teste enviado duas vezes e rejeitado, para que a
    // evidencia nao entre no produto elevada ao quadrado.
    // ------------------------------------------------------------------
    @Test
    void testeRepetido_eRejeitado() {
        TesteBioquimico t1 = teste(1L, "Teste1");
        List<ResultadoTesteIdentificacao> comRepetido = List.of(
                resultado(t1, ResultadoTeste.POSITIVO),
                resultado(t1, ResultadoTeste.NEGATIVO),
                resultado(teste(2L, "Teste2"), ResultadoTeste.POSITIVO),
                resultado(teste(3L, "Teste3"), ResultadoTeste.POSITIVO),
                resultado(teste(4L, "Teste4"), ResultadoTeste.POSITIVO),
                resultado(teste(5L, "Teste5"), ResultadoTeste.POSITIVO));

        assertThrows(RequisicaoInvalidaException.class,
                () -> service.algoritmoBayes(comRepetido));
    }

    @Test
    void listaVazia_eRejeitada() {
        assertThrows(RequisicaoInvalidaException.class,
                () -> service.algoritmoBayes(List.of()));
    }
}
