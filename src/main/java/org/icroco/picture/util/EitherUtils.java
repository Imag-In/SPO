package org.icroco.picture.util;

import io.jbock.util.Either;
import lombok.experimental.UtilityClass;
import org.springframework.lang.NonNull;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;

@UtilityClass
public class EitherUtils {
    @NonNull
    public static <L, R> Either<L, R> of(@NonNull Optional<R> possibleValue, @NonNull Supplier<L> errorSupplier) {
        return possibleValue.map(Either::<L, R>right).orElseGet(() -> Either.left(errorSupplier.get()));
    }

    public static <R> Either<Throwable, R> of(Supplier<R> valueSupplier) {
        try {
            return Either.right(valueSupplier.get());
        } catch (Throwable t) {
            return Either.left(t);
        }
    }

    public static <R> Either<Throwable, R> ofCallable(Callable<R> valueSupplier) {
        try {
            return Either.right(valueSupplier.call());
        } catch (Throwable t) {
            return Either.left(t);
        }
    }

    public record EitherError<I>(I Item, Throwable exception) {
    }

    public static <T, R> Either<EitherError<T>, R> of(T t, Function<T, R> valueSpplier) {
        try {
            return Either.right(valueSpplier.apply(t));
        } catch (Throwable throwable) {
            return Either.left(new EitherError<>(t, throwable));
        }
    }
}
