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
package org.apache.ant.antcore.config;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.ant.common.util.ConfigException;
import org.apache.ant.common.util.PathTokenizer;
import org.apache.ant.init.InitUtils;

/**
 * An AntConfig is the java class representation of the antconfig.xml files
 * used to configure Ant.
 *
 * @author <a href="mailto:conor@apache.org">Conor MacNeill</a>
 * @created 20 January 2002
 */
public class AntConfig {
    /** The list of additional directories to be searched for Ant libraries */
    private List libraryLocations = new ArrayList();

    /**
     * A list of additional paths for each ant library, indexed on the
     * library id
     */
    private Map libPaths = new HashMap();

    /** Indicates if remote libraries may be used */
    private boolean allowRemoteLibs = false;

    /** Indicates if remote projects may be used */
    private boolean allowRemoteProjects = false;

    /**
     * Indicate if the use of remote library's is allowe dby this config.
     *
     * @return true if this config allows the use of remote libraries,
     */
    public boolean isRemoteLibAllowed() {
        return allowRemoteLibs;
    }

    /**
     * Indicate if this config allows the execution of a remote project 
     *
     * @return true if remote projects are allowed
     */
    public boolean isRemoteProjectAllowed() {
        return allowRemoteProjects;
    }

    /**
     * Get the additional locations in which to search for Ant Libraries
     *
     * @return an iterator over the library locations
     */
    public Iterator getLibraryLocations() {
        return libraryLocations.iterator();
    }

    /**
     * Get the list of additional path components for a given path
     *
     * @param libraryId the identifier for the library
     * @return the list of URLs for the additional paths for the given
     *      library
     */
    public List getLibraryPathList(String libraryId) {
        List libraryPathList = (List)libPaths.get(libraryId);
        if (libraryPathList == null) {
            libraryPathList = new ArrayList();
            libPaths.put(libraryId, libraryPathList);
        }
        return libraryPathList;
    }

    /**
     * Gets the libraryIds of the AntConfig
     *
     * @return an interator over the library identifiers for which there is
     *      additional path information
     */
    public Iterator getLibraryIds() {
        return libPaths.keySet().iterator();
    }

    /**
     * Add an additional set of paths for the given library.
     *
     * @param libraryId The library id for which the additional class path
     *      is being specified
     * @param libraryPath the classpath style string for the library's
     *      additonal paths
     * @exception ConfigException if the appropriate URLs cannot be formed.
     */
    public void addLibPath(String libraryId, String libraryPath)
         throws ConfigException {
        try {
            List libraryPathList = getLibraryPathList(libraryId);
            PathTokenizer p = new PathTokenizer(libraryPath);
            while (p.hasMoreTokens()) {
                String pathElement = p.nextToken();
                File pathElementFile = new File(pathElement);
                URL pathElementURL = InitUtils.getFileURL(pathElementFile);
                libraryPathList.add(pathElementURL);
            }
        } catch (MalformedURLException e) {
            throw new ConfigException("Unable to process libraryPath '"
                 + libraryPath + "' for library '" + libraryId + "'", e);
        }
    }

    /**
     * Add an additional URL for the library's classpath
     *
     * @param libraryId the library's unique Id
     * @param libraryURL a string which points to the additonal path
     * @exception ConfigException if the URL could not be formed
     */
    public void addLibURL(String libraryId, String libraryURL)
         throws ConfigException {
        try {
            List libraryPathList = getLibraryPathList(libraryId);
            libraryPathList.add(new URL(libraryURL));
        } catch (MalformedURLException e) {
            throw new ConfigException("Unable to process libraryURL '"
                 + libraryURL + "' for library '" + libraryId + "'", e);
        }

    }

    /**
     * Merge in another ocnfiguration. The configuration being merged in
     * takes precedence
     *
     * @param otherConfig the other AntConfig to be merged.
     */
    public void merge(AntConfig otherConfig) {
        // merge by
        List currentLibraryLocations = libraryLocations;
        libraryLocations = new ArrayList();
        libraryLocations.addAll(otherConfig.libraryLocations);
        libraryLocations.addAll(currentLibraryLocations);

        Iterator i = otherConfig.libPaths.keySet().iterator();
        while (i.hasNext()) {
            String libraryId = (String)i.next();
            List currentList = getLibraryPathList(libraryId);
            List combined = new ArrayList();
            combined.addAll(otherConfig.getLibraryPathList(libraryId));
            combined.addAll(currentList);
            libPaths.put(libraryId, combined);
        }
    }

    /**
     * Add a new task directory to be searched for additional Ant libraries
     *
     * @param libraryLocation the location (can be a file or a URL) where
     *      the libraries may be loaded from.
     */
    public void addAntLibraryLocation(String libraryLocation) {
        libraryLocations.add(libraryLocation);
    }
}

