package software.ulpgc.aoc.dia01.dia01b;


import software.ulpgc.aoc.dia01.CajaFuerte;
import software.ulpgc.aoc.dia01.CargadorEntrada;
import software.ulpgc.aoc.dia01.ProtocolosSeguridad;

public class Main01b {
    public static void main(String[] args) {
        // Inyectamos la Estrategia B
        // Fíjate que usamos la MISMA clase CajaFuerte. Reutilización total.
        CajaFuerte safe = new CajaFuerte(ProtocolosSeguridad.PART_B);

        CargadorEntrada.process("dia01input", safe);

        System.out.println("--- RESULTADO PARTE B ---");
        System.out.println("Contraseña: " + safe.getVecesCero());
    }
}