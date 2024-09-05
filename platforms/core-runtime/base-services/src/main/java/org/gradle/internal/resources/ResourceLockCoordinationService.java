/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.resources;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import org.gradle.api.Action;
import org.gradle.internal.InternalTransformer;
import org.gradle.internal.service.scopes.Scope;
import org.gradle.internal.service.scopes.ServiceScope;

import javax.annotation.Nullable;

@ServiceScope(Scope.CrossBuildSession.class)
public interface ResourceLockCoordinationService {
    /**
     * Gets the current {@link ResourceLockState} active in this thread.  This must be called in the context
     * of a {@link #withStateLock(InternalTransformer)} transform.
     *
     * @return the current {@link ResourceLockState} or null if not in a transform.
     */
    @Nullable
    ResourceLockState getCurrent();

    /**
     * Attempts to atomically change the state of resource locks.  Only one thread can alter the resource lock
     * states at one time.  Other threads will block until the resource lock state is free.  The provided
     * {@link InternalTransformer} should return a {@link org.gradle.internal.resources.ResourceLockState.Disposition}
     * that tells the resource coordinator how to proceed:
     *
     * FINISHED - All locks were acquired, release the state lock
     * FAILED - One or more locks were not acquired, roll back any locks that were acquired and release the state lock
     * RETRY - One or more locks were not acquired, roll back any locks that were acquired and block waiting for the
     * state to change, then run the transform again
     *
     * @return true if the lock state changes finished successfully, otherwise false.
     */
    boolean withStateLock(InternalTransformer<ResourceLockState.Disposition, ResourceLockState> stateLockAction);

    /**
     * A convenience for using {@link #withStateLock(InternalTransformer)}.
     *
     * Runs the given action while holding the resource state lock, repeating it until the action signals that it has completed.
     *
     * The action can return:
     * {@link OperationResult#COMPLETED_NO_RESULT} - the action has completed and this method returns {@code null}.
     * {@link Finished} - the action has completed and this method returns the value from this object.
     * {@link OperationResult#RETRY} - try to run the action again, blocking until the resource state has changed.
     */
    @Nullable
    <T> T runUntilFinished(Function<ResourceLockState, OperationResult<? super T>> action);

    /**
     * A convenience for using {@link #withStateLock(InternalTransformer)}.
     *
     * Runs the given action once while holding the resource state lock.
     */
    void run(Runnable action);

    /**
     * A convenience for using {@link #withStateLock(InternalTransformer)}.
     *
     * Runs the given action once while holding the resource state lock, and returns the result.
     */
    <T> T run(Supplier<T> action);

    /**
     * Notify other threads about changes to resource locks.
     */
    void notifyStateChange();

    void assertHasStateLock();

    /**
     * Adds a listener that is notified when a lock is released. Called while the state lock is held.
     */
    void addLockReleaseListener(Action<ResourceLock> listener);

    void removeLockReleaseListener(Action<ResourceLock> listener);

    interface OperationResult<T> {
        OperationResult<Object> RETRY = new OperationResult<Object>() {};
        OperationResult<Object> COMPLETED_NO_RESULT = new OperationResult<Object>() {};
    }

    class Finished<T> implements OperationResult<T> {
        private final T result;

        public Finished(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }
    }

}
