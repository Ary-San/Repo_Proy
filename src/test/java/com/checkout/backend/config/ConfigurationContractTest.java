package com.checkout.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba que application.properties y .env.example digan lo mismo.
 *
 * Existe por un fallo concreto: application.properties exigia DB_HOST, DB_PORT y
 * DB_NAME sin valor por defecto, y .env.example no las mencionaba. Quien seguia
 * la plantilla al pie de la letra se encontraba con que la aplicacion no
 * arrancaba, y el mensaje de Spring solo nombra la primera variable que falta,
 * asi que el problema se descubria de a una.
 *
 * Es un test de archivos y no de contexto a proposito. Levantar la aplicacion
 * para comprobarlo exigiria una base de datos, y lo que se esta verificando no
 * necesita ninguna: es una contradiccion entre dos archivos de texto, y se
 * detecta leyendolos.
 */
class ConfigurationContractTest {

    private static final Path PROPERTIES = Path.of("src/main/resources/application.properties");
    private static final Path ENV_EXAMPLE = Path.of(".env.example");

    /** ${VARIABLE} sin dos puntos, es decir, sin valor por defecto. */
    private static final Pattern REQUIRED = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)}");

    /** ${VARIABLE:...}, opcional porque trae respaldo. */
    private static final Pattern OPTIONAL = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*):");

    @Test
    @DisplayName("toda variable obligatoria esta documentada en .env.example")
    void everyRequiredVariableIsDocumented() throws IOException {
        Set<String> required = matches(REQUIRED, Files.readString(PROPERTIES));
        Set<String> documented = declaredInEnvExample();

        assertThat(required)
                .as("""
                        Estas variables no tienen valor por defecto, asi que sin ellas la \
                        aplicacion no arranca, y .env.example no las menciona. Agregarlas \
                        ahi, o darles un default en application.properties si de verdad son \
                        opcionales.""")
                .isSubsetOf(documented);
    }

    @Test
    @DisplayName(".env.example no documenta variables que ya nadie usa")
    void envExampleHasNoStaleEntries() throws IOException {
        String properties = Files.readString(PROPERTIES);
        Set<String> used = new LinkedHashSet<>(matches(REQUIRED, properties));
        used.addAll(matches(OPTIONAL, properties));

        assertThat(declaredInEnvExample())
                .as("""
                        La plantilla nombra variables que application.properties ya no lee. \
                        Una variable que no hace nada es peor que ninguna: alguien la \
                        rellena y se queda esperando un efecto que no llega.""")
                .isSubsetOf(used);
    }

    @Test
    @DisplayName("ninguna credencial queda escrita en application.properties")
    void noCredentialIsHardcoded() throws IOException {
        List<String> offenders = Files.readAllLines(PROPERTIES).stream()
                .map(String::trim)
                .filter(line -> !line.startsWith("#"))
                .filter(ConfigurationContractTest::looksLikeASecret)
                .toList();

        assertThat(offenders)
                .as("""
                        Una propiedad de credencial tiene un valor escrito en el archivo en \
                        vez de leerlo del entorno. Aunque hoy sea un marcador de posicion, \
                        es el sitio exacto donde alguien acabara pegando la credencial \
                        buena, y de ahi viaja al repositorio.""")
                .isEmpty();
    }

    /**
     * Una linea es sospechosa cuando su clave suena a credencial y su valor no
     * es una sustitucion del entorno.
     */
    private static boolean looksLikeASecret(String line) {
        int separator = line.indexOf('=');
        if (separator < 0) {
            return false;
        }

        String key = line.substring(0, separator).toLowerCase();
        String value = line.substring(separator + 1).trim();

        boolean sensitive = key.contains("password") || key.contains("secret")
                || key.contains("username") || key.contains("token");

        return sensitive && !value.isEmpty() && !value.startsWith("${");
    }

    private static Set<String> declaredInEnvExample() throws IOException {
        return Files.readAllLines(ENV_EXAMPLE).stream()
                .map(String::trim)
                .filter(line -> !line.startsWith("#") && line.contains("="))
                .map(line -> line.substring(0, line.indexOf('=')))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> matches(Pattern pattern, String text) {
        Set<String> found = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

}
