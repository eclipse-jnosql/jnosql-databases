/*
 *
 *  Copyright (c) 2025 Contributors to the Eclipse Foundation
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
 *
 */
package org.eclipse.jnosql.databases.neo4j.communication;

import java.util.regex.Pattern;

enum LikeToCypherRegex {
    INSTANCE;

    public String toCypherRegex(String like) {
        if (like == null) {
            return "(?!)";
        }
        StringBuilder regex = new StringBuilder(like.length() + 8);
        StringBuilder lit = new StringBuilder();

        regex.append('^');
        for (int i = 0; i < like.length(); i++) {
            char c = like.charAt(i);
            if (c == '%' || c == '_') {
                if (!lit.isEmpty()) {
                    regex.append(Pattern.quote(lit.toString()));
                    lit.setLength(0);
                }
                regex.append(c == '%' ? ".*" : ".");
            } else {
                lit.append(c);
            }
        }
        if (!lit.isEmpty()) {
            regex.append(Pattern.quote(lit.toString()));
        }
        regex.append('$');
        return regex.toString();
    }
}