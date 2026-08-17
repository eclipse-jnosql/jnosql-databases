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
package org.eclipse.jnosql.databases.mongodb.communication;

import com.mongodb.AuthenticationMechanism;
import com.mongodb.MongoCredential;
import org.eclipse.jnosql.communication.CommunicationException;
import org.eclipse.jnosql.communication.Settings;
import org.junit.jupiter.api.Test;

import static com.mongodb.AuthenticationMechanism.GSSAPI;
import static com.mongodb.AuthenticationMechanism.SCRAM_SHA_1;
import static com.mongodb.AuthenticationMechanism.SCRAM_SHA_256;
import static org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentConfigurations.AUTHENTICATION_MECHANISM;
import static org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentConfigurations.AUTHENTICATION_SOURCE;
import static org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentConfigurations.PASSWORD;
import static org.eclipse.jnosql.databases.mongodb.communication.MongoDBDocumentConfigurations.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class MongoAuthenticationTest {

    @Test
    void shouldReturnErrorWhenTheNumberParameterIsInvalid() {
        Settings settings = Settings.builder().put(USER, "value")
                .build();

        assertThatExceptionOfType(CommunicationException.class).isThrownBy(() -> MongoAuthentication.of(settings));

    }
    @Test
    void shouldReturnOneAuthentication() {
        Settings settings = Settings.builder()
                .put(AUTHENTICATION_SOURCE, "database")
                .put(PASSWORD, "password")
                .put(USER, "user")
                .build();

        MongoCredential credential = MongoAuthentication.of(settings).get();
        assertThat(credential.getSource()).isEqualTo("database");
        assertThat(credential.getPassword()).isEqualTo("password".toCharArray());
        assertThat(credential.getUserName()).isEqualTo("user");
        assertThat(credential.getMechanism()).isNull();

    }

    @Test
    void shouldReturnOneAuthenticationWithGSSAPI() {
        Settings settings = Settings.builder()
                .put(AUTHENTICATION_SOURCE, "database")
                .put(PASSWORD, "password")
                .put(USER, "user")
                .put(AUTHENTICATION_MECHANISM, "GSSAPI")
                .build();

        MongoCredential credential = MongoAuthentication.of(settings).get();
        assertThat(credential.getSource()).isEqualTo("$external");
        assertThat(credential.getUserName()).isEqualTo("user");
        assertThat(credential.getMechanism()).isEqualTo(GSSAPI.getMechanismName());

    }

    @Test
    void shouldReturnOneAuthenticationWithMongoX509() {
        Settings settings = Settings.builder()
                .put(AUTHENTICATION_SOURCE, "database")
                .put(PASSWORD, "password")
                .put(USER, "user")
                .put(AUTHENTICATION_MECHANISM, "MONGODB-X509")
                .build();

        MongoCredential credential = MongoAuthentication.of(settings).get();
        assertThat(credential.getSource()).isEqualTo("$external");
        assertThat(credential.getUserName()).isEqualTo("user");
        assertThat(credential.getMechanism()).isEqualTo(AuthenticationMechanism.MONGODB_X509.getMechanismName());
    }

    @Test
    void shouldReturnOneAuthenticationWithSCRAMSHA1() {
        Settings settings = Settings.builder()
                .put(AUTHENTICATION_SOURCE, "database")
                .put(PASSWORD, "password")
                .put(USER, "user")
                .put(AUTHENTICATION_MECHANISM, "SCRAM-SHA-1")
                .build();

        MongoCredential credential = MongoAuthentication.of(settings).get();
        assertThat(credential.getSource()).isEqualTo("database");
        assertThat(credential.getPassword()).isEqualTo("password".toCharArray());
        assertThat(credential.getUserName()).isEqualTo("user");
        assertThat(credential.getMechanism()).isEqualTo(SCRAM_SHA_1.getMechanismName());
    }

    @Test
    void shouldReturnOneAuthenticationWithSCRAMSHA256() {
        Settings settings = Settings.builder()
                .put(AUTHENTICATION_SOURCE, "database")
                .put(PASSWORD, "password")
                .put(USER, "user")
                .put(AUTHENTICATION_MECHANISM, "SCRAM-SHA-256")
                .build();

        MongoCredential credential = MongoAuthentication.of(settings).get();
        assertThat(credential.getSource()).isEqualTo("database");
        assertThat(credential.getPassword()).isEqualTo("password".toCharArray());
        assertThat(credential.getUserName()).isEqualTo("user");
        assertThat(credential.getMechanism()).isEqualTo(SCRAM_SHA_256.getMechanismName());
    }

}