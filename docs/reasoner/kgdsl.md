# KGDSL language guide

KGDSL (Knowledge Graph Domain Specific Language) is OpenSPG’s “programmable reasoning” language. It is designed for:

- Describing **graph patterns** (what subgraph you want to match).
- Expressing **logical constraints and derived variables** (what should be true / how to compute).
- Performing **actions**:
  - **Query/projection**: return a table-like result (`get(...)`).
  - **Derivation/materialization**: create nodes/edges (`createNodeInstance`, `createEdgeInstance`).

If you are new to the runtime pipeline, read `docs/architecture.md` and `docs/reasoner/pipeline.md` after this page.

## Which “mode” should you use?

KGDSL supports multiple entry forms (all handled by the same parser):

1. **Query mode (most common for analytics/debugging)**  
   Use `GraphStructure { ... } Rule { ... } Action { get(...) }` to return a tabular result (`Row`).

2. **Define mode (predicate / rule definition)**  
   Use `Define (...) { ... }` to define a derived predicate (often `belongTo` or a derived relation) and optionally materialize it via `Action { createEdgeInstance(...) }` / `Action { createNodeInstance(...) }`.

3. **GQL subset mode (Cypher-like shape)**  
   `MATCH ... WHERE ... RETURN ...` is supported as a simplified query form and compiles into the same planning stack.

Terminology note: the grammar accepts both `GraphStructure` and `Structure`, and both `Rule` and `Constraint`. They are equivalent headings.

Comments are supported: `// line comment` and `/* block comment */`.

## Quick start

### 1) Minimal query (pattern + projection)

```
GraphStructure {
  (u:User)-[e:lk]->(v:User)
}
Rule {
  R1("adult to adult"): u.age > 18 && v.age > 18
}
Action {
  get(u.id as uid, v.id as vid)
}
```

### 2) Aggregation in Rule, then project it

```
GraphStructure {
  s [CustFundKG.Account, __start__='true']
  inUser,outUser [CustFundKG.Account]
  inUser -> s [accountFundContact] as in1
  s -> outUser [accountFundContact] as out1
}
Rule {
  inCnt = group(s).count(inUser.id)
  outAmt = group(s).sum(out1.sumAmt)
  ok = rule_value(inCnt >= 2 && outAmt > 100, true, false)
}
Action {
  get(s.id, ok, inCnt, outAmt)
}
```

Why compute first? `get(...)` only accepts variables/properties/functions, not full arithmetic expressions. If you need `a+b`, compute it in `Rule` first and then `get(sum)`.

### 3) Dynamic classification (`belongTo`) with a concept instance

In SPG terms, `belongTo` links an entity instance to a concept instance when the rule holds.

```
Define (s:Person)-[p:belongTo]->(o:UserClass/`ManyDeviceUser`) {
  Structure {
    (s)-[t:has]->(u:Device)
  }
  Constraint {
    has_device_num("device count") = group(s).count(u.id)
    R1("more than 100 devices"): has_device_num > 100
    R2("adult"): s.age > 18
  }
}
```

Notes:
- `UserClass/`ManyDeviceUser`` is a **concept** label (meta concept type + `/` + backticked concept instance id).
- In current code, “Define” focuses on defining a derived predicate; if you want to persist edges/nodes, add an `Action` with `createEdgeInstance`/`createNodeInstance`.

### 4) Derived relation (materialize an inferred edge)

```
Define (i:Incident)-[rc:rootCause]->(c:Component) {
  GraphStructure {
    (i:Incident)-[t:triggeredBy]->(a:Alarm)-[on]->(c:Component)
  }
  Rule {
  }
  Action {
    createEdgeInstance(src=i, dst=c, type=rootCause, value={})
  }
}
```

## Language basics

### Identifiers and escaping

- Identifiers are “symbolic names”: `User`, `lk`, `CustFundKG.Account`.
- Use backticks to escape anything that is not a plain identifier: `` `Risk User` ``, `` `规则1` ``.
- Many KGDSL constructs accept escaped names because the lexer treats backticked names as identifiers.

### Variables (aliases)

KGDSL is variable-based. You refer to nodes/edges via aliases:

- Node alias: `u`, `s`, `A`
- Edge alias: `e1`, `out`, `p1`

Property access is `alias.property`, for both nodes and edges:

- `u.id`, `u.age`
- `e.weight`, `out1.sumAmt`

### Types and labels

KGDSL uses labels in patterns:

- **Entity type**: `Person`, `Company`, `CustFundKG.Account`
- **Concept instance**: `TaxonomyOfRiskUser/`Fraudster`` (meta concept type is `TaxonomyOfRiskUser`)
- **Multiple labels** are allowed in some contexts (see GraphStructure line syntax).

