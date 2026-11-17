package io.github.eduardomodolodev.pagamentos;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regra de ouro da arquitetura: os pacotes de domínio e de padrões não podem
 * importar nada de framework. O Spring (e o que vem com ele: Jakarta, Swagger)
 * só pode aparecer nas camadas {@code api} e {@code config}.
 */
class ArquiteturaTest {

    private static final Path RAIZ = Path.of("src/main/java/io/github/eduardomodolodev/pagamentos");
    private static final Set<String> CAMADAS_DE_FRAMEWORK = Set.of("api", "config");
    private static final List<String> IMPORTS_PROIBIDOS = List.of(
            "import org.springframework", "import jakarta.", "import io.swagger");

    @Test
    void nucleoNaoDependeDeFramework() throws IOException {
        List<String> violacoes;
        try (Stream<Path> arquivos = Files.walk(RAIZ)) {
            violacoes = arquivos
                    .filter(arquivo -> arquivo.toString().endsWith(".java"))
                    .filter(arquivo -> !CAMADAS_DE_FRAMEWORK.contains(RAIZ.relativize(arquivo).getName(0).toString()))
                    .flatMap(ArquiteturaTest::importsProibidos)
                    .toList();
        }

        assertThat(violacoes).as("imports de framework fora de api/ e config/").isEmpty();
    }

    @Test
    void oNucleoTemOsPacotesEsperados() {
        for (String pacote : List.of("domain", "repository", "strategy", "decorator", "factory",
                "validation", "observer", "service")) {
            assertThat(RAIZ.resolve(pacote)).as("pacote %s", pacote).isDirectory();
        }
    }

    private static Stream<String> importsProibidos(Path arquivo) {
        try {
            return Files.readAllLines(arquivo).stream()
                    .map(String::strip)
                    .filter(linha -> IMPORTS_PROIBIDOS.stream().anyMatch(linha::startsWith))
                    .map(linha -> RAIZ.relativize(arquivo) + ": " + linha);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + arquivo, e);
        }
    }
}
