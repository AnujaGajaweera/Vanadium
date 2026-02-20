# Conflict Avoidance

Vanadium conflict policy:

- no OpenGL shader hooks
- no Iris format parsing
- no Sodium injection hooks
- no mixins targeting renderer replacement
- no writes to third-party config files

This keeps coexistence safe with Iris, Sodium, Lithium, and Phosphor.
