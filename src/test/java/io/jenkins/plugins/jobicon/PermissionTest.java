package io.jenkins.plugins.jobicon;

import static io.jenkins.plugins.jobicon.utils.TestUtils.createCustomIconFile;
import static io.jenkins.plugins.jobicon.utils.TestUtils.createMultipartEntityBuffer;
import static io.jenkins.plugins.jobicon.utils.TestUtils.validateResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.User;
import hudson.security.ACL;
import hudson.security.ACLContext;
import io.jenkins.plugins.jobicon.CustomJobIcon.DescriptorImpl;
import io.jenkins.plugins.jobicon.utils.MockMultiPartRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Collections;
import jenkins.model.Jenkins;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest2;
import org.springframework.security.access.AccessDeniedException;

/**
 * Test various permission checks
 */
@WithJenkins
class PermissionTest {

    private static final String ADMINISTRATOR_USER = "administering_sloth";

    private static final String MANAGE_USER = "managing_axolotl";

    private static final String CONFIGURE_USER = "configuring_red_panda";

    private static final String READ_USER = "reading_duck";

    private static final String FILE_NAME_PATTERN =
            "^[0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}\\.png$";

    /**
     * Test behavior of {@link DescriptorImpl#doUploadIcon(StaplerRequest2, Item)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoUploadIcon(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");

        File upload = new File("./src/main/webapp/icons/default.svg");

        byte[] buffer = createMultipartEntityBuffer(upload);
        MockMultiPartRequest mockRequest = new MockMultiPartRequest(buffer);
        DescriptorImpl descriptor = new DescriptorImpl();

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());

        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        strategy.grant(Jenkins.ADMINISTER).onItems(project).to(ADMINISTRATOR_USER);
        strategy.grant(Jenkins.MANAGE).onItems(project).to(MANAGE_USER);
        strategy.grant(Item.CONFIGURE).onItems(project).to(CONFIGURE_USER);
        strategy.grant(Item.READ).onItems(project).to(READ_USER);
        r.jenkins.setAuthorizationStrategy(strategy);

        // unauthenticated
        assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, null));
        assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, project));

        // Item.READ
        try (ACLContext ignored = ACL.as(User.get(READ_USER, true, Collections.emptyMap()))) {
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, null));
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, project));

            strategy.grant(Jenkins.READ).onRoot().to(READ_USER);
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, project));
        }

        // Item.CONFIGURE
        try (ACLContext ignored = ACL.as(User.get(CONFIGURE_USER, true, Collections.emptyMap()))) {
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, null));

            HttpResponse response = descriptor.doUploadIcon(mockRequest, project);
            validateResponse(response, 0, FILE_NAME_PATTERN, null);
        }

        // Jenkins.MANAGE
        try (ACLContext ignored = ACL.as(User.get(MANAGE_USER, true, Collections.emptyMap()))) {
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, null));
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, project));

            strategy.grant(Jenkins.MANAGE).onRoot().to(MANAGE_USER);
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, project));
        }

        // Jenkins.ADMINISTER
        try (ACLContext ignored = ACL.as(User.get(ADMINISTRATOR_USER, true, Collections.emptyMap()))) {
            assertThrows(AccessDeniedException.class, () -> descriptor.doUploadIcon(mockRequest, null));

            HttpResponse response = descriptor.doUploadIcon(mockRequest, project);
            validateResponse(response, 0, FILE_NAME_PATTERN, null);

            strategy.grant(Jenkins.ADMINISTER).onRoot().to(ADMINISTRATOR_USER);
            response = descriptor.doUploadIcon(mockRequest, project);
            validateResponse(response, 0, FILE_NAME_PATTERN, null);
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#doCleanup(StaplerRequest2)}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testDoCleanup(JenkinsRule r) throws Exception {
        FilePath file = createCustomIconFile(r);

        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());

        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        strategy.grant(Jenkins.ADMINISTER).onRoot().to(ADMINISTRATOR_USER);
        strategy.grant(Jenkins.MANAGE).onRoot().to(MANAGE_USER);
        strategy.grant(Jenkins.READ).onRoot().to(READ_USER);
        r.jenkins.setAuthorizationStrategy(strategy);

        // unauthenticated
        assertThrows(AccessDeniedException.class, () -> descriptor.doCleanup(null));
        assertTrue(file.exists());

        // Jenkins.READ
        try (ACLContext ignored = ACL.as(User.get(READ_USER, true, Collections.emptyMap()))) {
            assertThrows(AccessDeniedException.class, () -> descriptor.doCleanup(null));
            assertTrue(file.exists());
        }

        // Jenkins.MANAGE
        try (ACLContext ignored = ACL.as(User.get(MANAGE_USER, true, Collections.emptyMap()))) {
            HttpResponse response = descriptor.doCleanup(null);
            validateResponse(response, HttpServletResponse.SC_OK, null, null);
            assertFalse(file.exists());
        }

        // Jenkins.ADMINISTER
        try (ACLContext ignored = ACL.as(User.get(ADMINISTRATOR_USER, true, Collections.emptyMap()))) {
            HttpResponse response = descriptor.doCleanup(null);
            validateResponse(response, HttpServletResponse.SC_OK, null, null);
            assertFalse(file.exists());
        }
    }

    /**
     * Test behavior of {@link CustomJobIconConfiguration#getDiskUsage()}.
     */
    @Test
    void testGetDiskUsage(JenkinsRule r) {
        CustomJobIconConfiguration descriptor = new CustomJobIconConfiguration();

        r.jenkins.setSecurityRealm(r.createDummySecurityRealm());

        MockAuthorizationStrategy strategy = new MockAuthorizationStrategy();
        strategy.grant(Jenkins.ADMINISTER).onRoot().to(ADMINISTRATOR_USER);
        strategy.grant(Jenkins.MANAGE).onRoot().to(MANAGE_USER);
        strategy.grant(Jenkins.READ).onRoot().to(READ_USER);
        r.jenkins.setAuthorizationStrategy(strategy);

        // unauthenticated
        assertThrows(AccessDeniedException.class, descriptor::getDiskUsage);

        // Jenkins.READ
        try (ACLContext ignored = ACL.as(User.get(READ_USER, true, Collections.emptyMap()))) {
            assertThrows(AccessDeniedException.class, descriptor::getDiskUsage);
        }

        // Jenkins.MANAGE
        try (ACLContext ignored = ACL.as(User.get(MANAGE_USER, true, Collections.emptyMap()))) {
            String size = descriptor.getDiskUsage();
            assertEquals(FileUtils.byteCountToDisplaySize(0), size);
        }

        // Jenkins.ADMINISTER
        try (ACLContext ignored = ACL.as(User.get(ADMINISTRATOR_USER, true, Collections.emptyMap()))) {
            String size = descriptor.getDiskUsage();
            assertEquals(FileUtils.byteCountToDisplaySize(0), size);
        }
    }
}
