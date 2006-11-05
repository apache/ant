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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FilterOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.Reference;

/**
 * Exposes a string as a Resource.
 * @since Ant 1.7
 */
public class StringResource extends Resource {

    /** Magic number */
    private static final int STRING_MAGIC
        = Resource.getMagicNumber("StringResource".getBytes());

    private String encoding = null;

    /**
     * Default constructor.
     */
    public StringResource() {
    }

    /**
     * Construct a StringResource with the supplied value.
     * @param value the value of this StringResource.
     */
    public StringResource(String value) {
        setValue(value);
    }

    /**
     * Enforce String immutability.
     * @param s the new name/value for this StringResource.
     */
    public synchronized void setName(String s) {
        if (getName() != null) {
            throw new BuildException(new ImmutableResourceException());
        }
        super.setName(s);
    }

    /**
     * The value attribute is a semantically superior alias for the name attribute.
     * @param s the String's value.
     */
    public synchronized void setValue(String s) {
        setName(s);
    }

    /**
     * Synchronize access.
     * @return the name/value of this StringResource.
     */
    public synchronized String getName() {
        return super.getName();
    }

    /**
     * Get the value of this StringResource.
     * @return the represented String.
     */
    public synchronized String getValue() {
        return getName();
    }

    /**
     * Set the encoding to be used for this StringResource.
     * @param s the encoding name.
     */
    public synchronized void setEncoding(String s) {
        encoding = s;
    }

    /**
     * Get the encoding used by this StringResource.
     * @return the encoding name.
     */
    public synchronized String getEncoding() {
        return encoding;
    }

    /**
     * Get the size of this Resource.
     * @return the size, as a long, 0 if the Resource does not exist (for
     *         compatibility with java.io.File), or UNKNOWN_SIZE if not known.
     */
    public synchronized long getSize() {
        return isReference()
            ? ((Resource) getCheckedRef()).getSize()
            : (long) getContent().length();
    }

    /**
     * Get the hash code for this Resource.
     * @return hash code as int.
     */
    public synchronized int hashCode() {
        if (isReference()) {
            return getCheckedRef().hashCode();
        }
        return super.hashCode() * STRING_MAGIC;
    }

    /**
     * Get the string.
     *
     * @return the string contents of the resource.
     * @since Ant 1.7
     */
    public String toString() {
        if (isReference()) {
            return getCheckedRef().toString();
        }
        return String.valueOf(getContent());
    }

    /**
     * Get an InputStream for the Resource.
     * @return an InputStream containing this Resource's content.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if InputStreams are not
     *         supported for this Resource type.
     */
    public synchronized InputStream getInputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getInputStream();
        }
        //I can't get my head around this; is encoding treatment needed here?
        return
            //new oata.util.ReaderInputStream(new InputStreamReader(
            new ByteArrayInputStream(getContent().getBytes());
            //, encoding), encoding);
    }

    /**
     * Get an OutputStream for the Resource.
     * @return an OutputStream to which content can be written.
     * @throws IOException if unable to provide the content of this
     *         Resource as a stream.
     * @throws UnsupportedOperationException if OutputStreams are not
     *         supported for this Resource type.
     */
    public synchronized OutputStream getOutputStream() throws IOException {
        if (isReference()) {
            return ((Resource) getCheckedRef()).getOutputStream();
        }
        if (getValue() != null) {
            throw new ImmutableResourceException();
        }
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        return new FilterOutputStream(baos) {
            public void close() throws IOException {
                super.close();
                StringResource.this.setValue(encoding == null
                    ? baos.toString() : baos.toString(encoding));
            }
        };
    }

    /**
     * Overrides the super version.
     * @param r the Reference to set.
     */
    public void setRefid(Reference r) {
        if (encoding != null) {
            throw tooManyAttributes();
        }
        super.setRefid(r);
    }

    /**
     * Get the content of this StringResource.
     * @return a String; if the Project has been set properties
     *         replacement will be attempted.
     */
    protected synchronized String getContent() {
        if (isReference()) {
            return ((StringResource) getCheckedRef()).getContent();
        }
        String value = getValue();
        if (value == null) {
            return value;
        }
        return getProject() == null
            ? value : getProject().replaceProperties(value);
    }

}
