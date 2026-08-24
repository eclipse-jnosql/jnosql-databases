/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.databases.oracle.communication;

import oracle.nosql.driver.AuthorizationProvider;
import oracle.nosql.driver.iam.SignatureProvider;
import oracle.nosql.driver.kv.StoreAccessTokenProvider;
import org.eclipse.jnosql.communication.Configurations;
import org.eclipse.jnosql.communication.Settings;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The {@code DeploymentType} enum represents different deployment options for software solutions.
 * It is used to categorize deployments as either "On-premises" or "Cloud."
 */
public enum DeploymentType implements Function<Settings, Optional<AuthorizationProvider>> {
    /**
     * Represents an "On-premises" deployment where software solutions are deployed and managed
     * within an organization's physical premises or data centers.
     * Usage: Choose this deployment type when your software solutions are hosted and managed within your organization's physical infrastructure.
     * - Authentication: Uses username and password for access.
     * - Configuration: Requires USER (username) and PASSWORD (password).
     * If not provided, it falls back to default values or empty credentials.
     */
    ON_PREMISES {
        @Override
        public Optional<AuthorizationProvider> apply(Settings settings) {
            String user = settings.get(List.of(OracleNoSQLConfigurations.USER.get(), Configurations.USER.get()))
                    .map(Object::toString).orElse(null);
            String password = settings.get(List.of(OracleNoSQLConfigurations.PASSWORD.get(), Configurations.PASSWORD.get()))
                    .map(Object::toString).orElse(null);
            if (user != null && password != null) {
                return Optional.of(new StoreAccessTokenProvider(user, password.toCharArray()));
            } else {
                return Optional.of(new StoreAccessTokenProvider());
            }

        }
    },

    /**
     * Represents a "Cloud" deployment using API key for authentication and authorization.
     */
    CLOUD_API_KEY {
        @Override
        public Optional<AuthorizationProvider> apply(Settings settings) {

            String user = settings.get(List.of(OracleNoSQLConfigurations.USER.get(), Configurations.USER.get()))
                    .map(Object::toString).orElse(null);
            char[] password = settings.get(List.of(OracleNoSQLConfigurations.PASSWORD.get(), Configurations.PASSWORD.get()))
                    .map(Object::toString).map(String::toCharArray).orElse(new char[0]);
            String tenantId = settings.get(OracleNoSQLConfigurations.TENANT, String.class).orElse(null);
            String fingerprint = settings.get(OracleNoSQLConfigurations.FINGERPRINT, String.class).orElse(null);
            String privateKey = settings.get(OracleNoSQLConfigurations.PRIVATE_KEY, String.class).orElse(null);
            String profileName = settings.get(OracleNoSQLConfigurations.PROFILE_NAME, String.class).orElse(null);
            String configFile = settings.get(OracleNoSQLConfigurations.CONFIG_FILE, String.class).orElse(null);

            if (user != null && tenantId != null && fingerprint != null && privateKey != null) {
                return Optional.of(new SignatureProvider(tenantId, user, fingerprint, new File(privateKey), password));
            }
            try {
                if (profileName != null && configFile != null) {
                    return Optional.of(new SignatureProvider(configFile, profileName));
                } else if (profileName != null) {
                    return Optional.of(new SignatureProvider(profileName));
                }
                return Optional.of(new SignatureProvider());
            } catch (IOException e) {
                throw new OracleNoSQLException("Error to load configuration to Oracle Cloud at Oracle NoSQL", e);
            }

        }

    },
    /**
     * Represents a "Cloud" deployment using resource principal for authentication and authorization.
     */
    CLOUD_INSTANCE_PRINCIPAL {
        @Override
        public Optional<AuthorizationProvider> apply(Settings settings) {
            return Optional.of(SignatureProvider.createWithInstancePrincipal());
        }
    },
    /**
     * Represents a "Cloud" deployment using resource principal for authentication and authorization.
     */
    CLOUD_RESOURCE_PRINCIPAL {
        @Override
        public Optional<AuthorizationProvider> apply(Settings settings) {
            return Optional.of(SignatureProvider.createWithResourcePrincipal());
        }
    },
    /**
     * Represents a "Cloud" deployment using instance principal for delegation with an OBO token.
     */
    CLOUD_INSTANCE_OBO_USER {
        @Override
        public Optional<AuthorizationProvider> apply(Settings settings) {
            return Optional.of(SignatureProvider.createWithInstancePrincipalForDelegation(new File(System.getenv("OCI_obo_token_path"))));
        }
    },
    /**
     * Represents a "Cloud" deployment using resource principal for delegation with an OBO token.
     */
    CLOUD_SECURITY_TOKEN {
        @Override
        public Optional<AuthorizationProvider> apply(Settings settings) {

            String profileName = settings.get(OracleNoSQLConfigurations.PROFILE_NAME, String.class).orElse(null);
            String configFile = settings.get(OracleNoSQLConfigurations.CONFIG_FILE, String.class).orElse(null);
            if (profileName != null && configFile != null) {
                return Optional.of(SignatureProvider.createWithSessionToken(configFile, profileName));
            } else if (profileName != null) {
                return Optional.of(SignatureProvider.createWithSessionToken(profileName));
            } else {
                return Optional.of(SignatureProvider.createWithSessionToken());
            }
        }
    },

    /**
     * Represents a "Cloud" deployment using OKE workload identity for authentication and authorization.
     */
    CLOUD_OKE_WORKLOAD_IDENTITY {
        @Override
        public Optional<AuthorizationProvider> apply(Settings settings) {
            return Optional.of(SignatureProvider.createWithOkeWorkloadIdentity());
        }
    };


    /**
     * Parses a string representation of a deployment type into the {@link DeploymentType} enum,
     * with a default value of {@link DeploymentType#ON_PREMISES} for invalid or null input.
     *
     * @param value the string representation of the deployment type (case-insensitive)
     * @return the corresponding {@link DeploymentType} enum value, or {@link DeploymentType#ON_PREMISES}
     * if the input is invalid or null
     */
    public static DeploymentType parse(String value) {
        try {
            if (value == null) {
                return DeploymentType.ON_PREMISES;
            }
            return DeploymentType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return DeploymentType.ON_PREMISES;
        }
    }

}
