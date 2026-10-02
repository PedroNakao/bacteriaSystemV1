package com.example.bacteriaSystem.service;

import com.example.bacteriaSystem.model.Bacteria;
import com.example.bacteriaSystem.model.RankingIdentificacao;
import com.example.bacteriaSystem.model.ResultadoTeste;
import com.example.bacteriaSystem.model.ResultadoTesteIdentificacao;
import com.example.bacteriaSystem.model.TesteBioquimico;
import com.example.bacteriaSystem.repository.BacteriaRepository;
import com.example.bacteriaSystem.repository.IdentificacaoRepository;
import com.example.bacteriaSystem.repository.ProbabilidadeTesteRepository;
import com.example.bacteriaSystem.repository.RankingRepository;
import com.example.bacteriaSystem.repository.ResultadoTesteIdentificacaoRepository;
import com.example.bacteriaSystem.repository.TesteBioquimicoRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Roda o IdentificacaoService.algoritmoBayes DE VERDADE contra o dataset dos
 * CSVs, usando mocks dos repositorios alimentados pelo proprio arquivo em vez
 * de valores fixos no codigo. Serve para conferir cenarios reais a mao antes
 * de ter o app Android pronto.
 *
 * Os CSVs usam ';' como delimitador, mesmo formato do DataLoader.
 * Fica em src/test/java porque depende do Mockito.
 *
 * Como usar: edite TESTES_MARCADOS abaixo e execute o main().
 * Lembre que a RN06 exige ao menos 5 testes POSITIVO ou NEGATIVO — resultados
 * VARIAVEL podem ser incluidos, mas nao contam para esse minimo nem alteram
 * o ranking.
 */
public class TesteManualRankingReal {

    private static final String DELIMITADOR = ";";

    @Mock BacteriaRepository bacteriaRepository;
    @Mock TesteBioquimicoRepository testeRepository;
    @Mock ProbabilidadeTesteRepository probabilidadeRepository;
    @Mock IdentificacaoRepository identificacaoRepository;
    @Mock RankingRepository rankingRepository;
    @Mock ResultadoTesteIdentificacaoRepository resultadoTesteIdentificacaoRepository;

    @InjectMocks
    IdentificacaoService service;

