import org.junit.jupiter.api.Test;
import software.ulpgc.aoc.dia01.CajaFuerte;
import software.ulpgc.aoc.dia01.ProtocolosSeguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Dia01BTest {

    @Test
    void testMultiplesVueltas() {
        // Inyectamos la estrategia B (contar cruces)
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_B);

        // Inicio 50. R1000 son 10 vueltas completas (1000/100 = 10).
        // Pasa por el 0 diez veces.
        caja.rotar("R1000");
        assertEquals(10, caja.getVecesCero());
    }

    @Test
    void testCruceDerecha() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_B);

        // 50 -> 90 (Avanza 40, no cruza el 0)
        caja.rotar("R40");
        assertEquals(0, caja.getVecesCero());

        // 90 -> 10 (Avanza 20: 90..99..0..10. Cruza el 0)
        caja.rotar("R20");
        assertEquals(1, caja.getVecesCero());
    }

    @Test
    void testCruceIzquierda() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_B);

        // 50 -> 10 (Retrocede 40, no cruza el 0)
        caja.rotar("L40");
        assertEquals(0, caja.getVecesCero());

        // 10 -> 90 (Retrocede 20: 10..0..99..90. Cruza el 0 hacia atrás)
        caja.rotar("L20");
        assertEquals(1, caja.getVecesCero());
    }

    @Test
    void testAterrizajeExactoEnCero() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_B);
        // 50 -> 0 (Cuenta como 1 toque/cruce)
        caja.rotar("R50");
        assertEquals(1, caja.getVecesCero());
    }

    @Test
    void testAceptacionParteB() {
        CajaFuerte caja = new CajaFuerte(ProtocolosSeguridad.PART_B);
        String[] entradas = {
                "L68", "L30", "R48", "L5", "R60",
                "L55", "L1", "L99", "R14", "L82"
        };

        for (String entrada : entradas) caja.rotar(entrada);

        // Según el enunciado, para la parte B la contraseña es 6
        assertEquals(6, caja.getVecesCero());
    }
}