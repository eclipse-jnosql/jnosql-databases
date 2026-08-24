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
 *   Michele Rastelli
 */
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoDB;

/**
 * Provides access to the underlying ArangoDB driver instance.
 */
public interface ArangoDBAccessor {

    /**
     * Accessor for the underlying ArangoDB driver instance.
     *
     * @return the {@link ArangoDB} instance
     */
    ArangoDB getArangoDB();

}
