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
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.gui.util.StackFrame;
import org.apache.tools.ant.gui.command.Command;
import org.apache.tools.ant.gui.command.NoOpCmd;
import org.apache.tools.ant.gui.AppContext;
import java.util.EventObject;

/**
 * Wrapper event for the events generated during an Ant build.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class AntBuildEvent extends AntEvent {

    /** The original event we are wrapping. */
    private BuildEvent _buildEvent = null;
    /** The type of event we are wrapping. */
    private BuildEventType _type = null;

    /**
	 * Standard ctor.
	 * 
	 * @param context application context.
	 */
    public AntBuildEvent(AppContext context, 
                            BuildEvent buildEvent, BuildEventType type) {
        super(context);
        _buildEvent = buildEvent;
        _type = type;

        if(_buildEvent == null || _type == null) {
            throw new IllegalArgumentException("Null parameter passed");
        }
    }

	/** 
	 * Get the wrapped build event.
	 * 
	 * @return Build event.
	 */
    public BuildEvent getEvent() {
        return _buildEvent;
    }

	/** 
	 * Get the build event type.
	 * 
	 * @return Event type.
	 */
    public BuildEventType getType() {
        return _type;
    }

	/** 
	 * Create the appropriate default response command to this event.
	 * 
	 * @return Command representing an appropriate response to this event.
	 */
    public Command createDefaultCmd() {
        return new NoOpCmd();
    }
    
	/** 
	 * Create a string representation of this.
	 * 
	 * @return String representation.
	 */
    public String toString() {
        StringBuffer buf = new StringBuffer();

        if(_buildEvent.getMessage() != null) {
            buf.append(_buildEvent.getMessage());
            buf.append('\n');
        }

        if(_buildEvent.getException() != null) {
            buf.append(StackFrame.toString(_buildEvent.getException()));
        }

        return buf.toString();
    }

}
