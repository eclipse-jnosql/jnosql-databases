/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */
package org.eclipse.jnosql.databases.dynamodb.mapping.inheritance;

import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface Projects {

    @Save
    <T extends Project> T save(T project);

    @Delete
    void delete(Project project);

    @Find
    Stream<Project> findAll();

    @Find
    Stream<SmallProject> findAllSmallProjects();

    @Find
    Stream<BigProject> findAllBigProjects();

    @Find
    Optional<Project> findById(@By("_id") String id);
}
