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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication;

final class CouchDBConstant {


    static final String ID = "_id";
    static final String REV = "_rev";
    static final String REV_RESPONSE = "rev";
    static final String ID_RESPONSE = "id";
    static final String ENTITY = "@entity";

    static final String ALL_DBS = "_all_dbs";
    static final String TOTAL_ROWS_RESPONSE = "total_rows";
    static final String REV_HEADER = "If-Match";
    static final String FIND = "/_find";
    static final String DOCS_RESPONSE = "docs";

    static final String COUNT = "/_all_docs?limit=0";
    static final String BOOKMARK = "bookmark";
    static final String OR_CONDITION = "$or";
    static final String AND_CONDITION = "$and";


    static final String IN_CONDITION = "$in";
    static final String LTE_CONDITION = "$lte";
    static final String LT_CONDITION = "$lt";
    static final String GTE_CONDITION = "$gte";
    static final String GT_CONDITION = "$gt";


    static final String SELECTOR_QUERY = "selector";
    static final String SORT_QUERY = "sort";
    static final String SKIP_QUERY = "skip";
    static final String LIMIT_QUERY = "limit";
    static final String FIELDS_QUERY = "fields";

    private CouchDBConstant() {
    }
}
