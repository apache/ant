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
package org.apache.tools.ant.taskdefs.optional.rjunit;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.optional.rjunit.formatter.Formatter;
import org.apache.tools.ant.taskdefs.optional.rjunit.remote.Server;

/**
 * An element representing the server configuration.
 *
 * <pre>
 * <!ELEMENT server (formatter)*>
 * <!ATTLIST server port numeric 6666>
 * <!ATTLIST server haltonfailure (yes|no) no>
 * <!ATTLIST server haltonerror (yes|no) no>
 * </pre>
 *
 */
public final class ServerElement extends ProjectComponent {

    /** formatters that write the tests results */
    private ArrayList formatters = new ArrayList();

    /** port to run the server on. Default to 6666 */
    private int port = 6666;

    /** stop the client run if a failure occurs */
    private boolean haltOnFailure = false;

    /** stop the client run if an error occurs */
    private boolean haltOnError = false;

    /** the parent task */
    private RJUnitTask parent;

    private Server server;

    /** create a new server */
    public ServerElement(RJUnitTask value) {
        parent = value;
    }

    /** start the server and block until client has finished */
    public void execute() throws BuildException {
        // configure the server...
        server = new Server(port);
        final int formatterCount = formatters.size();
        for (int i = 0; i < formatterCount; i++) {
            final Formatter f = (Formatter) formatters.get(i);
            server.addListener(f);
        }

        // and run it. It will stop once a client has finished.
        try {
            server.start(false); // do not loop
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            server.shutdown();
        }
    }

    /** set the port to listen to */
    public void setPort(int value) {
        port = value;
    }

//@fixme  logic problem here, should the server say to the client
// that there it should stop or should the client do it itself ?

    public void setHaltOnFailure(boolean value) {
        haltOnFailure = value;
    }

    public void setHaltOnError(boolean value) {
        haltOnError = value;
    }

    /** add a new formatter element */
    public void addConfiguredFormatter(ResultFormatterElement fe) {
        Formatter formatter = fe.createFormatter();
        formatters.add(formatter);
    }
}
