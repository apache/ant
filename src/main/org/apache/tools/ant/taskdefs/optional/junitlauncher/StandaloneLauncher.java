package org.apache.tools.ant.taskdefs.optional.junitlauncher;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.Constants.LD_XML_ATTR_HALT_ON_FAILURE;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.Constants.LD_XML_ATTR_PRINT_SUMMARY;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.Constants.LD_XML_ELM_LAUNCH_DEF;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.Constants.LD_XML_ELM_LISTENER;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.Constants.LD_XML_ELM_TEST;
import static org.apache.tools.ant.taskdefs.optional.junitlauncher.Constants.LD_XML_ELM_TEST_CLASSES;

/**
 * Used for launching forked tests from the {@link JUnitLauncherTask}.
 * <p>
 * Although this class is public, this isn't meant for external use. The contract of what
 * program arguments {@link #main(String[]) the main method} accepts and how it interprets it,
 * is also an internal detail and can change across Ant releases.
 *
 * @since Ant 1.10.6
 */
public class StandaloneLauncher {

    /**
     * Entry point to launching the forked test.
     *
     * @param args The arguments passed to this program for launching the tests
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        // The main responsibility of this entry point is to create a LaunchDefinition,
        // by parsing the passed arguments and then use the LauncherSupport to
        // LauncherSupport#launch the tests
        try {
            ForkedLaunch launchDefinition = null;
            final ForkedExecution forkedExecution = new ForkedExecution();
            for (int i = 0; i < args.length; ) {
                final String arg = args[i];
                int numArgsConsumed = 1;
                switch (arg) {
                    case Constants.ARG_PROPERTIES: {
                        final Path propsPath = Paths.get(args[i + 1]);
                        if (!Files.isRegularFile(propsPath)) {
                            throw new IllegalArgumentException(propsPath + " does not point to a properties file");
                        }
                        final Properties properties = new Properties();
                        try (final InputStream is = Files.newInputStream(propsPath)) {
                            properties.load(is);
                        }
                        forkedExecution.setProperties(properties);
                        numArgsConsumed = 2;
                        break;
                    }
                    case Constants.ARG_LAUNCH_DEFINITION: {
                        final Path launchDefXmlPath = Paths.get(args[i + 1]);
                        if (!Files.isRegularFile(launchDefXmlPath)) {
                            throw new IllegalArgumentException(launchDefXmlPath + " does not point to a launch definition file");
                        }
                        launchDefinition = parseLaunchDefinition(launchDefXmlPath);
                        numArgsConsumed = 2;
                        break;
                    }
                }
                i = i + numArgsConsumed;
            }


            launchDefinition.setTestExecutionContext(forkedExecution);
            final LauncherSupport launcherSupport = new LauncherSupport(launchDefinition);
            try {
                launcherSupport.launch();
            } catch (Throwable t) {
                if (launcherSupport.hasTestFailures()) {
                    System.exit(Constants.FORK_EXIT_CODE_TESTS_FAILED);
                    throw t;
                }
            }
            if (launcherSupport.hasTestFailures()) {
                System.exit(Constants.FORK_EXIT_CODE_TESTS_FAILED);
                return;
            }
            System.exit(Constants.FORK_EXIT_CODE_SUCCESS);
            return;
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    private static ForkedLaunch parseLaunchDefinition(final Path pathToLaunchDefXml) {
        if (pathToLaunchDefXml == null || !Files.isRegularFile(pathToLaunchDefXml)) {
            throw new IllegalArgumentException(pathToLaunchDefXml + " is not a file");
        }
        final ForkedLaunch forkedLaunch = new ForkedLaunch();
        try (final InputStream is = Files.newInputStream(pathToLaunchDefXml)) {
            final XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(is);
            reader.require(START_DOCUMENT, null, null);
            reader.nextTag();
            reader.require(START_ELEMENT, null, LD_XML_ELM_LAUNCH_DEF);
            final String haltOnfFailure = reader.getAttributeValue(null, LD_XML_ATTR_HALT_ON_FAILURE);
            if (haltOnfFailure != null) {
                forkedLaunch.setHaltOnFailure(Boolean.parseBoolean(haltOnfFailure));
            }
            final String printSummary = reader.getAttributeValue(null, LD_XML_ATTR_PRINT_SUMMARY);
            if (printSummary != null) {
                forkedLaunch.setPrintSummary(Boolean.parseBoolean(printSummary));
            }
            if (haltOnfFailure != null) {
                forkedLaunch.setHaltOnFailure(Boolean.parseBoolean(haltOnfFailure));
            }
            reader.nextTag();
            reader.require(START_ELEMENT, null, null);
            final String elementName = reader.getLocalName();
            switch (elementName) {
                case LD_XML_ELM_TEST: {
                    forkedLaunch.addTests(Collections.singletonList(SingleTestClass.fromForkedRepresentation(reader)));
                    break;
                }
                case LD_XML_ELM_TEST_CLASSES: {
                    forkedLaunch.addTests(TestClasses.fromForkedRepresentation(reader));
                    break;
                }
                case LD_XML_ELM_LISTENER: {
                    forkedLaunch.addListener(ListenerDefinition.fromForkedRepresentation(reader));
                    break;
                }
            }
            reader.nextTag();
            reader.require(END_ELEMENT, null, LD_XML_ELM_LAUNCH_DEF);
            reader.next();
            reader.require(END_DOCUMENT, null, null);
            return forkedLaunch;
        } catch (Exception e) {
            throw new BuildException("Failed to construct definition from forked representation", e);
        }
    }


    private static final class ForkedExecution implements TestExecutionContext {
        private Properties properties = new Properties();

        private ForkedExecution() {
        }

        private ForkedExecution setProperties(final Properties properties) {
            this.properties = properties;
            return this;
        }

        @Override
        public Properties getProperties() {
            return this.properties;
        }

        @Override
        public Optional<Project> getProject() {
            // forked execution won't have access to the Ant Project
            return Optional.empty();
        }
    }

    private static final class ForkedLaunch implements LaunchDefinition {

        private boolean printSummary;
        private boolean haltOnFailure;
        private TestExecutionContext testExecutionContext;
        private List<TestDefinition> tests = new ArrayList<>();
        private List<ListenerDefinition> listeners = new ArrayList<>();

        @Override
        public List<TestDefinition> getTests() {
            return this.tests;
        }

        ForkedLaunch addTests(final List<TestDefinition> tests) {
            this.tests.addAll(tests);
            return this;
        }

        @Override
        public List<ListenerDefinition> getListeners() {
            return this.listeners;
        }

        ForkedLaunch addListener(final ListenerDefinition listener) {
            this.listeners.add(listener);
            return this;
        }

        @Override
        public boolean isPrintSummary() {
            return this.printSummary;
        }

        private ForkedLaunch setPrintSummary(final boolean printSummary) {
            this.printSummary = printSummary;
            return this;
        }

        @Override
        public boolean isHaltOnFailure() {
            return this.haltOnFailure;
        }

        public ForkedLaunch setHaltOnFailure(final boolean haltOnFailure) {
            this.haltOnFailure = haltOnFailure;
            return this;
        }

        public ForkedLaunch setTestExecutionContext(final TestExecutionContext testExecutionContext) {
            this.testExecutionContext = testExecutionContext;
            return this;
        }

        @Override
        public ClassLoader getClassLoader() {
            return this.getClass().getClassLoader();
        }

        @Override
        public TestExecutionContext getTestExecutionContext() {
            return this.testExecutionContext;
        }
    }
}
