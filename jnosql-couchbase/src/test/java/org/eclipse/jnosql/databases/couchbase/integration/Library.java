
/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at https://www.eclipse.org/legal/epl-2.0
 * and the Apache License v2.0 is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 *
 */
package org.eclipse.jnosql.databases.couchbase.integration;

import jakarta.data.repository.Repository;
import org.eclipse.jnosql.databases.couchbase.mapping.CouchbaseRepository;

import java.util.stream.Stream;

@Repository
public interface Library extends CouchbaseRepository<Magazine, String> {

    Stream<Magazine> findByEditionLessThan(Integer edition);

    Stream<Magazine> findByEditionGreaterThan(Integer edition);

    Stream<Magazine> findByTitleLike(String title);

}
