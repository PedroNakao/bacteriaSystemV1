# BacteriaSys: Identificação de Enterobactérias por Teorema de Bayes

API REST em **Java / Spring Boot** que identifica bactérias da família *Enterobacteriaceae* a partir dos resultados de testes bioquímicos. O pesquisador informa o resultado de cada teste (**positivo**, **negativo** ou **variável**) e o sistema devolve um **ranking das espécies mais prováveis**, calculado pelo **Teorema de Bayes**. Cada identificação fica salva no histórico do usuário.

As probabilidades de positividade vêm do manual da **ANVISA**, *Módulo 6: Detecção e Identificação de Bactérias de Importância Médica* (Tabela 7.12).

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Como funciona o cálculo](#como-funciona-o-cálculo)
- [Regras de negócio](#regras-de-negócio)
- [Tecnologias](#tecnologias)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Diagrama Entidade-Relacionamento](#diagrama-entidade-relacionamento)
- [Base de dados inicial](#base-de-dados-inicial)
- [Como executar](#como-executar)
- [Autenticação](#autenticação)
- [Endpoints](#endpoints)
- [Tratamento de erros](#tratamento-de-erros)
- [Testes](#testes)
- [Limitações conhecidas e próximos passos](#limitações-conhecidas-e-próximos-passos)
- [Autor](#autor)

---

## Funcionalidades

- Cadastro e login de usuários com **JWT** (perfis `ADMIN` e `PESQUISADOR`)
- Listagem das espécies e dos testes bioquímicos ativos
- Identificação bacteriana por **Teorema de Bayes**, com três estados por teste: `POSITIVO`, `NEGATIVO` e `VARIAVEL`
- Classificação automática da identificação como **CONCLUÍDA** ou **INCONCLUSIVA**
- Histórico persistido: cada identificação salva os testes informados e o ranking completo
- Carga inicial da base a partir de CSVs, com validação linha a linha
- Respostas de erro padronizadas em JSON

---

## Como funciona o cálculo

Para cada espécie *B* cadastrada, o sistema calcula a probabilidade posterior dada a evidência *E* (o conjunto de resultados informados):

```
                 P(E | B) · P(B)
P(B | E) = ─────────────────────────────
            Σ  P(E | Bᵢ) · P(Bᵢ)
```

| Termo | Como é calculado |
|---|---|
| **P(B)**, a priori | Uniforme: `1 / número de espécies` |
| **P(E \| B)**, a verossimilhança | Produto das probabilidades de cada teste. Os testes são tratados como independentes (*Naive Bayes*) |
| Teste `POSITIVO` | usa `p`, o percentual de positividade da espécie naquele teste |
| Teste `NEGATIVO` | usa `100 − p` |
| Teste `VARIAVEL` | usa `50%` fixo |
| **Denominador** | Soma dos numeradores de todas as espécies (normalização: o ranking soma 1) |

Ajustes aplicados:

- **Limite de 1% a 99%**: percentuais 0% e 100% são tratados como 1% e 99%. Assim, um único resultado discordante (ex.: erro de leitura) não elimina uma espécie do ranking.
- **Par sem dado cadastrado**: se faltar a probabilidade de um par espécie × teste, o fator usado é `1.0` (neutro).
- **Resultado variável**: como o fator 0,5 é igual para todas as espécies, ele se cancela na normalização. Na prática, um teste `VARIAVEL` não altera o ranking.

Depois do cálculo, as espécies são ordenadas da maior para a menor probabilidade. A identificação é marcada como **CONCLUIDA** se a 1ª colocada tiver probabilidade **≥ 90%**. Caso contrário, fica como **INCONCLUSIVA**.

Lógica principal: [`IdentificacaoService`](bacteriaSystem/src/main/java/com/example/bacteriaSystem/service/IdentificacaoService.java).

---

## Regras de negócio

| Regra | Descrição |
|---|---|
| Família | Só são aceitas espécies da família *Enterobacteriaceae* |
| Percentuais | Toda probabilidade cadastrada deve estar entre 0 e 100 |
| Matriz | Cada par (espécie, teste) tem no máximo uma probabilidade (restrição `UNIQUE` no banco) |
| Mínimo de testes | São necessários **pelo menos 5 testes** com resultado positivo ou negativo. Os variáveis não contam |
| Sem repetição | Cada teste bioquímico pode aparecer no máximo uma vez por identificação |
| Usuário | A identificação é vinculada ao usuário autenticado, obtido do token JWT |
| Status | ≥ 90% na 1ª posição → `CONCLUIDA`. Abaixo disso → `INCONCLUSIVA` |

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5 (Web, Data JPA, Validation, Security) |
| Autenticação | JWT (`com.auth0:java-jwt` 4.6) + BCrypt |
| Banco de dados | Microsoft SQL Server (`mssql-jdbc`) |
| ORM | Hibernate / JPA |
| Utilitários | Lombok |
| Testes | JUnit 5 + Mockito |
| Build | Maven (wrapper `mvnw` incluso) |

---

## Estrutura do projeto

```
bacteriaSystem/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/example/bacteriaSystem/
    │   │   ├── BacteriaSystemApplication.java
    │   │   ├── config/security/
    │   │   │   ├── SecurityConfig.java          # Rotas públicas/protegidas, sessão stateless
    │   │   │   ├── SecurityFilter.java          # Lê o token do header Authorization
    │   │   │   └── TokenService.java            # Gera e valida JWT
    │   │   ├── controller/
    │   │   │   ├── AuthenticationController.java  # /auth/login e /auth/register
    │   │   │   └── IdentificacaoController.java   # /identificacao, /bacterias, /testes
    │   │   ├── service/
    │   │   │   ├── IdentificacaoService.java    # Algoritmo de Bayes + persistência
    │   │   │   ├── ResultadoTesteService.java   # Monta os resultados informados
    │   │   │   └── AuthorizationService.java    # UserDetailsService (login por e-mail)
    │   │   ├── model/                           # Entidades JPA e enums
    │   │   ├── repository/                      # Spring Data JPA
    │   │   ├── dto/                             # Requests e responses
    │   │   ├── exceptions/                      # Handler global + exceções customizadas
    │   │   └── dataLoader/
    │   │       └── DataLoader.java              # Carga inicial a partir dos CSVs
    │   └── resources/
    │       ├── application.properties
    │       └── arquivos-teste/                  # bacterias.csv, testes.csv, probabilidades.csv
    └── test/
        └── java/com/example/bacteriaSystem/service/
            ├── IdentificacaoServiceTest.java    # Testes unitários do algoritmo
            └── TesteManualRankingReal.java      # Simulação com os CSVs reais
```

---

## Diagrama Entidade-Relacionamento

<img width="779" height="513" alt="diagrama-er" src="https://github.com/user-attachments/assets/e1d9034f-439c-4505-ae8d-2b080bfc203b" />


O modelo tem dois blocos:

- **Base de conhecimento** (`Bacteria`, `TesteBioquimico` e `ProbabilidadeTeste`): é o "livro de referência" do sistema. `ProbabilidadeTeste` é a tabela associativa que guarda o percentual de positividade de cada espécie em cada teste e forma a matriz espécie × teste.
- **Histórico de uso** (`Usuario`, `Identificacao`, `ResultadoTesteIdentificacao` e `RankingIdentificacao`): registra quem fez cada identificação, quais resultados informou e qual ranking recebeu.

### Tabelas

| Tabela | Papel | Campos principais |
|---|---|---|
| **Usuario** | Quem usa o sistema | `nome`, `email` (único, usado como login), `senha` (BCrypt), `perfil` (`ADMIN` / `PESQUISADOR`), `dataCadastro` |
| **Bacteria** | Espécies cadastradas | `nomeCientifico` (único), `genero`, `familia`, `descricaoClinica`, `ativo` |
| **TesteBioquimico** | Testes disponíveis | `nome` (único), `descricao`, `criterioPositivo`, `ativo` |
| **ProbabilidadeTeste** | % de positividade de uma espécie em um teste | `percentualPositividade`, `fonteBibliografica`, FK `Bacteriaid`, FK `TesteBioquimicoid` |
| **Identificacao** | Uma consulta feita por um usuário | `dataHora`, `status` (`CONCLUIDA` / `INCONCLUSIVA`), FK `Usuarioid` |
| **ResultadoTesteIdentificacao** | Resultado de um teste dentro de uma identificação | `resultado`, FK `Identificacaoid`, FK `TesteBioquimicoid` |
| **RankingIdentificacao** | Posição de cada espécie no resultado de uma identificação | `probabilidadeCalculada`, `posicao`, FK `Identificacaoid`, FK `Bacteriaid` |

### Relacionamentos

| Relacionamento | Cardinalidade | Significado |
|---|---|---|
| Usuario → Identificacao | 1 : N | Um usuário faz várias identificações |
| Identificacao → ResultadoTesteIdentificacao | 1 : N | Uma identificação tem vários resultados de teste (mínimo 5 informativos) |
| TesteBioquimico → ResultadoTesteIdentificacao | 1 : N | Um teste aparece em várias identificações |
| Identificacao → RankingIdentificacao | 1 : N | Uma identificação gera uma linha de ranking por espécie |
| Bacteria → RankingIdentificacao | 1 : N | Uma espécie aparece no ranking de várias identificações |
| Bacteria → ProbabilidadeTeste | 1 : N | Uma espécie tem um percentual para cada teste |
| TesteBioquimico → ProbabilidadeTeste | 1 : N | Um teste tem um percentual para cada espécie |

> **Diferenças entre o diagrama e o código atual**
> - Em `ResultadoTesteIdentificacao`, o campo `resultadoPositivo (bit)` foi substituído por `resultado`, um enum gravado como texto (`POSITIVO`, `NEGATIVO` ou `VARIAVEL`) para suportar o resultado variável.
> - `percentualPositividade` aparece como `decimal(3,2)`, mas os valores vão de 0 a 100 (ex.: `96.5`). Um tipo como `decimal(5,2)` comporta essa faixa.
> - `probabilidadeCalculada` aparece como `decimal(2,1)`. No código é um `Double` entre 0 e 1, e uma casa decimal não seria suficiente para diferenciar as espécies do ranking.

---

## Base de dados inicial

Ao iniciar com o banco vazio, o `DataLoader` popula a base a partir de três CSVs em `src/main/resources/arquivos-teste/`:

| Arquivo | Conteúdo | Colunas |
|---|---|---|
| `bacterias.csv` | **39 espécies** de *Enterobacteriaceae* | `nomeCientifico;genero;familia;descricaoClinica;ativo` |
| `testes.csv` | **16 testes bioquímicos** | `nome;descricao;criterioPositivo;ativo` |
| `probabilidades.csv` | **624 probabilidades** (matriz completa 39 × 16) | `nomeCientifico;nomeTeste;percentualPositividade;fonteBibliografica` |

**Testes cadastrados:** Indol, Citrato, H2S, Ureia, Motilidade, Lisina, Lactose, Gás glicose, Fenilalanina, Arginina, Ornitina, Gelatina, Malonato, Sacarose, Esculina e DNase.

**Gêneros cobertos:** *Escherichia, Klebsiella, Proteus, Enterobacter, Salmonella, Citrobacter, Edwardsiella, Shigella, Hafnia, Morganella, Pantoea, Providencia, Serratia* e *Yersinia*.

Regras de leitura:

- Delimitador **`;`** (os textos podem conter vírgulas), codificação UTF-8, primeira linha é cabeçalho
- Percentuais aceitam vírgula ou ponto decimal (`96,5` ou `96.5`)
- Qualquer inconsistência interrompe a inicialização com o arquivo e o número da linha. Exemplos: coluna faltando, família diferente de *Enterobacteriaceae*, espécie ou teste duplicado, espécie inexistente, percentual fora de 0–100. Isso é intencional: é melhor a aplicação não subir do que gerar rankings errados.
- A pasta `arquivos-teste/Antiga/` guarda a versão anterior do dataset (5 espécies × 8 testes).

---

## Como executar

### Pré-requisitos

- JDK 21
- SQL Server (ex.: SQL Server Express) rodando localmente
- Um banco chamado `bacteria_db`

### 1. Configurar o banco

Em `bacteriaSystem/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost\\SQLSERVEREXPRESS;databaseName=bacteria_db;encrypt=true;trustServerCertificate=true
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
spring.jpa.hibernate.ddl-auto=create
server.port=8095
api.security.token.secret=${JWT_SECRET:my-secret-key}
```

### 2. Definir a chave do JWT (recomendado)

```bash
# Linux / macOS
export JWT_SECRET="uma-chave-longa-e-secreta"

# Windows (PowerShell)
$env:JWT_SECRET="uma-chave-longa-e-secreta"
```

Sem essa variável, a aplicação usa a chave padrão `my-secret-key`, que só serve para desenvolvimento.

### 3. Subir a aplicação

```bash
cd bacteriaSystem

# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8095`.

---

## Autenticação

A API é **stateless** e usa **JWT**. Fluxo:

1. Cadastrar um usuário em `POST /auth/register`
2. Fazer login em `POST /auth/login` e receber o token
3. Enviar o token nas demais requisições:

```
Authorization: Bearer <token>
```

O token expira em **8 horas**. As únicas rotas públicas são `/auth/login` e `/auth/register`. Rotas em `/admin/**` exigem o perfil `ADMIN`.

---

## Endpoints

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| `POST` | `/auth/register` | Pública | Cadastra um usuário |
| `POST` | `/auth/login` | Pública | Retorna o token JWT |
| `GET` | `/bacterias` | Token | Lista as espécies ativas |
| `GET` | `/testes` | Token | Lista os testes bioquímicos ativos |
| `POST` | `/identificacao` | Token | Calcula o ranking e salva a identificação |

### `POST /auth/register`

```json
{
  "nome": "Maria Silva",
  "login": "maria@email.com",
  "password": "senha123",
  "role": "PESQUISADOR"
}
```

Resposta: `200 OK` (sem corpo). Se o e-mail já existir: `400 Bad Request`.

### `POST /auth/login`

```json
{
  "login": "maria@email.com",
  "password": "senha123"
}
```

Resposta:

```json
{ "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
```

### `GET /testes`

Retorna os testes ativos com `id`, `nome`, `descricao` e `criterioPositivo`. Os `id` daqui são os usados no corpo da identificação.

### `POST /identificacao`

**Requisição:** lista com o resultado de cada teste realizado (mínimo 5 positivos/negativos, sem repetir teste):

```json
[
  { "idTeste": 1, "resultado": "POSITIVO" },
  { "idTeste": 2, "resultado": "NEGATIVO" },
  { "idTeste": 3, "resultado": "NEGATIVO" },
  { "idTeste": 4, "resultado": "NEGATIVO" },
  { "idTeste": 5, "resultado": "POSITIVO" },
  { "idTeste": 6, "resultado": "POSITIVO" },
  { "idTeste": 7, "resultado": "POSITIVO" },
  { "idTeste": 8, "resultado": "POSITIVO" }
]
```

*Indol +, Citrato −, H2S −, Ureia −, Motilidade +, Lisina +, Lactose +, Gás glicose +: perfil clássico de E. coli.*

**Resposta:** ranking com todas as espécies, da mais para a menos provável. `probCalculada` vai de 0 a 1:

```json
[
  { "idBacteria": 1,  "probCalculada": 0.99789,  "posicao": 1, "nomeCientifico": "Escherichia coli",          "status": "CONCLUIDA" },
  { "idBacteria": 10, "probCalculada": 0.00059,  "posicao": 2, "nomeCientifico": "Enterobacter aerogenes",    "status": "CONCLUIDA" },
  { "idBacteria": 18, "probCalculada": 0.00048,  "posicao": 3, "nomeCientifico": "Hafnia alvei",              "status": "CONCLUIDA" },
  { "idBacteria": 13, "probCalculada": 0.00026,  "posicao": 4, "nomeCientifico": "Escherichia coli inativa",  "status": "CONCLUIDA" },
  ...
]
```

> Valores calculados com a base atual de 39 espécies × 16 testes. O campo `status` se refere à identificação como um todo e por isso se repete em todos os itens.

---

## Tratamento de erros

Os erros são tratados por um `@RestControllerAdvice` e retornam sempre o mesmo formato:

```json
{
  "timestamp": "2026-11-13T22:40:00",
  "status": "Bad Request",
  "message": "São necessários ao menos 5 testes com resultado positivo ou negativo. Informados: 3 (resultados variáveis não alteram o cálculo).",
  "httpStatus": 400,
  "url": "/identificacao"
}
```

| Situação | HTTP |
|---|---|
| Lista vazia, item sem `idTeste`/`resultado` ou teste repetido | `400` |
| Menos de 5 testes informativos | `400` |
| E-mail já cadastrado | `400` |
| Login ou senha incorretos | `400` |
| Teste bioquímico inexistente | `404` |
| Usuário não encontrado | `404` |
| Base de espécies vazia | `404` |
| Acesso negado | `403` |

---

## Testes

### Testes unitários do algoritmo

`IdentificacaoServiceTest` testa o `IdentificacaoService` com repositórios *mockados* (sem Spring e sem banco), comparando com cálculos feitos à mão:

| Caso | O que verifica |
|---|---|
| `cincoTestesPositivos_especieComMaiorPositividadeVence` | A espécie mais compatível fica em 1º |
| `testesPositivosENegativosMisturados_ordenaCorretamente` | Ordenação com resultados mistos |
| `resultadoVariavel_naoAlteraAsProbabilidades` | `VARIAVEL` não muda o ranking |
| `probabilidadeNaoCadastrada_naoQuebraOCalculo` | Par sem dado usa fator neutro |
| `percentualZero_naoEliminaEspecieDoRanking` | O limite de 1% mantém a espécie viva |
| `probabilidadeAcimaDoLimiar_marcaIdentificacaoComoConcluida` | Regra dos 90% |
| `cincoResultadosVariaveis_saoRejeitadosPelaRN06` | Variáveis não contam para o mínimo |
| `menosDeCincoTestes_saoRejeitados` | Mínimo de 5 testes |
| `testeRepetido_eRejeitado` | Teste duplicado |
| `listaVazia_eRejeitada` | Requisição vazia |

```bash
cd bacteriaSystem
./mvnw test -Dtest=IdentificacaoServiceTest
```

### Simulação com os dados reais

`TesteManualRankingReal` roda o algoritmo contra os CSVs do projeto, sem banco. Edite o mapa `TESTES_MARCADOS` com os resultados que quiser simular e execute o `main()` pela IDE. O ranking é impresso no console.

---

## Limitações conhecidas e próximos passos

- **O banco é recriado a cada inicialização.** Com `spring.jpa.hibernate.ddl-auto=create`, as tabelas são apagadas sempre que a aplicação sobe, e o histórico de identificações e os usuários se perdem. Depois da primeira carga, o ideal é trocar para `update` (ou usar migrações com Flyway).
- **Qualquer pessoa pode se cadastrar como ADMIN.** O `/auth/register` é público e aceita o campo `role` livremente. Em um próximo commit tratará desse erro, mas foi permitido para que fosse realizado testes.
- **O ranking considera espécies inativas.** O `GET /bacterias` filtra por `ativo = true`, mas o cálculo usa todas as espécies cadastradas.
- **Credenciais no repositório.** O usuário e a senha do banco estão versionados no `application.properties`. O ideal é movê-los para variáveis de ambiente, como já é feito com o `JWT_SECRET`.
- **Ainda não há endpoint para consultar o histórico.** As identificações são salvas, mas não podem ser listadas pela API.
- **Próximos passos:** integração com o app Android, consulta do histórico por usuário e rotas administrativas em `/admin/**` para manter a base de conhecimento.

---

## Autor

**Pedro Nakao**: [github.com/PedroNakao](https://github.com/PedroNakao)
