/*
 * Copyright  2004 The Apache Software Foundation
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
package org.apache.tools.ant.taskdefs.repository;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This task will retrieve one or more libraries from a repository.
 * <ol>
 * <li>Users must declare a repository, either inline or by reference</li>
 * <li>Dependency checking is used (timestamps) unless forceDownload=true</li>
 * <li>It is an error if, at the end of the task, a library is missing.
 * </ol>
 *
 * @ant.task
 * @since Ant 1.7
 */
public final class Libraries extends Task {

    /**
     * destination
     */
    private File destDir;

    /**
     * flag to force offline
     */
    private boolean offline = false;

    /**
     * list of libraries
     */
    private EnabledLibraryElementList libraries = new EnabledLibraryElementList();

    /**
     * helper list
     */
    private EnabledLibraryElementList policies = new EnabledLibraryElementList();

    /**
     * repository for retrieval
     */

    private Repository repository;

    /**
     * Optional. A name for a path to define from the dependencies specified.
     */
    private String pathid;

    /**
     * should we be timestamp aware in downloads?
     */
    private boolean useTimestamp = false;

    public static final String ERROR_ONE_REPOSITORY_ONLY = "Only one repository is allowed";
    public static final String ERROR_NO_DEST_DIR = "No destination directory";
    public static final String ERROR_NO_REPOSITORY = "No repository defined";
    public static final String ERROR_NO_LIBRARIES = "No libraries declared";
    public static final String ERROR_REPO_PROBE_FAILED = "Repository probe failed with ";
    public static final String ERROR_LIBRARY_FETCH_FAILED = "Failed to retrieve ";
    public static final String ERROR_INCOMPLETE_RETRIEVAL = "Missing Libraries :";
    public static final String MSG_NO_RETRIEVE = "Connections disabled";
    public static final String MSG_NO_LIBRARIES_TO_FETCH = "No libraries marked for retrieval";


    /**
     * Init the task
     *
     * @throws org.apache.tools.ant.BuildException
     *          if something goes wrong with the build
     */
    public void init() throws BuildException {
        super.init();
        //set our default polocy
        add(new AbsentFilesPolicy());
    }

    /**
     * add a repository. Only one is (currently) supported
     *
     * @param repo
     */
    public void add(Repository repo) {
        if (repository != null) {
            throw new BuildException(ERROR_ONE_REPOSITORY_ONLY);
        }
        repository = repo;
    }


    /**
     * add a repository. Unless there is explicit support for a subclass
     *
     * @param repo
     */
    public void addRepository(RepositoryRef repo) {
        add(repo);
    }

    /**
     * bind to a repository.
     */
    public void setRepositoryRef(final Reference ref) {
        //create a special repository that can only
        //resolve references.
        Repository r = new RepositoryRef(getProject(), ref);
        add(r);
    }

    /**
     * add anything that implements the library policy interface
     * @param policy
     */
    public void add(LibraryPolicy policy) {
        policies.add(policy);
    }

    /**
     * add a schedule
     * @param update
     */
    public void addSchedule(ScheduledUpdatePolicy update) {
        add(update);
    }

    /**
     * Declare that the update should be forced: everything
     * must be fetched; it will be a failure if any are not
     * @param policy
     */
    public void addForce(ForceUpdatePolicy policy) {
        add(policy);
    }

    /**
     * Declare that no files should be fetched
     * @param policy
     */
    public void addNoupdate(NoUpdatePolicy policy) {
        add(policy);
    }

    /**
     * declare that the update should be timestamp driven
     * @param policy
     */
    public void addTimestamp(TimestampPolicy policy) {
        add(policy);
    }

    /**
     * declare that only absent files are to be fetched
     * @param policy
     */
    public void addAbsentfiles(AbsentFilesPolicy policy) {
        add(policy);
    }


    /**
     * make a declaration about the number of files to fetch
     *
     * @param policy
     */
    public void addAssertDownloaded(AssertDownloaded policy) {
        add(policy);
    }

    /**
     * add a library for retrieval
     *
     * @param lib
     */
    public void addLibrary(Library lib) {
        libraries.add(lib);
    }

    /**
     * destination directory for all library files
     *
     * @param destDir
     */
    public void setDestDir(File destDir) {
        this.destDir = destDir;
    }


    /**
     * test for being offline
     *
     * @return true if the offline flag is set
     */
    public boolean isOffline() {
        return offline;
    }

