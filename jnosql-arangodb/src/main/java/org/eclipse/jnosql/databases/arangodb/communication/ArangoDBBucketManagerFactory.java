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
package org.eclipse.jnosql.databases.arangodb.communication;

import com.arangodb.ArangoDB;
import org.eclipse.jnosql.communication.keyvalue.BucketManagerFactory;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * The ArangoDB implementation to {@link BucketManagerFactory}
 * it does not support:
 * <p>{@link BucketManagerFactory#getMap(String, Class, Class)}</p>
 * <p>{@link BucketManagerFactory#getSet(String, Class)}</p>
 * <p>{@link BucketManagerFactory#getQueue(String, Class)}</p>
 * <p>{@link BucketManagerFactory#getList(String, Class)}</p>
 */
public class ArangoDBBucketManagerFactory implements BucketManagerFactory {

    private static final String DEFAULT_NAMESPACE = "diana";

    private final ArangoDBBuilder arangoDBBuilder;

    ArangoDBBucketManagerFactory(ArangoDBBuilder arangoDBBuilder) {
        this.arangoDBBuilder = arangoDBBuilder;
    }

    @Override
    public ArangoDBBucketManager apply(String bucketName) throws UnsupportedOperationException {
        return getBucketManager(bucketName, DEFAULT_NAMESPACE);
    }

    public ArangoDBBucketManager getBucketManager(String bucketName, String namespace) {
        ArangoDB arangoDB = arangoDBBuilder.build();
        ArangoDBUtil.checkCollection(bucketName, arangoDB, namespace);
        return new ArangoDBBucketManager(arangoDB, bucketName, namespace);
    }

    @Override
    public Map getMap(String bucketName, Class keyValue, Class valueValue) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The ArangoDB does not support getMap method");
    }

    @Override
    public Queue getQueue(String bucketName, Class clazz) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The ArangoDB does not support getQueue method");
    }

    @Override
    public Set getSet(String bucketName, Class clazz) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The ArangoDB does not support getSet method");
    }

    @Override
    public List getList(String bucketName, Class clazz) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("The ArangoDB does not support getList method");
    }

    @Override
    public void close() {
        // no-op
        // ArangoDB driver instance will be closed in ArangoDBBucketManager.close()
    }

}
