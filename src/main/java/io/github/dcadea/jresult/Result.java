package io.github.dcadea.jresult;

import java.util.Optional;
import java.util.function.*;

/**
 * A Result type that represents either success - {@link Ok}, or failure - {@link Err}.
 * <pre>{@code
 * public Result<String, AppError> sessionId(Request req) {
 *     Cookie[] cookies = req.getCookies();
 *     if (cookies == null || cookies.length == 0) {
 *         return Result.err(EMPTY_COOKIES);
 *     }
 *
 *     for (Cookie cookie : cookies) {
 *         if ("session_id".equals(cookie.getName())) {
 *             return Result.ok(cookie.getValue());
 *         }
 *     }
 *
 *     return Result.err(MISSING_SESSION);
 * }
 *
 * enum AppError {
 *     MISSING_SESSION,
 *     EMPTY_COOKIES;
 * }
 * }</pre>
 * <p>
 * Ok and Err are exposed to allow <strong>pattern matching and deconstruction</strong>.
 *
 * <pre>{@code
 * Request req = ...;
 * var res = sessionId(req); // Result<String, AppError>
 *
 * switch (res) {
 *   case Ok(String sid) -> validate(sid);
 *   case Err(AppError e) -> logger.error("AppError: {}", e);
 * }
 *
 * // or more granular
 * switch (res) {
 *     case Ok(String sid) -> validate(sid);
 *     case Err(AppError e) when e == MISSING_SESSION -> unauthorized();
 *     case Err(AppError e) when e == EMPTY_COOKIES -> redirect("/login");
 * }
 *
 * // react to success
 * if (res instanceof Ok(String sid)) {
 *    validate(sid);
 * }
 *
 * // react to error
 * if (res instanceof Err(AppError e)) {
 *     logger.error("AppError: {}", e);
 *     switch (e) {
 *         case MISSING_SESSION -> unauthorized();
 *         case EMPTY_COOKIES -> redirect("/login");
 *     }
 * }
 *
 * // error yields a default value
 * double total = 1234.56
 * int installments = 3;
 * var res = splitPayments(total, installments);
 * var first = switch (res) {
 *    case Ok(List<Double> installments) -> installments.getFirst();
 *    case Err(_) -> total;
 * }
 * }</pre>
 *
 * @param <O> Type of success value.
 * @param <E> Type of error value.
 * @author dcadea
 * @since 1.0.0
 */
public sealed interface Result<O, E> permits Ok, Err {

    /**
     * Empty result.
     * <p>Useful for "void" operations to signal success.</p>
     */
    @SuppressWarnings("unchecked")
    static <O, E> Result<O, E> empty() {
        return (Ok<O, E>) Ok.EMPTY;
    }

    /**
     * Create a result with a success value.
     *
     * @param value Success value.
     * @param <O>   Type of success value.
     * @param <E>   Type of error value.
     * @return Ok result.
     */
    static <O, E> Result<O, E> ok(O value) {
        if (value == null) {
            return empty();
        }

        return new Ok<>(value);
    }

    /**
     * Create a result with an error value.
     *
     * @param error Error value.
     * @param <O>   Type of success value.
     * @param <E>   Type of error value.
     * @return Err result.
     * @throws NullPointerException if the error is null.
     */
    static <O, E> Result<O, E> err(E error) {
        return new Err<>(error);
    }

    /**
     * Check if the result is empty.
     * <p>
     * Examples:
     * <pre>{@code
     * var res = Result.empty();
     * assertThat(res.isEmpty()).isTrue();
     *
     * var res = Result.ok(null);
     * assertThat(res.isEmpty()).isTrue();
     *
     * var res = Result.ok(5);
     * assertThat(res.isEmpty()).isFalse();
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.isEmpty()).isFalse();
     * }</pre>
     *
     * @return {@code true} if the result is empty, {@code false} otherwise.
     */
    default boolean isEmpty() {
        return this == Ok.EMPTY;
    }

    /**
     * Check if the result is Ok.
     * <p>
     * Examples:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.isOk()).isTrue();
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.isOk()).isFalse();
     * }</pre>
     *
     * @return {@code true} if the result is Ok, {@code false} otherwise.
     */
    default boolean isOk() {
        return this instanceof Ok;
    }

