/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.tools.ant.taskdefs;

import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.GreedyInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.input.MultipleChoiceInputRequest;
import org.apache.tools.ant.input.PropertyFileInputHandler;
import org.apache.tools.ant.input.SecureInputHandler;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.util.ClasspathUtils;
import org.apache.tools.ant.util.StringUtils;

/**
 * Reads an input line from the console.
 *
 * @since Ant 1.5
 *
 * @ant.task category="control"
 */
public class Input extends Task {

    /**
     * Represents an InputHandler.
     */
    public class Handler extends DefBase {

        private String refid = null;
        private HandlerType type = null;
        private String classname = null;

        /**
         * Specify that the handler is a reference on the project;
         * this allows the use of a custom inputhandler.
         * @param refid the String refid.
         */
        public void setRefid(final String refid) {
            this.refid = refid;
        }

        /**
         * Get the refid of this Handler.
         * @return String refid.
         */
        public String getRefid() {
            return refid;
        }

        /**
         * Set the InputHandler classname.
         * @param classname the String classname.
         */
        public void setClassname(final String classname) {
            this.classname = classname;
        }

        /**
         * Get the classname of the InputHandler.
         * @return String classname.
         */
        public String getClassname() {
            return classname;
        }

        /**
         * Set the handler type.
         * @param type a HandlerType.
         */
        public void setType(final HandlerType type) {
            this.type = type;
        }

        /**
         * Get the handler type.
         * @return a HandlerType object.
         */
        public HandlerType getType() {
            return type;
        }

        private InputHandler getInputHandler() {
            if (type != null) {
               return type.getInputHandler();
            }
            if (refid != null) {
               try {
                   return getProject().getReference(refid);
               } catch (final ClassCastException e) {
                   throw new BuildException(
                       refid + " does not denote an InputHandler", e);
               }
            }
            if (classname != null) {
               return ClasspathUtils.newInstance(classname,
                   createLoader(), InputHandler.class);
            }
            throw new BuildException(
                "Must specify refid, classname or type");
        }
    }

    /**
     * EnumeratedAttribute representing the built-in input handler types:
     * "default", "propertyfile", "greedy", "secure" (since Ant 1.8).
     */
    public static class HandlerType extends EnumeratedAttribute {
        private static final String[] VALUES = {"default", "propertyfile", "greedy", "secure"};

        private static final InputHandler[] HANDLERS = {new DefaultInputHandler(),
                new PropertyFileInputHandler(),
                new GreedyInputHandler(),
                new SecureInputHandler()};

        /** {@inheritDoc} */
        @Override
        public String[] getValues() {
            return VALUES;
        }

        private InputHandler getInputHandler() {
            return HANDLERS[getIndex()];
        }
    }

    private String validargs = null;
    private String message = "";
    private String addproperty = null;
    private String defaultvalue = null;
    private Handler handler = null;
    private boolean messageAttribute;

    /**
     * Defines valid input parameters as comma separated strings. If set, input
     * task will reject any input not defined as accepted and requires the user
     * to reenter it. Validargs are case sensitive. If you want 'a' and 'A' to
     * be accepted you need to define both values as accepted arguments.
     *
     * @param validargs A comma separated String defining valid input args.
     */
    public void setValidargs(final String validargs) {
        this.validargs = validargs;
    }

    /**
     * Defines the name of a property to be created from input. Behaviour is
     * according to property task which means that existing properties
     * cannot be overridden.
     *
     * @param addproperty Name for the property to be created from input
     */
    public void setAddproperty(final String addproperty) {
        this.addproperty = addproperty;
    }

    /**
     * Sets the Message which gets displayed to the user during the build run.
     * @param message The message to be displayed.
     */
    public void setMessage(final String message) {
        this.message = message;
        messageAttribute = true;
    }

    /**
     * Defines the default value of the property to be created from input.
     * Property value will be set to default if not input is received.
     *
     * @param defaultvalue Default value for the property if no input
     * is received
     */
    public void setDefaultvalue(final String defaultvalue) {
        this.defaultvalue = defaultvalue;
    }

    /**
     * Set a multiline message.
     * @param msg The message to be displayed.
     */
    public void addText(final String msg) {
        if (messageAttribute && msg.trim().isEmpty()) {
            return;
        }
        message += getProject().replaceProperties(msg);
    }

    /**
     * Actual method executed by ant.
     * @throws BuildException on error
     */
    @Override
    public void execute() throws BuildException {
        if (addproperty != null
            && getProject().getProperty(addproperty) != null) {
            log("skipping " + getTaskName() + " as property " + addproperty
                + " has already been set.");
            return;
        }

        InputRequest request = null;
        if (validargs != null) {
            final List<String> accept = StringUtils.split(validargs, ',');
            request = new MultipleChoiceInputRequest(message, accept);
        } else {
            request = new InputRequest(message);
        }
        request.setDefaultValue(defaultvalue);

        final InputHandler h = handler == null
            ? getProject().getInputHandler()
            : handler.getInputHandler();

        h.handleInput(request);

        String value = request.getInput();
        if ((value == null || value.trim().isEmpty())
            && defaultvalue != null) {
            value = defaultvalue;
        }
        if (addproperty != null && value != null) {
            getProject().setNewProperty(addproperty, value);
        }
    }

    /**
     * Create a nested handler element.
     * @return a Handler for this Input task.
     */
    public Handler createHandler() {
        if (handler != null) {
            throw new BuildException(
                "Cannot define > 1 nested input handler");
        }
        handler = new Handler();
        return handler;
    }

}
