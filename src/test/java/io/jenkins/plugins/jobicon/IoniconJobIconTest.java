package io.jenkins.plugins.jobicon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.JobIcon;
import hudson.model.JobIconDescriptor;
import io.jenkins.plugins.ionicons.Ionicons;
import io.jenkins.plugins.jobicon.IoniconJobIcon.DescriptorImpl;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Ionicon Job Icon Tests
 */
@WithJenkins
class IoniconJobIconTest {

    private static final String DUMMY_ICON = "dummy";
    private static final String DEFAULT_ICON = "jenkins";

    private static final String DUMMY_ICON_CLASS_NAME = Ionicons.getIconClassName(DUMMY_ICON);
    private static final String DEFAULT_ICON_CLASS_NAME = Ionicons.getIconClassName(DEFAULT_ICON);

    /**
     * Test behavior on a regular {@link Job}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testJob(JenkinsRule r) throws Exception {
        IoniconJobIcon customIcon = new IoniconJobIcon(null);
        assertEquals(DEFAULT_ICON, customIcon.getIcon());
        assertNull(customIcon.getImageOf(null));
        assertEquals(DEFAULT_ICON_CLASS_NAME, customIcon.getIconClassName());

        customIcon = new IoniconJobIcon(DUMMY_ICON);
        assertNull(customIcon.getDescription());
        assertEquals(DUMMY_ICON, customIcon.getIcon());
        assertNull(customIcon.getImageOf(null));
        assertEquals(DUMMY_ICON_CLASS_NAME, customIcon.getIconClassName());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);
        JobIcon icon = project.getIcon();

        assertInstanceOf(IoniconJobIcon.class, icon);
        assertEquals(project.getIconColor().getDescription(), icon.getDescription());
    }

    /**
     * Test behavior of {@link DescriptorImpl}.
     */
    @Test
    void testDescriptor(@SuppressWarnings("unused") JenkinsRule r) {
        IoniconJobIcon customIcon = new IoniconJobIcon(DUMMY_ICON);
        JobIconDescriptor descriptor = customIcon.getDescriptor();
        assertEquals(Messages.IoniconJobIcon_description(), descriptor.getDisplayName());
    }
}
