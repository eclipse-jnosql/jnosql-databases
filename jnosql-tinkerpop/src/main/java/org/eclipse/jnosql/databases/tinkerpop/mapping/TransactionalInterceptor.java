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

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.eclipse.jnosql.databases.tinkerpop.communication.GraphTransactionUtil;


@Transactional
@Interceptor
@Alternative
@Priority(Interceptor.Priority.APPLICATION)
class TransactionalInterceptor {

    @Inject
    private Instance<Graph> graph;

    @AroundInvoke
    public Object manageTransaction(InvocationContext context) throws Exception {
        Transaction transaction = graph.get().tx();
        GraphTransactionUtil.lock(transaction);
        if (!transaction.isOpen()) {
            transaction.open();
        }
        try {
            Object proceed = context.proceed();
            transaction.commit();
            return proceed;
        } catch (Exception exception) {
            transaction.rollback();
            throw exception;
        }finally {
            GraphTransactionUtil.unlock();
        }

    }


}
