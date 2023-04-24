package com.mercedesbenz.sechub.systemtest.config;

import static com.mercedesbenz.sechub.systemtest.TestConfigConstants.*;
import static com.mercedesbenz.sechub.systemtest.config.DefaultFallback.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mercedesbenz.sechub.commons.model.JSONConverter;

class SystemTestConfigurationTest {

    private static final Logger LOG = LoggerFactory.getLogger(SystemTestConfigurationTest.class);

    @Test
    void a_full_blown_setup_can_be_serialized_and_deserialized() throws Exception {

        /* prepare */
        SystemTestConfiguration configuration = createFullBlownExampleConfiguration();

        /* execute 1 */
        String json = JSONConverter.get().toJSON(configuration, true);
        LOG.info(
                "A full blown test configuration. Not really runnable (e.g. remote and local setup at once), but can be used for documentation/explanation... JSON =\n{}",
                json);

        /* test 1 */
        assertNotNull(json);

        /* execute 2 */
        SystemTestConfiguration configLoaded = JSONConverter.get().fromJSON(SystemTestConfiguration.class, json);

        /* test 2 */
        assertNotNull(configLoaded);
        List<PDSSolutionDefinition> solutionsLoaded = configLoaded.getSetup().getLocal().get().getPdsSolutions();
        assertEquals(1, solutionsLoaded.size());
        PDSSolutionDefinition solutionLoaded = solutionsLoaded.get(0);
        List<ExecutionStepDefinition> steps = solutionLoaded.getStart();

        assertEquals(2, steps.size());

    }

    private SystemTestConfiguration createFullBlownExampleConfiguration() {
        SystemTestConfiguration configuration = new SystemTestConfiguration();

        configuration.getVariables().put("var1", "1");
        configuration.getVariables().put("custom_data_txt", "${env.FROM_OUTSIDE}/subfolder1/data.txt");

        buildSetup(configuration);

        buildTest(configuration);

        return configuration;
    }

    private void buildSetup(SystemTestConfiguration configuration) {
        buildLocalSetup(configuration);
        buildRemoteSetup(configuration);
    }

    private void buildLocalSetup(SystemTestConfiguration configuration) {
        LocalSetupDefinition local = new LocalSetupDefinition();
        local.setComment("This is a local setup - it uses not an existing SecHub infrastructure, but does build all by its own");

        configuration.getSetup().setLocal(Optional.of(local));

        /* ------ */
        /* SecHub */
        /* ------ */

        LocalSecHubDefinition sechub = local.getSecHub();
        sechub.setComment(
                "If there are variants defined, the further defined tests will be run with all of those variants by the framework! ADDITION: we could introced here start/stop step like in pds solutions to build the sechub container from scratch as well (with different jdks etc.)");
        // start + stop (as an addition to variants

        SecHubConfigurationDefinition sechubConfig = sechub.getConfigure();
        sechubConfig.setComment(
                "Here we can configure sechub. At least one executor entry is mandatory " + "(we must know which products shall be inside the profile). "
                        + "Projects is optional. If not defined only a default project and a default profile"
                        + " for the given executors will be automatically created and used.");

        SecHubExecutorConfigDefinition executor1 = new SecHubExecutorConfigDefinition();

        Map<String, String> parameters1 = executor1.getParameters();
        parameters1.put("gosec.additional.parameter", "${variables.custom_data_txt}");
        parameters1.put("pds.reuse.sechubstorage", "false (overrides default)");

        executor1.setPdsProductId("GOSEC");
        executor1.setBaseURL("https://gosec_pds.example.com:8443");

        sechubConfig.getExecutors().add(executor1);

        List<ProjectDefinition> projects = new ArrayList<>();
        ProjectDefinition project1 = new ProjectDefinition();
        project1.setName(FALLBACK_PROJECT_NAME.getValue());
        project1.getProfiles().add(FALLBACK_PROFILE_ID.getValue());

        projects.add(project1);
        Optional<List<ProjectDefinition>> projectsOpt = Optional.of(projects);
        sechubConfig.setProjects(projectsOpt);

        /* --------- */
        /* Solutions */
        /* --------- */

        // start
        PDSSolutionDefinition solution1 = new PDSSolutionDefinition();
        solution1.setName("gosec");
        solution1.setWaitForAvailable(true);
        solution1.setComment(
                "Test solution1, the scan types etc. cannot be defined, because runtime loads all meta information from config file. Basedir is optional, normally calculated automatically by name");
        solution1.setBaseDirectory("${env.base_dir}/pds-solutions/gosec");

        ExecutionStepDefinition startStep1 = new ExecutionStepDefinition();
        startStep1.setComment("In first start step, we build the image");

        ScriptDefinition startStep1Script1 = new ScriptDefinition();
        startStep1Script1.setPath("./01-build-the-image.sh ${variables.myContainer.values}");
        startStep1.setScript(Optional.of(startStep1Script1));

        solution1.getStart().add(startStep1);

        ScriptDefinition startStep2Script1 = new ScriptDefinition();
        startStep2Script1.setPath("./05-start-container.sh");
        startStep1Script1.setWorkingDir("./");

        ExecutionStepDefinition startStep2 = new ExecutionStepDefinition();
        startStep2.setComment("As last start step... start the gosec PDS solution");
        startStep2.setScript(Optional.of(startStep2Script1));

        solution1.getStart().add(startStep2);

        List<PDSSolutionDefinition> pdsSolutions = local.getPdsSolutions();
        pdsSolutions.add(solution1);

        // stop
        ExecutionStepDefinition stopStep1 = new ExecutionStepDefinition();
        stopStep1.setComment("One single step to finally stop the gosec PDS solution when tests have been done.");

        ScriptDefinition stopStep1Script1 = new ScriptDefinition();
        stopStep1Script1.setPath("./06-stop.sh");
        stopStep1.setScript(Optional.of(startStep1Script1));

        solution1.getStop().add(stopStep1);
    }