    /**
     * declare the system offline. This disables any attempt to retrieve files.
     * In this mode, only the presence of files is verified. If forceDownload is
     * true, or there is a missing library, then an error will be raised.
     *
     * @param offline
     */
    public void setOffline(boolean offline) {
        this.offline = offline;
    }


    /**
     * get the destination directory
     * @return
     */
    public File getDestDir() {
        return destDir;
    }


    /**
     * get the list of libraries
     * @return
     */
    public List getLibraries() {
        return libraries;
    }

    /**
     * get our repository
     * @return
     */
    public Repository getRepository() {
        return repository;
    }

    /**
     * get the pathID if defined
     * @return
     */
    public String getPathid() {
        return pathid;
    }

    /**
     * the name of a path reference to be created referring
     * to the libraries.
     * @param pathid
     */
    public void setPathid(String pathid) {
        this.pathid = pathid;
    }

    /**
     * get the current timestamp flag
     * @return
     */
    public boolean isUseTimestamp() {
        return useTimestamp;
    }

    /**
     * set the timestamp flag. Not for export into XML
     * @param useTimestamp
     */
    public void _setUseTimestamp(boolean useTimestamp) {
        this.useTimestamp = useTimestamp;
    }

    /**
     * get the current policy list
     * @return
     */
    public List getPolicies() {
        return policies;
    }

