package com.artsafin.ofa.domain;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

public class Result<E, R> {
    private E error = null;
    private R result = null;

    public static <E,R> Stream<E> errorsOnly(Collection<Result<E,R>> coll) {
        return coll.stream().filter(Result::isError).map(Result::getError);
    }

    public static <E,R> Stream<R> resultsOnly(Collection<Result<E,R>> coll) {
        return coll.stream().filter(Result::notError).map(Result::getResult);
    }

    public static <E, R> Result<E, R> ofError(E error) {
        Result<E, R> ret = new Result<>();

        ret.error = error;

        return ret;
    }

    public static <E, R> Result<E, R> ofResult(R result) {
        Result<E, R> ret = new Result<>();

        ret.result = result;

        return ret;
    }

    private Result() {
    }

    public boolean isError() {
        return error != null;
    }

    public boolean notError() {
        return !isError();
    }

    @Nullable
    public E getError() {
        return error;
    }

    @Nullable
    public R getResult() {
        return result;
    }

    public <M> M map(Function<R, M> resultMapper, Function<E, M> errorMapper) {
        if (result != null) {
            return resultMapper.apply(result);
        }
        if (error != null) {
            return errorMapper.apply(error);
        }

        throw new AssertionError("Either result or error must have been set");
    }
}
