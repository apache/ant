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

package org.apache.tools.ant.listener;

import org.apache.tools.ant.DefaultLogger;

/**
 * Like a normal logger, except with timed outputs
 */
public class TimestampedLogger extends DefaultLogger {

    /**
     * what appears between the old message and the new
     */
    public static final String SPACER = " - at ";

    /**
     * This is an override point: the message that indicates whether a build failed.
     * Subclasses can change/enhance the message.
     *
     * @return The classic "BUILD FAILED" plus a timestamp
     */
    @Override
    protected String getBuildFailedMessage() {
        return super.getBuildFailedMessage() + SPACER + getTimestamp();
    }

    /**
     * This is an override point: the message that indicates that a build succeeded.
     * Subclasses can change/enhance the message.
     *
     * @return The classic "BUILD SUCCESSFUL" plus a timestamp
     */
    @Override
    protected String getBuildSuccessfulMessage() {
        return super.getBuildSuccessfulMessage() + SPACER + getTimestamp();
    }

}
