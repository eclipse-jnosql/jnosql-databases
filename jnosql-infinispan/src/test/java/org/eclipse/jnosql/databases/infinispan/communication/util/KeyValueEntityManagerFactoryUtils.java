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
 *   The Infinispan Team
 */
package org.eclipse.jnosql.databases.infinispan.communication.util;

import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;
import org.eclipse.jnosql.databases.infinispan.communication.InfinispanConfigurations;
import org.eclipse.jnosql.databases.infinispan.communication.InfinispanKeyValueConfiguration;


public class KeyValueEntityManagerFactoryUtils {

    public static BucketManagerFactory get() {
        KeyValueConfiguration configuration = new InfinispanKeyValueConfiguration();
        Settings settings = Settings.builder()
                .put(InfinispanConfigurations.CONFIG, "infinispan.xml")
                .build();
        BucketManagerFactory managerFactory = configuration.apply(settings);
        return managerFactory;
    }
}