    // ---------- EDITE AQUI OS TESTES QUE VOCE QUER SIMULAR ----------
    static final Map<String, ResultadoTeste> TESTES_MARCADOS = new LinkedHashMap<>() {{
        put("Indol", ResultadoTeste.POSITIVO);
        put("Citrato", ResultadoTeste.NEGATIVO);
        put("H2S", ResultadoTeste.NEGATIVO);
        put("Ureia", ResultadoTeste.NEGATIVO);
        put("Motilidade", ResultadoTeste.POSITIVO);
        put("Lisina", ResultadoTeste.POSITIVO);
        put("Lactose", ResultadoTeste.POSITIVO);
        put("Gas glicose", ResultadoTeste.POSITIVO);
    }};
    // ------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        new TesteManualRankingReal().executar();
    }

    void executar() throws IOException {
        MockitoAnnotations.openMocks(this);

        List<Bacteria> bacterias = carregarBacterias("/arquivos-teste/bacterias.csv");
        Map<String, TesteBioquimico> testesPorNome = carregarTestes("/arquivos-teste/testes.csv");
        Map<Long, Map<Long, BigDecimal>> probabilidades = carregarProbabilidades(
                "/arquivos-teste/probabilidades.csv", bacterias, testesPorNome);

        when(bacteriaRepository.findAll()).thenReturn(bacterias);
        when(probabilidadeRepository.pegarProbabilidade(anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    Long idBacteria = invocation.getArgument(0);
                    Long idTeste = invocation.getArgument(1);
                    return probabilidades.getOrDefault(idBacteria, Map.of()).get(idTeste);
                });

        List<ResultadoTesteIdentificacao> resultados = new ArrayList<>();
        for (Map.Entry<String, ResultadoTeste> entrada : TESTES_MARCADOS.entrySet()) {
            TesteBioquimico teste = testesPorNome.get(entrada.getKey());
            if (teste == null) {
                System.out.println("Aviso: teste '" + entrada.getKey()
                        + "' nao existe no CSV, ignorando.");
                continue;
            }
            ResultadoTesteIdentificacao r = new ResultadoTesteIdentificacao();
            r.setTesteBioquimico(teste);
            r.setResultado(entrada.getValue());
            resultados.add(r);
        }

        List<RankingIdentificacao> ranking = service.algoritmoBayes(resultados, null);

        System.out.println("Testes marcados: " + TESTES_MARCADOS);
        System.out.println();
        System.out.println("Ranking (via IdentificacaoService.algoritmoBayes real):");
        for (RankingIdentificacao r : ranking) {
            System.out.printf("%d\u00ba - %s: %.2f%%%n",
                    r.getPosicao(),
                    r.getBacteria().getNomeCientifico(),
                    r.getProbabilidadeCalculada() * 100);
        }
    }

    // ---------- leitura dos CSVs ----------

    private List<Bacteria> carregarBacterias(String recurso) throws IOException {
        List<Bacteria> lista = new ArrayList<>();
        long id = 1;
        try (BufferedReader br = abrir(recurso)) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.isBlank()) continue;
                String[] c = linha.split(DELIMITADOR, -1);
                Bacteria b = new Bacteria();
                b.setId(id++);
                b.setNomeCientifico(c[0].trim());
                b.setGenero(c[1].trim());
                b.setFamilia(c[2].trim());
                b.setDescricaoClinica(c.length > 3 ? c[3].trim() : "");
                b.setAtivo(true);
                lista.add(b);
            }
        }
        return lista;
    }

    private Map<String, TesteBioquimico> carregarTestes(String recurso) throws IOException {
        Map<String, TesteBioquimico> mapa = new LinkedHashMap<>();
        long id = 1;
        try (BufferedReader br = abrir(recurso)) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.isBlank()) continue;
                String[] c = linha.split(DELIMITADOR, -1);
                TesteBioquimico t = new TesteBioquimico();
                t.setId(id++);
                t.setNome(c[0].trim());
                t.setDescricao(c.length > 1 ? c[1].trim() : "");
                t.setCriterioPositivo(c.length > 2 ? c[2].trim() : "");
                t.setAtivo(true);
                mapa.put(t.getNome(), t);
            }
        }
        return mapa;
    }

    private Map<Long, Map<Long, BigDecimal>> carregarProbabilidades(
            String recurso,
            List<Bacteria> bacterias,
            Map<String, TesteBioquimico> testes) throws IOException {

        Map<String, Long> idPorNome = new HashMap<>();
        for (Bacteria b : bacterias) {
            idPorNome.put(b.getNomeCientifico(), b.getId());
        }

        Map<Long, Map<Long, BigDecimal>> resultado = new HashMap<>();
        try (BufferedReader br = abrir(recurso)) {
            br.readLine();
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.isBlank()) continue;
                String[] c = linha.split(DELIMITADOR, -1);
                Long idBacteria = idPorNome.get(c[0].trim());
                TesteBioquimico teste = testes.get(c[1].trim());
                if (idBacteria == null || teste == null) continue;
                BigDecimal percentual = new BigDecimal(c[2].trim().replace(",", "."));
                resultado.computeIfAbsent(idBacteria, k -> new HashMap<>())
                        .put(teste.getId(), percentual);
            }
        }
        return resultado;
    }

    private BufferedReader abrir(String recurso) {
        InputStream is = TesteManualRankingReal.class.getResourceAsStream(recurso);
        if (is == null) {
            throw new IllegalStateException("Nao encontrei o arquivo no classpath: " + recurso);
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }
}
