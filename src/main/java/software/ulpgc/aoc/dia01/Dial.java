package software.ulpgc.aoc.dia01;


/**
 * Representa el dial físico de la caja fuerte.
 * Principio: Alta Cohesión (Solo sabe de matemáticas circulares).
 * Patrón: Value Object (Inmutable).
 */
public record Dial(int posicion) {
    // Constructor por defecto: Empieza en 50 según el enunciado
    public Dial() {
        this(50);
    }
    // Constructor canónico: Asegura que el dial siempre tenga un valor válido (0-99)
    public Dial {
        posicion = normalize(posicion);
    }


    // Lógica pura: Calcula una nueva posición sin modificar la actual (Inmutabilidad)
    public Dial rotar(int cantidad) {
        return new Dial(this.posicion + cantidad);
    }

    // Abstracción: Oculta la complejidad del módulo negativo
    private static int normalize(int n) {
        return ((n % 100) + 100) % 100;
    }
}

