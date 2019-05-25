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

import org.apache.tools.ant.AntTypeDefinition;
import org.apache.tools.ant.ComponentHelper;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.attribute.AttributeNamespace;

/**
 * Definition to allow the URI to be considered for
 * Ant attributes.
 *
 * @since Ant 1.9.1
 */
public final class AttributeNamespaceDef  extends AntlibDefinition {

    /**
     * Run the definition.
     * This registers the XML namespace (URI) as a namespace for
     * attributes.
     */
    public void execute() {
        String componentName = ProjectHelper.nsToComponentName(
            getURI());
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(componentName);
        def.setClassName(AttributeNamespace.class.getName());
        def.setClass(AttributeNamespace.class);
        def.setRestrict(true);
        def.setClassLoader(AttributeNamespace.class.getClassLoader());
        ComponentHelper.getComponentHelper(getProject())
            .addDataTypeDefinition(def);
    }
}
