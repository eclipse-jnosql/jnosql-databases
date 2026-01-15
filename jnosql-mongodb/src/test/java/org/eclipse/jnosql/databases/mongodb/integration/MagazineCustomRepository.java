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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.mongodb.integration;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.By;
import jakarta.data.repository.Delete;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface MagazineCustomRepository {

    @Save
    Magazine save(Magazine magazine);

    @Save
    Iterable<Magazine> saveAll(Iterable<Magazine> books);

    @Delete
    void delete(Magazine magazine);

    @Delete
    void removeAll(Iterable<Magazine> books);

    @Find
    Optional<Magazine> getById(@By("id") String id);

    Stream<Magazine> findByIdIn(Iterable<String> ids);

    @Find
    Stream<Magazine> listAll();

    @Find
    Page<Magazine> listAll(PageRequest pageRequest, Order<Magazine> sortBy);

    @Query("delete from Magazine")
    void deleteAll();

}
