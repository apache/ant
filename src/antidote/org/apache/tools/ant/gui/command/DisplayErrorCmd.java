/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999, 2000 The Apache Software Foundation.  All rights
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
package org.apache.tools.ant.gui.command;
import org.apache.tools.ant.gui.core.AppContext;
import org.apache.tools.ant.gui.util.StackFrame;
import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Command for displaying an arbitrary error message to the user.
 *
 * @version $Revision$ 
 * @author Simeon H.K. Fitch 
 */
public class DisplayErrorCmd extends AbstractCommand {
    /** Text description of error. */
    private String _message = null;
    /** Throwable associated with the error. */
    private Throwable _ex = null;

	/** 
	 * Standard ctor.
	 * 
	 * @param context Application context.
	 */
    public DisplayErrorCmd(AppContext context) {
        super(context);
    }

	/** 
	 * Standard constuctor.
	 * 
	 * @param context Application context.
	 * @param message Error message.
	 * @param ex Throwable assocated with error.
	 */
    public DisplayErrorCmd(AppContext context, String message, Throwable ex) {
        this(context);
        setMessage(message);
        setThrowable(_ex);
    }

	/** 
	 * No Throwable constructor.
	 * 
	 * @param context Application context.
	 * @param message Error message.
	 */
    public DisplayErrorCmd(AppContext context, String message) {
        this(context, message, null);
    }

	/** 
	 * Set the error message.
	 * 
	 * @param message Error message.
	 */
    public void setMessage(String message) {
        _message = message;
    }

	/** 
	 * Set the throwable associated with the error.
	 * 
	 * @param ex Throwable associated with the error.
	 */
    public void setThrowable(Throwable ex) {
        _ex = ex;
    }

	/** 
	 * Display the error.
	 * 
	 */
    public void run() {
        String title = getContext().getResources().
            getString(getClass(), "title"); 

        JOptionPane.showMessageDialog(
            getContext().getParentFrame(), new MsgPanel(),
            title, JOptionPane.ERROR_MESSAGE);
    }

    // Panel for assembling the error information.
    private class MsgPanel extends JPanel implements ActionListener {
        public MsgPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            add(new JLabel(_message));
            if(_ex != null) {
                add(new JLabel(_ex.getMessage()));
                JButton b = new JButton(getContext().getResources().
                                        getString(DisplayErrorCmd.class, 
                                                  "expand"));
                b.addActionListener(this);
                add(Box.createVerticalStrut(20));
                add(b);
            }
        }
        // Called when the user clicks the expand button.
        public void actionPerformed(ActionEvent e) {
            JComponent source = (JComponent) e.getSource();
            JComponent parent = (JComponent) source.getParent();
            parent.remove(source);
            JTextArea text = new JTextArea();
            text.setEditable(false);
            text.setText(StackFrame.toString(_ex));
            parent.add(new JScrollPane(text));
            SwingUtilities.windowForComponent(parent).pack();
        }
    }
}
