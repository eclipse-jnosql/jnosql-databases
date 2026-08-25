/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.mapping;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code SQL} annotation is a custom annotation used to associate SQL query strings
 * with methods or elements in your code.
 * <p>
 * When applied to a method, field, or other element, this annotation provides a convenient
 * way to specify an SQL query that should be associated with that element.
 * </p>
 * <p>
 * For example, you can use this annotation to specify a SQL query for a method as follows:
 * </p>
 * <pre>
 * {@code
 * @SQL("SELECT * FROM users WHERE status = 'active'")
 * public void getUserData() {
 *     // Method implementation
 * }
 * }
 * </pre>
 * <p>
 * In the context of an {@code OracleRepository}, you can use the {@code @SQL} annotation
 * to define custom SQL queries for repository methods. Here's an example:
 * </p>
 * <pre>
 * {@code
 * public interface UserRepository extends OracleRepository<User, Long> {
 *     // Find all active users using a custom SQL query
 *     @SQL("SELECT * FROM users WHERE status = 'active'")
 *     List<User> findActiveUsers();
 * }
 * }
 * </pre>
 * <p>
 * In this example, the {@code @SQL} annotation is used to define a custom SQL query for the
 * {@code findActiveUsers} method in an {@code OracleRepository} interface, allowing you
 * to execute the specified SQL query when calling the repository method.
 * </p>
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SQL {


    /**
     * The SQL query to associate with the annotated method or element.
     *
     * @return The SQL query to associate with the annotated method or element.
     */
    String value();
}
