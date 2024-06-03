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
import jakarta.data.repository.*;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
public interface BookCustomRepository {

    @Save
    Book save(Book book);

    @Save
    Iterable<Book> saveAll(Iterable<Book> books);

    @Delete
    void delete(Book book);

    @Delete
    void removeAll(Iterable<Book> books);

    @Find
    Optional<Book> getById(@By("id") String id);

    @Find
    Stream<Book> findByIdIn(Iterable<String> ids);

    @Find
    Stream<Book> listAll();

    @Find
    Page<Book> listAll(PageRequest pageRequest, Order<Book> sortBy);

    @Query("delete from Book")
    void deleteAll();

}
