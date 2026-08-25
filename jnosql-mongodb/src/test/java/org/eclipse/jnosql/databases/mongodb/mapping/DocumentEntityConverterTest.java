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
package org.eclipse.jnosql.databases.mongodb.mapping;


import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.document.DocumentTemplate;
import org.eclipse.jnosql.mapping.document.spi.DocumentExtension;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.reflection.spi.ReflectionEntityMetadataExtension;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class, DocumentTemplate.class, MongoDBTemplate.class})
@AddPackages(Music.class)
@AddPackages(Reflections.class)
@AddExtensions({ReflectionEntityMetadataExtension.class,
        DocumentExtension.class})
class DocumentEntityConverterTest {

    @Inject
    private EntityConverter converter;

    @Test
    void shouldConverterToDocument() {
        ObjectId id = new ObjectId();
        Music music = new Music(id.toString(), "Music", 2021);
        CommunicationEntity entity = converter.toCommunication(music);
        assertThat(entity).isNotNull();
        assertThat(entity.name()).isEqualTo(Music.class.getSimpleName());
        assertThat(entity.find("_id", ObjectId.class).get()).isEqualTo(id);
        assertThat(entity.find("name", String.class).get()).isEqualTo("Music");
        assertThat(entity.find("year", int.class).get()).isEqualTo(2021);
    }

    @Test
    void shouldConvertToEntity() {
        ObjectId id = new ObjectId();
        CommunicationEntity entity = CommunicationEntity.of("Music");
        entity.add("name", "Music");
        entity.add("year", 2022);
        entity.add("_id", id);

        Music music = converter.toEntity(entity);
        assertThat(music).isNotNull();
        assertThat(music.getName()).isEqualTo("Music");
        assertThat(music.getYear()).isEqualTo(2022);
        assertThat(music.getId()).isEqualTo(id.toString());
    }
}
