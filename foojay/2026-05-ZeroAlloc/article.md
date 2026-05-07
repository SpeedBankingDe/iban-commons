# Near-Zero Allocation Java: Lessons from a High-Performance IBAN Library

---

We've all heard it: "The GC is your friend." And for the most part, that is true.
But in high-throughput systems — payment processors, API gateways, fraud-detection pipelines — the garbage collector can silently become your worst enemy. Not because it is broken, but because you are feeding it too much.

This article walks through the techniques behind near-zero allocation Java development, using [iban-commons](https://github.com/SpeedBankingDe/iban-commons) — a pure-Java IBAN/BIC validation library — as a concrete, open-source case study.

---

## Why Allocation Matters

In Java, every `new` you write is cheap — until it is not.

The JVM allocates objects in the Eden space (part of the Young Generation).
When Eden fills up, a **Minor GC** is triggered.
Most objects die young, which is good, but every Minor GC is a stop-the-world event that pauses all application threads.
At high throughput, these pauses accumulate into measurable latency spikes.

The insidious part: individual allocations are invisible.
Nobody writes `new String()` in a tight loop on purpose.
But standard Java idioms do it for you:

```java
// three hidden allocations in one line:
String normalized = rawIban.trim().toUpperCase().replaceAll("\\s+", "");
//                         ^new String  ^new String  ^Pattern + Matcher + new String
```

In a validation library called 100,000 times per second, that line alone creates 300,000 short-lived objects per second — and those objects need to be collected.

---

## The Starting Point: What Other Libraries Do

To understand where iban-commons ends up, it helps to see where most IBAN libraries begin.

A typical approach looks roughly like this:

```java
// Simplified illustration of a naive approach
public static boolean isValid(String iban) {
    String normalized = iban.replaceAll(" ", "").toUpperCase(); // Strings 1 + 2
    if (!normalized.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) { // Pattern + Matcher
        return false;
    }
    String rearranged = normalized.substring(4)     // String 3
                      + normalized.substring(0, 4); // String 4
    // ... Mod97 on the rearranged string ...
}
```

JMH benchmarks (using the `gc.alloc.rate.norm` profiler) reveal exactly how expensive this is in practice:

| Library              | Valid IBAN (ops/s) | Alloc (B/op) | Invalid IBAN (ops/s) | Alloc (B/op) |
|----------------------|------------------:|-------------:|---------------------:|-------------:|
| **iban-commons**     |  **5,591,566**    |  **~0.0**    |     **10,740,280**   |  **~0.0**    |
| Apache Commons       |  3,208,846        |  441.9        |      6,531,841       |  246.8        |
| iban4j               |  2,870,107        | 1,132.9       |      1,801,759       | 1,155.6       |

*Measured on Intel Core i7-1165G7 @ 2.80GHz, OpenJDK 21.0.7, Linux, single core, ParallelGC, `-XX:-StackTraceInThrowable`*

iban-commons allocates effectively **zero bytes** per validation, while competing libraries allocate up to 1,133 bytes per operation.
At 100,000 validations/second, that is the difference between generating almost no garbage and generating over 100 MB of short-lived objects per second.

---

## The Techniques: How to Get There

Here are the concrete techniques that drive iban-commons to near-zero allocation.
None of them require exotic APIs — they are all pure Java 8+.

### 1. `char[]` Instead of `String` Throughout the Pipeline

The most impactful decision: replace `CharSequence`/`String` as the internal processing type with a raw `char[]`.

A `String` in Java is an immutable object. When you call `substring()`, `toUpperCase()`, or `trim()`, you create new `String` objects each time.
A `char[]` is a primitive array — you can index, scan, and compare it in place with no allocation at all.

In iban-commons, the entire validation pipeline — normalization, length checks, BBAN structure validation across all 120 country validators, and the Mod 97-10 checksum — operates exclusively on `char[]`:

```java
// CountryValidator interface — char[] all the way down
@FunctionalInterface
interface CountryValidator {
    boolean validateIban(char[] iban);

    // backwards-compatible default overload at the public boundary
    default boolean validateIban(CharSequence iban) {
        return validateIban(toCharArray(iban));
    }
}
```

And a concrete country validator for Germany:

```java
static final class DE extends AbstractCountryValidator {
    @Override
    public boolean validateIban(final char[] iban) {
        return isAllDigits(iban, 4, 22);
    }
}
```

`isAllDigits()` is a tight loop over the raw `char[]` — no objects, no virtual dispatch, no regex:

```java
public static boolean isAllDigits(final char[] chars, final int beginIndex, final int endIndex) {
    for (int i = beginIndex; i < endIndex; i++) {
        if (chars[i] < '0' || chars[i] > '9') {
            return false;
        }
    }
    return true;
}
```

This also allows the JIT to monomorphize the call site (specialize the code for a single concrete type) and apply auto-vectorization (SIMD—processing multiple data points in a single CPU cycle), squeezing out further performance without any code change.

### 2. A ThreadLocal Buffer for Normalization

The validation entry point receives raw user input: a `String` or `CharSequence` that might contain spaces, or lowercase letters. It needs to be normalized before any further processing. That normalization must happen somewhere — and that somewhere should not be the heap.

iban-commons utilizes a ThreadLocal<char[]> as a persistent, pre-allocated normalization buffer, ensuring each thread operates on its own dedicated instance.

```java
 /**
  * Internal thread-local buffer used to perform IBAN normalization and validation without heap allocations.
  */
private static final ThreadLocal<char[]> VALIDATION_BUFFER =
    ThreadLocal.withInitial(() -> new char[MAX_IBAN_LENGTH]);
```

On each validation call, the raw input is normalized into this buffer in a single pass — stripping spaces, uppercasing — with no intermediate `String` created:

```java
public static boolean isValid(final String iban) {
    char[] normIban = VALIDATION_BUFFER.get(); // no allocation — reuse thread buffer
    int len = normalize(iban, iban.length(), normIban, IbanConfig.isAllowSpace(), IbanConfig.isAllowLowercase());
    if (len < MIN_IBAN_LENGTH) {
        return false;
    }

    IbanRegistry country = IbanRegistry.getBaseEntryByCode(normIban[0], normIban[1]);
    return country != null
        && len == country.getIbanLength()
        && getCountryValidator(country).validateIban(normIban)
        && Mod97.isValid(normIban, len);
}
```

The `ThreadLocal` buffer is allocated once per thread (typically once per application) and reused for every subsequent validation call on that thread. Under high concurrency, each thread pays exactly one allocation ever.
There is also no need to reset (or null) the buffer. It is simply overwritten from index 0 to IBAN length.

### 3. Fail Fast, Fail Cheap

An often-overlooked allocation source: doing expensive work before cheap checks. iban-commons enforces a strict validation order:

1. **Null / empty check** — constant time, no allocation
2. **Minimum length check** — constant time
3. **Country code lookup** — O(1) array index into a pre-built `CountryValidator[]`
4. **Country-specific length check** — constant time
5. **BBAN structure check** — direct `char[]` scan
6. **Mod 97-10 checksum** — most expensive, done last

Invalid IBANs are rejected at step 1 or 2 in the overwhelming majority of cases, which is exactly why the rejection throughput (10.7M ops/s) is almost double the acceptance throughput (5.6M ops/s): most invalid inputs never reach the checksum computation at all.

### 4. `String.getChars()` as a JVM Intrinsic

For `String` input specifically, iban-commons takes one more step. The `normalize()` method has a `String`-specific overload that uses `String.getChars()`:

```java
static int normalize(final String input, final int inputLen,
    final char[] output, final boolean allowSpace, final boolean allowLower) {

    // fast path: validate and copy in one pass
    for (int i = 0; i < inputLen; i++) {
        output[i] = input.charAt(i);
        if (!isDigitOrUpperCase(output[i])) {
            return INVALID_INPUT;
        }
    }
    return inputLen;
}
```

The Java compiler resolves this overload **statically** when the caller passes a `String` literal or variable. The JIT inlines `String.charAt()` to a direct array access, making this path a tight, branchless memory scan — effectively equivalent to a native `memcpy` followed by a scan.

### 5. Lazy Computation and Minimal Eager State

For objects that are instantiated and then interrogated (not just validated), every eagerly computed field that might never be read is wasted allocation. The `Bic` class demonstrates the pattern:

```java
public final class Bic implements Serializable, CharSequence {
    private final String bic8;       // eagerly stored — always needed
    private final String bic11;      // eagerly stored — always needed for equality
    private final String branchCode; // eagerly stored — only for BIC-11

    // lazily derived — most callers never ask for these
    private transient volatile String bankCode;
    private transient volatile String countryCode;
    private transient volatile String locationCode;
}
```

Component strings like `bankCode` and `countryCode` are derived from `bic8` only when a caller actually invokes `getBankCode()` or `getCountryCode()`. Callers who just validate and discard the object pay nothing for the decomposition.

---

## Measuring It: JMH Setup

You cannot optimize what you cannot measure. The benchmark harness used by iban-commons is instructive in itself:

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(value = 2, jvmArgs = {
    "-Xms2G", "-Xmx2G",
    "-XX:+UseParallelGC",
    "-XX:-StackTraceInThrowable"
})
public static class ValidBenchmarks {

    @Benchmark
    @OperationsPerInvocation(TARGET_SIZE) // 1,000,000
    public void bmv1_IbanCommons(ValidState state, Blackhole bh) {
        for (String iban : state.ibans) {
            bh.consume(IbanValidator.isValid(iban));
        }
    }
}
```

A few things worth noting:

**`gc.alloc.rate.norm`** is the profiler flag that reveals bytes allocated per operation. Run with: `java -jar benchmarks.jar IbanBenchmarks -prof gc`. This is the metric that matters most for GC pressure analysis.

**`Blackhole`** prevents the JIT from eliminating calls whose results are not used — dead code elimination is a common JMH pitfall.

**`-XX:-StackTraceInThrowable`** suppresses stack trace generation. Libraries that use exceptions for control flow (like iban4j on the rejection path) benefit massively from this flag on benchmarks — remove it for production-realistic numbers.

**`@OperationsPerInvocation`** tells JMH that each benchmark invocation performs 1,000,000 logical operations, producing per-IBAN throughput numbers rather than per-batch numbers.

The full benchmark source is open at [github.com/SpeedBankingDe/iban-commons-benchmarks](https://github.com/SpeedBankingDe/iban-commons-benchmarks).

---

## Where This Makes Sense — and Where It Does Not

Near-zero allocation is not universally necessary. These techniques add code complexity: `ThreadLocal` buffers require care under frameworks that reuse threads unpredictably (some virtual thread schedulers, for instance), and `char[]`-based code is more verbose than idiomatic `String` code.

The ROI is highest when:

- The method is called **very frequently** (thousands to millions of times per second)
- The method sits on a **hot path** (request validation, event processing, parsing)
- GC pauses are **observable** in your latency SLOs

For batch jobs, admin endpoints, or infrequently called code, standard Java idioms are perfectly fine. Profile first, optimize second.

---

## What's Next: Project Valhalla

The techniques described here are workarounds for a fundamental limitation: in today's JVM, every object has a header, lives on the heap, and is subject to GC. **Project Valhalla** aims to change that with *value types* — objects that behave like primitives: stack-allocated, inlined into arrays, and with no object header overhead. When value types land in a stable JDK release, many of these patterns can be replaced with cleaner, safer code that the JVM optimizes automatically.

---

## Getting Started

iban-commons is available on Maven Central:

```xml
<dependency>
    <groupId>de.speedbanking</groupId>
    <artifactId>iban-commons</artifactId>
    <version>1.8.6</version>
</dependency>
```

Source, benchmarks, and documentation live at [github.com/SpeedBankingDe/iban-commons](https://github.com/SpeedBankingDe/iban-commons).
If the library is useful to you — or the techniques in this article are — a ⭐star⭐ on GitHub helps keep the project visible.

---

*iban-commons is licensed under the Apache License 2.0.*
