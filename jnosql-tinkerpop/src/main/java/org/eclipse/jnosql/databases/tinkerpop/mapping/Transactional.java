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
package org.eclipse.jnosql.databases.tinkerpop.mapping;


import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Runs a CDI bean method, or every method of a bean, inside a TinkerPop
 * {@link org.apache.tinkerpop.gremlin.structure.Transaction}. The transaction is opened if needed, committed when
 * the method returns, and rolled back before the exception is rethrown when it fails.
 *
 * {@snippet lang="java" :
 * @ApplicationScoped
 * public class PersonService {
 *     @Inject
 *     private TinkerpopTemplate template;
 *
 *     @Transactional
 *     public void register(Person person) {
 *         template.insert(person);
 *     }
 * }
 * }
 */
@InterceptorBinding
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface Transactional {
}
