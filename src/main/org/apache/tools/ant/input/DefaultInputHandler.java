/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
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

package org.apache.tools.ant.input;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;

/**
 * Prompts on System.err, reads input from System.in
 *
 * @author Stefan Bodewig
 * @version $Revision$
 * @since Ant 1.5
 */
public class DefaultInputHandler implements InputHandler {

    /**
     * Empty no-arg constructor
     */
    public DefaultInputHandler() {
    }

    /**
     * Prompts and requests input.  May loop until a valid input has
     * been entered.
     */
    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        DataInputStream in = new DataInputStream(getInputStream());
        do {
            System.err.println(prompt);
            try {
                String input = in.readLine();
                request.setInput(input);
            } catch (IOException e) {
                throw new BuildException("Failed to read input from Console.",
                                         e);
            }
        } while (!request.isInputValid());
    }

    /**
     * Constructs user prompt from a request.
     *
     * <p>This implementation adds (choice1,choice2,choice3,...) to the
     * prompt for <code>MultipleChoiceInputRequest</code>s.</p>
     *
     * @param request the request to construct the prompt for.
     *                Must not be <code>null</code>.
     */
    protected String getPrompt(InputRequest request) {
        String prompt = request.getPrompt();
        if (request instanceof MultipleChoiceInputRequest) {
            StringBuffer sb = new StringBuffer(prompt);
            sb.append("(");
            Enumeration enum =
                ((MultipleChoiceInputRequest) request).getChoices().elements();
            boolean first = true;
            while (enum.hasMoreElements()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(enum.nextElement());
                first = false;
            }
            sb.append(")");
            prompt = sb.toString();
        }
        return prompt;
    }

    /**
     * Returns the input stream from which the user input should be read.
     */
    protected InputStream getInputStream() {
        return System.in;
    }

}
