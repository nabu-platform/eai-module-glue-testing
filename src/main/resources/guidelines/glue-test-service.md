# Artifact: glueTestService

A Glue test service has all `glueService` fragments plus `stub.xml`.

## Test script

Write tests in `script.glue`. Test checks return a boolean and add a validation to the test report.

### Non-fatal validations

`validate...` records success or failure and continues execution:

- `validateEquals(message, expected, actual)`
- `validateNotEquals(message, expected, actual)`
- `validateNull(message, actual)`
- `validateNotNull(message, actual)`
- `validateTrue(message, actual)`
- `validateFalse(message, actual)`
- `validateMatches(message, regex, actual)`
- `validateNotMatches(message, regex, actual)`
- `validateContains(message, regex, actual)`
- `validateNotContains(message, regex, actual)`
- `validateFails(message, expressions...)`: succeeds when one of the supplied expressions throws
- `validateNotFails(message, expressions...)`: succeeds when none of the supplied expressions throws

`Matches` applies a Java regular expression to the complete converted value. `Contains` wraps the supplied expression in a case-insensitive, dot-all contains match, so its second argument is still a regex rather than a literal substring. Equality converts `actual` to the runtime type of `expected` where possible, compares arrays as lists, ignores carriage returns in strings, and uses `Comparable.compareTo` where applicable.

### Fatal confirmations

Every regular validation has a fatal `confirm...` counterpart:

- `confirmEquals`, `confirmNotEquals`
- `confirmNull`, `confirmNotNull`
- `confirmTrue`, `confirmFalse`
- `confirmMatches`, `confirmNotMatches`
- `confirmContains`, `confirmNotContains`
- `confirmFails`, `confirmNotFails`

A failed confirmation records the error and immediately throws an assertion exception, stopping normal test execution. Use `validate...` to collect independent failures and `confirm...` when later checks cannot be meaningful after failure.

### Reporting helpers

- `report()` returns the validations collected in the current runtime, or null when none exist.
- `not(value)` returns true for null or false and false for true; ordinary `!value` is usually clearer.
- `check(message, result, detail, fail)` and `addValidation(severity, message, description, fail)` are low-level APIs. Prefer the named validation and confirmation methods because they produce consistent diagnostics.

Use specific messages that describe expected behavior:

```glue
result = my.module.services.calculate(value: 2)
validateNotNull("Calculation returns a result", result)
confirmNull("Calculation has no error", result/error)
validateEquals("Double the supplied value", 4, result/value)
validateMatches("Reference is numeric", "[0-9]+", result/reference)
validateFails("Invalid input is rejected", my.module.services.calculate(value: -1))
```

### Service stubs

A test can install a script-local service override with `stub(serviceId, lambda, condition)`:

```glue
stub("my.module.services.lookup", lambda(input, structure(name: "Example")), null)
```

`serviceId` is the fully qualified Nabu service id. The lambda supplies the replacement behavior. `condition` is optional and allows conditional matching. Prefer inline stubs when behavior is naturally expressed in Glue; use `stub.xml` for configured request/response fixtures.

## stub.xml

`stub.xml` contains one `runProfile` with optional `description` and service profiles. Each profile references a service and contains one or more configurations:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<runProfile>
	<description>Configured service responses for this test</description>
	<profiles>
		<service>my.module.services.lookup</service>
		<configurations>
			<inputQueries>customerId == "123"</inputQueries>
			<output>{"name":"Example"}</output>
		</configurations>
	</profiles>
</runProfile>
```

- `service` is the fully qualified service artifact id.
- Each `inputQueries` expression is evaluated against the service input; all configured expressions must match.
- A configuration without input queries matches a null input.
- `output` is JSON matching the referenced service's output definition. Empty output returns null.
- Set `errorCode` and optionally `errorMessage` instead of `output` to simulate a service error.
- Configurations are checked in order; the first match supplies the stubbed result.

The XML and referenced artifacts are validated while loading before persistence. Preserve unrelated profiles when editing one fixture.
