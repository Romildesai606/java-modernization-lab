# java-modernization-lab

**Java 8 → Java 21 migration, shown as a diff you can run.**

Most "modernization" work is not a rewrite. It is moving a production estate onto a
supported LTS baseline without changing behaviour, and proving that nothing moved.
This repository is a small, self-contained ledger domain implemented twice — once in
the Java 8 idiom that most enterprise codebases are actually written in, and once in
Java 21 — with tests that assert the two produce identical results.

> **Representative portfolio project.** Written from scratch with synthetic data.
> It contains no employer, client or vendor code, and no confidential material.

---

## Why this repo exists

I spend a lot of my time on version migrations: Java 8/11 estates onto 17/21, Angular
applications onto current releases. The interesting part is never the syntax. It is
which language features remove a *class* of defect, which ones are cosmetic, and how
you keep the old path alive until the new one has earned its place.

This repo makes that argument concretely on three changes.

### 1. `instanceof` chains → sealed interface + pattern-matching `switch`

`LegacyBalanceCalculator` walks a chain of `instanceof` checks with an explicit cast in
every branch — the shape you find in any long-lived Java 8 service:

```java
if (event instanceof LedgerEvent.Deposit) {
    LedgerEvent.Deposit deposit = (LedgerEvent.Deposit) event;
    balance = balance.plus(deposit.amount());
} else if (event instanceof LedgerEvent.Withdrawal) {
    ...
}
// no else branch — a new event type is silently ignored
```

`BalanceProjector` is the same logic over a `sealed interface`:

```java
return switch (event) {
    case LedgerEvent.Deposit d    -> balance.plus(d.amount());
    case LedgerEvent.Withdrawal w -> balance.minus(w.amount());
    case LedgerEvent.Transfer t   -> balance.minus(t.amount());
    case LedgerEvent.Reversal r   -> balance.plus(r.amount());
};
```

There is deliberately no `default`. Add a fifth event type to `LedgerEvent` and this
file **stops compiling**. The legacy version compiles fine and quietly returns a wrong
balance. On a ledger, that is the difference between a build failure and a
reconciliation incident.

### 2. Hand-written value classes → `record`

`Money` is a `record` that normalises scale in its compact constructor and refuses to
add two different currencies. The Java 8 equivalent was ~90 lines of constructor,
getters, `equals`, `hashCode` and `toString` — every one of them a place for a field to
be forgotten during a refactor.

### 3. Fixed thread pools → virtual threads, *for I/O only*

`StatementFetcher` fans out 500 blocking downstream calls on
`Executors.newVirtualThreadPerTaskExecutor()`. The pool size stops being a tuning
parameter you guess at.

The caveat matters more than the feature. This only helps for **blocking I/O**.
CPU-bound work still wants a bounded pool, and a `synchronized` block around a blocking
call pins the carrier thread and gives back the entire benefit — use `ReentrantLock`
instead. Migrations that skip this distinction get slower, not faster.

---

## Run it

No network access needed, no framework, no database. JDK 21+ only.

```bash
# with Maven (also runs the unit tests)
mvn verify

# or with nothing but the JDK
mkdir -p target/classes
javac -d target/classes $(find src/main/java -name '*.java')
java -cp target/classes dev.romildesai.modernization.modern.Demo
```

Output:

```
=== 1. Sealed interface + pattern-matching switch ===
  deposit of USD 2500.00 to ACCT-1001
  withdrawal of USD 300.00 from ACCT-1001
  transfer of USD 125.50 from ACCT-1001 to ACCT-2002
  reversal of EVT-0003 on ACCT-1001
  modern projector : USD 2200.00
  legacy projector : USD 2200.00
  parity           : true

=== 2. Virtual threads for I/O fan-out ===
  fetched 500 statements in 78 ms
  each call blocks 50 ms; sequentially this would be 25000 ms
  first result: statement:ACCT-0001
```

---

## Layout

```
src/main/java/dev/romildesai/modernization/
├── common/Money.java                  record + compact constructor, half-even rounding
├── legacy/LegacyBalanceCalculator.java  Java 8 instanceof chain (kept on purpose)
└── modern/
    ├── LedgerEvent.java               sealed interface, four record variants
    ├── BalanceProjector.java          exhaustive switch + guarded patterns
    ├── StatementFetcher.java          virtual-thread I/O fan-out
    └── Demo.java                      runnable walkthrough
src/test/java/dev/romildesai/modernization/
├── BalanceProjectorTest.java          parity, rounding, currency safety, guards
└── StatementFetcherTest.java          wall-clock assertion on the fan-out
```

## Tests

`mvn verify` runs seven JUnit 5 tests. The one that matters on a real engagement is
`legacyAndModernAgree` — the parity test that lets you ship the new path behind a flag
and delete the old one only once it has agreed on production traffic.

## Notes and limitations

- `StatementFetcherTest` asserts a generous 5 s upper bound rather than a tight one, so
  it does not go flaky on a loaded CI runner.
- The `Money` type covers the operations this domain needs. It is not a general-purpose
  money library — use Joda-Money or `javax.money` for that.
- There is no persistence here by design. Storage, HTTP and messaging live in the other
  repositories in this portfolio.

## Security

No credentials, no configuration secrets, no network calls. The downstream in
`StatementFetcher` is a `Function<String, String>` supplied by the caller, so the tests
and demo inject a pure function rather than reaching outside the process.

## Licence

MIT — see [LICENSE](LICENSE).
