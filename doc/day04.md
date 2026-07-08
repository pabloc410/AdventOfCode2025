### **Day 4 \- Print Department**

#### **1\. Introduction and Problem**

The problem places us in the print shop warehouse, represented by a 2D grid with paper rolls (`@`) and empty spaces (`.`). The goal is to optimize logistics by counting how many rolls are "accessible". A roll is accessible if it has **fewer than 4 roll neighbors** (out of the 8 possible: horizontal, vertical and diagonal). The challenge has two parts that share all the mechanics (parsing the map, looking at neighbors, deciding accessibility) and only change *how* that rule is used:

* **Part A:** count how many rolls are accessible in the initial state (static snapshot).
* **Part B:** full simulation. When the accessible rolls are removed, the ones behind them may become free; the process is repeated in a loop until no more can be removed.

Since the only thing that changes is how the rule is exploited, the use case's "executor" is modeled as an abstraction with two interchangeable implementations (static and dynamic).

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day04
    ├── model         (pure domain, depends on nothing)
    │   ├── CellContent
    │   ├── Coordinate
    │   ├── Executor
    │   └── WarehouseGrid
    ├── control       (orchestrates the use case)
    │   ├── ForkliftOptimizer
    │   ├── PrintShopSolverA
    │   ├── PrintShopSolverB
    │   └── ExecutorFactory
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main04A, b/Main04B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`CellContent`** *(enum)*: encapsulates the representation of the data (`PAPER_ROLL`, `EMPTY`) and its parsing from characters (`fromChar`). It centralizes in a single place "which character means what": the rest of the program talks in high-level terms (`PAPER_ROLL`) and not low-level details (`@`). If the symbol changes tomorrow, a single point is touched.
* **`Coordinate`** *(record)*: only knows how to compute its 8 neighboring coordinates (`neighbors()`) → **high cohesion**.
* **`Executor`** *(functional interface)*: the **abstraction** of the use case (`long execute()`). It lives in the domain because it depends on no other layer; it is the piece that enables DIP and polymorphism between Part A and B.
* **`WarehouseGrid`** *(record)*: manages the matrix. Its `from(...)` adds a safety border of dots (*padding*) around the map, which eliminates bounds checks (`IndexOutOfBounds`) when looking at neighbors. For Part B it is **immutable**: `removeRolls(...)` returns a **new** grid instead of mutating the state.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (InputLoader calls WarehouseGrid.from). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`ForkliftOptimizer`** *(utility class)*: contains exclusively the rule of what a blockage is (`>= 4` roll neighbors) and which rolls are accessible. It is static (a pure function: receives a state, returns a result), so the Part B loop does not instantiate an object on each iteration.
* **`PrintShopSolverA`** *(implements `Executor`)*: purely logical responsibility; receives the warehouse and runs the single computation by delegating to the optimizer. It handles no I/O.
* **`PrintShopSolverB`** *(implements `Executor`)*: manages the simulation loop; receives the initial model and, on each iteration, removes the accessible ones and updates the reference until none remain.
* **`ExecutorFactory`** *(Builder + Factory)*: a hybrid that configures step by step (`from(warehouse).type(A|B).build()`) and creates the correct concrete instance (`PrintShopSolverA` or `PrintShopSolverB`) transparently for the client. It **no longer reads files**: it receives the already-built `WarehouseGrid` and validates that nothing is missing before creating.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade, in `application`)*: uses the shared `ResourceLineLoader` to read the lines and parses them into the domain before building the use case.
* **`Main04A` / `Main04B`** *(composition root)*: the single point where the loader and the type are chosen and wired with the factory. Part A and Part B differ only in the injected `ExecutorType`.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** the `Main` and the factory depend on the `Executor` abstraction, not on the concrete solver classes; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** switching between the static logic (A) and dynamic logic (B) means choosing another `ExecutorType`; the client code stays closed to modification.
* **Builder + Factory pattern:** `ExecutorFactory` configures step by step and hides which concrete implementation is instantiated.
* **Single Responsibility (SRP):** `WarehouseGrid` manages the matrix, `ForkliftOptimizer` only holds the blockage rule, `PrintShopSolverA` does the single computation, `PrintShopSolverB` manages the loop, `ResourceLineLoader` (shared) only reads I/O, `ExecutorFactory` only assembles.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Dependency Injection (DI) / Strategy:** the loader and the type are passed from outside; the behavior is chosen without touching the core.
* **Immutability and robustness:** `removeRolls` returns a new grid (no side effects) and the *padding* simplifies neighbor lookup by removing bounds checks.
* **Low Coupling and DRY:** the accessibility rule is independent of how it is exploited (A or B), and input reading is centralized in a single implementation.
