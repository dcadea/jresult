package io.github.dcadea.jresult;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Objects;

import static io.github.dcadea.jresult.external.assertj.ResultAssertions.assertThat;

class ResultTest {

    @Test
    void shouldThrowNullPointerExceptionWhenOkOfNull() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> {
                    var _ = Result.ok(null);
                });
    }

    @Test
    void shouldThrowNullPointerExceptionWhenErrOfNull() {
        Assertions.assertThatNullPointerException()
                .isThrownBy(() -> {
                    var _ = Result.err(null);
                });
    }

    @Test
    void shouldReturnOkWhenCreatingOkResult() {
        var result = Result.ok("value");
        assertThat(result).hasValue("value");
    }

    @Test
    void shouldReturnErrWhenCreatingErrResult() {
        var result = Result.err(new Kaboom("something went wrong"));
        assertThat(result).hasError(new Kaboom("something went wrong"));
    }

    @Test
    void shouldReturnTrueOnIsOkWhenResultHasValue() {
        var result = Result.ok(5);
        Assertions.assertThat(result.isOk()).isTrue();
    }

    @Test
    void shouldReturnFalseOnIsOkWhenResultIsErr() {
        var result = Result.err(new Boom(10));
        Assertions.assertThat(result.isOk()).isFalse();
    }

    @Test
    void shouldReturnTrueOnIsOkAndWhenResultHasValueGreaterThanFive() {
        var result = Result.ok(6);
        Assertions.assertThat(result.isOkAnd(v -> v > 5)).isTrue();
    }

    @Test
    void shouldReturnFalseOnIsOkAndWhenResultHasValueLessThanFive() {
        var result = Result.ok(6);
        Assertions.assertThat(result.isOkAnd(v -> v < 5)).isFalse();
    }

    @Test
    void shouldReturnFalseOnIsOkAndWhenResultIsErr() {
        Result<String, TestError> result = Result.err(new Crash(-3.14f));
        Assertions.assertThat(result.isOkAnd(Objects::isNull)).isFalse();
    }

    @Test
    void shouldReturnFalseOnIsErrWhenResultHasValue() {
        var result = Result.ok(5);
        Assertions.assertThat(result.isErr()).isFalse();
    }

    @Test
    void shouldReturnTrueOnIsErrWhenResultIsErr() {
        var result = Result.err(new Boom(10));
        Assertions.assertThat(result.isErr()).isTrue();
    }

    @Test
    void shouldReturnFalseOnIsErrAndWhenResultIsOk() {
        Result<Integer, Boom> result = Result.ok(6);
        Assertions.assertThat(result.isErrAnd(e -> e.radius() == 0)).isFalse();
    }

    @Test
    void shouldReturnTrueOnIsErrAndWhenResultHasBoomErrEqualsZero() {
        var result = Result.err(new Boom(0));
        Assertions.assertThat(result.isErrAnd(e -> e.radius() == 0)).isTrue();
    }

    @Test
    void shouldReturnFalseOnIsErrAndWhenResultCrashErrDoesNotEqualPi() {
        var result = Result.err(new Crash(3.14f));
        Assertions.assertThat(result.isErrAnd(e -> e.damage() == 3.15f)).isFalse();
    }

    @Test
    void shouldReturnOptionalContainingValueOnOkWhenResultIsOk() {
        var result = Result.ok(5);
        Assertions.assertThat(result.ok()).contains(5);
    }

    @Test
    void shouldReturnOptionalEmptyOnOkWhenResultIsErr() {
        var result = Result.err("error");
        Assertions.assertThat(result.ok()).isEmpty();
    }

    @Test
    void shouldReturnOptionalEmptyOnErrWhenResultIsOk() {
        var result = Result.ok(5);
        Assertions.assertThat(result.err()).isEmpty();
    }

    @Test
    void shouldReturnOptionalContainingErrorValueOnErrWhenResultIsErr() {
        var result = Result.err("error");
        Assertions.assertThat(result.err()).hasValue("error");
    }

    @Test
    void shouldReturnMappedOkResultWhenResultIsOk() {
        var result = Result.ok(5);
        var actual = result.map("result: %d"::formatted);
        assertThat(actual).hasValue("result: 5");
    }

    @Test
    void shouldNotApplyMappingWhenResultIsErr() {
        Result<Integer, String> result = Result.err("error");
        var actual = result.map(v -> v * 2);
        assertThat(actual).hasError("error");
    }

    @Test
    void shouldNotApplyErrorMappingWhenResultIsOk() {
        Result<Integer, String> result = Result.ok(5);
        var actual = result.mapErr(Kaboom::new);
        assertThat(actual).hasValue(5);
    }

    @Test
    void shouldReturnMappedErrResultWhenResultIsErr() {
        var result = Result.err("error");
        var actual = result.mapErr(String::length);
        assertThat(actual).hasError(5);
    }

    @Test
    void shouldReturnMappedValueOnMapOrWhenResultIsOk() {
        var result = Result.ok(5);
        var actual = result.mapOr(v -> v * 2, 0);
        Assertions.assertThat(actual).isEqualTo(10);
    }

    @Test
    void shouldReturnFallbackValueOnMapOrWhenResultIsErr() {
        Result<Integer, String> result = Result.err("error");
        var actual = result.mapOr(v -> v * 2, 0);
        Assertions.assertThat(actual).isZero();
    }

    @Test
    void shouldReturnMappedValueOnMapOrElseWhenResultIsOk() {
        var result = Result.ok(10);
        var actual = result.mapOrElse(v -> v * 2, () -> 5);
        Assertions.assertThat(actual).isEqualTo(20);
    }

    @Test
    void shouldReturnFallbackSuppliedValueOnMapOrElseWhenResultIsErr() {
        Result<Integer, String> result = Result.err("error");
        var actual = result.mapOrElse(v -> v * 10, () -> 3);
        Assertions.assertThat(actual).isEqualTo(3);
    }

    @Test
    void shouldInspectWhenResultIsOk() {
        var result = Result.ok(new Mutable(5));
        var list = new ArrayList<Integer>();

        var actual = result.inspect(v -> {
            v.a = 20;
            list.add(v.a);
        });

        Assertions.assertThat(list).containsOnly(20);
        assertThat(actual).hasValue(new Mutable(20));
    }

    @Test
    void shouldNotInspectWhenResultIsErr() {
        Result<Mutable, Crash> result = Result.err(new Crash(1.0f));
        var list = new ArrayList<Integer>();

        var actual = result.inspect(v -> {
            v.a = 20;
            list.add(v.a);
        });

        Assertions.assertThat(list).isEmpty();
        assertThat(actual).isErr();
    }

    @Test
    void shouldNotInspectErrWhenResultIsOk() {
        Result<Mutable, TestError> result = Result.ok(new Mutable(5));
        var errors = new ArrayList<TestError>();

        var actual = result.inspectErr(errors::add);

        Assertions.assertThat(errors).isEmpty();
        assertThat(actual).isOk();
    }

    @Test
    void shouldInspectErrWhenResultIsErr() {
        Result<Integer, Mutable> result = Result.err(new Mutable(2));
        var errors = new ArrayList<Mutable>();

        var actual = result.inspectErr(e -> {
            e.a *= 2;
            errors.add(e);
        });

        Assertions.assertThat(errors).containsOnly(new Mutable(4));
        assertThat(actual).isErr();
    }

    @Test
    void shouldReturnValueOnExpectWhenResultIsOk() {
        var result = Result.ok(5);
        Assertions.assertThat(result.expect("no value")).isEqualTo(5);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnExpectWhenResultIsErr() {
        Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> {
                    var result = Result.err("error");
                    result.expect("no value found");
                })
                .withMessage("no value found");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnExpectErrWhenResultIsOk() {
        Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> {
                    var result = Result.ok(5);
                    result.expectErr("it is ok");
                })
                .withMessage("it is ok");
    }

    @Test
    void shouldReturnErrorValueOnExpectErrWhenResultIsErr() {
        var result = Result.err(new Kaboom("omg"));
        Assertions.assertThat(result.expectErr("not an error"))
                .isEqualTo(new Kaboom("omg"));
    }

    @Test
    void shouldReturnValueOnUnwrapWhenResultIsOk() {
        var result = Result.ok(5);
        Assertions.assertThat(result.unwrap()).isEqualTo(5);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnUnwrapWhenResultIsErr() {
        Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> {
                    var result = Result.err("error");
                    result.unwrap();
                })
                .withMessage("called `Result.unwrap()` on an `Err` value");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionOnUnwrapErrWhenResultIsOk() {
        Assertions.assertThatIllegalStateException()
                .isThrownBy(() -> {
                    var result = Result.ok(5);
                    result.unwrapErr();
                })
                .withMessage("called `Result.unwrapErr()` on an `Ok` value");
    }

    @Test
    void shouldReturnErrorValueOnUnwrapErrWhenResultIsErr() {
        var result = Result.err(new Boom(4));
        Assertions.assertThat(result.unwrapErr())
                .isEqualTo(new Boom(4));
    }

    @Test
    void shouldReturnOriginalValueOnUnwrapOrWhenResultIsOk() {
        var result = Result.ok(5);
        Assertions.assertThat(result.unwrapOr(0)).isEqualTo(5);
    }

    @Test
    void shouldReturnFallbackValueOnUnwrapOrWhenResultIsErr() {
        Result<Integer, String> result = Result.err("error");
        Assertions.assertThat(result.unwrapOr(0)).isZero();
    }

    @Test
    void shouldReturnOriginalValueOnUnwrapOrElseWhenResultIsOk() {
        var result = Result.ok(5);
        Assertions.assertThat(result.unwrapOrElse(() -> 0))
                .isEqualTo(5);
    }

    @Test
    void shouldReturnFallbackValueOnUnwrapOrElseWhenResultIsErr() {
        Result<Integer, String> result = Result.err("error");
        Assertions.assertThat(result.unwrapOrElse(() -> 0)).isZero();
    }

    @Test
    void shouldReturnErrOnAndWhenFirstIsOkAndSecondIsErr() {
        Result<Integer, String> ok = Result.ok(5);
        Result<Mutable, String> err = Result.err("error");

        assertThat(ok.and(err)).hasError("error");
    }

    @Test
    void shouldReturnErrOnAndWhenFirstIsErrAndSecondIsEOk() {
        Result<Mutable, TestError> err = Result.err(new Boom(7));
        Result<Integer, TestError> ok = Result.ok(5);

        assertThat(err.and(ok)).hasError(new Boom(7));
    }

    @Test
    void shouldReturnFirstErrOnAndWhenBothAreErr() {
        Result<Integer, String> err1 = Result.err("error1");
        Result<Mutable, String> err2 = Result.err("error2");

        assertThat(err1.and(err2)).hasError("error1");
    }

    @Test
    void shouldReturnLastOkOnAndWhenBothAreOk() {
        Result<Integer, TestError> ok1 = Result.ok(5);
        Result<Mutable, TestError> ok2 = Result.ok(new Mutable(10));

        assertThat(ok1.and(ok2)).hasValue(new Mutable(10));
    }

    @Test
    void shouldReturnErrOnAndThenWhenFirstIsOkAndSecondIsErr() {
        Result<Integer, String> ok = Result.ok(5);
        Result<Mutable, String> err = Result.err("error");

        assertThat(ok.andThen(() -> err)).hasError("error");
    }

    @Test
    void shouldReturnErrOnAndThenWhenFirstIsErrAndSecondIsEOk() {
        Result<Mutable, TestError> err = Result.err(new Boom(7));
        Result<Integer, TestError> ok = Result.ok(5);

        assertThat(err.andThen(() -> ok)).hasError(new Boom(7));
    }

    @Test
    void shouldReturnFirstErrOnAndThenWhenBothAreErr() {
        Result<Integer, String> err1 = Result.err("error1");
        Result<Mutable, String> err2 = Result.err("error2");

        assertThat(err1.andThen(() -> err2)).hasError("error1");
    }

    @Test
    void shouldReturnLastOkOnAndThenWhenBothAreOk() {
        Result<Integer, TestError> ok1 = Result.ok(5);
        Result<Mutable, TestError> ok2 = Result.ok(new Mutable(10));

        assertThat(ok1.andThen(() -> ok2)).hasValue(new Mutable(10));
    }

    @Test
    void shouldReturnFirstOkOnOrWhenFirstIsOkAndSecondIsErr() {
        Result<Integer, TestError> ok = Result.ok(5);
        Result<Integer, String> err = Result.err("error");

        assertThat(ok.or(err)).hasValue(5);
    }

    @Test
    void shouldReturnLastOkOnOrWhenAtLeastOneIsOk() {
        Result<Integer, Boom> err = Result.err(new Boom(7));
        Result<Integer, Kaboom> ok = Result.ok(5);

        assertThat(err.or(ok)).hasValue(5);
    }

    @Test
    void shouldReturnLastErrorOnOrWhenAllAreErr() {
        Result<Mutable, Crash> err1 = Result.err(new Crash(3.14f));
        Result<Mutable, Boom> err2 = Result.err(new Boom(2));

        assertThat(err1.or(err2)).hasError(new Boom(2));
    }

    @Test
    void shouldReturnFirstOkOnOrWhenAllAreOk() {
        Result<Integer, String> ok1 = Result.ok(5);
        Result<Integer, TestError> ok2 = Result.ok(10);

        assertThat(ok1.or(ok2)).hasValue(5);
    }

    @Test
    void shouldReturnFirstOkOnOrElseWhenFirstIsOkAndSecondIsErr() {
        Result<Integer, TestError> ok = Result.ok(5);
        Result<Integer, String> err = Result.err("error");

        assertThat(ok.orElse(() -> err)).hasValue(5);
    }

    @Test
    void shouldReturnLastOkOnOrElseWhenAtLeastOneIsOk() {
        Result<Integer, Boom> err = Result.err(new Boom(7));
        Result<Integer, Kaboom> ok = Result.ok(5);

        assertThat(err.orElse(() -> ok)).hasValue(5);
    }

    @Test
    void shouldReturnLastErrorOnOrElseWhenAllAreErr() {
        Result<Mutable, Crash> err1 = Result.err(new Crash(3.14f));
        Result<Mutable, Boom> err2 = Result.err(new Boom(2));

        assertThat(err1.orElse(() -> err2)).hasError(new Boom(2));
    }

    @Test
    void shouldReturnFirstOkOnOrElseWhenAllAreOk() {
        Result<Integer, String> ok1 = Result.ok(5);
        Result<Integer, TestError> ok2 = Result.ok(10);

        assertThat(ok1.orElse(() -> ok2)).hasValue(5);
    }
}

sealed interface TestError permits Kaboom, Boom, Crash {
}

record Kaboom(String reason) implements TestError {
}

record Boom(int radius) implements TestError {
}

record Crash(float damage) implements TestError {
}

class Mutable {
    int a;

    Mutable(int a) {
        this.a = a;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Mutable m) {
            return this.a == m.a;
        }

        return false;
    }

    @Override
    public String toString() {
        return "Mutable{a:%d}".formatted(a);
    }
}