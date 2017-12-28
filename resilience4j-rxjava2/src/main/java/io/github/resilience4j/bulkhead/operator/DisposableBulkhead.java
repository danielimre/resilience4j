package io.github.resilience4j.bulkhead.operator;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.github.resilience4j.adapter.Permit;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.disposables.Disposable;

/**
 * A disposable bulkhead.
 */
class DisposableBulkhead implements Disposable {
    private Disposable disposable;
    private final Bulkhead bulkhead;
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicReference<Permit> permitted = new AtomicReference<>(Permit.PENDING);

    DisposableBulkhead(Bulkhead bulkhead) {
        this.bulkhead = requireNonNull(bulkhead);
    }

    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            releaseBulkhead();
            disposable.dispose();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }

    protected void setDisposable(Disposable disposable) {
        this.disposable = requireNonNull(disposable);
    }

    protected boolean acquireCallPermit() {
        boolean callPermitted = false;
        if (permitted.compareAndSet(Permit.PENDING, Permit.ACQUIRED)) {
            callPermitted = bulkhead.isCallPermitted();
            if (!callPermitted) {
                permitted.set(Permit.REJECTED);
            }
        }
        return callPermitted;
    }

    protected boolean isInvocationPermitted() {
        return !isDisposed() && wasCallPermitted();
    }

    protected void releaseBulkhead() {
        if (wasCallPermitted()) {
            bulkhead.onComplete();
        }
    }

    protected Exception bulkheadFullException() {
        return new BulkheadFullException(String.format("Bulkhead '%s' is full", bulkhead.getName()));
    }

    private boolean wasCallPermitted() {
        return permitted.get() == Permit.ACQUIRED;
    }
}
