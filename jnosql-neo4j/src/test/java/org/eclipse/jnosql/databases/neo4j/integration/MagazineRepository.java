/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
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
 *   Maximillian Arruda
 */

package org.eclipse.jnosql.databases.neo4j.integration;

import jakarta.data.repository.Param;
import jakarta.data.repository.Repository;
import org.eclipse.jnosql.databases.neo4j.mapping.Cypher;
import org.eclipse.jnosql.databases.neo4j.mapping.Neo4JRepository;

import java.util.List;

@Repository
public interface MagazineRepository extends Neo4JRepository<Magazine, String> {

    @Cypher("MATCH (m:Magazine) RETURN m")
    List<Magazine> findAllByCypher();

    @Cypher("MATCH (m:Magazine{title: $title}) RETURN m")
    List<Magazine> findByTitle(@Param("title") String title);
}
