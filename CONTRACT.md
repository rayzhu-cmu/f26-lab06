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

Design proposal only; no production code or consumer files changed.

### The misuse

**What is easy to get wrong.** `cancelBooking(long bookingId, boolean
notifyWaitlist)` hides the cancellation policy behind a boolean. A reader
cannot tell from `true` alone whether it sends a message, promotes a booking,
or does something else. Here it promotes at most one eligible waitlisted
booking; it does not send a notification. Accidentally choosing the opposite
boolean still compiles.

**The call site.** In
`consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:48`:

```java
return api.cancelBooking(bookingId, true);
```

The quiet cancellation at line 53 uses the same method with the other value:

```java
return api.cancelBooking(bookingId, false);
```

The enclosing method names help, but the API calls themselves do not express
the policy. Both existing calls are correct; the critique is that a future
caller can invert the flag without a compiler error.

**What goes wrong when it happens.** Suppose a CONFIRMED booking and a
WAITLISTED booking both cover room R1 from minute 540 to minute 600, with no
other conflict. If a caller intends a quiet cancellation but passes `true`,
the first booking becomes CANCELLED and the queued booking becomes CONFIRMED,
holding the room unexpectedly. If a caller intends promotion but passes
`false`, the guest stays WAITLISTED even though the room is now free. Both
calls return true because cancellation succeeded: the wrong policy creates
silent business behavior rather than an exception.

### The redesign

**The proposal.** Replace the flag in the preferred API with an explicit
policy type. Proposed declarations, not implemented in this milestone:

```java
public enum CancellationPolicy {
    CANCEL_ONLY,
    PROMOTE_FIRST_ELIGIBLE
}

boolean cancelBooking(long bookingId, CancellationPolicy policy);
```

The proposed replacements for the two consumer calls are:

```java
// FrontDesk.cancelAndOfferToWaitlist
return api.cancelBooking(bookingId, CancellationPolicy.PROMOTE_FIRST_ELIGIBLE);

// FrontDesk.cancelQuietly
return api.cancelBooking(bookingId, CancellationPolicy.CANCEL_ONLY);
```

`PROMOTE_FIRST_ELIGIBLE` preserves the existing contract: consider overlapping
WAITLISTED bookings on the same room in creation order, and promote at most
the first that no longer conflicts with a remaining CONFIRMED booking.
`CANCEL_ONLY` never promotes anyone. Id handling and the return value stay
the same. The proposed implementation rejects a null policy with
`IllegalArgumentException` before mutating state, and uses an exhaustive
switch expression over the enum to select the behavior.

**Why the mistake is now hard or impossible to make.** The compiler rejects
`true` or `false` for the new signature because boolean is not
`CancellationPolicy`. Named values expose intent at the call site and make
an accidental inversion easier to spot during review. The exhaustive switch
expression requires handling a new enum constant when recompiled. This does
not prove business intent: choosing the wrong valid enum constant can still
compile. The redesign prevents opaque boolean arguments in new calls and
makes the remaining mistake more visible.

### One tradeoff

**What it costs.** Migration burden: every caller must learn the new type and
update its cancellation call. To avoid the source break observed in Milestone
2, retain the old boolean signature as an `@Deprecated` adapter that maps
true to `PROMOTE_FIRST_ELIGIBLE` and false to `CANCEL_ONLY`, then delegates to
the typed method. This adds an overload and a compatibility path to maintain.
The old calls stay susceptible to flag mistakes until their owners migrate;
removing the adapter requires a coordinated breaking release. The creation
adapters already built in Milestone 2 would remain unchanged.

**When the price is worth paying.** When several independent teams call this
API, or an unexpected promotion has a meaningful operational cost, explicit
policies are worth the migration effort. A small internal API with few,
well-understood calls may get less benefit from the additional type and adapter.

*Critique drafted with agent assistance; review it and explain the design and
its limits yourself when presenting to the TA.*
