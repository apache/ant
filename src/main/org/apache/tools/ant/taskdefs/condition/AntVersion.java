/*
 * Copyright  2006 The Apache Software Foundation
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

package org.apache.tools.ant.taskdefs.condition;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.DeweyDecimal;

/**
 * An antversion condition
 * @since ant 1.7
 */
public class AntVersion implements Condition {
    
    private String atLeast = null;
    private String exactly = null;
    
    public boolean eval() throws BuildException {
        validate();
        DeweyDecimal actual = getVersion();
        if (null != atLeast) {
            if (actual.isGreaterThanOrEqual(new DeweyDecimal(atLeast))) {
                return true;
            } else {
                return false;
            }
        }
        if (null != exactly) {
            if (actual.isEqual(new DeweyDecimal(exactly))) {
                return true;
            } else {
                return false;
            }
        }
        //default
        return false;
    }
    
    private void validate() throws BuildException {
        if (atLeast != null && exactly != null) {
            throw new BuildException("Only one of atleast or exactly may be set.");
        }
        if (null == atLeast && null == exactly) {
            throw new BuildException("One of atleast or exactly must be set.");
        }
    }
    
    private DeweyDecimal getVersion() {
        Project p = new Project();
        p.init();
        String versionString = p.getProperty("ant.version");
        String v = versionString.substring(versionString.indexOf("Ant version")+12, 
                versionString.indexOf("compiled on")-1);
        char[] cs = v.toCharArray();
        int end = cs.length;
        for (int i = cs.length; i > 0; i--) {
            if (!Character.isLetter(cs[i-1])) {
                end = i;
                break;
            }
        }
        v = v.substring(0, end);
        return new DeweyDecimal(v);
    }
    
    public String getAtLeast() {
        return atLeast;
    }
    
    public void setAtLeast(String atLeast) {
        this.atLeast = atLeast;
    }
    
    public String getExactly() {
        return exactly;
    }
    
    public void setExactly(String exactly) {
        this.exactly = exactly;
    }   
}