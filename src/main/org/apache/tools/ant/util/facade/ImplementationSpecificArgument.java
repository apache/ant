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

package org.apache.tools.ant.util.facade;

import org.apache.tools.ant.types.Commandline;

/**
 * Extension of Commandline.Argument with a new attribute that choses
 * a specific implementation of the facade.
 *
 * @author Stefan Bodewig
 *
 * @version $Revision$
 *
 * @since Ant 1.5
 */
public class ImplementationSpecificArgument extends Commandline.Argument {
    private String impl;

    public ImplementationSpecificArgument() {
        super();
    }

    public void setImplementation(String impl) {
        this.impl = impl;
    }

    public final String[] getParts(String chosenImpl) {
        if (impl == null || impl.equals(chosenImpl)) {
            return super.getParts();
        } else {
            return new String[0];
        }
    }
}
