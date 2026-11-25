/*
 *  Copyright (c) 2026 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.integration;

import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Param;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface NativeFunctionAccountRepository extends BasicRepository<NativeFunctionAccount, String> {

    @Query("WHERE UPPER(name) = UPPER(:name)")
    List<NativeFunctionAccount> sameUpperName(@Param("name") String name);

    @Query("WHERE LEFT(name, LENGTH(:prefix)) = :prefix")
    List<NativeFunctionAccount> startingWith(@Param("prefix") String prefix);

    @Query("WHERE RIGHT(name, LENGTH(:suffix)) = :suffix")
    List<NativeFunctionAccount> endingWith(@Param("suffix") String suffix);

    @Query("WHERE UPPER(name) = 'OTAVIO'")
    List<NativeFunctionAccount> upperOtavio();

    @Query("WHERE UPPER(name) = UPPER(otherName)")
    List<NativeFunctionAccount> sameNameIgnoringCase();

    @Query("WHERE UPPER(LEFT(name, 3)) = :prefix")
    List<NativeFunctionAccount> upperPrefix(@Param("prefix") String prefix);

    @Query("WHERE ABS(balance) = :amount")
    List<NativeFunctionAccount> absoluteBalance(@Param("amount") int amount);

    @Query("WHERE UPPER(name) = :name AND ABS(balance) > :amount")
    List<NativeFunctionAccount> upperNameAndAbsoluteAbove(@Param("name") String name, @Param("amount") int amount);

    @Query("WHERE LOWER(name) LIKE :pattern")
    List<NativeFunctionAccount> lowerLike(@Param("pattern") String pattern);

    @Query("WHERE LEFT(name, balance) = 'x'")
    List<NativeFunctionAccount> unsupportedLength();
}
