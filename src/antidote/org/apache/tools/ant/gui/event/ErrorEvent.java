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
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
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
package org.apache.tools.ant.gui.event;
import org.apache.tools.ant.gui.AppContext;
import org.apache.tools.ant.gui.command.DisplayErrorCmd;
import org.apache.tools.ant.gui.command.Command;
import org.apache.tools.ant.gui.util.StackFrame;


/**
 * Event fired whenever there is an error of any sort.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class ErrorEvent extends AntEvent {
    /** Text description of error. */
    private String _message = null;
    /** Throwable associated with the error. */
    private Throwable _ex = null;

	/** 
	 * Standard constructor.
	 * 
	 * @param context Application context.
	 * @param message Message about the error.
	 * @param ex Throwable associated with the error.
	 */
    public ErrorEvent(AppContext context, String message, Throwable ex) {
        super(context);
        _message = message;
        _ex = ex;
    }

	/** 
	 * Message centric constructor.
	 * 
	 * @param context Application context.
	 * @param message Message to display.
	 */
    public ErrorEvent(AppContext context, String message) { 
        this(context, message, null);
    }

	/** 
	 * Throwable centric constructor.
	 * 
	 * @param context Application context.
	 * @param ex Throwable behind the error.
	 */
    public ErrorEvent(AppContext context, Throwable ex) {
        this(context, ex.getMessage(), ex);
    }

	/** 
	 * Create the appropriate response command to this event.
	 * 
	 * @return Command representing an appropriate response to this event.
	 */
    public Command createDefaultCmd() {
        Command retval = new DisplayErrorCmd(getContext(), _message, _ex);
        return retval;
    }

	/** 
	 * Create human readable version of this.
	 * 
	 * @return String representation.a
	 */
    public String toString() {
        StringBuffer buf = new StringBuffer("Error: ");

        if(_message != null) {
            buf.append(_message);
            buf.append('\n');
        }
        if(_ex != null) {
            buf.append(StackFrame.toString(_ex));
        }
        return buf.toString();
    }

}
