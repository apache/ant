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
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.util.ArrayList;
import java.util.List;

/**
 * The task of the tutorial.
 * Prints a message or lets the build fail.
 * @author Jan Matérne
 * @since 2003-08-19
 */
public class HelloWorld extends Task {


    /** The message to print. As attribute. */
    String message;
    public void setMessage(String msg) {
        message = msg;
    }

    /** Should the build fail? Defaults to <i>false</i>. As attribute. */
    boolean fail = false;
    public void setFail(boolean b) {
        fail = b;
    }

    /** Support for nested text. */
    public void addText(String text) {
        message = text;
    }


    /** Do the work. */
    public void execute() {
        // handle attribute 'fail'
        if (fail) throw new BuildException("Fail requested.");

        // handle attribute 'message' and nested text
        if (message != null) log(message);

        // handle nested elements
        for (Message msg : messages) {
            log(msg.getMsg());
        }
    }


    /** Store nested 'message's. */
    List<Message> messages = new ArrayList<>();

    /** Factory method for creating nested 'message's. */
    public Message createMessage() {
        Message msg = new Message();
        messages.add(msg);
        return msg;
    }

    /** A nested 'message'. */
    public class Message {
        // Bean constructor
        public Message() {}

        /** Message to print. */
        String msg;
        public void setMsg(String msg) { this.msg = msg; }
        public String getMsg() { return msg; }
    }

}
