package io.jenkins.plugins.jobicon;

import static io.jenkins.plugins.jobicon.utils.TestUtils.createCustomIconFile;
import static io.jenkins.plugins.jobicon.utils.TestUtils.mockStaplerRequest;
import static io.jenkins.plugins.jobicon.utils.TestUtils.validateResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import jenkins.appearance.AppearanceCategory;
import jenkins.model.GlobalConfigurationCategory;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Custom Job Icon Configuration Tests
 */
@WithJenkins
class CustomJobIconConfigurationTest {

    private static final Logger LOGGER = Logger.getLogger(CustomJobIconConfigurationTest.class.getName());

    private static final String DUMMY_PNG = "dummy.png";

    /**
     * Test behavior of {@link CustomJobIconConfiguration#getCategory()}.
     */
    @Test
    void testGetCategory(@SuppressWarnings("unused") JenkinsRule r) {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();
        assertEquals(descriptor.getCategory(), GlobalConfigurationCategory.get(AppearanceCategory.class));
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#getRequiredGlobalConfigPagePermission()}.
     */
    @Test
    void testGetRequiredGlobalConfigPagePermission(@SuppressWarnings("unused") JenkinsRule r) {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();
        assertEquals(Jenkins.MANAGE, descriptor.getRequiredGlobalConfigPagePermission());
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#getDiskUsage()}}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testGetDiskUsage(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        FilePath file = createCustomIconFile(r);

        String usage = descriptor.getDiskUsage();
        assertEquals(FileUtils.byteCountToDisplaySize(file.length()), usage);
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#getDiskUsage()}}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testGetDiskUsageNoIcons(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        FilePath parent = r.jenkins
                .getRootPath()
                .child(CustomJobIconConfiguration.USER_CONTENT_PATH)
                .child(CustomJobIconConfiguration.PLUGIN_PATH);
        parent.mkdirs();

        String usage = descriptor.getDiskUsage();
        assertEquals(FileUtils.byteCountToDisplaySize(0L), usage);
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#getDiskUsage()}}.
     */
    @Test
    void testGetDiskUsageNoRoot(@SuppressWarnings("unused") JenkinsRule r) {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        String usage = descriptor.getDiskUsage();
        assertEquals(FileUtils.byteCountToDisplaySize(0L), usage);
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#getDiskUsage()}}.
     */
    @Test
    void testGetDiskUsageWithException(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);
        FilePath file = createCustomIconFile(r);

        try (@SuppressWarnings("unused")
                MockedConstruction<FilePath> mocked = mockConstructionWithAnswer(FilePath.class, invocation -> {
                    String call = invocation.toString();
                    if (StringUtils.equals(call, "filePath.child(\"userContent\");")) {
                        return userContent;
                    } else if (StringUtils.equals(call, "filePath.exists();")) {
                        return true;
                    } else if (StringUtils.equals(call, "filePath.list();")) {
                        return List.of(file);
                    } else if (StringUtils.equals(call, "filePath.child(\n    \"" + file.getName() + "\"\n);")) {
                        throw new IOException("Mocked Exception!");
                    }
                    return fail("Unexpected invocation '" + call + "' - Test is broken!");
                })) {
            String usage = descriptor.getDiskUsage();
            assertEquals(FileUtils.byteCountToDisplaySize(0L), usage);
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanupNoItems(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);

            FilePath file = createCustomIconFile(r);
            HttpResponse response = descriptor.doCleanup(mockReq);

            validateResponse(response, HttpServletResponse.SC_OK, null, null);
            assertFalse(file.exists());
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanupMissingIcon(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();
        CustomJobIcon customIcon = new CustomJobIcon(DUMMY_PNG);

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);

        FilePath parent = r.jenkins
                .getRootPath()
                .child(CustomJobIconConfiguration.USER_CONTENT_PATH)
                .child(CustomJobIconConfiguration.PLUGIN_PATH);
        parent.mkdirs();
        assertTrue(parent.exists());

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);
            HttpResponse response = descriptor.doCleanup(mockReq);

            validateResponse(response, HttpServletResponse.SC_OK, null, null);
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanupOnlyUsedIcons(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();
        CustomJobIcon customIcon = new CustomJobIcon(DUMMY_PNG);

        FreeStyleProject project1 = r.jenkins.createProject(FreeStyleProject.class, "job1");
        FreeStyleProject project2 = r.jenkins.createProject(FreeStyleProject.class, "job2");

        project1.setIcon(customIcon);
        project2.setIcon(customIcon);

        FilePath dummy = createCustomIconFile(r);

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);
            HttpResponse response = descriptor.doCleanup(mockReq);

            validateResponse(response, HttpServletResponse.SC_OK, null, null);
            assertTrue(dummy.exists());
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanupUsedAndUnusedIcons(JenkinsRule r) throws Exception {
        FilePath dummy = createCustomIconFile(r);
        FilePath unused = createCustomIconFile(r);

        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();
        CustomJobIcon customIcon = new CustomJobIcon(dummy.getName());

        FreeStyleProject project1 = r.jenkins.createProject(FreeStyleProject.class, "job1");
        FreeStyleProject project2 = r.jenkins.createProject(FreeStyleProject.class, "job2");

        project1.setIcon(customIcon);
        project2.setIcon(customIcon);

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);
            HttpResponse response = descriptor.doCleanup(mockReq);

            validateResponse(response, HttpServletResponse.SC_OK, null, null);
            assertTrue(dummy.exists());
            assertFalse(unused.exists());
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)} when root does not exist.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanupNoRoot(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);

            FilePath parent = r.jenkins
                    .getRootPath()
                    .child(CustomJobIconConfiguration.USER_CONTENT_PATH)
                    .child(CustomJobIconConfiguration.PLUGIN_PATH);
            assertTrue(parent.delete());

            HttpResponse response = descriptor.doCleanup(mockReq);

            validateResponse(response, HttpServletResponse.SC_OK, null, null);
            assertFalse(parent.exists());
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)} if a file can not be deleted.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanupFileNotDeleted(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);

            FilePath file = createCustomIconFile(r);
            FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);

            try (@SuppressWarnings("unused")
                    MockedConstruction<FilePath> mocked = mockConstructionWithAnswer(FilePath.class, invocation -> {
                        String call = invocation.toString();
                        if (StringUtils.equals(call, "filePath.child(\"userContent\");")) {
                            return userContent;
                        } else if (StringUtils.equals(call, "filePath.exists();")) {
                            return true;
                        } else if (StringUtils.equals(call, "filePath.list();")) {
                            return List.of(file);
                        } else if (StringUtils.equals(call, "filePath.child(\n    \"" + file.getName() + "\"\n);")) {
                            FilePath mock = mock(FilePath.class);
                            when(mock.delete()).thenReturn(false);
                            return mock;
                        }
                        return fail("Unexpected invocation '" + call + "' - Test is broken!");
                    })) {
                HttpResponse response = descriptor.doCleanup(mockReq);
                validateResponse(response, HttpServletResponse.SC_OK, null, null);
            }

            assertTrue(file.exists());
            assertTrue(file.delete());
            assertFalse(file.exists());
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)} if a file can not be deleted due to an exception.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanupFileNotDeletedWithException(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);

            FilePath file = createCustomIconFile(r);
            File remoteFile = new File(file.getRemote());

            // jenkins is pretty brutal when deleting files...
            Thread blocker = new Thread() {
                @Override
                public void run() {
                    while (!this.isInterrupted()) {
                        if (!remoteFile.setReadOnly()) {
                            LOGGER.warning("Unable to set file to read-only!");
                        }
                    }
                }
            };

            blocker.start();
            assertTrue(file.exists());

            HttpResponse response = descriptor.doCleanup(mockReq);
            validateResponse(response, HttpServletResponse.SC_OK, null, null);

            blocker.interrupt();
            response = descriptor.doCleanup(mockReq);

            validateResponse(response, HttpServletResponse.SC_OK, null, null);
            assertFalse(file.exists());
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)} if a file can not be deleted due to an exception.
     *
     * @throws Exception in case anything goes wrong
     * @implNote Sometimes {@link #testDoCleanupFileNotDeletedWithException(JenkinsRule)} does not work.
     */
    @Test
    void testDoCleanupFileNotDeletedWithMockedException(JenkinsRule r) throws Exception {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);

            FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);
            FilePath file = createCustomIconFile(r);

            try (@SuppressWarnings("unused")
                    MockedConstruction<FilePath> mocked = mockConstructionWithAnswer(FilePath.class, invocation -> {
                        String call = invocation.toString();
                        if (StringUtils.equals(call, "filePath.child(\"userContent\");")) {
                            return userContent;
                        } else if (StringUtils.equals(call, "filePath.exists();")) {
                            return true;
                        } else if (StringUtils.equals(call, "filePath.list();")) {
                            return List.of(file);
                        } else if (StringUtils.equals(call, "filePath.child(\n    \"" + file.getName() + "\"\n);")) {
                            throw new IOException("Mocked Exception!");
                        }
                        return fail("Unexpected invocation '" + call + "' - Test is broken!");
                    })) {
                HttpResponse response = descriptor.doCleanup(mockReq);
                validateResponse(response, HttpServletResponse.SC_OK, null, null);
            }

            assertTrue(file.exists());
            assertTrue(file.delete());
            assertFalse(file.exists());
        }
    }
}
