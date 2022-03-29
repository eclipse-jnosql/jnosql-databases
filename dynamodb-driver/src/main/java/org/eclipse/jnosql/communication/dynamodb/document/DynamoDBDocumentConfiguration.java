/*
 *  Copyright (c) 2022 Otávio Santana and others
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
 *   Alessandro Moscatelli
 */

package org.eclipse.jnosql.communication.dynamodb.document;

import jakarta.nosql.Settings;
import jakarta.nosql.document.DocumentConfiguration;
import org.eclipse.jnosql.communication.dynamodb.DynamoDBConfiguration;

public class DynamoDBDocumentConfiguration extends DynamoDBConfiguration implements DocumentConfiguration {
    
    @Override
    public DynamoDBDocumentCollectionManagerFactory get() {
        return new DynamoDBDocumentCollectionManagerFactory(
                builder.build()
        );
    }

    @Override
    public DynamoDBDocumentCollectionManagerFactory get(Settings stngs) {
        return new DynamoDBDocumentCollectionManagerFactory(
                getDynamoDB(stngs)
        );
    }
    
}
