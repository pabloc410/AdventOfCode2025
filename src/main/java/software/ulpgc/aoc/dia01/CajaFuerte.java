package software.ulpgc.aoc.dia01;


public class CajaFuerte {
    private final ProtocoloSeguridad protocolo;
    private Dial dial;
    private int vecesEnCero;

    // Inyección de dependencias a través del constructor
    public CajaFuerte(ProtocoloSeguridad protocolo) {
        this.dial = new Dial();
        this.vecesEnCero = 0;
        this.protocolo = protocolo;
    }

    /**
     * Procesa una instrucción de texto, rota el dial y aplica reglas de negocio.
     * Cumple SRP: Coordina la acción, pero delega la matemática al Dial.
     */
    public void rotar(String instruccion) {
        if (instruccion == null || instruccion.trim().isEmpty()) return;

        // 1. Parsing (Interpretar la entrada)
        char direccion = instruccion.charAt(0);
        int cantidad = Integer.parseInt(instruccion.substring(1));

        // Determinar signo: R suma, L resta
        int movimiento = (direccion == 'L') ? -cantidad : cantidad;

        // 2. Simular futuro
        Dial suguienteDial = dial.rotar(movimiento);

        // 3. Delegar la regla de negocio al protocolo (OCP en acción)
        this.vecesEnCero += protocolo.calculatePoints(this.dial, movimiento, suguienteDial);

        // 4. Avanzar
        this.dial = suguienteDial;
    }

    public ProtocoloSeguridad getProtocolo() {
        return protocolo;
    }

    public int getVecesCero() {
        return vecesEnCero;
    }


    public int getPosicionActual() {
        return dial.posicion();
    }
}
