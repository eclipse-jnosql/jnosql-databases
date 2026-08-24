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
package org.eclipse.jnosql.databases.orientdb.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for defining dynamic SQL queries in OrientDB repositories.
 * This annotation is used on methods within {@link OrientDBCrudRepository} to execute
 * custom SQL queries directly on OrientDB.
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @SQL("SELECT FROM User WHERE age > :age")
 * List<User> findUsersByAge(@Param("age") int age);
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SQL {

    /**
     * Defines the SQL query to be executed.
     *
     * @return the SQL query string
     */
    String value();
}
