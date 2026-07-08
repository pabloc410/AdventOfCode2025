### **Day 5 \- Cafeteria**

#### **1\. Introduction and Problem**

The problem places us in the elves' cafeteria, where a corrupted inventory database prevents telling fresh ingredients apart. The input consists of a list of **freshness ranges** (e.g. `3-5`, `10-20`) and, after a blank line, a list of specific **ingredient IDs**. The challenge has two parts that share the input (loading the file, separating ranges from IDs, encapsulating the interval math) and only change *how* the ranges are exploited:

* **Part A:** count how many of the IDs in the list fall within **at least one** freshness range.
* **Part B:** ignore the list of IDs and compute the **total coverage**: how many unique integers the union of all ranges covers, merging the overlapping ones (e.g. `10-15` and `12-20` must not count the shared numbers twice).

Since the only thing that changes is how the ranges are exploited, the rule is modeled as an abstraction (`FreshnessProtocol`) with interchangeable implementations: the standard policy (A) and a null protocol (B), which delegates all the power to the interval-merging algorithm.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day05
    ├── model         (pure domain, depends on nothing)
    │   ├── Range
    │   └── FreshnessProtocol
    ├── control       (orchestrates the use case)
    │   ├── InventoryAuditor
    │   └── AuditBuilder
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main05A, b/Main05B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`Range`** *(record)*: a *Value Object* that encapsulates all the interval math. It knows whether it contains a number (`contains`), its length (`length`), whether it overlaps another range (`overlapsWith`) and how to merge with another (`merge`), plus its natural ordering (`Comparable`). By concentrating this logic here → **high cohesion** and the auditor stays clean.
* **`FreshnessProtocol`** *(functional interface)*: the **abstraction** of the freshness rule (`boolean isFresh(long ingredientId, List<Range> ranges)`). It lives in the domain because it depends on no other layer; it is the piece that enables DIP and choosing the behavior (A vs B) via polymorphism.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (AuditBuilder parses the two sections). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`InventoryAuditor`**: the use case. It receives the ranges, the IDs and the injected `FreshnessProtocol`. `audit()` counts the fresh IDs by delegating to the protocol; `calculateTotalCoverage()` implements the **interval-merging** algorithm (sort and unite overlapping ones) for the total coverage of Part B.
* **`AuditBuilder`** *(Builder pattern, fluent interface)*: builds the `InventoryAuditor` step by step (`from(lines).using(protocol).build()`). It **no longer reads files**: it receives the already-loaded lines and takes care of **parsing** the two sections (ranges before the blank line, IDs after), guaranteeing an incomplete auditor is never created.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade)*: uses the shared `ResourceLineLoader` to read the lines and builds the `InventoryAuditor` with the `AuditBuilder` and the injected protocol.
* **`Main05A` / `Main05B`** *(composition root)*: the single point where the protocol is chosen and the result is requested. Part A injects `standardPolicy` (`(id, ranges) -> ranges.stream().anyMatch(r -> r.contains(id))`) and calls `audit()`; Part B injects a null protocol and calls `calculateTotalCoverage()`.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** `InventoryAuditor` depends on the `FreshnessProtocol` abstraction, not on a concrete rule; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** changing the freshness rule (e.g. excluding even numbers) means injecting another lambda; the core stays closed to modification and open to extension via polymorphism.
* **Single Responsibility (SRP):** `Range` holds the interval math, `InventoryAuditor` orchestrates the computation, `AuditBuilder` only parses and assembles, `ResourceLineLoader` (shared) only reads I/O.
* **Builder pattern + fluent interface:** `AuditBuilder` builds the auditor step by step and validates that it is complete before creating it.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Dependency Injection (DI) / Strategy:** the loader and the protocol are passed from outside; the behavior is chosen without touching the core.
* **High Cohesion and DRY:** the interval math is centralized in `Range`, and input reading in a single reusable implementation.
