package io.github.dcadea.jresult;

import static java.util.Objects.requireNonNull;

/**
 * Err represents the error branch of the {@link Result} type.
 * It contains the error value and never a success value.
 * <p>
 * It is encouraged to use the static factory method {@link Result#err(Object)} to create an instance of Err.
 *
 * @param <O>   Type of success value.
 * @param <E>   Type of error value.
 * @param error Error value
 * @author dcadea
 * @see Result#err(Object)
 * @since 0.1.0
 */
public record Err<O, E>(E error) implements Result<O, E> {

    /**
     * Creates an error variant of result.
     *
     * @param error Error value.
     * @throws NullPointerException if the error is null.
     */
    public Err {
        requireNonNull(error, "Error cannot contain null");
    }

}
