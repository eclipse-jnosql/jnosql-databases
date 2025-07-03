/*
 * Copyright (c) 2023 Contributors to the Eclipse Foundation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *
 * Maximillian Arruda
 */

package org.eclipse.jnosql.databases.dynamodb.communication;

import jakarta.json.Json;
import jakarta.json.bind.Jsonb;
import net.datafaker.Faker;
import org.eclipse.jnosql.communication.driver.JsonbSupplier;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.StringReader;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.MATCHES;
import static org.eclipse.jnosql.communication.driver.IntegrationTest.NAMED;

@EnabledIfSystemProperty(named = NAMED, matches = MATCHES)
class DynamoDBConverterTest {

    static final Faker faker = new Faker();

    private static final Jsonb JSONB = JsonbSupplier.getInstance().get();

    @Test
    void shouldConvertToItemRequest() {

        assertSoftly(softly -> {
            var entity = CommunicationEntity.of("entityA",
                    List.of(
                            Element.of("_id", UUID.randomUUID().toString()),
                            Element.of("city", faker.address().city()),
                            Element.of("total", 10.0),
                            Element.of("address", List.of(
                                    Element.of("zipcode", faker.address().zipCode()),
                                    Element.of("city", faker.address().cityName()))),
                            Element.of("phones", List.of(faker.name().firstName(), faker.name().firstName(), faker.name().firstName()))
                    ));

            var item = DynamoDBConverter.toItem(entity);

            var entityFromItem = DynamoDBConverter.toCommunicationEntity("entityA", item);

            var expected = Json.createReader(new StringReader(JSONB.toJson(DynamoDBConverter.getMap(entity)))).readObject();

            var actual = Json.createReader(new StringReader(JSONB.toJson(DynamoDBConverter.getMap(entityFromItem)))).readObject();

            softly.assertThat(actual).as("cannot convert a simple DocumentEntity")
                    .isEqualTo(expected);
        });


    }

}
