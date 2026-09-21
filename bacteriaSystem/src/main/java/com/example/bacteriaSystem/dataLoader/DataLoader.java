package com.example.bacteriaSystem.dataLoader;

import com.example.bacteriaSystem.model.Bacteria;
import com.example.bacteriaSystem.model.ProbabilidadeTeste;
import com.example.bacteriaSystem.model.TesteBioquimico;
import com.example.bacteriaSystem.repository.BacteriaRepository;
import com.example.bacteriaSystem.repository.ProbabilidadeTesteRepository;
import com.example.bacteriaSystem.repository.TesteBioquimicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Carga inicial do catálogo de espécies, testes bioquímicos e da matriz de
 * probabilidades consumida pelo cálculo bayesiano.
 *
 * FORMATO DOS ARQUIVOS (delimitador ';' — NÃO use vírgula, porque os campos
 * de texto contêm vírgulas; UTF-8; primeira linha é cabeçalho):
 *
 *   bacterias.csv      nomeCientifico;genero;familia;descricaoClinica;ativo
 *   testes.csv         nome;descricao;criterioPositivo;ativo
 *   probabilidades.csv nomeCientifico;nomeTeste;percentualPositividade;fonteBibliografica
 *
 * Toda inconsistência interrompe o boot da aplicação com uma mensagem que
 * identifica o arquivo e o número da linha. Isso é intencional: subir com a
 * base incompleta produz rankings silenciosamente errados, o que é muito
 * pior do que não subir.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private static final String DELIMITADOR = ";";
    private static final String FAMILIA_ESPERADA = "Enterobacteriaceae";
    private static final BigDecimal PERCENTUAL_MAXIMO = new BigDecimal("100");

    private static final String CSV_BACTERIAS = "arquivos-teste/bacterias.csv";
    private static final String CSV_TESTES = "arquivos-teste/testes.csv";
    private static final String CSV_PROBABILIDADES = "arquivos-teste/probabilidades.csv";

    private final BacteriaRepository bacteriaRepository;
    private final TesteBioquimicoRepository bioquimicoRepository;
    private final ProbabilidadeTesteRepository probabilidadeRepository;

    public DataLoader(BacteriaRepository bacteriaRepository,
                      TesteBioquimicoRepository bioquimicoRepository,
                      ProbabilidadeTesteRepository probabilidadeRepository) {
        this.bacteriaRepository = bacteriaRepository;
        this.bioquimicoRepository = bioquimicoRepository;
        this.probabilidadeRepository = probabilidadeRepository;
    }

    /** Uma linha de dados do CSV, com o número original para mensagens de erro. */
    private record LinhaCsv(int numero, String[] campos) {
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (bacteriaRepository.count() > 0) {
            log.info("Base já populada — carga inicial ignorada.");
            return;
        }

        log.info("Iniciando carga inicial da base...");
        int especies = carregaBacterias();
        int testes = carregaTestes();
        int probabilidades = carregaProbabilidades();

        log.info("Carga concluída: {} espécies, {} testes, {} probabilidades.",
                especies, testes, probabilidades);
    }

    // ------------------------------------------------------------------
    // Leitura genérica
    // ------------------------------------------------------------------

    private List<LinhaCsv> lerCsv(String caminho, int colunasEsperadas) {
        ClassPathResource recurso = new ClassPathResource(caminho);

        if (!recurso.exists()) {
            throw new IllegalStateException(
                    "Arquivo não encontrado no classpath: " + caminho
                            + ". Verifique se ele está em src/main/resources/.");
        }

        List<LinhaCsv> linhas = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {

            if (br.readLine() == null) {
                throw new IllegalStateException("Arquivo vazio: " + caminho);
            }

            String linha;
            int numero = 1;

            while ((linha = br.readLine()) != null) {
                numero++;
                linha = linha.replace("\uFEFF", "").trim();

                if (linha.isEmpty()) {
                    continue;
                }

                String[] campos = linha.split(DELIMITADOR, -1);

                if (campos.length != colunasEsperadas) {
                    throw new IllegalStateException(String.format(
                            "%s, linha %d: esperadas %d colunas, encontradas %d. "
                                    + "Confira se o delimitador é '%s'. Conteúdo: %s",
                            caminho, numero, colunasEsperadas, campos.length, DELIMITADOR, linha));
                }

                for (int i = 0; i < campos.length; i++) {
                    campos[i] = campos[i].trim();
                }

                linhas.add(new LinhaCsv(numero, campos));
            }

        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler o arquivo " + caminho, e);
        }

        if (linhas.isEmpty()) {
            throw new IllegalStateException(
                    "Nenhuma linha de dados encontrada em " + caminho);
        }

        return linhas;
    }

    // ------------------------------------------------------------------
    // Espécies
    // ------------------------------------------------------------------

    private int carregaBacterias() {
        List<LinhaCsv> linhas = lerCsv(CSV_BACTERIAS, 5);

        Set<String> nomesVistos = new HashSet<>();
        List<Bacteria> bacterias = new ArrayList<>();

        for (LinhaCsv l : linhas) {
            String nomeCientifico = obrigatorio(CSV_BACTERIAS, l, 0, "nomeCientifico");
            String genero = obrigatorio(CSV_BACTERIAS, l, 1, "genero");
            String familia = obrigatorio(CSV_BACTERIAS, l, 2, "familia");

            if (!FAMILIA_ESPERADA.equalsIgnoreCase(familia)) {
                throw new IllegalStateException(erro(CSV_BACTERIAS, l,
                        "a família deve ser " + FAMILIA_ESPERADA + " (RN01). Encontrado: " + familia));
            }

            if (!nomesVistos.add(nomeCientifico.toLowerCase())) {
                throw new IllegalStateException(erro(CSV_BACTERIAS, l,
                        "espécie repetida no arquivo: " + nomeCientifico));
            }

            Bacteria bacteria = new Bacteria();
            bacteria.setNomeCientifico(nomeCientifico);
            bacteria.setGenero(genero);
            bacteria.setFamilia(FAMILIA_ESPERADA);
            bacteria.setDescricaoClinica(vazioComoNulo(l.campos()[3]));
            bacteria.setAtivo(parseBoolean(CSV_BACTERIAS, l, 4, "ativo"));

            bacterias.add(bacteria);
        }

        bacteriaRepository.saveAll(bacterias);
        return bacterias.size();
    }

    // ------------------------------------------------------------------
    // Testes bioquímicos
    // ------------------------------------------------------------------

    private int carregaTestes() {
        List<LinhaCsv> linhas = lerCsv(CSV_TESTES, 4);

        Set<String> nomesVistos = new HashSet<>();
        List<TesteBioquimico> testes = new ArrayList<>();

        for (LinhaCsv l : linhas) {
            String nome = obrigatorio(CSV_TESTES, l, 0, "nome");
            String descricao = obrigatorio(CSV_TESTES, l, 1, "descricao");
            String criterioPositivo = obrigatorio(CSV_TESTES, l, 2, "criterioPositivo");

            if (!nomesVistos.add(nome.toLowerCase())) {
                throw new IllegalStateException(erro(CSV_TESTES, l,
                        "teste repetido no arquivo: " + nome));
            }

            TesteBioquimico teste = new TesteBioquimico();
            teste.setNome(nome);
            teste.setDescricao(descricao);
            teste.setCriterioPositivo(criterioPositivo);
            teste.setAtivo(parseBoolean(CSV_TESTES, l, 3, "ativo"));

            testes.add(teste);
        }

        bioquimicoRepository.saveAll(testes);
        return testes.size();
    }

    // ------------------------------------------------------------------
    // Matriz de probabilidades
    // ------------------------------------------------------------------

    private int carregaProbabilidades() {
        List<LinhaCsv> linhas = lerCsv(CSV_PROBABILIDADES, 4);

        // Duas consultas no total, em vez de uma por linha do CSV.
        Map<String, Bacteria> bacteriasPorNome = bacteriaRepository.findAll().stream()
                .collect(Collectors.toMap(
                        b -> b.getNomeCientifico().toLowerCase(), Function.identity()));

        Map<String, TesteBioquimico> testesPorNome = bioquimicoRepository.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getNome().toLowerCase(), Function.identity()));

        Set<String> paresVistos = new HashSet<>();
        List<ProbabilidadeTeste> probabilidades = new ArrayList<>();

        for (LinhaCsv l : linhas) {
            String nomeBacteria = obrigatorio(CSV_PROBABILIDADES, l, 0, "nomeCientifico");
            String nomeTeste = obrigatorio(CSV_PROBABILIDADES, l, 1, "nomeTeste");

            Bacteria bacteria = bacteriasPorNome.get(nomeBacteria.toLowerCase());
            if (bacteria == null) {
                throw new IllegalStateException(erro(CSV_PROBABILIDADES, l,
                        "espécie não cadastrada em bacterias.csv: " + nomeBacteria));
            }

            TesteBioquimico teste = testesPorNome.get(nomeTeste.toLowerCase());
            if (teste == null) {
                throw new IllegalStateException(erro(CSV_PROBABILIDADES, l,
                        "teste não cadastrado em testes.csv: " + nomeTeste));
            }

            String chave = nomeBacteria.toLowerCase() + "|" + nomeTeste.toLowerCase();
            if (!paresVistos.add(chave)) {
                throw new IllegalStateException(erro(CSV_PROBABILIDADES, l,
                        "par (espécie, teste) duplicado, o que viola a RN04: "
                                + nomeBacteria + " / " + nomeTeste));
            }

            ProbabilidadeTeste probabilidade = new ProbabilidadeTeste();
            probabilidade.setBacteria(bacteria);
            probabilidade.setTesteBioquimico(teste);
            probabilidade.setPercentualPositividade(parsePercentual(CSV_PROBABILIDADES, l, 2));
            probabilidade.setFonteBibliografica(vazioComoNulo(l.campos()[3]));

            probabilidades.add(probabilidade);
        }

        probabilidadeRepository.saveAll(probabilidades);

        int esperado = bacteriasPorNome.size() * testesPorNome.size();
        if (probabilidades.size() < esperado) {
            log.warn("Cobertura incompleta da matriz: {} de {} combinações possíveis. "
                            + "Os pares ausentes recebem verossimilhança neutra no cálculo.",
                    probabilidades.size(), esperado);
        }

        return probabilidades.size();
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private String erro(String arquivo, LinhaCsv l, String mensagem) {
        return String.format("%s, linha %d: %s", arquivo, l.numero(), mensagem);
    }

    private String obrigatorio(String arquivo, LinhaCsv l, int indice, String campo) {
        String valor = l.campos()[indice];
        if (valor.isEmpty()) {
            throw new IllegalStateException(erro(arquivo, l, "campo obrigatório vazio: " + campo));
        }
        return valor;
    }

    private String vazioComoNulo(String valor) {
        return (valor == null || valor.isEmpty()) ? null : valor;
    }

    private Boolean parseBoolean(String arquivo, LinhaCsv l, int indice, String campo) {
        String valor = l.campos()[indice].toLowerCase();

        return switch (valor) {
            case "true", "1", "sim", "s" -> Boolean.TRUE;
            case "false", "0", "nao", "não", "n" -> Boolean.FALSE;
            default -> throw new IllegalStateException(erro(arquivo, l,
                    "valor booleano inválido em '" + campo + "': " + l.campos()[indice]
                            + ". Use true ou false."));
        };
    }

    private BigDecimal parsePercentual(String arquivo, LinhaCsv l, int indice) {
        // Planilhas em português gravam decimal com vírgula.
        String bruto = l.campos()[indice].replace(",", ".");

        if (bruto.isEmpty()) {
            throw new IllegalStateException(erro(arquivo, l,
                    "percentualPositividade é obrigatório."));
        }

        BigDecimal valor;
        try {
            valor = new BigDecimal(bruto);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(erro(arquivo, l,
                    "percentual inválido: " + l.campos()[indice]), e);
        }

        if (valor.compareTo(BigDecimal.ZERO) < 0 || valor.compareTo(PERCENTUAL_MAXIMO) > 0) {
            throw new IllegalStateException(erro(arquivo, l,
                    "percentual fora do intervalo 0 a 100, o que viola a RN03: " + valor));
        }

        return valor;
    }
}
