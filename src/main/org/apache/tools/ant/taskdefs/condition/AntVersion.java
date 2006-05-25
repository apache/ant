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

/**
 * An antversion condition
 * @since ant 1.7
 */
public class AntVersion implements Condition {
    
    private String atLeast = null;
    private String exactly = null;
    
    public boolean eval() throws BuildException {
        validate();
        float actual = getVersion();
        if (null != atLeast) {
            if (actual >= Versions.getVersion(atLeast)) {
                return true;
            } else {
                return false;
            }
        }
        if (null != exactly) {
            if (actual == Versions.getVersion(exactly)) {
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
    
    private float getVersion() {
        Project p = new Project();
        p.init();
        String versionString = p.getProperty("ant.version");
        String v = versionString.substring(versionString.indexOf("Ant version")+12, 
                versionString.indexOf("compiled on")-1);
        return Versions.getVersion(v);
    }
    
    private static class Versions {
        static float getVersion(String vs) {
            if (vs.equals("1.1"))       return 11f;
            if (vs.equals("1.2"))       return 12f;
            if (vs.equals("1.3"))       return 13f;
            if (vs.equals("1.4"))       return 14f;
            if (vs.equals("1.4.1"))     return 14.1f;
            if (vs.equals("1.5"))       return 15f;
            if (vs.equals("1.5.1"))     return 15.1f;
            if (vs.equals("1.5.2"))     return 15.2f;
            if (vs.equals("1.5.3"))     return 15.3f;
            if (vs.equals("1.5.4"))     return 15.4f;
            if (vs.equals("1.5alpha"))  return 15.880f;
            if (vs.equals("1.6beta1"))  return 15.991f;
            if (vs.equals("1.6beta2"))  return 15.992f;
            if (vs.equals("1.6beta3"))  return 15.993f;
            if (vs.equals("1.6"))       return 16f;
            if (vs.equals("1.6.0"))     return 16f;
            if (vs.equals("1.6.1"))     return 16.1f;
            if (vs.equals("1.6.2"))     return 16.2f;
            if (vs.equals("1.6.3"))     return 16.3f;
            if (vs.equals("1.6.4"))     return 16.4f;
            if (vs.equals("1.6.5"))     return 16.5f;
            if (vs.equals("1.7alpha"))  return 16.880f;
            if (vs.equals("1.7beta"))   return 16.990f;
            if (vs.equals("1.7"))       return 17f;
            if (vs.equals("1.7.0"))     return 17f;
            if (vs.equals("1.7.1"))     return 17.1f;
            if (vs.equals("1.7.2"))     return 17.2f;
            return 0f;
        }
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