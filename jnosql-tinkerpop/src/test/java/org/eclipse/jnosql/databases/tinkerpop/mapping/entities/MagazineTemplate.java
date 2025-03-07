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
package org.eclipse.jnosql.databases.tinkerpop.mapping.entities;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.tinkerpop.mapping.TinkerpopTemplate;
import org.eclipse.jnosql.databases.tinkerpop.mapping.Transactional;

@ApplicationScoped
public class MagazineTemplate {

    @Inject
    private TinkerpopTemplate tinkerpopTemplate;

    @Transactional
    public void insert(Magazine actor) {
        tinkerpopTemplate.insert(actor);
    }

    @Transactional
    public void insertException(Magazine actor) {
        tinkerpopTemplate.insert(actor);
        throw new NullPointerException("should get a rollback");
    }

    public void normalInsertion(Magazine actor) {
        tinkerpopTemplate.insert(actor);
    }

}
