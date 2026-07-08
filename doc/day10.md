### **Day 10 \- Factory**

#### **1\. Introduction and Problem**

The setting is a factory where machines must be repaired. Each machine has a set of buttons and an indicator panel with lights and voltages. Pressing a button toggles specific states (lights on/off) and increases the voltages of certain components. The challenge has two parts that share the model (machines, buttons, indicators) and differ in *what each machine is asked to solve*:

* **Part A:** find the **minimum** sequence of presses so the lights match a target configuration, ignoring the voltages (solved with BFS over subsets of buttons).
* **Part B:** meet strict **accumulated voltage** requirements. It requires a **recursive** search (with memoization) that first solves the parity of the lights and then mathematically adjusts the remaining voltages.

Since both parts iterate over the same list of machines and only the function that solves each one changes, that function is modeled as an injectable abstraction (`MachineSolver`): a different strategy for A and B over the same controller.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day10
    ├── model         (pure domain, depends on nothing)
    │   ├── State
    │   ├── Button
    │   ├── Indicator
    │   ├── Machine
    │   └── MachineSolver
    ├── control       (orchestrates the use case)
    │   └── FactoryController
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main10A, b/Main10B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`State`** *(enum)*: the lights (`ON`, `OFF`). It hides the low-level representation (`#` / `.`) behind semantic concepts via `fromChar` and `parse`. The rest of the system talks about logical states, not characters.
* **`Button`** *(record)*: an atomic *Value Object*; it only holds the set of indices it affects and its parsing (`from`).
* **`Indicator`** *(record)*: groups closely-related data (light states and voltages) and operates on them **immutably**: `reduceVoltagesWith`, `voltageHalf`, `toggleState` and `createInitialState` return **new** instances instead of mutating → **high cohesion**.
* **`Machine`** *(record)*: the solving engine, **pure and immutable**. It centralizes the algorithms: BFS over button masks (`solveMinPresses`, Part A) and the recursive search with cache (`solveVoltageRequirements`, Part B). It does not read files or print: it only computes costs. It lives in the domain because it depends only on `State`, `Button` and `Indicator`.
* **`MachineSolver`** *(functional interface)*: the **abstraction** of "how to solve a machine" (`int solve(Machine machine)`). It is the piece that enables DIP and choosing the A or B strategy via polymorphism.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (InputLoader parses with Machine.from). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`FactoryController`**: the use case. It receives the list of machines and the injected `MachineSolver`, and in `execute()` sums the cost the solver computes for each machine. It ignores whether inside it is BFS (A) or recursion (B).

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade)*: reads the lines, parses them into machines (`Machine::from`) and wires everything with the injected `MachineSolver`, returning a ready `FactoryController`.
* **`Main10A` / `Main10B`** *(composition root)*: the single point where the strategy is chosen. Part A injects `Machine::solveMinPresses` and Part B `machine -> machine.solveVoltageRequirements(new HashMap<>())`; the rest of the flow is identical.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** `FactoryController` depends on the `MachineSolver` abstraction, not on a concrete algorithm; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** asking for another way to solve a machine means injecting another strategy; `FactoryController` stays closed to modification.
* **Single Responsibility (SRP):** `Button` holds the indices, `Indicator` the panel data, `Machine` only the algorithms, `FactoryController` only aggregates, `ResourceLineLoader` (shared) only reads I/O, `InputLoader` only assembles.
* **Immutability and robustness:** the records (`Button`, `Indicator`, `Machine`) guarantee the state does not change unexpectedly; methods like `applyButton` return a new `Indicator`, which avoids side effects and makes the recursion and BFS safe.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Factory Method pattern:** `Machine.from`, `Button.from`, `Indicator.from` and `State.fromChar` turn raw text into already-valid domain objects.
* **Dependency Injection (DI) / Strategy:** the machines and the solving strategy are passed from outside; the behavior is chosen without touching the core.
* **Abstraction / Tell-Don't-Ask:** the system works with `State.ON/OFF` and delegates the computations to the domain objects themselves instead of manipulating characters or raw lists.