    /**
     * Check if the result is Ok and the success value satisfies the predicate.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.isOkAnd(v -> v > 0)).isTrue();
     * assertThat(res.isOkAnd(v -> v < 0)).isFalse();
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.isOkAnd(v -> v > 0)).isFalse();
     * }</pre>
     *
     * @param p Predicate to test the success value.
     * @return {@code true} if the result is Ok and the success value satisfies the predicate, {@code false} otherwise.
     */
    default boolean isOkAnd(Predicate<? super O> p) {
        return switch (this) {
            case Ok(O value) -> p.test(value);
            case Err(_) -> false;
        };
    }

    /**
     * Check if the result is Err.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.isErr()).isFalse();
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.isErr()).isTrue();
     * }</pre>
     *
     * @return {@code true} if the result is Err, {@code false} otherwise.
     */
    default boolean isErr() {
        return this instanceof Err;
    }

    /**
     * Check if the result is Err and the error value satisfies the predicate.
     * <p>
     * Example:
     * <pre>{@code
     * Result<Integer, String> res = Result.ok(5);
     * assertThat(res.isErrAnd(v -> v.startsWith("e"))).isFalse();
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.isErrAnd(v -> v.startsWith("e"))).isTrue();
     *
     * Result<Integer, String> res = Result.err("ERROR");
     * assertThat(res.isErrAnd(v -> v.startsWith("e"))).isFalse();
     * }</pre>
     *
     * @param p Predicate to test the error value.
     * @return {@code true} if the result is Err and the error value satisfies the predicate, {@code false} otherwise.
     */
    default boolean isErrAnd(Predicate<? super E> p) {
        return switch (this) {
            case Ok(_) -> false;
            case Err(E error) -> p.test(error);
        };
    }

    /**
     * Convert the result to an optional of success and discard the error.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.ok()).contains(5);
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.ok()).isEmpty();
     * }</pre>
     *
     * @return Optional containing success value.
     */
    default Optional<O> ok() {
        return switch (this) {
            case Ok(O value) -> Optional.ofNullable(value);
            case Err(_) -> Optional.empty();
        };
    }


    /**
     * Convert the result to an optional of error and discard the success.
     * <p>
     * Example:
     * <pre>{@code
     * Result<Integer, String> res = Result.ok(5);
     * assertThat(res.err()).isEmpty();
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.err()).contains("error");
     * }</pre>
     *
     * @return Optional containing error value.
     */
    default Optional<E> err() {
        return switch (this) {
            case Ok(_) -> Optional.empty();
            case Err(E error) -> Optional.of(error);
        };
    }

    /**
     * Map the success value to a new value leaving error unchanged.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * var actual = res.map(v -> "result: %d".formatted(v));
     * assertThat(actual).hasValue("result: 5");
     *
     * Result<Integer, String> res = Result.err("error");
     * var actual = res.map(v -> v * 2);
     * assertThat(actual).hasError("error");
     * }</pre>
     *
     * @param op   Mapping function.
     * @param <NO> New type of success value.
     * @return New result with the mapped success value.
     */
    default <NO> Result<NO, E> map(Function<? super O, ? extends NO> op) {
        return switch (this) {
            case Ok(O value) -> ok(op.apply(value));
            case Err(E error) -> err(error);
        };
    }

    /**
     * Map the error value to a new value leaving success unchanged.
     * <p>
     * Example:
     * <pre>{@code
     * Result<Integer, String> res = Result.err("error");
     * var actual = res.mapErr(String::length);
     * assertThat(actual).hasError(5);
     *
     * Result<Integer, String> result = Result.ok(5);
     * var actual = result.mapErr(Kaboom::new);
     * assertThat(actual).hasValue(5);
     * }</pre>
     *
     * @param op   Mapping function.
     * @param <NE> New type of error value.
     * @return New result with the mapped error value.
     */
    default <NE> Result<O, NE> mapErr(Function<? super E, ? extends NE> op) {
        return switch (this) {
            case Ok(O value) -> ok(value);
            case Err(E error) -> err(op.apply(error));
        };
    }

    /**
     * Map the success value to a new value or return a fallback value.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * res.mapOr(v -> v * 2, 0); // 10
     *
     * Result<Integer, String> res = Result.err("error");
     * res.mapOr(v -> v * 2, 0); // 0
     * }</pre>
     *
     * @param op       Mapping function.
     * @param fallback Fallback value.
     * @return Mapped success value or fallback value.
     */
    default O mapOr(UnaryOperator<O> op, O fallback) {
        return switch (this) {
            case Ok(O value) -> op.apply(value);
            case Err(_) -> fallback;
        };
    }

