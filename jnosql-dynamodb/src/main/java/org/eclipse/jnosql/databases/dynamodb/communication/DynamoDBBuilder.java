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
 * Defines the dynamo dbbuilder contract.
 */
public interface DynamoDBBuilder {
/**
 * Performs the endpoint operation.
 *
 * @param endpoint the endpoint
 */
    void endpoint(String endpoint);

/**
 * Performs the region operation.
 *
 * @param region the region
 */
    void region(String region);

/**
 * Performs the profile operation.
 *
 * @param profile the profile
 */
    void profile(String profile);

/**
 * Performs the aws access key operation.
 *
 * @param awsAccessKey the aws access key
 */
    void awsAccessKey(String awsAccessKey);

/**
 * Performs the aws secret access operation.
 *
 * @param awsSecretAccess the aws secret access
 */
    void awsSecretAccess(String awsSecretAccess);

}
