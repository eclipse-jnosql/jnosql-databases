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
 *   Jesse Gallagher
 */
package org.eclipse.jnosql.communication.driver.attachment;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Default representation of {@link EntityAttachment} for in-memory data.
 * 
 * @since 0.0.9
 */
public class ByteArrayEntityAttachment implements EntityAttachment {
    private final String name;
    private final String contentType;
    private final long lastModified;
    private final byte[] data;

    /**
     * Creates an in-memory attachment.
     *
     * @param name the attachment name
     * @param contentType the attachment content type
     * @param lastModified the last modification date, in ms since the epoch
     * @param data the attachment data
     */
    public ByteArrayEntityAttachment(String name, String contentType, long lastModified, byte[] data) {
        this.name = name;
        this.contentType = contentType;
        this.lastModified = lastModified;
        this.data = data != null ? Arrays.copyOf(data, data.length) : new byte[0];

    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public InputStream getData() {
        return new ByteArrayInputStream(data);
    }
    
    @Override
    public long getLength() {
        return data.length;
    }

}