    /**
     * Map the success value to a new value or return a fallback value.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * res.mapOrElse(v -> v * 2, () -> 0); // 10
     *
     * Result<Integer, String> res = Result.err("error");
     * res.mapOrElse(v -> v * 2, () -> 0); // 0
     * }</pre>
     *
     * @param op         Mapping function.
     * @param fallbackOp Fallback value supplier.
     * @return Mapped success value or fallback value.
     */
    default O mapOrElse(UnaryOperator<O> op, Supplier<? extends O> fallbackOp) {
        return switch (this) {
            case Ok(O value) -> op.apply(value);
            case Err(_) -> fallbackOp.get();
        };
    }

    /**
     * Call the consumer with the success value.
     * Note that this method does not prevent value mutation.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * res.inspect(System.out::println); // prints 5
     *
     * var res = Result.ok(5);
     * res.inspect(v -> v * 2);
     * assertThat(res).hasValue(5);
     *
     * Result<Integer, String> res = Result.err("error");
     * res.inspect(System.out::println); // does nothing
     * }</pre>
     *
     * @param op Consumer to call with the success value.
     * @return The same result.
     */
    default Result<O, E> inspect(Consumer<? super O> op) {
        if (this instanceof Ok(O value)) {
            op.accept(value);
        }

        return this;
    }

    /**
     * Call the consumer with the error value.
     * Note that this method does not prevent value mutation.
     * <p>
     * Example:
     * <pre>{@code
     * Result<Integer, String> res = Result.err("error");
     * res.inspectErr(System.out::println); // prints "error"
     *
     * Result<Integer, String> res = Result.err("error");
     * res.inspectErr(e -> e.toUpperCase());
     * assertThat(res).hasError("error");
     *
     * Result<Integer, String> res = Result.ok(5);
     * res.inspectErr(System.out::println); // does nothing
     * }</pre>
     *
     * @param op Consumer to call with the error value.
     * @return The same result.
     */
    default Result<O, E> inspectErr(Consumer<? super E> op) {
        if (this instanceof Err(E error)) {
            op.accept(error);
        }
        return this;
    }

    /**
     * Return the success value or throw an exception.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.expect("error")).isEqualTo(5);
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThatThrownBy(() -> res.expect("error"))
     *     .isInstanceOf(IllegalStateException.class)
     *     .hasMessage("error");
     * }</pre>
     *
     * @param message Error message.
     * @return Success value.
     * @throws IllegalStateException if the result is an error.
     */
    default O expect(String message) {
        return switch (this) {
            case Ok(O value) -> value;
            case Err(_) -> throw new IllegalStateException(message);
        };
    }

    /**
     * Return the error value or throw an exception.
     * <p>
     * Example:
     * <pre>{@code
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.expectErr("error")).isEqualTo("error");
     *
     * var res = Result.ok(5);
     * assertThatThrownBy(() -> res.expectErr("should be an error"))
     *     .isInstanceOf(IllegalStateException.class)
     *     .hasMessage("should be an error");
     * }</pre>
     *
     * @param message Error message.
     * @return Error value.
     * @throws IllegalStateException if the result is a success.
     */
    default E expectErr(String message) {
        return switch (this) {
            case Ok(_) -> throw new IllegalStateException(message);
            case Err(E error) -> error;
        };
    }

    /**
     * Unwrap the success value or throw an exception.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.unwrap()).isEqualTo(5);
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThatThrownBy(res::unwrap)
     *     .isInstanceOf(IllegalStateException.class)
     *     .hasMessage("called `Result.unwrap()` on an `Err` value");
     * }</pre>
     *
     * @return Success value.
     * @throws IllegalStateException if the result is an error.
     */
    default O unwrap() {
        return switch (this) {
            case Ok(O value) -> value;
            case Err(_) -> throw new IllegalStateException("called `Result.unwrap()` on an `Err` value");
        };
    }

    /**
     * Unwrap the error value or throw an exception.
     * <p>
     * Example:
     * <pre>{@code
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.unwrapErr()).isEqualTo("error");
     *
     * var res = Result.ok(5);
     * assertThatThrownBy(res::unwrapErr)
     *     .isInstanceOf(IllegalStateException.class)
     *     .hasMessage("called `Result.unwrapErr()` on an `Ok` value");
     * }</pre>
     *
     * @return Error value.
     * @throws IllegalStateException if the result is a success.
     */
    default E unwrapErr() {
        return switch (this) {
            case Ok(_) -> throw new IllegalStateException("called `Result.unwrapErr()` on an `Ok` value");
            case Err(E error) -> error;
        };
    }

