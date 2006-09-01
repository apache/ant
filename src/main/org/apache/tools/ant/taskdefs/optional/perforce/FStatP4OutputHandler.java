/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
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
package org.apache.tools.ant.taskdefs.optional.perforce;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.oro.text.perl.Perl5Util;

import java.util.ArrayList;

/**
 * FStatP4OutputHandler  - spezialied Perforce output handler
 * able to sort files recognized as managed by Perforce and files not
 * managed by Perforce in the output
 *
 */
class FStatP4OutputHandler extends P4HandlerAdapter {
    private P4Fstat parent;
    private ArrayList existing = new ArrayList();
    private ArrayList nonExisting = new ArrayList();
    private static Perl5Util util = new Perl5Util();

    public FStatP4OutputHandler(P4Fstat parent) {
        this.parent = parent;
    }

    public void process(String line) throws BuildException {
        if (util.match("/^... clientFile (.+)$/", line)) {
            String f = util.group(1);
            existing.add(f);
        } else if (util.match("/^(.+) - no such file/", line)) {
            String f = util.group(1);
            nonExisting.add(f);
        }
        parent.log(parent.util.substitute("s/^.*: //", line),
                   Project.MSG_VERBOSE);
    }

    public ArrayList getExisting() {
        return existing;
    }

    public ArrayList getNonExisting() {
        return nonExisting;
    }
}