## GraphStructure (pattern declaration)

You can declare patterns using two syntaxes. They compile to the same internal representation.

### Syntax A: “Line” syntax (vertex/edge declarations)

This is a compact way to declare aliases, types, and edges.

#### Vertex declarations

```
GraphStructure {
  A [User]
  C,D [FilmDirector]
  Start [User, __start__='true']
}
```

- `A` is a node alias.
- `[User]` is the node label/type.
- You can declare multiple aliases with the same type: `C,D [FilmDirector]`.
- You can attach pattern properties after labels. A common built-in key is `__start__='true'` to mark the start vertex.

If you don’t want to mark `__start__` in DSL, you can also drive the start vertex from the runtime (see `ConfigKey.KG_REASONER_START_ID_LIST` in `docs/reasoner/runtime.md`).

#### Edge declarations

```
GraphStructure {
  A [FilmPerson]
  C,D [FilmDirector]

  A->C [test] as e1
  C->D [t1] repeat(2,5) as e2
}
```

- `A->C [test] as e1` creates an edge pattern from node alias `A` to `C`.
- `repeat(m,n)` is a bounded variable-length traversal (useful for “transitive-like” expansions).
- Edge aliases (`as e1`) let you reference edge properties in `Rule` and `get(...)`.

Line syntax supports `->` and `<->`. If you need explicit `<-` direction, use the pattern syntax below.

You can also attach edge “pattern properties” inside `[...]`:

```
GraphStructure {
  u,v [User]
  u->v [lk, weight=1] as e
}
```

Note: GraphStructure pattern properties are lightweight (numbers, strings, identifiers, `$params`). For computed expressions, put them in `Rule`.

### Syntax B: “Pattern” syntax (GQL-style patterns inside GraphStructure)

This is closer to ISO GQL/Cypher pattern notation:

```
GraphStructure {
  (A:Film)-[E1:directFilm]-(B:FilmDirector)
  (A:Film)-[E2:writerOfFilm]-(C:FilmWriter)
  (B:FilmDirector)-[E3:workmates]-(C:FilmWriter)
}
Rule {
  R1("80s director"): B.birthDate > '1980'
  R2("same gender"): B.gender == C.gender
}
Action {
  get(B.name, C.name)
}
```

Supported features include:

- Direction: `->`, `<-`, and `-` (any direction).
- Quantifiers: `?` (optional), `{m,n}` (bounded length).
- Multi-label edges: `[:developer|boss]` (label alternatives).
- Per-node limit (edge expansion cap): `-[e:rel per_node_limit 10]->(...)`.
- OPTIONAL paths: `OPTIONAL (a)-[...]-(b)` (optional match).
- Pattern `WHERE` clause in GQL mode (`MATCH ... WHERE ... RETURN ...`) and also in some pattern expressions.

## GQL subset mode (MATCH/RETURN)

KGDSL grammar includes a subset of ISO GQL:

```
MATCH
  (a:App)-[:developer|boss]->(u:Person)
WHERE
  a.id = 'app-1'
RETURN
  u.id as uid, u.name as uname
```

Use this form when you want “query-only” behavior and don’t need the `Rule { ... }` computed-variable style.

## Rule / Constraint (logic + derived variables)

The `Rule { ... }` (or `Constraint { ... }`) block is where you:

- Define boolean rules (named constraints).
- Compute intermediate variables (aggregations, scoring, classification labels).
- Assign computed values to a target variable (often `o` in SPG-style definitions).

### 1) Named boolean rules

```
Rule {
  R1("has house"): s.haveHouse == 'Y'
  R2("has car"): s.haveCar == 'Y'
  R3("tall"): s.height > 180
  R4("good"): R1 && R2 && R3
}
```

Notes:
- `R4` can reference earlier rules by name (`R1`, `R2`, ...).
- `and/or/not` work in both keyword form and symbol form: `and` / `&&`, `or` / `||`, `not(x)` or `!x`.

### 2) Derived variables (assignment)

Assignments use `=`:

```
Rule {
  score = s.a + s.b * 2
  label = rule_value(score > 10, 'high', 'low')
}
```

You can also assign to an `alias.property` target (used when defining derived relations/properties):

```
Rule {
  real_rate("actual shares") = group(s,o).sum(p1.shares * p2.shares)
  p.shares = real_rate
}
```

### 3) Aggregation (`group(...)`)

KGDSL supports group-by + aggregation directly in Rule:

```
Rule {
  amt = group(s, o).sum(p.amt)
  totalAmt = group(s).sum(amt)
}
```

Supported aggregations: `sum`, `avg`, `count`, `min`, `max`.

Conditional aggregation has two forms; the chain form is the most readable:

