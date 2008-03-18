/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Ant", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.taskdefs.optional.rjunit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

/**
 * Attempt to create an output specific attribute.
 * <p>
 * The possible output values are 'stdout' and 'stderr', otherwise
 * it is assumed that the value represent a file.
 * </p>
 * Note that stdout and stderr are wrapped by a <tt>KeepAliveOutputStream</tt>
 * so that the stream cannot be closed.
 *
 * @see KeepAliveOutputStream
 */
public class OutputAttribute extends ProjectComponent {

    /** keyword to represent stdout output */
    public final static String STDOUT = "stdout";

    /** keyword to represent stderr output */
    public final static String STDERR = "stderr";

    /** the selected value for output, either stdout,stderr or filepath */
    private String value;

    /**
     * Create a new output attribute from a value.
     */
    public OutputAttribute(String value) {
        this.value = value;
    }

    /**
     * @return the outputstream corresponding to the selected attribute.
     */
    public OutputStream getOutputStream() {
        if (STDOUT.equals(value)) {
            return new KeepAliveOutputStream(System.out);
        } else if (STDERR.equals(value)) {
            return new KeepAliveOutputStream(System.err);
        }
        File f = getProject().resolveFile(value);
        try {
            return new FileOutputStream(f);
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

}
