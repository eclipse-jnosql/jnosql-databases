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
package org.eclipse.jnosql.databases.riak.communication;


import com.basho.riak.client.core.RiakNode;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;

public final class RiakTestUtils {


    public static BucketManagerFactory get() {

        RiakKeyValueConfiguration riakKeyValueConfiguration = new RiakKeyValueConfiguration();
        RiakNode node = new RiakNode.Builder()
                .withRemoteAddress("localhost").build();
        riakKeyValueConfiguration.add(node);
        return riakKeyValueConfiguration.apply(Settings.builder().build());
    }
}
