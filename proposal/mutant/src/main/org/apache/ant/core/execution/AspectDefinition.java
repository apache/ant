/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

package org.apache.ant.core.execution;

import java.util.*;
import java.net.URL;

/**
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 */ 
public class AspectDefinition {
    /** The URL of the library which defines this aspect handler */
    private URL aspectLibraryURL;
     
    /** The aspect's tag */
    private String aspectPrefix;
    
    /** The aspect handler's classname */
    private String aspectClassName;
    
    /** The aspect handler's class loader. */
    private ClassLoader aspectClassLoader;
    
    /** The class to which this converter converts. */
    private Class aspectHandlerClass = null;

    public AspectDefinition(URL aspectLibraryURL, String aspectPrefix, 
                            String aspectClassName, ClassLoader aspectClassLoader) {
        this.aspectLibraryURL = aspectLibraryURL;
        this.aspectPrefix = aspectPrefix;
        this.aspectClassName = aspectClassName;
        this.aspectClassLoader = aspectClassLoader;
    }
    
    /**
     * Get the URL where this aspect handler was defined.
     *
     * @returns a URL of the lib defintion file
     */
    public URL getLibraryURL() {
        return aspectLibraryURL;
    }
    
    /**
     * Get the Aspect's Prefix
     */
    public String getAspectPrefix() {
        return aspectPrefix;
    }


    /**
     * Get the aspect handler class
     *
     * @return a class object for this aspect handler's class
     */
    public synchronized Class getAspectHandlerClass() throws ClassNotFoundException {
        if (aspectHandlerClass == null) {
            aspectHandlerClass = Class.forName(aspectClassName, true, aspectClassLoader);
        }
        return aspectHandlerClass;
    }
    
    /**
     * Get the classname of the aspect handler that is being defined.
     */
    public String getAspectHandlerClassName() {
        return aspectClassName;
    }
    
}

