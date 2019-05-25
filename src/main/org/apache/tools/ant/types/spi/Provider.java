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
package org.apache.tools.ant.types.spi;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ProjectComponent;

/**
 * ANT Jar-Task SPI extension
 * This class corresponds to the nested element
 * &lt;provider type="type"&gt; in the &lt;service type=""&gt;
 * nested element of the jar task.
 * @see <a href="https://issues.apache.org/bugzilla/show_bug.cgi?id=31520">
 * https://issues.apache.org/bugzilla/show_bug.cgi?id=31520</a>
 */
public class Provider extends ProjectComponent {
    private String type;

    /**
     * @return the class name for
     */
    public String getClassName() {
        return type;
    }

    /**
     * Set the provider classname.
     * @param type the value to set.
     */
    public void setClassName(String type) {
        this.type = type;
    }

    /**
     * Check if the component has been configured correctly.
     */
    public void check() {
        if (type == null) {
            throw new BuildException(
                "classname attribute must be set for provider element",
                getLocation());
        }
        if (type.isEmpty()) {
            throw new BuildException(
                "Invalid empty classname", getLocation());
        }
    }
}