```
Rule {
  dailyAmt = group(user).if(s.statPriod == '日').sum(s.amount)
  monthAmt = group(user).if(s.statPriod == '月').sum(s.amount)
}
```

### 4) Expressions and operators

KGDSL expressions are a mix of boolean logic, comparisons, arithmetic, lists, and function calls:

- Comparisons: `==`, `!=`/`<>`, `>`, `>=`, `<`, `<=`
- Set membership: `x in [1,2,3]`, `x in ['a','b']`
- Pattern matching: `like`, `rlike`
- Booleans: `and`/`&&`, `or`/`||`, `not`/`!`, `xor`
- Lists: `[expr1, expr2, ...]`
- Parameters: `$paramName` (useful for templating)

Common built-in functions you’ll see in code/tests:

- `rule_value(cond, a, b)`: ternary-like conditional.
- `date_diff(a, b)`: date difference (used in examples).
- `abs(x)`, `floor(x)`, `ceil(x)` / `ceiling(x)`.
- `exist(x)`: existence check.

Function calls are generic: any `name(arg1, arg2, ...)` parses as a function expression.

### 5) List operations (advanced)

KGDSL has chainable operators for list-like values (useful when functions return lists):

- Filtering: `.if(cond)`
- Aggregation: `.sum(...)`, `.count(...)`, `.min(...)`, `.max(...)`, `.avg(...)`
- Ordering/limit: `.desc(x).limit(n)` / `.asc(x).limit(n)`
- Slicing: `.slice(from,to)`
- Indexing: `.get(i)`
- Utility: `.head(n)`, `.tail(n)`, `.nodes()`, `.edges()`
- Functional: `.reduce((a,b)=>expr, init)` and `.constraint((a,b)=>expr)`

These are defined in the grammar; whether a given function returns a list depends on your runtime and UDFs.

## Action (output or mutation)

### Query output: `get(...)` / `distinctGet(...)`

`Action { get(...) }` returns a row set (tabular output). You can:

- Select properties: `get(u.id, u.name)`
- Alias columns: `get(u.id as uid, u.name as uname)`
- Attach a description: `get(u.id as uid COMMENT 'user id')`
- Use a constant with alias: `get('ok' as status)`

There is also a SQL post-processing hook:

```
Action {
  get(u.id as uid, u.name as uname)
  .sql(>>>
    select uid, uname from view
  <<<)
}
```

### Graph mutation: `createEdgeInstance(...)` / `createNodeInstance(...)`

In Define-style materialization, you can create derived graph elements.

Create an edge:

```
Action {
  createEdgeInstance(
    src=s,
    dst=o,
    type=belongTo,
    value={ reason = 'rule-based' }
  )
}
```

Create a node (optional assignment captures the created node):

```
Action {
  n = createNodeInstance(
    type=IndustryChainEvent,
    value={
      subject = I.name
      index = 'price'
      trend = 'increase'
    }
  )
}
```

Notes:
- The `value={...}` object uses `key = expression` entries.
- Inside `value`, you can reference computed variables from `Rule`.

## Multi-statement input

The parser supports multiple statements in one input string (internally `parseMultipleStatement` returns a list of blocks). This is useful when you want to define multiple predicates or mix definitions with queries.

## Schema binding (what must exist for your KGDSL to work)

KGDSL is schema-bound: labels and properties must exist in the `Catalog`.

For the in-memory `PropertyGraphCatalog` used in tests/examples, edge types are conventionally named:

```
<SrcType>_<EdgeLabel>_<DstType>
```

Example: for `(User)-[:lk]->(User)`, the edge type key is `User_lk_User`.

If you see validation errors like “unknown type” or “unknown property”, the fix is usually one of:

- Add the node type and its properties to the schema.
- Add the edge type (with correct `Src_label_Dst` convention) to the schema.
- Ensure you’re referencing properties that actually exist.

## Execution mental model (why KGDSL looks like this)

KGDSL is designed to compile into an operator tree:

- Pattern scan / expand (graph pattern matching)
- Filters (rule predicates)
- GroupBy + aggregations
- Projection (get) or DDL/mutation (create node/edge)

That operator tree is planned and then executed over RDG. See `docs/architecture.md` for diagrams and code entry points.

## Source of truth

This guide documents the “developer-facing” subset. The full syntax is defined by:

- Grammar: `reasoner/kgdsl-parser/src/main/antlr4/com/antgroup/openspg/reasoner/KGDSL.g4`
- Parser: `reasoner/kgdsl-parser/src/main/scala/com/antgroup/openspg/reasoner/parser/OpenSPGDslParser.scala`

When in doubt, check the grammar and the planner tests under `reasoner/lube-logical/src/test`.