    private void buildRemoteSetup(SystemTestConfiguration configuration) {
        RemoteSetupDefinition remote = new RemoteSetupDefinition();
        remote.setComment("This is a remote setup - we use an existing and configured SecHub cluster/instance");
        RemoteSecHubDefinition secHub = remote.getSecHub();

        try {
            secHub.setUrl(new URL("https://sechub.example.com:8443"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("url should be valid", e);
        }

        CredentialsDefinition admin = secHub.getAdmin();
        admin.setUserId("testadmin");
        admin.setApiToken("${env.SYSTEM_TEST_ADMIN_TOKEN}");

        CredentialsDefinition user = secHub.getUser();
        user.setUserId("testuser");
        user.setApiToken("${env.SYSTEM_TEST_USER_TOKEN}");

        configuration.getSetup().setRemote(Optional.of(remote));

    }

    private void buildTest(SystemTestConfiguration configuration) {
        /* ----------- */
        /* Define test */
        /* ----------- */

        // prepare

        TestDefinition test1 = new TestDefinition();
        test1.setName("Test1");
        test1.setComment("This test does a git checkout of a simple gosec application. After this we ensure the sechub result json file is as expected.");
        configuration.getTests().add(test1);

        ScriptDefinition testPrepareScript1 = new ScriptDefinition();
        testPrepareScript1.setComment("The script call here would call the script inside the 'tests' subfoler of the solution");
        testPrepareScript1.setPath("./../tests/checkout-simple-go-project-withouth-sechub-json.sh ${" + RUNTIME_WORKSPACE_ROOT + "}/checkout");

        ExecutionStepDefinition test1Step1 = new ExecutionStepDefinition();

        test1.getPrepare().add(test1Step1);
        test1Step1.setScript(Optional.of(testPrepareScript1));

        ExecutionStepDefinition test1Step2 = new ExecutionStepDefinition();

        test1.getPrepare().add(test1Step2);

        ScriptDefinition testPrepareScript2 = new ScriptDefinition();
        testPrepareScript2.setComment("Just another scriptcall as an example for possibility of multiple scripts + an environment variable from 'outside'...");
        testPrepareScript2.setPath("${env.FROM_OUT_SIDE}/do-something-else.sh");

        test1Step2.setScript(Optional.of(testPrepareScript2));

        // execute (test)
        TestExecutionDefinition test1execute1 = new TestExecutionDefinition();
        test1.setExecute(test1execute1);
        test1execute1.setComment("This part can be defined only once. It describes what is executed");
        RunSecHubJobDefinition runSecHubJob1 = new RunSecHubJobDefinition();
        runSecHubJob1.setComment("If no project is defined, " + FALLBACK_PROJECT_NAME + " is used with default profile etc.");
        runSecHubJob1.setProject(FALLBACK_PROJECT_NAME.getValue());

        UploadDefinition upload = runSecHubJob1.getUpload();
        upload.setComment("Here we can define either binaries or sources to upload - we define the folders, framework will create tars/zips automatically");
        upload.setSourceFolder("${" + RUNTIME_WORKSPACE_ROOT + "}/checkout/sources");
        upload.setBinariesFolder("${" + RUNTIME_WORKSPACE_ROOT + "}/checkout/binaries");

        test1execute1.setRunSecHubJob(Optional.of(runSecHubJob1));

        // asserts
        TestAssertDefinition test1Assert1 = new TestAssertDefinition();
        test1.getAssert().add(test1Assert1);

        AssertSechubResultDefinition assertSecHubResult1 = new AssertSechubResultDefinition();
        test1Assert1.getSechubResult().add(assertSecHubResult1);

        AssertEqualsFileDefinition test1Assert1equalsFile1 = new AssertEqualsFileDefinition();
        test1Assert1equalsFile1.setComment(
                "At execution time, the different job uuids inside the two reports may are handled automatically. The given file is just a normal report.");
        test1Assert1equalsFile1.setPath("./../tests/expected-sechub-json.json");
        assertSecHubResult1.setEqualsFile(Optional.of(test1Assert1equalsFile1));

        AssertContainsStringsDefinition test1Assert1ContainsStrings = new AssertContainsStringsDefinition();
        test1Assert1ContainsStrings.setComment("Checks if the sechub result file contains the given strings.");
        test1Assert1ContainsStrings.setValues(Arrays.asList("CWE-89", "SQL-Injection", "Improper"));
        assertSecHubResult1.setContainsStrings(Optional.of(test1Assert1ContainsStrings));
    }

}
