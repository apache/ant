/*
 * Copyright 2003-2005 The Apache Software Foundation
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
package org.apache.tools.ant.util;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.TimeComparison;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.selectors.Or;
import org.apache.tools.ant.types.resources.selectors.And;
import org.apache.tools.ant.types.resources.selectors.Not;
import org.apache.tools.ant.types.resources.selectors.Date;
import org.apache.tools.ant.types.resources.selectors.Type;
import org.apache.tools.ant.types.resources.selectors.Exists;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.types.selectors.SelectorUtils;

/**
 * This class provides utility methods to process Resources.
 *
 * @since Ant 1.5.2
 */
public class ResourceUtils {

    private static class Outdated implements ResourceSelector {
        private Resource control;
        private long granularity;
        private Outdated(Resource control, long granularity) {
            this.control = control;
            this.granularity = granularity;
        }
        public boolean isSelected(Resource r) {
            return SelectorUtils.isOutOfDate(control, r, granularity);
        }
    }
    /** Utilities used for file operations */
    private static final FileUtils FILE_UTILS = FileUtils.getFileUtils();

    private static final ResourceSelector NOT_EXISTS = new Not(new Exists());

    /**
     * Tells which source files should be reprocessed based on the
     * last modification date of target files.
     * @param logTo where to send (more or less) interesting output.
     * @param source array of resources bearing relative path and last
     * modification date.
     * @param mapper filename mapper indicating how to find the target
     * files.
     * @param targets object able to map as a resource a relative path
     * at <b>destination</b>.
     * @return array containing the source files which need to be
     * copied or processed, because the targets are out of date or do
     * not exist.
     */
    public static Resource[] selectOutOfDateSources(ProjectComponent logTo,
                                                    Resource[] source,
                                                    FileNameMapper mapper,
                                                    ResourceFactory targets) {
        return selectOutOfDateSources(logTo, source, mapper, targets,
                                      FILE_UTILS.getFileTimestampGranularity());
    }

    /**
     * Tells which source files should be reprocessed based on the
     * last modification date of target files.
     * @param logTo where to send (more or less) interesting output.
     * @param source array of resources bearing relative path and last
     * modification date.
     * @param mapper filename mapper indicating how to find the target
     * files.
     * @param targets object able to map as a resource a relative path
     * at <b>destination</b>.
     * @param granularity The number of milliseconds leeway to give
     * before deciding a target is out of date.
     * @return array containing the source files which need to be
     * copied or processed, because the targets are out of date or do
     * not exist.
     * @since Ant 1.6.2
     */
    public static Resource[] selectOutOfDateSources(ProjectComponent logTo,
                                                    Resource[] source,
                                                    FileNameMapper mapper,
                                                    ResourceFactory targets,
                                                    long granularity) {
        Union u = new Union();
        u.addAll(Arrays.asList(source));
        ResourceCollection rc
            = selectOutOfDateSources(logTo, u, mapper, targets, granularity);
        return rc.size() == 0 ? new Resource[0] : ((Union) rc).listResources();
    }

    /**
     * Tells which sources should be reprocessed based on the
     * last modification date of targets.
     * @param logTo where to send (more or less) interesting output.
     * @param source ResourceCollection.
     * @param mapper filename mapper indicating how to find the target Resources.
     * @param targets object able to map a relative path as a Resource.
     * @param granularity The number of milliseconds leeway to give
     * before deciding a target is out of date.
     * @return ResourceCollection.
     * @since Ant 1.7
     */
    public static ResourceCollection selectOutOfDateSources(ProjectComponent logTo,
                                                            ResourceCollection source,
                                                            FileNameMapper mapper,
                                                            ResourceFactory targets,
                                                            long granularity) {
        if (source.size() == 0) {
            logTo.log("No sources found.", Project.MSG_VERBOSE);
            return Resources.NONE;
        }
        source = Union.getInstance(source);
        logFuture(logTo, source, granularity);

        Union result = new Union();
        for (Iterator iter = source.iterator(); iter.hasNext();) {
            Resource sr = (Resource) iter.next();
            String[] targetnames = mapper.mapFileName(
                sr.getName().replace('/', File.separatorChar));

            if (targetnames == null || targetnames.length == 0) {
                logTo.log(sr.getName()
                      + " skipped - don\'t know how to handle it",
                      Project.MSG_VERBOSE);
                continue;
            }
            Union targetColl = new Union();
            for (int i = 0; i < targetnames.length; i++) {
                targetColl.add(targets.getResource(
                    targetnames[i].replace(File.separatorChar, '/')));
            }
            //find the out-of-date targets:
            Restrict r = new Restrict();
            r.add(new And(new ResourceSelector[] {Type.FILE, new Or(
                new ResourceSelector[] {NOT_EXISTS, new Outdated(sr, granularity)})}));
            r.add(targetColl);
            if (r.size() > 0) {
                result.add(sr);
                Resource t = (Resource) (r.iterator().next());
                logTo.log(sr.getName() + " added as " + t.getName()
                    + (t.isExists() ? " is outdated." : " doesn\'t exist."),
                    Project.MSG_VERBOSE);
                continue;
            }
            //log uptodateness of all targets:
            logTo.log(sr.getName()
                  + " omitted as " + targetColl.toString()
                  + (targetColl.size() == 1 ? " is" : " are ")
                  + " up to date.", Project.MSG_VERBOSE);
        }
        return result;
    }

    private static void logFuture(ProjectComponent logTo,
                                  ResourceCollection rc, long granularity) {
        long now = System.currentTimeMillis() + granularity;
        Date sel = new Date();
        sel.setMillis(now);
        sel.setWhen(TimeComparison.AFTER);
        Restrict future = new Restrict();
        future.add(sel);
        future.add(rc);
        for (Iterator iter = future.iterator(); iter.hasNext();) {
            logTo.log("Warning: " + ((Resource) iter.next()).getName()
                     + " modified in the future.", Project.MSG_WARN);
        }
    }

}
