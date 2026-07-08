### **Day 2 \- Gift Shop**

#### **1\. Introduction and Problem**

The problem places us in a gift shop with a corrupted database. We have ranges of product IDs (e.g. "10-20") and must find which ones are invalid and sum them. The challenge has two parts that change the definition of "invalid":

* **Part A:** An ID is invalid if it is made of a sequence repeated **exactly twice** (e.g. 1212, or 11).
* **Part B:** An ID is invalid if the sequence repeats **two or more times** (e.g. 121212 and 111 also count, in addition to those of Part A).

Both parts share everything: reading the ranges, expanding them into IDs and summing the invalid ones. The only thing that changes is the rule that decides whether an ID is invalid. That is why that rule is modeled as an interchangeable strategy.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as day 1, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day02
    ├── model         (pure domain, depends on nothing)
    │   └── IdRange
    ├── control       (orchestrates the use case)
    │   ├── Engine
    │   ├── EngineBuilder
    │   └── ValidationStrategies
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main02a, b/Main02b
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`IdRange`** *(record)*: represents a range of IDs (e.g. "10-20"). It has a "translator" constructor that receives the dirty `String` from the file and converts it into two `long`s. It knows how to expand into a stream of numbers (`getIds()`) and filter the invalid ones according to a rule (`getInvalidIds(validator)`). It knows nothing about files, the engine, or which rule decides validity → **high cohesion**.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (Main02a/b parse via InputLoader). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`Engine`** *(record)*: the use case. It receives the already-loaded ranges and the validation strategy (`LongPredicate`), and in `run()` it goes through each range, keeps the invalid IDs and sums them. It does not know where the ranges come from or how each ID is validated.
* **`EngineBuilder`** *(Builder pattern, fluent interface)*: builds the `Engine` step by step (`from(...).use(...).runner()`) and guarantees it is never created incomplete (if the source or the strategy is missing, it throws). It **no longer reads files**: it receives the already-loaded ranges, so construction is isolated from I/O.
* **`ValidationStrategies`** *(utility class)*: groups the two business rules (`PATTERN_A`, `PATTERN_B`) as `LongPredicate` constants based on regular expressions. Its only reason to change is a change in the definition of an "invalid" ID.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade, in `application`)*: uses the shared `ResourceLineLoader` to read the lines and parses them into the domain before building the use case.
* **`Main02a` / `Main02b`** *(composition root)*: the single point where the loader and the strategy are chosen and wired with the Builder. Part A and Part B differ only in the injected strategy.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** the `Engine` depends on the `LongPredicate` abstraction (receives a `long` and says whether it is valid), not on a concrete rule; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** switching from Part A to B (or adding a new rule) means injecting another strategy; the `Engine` stays closed to modification.
* **Single Responsibility (SRP):** `IdRange` only understands ranges, `Engine` only processes, `ValidationStrategies` only holds the rules, `ResourceLineLoader` (shared) only reads I/O, `EngineBuilder` only assembles.
* **Builder pattern + fluent interface:** `EngineBuilder` builds the `Engine` step by step and validates that it is complete before creating it.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Dependency Injection (DI) / Strategy:** the loader and the strategy are passed from outside; the behavior is chosen without touching the core.
* **Low Coupling and DRY:** the processing is independent of the validation rules, and input reading is centralized in a single reusable implementation.
