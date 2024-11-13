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
 */
package org.eclipse.jnosql.databases.arangodb.communication;


import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.keyvalue.KeyValueConfiguration;


/**
 * The ArangoDB implementation to {@link KeyValueConfiguration}
 *
 * @see ArangoDBConfigurations
 */
public final class ArangoDBKeyValueConfiguration extends ArangoDBConfiguration
        implements KeyValueConfiguration {


    @Override
    public ArangoDBBucketManagerFactory apply(Settings settings) {
        ArangoDBBuilder arangoDBBuilder = getArangoDBBuilder(settings);
        return new ArangoDBBucketManagerFactory(arangoDBBuilder);
    }
}
