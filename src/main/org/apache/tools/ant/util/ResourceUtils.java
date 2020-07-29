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
package org.apache.tools.ant.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Vector;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.filters.util.ChainReaderHelper;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.ResourceFactory;
import org.apache.tools.ant.types.TimeComparison;
import org.apache.tools.ant.types.resources.Appendable;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.types.resources.Resources;
import org.apache.tools.ant.types.resources.Restrict;
import org.apache.tools.ant.types.resources.StringResource;
import org.apache.tools.ant.types.resources.Touchable;
import org.apache.tools.ant.types.resources.Union;
import org.apache.tools.ant.types.resources.selectors.Date;
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

    /**
     * Name of charset "ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1".
     *
     * @deprecated use StandardCharsets.ISO_8859_1
     * @since Ant 1.8.1
     */
    @Deprecated
    public static final String ISO_8859_1 = "ISO-8859-1";

    private static final long MAX_IO_CHUNK_SIZE = 16 * 1024 * 1024L; // 16 MB

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
    public static Resource[] selectOutOfDateSources(final ProjectComponent logTo,
                                                    final Resource[] source,
                                                    final FileNameMapper mapper,
                                                    final ResourceFactory targets) {
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
    public static Resource[] selectOutOfDateSources(final ProjectComponent logTo,
                                                    final Resource[] source,
                                                    final FileNameMapper mapper,
                                                    final ResourceFactory targets,
                                                    final long granularity) {
        final Union u = new Union();
        u.addAll(Arrays.asList(source));
        final ResourceCollection rc
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
    public static ResourceCollection selectOutOfDateSources(final ProjectComponent logTo,
                                                            final ResourceCollection source,
                                                            final FileNameMapper mapper,
                                                            final ResourceFactory targets,
                                                            final long granularity) {
        logFuture(logTo, source, granularity);
        return selectSources(logTo, source, mapper, targets,
            sr -> target -> SelectorUtils.isOutOfDate(sr, target, granularity));
    }

    /**
     * Tells which sources should be reprocessed because the given
     * selector selects at least one target.
     *
     * @param logTo where to send (more or less) interesting output.
     * @param source ResourceCollection.
     * @param mapper filename mapper indicating how to find the target Resources.
     * @param targets object able to map a relative path as a Resource.
     * @param selector returns a selector that is applied to target
     * files.  If it selects at least one target the source will be
     * added to the returned collection.
     * @return ResourceCollection.
     * @since Ant 1.8.0
     */
    public static ResourceCollection selectSources(final ProjectComponent logTo,
                                                   ResourceCollection source,
                                                   final FileNameMapper mapper,
                                                   final ResourceFactory targets,
                                                   final ResourceSelectorProvider selector) {
        if (source.isEmpty()) {
            logTo.log("No sources found.", Project.MSG_VERBOSE);
            return Resources.NONE;
        }
        source = Union.getInstance(source);

        final Union result = new Union();
        for (final Resource sr : source) {
            String srName = sr.getName();
            if (srName != null) {
                srName = srName.replace('/', File.separatorChar);
            }


            String[] targetnames = null;
            try {
                targetnames = mapper.mapFileName(srName);
            } catch (final Exception e) {
                logTo.log("Caught " + e + " mapping resource " + sr,
                    Project.MSG_VERBOSE);
            }
            if (targetnames == null || targetnames.length == 0) {
                logTo.log(sr + " skipped - don't know how to handle it",
                      Project.MSG_VERBOSE);
                continue;
            }
            final Union targetColl = new Union();
            for (String targetname : targetnames) {
                if (targetname == null) {
                    targetname = "(no name)";
                }
                targetColl.add(targets.getResource(
                        targetname.replace(File.separatorChar, '/')));
            }
            //find the out-of-date targets:
            final Restrict r = new Restrict();
            r.add(selector.getTargetSelectorForSource(sr));
            r.add(targetColl);
            if (r.size() > 0) {
                result.add(sr);
                final Resource t = r.iterator().next();
                logTo.log(sr.getName() + " added as " + t.getName()
                    + (t.isExists() ? " is outdated." : " doesn't exist."),
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
    public static void copyResource(final Resource source, final Resource dest) throws IOException {
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
    public static void copyResource(final Resource source, final Resource dest, final Project project)
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
    public static void copyResource(final Resource source, final Resource dest,
                             final FilterSetCollection filters, final Vector<FilterChain> filterChains,
                             final boolean overwrite, final boolean preserveLastModified,
                             final String inputEncoding, final String outputEncoding,
                             final Project project)
        throws IOException {
        copyResource(source, dest, filters, filterChains, overwrite, preserveLastModified, false, inputEncoding, outputEncoding, project);
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
     * @param append Whether to append to an Appendable Resource.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance.
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.8
     */
    public static void copyResource(final Resource source, final Resource dest,
                            final FilterSetCollection filters, final Vector<FilterChain> filterChains,
                            final boolean overwrite, final boolean preserveLastModified,
                                    final boolean append,
                            final String inputEncoding, final String outputEncoding,
                            final Project project)
        throws IOException {
        copyResource(source, dest, filters, filterChains, overwrite,
                     preserveLastModified, append, inputEncoding,
                     outputEncoding, project, /* force: */ false);
    }

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
     * @param append Whether to append to an Appendable Resource.
     * @param inputEncoding the encoding used to read the files.
     * @param outputEncoding the encoding used to write the files.
     * @param project the project instance.
     * @param force whether read-only target files will be overwritten
     *
     * @throws IOException if the copying fails.
     *
     * @since Ant 1.8.2
     */
    public static void copyResource(final Resource source, final Resource dest,
                            final FilterSetCollection filters, final Vector<FilterChain> filterChains,
                            final boolean overwrite, final boolean preserveLastModified,
                                    final boolean append,
                                    final String inputEncoding, final String outputEncoding,
                                    final Project project, final boolean force)
        throws IOException {
        if (!overwrite && !SelectorUtils.isOutOfDate(source, dest,
                FileUtils.getFileUtils().getFileTimestampGranularity())) {
            return;
        }
        final boolean filterSetsAvailable = (filters != null
                                             && filters.hasFilters());
        final boolean filterChainsAvailable = (filterChains != null
                                               && !filterChains.isEmpty());
        String effectiveInputEncoding;
        if (source instanceof StringResource) {
            effectiveInputEncoding = ((StringResource) source).getEncoding();
        } else {
            effectiveInputEncoding = inputEncoding;
        }
        File destFile = null;
        if (dest.as(FileProvider.class) != null) {
            destFile = dest.as(FileProvider.class).getFile();
        }
        if (destFile != null && destFile.isFile() && !destFile.canWrite()) {
            if (!force) {
                throw new ReadOnlyTargetFileException(destFile);
            }
            if (!FILE_UTILS.tryHardToDelete(destFile)) {
                throw new IOException(
                    "failed to delete read-only destination file " + destFile);
            }
        }

        if (filterSetsAvailable) {
            copyWithFilterSets(source, dest, filters, filterChains,
                               append, effectiveInputEncoding,
                               outputEncoding, project);
        } else if (filterChainsAvailable
                   || (effectiveInputEncoding != null
                       && !effectiveInputEncoding.equals(outputEncoding))
                   || (effectiveInputEncoding == null && outputEncoding != null)) {
            copyWithFilterChainsOrTranscoding(source, dest, filterChains,
                                              append, effectiveInputEncoding,
                                              outputEncoding,
                                              project);
        } else {
            boolean copied = false;
            if (source.as(FileProvider.class) != null
                && destFile != null && !append) {
                final File sourceFile =
                    source.as(FileProvider.class).getFile();
                try {
                    copyUsingFileChannels(sourceFile, destFile, project);
                    copied = true;
                } catch (final IOException ex) {
                    String msg = "Attempt to copy " + sourceFile
                        + " to " + destFile + " using NIO Channels"
                        + " failed due to '" + ex.getMessage()
                        + "'.  Falling back to streams.";
                    if (project != null) {
                        project.log(msg, Project.MSG_WARN);
                    } else {
                        System.err.println(msg);
                    }
                }
            }
            if (!copied) {
                copyUsingStreams(source, dest, append, project);
            }
        }
        if (preserveLastModified) {
            final Touchable t = dest.as(Touchable.class);
            if (t != null) {
                setLastModified(t, source.getLastModified());
            }
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
    public static void setLastModified(final Touchable t, final long time) {
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
    public static boolean contentEquals(final Resource r1, final Resource r2, final boolean text) throws IOException {
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
            final long s1 = r1.getSize();
            final long s2 = r2.getSize();
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
    public static int compareContent(final Resource r1, final Resource r2, final boolean text) throws IOException {
        if (r1.equals(r2)) {
            return 0;
        }
        final boolean e1 = r1.isExists();
        final boolean e2 = r2.isExists();
        if (!e1 && !e2) {
            return 0;
        }
        if (e1 != e2) {
            return e1 ? 1 : -1;
        }
        final boolean d1 = r1.isDirectory();
        final boolean d2 = r2.isDirectory();
        if (d1 && d2) {
            return 0;
        }
        if (d1 || d2) {
            return d1 ? -1 : 1;
        }
        return text ? textCompare(r1, r2) : binaryCompare(r1, r2);
    }

    /**
     * Convenience method to turn any fileProvider into a basic
     * FileResource with the file's immediate parent as the basedir,
     * for tasks that need one.
     * @param fileProvider input
     * @return fileProvider if it is a FileResource instance, or a new
     * FileResource with fileProvider's file.
     * @since Ant 1.8
     */
    public static FileResource asFileResource(final FileProvider fileProvider) {
        if (fileProvider instanceof FileResource || fileProvider == null) {
            return (FileResource) fileProvider;
        }
        return new FileResource(Project.getProject(fileProvider),
            fileProvider.getFile());
    }

    /**
     * Binary compares the contents of two Resources.
     * <p>
     * simple but sub-optimal comparison algorithm. written for working
     * rather than fast. Better would be a block read into buffers followed
     * by long comparisons apart from the final 1-7 bytes.
     * </p>
     *
     * @param r1 the Resource whose content is to be compared.
     * @param r2 the other Resource whose content is to be compared.
     * @return a negative integer, zero, or a positive integer as the first
     *         argument is less than, equal to, or greater than the second.
     * @throws IOException if the Resources cannot be read.
     * @since Ant 1.7
     */
    private static int binaryCompare(final Resource r1, final Resource r2) throws IOException {
        try (InputStream in1 = new BufferedInputStream(r1.getInputStream());
                InputStream in2 =
                    new BufferedInputStream(r2.getInputStream())) {

            for (int b1 = in1.read(); b1 != -1; b1 = in1.read()) {
                final int b2 = in2.read();
                if (b1 != b2) {
                    return b1 > b2 ? 1 : -1;
                }
            }
            return in2.read() == -1 ? 0 : -1;
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
    private static int textCompare(final Resource r1, final Resource r2) throws IOException {
        try (BufferedReader in1 =
            new BufferedReader(new InputStreamReader(r1.getInputStream()));
                BufferedReader in2 = new BufferedReader(
                    new InputStreamReader(r2.getInputStream()))) {

            String expected = in1.readLine();
            while (expected != null) {
                final String actual = in2.readLine();
                if (!expected.equals(actual)) {
                    if (actual == null) {
                        return 1;
                    }
                    return expected.compareTo(actual);
                }
                expected = in1.readLine();
            }
            return in2.readLine() == null ? 0 : -1; //NOSONAR
        }
    }

    /**
     * Log which Resources (if any) have been modified in the future.
     * @param logTo the ProjectComponent to do the logging.
     * @param rc the collection of Resources to check.
     * @param granularity the timestamp granularity to use.
     * @since Ant 1.7
     */
    private static void logFuture(final ProjectComponent logTo,
                                  final ResourceCollection rc, final long granularity) {
        final long now = System.currentTimeMillis() + granularity;
        final Date sel = new Date();
        sel.setMillis(now);
        sel.setWhen(TimeComparison.AFTER);
        final Restrict future = new Restrict();
        future.add(sel);
        future.add(rc);
        for (final Resource r : future) {
            logTo.log("Warning: " + r.getName() + " modified in the future.", Project.MSG_WARN);
        }
    }

    private static void copyWithFilterSets(final Resource source, final Resource dest,
                                           final FilterSetCollection filters,
                                           final Vector<FilterChain> filterChains,
                                           final boolean append,
                                           final String inputEncoding, final String outputEncoding,
                                           final Project project)
        throws IOException {

        if (areSame(source, dest)) {
            // copying the "same" file to itself will corrupt the file, so we skip it
            log(project, "Skipping (self) copy of " + source +  " to " + dest);
            return;
        }

        try (Reader in = filterWith(project, inputEncoding, filterChains,
                source.getInputStream());
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                     getOutputStream(dest, append, project),
                     charsetFor(outputEncoding)))) {

            final LineTokenizer lineTokenizer = new LineTokenizer();
            lineTokenizer.setIncludeDelims(true);
            String line = lineTokenizer.getToken(in);
            while (line != null) {
                if (line.isEmpty()) {
                    // this should not happen, because the lines are
                    // returned with the end of line delimiter
                    out.newLine();
                } else {
                    out.write(filters.replaceTokens(line));
                }
                line = lineTokenizer.getToken(in);
            }
        }
    }

    private static Reader filterWith(Project project, String encoding,
        Vector<FilterChain> filterChains, InputStream input) {
        Reader r = new InputStreamReader(input, charsetFor(encoding));
        if (filterChains != null && !filterChains.isEmpty()) {
            final ChainReaderHelper crh = new ChainReaderHelper();
            crh.setBufferSize(FileUtils.BUF_SIZE);
            crh.setPrimaryReader(r);
            crh.setFilterChains(filterChains);
            crh.setProject(project);
            r = crh.getAssembledReader();
        }
        return new BufferedReader(r);
    }

    private static Charset charsetFor(String encoding) {
        return encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
    }

    private static void copyWithFilterChainsOrTranscoding(final Resource source,
                                                          final Resource dest,
                                                          final Vector<FilterChain> filterChains,
                                                          final boolean append,
                                                          final String inputEncoding,
                                                          final String outputEncoding,
                                                          final Project project)
        throws IOException {

        if (areSame(source, dest)) {
            // copying the "same" file to itself will corrupt the file, so we skip it
            log(project, "Skipping (self) copy of " + source +  " to " + dest);
            return;
        }

        try (Reader in = filterWith(project, inputEncoding, filterChains,
                source.getInputStream());
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                     getOutputStream(dest, append, project),
                     charsetFor(outputEncoding)))) {
            final char[] buffer = new char[FileUtils.BUF_SIZE];
            while (true) {
                final int nRead = in.read(buffer, 0, buffer.length);
                if (nRead == -1) {
                    break;
                }
                out.write(buffer, 0, nRead);
            }
        }

    }

    private static void copyUsingFileChannels(final File sourceFile,
                                              final File destFile, final Project project)
        throws IOException {

        if (FileUtils.getFileUtils().areSame(sourceFile, destFile)) {
            // copying the "same" file to itself will corrupt the file, so we skip it
            log(project, "Skipping (self) copy of " + sourceFile +  " to " + destFile);
            return;
        }
        final File parent = destFile.getParentFile();
        if (parent != null && !parent.isDirectory()
            && !(parent.mkdirs() || parent.isDirectory())) {
            throw new IOException("failed to create the parent directory"
                                  + " for " + destFile);
        }

        try (FileChannel srcChannel =
            FileChannel.open(sourceFile.toPath(), StandardOpenOption.READ);
                FileChannel destChannel = FileChannel.open(destFile.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
            long position = 0;
            final long count = srcChannel.size();
            while (position < count) {
                final long chunk =
                    Math.min(MAX_IO_CHUNK_SIZE, count - position);
                position +=
                    destChannel.transferFrom(srcChannel, position, chunk);
            }
        }
    }

    private static void copyUsingStreams(final Resource source, final Resource dest,
                                         final boolean append, final Project project)
        throws IOException {

        if (areSame(source, dest)) {
            // copying the "same" file to itself will corrupt the file, so we skip it
            log(project, "Skipping (self) copy of " + source +  " to " + dest);
            return;
        }
        try (InputStream in = source.getInputStream();
             OutputStream out = getOutputStream(dest, append, project)) {

            final byte[] buffer = new byte[FileUtils.BUF_SIZE];
            int count = 0;
            do {
                out.write(buffer, 0, count);
                count = in.read(buffer, 0, buffer.length);
            } while (count != -1);
        }
    }

    private static OutputStream getOutputStream(final Resource resource, final boolean append, final Project project)
            throws IOException {
        if (append) {
            final Appendable a = resource.as(Appendable.class);
            if (a != null) {
                return a.getAppendOutputStream();
            }
            String msg = "Appendable OutputStream not available for non-appendable resource "
                + resource + "; using plain OutputStream";
            if (project != null) {
                project.log(msg, Project.MSG_VERBOSE);
            } else {
                System.out.println(msg);
            }
        }
        return resource.getOutputStream();
    }

    private static boolean areSame(final Resource resource1, final Resource resource2) throws IOException {
        if (resource1 == null || resource2 == null) {
            return false;
        }
        final FileProvider fileResource1 = resource1.as(FileProvider.class);
        final FileProvider fileResource2 = resource2.as(FileProvider.class);
        return fileResource1 != null && fileResource2 != null
                && FileUtils.getFileUtils().areSame(fileResource1.getFile(), fileResource2.getFile());
    }

    private static void log(final Project project, final String message) {
        log(project, message, Project.MSG_VERBOSE);
    }

    private static void log(final Project project, final String message, final int level) {
        if (project == null) {
            System.out.println(message);
        } else {
            project.log(message, level);
        }
    }

    public interface ResourceSelectorProvider {
        ResourceSelector getTargetSelectorForSource(Resource source);
    }

    /**
     * @since Ant 1.9.4
     */
    public static class ReadOnlyTargetFileException extends IOException {
        private static final long serialVersionUID = 1L;

        public ReadOnlyTargetFileException(final File destFile) {
            super("can't write to read-only destination file " + destFile);
        }
    }
}
