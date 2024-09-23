package io.jenkins.plugins.jobicon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.JobIcon;
import hudson.model.JobIconDescriptor;
import io.jenkins.plugins.jobicon.EmojiJobIcon.DescriptorImpl;
import jenkins.plugins.foldericon.Emojis;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Emoji Job Icon Tests
 */
@WithJenkins
class EmojiJobIconTest {

    private static final String DUMMY_ICON = "dummy";
    private static final String DEFAULT_ICON = "sloth";

    private static final String DUMMY_ICON_CLASS_NAME = Emojis.getIconClassName(DUMMY_ICON);
    private static final String DEFAULT_ICON_CLASS_NAME = Emojis.getIconClassName(DEFAULT_ICON);

    /**
     * Test behavior on a regular {@link Job}.
     *
     * @throws Exception in case anything goes wrong
     */
    @Test
    void testJob(JenkinsRule r) throws Exception {
        EmojiJobIcon customIcon = new EmojiJobIcon(null);
        assertEquals(DEFAULT_ICON, customIcon.getIcon());
        assertNull(customIcon.getImageOf(null));
        assertEquals(DEFAULT_ICON_CLASS_NAME, customIcon.getIconClassName());

        customIcon = new EmojiJobIcon(DUMMY_ICON);
        assertNull(customIcon.getDescription());
        assertEquals(DUMMY_ICON, customIcon.getIcon());
        assertNull(customIcon.getImageOf(null));
        assertEquals(DUMMY_ICON_CLASS_NAME, customIcon.getIconClassName());

        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");
        project.setIcon(customIcon);
        JobIcon icon = project.getIcon();

        assertInstanceOf(EmojiJobIcon.class, icon);
        assertEquals(project.getIconColor().getDescription(), icon.getDescription());
    }

    /**
     * Test behavior of {@link DescriptorImpl}.
     */
    @Test
    void testDescriptor(@SuppressWarnings("unused") JenkinsRule r) {
        EmojiJobIcon customIcon = new EmojiJobIcon(DUMMY_ICON);
        JobIconDescriptor descriptor = customIcon.getDescriptor();
        assertEquals(Messages.EmojiJobIcon_description(), descriptor.getDisplayName());
    }
}
