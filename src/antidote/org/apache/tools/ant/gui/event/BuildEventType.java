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
package org.apache.tools.ant.gui.event;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildEvent;
import java.lang.reflect.Method;

/**
 * Enumeration class of the different contexts in which Ant will generate
 * a BuildEvent.
 * 
 * @version $Revision$ 
 * @author Simeon Fitch 
 */
public class BuildEventType {
    /** Enum value. */
    private int _value = 0;

	/** 
	 * Standard ctor.
	 * 
	 * @param value Index value.
	 */
    private BuildEventType(int value) {
        _value = value;
    }

	/** 
	 * Get the enumeration value.
	 * 
	 * @return 
	 */
    public int getValue() {
        return _value;
    }

	/** 
	 * Pseudo abstract method for firing an event to a build listener
     * based on our enumation value. I overridded by the individual instances.
	 * 
	 * @param e Event to fire.
	 * @param l Listener to send event to.
	 */
    public void fireEvent(BuildEvent e, BuildListener l) {
        try {
            Method method = 
                BuildListener.class.getMethod(_methodNameMap[_value], 
                                              _listenerMethodParam);
            method.invoke(l, new Object[] { e });
        }
        catch(Exception ex) {
            // XXX log me.
            ex.printStackTrace();
        }
    }

	/** 
	 * Get the enumeration value with the given index value.
	 * 
	 * @param value Index value.
	 * @return Enumeration value.
	 */
    public static BuildEventType fromInt(int value) {
        return _objectMap[value];
    }

	/** 
	 * Determine if the given object is logically equal to this one.
	 * 
	 * @param o Object to compare to 
	 * @return True if equal, false otherwise.
	 */
    public boolean equals(Object o) {
        if(o instanceof BuildEventType) {
            return ((BuildEventType)o)._value == _value;
        }
        return false;
    }
	/** 
	 * Generate a hash value.
	 * 
	 * @return Hash value.
	 */
    public int hashValue() {
        return _value;
    }

	/** 
	 * Provide a string representation of this. 
	 * 
	 * @return String representation.
	 */
    public String toString() {
        return _stringMap[_value];
    }


    /* Index values. */ 
    public static final int BUILD_STARTED_VAL = 0;
    public static final int BUILD_FINISHED_VAL = 1;
    public static final int TARGET_STARTED_VAL = 2;
    public static final int TARGET_FINISHED_VAL = 3;
    public static final int TASK_STARTED_VAL = 4;
    public static final int TASK_FINISHED_VAL = 5;
    public static final int MESSAGE_LOGGED_VAL = 6;

    /* Enumeration values. */ 
    public static final BuildEventType BUILD_STARTED = 
      new BuildEventType(BUILD_STARTED_VAL);
    public static final BuildEventType BUILD_FINISHED = 
      new BuildEventType(BUILD_FINISHED_VAL);
    public static final BuildEventType TARGET_STARTED = 
      new BuildEventType(TARGET_STARTED_VAL);
    public static final BuildEventType TARGET_FINISHED = 
      new BuildEventType(TARGET_FINISHED_VAL);
    public static final BuildEventType TASK_STARTED = 
      new BuildEventType(TASK_STARTED_VAL);
    public static final BuildEventType TASK_FINISHED = 
      new BuildEventType(TASK_FINISHED_VAL);
    public static final BuildEventType MESSAGE_LOGGED = 
      new BuildEventType(MESSAGE_LOGGED_VAL);

    /** Index to object mapping. */
    private static final BuildEventType[] _objectMap = {
        BUILD_STARTED,
        BUILD_FINISHED,
        TARGET_STARTED,
        TARGET_FINISHED,
        TASK_STARTED,
        TASK_FINISHED,
        MESSAGE_LOGGED
    };

    /** String map. XXX needs to be localized. */
    private static final String[] _stringMap = {
        "Build Started",
        "Build Finished",
        "Target Started",
        "Target Finished",
        "Task Started",
        "Task Finished",
        "Message Logged"
    };

    /** Map of corresponding method names in the BuildListener intereface. */
    private static final String[] _methodNameMap = {
        "buildStarted",
        "buildFinished",
        "targetStarted",
        "targetFinished",
        "taskStarted",
        "taskFinished",
        "messageLogged"
    };

    private static final Class[] _listenerMethodParam = { BuildEvent.class };
}
