package software.ulpgc.aoc.dia01;


public class ProtocolosSeguridad {

    // --- Estrategia A: Contar si terminamos en 0 ---
    public static final ProtocoloSeguridad PART_A = (oldDial, movement, newDial) ->
            newDial.posicion() == 0 ? 1 : 0;

    // --- Estrategia B: Contar cruces por 0 (Matemática de intervalos) ---
    public static final ProtocoloSeguridad PART_B = (oldDial, movement, newDial) -> {
        long start = oldDial.posicion();
        long end = start + movement;

        // Definimos el intervalo numérico recorrido (min, max)
        long min = (movement > 0) ? start + 1 : end;
        long max = (movement > 0) ? end : start - 1;

        // Fórmula matemática para contar múltiplos de 100 en un rango [min, max]
        return (int) (Math.floorDiv(max, 100) - Math.floorDiv(min - 1, 100));
    };
}