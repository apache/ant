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
package org.apache.tools.ant.util;

import java.io.File;
import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedInputStream;
import java.util.Arrays;
import java.util.Vector;
import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.TimeComparison;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.Touchable;
import org.apache.tools.ant.types.resources.selectors.Not;
import org.apache.tools.ant.types.resources.selectors.Date;
import org.apache.tools.ant.types.resources.selectors.Exists;
import org.apache.tools.ant.types.resources.selectors.ResourceSelector;
import org.apache.tools.ant.types.selectors.SelectorUtils;

// CheckStyle:HideUtilityClassConstructorCheck OFF - bc

/**
 * This class provides utility methods to process Resources.
 *
 * @since Ant 1.5.2
 */
public class ResourceUtils {

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
                                                            final long granularity) {
        if (source.size() == 0) {
            logTo.log("No sources found.", Project.MSG_VERBOSE);
            return Resources.NONE;
        }
        source = Union.getInstance(source);
        logFuture(logTo, source, granularity);

        Union result = new Union();
        for (Iterator iter = source.iterator(); iter.hasNext();) {
            final Resource sr = (Resource) iter.next();
            String srName = sr.getName();
            srName = srName == null
                ? srName : srName.replace('/', File.separatorChar);

            String[] targetnames = null;
            try {
                targetnames = mapper.mapFileName(srName);
            } catch (Exception e) {
                logTo.log("Caught " + e + " mapping resource " + sr,
                    Project.MSG_VERBOSE);
            }
            if (targetnames == null || targetnames.length == 0) {
                logTo.log(sr + " skipped - don\'t know how to handle it",
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
            r.add(new ResourceSelector() {
                public boolean isSelected(Resource target) {
                    /* Extra I/O, probably wasted:
                    if (target.isDirectory()) {
                        return false;
                    }
                     */
                    return SelectorUtils.isOutOfDate(sr, target, granularity);
                }
            });
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

    /**
     * Convenience method to copy content from one Resource to another.
     * No filtering is performed.
     *
     * @param source the Resource to copy from.
     *                   Must not be <code>null</code>.
     * @param dest   the Resource to copy to.
     *                 Must not be <code>null</code>.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.7
     */
    public static void copyResource(Resource source, Resource dest) throws IOException {
        copyResource(source, dest, null);
    }

    /**
     * Convenience method to copy content from one Resource to another.
     * No filtering is performed.
     *
     * @param source the Resource to copy from.
     *                   Must not be <code>null</code>.
     * @param dest   the Resource to copy to.
     *                 Must not be <code>null</code>.
     * @param project the project instance.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.7
     */
    public static void copyResource(Resource source, Resource dest, Project project)
        throws IOException {
        copyResource(source, dest, null, null, false,
                     false, null, null, project);
    }

    // CheckStyle:ParameterNumberCheck OFF - bc
    /**
     * Convenience method to copy content from one Resource to another
     * specifying whether token filtering must be used, whether filter chains
     * must be used, whether newer destination files may be overwritten and
     * whether the last modified time of <code>dest</code> file should be made
     * equal to the last modified time of <code>source</code>.
     *
     * @param source the Resource to copy from.
     *                   Must not be <code>null</code>.
     * @param dest   the Resource to copy to.
     *                 Must not be <code>null</code>.
     * @param filters the collection of filters to apply to this copy.
     * @param filterChains filterChains to apply during the copy.
     * @param overwrite Whether or not the destination Resource should be
     *                  overwritten if it already exists.
     * @param preserveLastModified Whether or not the last modified time of
     *                             the destination Resource should be set to that
     *                             of the source.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.7
     */
    public static void copyResource(Resource source, Resource dest,
                             FilterSetCollection filters, Vector filterChains,
                             boolean overwrite, boolean preserveLastModified,
                             String inputEncoding, String outputEncoding,
                             Project project)
        throws IOException {
        if (!overwrite) {
            long slm = source.getLastModified();
            if (dest.isExists() && slm != 0
                && dest.getLastModified() > slm) {
                return;
            }
        }
        final boolean filterSetsAvailable = (filters != null
                                             && filters.hasFilters());
        final boolean filterChainsAvailable = (filterChains != null
                                               && filterChains.size() > 0);
        if (filterSetsAvailable) {
            BufferedReader in = null;
            BufferedWriter out = null;
            try {
                InputStreamReader isr = null;
                if (inputEncoding == null) {
                    isr = new InputStreamReader(source.getInputStream());
                } else {
                    isr = new InputStreamReader(source.getInputStream(),
                                                inputEncoding);
                }
                in = new BufferedReader(isr);
                OutputStreamWriter osw = null;
                if (outputEncoding == null) {
                    osw = new OutputStreamWriter(dest.getOutputStream());
                } else {
                    osw = new OutputStreamWriter(dest.getOutputStream(),
                                                 outputEncoding);
                }
                out = new BufferedWriter(osw);
                if (filterChainsAvailable) {
                    ChainReaderHelper crh = new ChainReaderHelper();
                    crh.setBufferSize(FileUtils.BUF_SIZE);
                    crh.setPrimaryReader(in);
                    crh.setFilterChains(filterChains);
                    crh.setProject(project);
                    Reader rdr = crh.getAssembledReader();
                    in = new BufferedReader(rdr);
                }
                LineTokenizer lineTokenizer = new LineTokenizer();
                lineTokenizer.setIncludeDelims(true);
                String newline = null;
                String line = lineTokenizer.getToken(in);
                while (line != null) {
                    if (line.length() == 0) {
                        // this should not happen, because the lines are
                        // returned with the end of line delimiter
                        out.newLine();
                    } else {
                        newline = filters.replaceTokens(line);
                        out.write(newline);
                    }
                    line = lineTokenizer.getToken(in);
                }
            } finally {
                FileUtils.close(out);
                FileUtils.close(in);
            }
        } else if (filterChainsAvailable
                   || (inputEncoding != null
                       && !inputEncoding.equals(outputEncoding))
                   || (inputEncoding == null && outputEncoding != null)) {
            BufferedReader in = null;
            BufferedWriter out = null;
            try {
                InputStreamReader isr = null;
                if (inputEncoding == null) {
                    isr = new InputStreamReader(source.getInputStream());
                } else {
                    isr = new InputStreamReader(source.getInputStream(),
                                                inputEncoding);
                }
                in = new BufferedReader(isr);
                OutputStreamWriter osw = null;
                if (outputEncoding == null) {
                    osw = new OutputStreamWriter(dest.getOutputStream());
                } else {
                    osw = new OutputStreamWriter(dest.getOutputStream(),
                                                 outputEncoding);
                }
                out = new BufferedWriter(osw);
                if (filterChainsAvailable) {
                    ChainReaderHelper crh = new ChainReaderHelper();
                    crh.setBufferSize(FileUtils.BUF_SIZE);
                    crh.setPrimaryReader(in);
                    crh.setFilterChains(filterChains);
                    crh.setProject(project);
                    Reader rdr = crh.getAssembledReader();
                    in = new BufferedReader(rdr);
                }
                char[] buffer = new char[FileUtils.BUF_SIZE];
                while (true) {
                    int nRead = in.read(buffer, 0, buffer.length);
                    if (nRead == -1) {
                        break;
                    }
                    out.write(buffer, 0, nRead);
                }
            } finally {
                FileUtils.close(out);
                FileUtils.close(in);
            }
        } else {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = source.getInputStream();
                out = dest.getOutputStream();

                byte[] buffer = new byte[FileUtils.BUF_SIZE];
                int count = 0;
                do {
                    out.write(buffer, 0, count);
                    count = in.read(buffer, 0, buffer.length);
                } while (count != -1);
            } finally {
                FileUtils.close(out);
                FileUtils.close(in);
            }
        }
        if (preserveLastModified && dest instanceof Touchable) {
            setLastModified((Touchable) dest, source.getLastModified());
        }
    }
    // CheckStyle:ParameterNumberCheck ON

    /**
     * Set the last modified time of an object implementing
     * org.apache.tools.ant.types.resources.Touchable .
     *
     * @param t the Touchable whose modified time is to be set.
     * @param time the time to which the last modified time is to be set.
     *             if this is -1, the current time is used.
     * @since Ant 1.7
     */
    public static void setLastModified(Touchable t, long time) {
        t.touch((time < 0) ? System.currentTimeMillis() : time);
    }

    /**
     * Compares the contents of two Resources.
     *
     * @param r1 the Resource whose content is to be compared.
     * @param r2 the other Resource whose content is to be compared.
     * @param text true if the content is to be treated as text and
     *        differences in kind of line break are to be ignored.
     *
     * @return true if the content of the Resources is the same.
     *
     * @throws IOException if the Resources cannot be read.
     * @since Ant 1.7
     */
    public static boolean contentEquals(Resource r1, Resource r2, boolean text) throws IOException {
        if (r1.isExists() != r2.isExists()) {
            return false;
        }
        if (!r1.isExists()) {
            // two not existing files are equal
            return true;
        }
        // should the following two be switched?  If r1 and r2 refer to the same file,
        // isn't their content equal regardless of whether that file is a directory?
        if (r1.isDirectory() || r2.isDirectory()) {
            // don't want to compare directory contents for now
            return false;
        }
        if (r1.equals(r2)) {
            return true;
        }
        if (!text) {
            long s1 = r1.getSize();
            long s2 = r2.getSize();
            if (s1 != Resource.UNKNOWN_SIZE && s2 != Resource.UNKNOWN_SIZE
                    && s1 != s2) {
                return false;
            }
        }
        return compareContent(r1, r2, text) == 0;
    }

    /**
     * Compare the content of two Resources. A nonexistent Resource's
     * content is "less than" that of an existing Resource; a directory-type
     * Resource's content is "less than" that of a file-type Resource.
     * @param r1 the Resource whose content is to be compared.
     * @param r2 the other Resource whose content is to be compared.
     * @param text true if the content is to be treated as text and
     *        differences in kind of line break are to be ignored.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     * @throws IOException if the Resources cannot be read.
     * @since Ant 1.7
     */
    public static int compareContent(Resource r1, Resource r2, boolean text) throws IOException {
        if (r1.equals(r2)) {
            return 0;
        }
        boolean e1 = r1.isExists();
        boolean e2 = r2.isExists();
        if (!(e1 || e2)) {
            return 0;
        }
        if (e1 != e2) {
            return e1 ? 1 : -1;
        }
        boolean d1 = r1.isDirectory();
        boolean d2 = r2.isDirectory();
        if (d1 && d2) {
            return 0;
        }
        if (d1 || d2) {
            return d1 ? -1 : 1;
        }
        return text ? textCompare(r1, r2) : binaryCompare(r1, r2);
    }

    /**
     * Binary compares the contents of two Resources.
     * <p>
     * simple but sub-optimal comparision algorithm. written for working
     * rather than fast. Better would be a block read into buffers followed
     * by long comparisions apart from the final 1-7 bytes.
     * </p>
     *
     * @param r1 the Resource whose content is to be compared.
     * @param r2 the other Resource whose content is to be compared.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     * @throws IOException if the Resources cannot be read.
     * @since Ant 1.7
     */
    private static int binaryCompare(Resource r1, Resource r2) throws IOException {
        InputStream in1 = null;
        InputStream in2 = null;
        try {
            in1 = new BufferedInputStream(r1.getInputStream());
            in2 = new BufferedInputStream(r2.getInputStream());

            for (int b1 = in1.read(); b1 != -1; b1 = in1.read()) {
                int b2 = in2.read();
                if (b1 != b2) {
                    return b1 > b2 ? 1 : -1;
                }
            }
            return in2.read() == -1 ? 0 : -1;
        } finally {
            FileUtils.close(in1);
            FileUtils.close(in2);
        }
    }

    /**
     * Text compares the contents of two Resources.
     * Ignores different kinds of line endings.
     * @param r1 the Resource whose content is to be compared.
     * @param r2 the other Resource whose content is to be compared.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     * @throws IOException if the Resources cannot be read.
     * @since Ant 1.7
     */
    private static int textCompare(Resource r1, Resource r2) throws IOException {
        BufferedReader in1 = null;
        BufferedReader in2 = null;
        try {
            in1 = new BufferedReader(new InputStreamReader(r1.getInputStream()));
            in2 = new BufferedReader(new InputStreamReader(r2.getInputStream()));

            String expected = in1.readLine();
            while (expected != null) {
                String actual = in2.readLine();
                if (!expected.equals(actual)) {
                    return expected.compareTo(actual);
                }
                expected = in1.readLine();
            }
            return in2.readLine() == null ? 0 : -1;
        } finally {
            FileUtils.close(in1);
            FileUtils.close(in2);
        }
    }

    /**
     * Log which Resources (if any) have been modified in the future.
     * @param logTo the ProjectComponent to do the logging.
     * @param rc the collection of Resources to check.
     * @param granularity the timestamp granularity to use.
     * @since Ant 1.7
     */
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
