/*
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.tinkerpop.mapping;

import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;
import org.eclipse.jnosql.databases.tinkerpop.mapping.entities.Human;

import java.util.List;

@Repository
public interface Population extends TinkerPopRepository<Human, String> {

    @Gremlin("g.V().hasLabel('Human').order().by('name', Order.asc)")
    List<Human> allHumans();

    @Gremlin("g.V().hasLabel('Human').has('name', @name)")
    List<Human> findByName (@Param("name") String name);

}
