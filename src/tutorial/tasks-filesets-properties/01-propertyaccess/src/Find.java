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

public class Find extends Task {

    private String property;
    private String value;
    private String print;

    public void setProperty(String property) {
        this.property = property;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setPrint(String print) {
        this.print = print;
    }

    public void execute() {
        if (print != null) {
            String propValue = getProject().getProperty(print);
            log(propValue);
        } else {
            if (property == null) throw new BuildException("property not set");
            if (value    == null) throw new BuildException("value not set");
            getProject().setNewProperty(property, value);
        }
    }

}
