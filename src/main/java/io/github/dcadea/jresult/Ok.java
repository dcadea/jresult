package io.github.dcadea.jresult;

import static java.util.Objects.requireNonNull;

/**
 * Ok represents the success branch of the {@link Result} type.
 * It contains the success value and never an error value.
 * <p>
 * It is encouraged to use the static factory method {@link Result#ok(Object)} to create an instance of Ok.
 *
 * @param <O>   Type of success value.
 * @param <E>   Type of error value.
 * @param value Ok value
 * @author dcadea
 * @see Result#ok(Object)
 * @since 0.1.0
 */
public record Ok<O, E>(O value) implements Result<O, E> {
    /**
     * Creates an ok variant of result.
     *
     * @param value Success value.
     * @throws NullPointerException if the value is null.
     */
    public Ok {
        requireNonNull(value, "Ok cannot contain null");
    }
}
