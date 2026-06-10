package net.lionarius.skinrestorer.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class Result<S, E> {
    private final S successValue;
    private final E errorValue;

    private Result(S successValue, E errorValue) {
        if (successValue == null && errorValue == null)
            throw new IllegalArgumentException("Cannot create result of null values");

        this.successValue = successValue;
        this.errorValue = errorValue;
    }

    public static <S, E> Result<S, E> success(@NotNull S successValue) {
        return new Result<>(successValue, null);
    }

    public static <S, E> Result<S, E> error(@NotNull E errorValue) {
        return new Result<>(null, errorValue);
    }

    public static <S, E> Result<Optional<S>, E> ofNullable(S successValue) {
        return Result.success(Optional.ofNullable(successValue));
    }

    public S getSuccessValue() {
        return successValue;
    }

    public E getErrorValue() {
        return errorValue;
    }

    public boolean isSuccess() {
        return successValue != null;
    }

    public boolean isError() {
        return errorValue != null;
    }

    public <N> Result<N, E> map(Function<S, N> mapper) {
        if (successValue == null) return new Result<>(null, errorValue);

        return new Result<>(mapper.apply(successValue), errorValue);
    }

    public <N> Result<S, N> mapError(Function<E, N> mapper) {
        if (errorValue == null) return new Result<>(successValue, null);

        return new Result<>(successValue, mapper.apply(errorValue));
    }

    public Optional<S> toOptional() {
        return Optional.ofNullable(successValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Result<?, ?> result = (Result<?, ?>) o;
        return Objects.equals(successValue, result.successValue) && Objects.equals(errorValue, result.errorValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(successValue, errorValue);
    }
}
