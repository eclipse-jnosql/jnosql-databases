/*
 *
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
 *
 */
package org.eclipse.jnosql.databases.couchdb.communication.configuration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.Arrays;
import java.util.List;

public class CouchDBContainer extends GenericContainer {

	public static final String IMAGE = "couchdb";
	public static final String DEFAULT_TAG = "latest";
	public static final Integer PORT = 5984;

	public CouchDBContainer() {
		super(IMAGE + ":" + DEFAULT_TAG);
	}

	@Override
	protected void configure() {
		setExposedPorts(List.of(PORT));
		setWaitStrategy(getCouchDBWaitStrategy());
		setEnv(Arrays.asList("COUCHDB_USER=admin", "COUCHDB_PASSWORD=password"));
	}

	private WaitStrategy getCouchDBWaitStrategy() {
		return new WaitAllStrategy()
			.withStrategy(new HttpWaitStrategy()
				.forPort(PORT)
				.forPath("/")
				.forStatusCode(200));
	}

}