    /**
     * validate ourselves
     *
     * @throws BuildException
     */
    public void validate() {
        if (destDir == null
        //        || !destDir.isDirectory()
        ) {
            throw new BuildException(ERROR_NO_DEST_DIR);
        }
        if (repository == null) {
            throw new BuildException(ERROR_NO_REPOSITORY);
        }
        Iterator it = libraries.iterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            library.validate();
        }
    }
    /**
     * Called by the project to let the task do its work.
     *
     * @throws org.apache.tools.ant.BuildException
     *          if something goes wrong with the build
     */
    public void execute() throws BuildException {
        validate();
        if (isOffline()) {
            log("No retrieval, task is \"offline\"");
        } else {
            doExecute();
        }
        //validate the state
        verifyAllLibrariesPresent();

        //create the path
        if (pathid != null) {
            createPath();
        }
    }
    /**
     * This is the real worker method
     *
     * @throws org.apache.tools.ant.BuildException
     *          if something goes wrong with the build
     */
    private void doExecute() throws BuildException {
        destDir.mkdirs();
        Repository repo = repository.resolve();
        repo.validate();
        if (libraries.size() == 0) {
            throw new BuildException(ERROR_NO_LIBRARIES);
        }
        log("Getting libraries from " + repo.toString(), Project.MSG_VERBOSE);
        log("Saving libraries to " + destDir.toString(), Project.MSG_VERBOSE);

        //map libraries to files
        bindAllLibraries();


        //flag to indicate whether the download should go ahead
        boolean retrieve = true;
        List processedPolicies = new ArrayList(policies.size());
        //iterate through all policies and execute their preload task
        Iterator policyIterator = policies.enabledIterator();
        while (retrieve && policyIterator.hasNext()) {
            LibraryPolicy libraryPolicy = (LibraryPolicy) policyIterator.next();
            retrieve = libraryPolicy.beforeConnect(this, libraryIterator());
            if (retrieve) {
                //add all processed properties to the list, 'cept for anything that
                //broke the chain
                processedPolicies.add(libraryPolicy);
            } else {
                log("Policy " + libraryPolicy.getClass().getName()
                        + " disabled retrieval",
                        Project.MSG_VERBOSE);
            }
        }

        //see if we need to do a download
        if (!retrieve) {
            //if not, log it
            log(MSG_NO_RETRIEVE);
        } else {
            int downloads = calculateFetchCount();
            if (downloads > 0) {
                //get the files
                connectAndRetrieve(repo, useTimestamp);
            } else {
                //nothing to fetch
                log(MSG_NO_LIBRARIES_TO_FETCH, Project.MSG_VERBOSE);
            }
        }

        //now reverse iterate through all processed properties.
        for (int i = processedPolicies.size() - 1; i >= 0; i--) {
            LibraryPolicy libraryPolicy = (LibraryPolicy) processedPolicies.get(i);
            //and call their post-processor
            libraryPolicy.afterFetched(this, libraryIterator());
        }
    }

    /**
     * connect to the remote system, retrieve files
     * @param repo
     * @param useTimestamp
     * @return number of failed retrievals.
     */
    private int connectAndRetrieve(Repository repo, boolean useTimestamp) {
        //connect the repository
        int failures = 0;
        repo.connect(this);
        try {

            //check for reachability.
            //it is up to each repository to decide that.
            boolean reachable;
            try {
                log("Checking repository for reachability", Project.MSG_DEBUG);
                reachable = repo.checkRepositoryReachable();
            } catch (IOException e) {

                log(ERROR_REPO_PROBE_FAILED + e.getMessage(),
                        Project.MSG_VERBOSE);
                reachable = false;
            }
            if (!reachable) {
                log("repository is not reachable", Project.MSG_INFO);
                return 0;
            }

            //iterate through the libs we have enabled
            Iterator it = enabledLibrariesIterator();
            while (it.hasNext()) {
                Library library = (Library) it.next();
                //check to see if it is for fetching
                if (library.isToFetch()) {
                    log("Fetching " + library.getNormalFilename(), Project.MSG_VERBOSE);
                    try {
                        //fetch it
                        boolean fetched = repo.fetch(library, useTimestamp);
                        //record the fact in the library
                        log("success; marking as fetched",
                                Project.MSG_DEBUG);
                        library._setFetched(fetched);
                    } catch (IOException e) {
                        log(ERROR_LIBRARY_FETCH_FAILED + library);
                        log(e.getMessage());
                        //add failures
                        failures++;
                    }
                } else {
                    //no fetch
                    log("Skipping " + library.getNormalFilename(), Project.MSG_VERBOSE);
                }
            }
        } finally {

            log("disconnecting", Project.MSG_VERBOSE);
            repo.disconnect();
        }
        return failures;
    }

    /**
     * bind all libraries to our destination
     */
    public  void bindAllLibraries() {
        Iterator it = libraries.iterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            library.bind(destDir);
        }
    }

    /**
     * set/clear the fetch flag on all libraries.
     * @param fetch
     */
    public void markAllLibrariesForFetch(boolean fetch) {
        Iterator it = libraryIterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            library._setToFetch(fetch);
        }
    }

    /**
     * set the fetch flag on all libraries that are absent; clear
     * it from all those that exist
     *
     */
    public void markMissingLibrariesForFetch() {
        Iterator it = libraryIterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            library._setToFetch(!library.exists());
        }
    }

    /**
     * work out how many libraries to fetch
     * @return count of enabled libraries with the to fetch bit set
     */
    public  int calculateFetchCount() {
        int count = 0;
        Iterator it = enabledLibrariesIterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            if (library.isToFetch()) {
                count++;
            };
        }
        return count;
    }

    /**
     * work out how many libraries were fetched
     * @return number of libraries that are enabled with the
     * {@link Library#wasFetched()} flag true.
     */
    public int calculateDownloadedCount() {
        int count = 0;
        //here verify that everything came in
        Iterator downloaded = enabledLibrariesIterator();
        while (downloaded.hasNext()) {
            Library library = (Library) downloaded.next();
            if (library.wasFetched()) {
                count++;
            }
        }
        return count;
    }


    /**
     * verify that all libraries are present
     */
    protected void verifyAllLibrariesPresent() {
        //iterate through the libs we have
        boolean missing = false;
        StringBuffer buffer = new StringBuffer();
        Iterator it = enabledLibrariesIterator();
        while (it.hasNext()) {
            Library library = (Library) it.next();
            //check for the library existing
            if (!library.exists()) {
                //and log if one is missing
                buffer.append(library.toString() + "; ");
                log("Missing: " + library.toString(),
                        Project.MSG_ERR);
                missing = true;
            }
        }
        if (missing) {
            throw new BuildException(ERROR_INCOMPLETE_RETRIEVAL + buffer);
        }
    }

    /**
     * create a path; requires pathID!=null
     */
    private void createPath() {
        Path path = new Path(getProject());
        for (Iterator iterator = enabledLibrariesIterator();
             iterator.hasNext();) {
            ((Library) iterator.next()).appendToPath(path);
        }
        getProject().addReference(pathid, path);
    }

    /**
     * get a filtered iterator of the dependencies
     * @return a new iterator that ignores disabled libraries
     */
    public Iterator enabledLibrariesIterator() {
        return libraries.enabledIterator();
    }

    /**
     * get a list iterator for the files
     * This gives you more power
     * @return
     */
    public ListIterator libraryIterator() {
        return libraries.listIterator();
    }

}