    /**
     * Unwrap the success value or return a fallback value.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.unwrapOr(0)).isEqualTo(5);
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.unwrapOr(0)).isEqualTo(0);
     * }</pre>
     *
     * @param fallback Fallback value.
     * @return Success value or fallback value.
     */
    default O unwrapOr(O fallback) {
        return switch (this) {
            case Ok(O value) -> value;
            case Err(_) -> fallback;
        };
    }

    /**
     * Unwrap the success value or return a fallback value.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.unwrapOrElse(() -> 0)).isEqualTo(5);
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.unwrapOrElse(() -> 0)).isEqualTo(0);
     * }</pre>
     *
     * @param fallbackOp Fallback value supplier.
     * @return Success value or fallback value.
     */
    default O unwrapOrElse(Supplier<? extends O> fallbackOp) {
        return switch (this) {
            case Ok(O value) -> value;
            case Err(_) -> fallbackOp.get();
        };
    }

    /**
     * Return res if the result is Ok, otherwise return current error result.
     * <p>
     * Example:
     * <pre>{@code
     * var res = ok(5).and(err("error"));
     * assertThat(res).hasError("error");
     *
     * var res = err("error").and(ok(5));
     * assertThat(res).hasError("error");
     *
     * var res = err("error1").and(err("error2"));
     * assertThat(res).hasError("error1");
     *
     * var res = ok(5).and(ok(10));
     * assertThat(res).hasValue(10);
     * }</pre>
     *
     * @param res  Result to return if current result is Ok.
     * @param <NO> New success value type.
     * @return res if the result is Ok, otherwise return current error result.
     */
    default <NO> Result<NO, E> and(Result<NO, E> res) {
        return switch (this) {
            case Ok(_) -> res;
            case Err(E error) -> err(error);
        };
    }

    /**
     * Return the result of the operation if the result is Ok, otherwise return current error result.
     * This method can be used to control the flow based on the result value.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.andThen(() -> Result.ok(10))).hasValue(10);
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.andThen(() -> Result.ok(10))).hasError("error");
     * }</pre>
     *
     * @param op   Operation to return if current result is Ok.
     * @param <NO> New success value type.
     * @return Operation result if the result is Ok, otherwise return current error result.
     */
    default <NO> Result<NO, E> andThen(Supplier<? extends Result<NO, E>> op) {
        return switch (this) {
            case Ok(_) -> op.get();
            case Err(E error) -> err(error);
        };
    }


    /**
     * Return res if the result is Err, otherwise return current success result.
     * <p>
     * Example:
     * <pre>{@code
     * var res = ok(5).or(err("error"));
     * assertThat(res).hasValue(5);
     *
     * var res = err("error").or(ok(5));
     * assertThat(res).hasValue(5);
     *
     * var res = err("error1").or(err("error2"));
     * assertThat(res).hasError("error2");
     *
     * var res = ok(5).or(ok(10));
     * assertThat(res).hasValue(5);
     * }</pre>
     *
     * @param <NE> New error value type.
     * @return res if the result is Err, otherwise return current success result.
     */
    default <NE> Result<O, NE> or(Result<O, NE> res) {
        return switch (this) {
            case Ok(O value) -> ok(value);
            case Err(_) -> res;
        };
    }

    /**
     * Return the result of the operation if the result is Err, otherwise return current success result.
     * This method can be used to control the flow based on the result value.
     * <p>
     * Example:
     * <pre>{@code
     * var res = Result.ok(5);
     * assertThat(res.orElse(() -> Result.ok(10))).hasValue(5);
     *
     * Result<Integer, String> res = Result.err("error");
     * assertThat(res.orElse(() -> Result.ok(10))).hasValue(10);
     * }</pre>
     *
     * @param op   Operation to return if current result is Err.
     * @param <NE> New error value type.
     * @return Operation result if the result is Err, otherwise return current success result.
     */
    default <NE> Result<O, NE> orElse(Supplier<? extends Result<O, NE>> op) {
        return switch (this) {
            case Ok(O value) -> ok(value);
            case Err(_) -> op.get();
        };
    }
}
