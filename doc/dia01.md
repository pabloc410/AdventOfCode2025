
### Parte 1: El Núcleo y la Estructura Base
En la primera fase, construimos los cimientos del sistema. El objetivo era que la caja fuerte funcionara correctamente para el primer caso (contar solo cuando el dial se detiene en 0), pero asegurando que el código fuera robusto y limpio.

**¿Qué hicimos?**
1.  Creamos el record `Dial` para encapsular las matemáticas circulares.
2.  Creamos la clase `CajaFuerte` para gestionar el estado.
3.  Creamos `CargadorEntrada` para leer el fichero.

**Principios y Fundamentos aplicados:**

* **Alta Cohesión :**
  Separamos la lógica matemática (en `Dial`) de la lógica de negocio (en `CajaFuerte`). El documento indica que las partes de un módulo deben estar estrechamente relacionadas y enfocadas en una única tarea . `Dial` solo sabe sumar/restar en círculo; no sabe nada sobre contraseñas ni archivos.
* **Abstracción :**
  En `Dial`, ocultamos la complejidad de la fórmula `((n % 100) + 100) % 100` detrás de un método simple. Como dice el texto, consiste en ocultar los detalles complejos detrás de una interfaz simple .
* **Principio de Responsabilidad Única (SRP) :**
  Cada clase tiene una sola razón para cambiar. `CargadorEntrada` solo cambia si cambia el formato del fichero. `CajaFuerte` solo cambia si cambia la mecánica de la caja. Esto refleja la alta cohesión mencionada anteriormente .
* **Patrón Iterator :**
  En `CargadorEntrada`, usamos `Stream` de Java (`Files.lines`), que es una implementación moderna del patrón Iterator. Esto permite acceder secuencialmente a los elementos sin exponer su representación subyacente , procesando el archivo línea por línea de manera eficiente.

---

### Parte 2: La Extensión y el Cambio de Reglas
En la segunda fase, el requerimiento cambió radicalmente: había que usar una nueva fórmula matemática ("método 0x434C49434B") sin romper lo que ya funcionaba.

**¿Qué hicimos?**
1.  Introdujimos la interfaz `ProtocoloSeguridad` (el contrato).
2.  Implementamos dos estrategias en `ProtocolosSeguridad`: `PART_A` (simple) y `PART_B` (matemática de intervalos).
3.  Modificamos `CajaFuerte` para recibir el protocolo desde fuera (Inyección de Dependencias).
4.  Creamos dos ejecutables: `Main01a` y `Main01b`.

**Principios y Fundamentos aplicados:**

* **Principio Abierto Cerrado (OCP) :**
  Este es el principio estrella de la Parte 2. "Las clases deben estar abiertas para la extensión, pero cerradas para la modificación" .
    * *Cómo lo logramos:* Para añadir la lógica de la Parte B, **no modificamos** el código fuente de `CajaFuerte` (una vez refactorizada). Simplemente creamos una nueva implementación de `ProtocoloSeguridad` y se la pasamos. La `CajaFuerte` quedó "cerrada" a cambios, pero "abierta" a comportarse diferente.
* **Principio de Inversión de Dependencias (DIP) :**
  El documento dice que "Módulos de alto nivel no deben depender de módulos de bajo nivel, sino de abstracciones" .
    * *Cómo lo logramos:* `CajaFuerte` (alto nivel) ya no depende de un código "hardcodeado" que suma +1. Ahora depende de la abstracción (interfaz) `ProtocoloSeguridad`. No le importa si es el protocolo A o B, solo sabe que debe llamar a `calculatePoints`.
* **Bajo Acoplamiento :**
  Al usar la interfaz, reducimos las interdependencias. Podemos cambiar la fórmula matemática compleja en `PART_B` tantas veces queramos sin riesgo de romper `CajaFuerte` ni `Main01a` .
* **Principio DRY (Don't Repeat Yourself) :**
  "Cada pieza de conocimiento... debería tener una representación única" .
    * *Cómo lo logramos:* Reutilizamos `CargadorEntrada`, `Dial` y `CajaFuerte` para ambos ejercicios. No copiamos y pegamos el proyecto entero para hacer la parte B; solo escribimos el código nuevo (el `Main01b` y la estrategia nueva).

    