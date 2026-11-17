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
package org.eclipse.jnosql.databases.dynamodb.communication;

/**
 * Attribute names of the items written by the DynamoDB key-value driver: each entry is stored as an item with
 * a {@value #KEY} attribute (the table's hash key) and a {@value #VALUE} attribute.
 */
public final class ConfigurationAmazonEntity {
    /**
     * The attribute that holds the entry key, a string, which is the table's hash key.
     */
    public static final String KEY = "key";
    /**
     * The attribute that holds the entry value.
     */
    public static final String VALUE = "value";

    private ConfigurationAmazonEntity() {
    }
}
