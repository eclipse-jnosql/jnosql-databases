/*
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.jnosql.databases.dynamodb.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code PartiQL} annotation is a custom annotation used to associate PartiQL query strings
 * with methods or elements in your code.
 *
 * <p>DynamoDB supports a limited subset of
 * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html">PartiQL</a>.
 * </p>
 * <p>
 * <p>
 * When applied to a method, field, or other element, this annotation provides a convenient
 * way to specify an PartiQL query that should be associated with that element.
 * </p>
 * <p>
 * For example, you can use this annotation to specify a PartiQL query for a method as follows:
 * </p>
 * <pre>
 * {@code
 * @PartiQL("SELECT * FROM users WHERE status = 'active'")
 * public void getUserData() {
 *     // Method implementation
 * }
 * }
 * </pre>
 * <p>
 * In the context of an {@code DynamoDBRepository}, you can use the {@code @PartiQL} annotation
 * to define custom PartiQL queries for repository methods. Here's an example:
 * </p>
 * <pre>
 * {@code
 * public interface UserRepository extends DynamoDBRepository<User, Long> {
 *     // Find all active users using a custom SQL query
 *     @PartiQL("SELECT * FROM users WHERE status = 'active'")
 *     List<User> findActiveUsers();
 * }
 * }
 * </pre>
 * <p>
 * In this example, the {@code @PartiQL} annotation is used to define a custom PartiQL query for the
 * {@code findActiveUsers} method in an {@code DynamoDBRepository} interface, allowing you
 * to execute the specified PartiQL query when calling the repository method.
 * </p>
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PartiQL {

    /**
     * The PartiQL query to associated with the annotated method or element.
     *
     * <p>DynamoDB supports a limited subset of
     * <a href="https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ql-reference.html">PartiQL</a>.
     * </p>
     *
     * @return the PartiQL query
     */
    String value();
}
