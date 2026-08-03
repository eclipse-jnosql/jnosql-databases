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
 *   Otavio Santana
 *   Lucas Furlaneto
 */
package org.eclipse.jnosql.databases.orientdb.communication;

/**
 * Defines the orient dblive create callback contract.
 *
 * @param <T> the type
 */
@FunctionalInterface
public interface OrientDBLiveCreateCallback<T> {
/**
 * Performs the accept operation.
 *
 * @param t the t
 */
    void accept(T t);
}
