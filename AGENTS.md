# Build and validation

After every change, run `./mill --no-server all` from the repository root.
Use the `all` target by default so validation, generated YANG trees, and
the draft HTML are rebuilt together. Report any build failures.

Do not edit files in `generated-tree-output` directly; regenerate them
through Mill by changing the source YANG or build configuration.
