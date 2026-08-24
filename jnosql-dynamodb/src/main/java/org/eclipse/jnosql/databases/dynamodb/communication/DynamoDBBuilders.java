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
 *   Alessandro Moscatelli
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import org.eclipse.jnosql.communication.Settings;

final class DynamoDBBuilders {



    private DynamoDBBuilders() {
    }

    static void load(Settings settings, DynamoDBBuilder dynamoDB) {
        settings.get(DynamoDBConfigurations.ENDPOINT).map(Object::toString).ifPresent(dynamoDB::endpoint);
        settings.get(DynamoDBConfigurations.REGION).map(Object::toString).ifPresent(dynamoDB::region);
        settings.get(DynamoDBConfigurations.PROFILE).map(Object::toString).ifPresent(dynamoDB::profile);
        settings.get(DynamoDBConfigurations.AWS_ACCESSKEY).map(Object::toString).ifPresent(dynamoDB::awsAccessKey);
        settings.get(DynamoDBConfigurations.AWS_SECRET_ACCESS).map(Object::toString).ifPresent(dynamoDB::awsSecretAccess);
    }

}
