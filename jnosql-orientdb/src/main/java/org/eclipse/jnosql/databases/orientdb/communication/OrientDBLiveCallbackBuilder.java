/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
 *   Lucas Furlaneto
 */
package org.eclipse.jnosql.databases.orientdb.communication;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;

import static java.util.Objects.requireNonNull;

public final class OrientDBLiveCallbackBuilder {
    private OrientDBLiveCreateCallback<CommunicationEntity> createCallback;
    private OrientDBLiveUpdateCallback<CommunicationEntity> updateCallback;
    private OrientDBLiveDeleteCallback<CommunicationEntity> deleteCallback;

    private OrientDBLiveCallbackBuilder() {
    }

    public static OrientDBLiveCallbackBuilder builder() {
        return new OrientDBLiveCallbackBuilder();
    }

    public OrientDBLiveCallbackBuilder onCreate(OrientDBLiveCreateCallback<CommunicationEntity> createCallback) {
        requireNonNull(createCallback, "createCallback is required");
        this.createCallback = createCallback;
        return this;
    }

    public OrientDBLiveCallbackBuilder onUpdate(OrientDBLiveUpdateCallback<CommunicationEntity> updateCallback) {
        requireNonNull(updateCallback, "updateCallback is required");
        this.updateCallback = updateCallback;
        return this;
    }

    public OrientDBLiveCallbackBuilder onDelete(OrientDBLiveDeleteCallback<CommunicationEntity> deleteCallback) {
        requireNonNull(deleteCallback, "deleteCallback is required");
        this.deleteCallback = deleteCallback;
        return this;
    }

    public OrientDBLiveCallback<CommunicationEntity> build() {
        validateNonNullCallbacks();
        return new OrientDBLiveCallback<>(createCallback, updateCallback, deleteCallback);
    }

    private void validateNonNullCallbacks() {
        if (createCallback == null && updateCallback == null && deleteCallback == null) {
            throw new IllegalArgumentException("At least one callback is required on OrientDB Live Query");
        }
    }
}