/*
 * Copyright  2002,2004 Apache Software Foundation
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
package org.apache.tools.ant.types;

/**
 * A parameter is composed of a name, type and value.
 *
 * @author Magesh Umasankar
 */
public final class Parameter {
    private String name = null;
    private String type = null;
    private String value = null;

    public final void setName(final String name) {
        this.name = name;
    }

    public final void setType(final String type) {
        this.type = type;
    }

    public final void setValue(final String value) {
        this.value = value;
    }

    public final String getName() {
        return name;
    }

    public final String getType() {
        return type;
    }

    public final String getValue() {
        return value;
    }
}
