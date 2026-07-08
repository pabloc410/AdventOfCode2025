### **Day 1 \- Secret Entrance**

#### **1\. Introduction and Problem**

The problem is to simulate a safe with a circular dial (0-99). It starts at 50; turning left (L10) subtracts, and turning right (R10) adds. The challenge has two parts that change the rule for how the password is obtained:

* **Part A:** The password depends on **where the dial ends up** after each rotation: add 1 every time a turn lands exactly on 0.
* **Part B:** The password counts **how many times the dial passes through zero** during the turn (not only where it ends, but every intermediate crossing).

Both parts read the same input and share all the dial mechanics; the only thing that changes is the scoring rule. That observation guides the design: if the only thing that varies is a rule, that rule must be pluggable from the outside without touching the rest.

#### **2\. Layered architecture**

I reorganized the day into **three layers** (plus the shared `common.io` boundary), so that dependencies always point towards the domain and never the other way:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day01
    ├── model         (pure domain, depends on nothing)
    │   ├── Dial
    │   ├── Instruction
    │   └── SecurityProtocol
    ├── control       (orchestrates the use case)
    │   ├── Safe
    │   └── SecurityProtocols
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main01a, b/Main01b
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`Dial`** *(immutable record)*: represents the wheel and its only task is circular arithmetic. Its constructor always normalizes the position to the 0-99 range, and `rotate(int)` returns a **new** `Dial` instead of mutating the current one. It knows nothing about files or scoring rules → **high cohesion**.
* **`Instruction`** *(record)*: represents a turn command ("L10", "R48"). It concentrates parsing and validation in `of(String)`, which returns an `Optional` and discards invalid input (nulls, empty, garbage). It exposes `movement()` (L subtracts, R adds). Pulling this out of the safe is what gives `Safe` a single responsibility.
* **`SecurityProtocol`** *(functional interface)*: the **abstraction** of the scoring rule (`calculatePoints(oldDial, movement, newDial)`). It lives in the domain because it only speaks the domain's language (`Dial`) and depends on no other layer. It is the piece that enables DIP.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (Main01a/b parse via InputLoader). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`Safe`**: the heart of the use case. It keeps the state (current dial and counter) and, for each instruction, turns the dial and delegates scoring to the injected `SecurityProtocol`. It does not read files or parse text: it receives already-validated `Instruction`s. `rotate(String)` is kept for convenience/robustness and delegates to `Instruction.of`.
* **`SecurityProtocols`**: groups the two concrete strategies (`PART_A`, `PART_B`) as reusable constants. Adding a new rule means creating another constant, without touching `Safe`.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade, in `application`)*: uses the shared `ResourceLineLoader` to read the lines and parses them into the domain before building the use case.
* **`Main01a` / `Main01b`** *(composition root)*: the single point where the concrete pieces are chosen (which loader and which protocol) and wired by injection. Part A and Part B differ only in the injected strategy.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** `Safe` depends on the `SecurityProtocol` abstraction, not on the concrete logic of A or B; startup depends on `LineLoader`, not on how the data is read.
* **Open/Closed (OCP):** adding a rule means injecting another strategy; `Safe` stays closed to modification and open to extension via polymorphism.
* **Single Responsibility (SRP):** `Dial` only does arithmetic, `Instruction` only parses/validates, `Safe` only orchestrates, `ResourceLineLoader` (shared) only reads I/O.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Dependency Injection (DI) / Strategy pattern:** the protocol and the loader are passed from outside; the behavior is chosen without touching the core.
* **Low Coupling and High Cohesion:** the layers communicate through abstractions and each class groups what is closely related.
* **DRY:** input reading is centralized in a single reusable implementation.
* **Tell, Don't Ask / encapsulation:** `Safe` is told to apply an instruction (`apply`) instead of being asked for its state to decide outside; the `Dial` is immutable and normalizes in its own constructor.
