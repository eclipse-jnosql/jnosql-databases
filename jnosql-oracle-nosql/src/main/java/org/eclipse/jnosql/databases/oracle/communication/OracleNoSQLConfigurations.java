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

import java.util.function.Supplier;

/**
 * An enumeration representing the available configuration options for connecting to the Oracle NoSQL database.
 * Each option implements the {@link java.util.function.Supplier} interface, which returns the corresponding property name.
 * These properties can be overwritten by the system environment using Eclipse Microprofile or Jakarta Config API.
 *
 * @see org.eclipse.jnosql.communication.Settings
 */
public enum OracleNoSQLConfigurations implements Supplier<String> {

    /**
     * Specifies the hostname or IP address of the Oracle NoSQL database server.
     * Default value: <b><a href="http://localhost:8080">http://localhost:8080</a></b>
     */
    HOST("jnosql.oracle.nosql.host"),

    /**
     * Specifies the username used to authenticate with the Oracle NoSQL database.
     */
    USER("jnosql.oracle.nosql.user"),

    /**
     * Specifies the password used to authenticate with the Oracle NoSQL database.
     */
    PASSWORD("jnosql.oracle.nosql.password"),

    /**
     * Specifies the desired throughput of read operations when creating a table using Eclipse JNoSQL.
     * A read unit represents 1 eventually consistent read per second for data up to 1 KB in size.
     * Double the read units are consumed for absolutely consistent reads.
     * Default value: <b>25</b>
     */
    TABLE_READ_LIMITS("jnosql.oracle.nosql.table.read.limit"),

    /**
     * Specifies the desired throughput of write operations when creating a table using Eclipse JNoSQL.
     * A write unit represents 1 write per second of data up to 1 KB in size.
     * Default value: <b>25</b>
     */
    TABLE_WRITE_LIMITS("jnosql.oracle.nosql.table.write.limit"),

    /**
     * Specifies the maximum storage, in gigabytes, to be consumed by a table created using Eclipse JNoSQL.
     * Default value: <b>25</b>
     */
    TABLE_STORAGE_GB("jnosql.oracle.nosql.table.storage.gb"),

    /**
     * Specifies the total amount of time to wait, in milliseconds, when creating a table.
     * This value must be non-zero and greater than the delayMillis.
     */
    TABLE_WAIT_MILLIS("jnosql.oracle.nosql.table.wait.millis"),

    /**
     * Specifies the amount of time to wait, in milliseconds, between polling attempts when creating a table.
     * If set to 0, it defaults to <b>500</b> milliseconds.
     */
    TABLE_DELAY_MILLIS("jnosql.oracle.nosql.table.delay.millis"),

    /**
     * Specifies the deployment type for Oracle NoSQL database.
     * Default value: {@link DeploymentType#ON_PREMISES}
     *
     */
    DEPLOYMENT("jnosql.oracle.nosql.deployment"),

    /**
     * Specifies the Oracle NoSQL database tenant ID for Cloud deployment.
     * Relevant for Cloud deployment only.
     */
    TENANT("jnosql.oracle.nosql.tenant.id"),

    /**
     * Specifies the fingerprint for authentication with Oracle NoSQL database for Cloud deployment.
     * Relevant for Cloud deployment only.
     */
    FINGERPRINT("jnosql.oracle.nosql.fingerprint"),

    /**
     * Specifies the private key for authentication with Oracle NoSQL database for Cloud deployment.
     * Relevant for Cloud deployment only.
     */
    PRIVATE_KEY("jnosql.oracle.nosql.private.key"),

    /**
     * Define the compartment name in Oracle Cloud Infrastructure.
     */
    COMPARTMENT("jnosql.oracle.nosql.compartment"),

    /**
     * Define the compartment name in Oracle NoSQL on-premise.
     */
    NAMESPACE("jnosql.oracle.nosql.namespace"),

    /**
     * Define the profile name used to load session token in Oracle NoSQL cloud.
     */
    PROFILE_NAME("jnosql.oracle.nosql.profile.name"),
    /**
     * Define the path of configuration file used to load session token in Oracle NoSQL cloud.
     */
    CONFIG_FILE("jnosql.oracle.nosql.config.file");

    private final String configuration;

    OracleNoSQLConfigurations(String configuration) {
        this.configuration = configuration;
    }

    @Override
    public String get() {
        return configuration;
    }
}
