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
package org.apache.ant.antcore.frontend;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.ant.antcore.config.AntConfig;
import org.apache.ant.antcore.config.AntConfigHandler;
import org.apache.ant.antcore.xml.ParseContext;
import org.apache.ant.antcore.xml.XMLParseException;
import org.apache.ant.init.InitUtils;
import org.apache.ant.common.constants.Namespace;

/**
 * Frontend Utilities methods and constants.
 *
 * @author Conor MacNeill
 * @created 16 April 2002
 */
public class FrontendUtils {
    /** The default build file name */
    public static final String DEFAULT_BUILD_FILENAME = "build.ant";

    /** The default build file name */
    public static final String DEFAULT_ANT1_FILENAME = "build.xml";


    /**
     * Get the AntConfig from the given config area if it is available
     *
     * @param configArea the config area from which the config may be read
     * @return the AntConfig instance representing the config info read in
     *      from the config area. May be null if the AntConfig is not present
     * @exception FrontendException if the URL for the config file cannotbe
     *      formed.
     */
    public static AntConfig getAntConfig(File configArea)
         throws FrontendException {
        File configFile = new File(configArea, "antconfig.xml");

        try {
            return getAntConfigFile(configFile);
        } catch (FileNotFoundException e) {
            // ignore if files are not present
            return null;
        }
    }


    /**
     * Read in a config file
     *
     * @param configFile the file containing the XML config
     * @return the parsed config object
     * @exception FrontendException if the config cannot be parsed
     * @exception FileNotFoundException if the file cannot be found.
     */
    public static AntConfig getAntConfigFile(File configFile)
         throws FrontendException, FileNotFoundException {
        try {
            URL configFileURL = InitUtils.getFileURL(configFile);

            ParseContext context = new ParseContext();

            AntConfigHandler configHandler = new AntConfigHandler();

            context.parse(configFileURL, "antconfig", configHandler);

            return configHandler.getAntConfig();
        } catch (MalformedURLException e) {
            throw new FrontendException("Unable to form URL to read "
                + "config from " + configFile, e);
        } catch (XMLParseException e) {
            if (e.getCause() instanceof FileNotFoundException) {
                throw (FileNotFoundException) e.getCause();
            }

            throw new FrontendException("Unable to parse config file from "
                 + configFile, e, e.getLocation());
        }
    }

}

