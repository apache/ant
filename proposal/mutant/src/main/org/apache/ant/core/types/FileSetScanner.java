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

package org.apache.ant.core.types;

import java.io.*;
import java.net.URL;
import org.apache.ant.core.execution.*;

/**
 * The FileSetInfo interface defines the result of applying filtering to
 * some base collection of files. Filtering involves both file exclusion 
 * and file name mapping.
 *
 * FileSetInfo should be lazily evaluated to allow them to be defined before the
 * required files have been created. They should be evaluated at first use.
 */
public interface FileSetScanner {
    /**
     * Get the included files after their file names have been mapped
     *
     * @return an array of strings, each one is the mapped name of a file.
     */
    String[] getIncludedFiles() throws ExecutionException ;
//    
//    /**
//     * Get directories included after their file names have been mapped
//     *
//     * @return an array of strings, each one is the mapped name of a file.
//     */
//    String[] getIncludedDirectories();
//
//    /**
//     * Get a file for the content of the named included file. If the content
//     * is not stored in the local filesystem, a temporary file is created with the content. 
//     * Callers should not rely on this file representing the actual location of the underlying
//     * data.
//     */
//    File getContentFile(String mappedName);
//    
//    /**
//     * Get a URL for the content. The content may be cached on the local system and thus
//     * callers should not rely on the location
//     *
//     */
//    URL getContentURL(String mappedName);
//    
//    /**
//     * Get an input stream to the content of the named entry of the fileset.
//     */
//    InputStream getInputStream(String mappedName);
//    
    /**
     * Get a local file.
     *
     * This method returns a file pointing to the actual local filesystem file from 
     * which the file content comes. If the file does not exist locally, a null is 
     * returned. Note that due to name mapping, the actual file name may be different
     * from the mapped name.
     *
     * @return a file representing the mapped file in the local filesystem.
     */
    File getLocalFile(String mappedName) throws ExecutionException ;
}
