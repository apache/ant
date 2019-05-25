/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.types.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 * A Bzip2 compressed resource.
 *
 * <p>Wraps around another resource, delegates all queries to that
 * other resource but uncompresses/compresses streams on the fly.</p>
 *
 * @since Ant 1.7
 */
public class BZip2Resource extends CompressedResource {
    private static final char[] MAGIC = new char[] {'B', 'Z'};

    /** A no-arg constructor */
    public BZip2Resource() {
    }

    /**
     * Constructor with another resource to wrap.
     * @param other the resource to wrap.
     */
    public BZip2Resource(org.apache.tools.ant.types.ResourceCollection other) {
        super(other);
    }

    /**
     * Decompress on the fly using {@link CBZip2InputStream}.
     * @param in the stream to wrap.
     * @return the wrapped stream.
     * @throws IOException if there is a problem.
     */
    @Override
    protected InputStream wrapStream(InputStream in) throws IOException {
        for (char ch : MAGIC) {
            if (in.read() != ch) {
                throw new IOException("Invalid bz2 stream.");
            }
        }
        return new CBZip2InputStream(in);
    }

    /**
     * Compress on the fly using {@link CBZip2OutputStream}.
     * @param out the stream to wrap.
     * @return the wrapped stream.
     * @throws IOException if there is a problem.
     */
    @Override
    protected OutputStream wrapStream(OutputStream out) throws IOException {
        for (char ch : MAGIC) {
            out.write(ch);
        }
        return new CBZip2OutputStream(out);
    }

    /**
     * Get the name of the compression method.
     * @return the string "Bzip2".
     */
    @Override
    protected String getCompressionName() {
        return "Bzip2";
    }
}
