package software.ulpgc.aoc.dia01.dia01a;


import software.ulpgc.aoc.dia01.CajaFuerte;
import software.ulpgc.aoc.dia01.CargadorEntrada;
import software.ulpgc.aoc.dia01.ProtocolosSeguridad;

public class Main01a {
    public static void main(String[] args) {
        // Inyectamos la Estrategia A
        CajaFuerte safe = new CajaFuerte(ProtocolosSeguridad.PART_A);

        CargadorEntrada.process("dia01input", safe);

        System.out.println("--- RESULTADO PARTE A ---");
        System.out.println("Contraseña: " + safe.getVecesCero());
    }
}