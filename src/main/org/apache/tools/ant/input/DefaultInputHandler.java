/*
 * Copyright  2002-2004 Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.input;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.KeepAliveInputStream;

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
     * @param request the request to handle
     * @throws BuildException if not possible to read from console
     */
    public void handleInput(InputRequest request) throws BuildException {
        String prompt = getPrompt(request);
        DataInputStream in = null;
        try {
            in =
                new DataInputStream(new KeepAliveInputStream(getInputStream()));
            do {
                System.err.println(prompt);
                System.err.flush();
                try {
                    String input = in.readLine();
                    request.setInput(input);
                } catch (IOException e) {
                    throw new BuildException("Failed to read input from"
                                             + " Console.", e);
                }
            } while (!request.isInputValid());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    throw new BuildException("Failed to close input.", e);
                }
            }
        }
    }

    /**
     * Constructs user prompt from a request.
     *
     * <p>This implementation adds (choice1,choice2,choice3,...) to the
     * prompt for <code>MultipleChoiceInputRequest</code>s.</p>
     *
     * @param request the request to construct the prompt for.
     *                Must not be <code>null</code>.
     * @return the prompt to ask the user
     */
    protected String getPrompt(InputRequest request) {
        String prompt = request.getPrompt();
        if (request instanceof MultipleChoiceInputRequest) {
            StringBuffer sb = new StringBuffer(prompt);
            sb.append("(");
            Enumeration e =
                ((MultipleChoiceInputRequest) request).getChoices().elements();
            boolean first = true;
            while (e.hasMoreElements()) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(e.nextElement());
                first = false;
            }
            sb.append(")");
            prompt = sb.toString();
        }
        return prompt;
    }

    /**
     * Returns the input stream from which the user input should be read.
     * @return the input stream from which the user input should be read.
     */
    protected InputStream getInputStream() {
        return System.in;
    }

}
