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
 */

package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.ServerAddress;

class HostPortConfiguration {


    private final String host;

    private final int port;

    HostPortConfiguration(String value) {
        String[] values = value.split(":");
        if (values.length == 2) {
            host = values[0];
            port = Integer.parseInt(values[1]);
        } else {
            host = values[0];
            port = MongoDBDocumentConfiguration.DEFAULT_PORT;
        }
    }

    ServerAddress toServerAddress() {
        return new ServerAddress(host, port);
    }
}
