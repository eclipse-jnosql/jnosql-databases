/*
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
 */
package org.eclipse.jnosql.databases.tinkerpop.communication;


/**
 * The like to regex converter
 */
enum LikeToRegex {
    INSTANCE;


    /**
     * Converts like pattern to regex pattern.
     *
     * @param text the like pattern to convert
     * @return the regex pattern
     */
    String likeToRegex(Object text) {
        String like = text == null ? null : text.toString();
        if (like == null) {
            return "(?!)";
        }
        StringBuilder rx = new StringBuilder("^");
        StringBuilder lit = new StringBuilder();
        for (int i = 0; i < like.length(); i++) {
            char c = like.charAt(i);
            if (c == '%' || c == '_') {
                if (!lit.isEmpty()) {
                    rx.append(java.util.regex.Pattern.quote(lit.toString()));
                    lit.setLength(0);
                }
                rx.append(c == '%' ? ".*" : ".");
            } else {
                lit.append(c);
            }
        }
        if (!lit.isEmpty()) {
            rx.append(java.util.regex.Pattern.quote(lit.toString()));
        }
        rx.append('$');
        return rx.toString();
    }

}