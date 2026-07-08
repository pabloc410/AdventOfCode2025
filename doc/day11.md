### **Day 11 \- Reactor**

#### **1\. Introduction and Problem**

The setting is a factory where a large toroidal reactor has connection problems with a server rack. The input is a list of devices and their one-way connections: a **directed graph**. The challenge is to count routes through that network, and it shares all the mechanics (parsing the graph, traversing with DFS + memoization) changing only *which query* is made:

* **Part A:** compute the total number of distinct routes from the start (`you`) to the exit (`out`).
* **Part B:** count the routes from the server (`svr`) to the exit (`out`) that must pass through two intermediate nodes (`dac` and `fft`), in **any order**. It is decomposed into segments and their counts are multiplied.

Since the only thing that changes is the query over the same graph, both parts are modeled as an abstraction (`RouteSolver`) with two interchangeable implementations, selected by a factory.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day11
    ├── model         (pure domain, depends on nothing)
    │   ├── RouteGraph
    │   └── RouteSolver
    ├── control       (orchestrates the use case)
    │   ├── RouteAnalyzer
    │   ├── TotalRoutesSolver
    │   ├── CriticalRoutesSolver
    │   └── RouteSolverFactory
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main11A, b/Main11B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`RouteGraph`** *(record)*: a *Value Object* that encapsulates the graph (`Map<String, List<String>>`). It offers a high-level operation (`neighborsOf`) instead of exposing the raw map, and centralizes its construction from text (`from(List<String>)`) → **high cohesion** and **Tell-Don't-Ask**.
* **`RouteSolver`** *(functional interface)*: the **abstraction** of the use case (`long solve()`). It lives in the domain because it depends on no other layer; it is the piece that enables DIP and polymorphism between Part A and B.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (InputLoader parses with RouteGraph.from). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`RouteAnalyzer`** *(algorithmic engine)*: concentrates the graph algorithmics. `countRoutes` traverses with **DFS and memoization** (a `Map` of partial results) to avoid recomputing; `countRoutesWithIntermediates` decomposes Part B into segments and multiplies their counts. It handles no I/O or global state.
* **`TotalRoutesSolver`** *(implements `RouteSolver`)*: the Part A strategy; queries `you → out`.
* **`CriticalRoutesSolver`** *(implements `RouteSolver`)*: the Part B strategy; queries `svr → out` passing through `dac` and `fft`.
* **`RouteSolverFactory`** *(Builder + Factory)*: configures step by step (`from(graph).type(A|B).build()`) and creates the correct concrete implementation transparently.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade)*: reads the lines, builds the `RouteGraph` (`RouteGraph.from`) and configures the correct `RouteSolver` via the factory (`loadTotalRoutes`, `loadCriticalRoutes`).
* **`Main11A` / `Main11B`** *(composition root)*: the single point where the strategy is chosen. The rest of the flow is identical: `solver.solve()`.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** the `Main`s and the factory depend on the `RouteSolver` abstraction, not on the concrete classes; startup depends on the `LineLoader` interface.
* **Open/Closed (OCP):** adding another query (e.g. routes that avoid a node) means creating another `RouteSolver` and injecting it; the `RouteAnalyzer` engine stays unchanged.
* **Builder + Factory pattern:** `RouteSolverFactory` configures step by step and hides which concrete implementation is instantiated.
* **Single Responsibility (SRP):** `RouteGraph` holds the graph, `RouteAnalyzer` only the algorithmics, each *Solver* only its query, `ResourceLineLoader` (shared) only reads I/O, the factory only assembles.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **DRY:** Part B reuses `countRoutes` by decomposing into segments instead of duplicating the DFS; reading is centralized in one implementation.
* **Factory Method pattern:** `RouteGraph.from(...)` turns raw text into the already-valid domain graph.
* **Memoization / performance:** storing partial results in a `Map` avoids recomputing sub-paths, key in a combinatorial problem.
