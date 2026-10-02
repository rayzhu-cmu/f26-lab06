# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** Yes.

**Why.** Prediction drafted with agent assistance before running the build.
The existing four-argument signature remains available. The new overload
adds a fifth argument, `String notes`, so it is not applicable to the
four-argument calls in `FrontDesk.java:27` and `FrontDesk.java:33`, even
when the fourth argument is `null`. Both still resolve to the old method.
That method will delegate with null notes, preserving the existing
waitlisting, conflict, and booking behavior. Schedule and cancellation
calls are unaffected.

### What happened

**The result.** The baseline `mvn -B test` passed with 5 api tests and
7 consumer tests. After the change, `mvn -B test` and a fresh
`mvn -B clean test` both passed. The clean build recompiled both modules.

```text
api:
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
consumer:
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] lab06-booking-parent ............................... SUCCESS
[INFO] lab06-api .......................................... SUCCESS
[INFO] lab06-consumer ..................................... SUCCESS
[INFO] BUILD SUCCESS
```

The five original api tests remain; two new tests check notes retention
through waitlisting and promotion, conflict rejection, and null notes
for both old and new overloads. No files under `consumer/` were edited.

**If your prediction was wrong.** It was correct. The consumer keeps
calling the four-argument method, which delegates with null notes.

**Is an additive change always safe in Java?** Merely adding an overload
is not always safe. With only `f(String value)`, a call `f(null)` compiles.
Adding `f(Integer value)` makes that existing call ambiguous: both
reference types accept null and neither is more specific. The caller
then fails at compile time. Our overload adds a parameter instead, so
the existing four-argument calls do not have that ambiguity. Strictly,
an addition that breaks an existing caller is a breaking change under
the handout's definition.

*Prediction and explanations drafted with agent assistance; review
them and explain the reasoning yourself when presenting to the TA.*

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

**Where.** Name the call sites you expect to be affected, if any.

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

**When the price is worth paying.** A condition under which it is.
