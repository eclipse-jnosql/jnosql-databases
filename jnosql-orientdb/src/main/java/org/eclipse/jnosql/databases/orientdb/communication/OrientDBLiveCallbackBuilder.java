/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
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

/**
 * Builds orient dblive callback builder instances.
 */
public final class OrientDBLiveCallbackBuilder {
    private OrientDBLiveCreateCallback<CommunicationEntity> createCallback;
    private OrientDBLiveUpdateCallback<CommunicationEntity> updateCallback;
    private OrientDBLiveDeleteCallback<CommunicationEntity> deleteCallback;

    private OrientDBLiveCallbackBuilder() {
    }

/**
 * Returns the builder.
 *
 * @return the result
 */
    public static OrientDBLiveCallbackBuilder builder() {
        return new OrientDBLiveCallbackBuilder();
    }

/**
 * Returns the on create.
 *
 * @param createCallback the create callback
 * @return the result
 */
    public OrientDBLiveCallbackBuilder onCreate(OrientDBLiveCreateCallback<CommunicationEntity> createCallback) {
        requireNonNull(createCallback, "createCallback is required");
        this.createCallback = createCallback;
        return this;
    }

/**
 * Returns the on update.
 *
 * @param updateCallback the update callback
 * @return the result
 */
    public OrientDBLiveCallbackBuilder onUpdate(OrientDBLiveUpdateCallback<CommunicationEntity> updateCallback) {
        requireNonNull(updateCallback, "updateCallback is required");
        this.updateCallback = updateCallback;
        return this;
    }

/**
 * Returns the on delete.
 *
 * @param deleteCallback the delete callback
 * @return the result
 */
    public OrientDBLiveCallbackBuilder onDelete(OrientDBLiveDeleteCallback<CommunicationEntity> deleteCallback) {
        requireNonNull(deleteCallback, "deleteCallback is required");
        this.deleteCallback = deleteCallback;
        return this;
    }

/**
 * Returns the build.
 *
 * @return the result
 */
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
