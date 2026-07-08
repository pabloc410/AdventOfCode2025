### **Day 9 \- Cinema**

#### **1\. Introduction and Problem**

The problem places us in the cinema of the North Pole base, with a floor of red tiles and tiles of other colors. The input is a list of coordinates of the red tiles. The goal is to find the **largest possible rectangle** using two red tiles as opposite corners. The challenge has two parts that share all the mechanics (generating rectangles, sorting them by area) and only change the **geometric constraint**:

* **Part A:** compute the maximum area by forming a rectangle with **any pair** of red tiles, regardless of what is in between.
* **Part B:** the red tiles form the outline of a **polygon**. The rectangle must be **valid**: fully contained inside the polygon (not crossing any edge and having its center inside). It requires intersection and containment checks (*ray casting*).

Since the only thing that changes is the criterion that decides which rectangle counts, both parts are modeled as an abstraction (`AreaSolver`) with two interchangeable implementations, selected by a factory.

#### **2\. Layered architecture**

I reorganized the day into the same **three layers** (plus the shared `common.io` boundary) as the previous days, with dependencies always pointing towards the domain:

```
software.ulpgc.aoc
├── common.io     (input shared by ALL days)
│   ├── LineLoader          (port: List<String> loadLines())
│   └── ResourceLineLoader  (adapter: reads the classpath resource)
└── day09
    ├── model         (pure domain, depends on nothing)
    │   ├── Coordinate
    │   ├── Rectangle
    │   └── AreaSolver
    ├── control       (orchestrates the use case)
    │   ├── RectangleFinder
    │   ├── MaxAreaSolver
    │   ├── AllowedAreaSolver
    │   └── AreaSolverFactory
    └── application   (details and startup)
        ├── InputLoader   (parses the lines into the domain)
        └── a/Main09A, b/Main09B
```

**Dependency direction:** `application → control → model` and `application → common.io`. The domain (`model`) imports no other layer; the shared loader (`common.io`) depends on nothing either.

#### **3\. Class-by-class explanation**

**`model` layer (pure domain)**

* **`Coordinate`** *(record)*: an immutable *Value Object* with the (x, y) coordinates. It encapsulates its parsing from text (`from("7,1")`), centralizing the data transformation.
* **`Rectangle`** *(record)*: a rich *Value Object* that not only holds two corners, but **encapsulates all the geometry**: width, height, area, whether it is vertical and its bounds (`minX`, `maxX`, `minY`, `maxY`). This way the geometric computations are not scattered across the rest of the program → **high cohesion**.
* **`AreaSolver`** *(functional interface)*: the **abstraction** of the use case (`long solve()`). It lives in the domain because it depends on no other layer; it is the piece that enables DIP and polymorphism between Part A and B.

**Input boundary (shared: `common.io`)**

* **`LineLoader`** *(interface, port)* and **`ResourceLineLoader`** *(adapter)*: they live in the shared package `software.ulpgc.aoc.common.io` and are reused by **every day**. `loadLines()` returns the raw lines of the resource (`List<String>`); parsing into the domain happens afterwards, in the `application` layer (InputLoader parses with Coordinate.from). This centralizes reading (a single implementation, no duplication) and separates it from interpretation (SRP).

**`control` layer (orchestrates the use case)**

* **`RectangleFinder`** *(algorithmic engine)*: contains the combinatorial search. It generates all possible rectangles sorted by area (`generateRectangles`), finds the largest (`findLargest`, Part A) and the largest valid one inside the polygon (`findLargestAllowed`, Part B), with its geometry helpers (polygon edges, intersection and containment via *ray casting*).
* **`MaxAreaSolver`** *(implements `AreaSolver`)*: the Part A strategy; delegates to the finder and returns the area of the largest rectangle.
* **`AllowedAreaSolver`** *(implements `AreaSolver`)*: the Part B strategy; returns the area of the largest allowed rectangle.
* **`AreaSolverFactory`** *(Builder + Factory)*: configures step by step (`from(tiles).type(A|B).build()`) and creates the correct concrete implementation transparently, validating that nothing is missing.

**`application` layer (details and startup)**

* **`InputLoader`** *(assembly facade)*: reads the lines, parses them into coordinates (`Coordinate::from`) and configures the correct `AreaSolver` via the factory (`loadMaxArea`, `loadAllowedArea`).
* **`Main09A` / `Main09B`** *(composition root)*: the single point where the strategy is chosen. The rest of the flow is identical: `solver.solve()`.

#### **4\. Principles and designs applied**

* **Dependency Inversion (DIP):** the `Main`s and the factory depend on the `AreaSolver` abstraction, not on the concrete classes; startup depends on the `LineLoader` interface, not on how the data is read.
* **Open/Closed (OCP):** adding another constraint (e.g. rectangles of a maximum size) means creating another `AreaSolver` and injecting it through the factory; the `RectangleFinder` engine stays unchanged.
* **Builder + Factory pattern:** `AreaSolverFactory` configures step by step and hides which concrete implementation is instantiated.
* **Single Responsibility (SRP):** `Rectangle` holds the geometry, `RectangleFinder` only the search, each *Solver* only its variant, `ResourceLineLoader` (shared) only reads I/O, `AreaSolverFactory` only assembles.
* **Interface Segregation (ISP):** the shared port `LineLoader` exposes a single cohesive method (`loadLines`).
* **Factory Method pattern:** `Coordinate.from(...)` turns raw text into an already-valid domain object.
* **Immutability:** `Coordinate` and `Rectangle` are immutable records, which provides safety during the massive generation of combinations in streams.
* **Dependency Injection (DI) / Strategy:** the tiles and the type are passed from outside; the behavior is chosen without touching the core.
