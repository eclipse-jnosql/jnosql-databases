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
 *   Maximillian Arruda
 */
package org.eclipse.jnosql.databases.mongodb.integration;

import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Save;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

@Repository
public interface AsciiCharacters extends DataRepository<AsciiCharacter, Integer> {

    @Save
    List<AsciiCharacter> saveAll(List<AsciiCharacter> characters);

    @Query("select thisCharacter" +
            " where hexadecimal like '4_'" +
            " and hexadecimal not like '%0'" +
            " and thisCharacter not in ('E', 'G')" +
            " and id not between 72 and 78" +
            " order by id asc")
    Character[] getABCDFO();

    long countByHexadecimalNotNull();

    default void populate() {
        if (this.countByHexadecimalNotNull() >= 127)
            return;

        var dictonary = new LinkedList<AsciiCharacter>();

        IntStream.range(1, 128)
                .mapToObj(AsciiCharacter::of)// Some databases don't support ASCII NULL character (0)
                .forEach(dictonary::add);

        this.saveAll(dictonary);
    }


}
