/*
 * Copyright  2001-2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.optional.sitraka;

import java.util.Hashtable;
import java.util.Vector;
import org.apache.tools.ant.BuildException;

/**
 * Trigger information. It will return as a command line argument by calling
 * the <tt>toString()</tt> method.
 *
 * @author <a href="mailto:sbailliez@imediation.com">Stephane Bailliez</a>
 */
public class Triggers {

    protected Vector triggers = new Vector();

    public Triggers() {
    }


    /**
     * add a method trigger
     */
    public void addMethod(Method method) {
        triggers.addElement(method);
    }

    // -jp_trigger=ClassName.*():E:S,ClassName.MethodName():X:X
    public String toString() {
        StringBuffer buf = new StringBuffer();
        final int size = triggers.size();
        for (int i = 0; i < size; i++) {
            buf.append(triggers.elementAt(i).toString());
            if (i < size - 1) {
                buf.append(',');
            }
        }
        return buf.toString();
    }


    /**
     * A trigger for the coverage report
     */
    public static class Method {
        protected String name;
        protected String event;
        protected String action;
        protected String param;

        /**
         * The name of the method(s) as a regular expression. The name
         * is the fully qualified name on the form <tt>package.classname.method</tt>
         *  required.
         */
        public void setName(String value) {
            name = value;
        }

        /**
         * the event on the method that will trigger the action. Must be
         * &quot;enter&quot; or &quot;exit&quot;
         *  required.
         */
        public void setEvent(String value) {
            if (eventMap.get(value) == null) {
                throw new BuildException("Invalid event, must be one of " + eventMap);
            }
            event = value;
        }

        /**
         * The action to execute; required. Must be one of &quot;clear&quot;,
         * &quot;pause&quot;, &quot;resume&quot;, &quot;snapshot&quot;, &quot;suspend&quot;,
         * or &quot;exit&quot;. They respectively clear recording, pause recording,
         * resume recording, take a snapshot, suspend the recording and exit the program.
         */
        public void setAction(String value) throws BuildException {
            if (actionMap.get(value) == null) {
                throw new BuildException("Invalid action, must be one of " + actionMap);
            }
            action = value;
        }

        /**
         * A alphanumeric custom name for the snapshot; optional.
         */
        public void setParam(String value) {
            param = value;
        }

        // return <name>:<event>:<action>[:param]
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(name).append(":"); //@todo name must not be null, check for it
            buf.append(eventMap.get(event)).append(":");
            buf.append(actionMap.get(action));
            if (param != null) {
                buf.append(":").append(param);
            }
            return buf.toString();
        }
    }

    /** mapping of actions to cryptic command line mnemonics */
    private static final Hashtable actionMap = new Hashtable(3);

    /** mapping of events to cryptic command line mnemonics */
    private static final Hashtable eventMap = new Hashtable(3);

    static {
        actionMap.put("enter", "E");
        actionMap.put("exit", "X");
        // clear|pause|resume|snapshot|suspend|exit
        eventMap.put("clear", "C");
        eventMap.put("pause", "P");
        eventMap.put("resume", "R");
        eventMap.put("snapshot", "S");
        eventMap.put("suspend", "A");
        eventMap.put("exit", "X");
    }

}
