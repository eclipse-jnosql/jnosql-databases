/*
 *
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

import jakarta.data.Sort;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

final class CouchdbDeleteQuery implements SelectQuery {

    private static final List<String> DOCUMENTS = asList(CouchDBConstant.ID, CouchDBConstant.REV);
    private final DeleteQuery query;

    CouchdbDeleteQuery(DeleteQuery query) {
        this.query = query;
    }

    @Override
    public long limit() {
        return 0;
    }

    @Override
    public long skip() {
        return 0;
    }

    @Override
    public String name() {
        return query.name();
    }

    @Override
    public Optional<CriteriaCondition> condition() {
        return query.condition();
    }

    @Override
    public List<Sort<?>> sorts() {
        return Collections.emptyList();
    }

    @Override
    public List<String> columns() {
        return DOCUMENTS;
    }
}
