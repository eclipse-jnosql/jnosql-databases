/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License 2.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 *   and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.databases.qdrant.communication;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import jakarta.data.exceptions.DataException;

import java.util.concurrent.ExecutionException;

/**
 * Blocks on the asynchronous Qdrant client calls and translates their failures.
 */
final class QdrantCalls {

    private QdrantCalls() {
    }

    static <T> T await(ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new DataException("Interrupted while waiting for Qdrant", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (cause instanceof RuntimeException runtime && status(runtime) == null) {
                throw runtime;
            }
            throw new DataException("Qdrant call failed: " + cause.getMessage(), cause);
        }
    }

    static boolean isNotFound(Throwable throwable) {
        return hasCode(throwable, Status.Code.NOT_FOUND);
    }

    static boolean isAlreadyExists(Throwable throwable) {
        return hasCode(throwable, Status.Code.ALREADY_EXISTS);
    }

    private static boolean hasCode(Throwable throwable, Status.Code code) {
        Throwable current = throwable;
        while (current != null) {
            Status status = status(current);
            if (status != null && status.getCode() == code) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static Status status(Throwable throwable) {
        return throwable instanceof StatusRuntimeException grpc ? grpc.getStatus() : null;
    }
}
