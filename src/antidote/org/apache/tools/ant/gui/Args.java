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

package org.apache.tools.ant.gui;
import org.apache.tools.ant.gui.core.ResourceManager;

/**
 * Class encapsulating the parsing of command-line arguments for Antidote.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class Args {

    private ResourceManager _resources = null;
    private boolean _wizardMode = false;
    private String _fileName = null;
    private boolean _debugMode = false;

    /** 
     * Ctor for parsing command line arguments.
     * 
     * @param args Arguments to parse.
     */
    public Args(String[] args) {
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(i == args.length - 1 && !arg.startsWith("-")) {
                _fileName = arg;
            }
            else if(arg.startsWith("-h")) {
                System.out.println(getUsage());
                System.exit(0);
            }
            else if(arg.equals("-wizard")) {
                _wizardMode = true;
            }
            else if(arg.equals("-debug")) {
                _debugMode = true;
            }
            else {
                String msg = getResources().getMessage(
                    "invalidArg", new Object[] { arg });
                abort(msg);
            }
        }
    }

    /** 
     * Get the resources, loading them if necessary.
     * 
     * @return Loaded resources.
     */
    private ResourceManager getResources() {
        if(_resources == null) {
            _resources = new ResourceManager(
                "org.apache.tools.ant.gui.resources.args");
        }
        return _resources;
    }

    /** 
     * Print message and exit.
     * 
     * @param error Error message to print.
     */
    private void abort(String error) {
        System.err.println(error);
        System.err.println(getUsage());
        System.exit(1);
    }

    /** 
     * Get the command line usage of Antidote.
     * 
     * @return Command line usage help.
     */
    public String getUsage() {
        return getResources().getString("usage");
    }

    /** 
     * Get the build filename.
     * 
     * @return Build file name.
     */
    public String getBuildFile() {
        return _fileName;
    }

    /** 
     * Determine if wizard mode was requested for generating a new 
     * build file.
     * 
     * @return True if wizard mode, false otherwise.
     */
    public boolean isWizardMode() {
        return _wizardMode;
    }

    /** 
     * Determine if debug mode was requested.
     * 
     * @return True if debug mode, false otherwise.
     */
    public boolean isDebugMode() {
        return _debugMode;
    }

}
