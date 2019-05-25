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
 * Instances of classes that implement this interface can register
 * to be also notified when things happened during a subbuild.
 *
 * <p>A subbuild is a separate project instance created by the
 * <code>&lt;ant&gt;</code> task family.  These project instances will
 * never fire the buildStarted and buildFinished events, but they will
 * fire subBuildStarted/ and subBuildFinished.  The main project
 * instance - the one created by running Ant in the first place - will
 * never invoke one of the methods of this interface.</p>
 *
 * @see BuildEvent
 * @see Project#addBuildListener(BuildListener)
 *
 * @since Ant 1.6.2
 */
public interface SubBuildListener extends BuildListener {

    /**
     * Signals that a subbuild has started. This event
     * is fired before any targets have started.
     *
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
     */
    void subBuildStarted(BuildEvent event);

    /**
     * Signals that the last target has finished. This event
     * will still be fired if an error occurred during the build.
     *
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getException()
     */
    void subBuildFinished(BuildEvent event);
}
