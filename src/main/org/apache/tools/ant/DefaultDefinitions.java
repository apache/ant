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

package org.apache.tools.ant;

/**
 * Default definitions.
 * @since Ant 1.9.1
 */
public final class DefaultDefinitions {
    private static final String IF_NAMESPACE = "ant:if";
    private static final String UNLESS_NAMESPACE = "ant:unless";

    private final ComponentHelper componentHelper;

    /**
     * Create a default definitions object.
     * @param componentHelper the componenthelper to initialize.
     */
    public DefaultDefinitions(ComponentHelper componentHelper) {
        this.componentHelper = componentHelper;
    }

    /**
     * Register the definitions.
     */
    public void execute() {
        attributeNamespaceDef(IF_NAMESPACE);
        attributeNamespaceDef(UNLESS_NAMESPACE);

        ifUnlessDef("true", "IfTrueAttribute");
        ifUnlessDef("set", "IfSetAttribute");
        ifUnlessDef("blank", "IfBlankAttribute");
    }

    private void attributeNamespaceDef(String ns) {
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(ProjectHelper.nsToComponentName(ns));
        def.setClassName(MagicNames.ANT_CORE_PACKAGE + ".attribute.AttributeNamespace");
        def.setClassLoader(getClass().getClassLoader());
        def.setRestrict(true);
        componentHelper.addDataTypeDefinition(def);
    }

    private void ifUnlessDef(String name, String base) {
        String classname = MagicNames.ANT_CORE_PACKAGE + ".attribute." + base;
        componentDef(IF_NAMESPACE, name, classname);
        componentDef(UNLESS_NAMESPACE, name, classname + "$Unless");
    }

    private void componentDef(String ns, String name, String classname) {
        AntTypeDefinition def = new AntTypeDefinition();
        def.setName(ProjectHelper.genComponentName(ns, name));
        def.setClassName(classname);
        def.setClassLoader(getClass().getClassLoader());
        def.setRestrict(true);
        componentHelper.addDataTypeDefinition(def);
    }
}
