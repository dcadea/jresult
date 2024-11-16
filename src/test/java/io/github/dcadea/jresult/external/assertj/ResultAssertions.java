package io.github.dcadea.jresult.external.assertj;

import io.github.dcadea.jresult.Result;
import org.assertj.core.api.AbstractAssert;

public class ResultAssertions<O, E> extends AbstractAssert<ResultAssertions<O, E>, Result<O, E>> {

    protected ResultAssertions(Result<O, E> result) {
        super(result, ResultAssertions.class);
    }

    public static <O, E> ResultAssertions<O, E> assertThat(Result<O, E> actual) {
        return new ResultAssertions<>(actual);
    }

    public void isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected result to be empty, but was not");
        }
    }

    public void isOk() {
        isNotNull();
        if (!actual.isOk()) {
            failWithMessage("Expected result to be ok, but was not");
        }
    }

    public void isErr() {
        isNotNull();
        if (!actual.isErr()) {
            failWithMessage("Expected result to be err, but was not");
        }
    }

    public void hasValue(O expected) {
        isNotNull();
        if (!actual.isOkAnd(v -> v.equals(expected))) {
            try {
                failWithMessage("Expected result to have value <%s>, but was <%s>", expected, actual.unwrap());
            } catch (IllegalStateException e) {
                failWithMessage("Expected result to have value, but was <%s>".formatted(actual));
            }
        }
    }

    public void hasError(E expected) {
        isNotNull();
        if (!actual.isErrAnd(e -> e.equals(expected))) {
            try {
                failWithMessage("Expected result to have error <%s>, but was <%s>", expected, actual.unwrapErr());
            } catch (IllegalStateException e) {
                failWithMessage("Expected result to have error, but was <%s>".formatted(actual));
            }
        }
    }
}
