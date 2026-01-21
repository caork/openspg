# KGDSL language guide

KGDSL (Knowledge Graph Domain Specific Language) is the reasoning DSL used by OpenSPG. It represents graph patterns, rules, and actions in a programmable form.

## Core sections

- `GraphStructure`: pattern declarations over nodes and edges.
- `Rule`: logical expressions used for filtering or derivation.
- `Action`: final projection or mutation (e.g., `get(...)`, `createEdgeInstance(...)`).

## Examples

### RCA inference (creates a rootCause edge)

```
Define (i:Incident)-[rc:rootCause]->(c:Component) {
  GraphStructure {
    (i:Incident)-[t:triggeredBy]->(a:Alarm)-[on]->(c:Component)
  }
  Rule {
  }
  Action {
    createEdgeInstance(
      src=i,
      dst=c,
      type=rootCause,
      value={}
    )
  }
}
```

The same example lives in `reasoner-examples/src/main/resources/kgdsl/rca_root_cause.kgdsl`.

### Basic projection

```
GraphStructure {
  (Student:Student)-[STEdge:STEdge]->(Teacher:Teacher)
}
Rule {
}
Action {
  get(Student.name, Teacher.name, STEdge.name)
}
```

The same example lives in `reasoner-examples/src/main/resources/kgdsl/student_teacher.kgdsl`.

## How KGDSL is represented internally

- Parser: `OpenSPGDslParser` converts KGDSL to `Block` IR.
- Blocks are unresolved logical plans (e.g., `MatchBlock`, `ProjectBlock`, `TableResultBlock`).
- KGDSL may contain multiple statements; `parseMultipleStatement` returns a list of blocks.
- Blocks are validated and then planned into `LogicalOperator` trees.

## Where to look in code

- Grammar: `reasoner/kgdsl-parser/src/main/antlr4/com/antgroup/openspg/reasoner/KGDSL.g4`.
- Parser implementation: `reasoner/kgdsl-parser/src/main/scala/com/antgroup/openspg/reasoner/parser/OpenSPGDslParser.scala`.

## Notes

- KGDSL is schema-bound; the `Catalog` resolves types and fields during validation.
- Use the grammar file as the source of truth for full syntax coverage.

