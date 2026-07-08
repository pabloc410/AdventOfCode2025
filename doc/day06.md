### **Day 6 \- Garbage Compactor**

#### **1\. Introduction and Problem**

The setting is a garbage compactor where some cephalopods need help with a math homework sheet. The input is a grid of characters where the problems are laid out visually in columns and rows, with an operator (`+` or `*`) at the foot of each block. The challenge is to **interpret the same grid in two different ways** to extract operands and sum the result of all operations:

* **Part A:** the numbers are written in rows aligned by columns. Each problem is a list of numbers and its associated operator.
* **Part B:** "Cephalopod Math". The columns stop being whole numbers and become positional digits; the numbers are read vertically across the columns and the problems are separated by empty columns.

Since the only thing that changes is *how* the same grid is parsed, that logic is modeled as an **interchangeable strategy** (`OperationBuilder`) with two implementations: the vertical analyzer (A) and the cephalopod one (B). The computation engine is identical for both.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day06
    ├── model         (pure domain, depends on nothing)
    │   ├── Operator
    │   ├── Operation
    │   └── OperationBuilder
    ├── control       (orchestrates the use case)
    │   ├── CompactorController
    │   ├── VerticalAnalyzer
    │   └── CephalopodAnalyzer
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main06A, b/Main06B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`Operator`** *(functional enum)*: encapsulates the arithmetic. By implementing `BinaryOperator<Long>`, the enum is not just a label but an **executable function**: it knows how to operate (`apply`), what its identity value is (`0` for addition, `1` for multiplication) and how to parse itself from a character (`from`, `isOperator`). It centralizes the arithmetic logic in a single point.
* **`Operation`** *(record)*: a *Value Object* that groups the operands and their operator. Its only responsibility is to orchestrate the final computation through a reduction (`calculate()`), delegating the pure math to the `Operator` → **high cohesion**.
* **`OperationBuilder`** *(interface)*: the **abstraction** of the parsing strategy (`addLine(String)` + `Stream<Operation> build()`). It lives in the domain because it only speaks the domain's language; it is the piece that allows swapping how the grid is interpreted without touching the rest.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (the injected OperationBuilder parses). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`CompactorController`**: the use case. It receives an already-parsed `Stream<Operation>` and in `execute()` sums the result of each `calculate()`. It completely ignores where the data came from or how it was parsed → low coupling.
* **`VerticalAnalyzer`** *(implements `OperationBuilder`)*: the Part A strategy (numbers by aligned columns).
* **`CephalopodAnalyzer`** *(implements `OperationBuilder`)*: the Part B strategy (positional digits read vertically, problems separated by empty columns).

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade)*: a static point that connects the loader with the injected strategy (`load(filename, builder)`): it reads the lines, feeds them to the `OperationBuilder` and returns a ready `CompactorController`. It isolates the wiring from I/O.
* **`Main06A` / `Main06B`** *(composition root)*: the single point where the strategy is chosen. Part A injects `new VerticalAnalyzer()` and Part B `new CephalopodAnalyzer()`; the rest of the flow is identical.

#### **4\. Principles and designs applied**

* **Strategy pattern:** `OperationBuilder` defines the family of parsing algorithms (`VerticalAnalyzer`, `CephalopodAnalyzer`) and makes them interchangeable without touching the loader or the controller.
* **Dependency Inversion (DIP):** `CompactorController` only knows a `Stream<Operation>` and the assembly depends on the `OperationBuilder` abstraction, not on the concrete analysis classes; startup depends on the `LineLoader` interface.
* **Open/Closed (OCP):** if a "Part C" appeared (e.g. diagonal reading), it is enough to create a new `OperationBuilder` and inject it; the computation engine and the loading stay unchanged.
* **Single Responsibility (SRP):** `Operator` holds the arithmetic, `Operation` orchestrates its computation, the analyzers only parse, `CompactorController` only sums, `ResourceLineLoader` only reads I/O, `InputLoader` only assembles.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Dependency Injection (DI):** the parsing strategy is passed from outside; the behavior is chosen without touching the core.
* **High Cohesion and DRY:** the arithmetic is centralized in the functional enum `Operator` (avoiding conditional blocks), and input reading in a single implementation.
