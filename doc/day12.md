### **Day 12 \- Christmas Tree Farm**

#### **1\. Introduction and Problem**

The setting is a cavern under the North Pole, a Christmas tree farm where the elves place gifts under the trees. The gifts have irregular shapes (polyominoes) and the areas are grids of specific sizes (e.g. 4x4, 12x5). The input has two sections: a **catalog of shapes** and a **list of regions** with the quantity of each gift that must fit. It is a classic **backtracking / packing** (*tiling*) problem:

* The gifts can be **rotated** (90º, 180º…) and **flipped** (mirror), but cannot overlap or go outside the bounds.
* The goal (the only part of the challenge) is to count **how many regions are solvable**: in how many all the assigned gifts fit without collisions.

Unlike other days, day 12 has **a single part**. The interesting variation is internal to the solver: it automatically chooses between two board representations (a `long` mask or a `BitSet`) depending on the region size, an encapsulated optimization decision that the client does not see.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day12
    ├── model         (pure domain, depends on nothing)
    │   ├── Coordinate
    │   ├── Shape
    │   ├── Region
    │   └── ProblemDefinition
    ├── control       (orchestrates the use case)
    │   ├── FarmSolver
    │   └── FarmController
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main12A
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`Coordinate`** *(record)*: an immutable *Value Object* (row, column) that encapsulates its basic geometric transformations: `rotate()` and `flip()`.
* **`Shape`** *(record)*: a polyomino. It concentrates **all the geometry**: area, generation of the 8 isometries (`generateVariations`), rotation, flip and normalization. By keeping data and transformations together, the solver only asks for the variants instead of doing vector math → **high cohesion**.
* **`Region`** *(record)*: the area to fill (width, height). It knows its area and whether it is "small" (`isSmall`, ≤ 64 cells), a criterion that guides the solver's optimization.
* **`ProblemDefinition`** *(record)*: groups a region with the list of pieces that must fit in it. It is the concrete case to solve.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (InputLoader parses the two sections: catalog + problems). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`FarmSolver`** *(algorithmic engine)*: the pure backtracking logic. It discards quickly by area, sorts the pieces (largest first) and dynamically chooses the board representation: a `long` mask for small regions or a `BitSet` for large ones. It precomputes the valid placements of each piece and tries combinations pruning collisions. It knows nothing about files or the overall format.
* **`FarmController`** *(use case)*: iterates over the `ProblemDefinition`s and counts how many are solvable (`countValidRegions`), delegating each one to a `FarmSolver`. It hides from the `Main` whether bit masks or recursion are used inside.

**`application` layer (details and startup)**

* **`InputLoader`** *(parsing and assembly facade)*: encapsulates the **complex parsing** of the two-section input (catalog of shapes and problem definitions) and builds the `FarmController`. It exposes `load(file)` for startup and `fromLines(lines)` for the tests.
* **`Main12A`** *(composition root)*: the single point where the loader is wired with the controller and the result is requested.

#### **4\. Principles and designs applied**

* **Single Responsibility (SRP):** `Shape` holds the geometry, `Region` its size, `FarmSolver` only the backtracking, `FarmController` only iterates and counts, `InputLoader` only parses and assembles, `ResourceLineLoader` (shared) only reads I/O.
* **High Cohesion:** `Shape` concentrates all the transformations (rotate, flip, normalize, variations) in a single place.
* **Abstraction / encapsulation:** the choice between a `long` mask and a `BitSet` is an **internal** detail of `FarmSolver`; the client only calls `solve(...)`. It is an example of a Strategy chosen internally by a condition (region size), not injected (not needed: there is only one use case → **YAGNI**).
* **Dependency Inversion (DIP) / ISP:** startup depends on the `LineLoader` interface, which exposes a single cohesive method (`loadLines`).
* **Immutability and Value Objects:** the records (`Coordinate`, `Shape`, `Region`, `ProblemDefinition`) guarantee that the thousands of rotations and translations generate **new** instances without corrupting the original shapes in the catalog.
* **Factory Method pattern:** the creation of shapes and problems is centralized in `InputLoader`, encapsulating the two-section format.
* **Performance:** representing the board as bits allows checking collisions with `AND`/`OR` operations in O(1), and the backtracking pruning avoids exploring invalid branches.
