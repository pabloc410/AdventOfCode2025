import org.junit.jupiter.api.Test;
import software.ulpgc.aoc.dia01.CajaFuerte;
import software.ulpgc.aoc.dia01.ProtocolosSeguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class Dia01ATest {

    @Test
    void testRobustez() {
        // Inyectamos la estrategia A
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_A);

        caja.rotar(null);
        caja.rotar("");
        caja.rotar("   ");

        // No debe haber cambiado la puntuación
        assertEquals(0, caja.getVecesCero());
    }

    @Test
    void testEstadoInicial() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_A);
        assertEquals(0, caja.getVecesCero());
    }

    @Test
    void testParadaEnCero() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_A);
        // Inicio 50 -> R50 -> Termina en 0 (Cuenta)
        caja.rotar("R50");
        assertEquals(1, caja.getVecesCero());
    }

    @Test
    void testPasoPorCeroIgnorado() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_A);
        // Inicio 50 -> R60 -> Termina en 10 (Cruzó el 0, pero no paró ahí)
        caja.rotar("R60");
        assertEquals(0, caja.getVecesCero());
    }

    @Test
    void testAceptacionParteA() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_A);
        String[] entradas = {
                "L68", "L30", "R48", "L5", "R60",
                "L55", "L1", "L99", "R14", "L82"
        };

        for (String entrada : entradas) caja.rotar(entrada);

        assertEquals(3, caja.getVecesCero());
    }
}