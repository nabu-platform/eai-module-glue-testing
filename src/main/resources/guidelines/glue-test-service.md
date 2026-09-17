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

### Test log descriptions

Use `##` descriptions before meaningful test steps. Descriptions are included in the generated test log, making setup, execution, validation, and cleanup readable without inspecting the source.

Descriptions may contain `${expression}` placeholders. They are evaluated against the current Glue context when the step runs, so include relevant identifiers and values:

```glue
## Create battery type: ${batteryTypeName}
batteryType = bebatOne.crud.batteryType.services.create(
	instance: structure(name: batteryTypeName))/instance

## Validate generated battery type id: ${batteryType/id}
confirmNotNull("Created battery type has an id", batteryType/id)

## Remove price: ${price/id}
bebatOne.crud.batteryTypePrice.services.delete(id: price/id)
```

Prefer descriptions that state the business action or expectation. Add them to significant steps rather than every assignment. Keep validation messages independently meaningful because descriptions provide narrative context while validations provide pass/fail details.

### Safe service updates

As described in the general Glue service guidance, Nabu services almost never treat omitted fields as "leave unchanged." This applies broadly to update-like services, including CRUD `update`: constructing an input with only changed fields can clear or overwrite omitted values.

Fetch the current instance first, change only the intended values on that complete instance, then pass it to `update`:

```glue
current = bebatOne.crud.batteryType.services.get(id: batteryTypeId)/instance
confirmNotNull("Battery type exists before update", current)

current/name = updatedName
updated = bebatOne.crud.batteryType.services.update(instance: current)/instance
```

When direct mutation is undesirable, derive a complete updated structure from the fetched instance:

```glue
current = bebatOne.crud.batteryType.services.get(id: batteryTypeId)/instance
confirmNotNull("Battery type exists before update", current)
updatedInput = structure(current, name: updatedName)
updated = bebatOne.crud.batteryType.services.update(instance: updatedInput)/instance
```

General service update rules:

- Fetch by id immediately before updating unless the test already holds the complete current instance.
- Confirm the fetched instance exists before writing.
- Preserve all untouched fields from that instance.
- Apply only the intended field changes, then send the complete instance to `update`.
- Use named parameters and select the actual output field from the service result.

### Safe pre-test cleanup

Glue tests normally clean matching existing data at the start and leave the final test data available for inspection. Cleanup should rediscover old test data, but a destructive dependent query must not run when its prerequisite lookup is empty.

A null or empty value in a CRUD filter may cause that filter field to be omitted. The query can then become unfiltered and return every record. Resolve prerequisite ids first, guard the dependent query with a non-empty check, and retain the bulk `in`-style query rather than issuing one query per parent.

```glue
batteryTypeIds = resolve(bebatOne.crud.batteryType.services.list(
	filter: structure(name: batteryTypeName))/results/id)

if (size(batteryTypeIds) > 0)
	prices = resolve(bebatOne.crud.batteryTypePrice.services.list(
		filter: structure(batteryTypeId: batteryTypeIds))/results)
	for (price : prices)
		## Remove price: ${price/id}
		bebatOne.crud.batteryTypePrice.services.delete(id: price/id)
```

General rules for destructive cleanup:

- Resolve lookup-derived filter values before constructing a dependent query.
- Guard the dependent query with `size(values) > 0`; when no ids match, skip it entirely.
- Preserve bulk filtering by passing the resolved id series once. Do not issue one dependent query per id unless required by the service contract.
- Materialize finite result sets with `resolve()` before deleting from the same dataset.
- Use named parameters for every Nabu service call.

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
