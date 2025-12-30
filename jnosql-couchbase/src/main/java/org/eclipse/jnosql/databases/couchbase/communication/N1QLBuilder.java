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
 */
package org.eclipse.jnosql.databases.couchbase.communication;

import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.communication.semistructured.UpdateQuery;

import java.util.function.Supplier;

sealed interface N1QLBuilder extends Supplier<N1QLQuery>
        permits N1QLSelectQueryBuilder, N1QLUpdateQueryBuilder {


    static N1QLBuilder of(SelectQuery query, String database, String scope) {
        return new N1QLSelectQueryBuilder(query, database, scope, false);
    }

    static N1QLBuilder countOf(SelectQuery query, String database, String scope) {
        return new N1QLSelectQueryBuilder(query, database, scope, true);
    }

    static N1QLBuilder of(UpdateQuery query, String database, String name) {
        return new N1QLUpdateQueryBuilder(query, database, name);
    }

}
