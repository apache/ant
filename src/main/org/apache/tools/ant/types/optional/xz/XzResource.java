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
package org.apache.tools.ant.types.optional.xz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.CompressedResource;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZInputStream;
import org.tukaani.xz.XZOutputStream;

/**
 * A XZ compressed resource.
 *
 * <p>Wraps around another resource, delegates all queries to that
 * other resource but uncompresses/compresses streams on the fly.</p>
 *
 * @since Ant 1.10.1
 */
public class XzResource extends CompressedResource {

    /** A no-arg constructor */
    public XzResource() {
    }

    /**
     * Constructor with another resource to wrap.
     * @param other the resource to wrap.
     */
    public XzResource(ResourceCollection other) {
        super(other);
    }

    /**
     * Decompress on the fly using java.util.zip.XZInputStream.
     * @param in the stream to wrap.
     * @return the wrapped stream.
     * @throws IOException if there is a problem.
     */
    @Override
    protected InputStream wrapStream(InputStream in) throws IOException {
        return new XZInputStream(in);
    }

    /**
     * Compress on the fly using java.util.zip.XZOutStream.
     * @param out the stream to wrap.
     * @return the wrapped stream.
     * @throws IOException if there is a problem.
     */
     @Override
    protected OutputStream wrapStream(OutputStream out) throws IOException {
        return new XZOutputStream(out, new LZMA2Options());
    }

    /**
     * Get the name of the compression method.
     * @return the string "XZ".
     */
    @Override
    protected String getCompressionName() {
        return "XZ";
    }
}
