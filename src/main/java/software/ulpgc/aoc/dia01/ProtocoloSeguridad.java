package software.ulpgc.aoc.dia01;

/**
 * Define el protocolo de seguridad para validar la contraseña.
 * Principio: Inversión de Dependencias (DIP)[cite: 37].
 * La CajaFuerte depende de esta abstracción, no de una implementación concreta.
 */
@FunctionalInterface
public interface ProtocoloSeguridad {
    /**
     * Calcula los puntos a sumar basándose en el estado anterior, el movimiento y el estado nuevo.
     */
    int calculatePoints(Dial oldDial, int movement, Dial newDial);
}
