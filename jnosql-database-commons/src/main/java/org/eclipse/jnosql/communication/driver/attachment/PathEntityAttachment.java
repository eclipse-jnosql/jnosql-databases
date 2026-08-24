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

import org.eclipse.jnosql.communication.CommunicationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Default representation of {@link EntityAttachment} for a filesystem {@link Path}.
 * 
 * @since 0.0.9
 */
public class PathEntityAttachment implements EntityAttachment {
    private final Path path;
    
    public PathEntityAttachment(Path path) {
        Objects.requireNonNull(path, "path cannot be null");
        if(!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Path is not a readable file: " + path);
        }
        
        this.path = path;
    }

    @Override
    public String name() {
        return path.getFileName().toString();
    }

    @Override
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContentType() {
        try {
            return Files.probeContentType(path);
        } catch(IOException e) {
            throw new CommunicationException("There is an error to load the content type", e);
        }
    }

    @Override
    public InputStream getData() throws IOException {
        return Files.newInputStream(path);
    }

    @Override
    public long getLength() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
