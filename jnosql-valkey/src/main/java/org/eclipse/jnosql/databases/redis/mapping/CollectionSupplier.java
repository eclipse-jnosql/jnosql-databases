/*
 *  Copyright (c) 2023 Eclipse Contribuitor
 * All rights reserved. This program and the accompanying materials
 *  and Apache License v2.0 which accompanies this distribution.
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *    You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.jnosql.databases.redis.mapping;

import jakarta.data.exceptions.MappingException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import org.eclipse.jnosql.databases.redis.communication.Counter;
import org.eclipse.jnosql.databases.redis.communication.RedisBucketManagerFactory;
import org.eclipse.jnosql.databases.redis.communication.SortedSet;
import org.eclipse.jnosql.mapping.keyvalue.KeyValueDatabase;


@ApplicationScoped
class CollectionSupplier {


    @Inject
    private RedisBucketManagerFactory factory;


    @Produces
    @KeyValueDatabase("")
    public Counter getSet(InjectionPoint injectionPoint) {
        return factory.getCounter(bucketName(injectionPoint));
    }

    @Produces
    @KeyValueDatabase("")
    public SortedSet getRanking(InjectionPoint injectionPoint) {
        return factory.getSortedSet(bucketName(injectionPoint));
    }


    private static String bucketName(InjectionPoint injectionPoint) {

        KeyValueDatabase keyValue = injectionPoint.getQualifiers()
                .stream().filter(KeyValueDatabase.class::isInstance)
                .map(KeyValueDatabase.class::cast)
                .findFirst().orElseThrow(() -> new MappingException("There is an issue to load " +
                        "a Collection from the database"));
        return keyValue.value();
    }
}
