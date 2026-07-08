### **Day 7 \- Laboratories**

#### **1\. Introduction and Problem**

The setting is a tachyon laboratory where a light beam (`S`) descends through a grid and, upon hitting a splitter (`^`), splits into two beams that continue downwards. The input is a grid of characters and the simulation advances **layer by layer** (top to bottom) propagating the intensity of each beam. The challenge has two parts that share exactly the same simulation and only change *what is measured* on the final result:

* **Part A:** count the total number of **splits** that occur (each time a beam hits a splitter).
* **Part B:** sum the **intensity** of all active beams in the **last layer** (the resulting "timelines").

Since the only thing that changes is how the result of a single simulation is read, that measurement is modeled as an injectable abstraction (`LabProtocol`): two different readings (counting splits vs. summing intensities) over the same already-solved `TachyonSimulator`.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day07
    ├── model         (pure domain, depends on nothing)
    │   ├── CellType
    │   ├── Cell
    │   ├── TachyonSimulator
    │   └── LabProtocol
    ├── control       (orchestrates the use case)
    │   ├── LabController
    │   └── GridBuilder
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main07A, b/Main07B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`CellType`** *(enum)*: the three possible states of a cell (`EMPTY`, `DIVISOR`, `BEAM`). It centralizes the domain vocabulary.
* **`Cell`** *(record)*: an immutable *Value Object* that combines the type and the beam intensity. It encapsulates its parsing from a character (`fromChar`) and its factories (`empty`, `divisor`, `beam`), so the rest of the program talks at a high level (`isBeam`, `isDivisor`) and not in symbols (`^`, `S`) → **high cohesion**.
* **`TachyonSimulator`** *(record)*: the simulation engine, **pure and immutable**. It advances layer by layer (`solve`) and on each step returns a **new** simulator with the updated layer and the accumulated split counter, without mutating state. It lives in the domain because it depends only on `Cell`.
* **`LabProtocol`** *(functional interface)*: the **abstraction** of the final measurement (`long measure(TachyonSimulator solved)`). It is the piece that enables DIP and choosing via polymorphism what is extracted from the result (splits in A, intensities in B).

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (GridBuilder parses the lines). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`LabController`** *(record)*: the use case. It receives the already-parsed grid and the injected `LabProtocol`, and in `run()` it launches the simulation (`new TachyonSimulator(grid, 0, 1).solve()`) and delegates the measurement to the protocol. It does not know where the data comes from or what exactly is measured.
* **`GridBuilder`** *(Builder pattern, fluent interface)*: builds the `LabController` step by step (`from(lines).using(protocol).build()`). It takes care of **parsing** the raw lines into a `List<List<Cell>>` and guarantees an incomplete controller is never created (if the input or the protocol is missing, it throws).

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade)*: a static point that connects the loader with the injected protocol (`load(file, protocol)`): it reads the lines and passes them to the `GridBuilder`, returning a ready `LabController`.
* **`Main07A` / `Main07B`** *(composition root)*: the single point where the measurement is chosen. Part A injects `solved -> solved.accumulatedDivisions()` and Part B a lambda that sums the intensity of the beams in the last layer; the rest of the flow is identical.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** `LabController` depends on the `LabProtocol` abstraction, not on a concrete measurement; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** measuring something else on the simulation (e.g. the intermediate layer with the most beams) means injecting another lambda; the core stays closed to modification.
* **Single Responsibility (SRP):** `Cell` holds the state of a cell, `TachyonSimulator` only simulates, `LabController` only orchestrates, `GridBuilder` only parses and assembles, `ResourceLineLoader` (shared) only reads I/O.
* **Immutability and robustness:** the simulation is functional (`solve` returns new simulators) and `Cell` is an immutable record, which eliminates side effects in the beam propagation.
* **Builder pattern + fluent interface:** `GridBuilder` builds the controller step by step and validates that it is complete before creating it.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Dependency Injection (DI) / Strategy:** the loader and the measurement are passed from outside; the behavior is chosen without touching the core.
* **High Cohesion and DRY:** the simulation is written once and both parts reuse it; only the final reading changes.
