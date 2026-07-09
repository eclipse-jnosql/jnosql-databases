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
 *   Alessandro Moscatelli
 */
package org.eclipse.jnosql.databases.elasticsearch.communication;


import org.apache.hc.core5.http.HttpHost;

import java.net.MalformedURLException;
import java.net.URL;

final class ElasticsearchAddress {

    private final HttpHost host;

    private ElasticsearchAddress(String address, int defaultPort) {
        HttpHost parsedHost;

        try {
            URL url = new URL(address);
            int port = url.getPort() == -1 ? defaultPort : url.getPort();

            parsedHost = new HttpHost(url.getProtocol(), url.getHost(), port);
        } catch (MalformedURLException ex) {
            String[] values = address.split(":");
            int port = values.length == 2 ? Integer.parseInt(values[1]) : defaultPort;
            parsedHost = new HttpHost(values[0], port);
        }

        this.host = parsedHost;
    }

    public HttpHost toHttpHost() {
        return this.host;
    }

    @Override
    public String toString() {
        return "ElasticsearchAddress{" +
                "host='" + host.getHostName() + '\'' +
                ", port=" + host.getPort() +
                '}';
    }

    static ElasticsearchAddress of(String address, int defaultPort) {
        return new ElasticsearchAddress(address, defaultPort);
    }
}