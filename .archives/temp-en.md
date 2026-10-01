## Basic Syntax

```
paramName operator [value]  [AND|OR  paramName operator [value]  ...]
```

- **paramName**: The request parameter name sent by the client (query string, form body, header, or request attribute)
- **operator**: Case-insensitive; supports aliases (see the operator table below)
- **value**: An optional matching value. String values must be wrapped in single quotes (e.g. `'John'`), numeric values are written directly (e.g. `100`), and dates use ISO format (e.g. `'2026-08-01'`)
- **Logical connectors**: AND and OR (case-insensitive)
- **Parentheses**: `(` `)` used to control precedence

---

## Supported Operators

### String Matching

| Operator | Description | Example | Condition is met when |
|----------|-------------|---------|----------------------|
| `=` `==` `equals` | Exact match | `status = active` | `status` parameter value equals `"active"` |
| `contains` `has` | Contains substring | `username contains 'John'` | `username` parameter value contains substring `"John"` |
| `regex` `matches` `~=` | Regex match | `phone regex '\d{11}'` | `phone` parameter value matches regex `\d{11}` |
| `exists` `present` | Parameter exists | `token exists` | `token` parameter exists (any value) |

### Numeric Comparison

Values are parsed as `BigDecimal` for comparison.

| Operator | Description | Example |
|----------|-------------|---------|
| `>` `gt` | Greater than | `amount > 100` |
| `<` `lt` | Less than | `age < 18` |
| `>=` `gte` | Greater than or equal | `score >= 60` |
| `<=` `lte` | Less than or equal | `price <= 500` |

### Date Comparison

Values are parsed as `Instant` for comparison.
Supported date formats: `yyyy-MM-dd HH:mm:ss`, `yyyy-MM-dd`, ISO-8601.

| Operator | Description | Example |
|----------|-------------|---------|
| `<` `before` | Before | `startDate before '2026-08-01'` |
| `>` `after` | After | `endDate after '2026-07-01'` |

> **Note**: The `<` and `>` operators are shared between numeric and date comparison. When both the parameter value and condition value are in a date format, date comparison is used automatically; otherwise, numeric comparison is applied.
