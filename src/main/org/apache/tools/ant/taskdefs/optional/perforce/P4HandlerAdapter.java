/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2003 The Apache Software Foundation.  All rights
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
 * 4. The names "Ant" and "Apache Software
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

package org.apache.tools.ant.taskdefs.optional.perforce;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
/**
 * base class to manage streams around the execution of the Perforce
 * command line client
 * @author <A HREF="mailto:leslie.hughes@rubus.com">Les Hughes</A>
 */
public abstract class P4HandlerAdapter  implements P4Handler {

    String p4input = "";
    private PumpStreamHandler myHandler = null;
    /**
     *  set any data to be written to P4's stdin
     *  @param p4Input the text to write to P4's stdin
     */
    public void setOutput(String p4Input) {
        this.p4input = p4Input;
    }
    /**
     * subclasses of P4HandlerAdapter must implement this routine
     * processing of one line of stdout or of stderr
     * @param line line of stdout or stderr to process
     */
    public abstract void process(String line);

    /**
     * this routine gets called by the execute routine of the Execute class
     * it connects the PumpStreamHandler to the input/output/error streams of the process.
     * @throws BuildException
     * @see org.apache.tools.ant.taskdefs.Execute#execute
     */
    public void start() throws BuildException {
        if (p4input != null && p4input.length() > 0) {
            myHandler = new PumpStreamHandler(new P4OutputStream(this), new P4OutputStream(this),
                new ByteArrayInputStream(p4input.getBytes()));
        } else {
            myHandler = new PumpStreamHandler(new P4OutputStream(this), new P4OutputStream(this));
        }
        myHandler.setProcessInputStream(os);
        myHandler.setProcessErrorStream(es);
        myHandler.setProcessOutputStream(is);
        myHandler.start();
    }

    /**
     * stops the processing of streams
     * called from P4Base#execP4Command(String command, P4Handler handler)
     * @see P4Base#execP4Command(String, P4Handler)
     */
    public void stop() {
        myHandler.stop();
    }

    OutputStream os;    //Input
    InputStream is;     //Output
    InputStream es;     //Error

    /**
     * connects the handler to the input stream into Perforce
     * used indirectly by tasks requiring to send specific standard input
     * such as p4label, p4change
     * @param os the stream bringing input to the p4 executable
     * @throws IOException under unknown circumstances
     */
    public void setProcessInputStream(OutputStream os) throws IOException {
        this.os = os;
    }

    /**
     * connects the handler to the stderr of the Perforce process
     * @param is stderr coming from Perforce
     * @throws IOException under unknown circumstances
     */
    public void setProcessErrorStream(InputStream is) throws IOException {
        this.es = is;
    }

    /**
     * connects the handler to the stdout of the Perforce process
     * @param is stdout coming from Perforce
     * @throws IOException under unknown circumstances
     */
    public void setProcessOutputStream(InputStream is) throws IOException {
        this.is = is;
    }
}
