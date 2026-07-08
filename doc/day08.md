### **Day 8 \- Playground**

#### **1\. Introduction and Problem**

The setting is an underground playground where the elves install Christmas lights. The input is a list of 3D coordinates (X, Y, Z) representing junction boxes; each box starts as its own independent circuit. The boxes are connected by cables, always prioritizing the closest ones (smallest Euclidean distance). The challenge has two parts that share the loading and the distance computation, and differ in *which connectivity algorithm* is applied:

* **Part A:** make exactly the **1000 shortest connections** and, at the end, multiply the sizes of the **three largest circuits** (safety factor).
* **Part B:** ignore the limit and keep joining cables until all boxes form **a single circuit**; the result is `X1 * X2` of the last pair that causes the total unification.

Since both parts are different algorithms over the same set of boxes, they are modeled as an abstraction (`CircuitSolver`) with two interchangeable implementations, selected by a factory.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day08
    ├── model         (pure domain, depends on nothing)
    │   ├── Box
    │   ├── BoxPair
    │   ├── Circuit
    │   └── CircuitSolver
    ├── control       (orchestrates the use case)
    │   ├── CircuitConnector
    │   ├── SafetyFactorSolver
    │   ├── MergeCostSolver
    │   └── SolverFactory
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main08A, b/Main08B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`Box`** *(record)*: an immutable *Value Object* with the 3D coordinates. It encapsulates the geometry: it knows how to compute the Euclidean distance to another box (`distanceTo`), keeping data and logic together → **high cohesion**.
* **`BoxPair`** *(record)*: a transfer object that associates two boxes with the **pre-computed** distance between them, which allows sorting by distance without recomputing the formula.
* **`Circuit`** *(record)*: a logical grouping of connected boxes (a `Set<Box>`). It also encapsulates its own parsing (`fromText`), turning a line `"x,y,z"` into a single-box circuit.
* **`CircuitSolver`** *(functional interface)*: the **abstraction** of the use case (`long solve()`). It lives in the domain because it depends on no other layer; it is the piece that enables DIP and polymorphism between Part A and B.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (InputLoader parses with Circuit.fromText). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`CircuitConnector`** *(algorithmic engine)*: contains exclusively the combinatorial logic: generating pairs, sorting them by distance (in parallel), merging circuits and computing both the safety factor (`calculateSafetyFactor`) and the unification cost (`calculateMergeCost`). It is the "how" of the algorithm, separated from the "what" of the use case.
* **`SafetyFactorSolver`** *(implements `CircuitSolver`)*: the Part A strategy; receives the circuits and the number of connections and delegates to the connector.
* **`MergeCostSolver`** *(implements `CircuitSolver`)*: the Part B strategy; receives the circuits and delegates the computation of the unification cost.
* **`SolverFactory`** *(Builder + Factory)*: a hybrid that configures step by step (`from(circuits).type(A|B).connections(n).build()`) and creates the correct concrete implementation transparently for the client, validating that nothing is missing before building.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade)*: a static point that reads the lines, parses them into circuits (`Circuit::fromText`) and configures the correct `CircuitSolver` via the factory (`loadSafetyFactor`, `loadMergeCost`).
* **`Main08A` / `Main08B`** *(composition root)*: the single point where the strategy is chosen. Part A requests the safety-factor solver with 1000 connections; Part B the unification one. The rest of the flow is identical: `solver.solve()`.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** the `Main`s and the factory depend on the `CircuitSolver` abstraction, not on the concrete classes; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** adding a "Part C" means creating another `CircuitSolver` and injecting it through the factory; the engine (`CircuitConnector`) and the loading stay unchanged.
* **Builder + Factory pattern:** `SolverFactory` configures step by step and hides which concrete implementation is instantiated.
* **Single Responsibility (SRP):** `Box` holds the geometry, `Circuit` the grouping and its parsing, `CircuitConnector` only the algorithm, each *Solver* only its variant, `ResourceLineLoader` (shared) only reads I/O, `SolverFactory` only assembles.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Immutability and parallel safety:** since `Box`, `BoxPair` and `Circuit` are immutable records, the parallel processing of pairs (`parallel()`) is free of race conditions.
* **Dependency Injection (DI) / Strategy:** the circuits and the type are passed from outside; the behavior is chosen without touching the core.
* **High Cohesion and DRY:** the geometry is in `Box`, the parsing in `Circuit`, and input reading centralized in a single implementation.
