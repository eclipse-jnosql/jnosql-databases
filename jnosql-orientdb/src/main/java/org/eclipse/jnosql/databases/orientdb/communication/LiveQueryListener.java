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

import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OLiveQueryResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;

class LiveQueryListener implements OLiveQueryResultListener {

    private final OrientDBLiveCallback<CommunicationEntity> callbacks;
    private final ODatabaseSession tx;

    LiveQueryListener(OrientDBLiveCallback<CommunicationEntity> callbacks, ODatabaseSession tx) {
        this.callbacks = callbacks;
        this.tx = tx;
    }

    @Override
    public void onCreate(ODatabaseDocument database, OResult data) {
        CommunicationEntity entity = OrientDBConverter.convert(data);
        callbacks.getCreateCallback().ifPresent(callback -> callback.accept(entity));
    }

    @Override
    public void onUpdate(ODatabaseDocument database, OResult before, OResult after) {
        CommunicationEntity entity = OrientDBConverter.convert(after);
        callbacks.getCreateCallback().ifPresent(callback -> callback.accept(entity));
    }

    @Override
    public void onDelete(ODatabaseDocument database, OResult data) {
        CommunicationEntity entity = OrientDBConverter.convert(data);
        callbacks.getCreateCallback().ifPresent(callback -> callback.accept(entity));
    }

    @Override
    public void onError(ODatabaseDocument database, OException exception) {
        System.out.print("error");
    }

    @Override
    public void onEnd(ODatabaseDocument database) {
        tx.close();
    }
}
