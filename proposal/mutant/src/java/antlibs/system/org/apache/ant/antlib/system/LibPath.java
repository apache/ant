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
package org.apache.ant.antlib.system;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.ant.common.antlib.AbstractTask;
import org.apache.ant.common.antlib.AntContext;
import org.apache.ant.common.antlib.ValidationException;
import org.apache.ant.common.service.ComponentService;
import org.apache.ant.common.util.AntException;
import org.apache.ant.common.event.MessageLevel;
import org.apache.ant.init.InitUtils;

/**
 * Task to add an additional classpath to the given library
 *
 * @author Conor MacNeill
 * @created 27 January 2002
 */
public class LibPath extends AbstractTask {
    /** The id of the library for which this additional path is being set */
    private String libraryId;
    /**
     * This is the location, either file or URL of the library or libraries
     * to be loaded
     */
    private URL url;

    /**
     * Sets the libraryId of the LibPath
     *
     * @param libraryId the new libraryId value
     */
    public void setLibraryId(String libraryId) {
        this.libraryId = libraryId;
    }

    /**
     * Sets the URL of the library to be loaded
     *
     * @param url the URL from which the library is to be loaded
     * @exception ValidationException if the URL cannot be set
     */
    public void setURL(URL url) throws ValidationException {
        checkNullURL();
        this.url = url;
    }

    /**
     * Set the file from which the library should be loaded.
     *
     * @param file the file from which the library should be loaded
     * @exception ValidationException if the file attribute cannot be set
     */
    public void setFile(File file) throws ValidationException {
        checkNullURL();
        try {
            this.url = InitUtils.getFileURL(file);
        } catch (MalformedURLException e) {
            throw new ValidationException(e);
        }
    }

    /**
     * Set the dir in which to search for AntLibraries.
     *
     * @param dir the dir from which all Ant Libraries found will be loaded.
     * @exception ValidationException if the dir attribute cannot be set
     */
    public void setDir(File dir) throws ValidationException {
        checkNullURL();
        try {
            this.url = InitUtils.getFileURL(dir);
        } catch (MalformedURLException e) {
            throw new ValidationException(e);
        }
    }

    /**
     * Validate this task is configured correctly
     *
     * @exception ValidationException if the task is not configured correctly
     */
    public void validateComponent() throws ValidationException {
        if (libraryId == null) {
            throw new ValidationException("You must specify the id of the"
                 + "library for which you are providing additional classpaths");
        }
        if (url == null) {
            throw new ValidationException("You must provide an additional "
                 + "classpath using one of the file, dir or url attributes");
        }
    }

    /**
     * Add the libpath to the set of paths associated with the library
     *
     * @exception AntException if the library path cannot be addded to
     *      the library
     */
    public void execute() throws AntException {
        AntContext context = getAntContext();
        ComponentService componentService = (ComponentService)
            context.getCoreService(ComponentService.class);
        log("Adding lib path " + url + " for " + libraryId,
            MessageLevel.MSG_DEBUG);
        componentService.addLibPath(libraryId, url);
    }

    /**
     * Check if any of the location specifying attributes have already been
     * set.
     *
     * @exception ValidationException if the search URL has already been set
     */
    private void checkNullURL() throws ValidationException {
        if (url != null) {
            throw new ValidationException("Location of library has already "
                + "been set. Please use only one of file, dir or url "
                + "attributes");
        }
    }
}

