package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RxJava {@link Observer} to protect another observer by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
final class CircuitBreakerObserver<T> extends DisposableCircuitBreaker implements Observer<T> {
    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerObserver.class);
    private final Observer<? super T> childObserver;

    CircuitBreakerObserver(CircuitBreaker circuitBreaker, Observer<? super T> childObserver) {
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
    public void onNext(T event) {
        LOG.debug("onNext: {}", event);
        if (isInvocationPermitted()) {
            childObserver.onNext(event);
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
    public void onComplete() {
        LOG.debug("onComplete");
        markSuccess();
        if (isInvocationPermitted()) {
            childObserver.onComplete();
        }
    }
}
