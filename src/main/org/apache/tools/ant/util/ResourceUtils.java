/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Ant" and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.tools.ant.util;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.selectors.SelectorUtils;

import java.io.File;
import java.util.Vector;

/**
 * this class provides utility methods to process resources
 *
 * @author <a href="mailto:levylambert@tiscali-dsl.de">Antoine Levy-Lambert</a>
 * @since Ant 1.5.2
 */
public class ResourceUtils {

    /**
     * tells which source files should be reprocessed based on the
     * last modification date of target files
     * @param logTo where to send (more or less) interesting output
     * @param source array of resources bearing relative path and last
     * modification date
     * @param mapper filename mapper indicating how to find the target
     * files
     * @param targets object able to map as a resource a relative path
     * at <b>destination</b>
     * @return array containing the source files which need to be
     * copied or processed, because the targets are out of date or do
     * not exist
     */
    public static Resource[] selectOutOfDateSources(ProjectComponent logTo,
                                                    Resource[] source,
                                                    FileNameMapper mapper,
                                                    ResourceFactory targets) {
        return selectOutOfDateSources(logTo, source, mapper, targets,
                                      FileUtils.newFileUtils()
                                      .getFileTimestampGranularity());
    }

    /**
     * tells which source files should be reprocessed based on the
     * last modification date of target files
     * @param logTo where to send (more or less) interesting output
     * @param source array of resources bearing relative path and last
     * modification date
     * @param mapper filename mapper indicating how to find the target
     * files
     * @param targets object able to map as a resource a relative path
     * at <b>destination</b>
     * @param granularity The number of milliseconds leeway to give
     * before deciding a target is out of date.
     * @return array containing the source files which need to be
     * copied or processed, because the targets are out of date or do
     * not exist
     * @since Ant 1.6
     */
    public static Resource[] selectOutOfDateSources(ProjectComponent logTo,
                                                    Resource[] source,
                                                    FileNameMapper mapper,
                                                    ResourceFactory targets,
                                                    long granularity) {
        long now = (new java.util.Date()).getTime() + granularity;

        Vector vresult = new Vector();
        for (int counter = 0; counter < source.length; counter++) {
            if (source[counter].getLastModified() > now) {
                logTo.log("Warning: " + source[counter].getName()
                         + " modified in the future.",
                         Project.MSG_WARN);
            }

            String[] targetnames =
                mapper.mapFileName(source[counter].getName()
                                   .replace('/', File.separatorChar));
            if (targetnames != null) {
                boolean added = false;
                StringBuffer targetList = new StringBuffer();
                for (int ctarget = 0; !added && ctarget < targetnames.length;
                     ctarget++) {
                    Resource atarget =
                        targets.getResource(targetnames[ctarget]
                                            .replace(File.separatorChar, '/'));
                    // if the target does not exist, or exists and
                    // is older than the source, then we want to
                    // add the resource to what needs to be copied
                    if (!atarget.isExists()) {
                        logTo.log(source[counter].getName() + " added as "
                                  + atarget.getName()
                                  + " doesn\'t exist.", Project.MSG_VERBOSE);
                        vresult.addElement(source[counter]);
                        added = true;
                    } else if (!atarget.isDirectory() && 
                               SelectorUtils.isOutOfDate(source[counter], 
                                                         atarget,
                                                         (int) granularity)) {
                        logTo.log(source[counter].getName() + " added as "
                                  + atarget.getName()
                                  + " is outdated.", Project.MSG_VERBOSE);
                        vresult.addElement(source[counter]);
                        added = true;
                    } else {
                        if (targetList.length() > 0) {
                            targetList.append(", ");
                        }
                        targetList.append(atarget.getName());
                    }
                }

                if (!added) {
                    logTo.log(source[counter].getName()
                              + " omitted as " + targetList.toString()
                              + (targetnames.length == 1 ? " is" : " are ")
                              + " up to date.", Project.MSG_VERBOSE);
                }
            } else {
                logTo.log(source[counter].getName()
                          + " skipped - don\'t know how to handle it",
                          Project.MSG_VERBOSE);
            }
        }
        Resource[] result = new Resource[vresult.size()];
        vresult.copyInto(result);
        return result;
    }
}
