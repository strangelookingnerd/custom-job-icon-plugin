package io.jenkins.plugins.jobicon;

import static io.jenkins.plugins.jobicon.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstructionWithAnswer;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.cloudbees.hudson.plugins.folder.Folder;
import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobIcon;
import hudson.model.JobIconDescriptor;
import io.jenkins.plugins.jobicon.CustomJobIcon.DescriptorImpl;
import io.jenkins.plugins.jobicon.utils.MockMultiPartRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.fileupload2.core.FileItem;
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
 * Custom FreeStyleProject  Icon Tests
 */
@WithJenkins
class CustomJobIconTest {

    private static final Logger LOGGER = Logger.getLogger(CustomJobIconTest.class.getName());

    private static final String DUMMY_PNG = "dummy.png";

    private static final String FILE_NAME_PATTERN =
            "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\\.png$";

    /**
     * Test behavior on a regular {@link Job}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testJob(JenkinsRule r) throws Exception {
        CustomJobIcon customIcon = new CustomJobIcon(DUMMY_PNG);
        assertEquals(DUMMY_PNG, customIcon.getIcon());
        assertNull(customIcon.getDescription());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);
        JobIcon icon = project.getIcon();

        assertInstanceOf(CustomJobIcon.class, icon);

        assertEquals(DUMMY_PNG, customIcon.getIcon());
        assertEquals(project.getIconColor().getDescription(), icon.getDescription());
    }

    /**
     * Test the default path of the image.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDefaultImagePath(JenkinsRule r) throws Exception {
        CustomJobIcon customIcon = new CustomJobIcon(null);
        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);
        JobIcon icon = project.getIcon();

        assertInstanceOf(CustomJobIcon.class, icon);

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            mockStaplerRequest(stapler);
            validateIcon(icon, "default.svg", null);
        }
    }

    /**
     * Test the context path of the image.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testImagePath(JenkinsRule r) throws Exception {
        CustomJobIcon customIcon = new CustomJobIcon(DUMMY_PNG);
        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);
        JobIcon icon = project.getIcon();

        assertInstanceOf(CustomJobIcon.class, icon);

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            mockStaplerRequest(stapler);
            validateIcon(icon, DUMMY_PNG, null);
        }
    }

    /**
     * Test behavior of {@link DescriptorImpl}.
     */
    @Test
    void testDescriptor(@SuppressWarnings("unused") JenkinsRule r) {
        CustomJobIcon customIcon = new CustomJobIcon(DUMMY_PNG);
        JobIconDescriptor descriptor = customIcon.getDescriptor();
        assertEquals(Messages.CustomJobIcon_description(), descriptor.getDisplayName());
    }

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadIcon(JenkinsRule r) throws Exception {
        File upload = new File("./src/main/webapp/icons/default.svg");

        byte[] buffer = createMultipartEntityBuffer(upload);
        MockMultiPartRequest mockRequest = new MockMultiPartRequest(buffer);
        DescriptorImpl descriptor = new DescriptorImpl();
        HttpResponse response = descriptor.doUploadIcon(mockRequest, null);

        validateResponse(response, 0, FILE_NAME_PATTERN, null);

        Field code = response.getClass().getDeclaredField("val$text");
        code.setAccessible(true);
        String filename = (String) code.get(response);

        FilePath parent = r.jenkins
                .getRootPath()
                .child(CustomJobIconConfiguration.USER_CONTENT_PATH)
                .child(CustomJobIconConfiguration.PLUGIN_PATH);
        FilePath file = parent.child(filename);
        assertTrue(file.exists());
    }

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)} with an item.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadWithItem(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        File upload = new File("./src/main/webapp/icons/default.svg");

        byte[] buffer = createMultipartEntityBuffer(upload);
        MockMultiPartRequest mockRequest = new MockMultiPartRequest(buffer);
        DescriptorImpl descriptor = new DescriptorImpl();
        HttpResponse response = descriptor.doUploadIcon(mockRequest, project);

        validateResponse(response, 0, FILE_NAME_PATTERN, null);

        Field code = response.getClass().getDeclaredField("val$text");
        code.setAccessible(true);
        String filename = (String) code.get(response);

        FilePath parent = r.jenkins
                .getRootPath()
                .child(CustomJobIconConfiguration.USER_CONTENT_PATH)
                .child(CustomJobIconConfiguration.PLUGIN_PATH);
        FilePath file = parent.child(filename);
        assertTrue(file.exists());
    }

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)} when there is no file in the request.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadIconNoFile(@SuppressWarnings("unused") JenkinsRule r) throws Exception {
        MockMultiPartRequest mockRequest = new MockMultiPartRequest(null);
        DescriptorImpl descriptor = new DescriptorImpl();
        HttpResponse response = descriptor.doUploadIcon(mockRequest, null);

        validateResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, Messages.Upload_invalidFile());
    }

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)} with a broken request.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadBrokenRequest(@SuppressWarnings("unused") JenkinsRule r) throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        try (MockedStatic<Stapler> stapler = mockStatic(Stapler.class)) {
            StaplerRequest2 mockReq = mockStaplerRequest(stapler);
            HttpResponse response = descriptor.doUploadIcon(mockReq, null);

            validateResponse(
                    response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, Messages.Upload_invalidFile());
        }
    }

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)} with a large file.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadLargeFile(@SuppressWarnings("unused") JenkinsRule r) throws Exception {
        File upload = File.createTempFile("large", ".png");
        upload.deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(upload, "rw")) {
            raf.setLength(1024L * 1024L * 2L);
        }

        byte[] buffer = createMultipartEntityBuffer(upload);
        MockMultiPartRequest mockRequest = new MockMultiPartRequest(buffer);
        DescriptorImpl descriptor = new DescriptorImpl();
        HttpResponse response = descriptor.doUploadIcon(mockRequest, null);

        validateResponse(
                response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                null,
                Messages.Upload_exceedsFileSize(mockRequest.getContentLength(), 1024L * 1024L));
    }

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)} with an empty file.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadEmptyFile(@SuppressWarnings("unused") JenkinsRule r) throws Exception {
        File upload = File.createTempFile("empty", ".png");
        upload.deleteOnExit();

        byte[] buffer = createMultipartEntityBuffer(upload);
        MockMultiPartRequest mockRequest = new MockMultiPartRequest(buffer) {
            @Override
            public int getContentLength() {
                return 0;
            }
        };

        DescriptorImpl descriptor = new DescriptorImpl();
        HttpResponse response = descriptor.doUploadIcon(mockRequest, null);

        validateResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, Messages.Upload_invalidFile());
    }

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)} when there are exceptions thrown.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadIconThrowingExceptions(@SuppressWarnings("unused") JenkinsRule r) throws Exception {
        List<String> exceptions = Arrays.asList("IOException", "InterruptedException", "ServletException");
        String exceptionMessage = "Oh no :(";

        for (String exception : exceptions) {
            MockMultiPartRequest mockRequest = new MockMultiPartRequest(null) {
                @Override
                public FileItem<?> getFileItem2(String name) throws IOException, ServletException {
                    return switch (exception) {
                        case "IOException" -> throw new IOException(exceptionMessage);
                        case "InterruptedException" -> throw new InterruptedIOException(exceptionMessage);
                        case "ServletException" -> throw new ServletException(exceptionMessage);
                        default -> fail("Unexpected exception '" + exception + "' - Test is broken!");
                    };
                }
            };

            DescriptorImpl descriptor = new DescriptorImpl();
            HttpResponse response = descriptor.doUploadIcon(mockRequest, null);

            validateResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null, exceptionMessage);
        }
    }

    /**
     * Test behavior of {@link CustomJobIcon#getAvailableIcons()}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testGetAvailableIcons(JenkinsRule r) throws Exception {
        Set<String> icons = CustomJobIcon.getAvailableIcons();

        assertNotNull(icons);
        assertTrue(icons.isEmpty());

        FilePath file1 = createCustomIconFile(r);
        FilePath file2 = createCustomIconFile(r);
        FilePath file3 = createCustomIconFile(r);

        icons = CustomJobIcon.getAvailableIcons();

        assertNotNull(icons);
        assertEquals(3, icons.size());

        Set<String> expected = Stream.of(file1, file2, file3)
                .sorted(Comparator.comparingLong((FilePath file) -> {
                            try {
                                return file.lastModified();
                            } catch (IOException | InterruptedException ex) {
                                return 0;
                            }
                        })
                        .reversed())
                .map(FilePath::getName)
                .collect(Collectors.toSet());

        assertEquals(expected, icons);
    }

    /**
     * Test behavior of {@link CustomJobIcon#getAvailableIcons()} when an exception is thrown in the main logic.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testGetAvailableIconsThrowingExceptions(JenkinsRule r) throws Exception {
        Set<String> icons = CustomJobIcon.getAvailableIcons();

        assertNotNull(icons);
        assertTrue(icons.isEmpty());

        createCustomIconFile(r);
        createCustomIconFile(r);
        createCustomIconFile(r);

        try (@SuppressWarnings("unused")
                MockedConstruction<FilePath> mocked = mockConstructionWithAnswer(FilePath.class, invocation -> {
                    String call = invocation.toString();
                    if (StringUtils.equals(call, "filePath.child(\"userContent\");")) {
                        throw new IOException("Mocked Exception!");
                    }
                    return fail("Unexpected invocation '" + call + "' - Test is broken!");
                })) {
            icons = CustomJobIcon.getAvailableIcons();

            assertNotNull(icons);
            assertTrue(icons.isEmpty());
        }
    }

    /**
     * Test behavior of {@link CustomJobIcon#getAvailableIcons()} when an exception is thrown in the comparator logic.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testGetAvailableIconsComparatorThrowingExceptions(JenkinsRule r) throws Exception {
        Set<String> icons = CustomJobIcon.getAvailableIcons();

        assertNotNull(icons);
        assertTrue(icons.isEmpty());

        FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);
        FilePath iconDir = userContent.child(CustomJobIconConfiguration.PLUGIN_PATH);

        FilePath file1 = createCustomIconFile(r);
        FilePath file2 = createCustomIconFile(r);
        FilePath file3 = createCustomIconFile(r);

        final int[] counter = {0};

        try (@SuppressWarnings("unused")
                MockedConstruction<FilePath> mocked = mockConstructionWithAnswer(FilePath.class, invocation -> {
                    String call = invocation.toString();

                    if (StringUtils.equals(call, "filePath.child(\"userContent\");")) {
                        return userContent;
                    } else if (StringUtils.equals(call, "filePath.exists();")) {
                        return true;
                    } else if (StringUtils.equals(call, "filePath.list();")) {
                        return iconDir.list();
                    } else if (StringUtils.equals(call, "filePath.lastModified();")) {
                        if (counter[0] == 0) {
                            counter[0] = 1;
                            return file3.lastModified();
                        } else if (counter[0] == 1) {
                            counter[0] = 2;
                            return file2.lastModified();
                        } else if (counter[0] == 2) {
                            counter[0] = 0;
                            throw new IOException("Mocked Exception!");
                        }
                    } else if (StringUtils.equals(call, "filePath.getName();")) {
                        if (counter[0] == 0) {
                            counter[0] = 1;
                            return file3.getName();
                        } else if (counter[0] == 1) {
                            counter[0] = 2;
                            return file2.getName();
                        } else if (counter[0] == 2) {
                            counter[0] = 0;
                            return file1.getName();
                        }
                    }
                    return fail("Unexpected invocation '" + call + "' - Test is broken!");
                })) {
            icons = CustomJobIcon.getAvailableIcons();

            assertNotNull(icons);
            assertEquals(3, icons.size());
        }

        Set<String> expected = Stream.of(file1, file2, file3)
                .sorted(Comparator.comparingLong((FilePath file) -> {
                            try {
                                return file.lastModified();
                            } catch (IOException | InterruptedException ex) {
                                return 0;
                            }
                        })
                        .reversed())
                .map(FilePath::getName)
                .collect(Collectors.toSet());

        assertEquals(expected, icons);
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testCleanupListener(JenkinsRule r) throws Exception {
        FilePath file = createCustomIconFile(r);
        CustomJobIcon customIcon = new CustomJobIcon(file.getName());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);

        assertTrue(file.exists());
        project.delete();
        assertFalse(file.exists());
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testCleanupListenerOtherProjects(JenkinsRule r) throws Exception {
        FilePath file = createCustomIconFile(r);
        CustomJobIcon customIcon = new CustomJobIcon(file.getName());

        FreeStyleProject project1 = r.jenkins.createProject(FreeStyleProject.class, "job-1");
        project1.setIcon(customIcon);

        FreeStyleProject project2 = r.jenkins.createProject(FreeStyleProject.class, "job-2");
        project2.setIcon(new CustomJobIcon(DUMMY_PNG));

        FreeStyleProject project3 = r.jenkins.createProject(FreeStyleProject.class, "job-3");
        Folder project4 = r.jenkins.createProject(Folder.class, "job-4");

        assertTrue(file.exists());

        project1.delete();
        project2.delete();
        project3.delete();
        project4.delete();

        assertFalse(file.exists());
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testCleanupListenerNoFile(JenkinsRule r) throws Exception {
        FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);
        FilePath iconDir = userContent.child(CustomJobIconConfiguration.PLUGIN_PATH);
        iconDir.mkdirs();

        FilePath file = iconDir.child(System.currentTimeMillis() + ".png");
        CustomJobIcon customIcon = new CustomJobIcon(file.getName());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);

        assertFalse(file.exists());
        project.delete();
        assertFalse(file.exists());
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testCleanupListenerDefaultIcon(JenkinsRule r) throws Exception {
        FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);
        FilePath iconDir = userContent.child(CustomJobIconConfiguration.PLUGIN_PATH);
        iconDir.mkdirs();

        CustomJobIcon customIcon = new CustomJobIcon(null);

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);

        project.delete();

        assertTrue(iconDir.exists());
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)} when the icon is used elsewhere.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testCleanupListenerIconUsed(JenkinsRule r) throws Exception {
        FilePath file = createCustomIconFile(r);
        CustomJobIcon customIcon = new CustomJobIcon(file.getName());

        FreeStyleProject project1 = r.jenkins.createProject(FreeStyleProject.class, "job1");
        project1.setIcon(customIcon);

        FreeStyleProject project2 = r.jenkins.createProject(FreeStyleProject.class, "job2");
        project2.setIcon(customIcon);

        assertTrue(file.exists());
        project1.delete();
        assertTrue(file.exists());
        project2.delete();
        assertFalse(file.exists());
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)} when the file is not deleted.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testCleanupListenerFileNotDeleted(JenkinsRule r) throws Exception {
        FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);

        FilePath file = createCustomIconFile(r);
        CustomJobIcon customIcon = new CustomJobIcon(file.getName());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);

        try (@SuppressWarnings("unused")
                MockedConstruction<FilePath> mocked = mockConstructionWithAnswer(FilePath.class, invocation -> {
                    String call = invocation.toString();
                    if (StringUtils.equals(call, "filePath.child(\"userContent\");")) {
                        return userContent;
                    } else if (StringUtils.equals(call, "filePath.child(\n    \"" + file.getName() + "\"\n);")) {
                        FilePath mock = mock(FilePath.class);
                        when(mock.delete()).thenReturn(false);
                        return mock;
                    }
                    return fail("Unexpected invocation '" + call + "' - Test is broken!");
                })) {
            project.delete();
        }

        assertTrue(file.exists());
        file.delete();
        assertFalse(file.exists());
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)} when the file is not deleted due to an exception.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testCleanupListenerFileNotDeletedWithException(JenkinsRule r) throws Exception {
        FilePath file = createCustomIconFile(r);
        File remoteFile = new File(file.getRemote());

        CustomJobIcon customIcon = new CustomJobIcon(file.getName());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);

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

        project.delete();
        blocker.interrupt();

        file.delete();
        assertFalse(file.exists());
    }

    /**
     * Test behavior of {@link CustomJobIcon.CustomJobIconCleanup#onDeleted(Item)} when the file is not deleted due to an exception.
     *
     * @throws Exception in case anything goes wrong
     * @implNote Sometimes {@link #testCleanupListenerFileNotDeletedWithException(JenkinsRule)} does not work.
     */
    @Test
    void testCleanupListenerFileNotDeletedWithMockedException(JenkinsRule r) throws Exception {
        FilePath userContent = r.jenkins.getRootPath().child(CustomJobIconConfiguration.USER_CONTENT_PATH);

        FilePath file = createCustomIconFile(r);
        CustomJobIcon customIcon = new CustomJobIcon(file.getName());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);

        try (@SuppressWarnings("unused")
                MockedConstruction<FilePath> mocked = mockConstructionWithAnswer(FilePath.class, invocation -> {
                    String call = invocation.toString();
                    if (StringUtils.equals(call, "filePath.child(\"userContent\");")) {
                        return userContent;
                    } else if (StringUtils.equals(call, "filePath.child(\n    \"" + file.getName() + "\"\n);")) {
                        throw new IOException("Mocked Exception!");
                    }
                    return fail("Unexpected invocation '" + call + "' - Test is broken!");
                })) {
            project.delete();
        }

        assertTrue(file.exists());
        file.delete();
        assertFalse(file.exists());
    }
}
