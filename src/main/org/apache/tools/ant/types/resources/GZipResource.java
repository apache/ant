/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.tools.ant.types.resources;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A GZip compressed resource.
 *
 * <p>Wraps around another resource, delegates all quries to that
 * other resource but uncompresses/compresses streams on the fly.</p>
 *
 * @since Ant 1.7
 */
public class GZipResource extends CompressedResource {

    public GZipResource() {
    }

    public GZipResource(org.apache.tools.ant.types.ResourceCollection other) {
        super(other);
    }

    protected InputStream wrapStream(InputStream in) throws IOException {
        return new GZIPInputStream(in);
    }
    protected OutputStream wrapStream(OutputStream out) throws IOException {
        return new GZIPOutputStream(out);
    }
    protected String getCompressionName() {
        return "GZip";
    }
}