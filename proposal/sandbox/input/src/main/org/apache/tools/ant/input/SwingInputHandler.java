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

package org.apache.tools.ant.input;

import org.apache.tools.ant.BuildException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;

/**
 * Very, very, very simplistic GUI input handler, nothing more than a
 * proof of concept.
 *
 * <p>I don't intend to commit this to the main branch if my proposal
 * should get accepted.</p>
 *
 * <p>I guess I can use this code to demonstrate why nobody should
 * hire me to do GUI stuff 8-)</p>
 *
 * @author <a href="mailto:stefan.bodewig@epost.de">Stefan Bodewig</a>
 * @version $Revision$
 */
public class SwingInputHandler implements InputHandler {

    private Component inputComponent;
    private boolean done = false;

    public SwingInputHandler() {
    }

    public synchronized void handleInput(final InputRequest request) 
        throws BuildException {
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Container cp = frame.getContentPane();
        cp.setLayout(new BorderLayout());
        
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(new JLabel(request.getPrompt()));
        if (request instanceof MultipleChoiceInputRequest) {
            JComboBox c = new JComboBox(((MultipleChoiceInputRequest) request).getChoices());
            c.setEditable(false);
            p.add(c);
            inputComponent = c;
        } else {
            p.add((inputComponent = new JTextField(20)));
        }
        
        cp.add(p, BorderLayout.CENTER);
        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    request.setInput(getInput());
                    frame.dispose();
                    synchronized (SwingInputHandler.this) {
                        SwingInputHandler.this.done = true;
                        SwingInputHandler.this.notifyAll();
                    }
                }
            });
        cp.add(button, BorderLayout.SOUTH);
        frame.pack();
        frame.show();

        while (!done) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        done = false;
    }

    private String getInput() {
        if (inputComponent instanceof JTextField) {
            return ((JTextField) inputComponent).getText();
        } else {
            return ((JComboBox) inputComponent).getSelectedItem().toString();
        }
    }
}
