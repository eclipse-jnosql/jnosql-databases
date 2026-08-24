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
package org.eclipse.jnosql.databases.couchbase.mapping;

import org.eclipse.jnosql.mapping.ProviderQuery;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation used to define a dynamic N1QL query in {@link CouchbaseRepository}.
 * This annotation allows repository methods to be mapped to Couchbase N1QL queries.
 * Parameters can be provided using the {@link jakarta.data.repository.Param} annotation.
 *
 * Example Usage
 * <pre>{@code
 * @Repository
 * interface ProductRepository extends CouchbaseRepository<Product, String> {
 *
 *     @N1QL("SELECT * FROM products WHERE category = $category")
 *     List<Product> findByCategory(@Param("category") String category);
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ProviderQuery("n1ql")
public @interface N1QL {
    /**
     * The N1QL query string to be executed.
     *
     * @return the N1QL query
     */
    String value();
}
