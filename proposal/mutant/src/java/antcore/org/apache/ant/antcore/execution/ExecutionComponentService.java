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
package org.apache.ant.antcore.execution;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.ant.antcore.antlib.AntLibManager;
import org.apache.ant.antcore.util.ConfigException;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.ExecutionException;

/**
 * The instance of the ComponentServices made available by the core to the
 * ant libraries.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 27 January 2002
 */
public class ExecutionComponentService implements ComponentService {
    /** The ExecutionFrame this service instance is working for */
    private ExecutionFrame frame;

    /** The library manager instance used to configure libraries. */
    private AntLibManager libManager;

    /**
     * Constructor
     *
     * @param executionFrame the frame containing this context
     * @param allowRemoteLibs true if remote libraries can be loaded though
     *      this service.
     */
    public ExecutionComponentService(ExecutionFrame executionFrame,
                                     boolean allowRemoteLibs) {
        this.frame = executionFrame;
        libManager = new AntLibManager(allowRemoteLibs);
    }

    /**
     * Load a library or set of libraries from a location making them
     * available for use
     *
     * @param libLocation the file or URL of the library location
     * @param importAll if true all tasks are imported as the library is
     *      loaded
     * @exception ExecutionException if the library cannot be loaded
     */
    public void loadLib(String libLocation, boolean importAll)
         throws ExecutionException {
        try {
            Map librarySpecs = new HashMap();
            libManager.loadLib(librarySpecs, libLocation);
            libManager.configLibraries(frame.getInitConfig(), librarySpecs,
                frame.getAntLibraries());

            if (importAll) {
                Iterator i = librarySpecs.keySet().iterator();
                while (i.hasNext()) {
                    String libraryId = (String)i.next();
                    frame.importLibrary(libraryId);
                }
            }
        } catch (MalformedURLException e) {
            throw new ExecutionException("Unable to load libraries from "
                 + libLocation, e);
        } catch (ConfigException e) {
            throw new ExecutionException(e);
        }
    }
}

