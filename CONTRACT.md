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

**Will the untouched consumer still compile and pass?** No. Removing both
positional overloads leaves only `createBooking(BookingRequest)`. The
`consumer` module will fail during main-source compilation, before its
tests run. This prediction was drafted with agent assistance before
the Milestone 2 build; step 2 was already known from the earlier lab review.

**Where.** `consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:27`
and `:33` still pass four arguments. Neither matches the new one-argument
signature. The consumer tests also contain positional calls, but Maven
will stop before compiling those tests.

**What about the tests in `api/`, after you update them?** They should
compile and pass when rewritten to construct `BookingRequest` objects.
Keep the five original behavior tests and the two notes tests. Their
success verifies the new calls and behavior, but does not prove that
existing consumer source code is compatible.

### Step 1: after the fold

**What the build printed.** `mvn -B clean test` exited with code 1.
Full output is saved in `milestone2-step1.log`; key lines follow:

```text
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.028 s -- in edu.cmu.cs214.booking.InMemoryBookingServiceTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[ERROR] COMPILATION ERROR :
[ERROR] /Users/ray/MyFIle/College/CMU/Courses/17-214/labs/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR] /Users/ray/MyFIle/College/CMU/Courses/17-214/labs/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[INFO] lab06-booking-parent ............................... SUCCESS [  0.064 s]
[INFO] lab06-api .......................................... SUCCESS [  0.831 s]
[INFO] lab06-consumer ..................................... FAILURE [  0.040 s]
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project lab06-consumer: Compilation failure: Compilation failure:
[ERROR] /Users/ray/MyFIle/College/CMU/Courses/17-214/labs/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,<nulltype>
[ERROR]   reason: actual and formal argument lists differ in length
[ERROR] /Users/ray/MyFIle/College/CMU/Courses/17-214/labs/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,java.lang.String
[ERROR]   reason: actual and formal argument lists differ in length
[ERROR] -> [Help 1]
[ERROR]
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR]
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
[ERROR]
[ERROR] After correcting the problems, you can resume the build with the command
[ERROR]   mvn <args> -rf :lab06-consumer
```

**Which module's tests ran, and which did not.** The api module passed all
8 tests: the original five, two migrated notes tests, and request validation.
The consumer failed compiling `FrontDesk.java` at lines 27 and 33, where four
arguments were supplied but only `BookingRequest` was accepted. Its tests were
neither compiled nor run. The producer's updated tests could not detect this
source compatibility break; compiling the independent consumer detected it.

### Step 2: the deprecation path

**What you added.** Both legacy signatures returned as `@Deprecated` default
methods on `BookingApi`, with `@deprecated` Javadoc naming the replacement:

```java
Booking createBooking(String roomId, long startMinute, long endMinute,
                      String waitlistKey);
Booking createBooking(String roomId, long startMinute, long endMinute,
                      String waitlistKey, String notes);
```

Each constructs a `BookingRequest` and delegates directly to
`createBooking(BookingRequest)`. The four-argument form supplies null notes;
the five-argument form retains the supplied notes. `InMemoryBookingService`
implements only the new method, so both paths share validation and behavior.

**The warnings and result.** A fresh `mvn -B clean test` exited with code 0.
Full output is saved in `milestone2-step2.log`; key lines follow:

```text
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.029 s -- in edu.cmu.cs214.booking.InMemoryBookingServiceTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[WARNING] /Users/ray/MyFIle/College/CMU/Courses/17-214/labs/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
[WARNING] /Users/ray/MyFIle/College/CMU/Courses/17-214/labs/f26-lab06/consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.036 s -- in edu.cmu.cs214.frontdesk.FrontDeskTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] lab06-booking-parent ............................... SUCCESS [  0.061 s]
[INFO] lab06-api .......................................... SUCCESS [  0.846 s]
[INFO] lab06-consumer ..................................... SUCCESS [  0.333 s]
[INFO] BUILD SUCCESS
```

All 9 api tests and all 7 consumer tests passed. The additional api test checks
both deprecated overloads, including conflict handling, validation, notes, and
waitlist promotion. Only that intentional compatibility test suppresses
deprecation warnings; consumer warnings remain visible.

**What the deprecation path resolves.** The untouched front desk consumer can
build again, while new callers use the request object immediately. The API
team can release the new surface now; the consumer team can migrate later on
its own schedule. Deprecation does not force migration or remove old methods;
removal still needs a separately coordinated breaking release.

**What the warnings accomplish that a README note would not.** Javac reports
the obsolete signature with a file, line, and column in the consumer's normal
build log, including `FrontDesk.java:27` and `:33`. The caller sees actionable
migration prompts when compiling, without seeking out the producer's README.
The Javadoc supplies the replacement method; the warnings do not themselves
change the caller's code or stop this build.

*Prediction and explanations drafted with agent assistance; review them and
explain the reasoning yourself when presenting to the TA.*

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
