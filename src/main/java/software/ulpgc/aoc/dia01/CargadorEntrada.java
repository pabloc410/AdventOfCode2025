package software.ulpgc.aoc.dia01;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/// Extraemos la lectura de archivos para cumplir con DRY (Don't Repeat Yourself)

public class CargadorEntrada {
    public static void process(String filename, CajaFuerte safe) {
        try {
            URL resource = CargadorEntrada.class.getClassLoader().getResource(filename);
            if (resource == null) throw new RuntimeException("File no encontrado: " + filename);

            Path path = Paths.get(resource.toURI());

            try (Stream<String> lines = Files.lines(path)) {
                lines.map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(safe::rotar);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error cargando entrada", e);
        }
    }
}