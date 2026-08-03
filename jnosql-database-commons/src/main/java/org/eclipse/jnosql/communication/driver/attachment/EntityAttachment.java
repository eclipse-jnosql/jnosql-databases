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
 *   Jesse Gallagher
 */
package org.eclipse.jnosql.communication.driver.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a binary attachment attached to a JNoSQL entity.
 * @since 0.0.9
 */
public interface EntityAttachment {
    /**
     * Returns the file name of the attachment.
     *
     * @return the file name of the attachment
     */
    String name();
    /**
     * Returns the last modification date of the attachment.
     *
     * @return the last modification date of the attachment, in ms since the epoch
     */
    long getLastModified();
    /**
     * Returns the MIME type of the content.
     *
     * @return the MIME type of the content
     */
    String getContentType();
    /**
     * Returns a stream for reading the attachment data.
     *
     * @return an {@link InputStream} representing the data of the attachment
     * @throws IOException if there is an I/O problem retrieving the attachment content
     */
    InputStream getData() throws IOException;
    /**
     * Returns the size of the content.
     *
     * @return the size of the content in bytes
     */
    long getLength();
    
    /**
     * Returns an ETag value for the current version of the content.
     *
     * @return an ETag value for the current version of the content
     */
    default String getETag() {
        return name() + "-" + Long.toString(getLastModified(), 16); //$NON-NLS-1$
    }
    
    /**
     * Creates a new in-memory {@link EntityAttachment} for the provided information
     * 
     * @param name the name of the attachment
     * @param lastModified the last modification date, in ms since the epoch
     * @param contentType the MIME type of the content
     * @param data the data if the attachment
     * @return a new {@link EntityAttachment}
     */
    static EntityAttachment of(String name, long lastModified, String contentType, byte[] data) {
        return new ByteArrayEntityAttachment(name, contentType, lastModified, data);
    }
    
    /**
     * Creates a new {@link EntityAttachment} to represent the provided file {@link Path}
     * 
     * @param path a {@link Path} representing a readable file on disk
     * @return a new {@link EntityAttachment}
     */
    static EntityAttachment of(Path path) {
        return new PathEntityAttachment(path);
    }
    
    /**
     * Creates a new {@link EntityAttachment} to represent the provided {@link File}
     * 
     * @param file a {@link File} representing a readable file on disk
     * @return a new {@link EntityAttachment}
     */
    static EntityAttachment of(File file) {
        Objects.requireNonNull(file, "file cannot be null");
        return of(file.toPath());
    }
}
