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
package org.apache.ant.start;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.apache.ant.init.InitConfig;
import org.apache.ant.init.InitException;
import java.io.File;

/**
 *  This is the main startup class for the command line interface of Ant. It
 *  establishes the classloaders used by the other components of Ant.
 *
 * @author  Conor MacNeill
 * @created  9 January 2002
 */
public class Main {
    /**  The actual class that implements the command line front end. */
    public static final String DEFAULT_COMMANDLINE_CLASS
         = "org.apache.ant.cli.Commandline";

    /**  The default front end name */
    public static final String DEFAULT_FRONTEND = "cli";


    /**
     *  Entry point for starting command line Ant
     *
     * @param  args commandline arguments
     * @exception  Exception if there is a problem running Ant
     */
    public static void main(String[] args) throws Exception {
        Main main = new Main();
        int frontendIndex = -1;
        String frontend = DEFAULT_FRONTEND;

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-frontend")) {
                frontendIndex = i;
                break;
            }
        }

        if (frontendIndex != -1) {
            try {
                frontend = args[frontendIndex + 1];
            } catch (IndexOutOfBoundsException e) {
                throw new InitException("You must specify a value for the "
                     + "-frontend argument");
            }

            String[] newArgs = new String[args.length - 2];

            System.arraycopy(args, 0, newArgs, 0, frontendIndex);
            if (args.length > (frontendIndex + 2)) {
                System.arraycopy(args, frontendIndex + 2, newArgs,
                    frontendIndex, args.length - frontendIndex - 2);
            }
            args = newArgs;
        }

        String defaultClass = frontend.equals(DEFAULT_FRONTEND)
             ? DEFAULT_COMMANDLINE_CLASS : null;

        main.start(frontend, defaultClass, args);
    }


    /**
     *  Internal start method used to initialise front end
     *
     * @param  frontend the frontend jar to launch
     * @param  args commandline arguments
     * @param  defaultClass the default class to use if it cannot be determined 
     *         from the jar itself
     * @exception  InitException if the front end cannot be started
     */
    public void start(String frontend, String defaultClass, String[] args)
         throws InitException {
        try {             
            InitConfig config = new InitConfig(getClass());
    
            URL frontendJar = new URL(config.getLibraryURL(),
                "frontend/" + frontend + ".jar");
            URL[] frontendJars = new URL[]{frontendJar};
            ClassLoader frontEndLoader
                 = new URLClassLoader(frontendJars, config.getCoreLoader());
    
            //System.out.println("Front End Loader config");
            //LoaderUtils.dumpLoader(System.out, frontEndLoader);

            if (frontendJar.getProtocol().equals("file")) {
                File jarFile = new File(frontendJar.getFile());
                if (!jarFile.exists()) {
                    throw new InitException("Could not find jar for frontend \""
                        + frontend + "\" - expected at " + frontendJar);
                }
            }
            String mainClass = getMainClass(frontendJar);
    
            if (mainClass == null) {
                mainClass = defaultClass;
            }
    
            if (mainClass == null) {
                throw new InitException("Unable to determine main class "
                     + " for \"" + frontend + "\" frontend");
            }
    
            // Now start the front end by reflection.
            Class frontendClass = Class.forName(mainClass, true,
                frontEndLoader);
    
            final Class[] param = {Class.forName("[Ljava.lang.String;"),
                InitConfig.class};
            final Method startMethod
                 = frontendClass.getMethod("start", param);
            final Object[] argument = {args, config};
    
            startMethod.invoke(null, argument);
        } catch (Exception e) {
            throw new InitException(e);
        }
    }


    /**
     *  Pick up the main class from a jar's manifest
     *
     * @param  jarURL the URL to the jar
     * @return  the jar's main-class or null if it is not specified or
     *      cannot be determined.
     */
    private String getMainClass(URL jarURL) {
        try {
            JarInputStream stream = null;

            try {
                stream = new JarInputStream(jarURL.openStream());

                Manifest manifest = stream.getManifest();

                if (manifest == null) {
                    return null;
                }
                Attributes mainAttributes = manifest.getMainAttributes();

                return mainAttributes.getValue("Main-Class");
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (IOException e) {
            // ignore
            return null;
        }
    }
}

