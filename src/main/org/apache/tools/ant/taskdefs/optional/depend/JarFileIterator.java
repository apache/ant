/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000 The Apache Software Foundation.  All rights 
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
package org.apache.tools.ant.taskdefs.optional.depend;

import java.util.zip.*;
import java.io.*;

/**
 * A class file iterator which iterates through the contents of a Java jar file.
 * 
 * @author Conor MacNeill
 */
public class JarFileIterator implements ClassFileIterator {
    private ZipInputStream jarStream;

    public JarFileIterator(InputStream stream) throws IOException {
        super();

        jarStream = new ZipInputStream(stream);
    }

    private byte[] getEntryBytes(InputStream stream) throws IOException {
        byte[]                buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int                   n;

        while ((n = stream.read(buffer, 0, buffer.length)) != -1) {
            baos.write(buffer, 0, n);
        } 

        return baos.toByteArray();
    } 

    public ClassFile getNextClassFile() {
        ZipEntry         jarEntry;
        ClassFile nextElement = null;

        try {
            jarEntry = jarStream.getNextEntry();


            while (nextElement == null && jarEntry != null) {
                String entryName = jarEntry.getName();

                if (!jarEntry.isDirectory() && entryName.endsWith(".class")) {

                        // create a data input stream from the jar input stream
                        ClassFile javaClass = new ClassFile();

                        javaClass.read(jarStream);

                        nextElement = javaClass;
                } else {
                        
                    jarEntry = jarStream.getNextEntry();
                } 
            } 
        } catch (IOException e) {
            String message = e.getMessage();
            String text = e.getClass().getName();

            if (message != null) {
                text += ": " + message;
            } 

            throw new RuntimeException("Problem reading JAR file: " + text);
        } 

        return nextElement;
    } 

}

