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
package org.apache.ant.init;
import java.net.MalformedURLException;

import java.net.URL;

/**
 * The ClassLocator is a utility class which is used to determine the URL
 * from which a class was loaded.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 9 January 2002
 */
public class ClassLocator {
    /**
     * Get the URL for the given class's load location.
     *
     * @param theClass the class whose loadURL is desired.
     * @return a URL which identifies the component from which this class
     *      was loaded.
     * @throws MalformedURLException if the class' URL cannot be
     *      constructed.
     */
    public static URL getClassLocationURL(Class theClass)
         throws MalformedURLException {
        String className = theClass.getName().replace('.', '/') + ".class";
        URL classRawURL = theClass.getClassLoader().getResource(className);

        String fileComponent = classRawURL.getFile();
        if (classRawURL.getProtocol().equals("file")) {
            // Class comes from a directory of class files rather than
            // from a jar.
            int classFileIndex = fileComponent.lastIndexOf(className);
            if (classFileIndex != -1) {
                fileComponent = fileComponent.substring(0, classFileIndex);
            }

            return new URL("file:" + fileComponent);
        } else if (classRawURL.getProtocol().equals("jar")) {
            // Class is coming from a jar. The file component of the URL
            // is actually the URL of the jar file
            int classSeparatorIndex = fileComponent.lastIndexOf("!");
            if (classSeparatorIndex != -1) {
                fileComponent = fileComponent.substring(0, classSeparatorIndex);
            }

            return new URL(fileComponent);
        } else {
            // its running out of something besides a jar.
            // We just return the Raw URL as a best guess
            return classRawURL;
        }
    }
}

