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

package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.MongoCredential;
import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

final class MongoAuthentication {

    private MongoAuthentication() {
    }

    static Optional<MongoCredential> of(Settings settings) {

        Optional<String> user = settings.getSupplier(Arrays.asList(MongoDBDocumentConfigurations.USER,
                Configurations.USER))
                .map(Object::toString);

        Optional<char[]> password = settings.getSupplier(Arrays.asList(MongoDBDocumentConfigurations.PASSWORD,
                Configurations.PASSWORD))
                .map(Object::toString).map(String::toCharArray);

        Optional<String> source = settings.get(MongoDBDocumentConfigurations.AUTHENTICATION_SOURCE)
                .map(Object::toString);

        Optional<AuthenticationMechanism> mechanism = settings.get(MongoDBDocumentConfigurations.AUTHENTICATION_MECHANISM)
                .map(Object::toString)
                .map(AuthenticationMechanism::fromMechanismName);

        if (user.isEmpty()) {
            return Optional.empty();
        }

        if (mechanism.isEmpty()) {
            return Optional.of(MongoCredential.createCredential(user.orElseThrow(missingExceptionUser()),
                    source.orElseThrow(missingExceptionSource()), password.orElseThrow(missingExceptionPassword())));
        }

        return switch (mechanism.get()) {
            case PLAIN -> Optional.of(MongoCredential.createPlainCredential(user.orElseThrow(missingExceptionUser()),
                    source.orElseThrow(missingExceptionSource()), password.orElseThrow(missingExceptionPassword())));
            case GSSAPI ->
                    Optional.of(MongoCredential.createGSSAPICredential(user.orElseThrow(missingExceptionUser())));
            case SCRAM_SHA_1 ->
                    Optional.of(MongoCredential.createScramSha1Credential(user.orElseThrow(missingExceptionUser()),
                            source.orElseThrow(missingExceptionSource()), password.orElseThrow(missingExceptionPassword())));
            case MONGODB_X509 ->
                    Optional.of(MongoCredential.createMongoX509Credential(user.orElseThrow(missingExceptionUser())));
            case SCRAM_SHA_256 ->
                    Optional.of(MongoCredential.createScramSha256Credential(user.orElseThrow(missingExceptionUser()),
                            source.orElseThrow(missingExceptionSource()), password.orElseThrow(missingExceptionPassword())));
            default -> throw new CommunicationException("There is not support to the type: " + mechanism);
        };

    }


    private static Supplier<CommunicationException> missingExceptionUser() {
        return missingException("user");
    }

    private static Supplier<CommunicationException> missingExceptionPassword() {
        return missingException("password");
    }

    private static Supplier<CommunicationException> missingExceptionSource() {
        return missingException("source");
    }


    private static Supplier<CommunicationException> missingException(String parameter) {
        return () -> new CommunicationException("There is a missing parameter in mongoDb authentication: " + parameter);
    }


}
