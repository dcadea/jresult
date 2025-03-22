# jResult

Inspired by :crab: [Rust](https://github.com/rust-lang/rust)

[![Maven Central](https://img.shields.io/maven-central/v/io.github.dcadea/jresult.svg?label=Maven%20Central)](https://search.maven.org/artifact/io.github.dcadea/jresult)
[![Build Status](https://github.com/dcadea/jresult/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/dcadea/jresult/actions/workflows/build.yml)

## Result pattern

This library implements a design pattern called `Result`. 
It provides a convenient representation of an operation outcome.
By wrapping return types into `Result` developers can gracefully react to both success - `Ok` and failure - `Err`.
Errors as values force programmers to deal with undesirable behavior at the earliest stage which results in a more reliable, resilient and robust system.

## Dependency

Maven:
```xml
<dependency>
  <groupId>io.github.dcadea</groupId>
  <artifactId>jresult</artifactId>
  <version>${version}</version>
</dependency>
```

Gradle:
```groovy
dependencies {
    implementation 'io.github.dcadea:jresult:${version}'
}
```

## Usage

```java
Result<String, Exception> readFile(String path) {
    try {
        return Result.ok(Files.readString(Paths.get(path)));
    } catch (IOException e) {
        return Result.err(e);
    }
}

<T> Result<T, Exception> parseJson(String json, Class<T> clazz) {
    try {
        return Result.ok(objectMapper.readValue(json, clazz));
    } catch (IOException e) {
        return Result.err(e);
    }
}

void main() {
    var result = readFile("some/path/awesome.json")
            .andThen(content -> parseJson(content, Model.class));

    switch (result) {
        case Ok(Model model) -> /* handle ok value */;
        case Err(Exception e) -> /* react to error */;
    }
}
```
Not only exceptions could be errors
```java
sealed interface ApiError permits NotFoundError, ValidationError, BadRequest {}
record NotFoundError(String message) implements ApiError {}
record ValidationError(String field, String message) implements ApiError {}
record BadRequest(String message) implements ApiError {}

// ProductService
Result<CartItem, ApiError> addToCart(UUID id, int qty) {
    if (qty < 0) 
        return err(new ValidationError("qty", "quantity could not be negative"));
    
    Product product = repository.findById(id);
    if (product == null)
        return err(new NotFoundError("product with id: %s not found".formatted(id)));
    
    if (qty > product.stock()) 
        return err(new BadRequest("not enough products in stock"));

    repository.reserve(product, qty);
    return ok(new CartItem(product, qty));
}

// Resource/Controller
@POST
@Produces(MediaType.APPLICATION_JSON)
Response<CartItem> addToCart(@FormParam UUID productId, @FormParam int qty) {
    var cartItem = productService.addToCart(productId, qty);
    
    return switch (cartItem) {
        case Ok(CartItem ci) -> Response.ok(ci);
        case Err(NotFoundError e) -> 
                Response.status(Status.NOT_FOUND).entity(e.message()).build();
        case Err(ValidationError e) ->
                Response.status(Status.BAD_REQUEST).entity(e).build();
        case Err(BadRequest e) ->
                Response.status(Status.BAD_REQUEST).entity(e).build();
    };
}
```

### Creating a Result

To create a `Result`, you can use the static methods `ok` and `err`:
```java
import io.github.dcadea.jresult.Result;

var ok = Result.ok("success");
var err = Result.err("failure");
```

### Checking the Result

```java
var result = Result.ok("success");

if (result.isOk())    { /* is an Ok variant holding a value */ }
if (result.isErr())   { /* is an Err variant holding an error value */ }
if (result.isEmpty()) { /* is an Ok variant with null as value */ }

// Additionally you can test if value of any rasult variant satisfies given predicate
if (result.isOkAnd(v -> v.startsWith("su")))     { /* good to go */ }
if (result.isErrAnd(e -> e.reason() == TIMEOUT)) { /* ... */ }
```


### Accessing values

A straightforward way of accessing value of a Result is by calling respective `unwrap`method.
Note that these methods throw `IllegalStateException` when Result is not in desired state.
You can customize exception message by calling alternative method - `expect`.
```java
var okValue = result.unwrap();     // throws exception when result is Err
var errValue = result.unwrapErr(); // throws exception when result is Ok

var okValue = result.expect("this should never happen");
var errValue = result.expectErr("why this is ok?");
```
Have a fallback?
```java
var result = Result.err("bad!");

var eager = result.unwrapOr(5);
var lazy = result.unwrapOrElse(() -> 5);
```

### Optional representation

If you don't care about errors of a result, there is a way to convert it to an Optional by calling `ok`.<br>
Same thing could be achieved when there is no point in success, just call `err`.
```java
Result<Integer, String> result = Result.ok(5);
Optional<Integer> opt = result.ok(); // Optional.of(5)
Optional<String> opt = result.err(); // Optional.empty()

Result<Integer, String> result = Result.err("crash");
Optional<Integer> opt = result.ok(); // Optional.empty()
Optional<String> opt = result.err(); // Optional.of("crash")
```

### Pattern matching

Works with Java 21+ only.
```java
if (result instanceof Ok(String value)) { /* use deconstructed value here */ }
if (result instanceof Err(Kaboom boom)) { /* use deconstructed error value here */ }

// or walk through all variants using switch
var value = switch (result) {
    case Ok(String v) -> v;
    case Err(Kaboom boom) -> {
        logger.log(boom);
        yield "default";
    };
```

### Mapping values

Use `map` and `mapErr` to apply conversion function.
```java
var ok = Result.ok(5); 
ok.map(v -> v * 2);                     // Ok(10)
ok.map(v -> "result: %d".formatted(v)); // Ok("result: 5")
ok.mapErr(e -> e + "!!!");              // Ok(5)

var err = Result.err("error");
err.map(v -> v * 2);                    // Err("error")
err.mapErr(Kaboom::new);                // Err(Kaboom("error"))
```
Have a fallback? Use `mapOr`.
```java
var res = Result.ok(5); 
res.mapOr(v -> v * 2, 0); // Ok(10)

Result<Integer, String> res = Result.err("error"); 
res.mapOr(v -> v * 2, 0); // Ok(0)
```
Fallback with lazy evaluation is achievable with `mapOrElse`.
```java
var res = Result.ok(5); 
res.mapOrElse(v -> v * 2, () -> 0); // Ok(10)

Result<Integer, String> res = Result.err("error"); 
res.mapOrElse(v -> v * 2, () -> 0); // Ok(0)
```

### Logical operations

Having two or more results you can apply logical short-circuit `and` & `or` operations.
```java
ok(5).and(err("error"));          // Err("error")
err("error").and(ok(5));          // Err("error")
err("error1").and(err("error2")); // Err("error1")
ok(5).and(ok(10));                // Ok(10)

ok(5).or(err("error"));           // Ok(5)
err("error").or(ok(5));           // Ok(5)
err("error1").or(err("error2"));  // Err("error2")
ok(5).or(ok(10));                 // Ok(5)
``` 

### Flow control

You can use lazy methods `andThen` & `orElse` to chain results.
```java
ok(5).andThen(v -> err("error"));          // Err("error")
err("error").andThen(v -> ok(v + 10));     // Err("error")
err("error1").andThen(v -> err("error2")); // Err("error1")
ok(5).andThen(v -> ok(v + 10));            // Ok(15)

ok(5).orElse(e -> err("error"));           // Ok(5)
err("error").orElse(e -> ok(10));          // Ok(10)
err("error1").orElse(e -> err("error2"));  // Err("error2")
ok(5).orElse(e -> ok(10));                 // Ok(10)
```
> Note that `andThen` passes value of `Ok` variant but `orElse` passes value of `Err` variant downstream.

### Inspection
Take a peek at ok or error values by calling respective `inspect` method.
```java
var res = Result.ok(5); 
res.inspect(System.out::println);    // prints 5
res.inspectErr(System.out::println); // does nothing

Result<Integer, String> res = Result.err("error"); 
res.inspect(System.out::println);    // does nothing
res.inspectErr(System.out::println); // prints "error"
```

## License
This project is licensed under [Apache 2.0](LICENSE-APACHE) and [MIT](LICENSE-APACHE) Licenses - see the corresponding file for details.
