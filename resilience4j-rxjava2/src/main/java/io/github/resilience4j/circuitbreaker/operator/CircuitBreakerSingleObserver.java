package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava {@link SingleObserver} to protect another observer by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerSingleObserver<T> extends DisposableCircuitBreaker implements SingleObserver<T> {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerSingleObserver.class);
    private final SingleObserver<? super T> childObserver;

    CircuitBreakerSingleObserver(CircuitBreaker circuitBreaker, SingleObserver<? super T> childObserver) {
        super(circuitBreaker);
        this.childObserver = requireNonNull(childObserver);
    }

    @Override
    public void onSubscribe(Disposable disposable) {
        LOG.debug("onSubscribe");
        setDisposable(disposable);
        if (acquireCallPermit()) {
            childObserver.onSubscribe(this);
        } else {
            disposable.dispose();
            childObserver.onSubscribe(this);
            childObserver.onError(circuitBreakerOpenException());
        }
    }

    @Override
    public void onError(Throwable e) {
        LOG.debug("onError", e);
        markFailure(e);
        if (isInvocationPermitted()) {
            childObserver.onError(e);
        }
    }

    @Override
    public void onSuccess(T value) {
        LOG.debug("onComplete");
        markSuccess();
        if (isInvocationPermitted()) {
            childObserver.onSuccess(value);
        }
    }
}