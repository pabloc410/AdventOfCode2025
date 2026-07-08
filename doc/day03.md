### **Day 3 \- Lobby**

#### **1\. Introduction and Problem**

The setting is the main lobby, where the escalators need emergency power. We have battery banks represented by sequences of digits (e.g. "987654321"). The goal is to select a sub-sequence of digits, respecting their order of appearance, that forms the highest possible number (shake). The challenge has two parts that only change how many digits are chosen from each bank:

* **Part A:** select exactly **2** digits from each bank to maximize the energy (e.g. "987654321" yields 98).
* **Part B:** static friction demands more power, so exactly **12** digits are selected from each bank with the same maximization logic.

Both parts read the same input and share everything: loading the banks, applying the greedy algorithm and summing the results. The only thing that changes is **how many digits** the rule asks for. That is why the rule is modeled as an interchangeable strategy: a *Greedy* algorithm that at each step takes the highest possible digit while ensuring enough digits remain on the right to complete length N.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as days 1 and 2, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day03
    ├── model         (pure domain, depends on nothing)
    │   ├── BatteryBank
    │   └── EnergyProtocol
    ├── control       (orchestrates the use case)
    │   ├── StaircaseController
    │   ├── StaircaseBuilder
    │   └── SearchStrategies
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main03A, b/Main03B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`BatteryBank`** *(record)*: a *Value Object* that only stores the digit sequence of a bank. It knows nothing about files, the algorithm, or how many digits are chosen → **high cohesion**.
* **`EnergyProtocol`** *(functional interface)*: the **abstraction** of the energy rule (`calculateEnergy(String)`). It lives in the domain because it only speaks the domain's language and depends on no other layer. It is the piece that enables DIP.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (Main03A/B parse via InputLoader). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`StaircaseController`** *(record)*: the use case. It receives the already-loaded banks and the injected `EnergyProtocol`, and in `activate()` sums the energy of each bank. It does not know where the banks come from or how each one's energy is computed.
* **`StaircaseBuilder`** *(Builder pattern, fluent interface)*: builds the `StaircaseController` step by step (`from(...).use(...).build()`) and guarantees it is never created incomplete (if the source or the protocol is missing, it throws). It **no longer reads files**: it receives the already-loaded banks, so construction is isolated from I/O.
* **`SearchStrategies`** *(utility class)*: contains the mathematical logic of the Greedy algorithm (`Greedy(sequence, n)`) as a static method, separating the "how to compute" from the "who has the data". Its only reason to change is a change in the selection algorithm.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade, in `application`)*: uses the shared `ResourceLineLoader` to read the lines and parses them into the domain before building the use case.
* **`Main03A` / `Main03B`** *(composition root)*: the single point where the loader and the strategy are chosen and wired with the Builder. Part A and Part B differ only in the injected lambda (`Greedy(s, 2)` vs `Greedy(s, 12)`).

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** `StaircaseController` depends on the `EnergyProtocol` abstraction, not on the concrete algorithm; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** switching from Part A to B (or asking for 50 digits) means injecting another lambda; `StaircaseController` stays closed to modification and open to extension via polymorphism.
* **Single Responsibility (SRP):** `BatteryBank` only holds data, `StaircaseController` only sums, `SearchStrategies` only computes, `ResourceLineLoader` (shared) only reads I/O, `StaircaseBuilder` only assembles.
* **Builder pattern + fluent interface:** `StaircaseBuilder` builds the controller step by step and validates that it is complete before creating it.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Dependency Injection (DI) / Strategy:** the loader and the strategy are passed from outside; the behavior is chosen without touching the core.
* **Low Coupling and DRY:** the processing is independent of the selection algorithm, and input reading is centralized in a single reusable implementation.
