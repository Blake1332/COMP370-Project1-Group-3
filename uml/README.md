# UML diagrams for COMP370 Mini Project 1 (SRMS)

All diagrams are provided both as Mermaid source (.mmd) and exported PNG artifacts in /exports.

This set is aligned to the Step 2 rubric in the project specification:
- Use case diagram
- Class diagram
- Sequence: Startup + Leader Election
- Sequence: Client Request (normal)
- Sequence: Client Request (during failover)

## Files
- `use-case-diagram.mmd`
- `class-diagram-required.mmd`
- `sequence-startup-and-election.mmd`
- `sequence-client-normal.mmd`
- `sequence-client-failover.mmd`

## Optional supporting files
- `class-diagram.mmd` (previous detailed class diagram)
- `state-diagram-raft-node.mmd` (extra, not required but useful)

## Rendering workflow used
Diagrams were rendered locally using VS Code with Mermaid preview support.

Process:
1. Open `.mmd` file
2. Paste into preview.md Mermaid block
3. Render in VS Code preview
4. Export via screenshot to PNG

## Important note for report accuracy
The implementation uses a Raft-style election and no separate monitor process. This is stated explicitly in the report/slides as a design choice relative to the "primary-backup + monitor" example architecture in the project brief.
