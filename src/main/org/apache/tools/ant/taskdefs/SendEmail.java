/*
 * Copyright  2000-2002,2004 Apache Software Foundation
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

package org.apache.tools.ant.taskdefs;

import org.apache.tools.ant.taskdefs.email.EmailTask;

/**
 * A task to send SMTP email.
 * This task can send mail using either plain
 * text, UU encoding or Mime format mail depending on what is available.
 * Attachments may be sent using nested FileSet
 * elements.

 * @author glenn_twiggs@bmc.com
 * @author Magesh Umasankar
 *
 * @since Ant 1.2
 *
 * @ant.task name="mail" category="network"
 */
public class SendEmail extends EmailTask {
    /**
     * Sets the mailport parameter of this build task.
     * @param value mail port name.
     *
     * @deprecated Use {@link #setMailport(int)} instead.
     */
    public void setMailport(Integer value) {
        setMailport(value.intValue());
    }
}
