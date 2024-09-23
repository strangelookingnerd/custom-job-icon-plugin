package io.jenkins.plugins.jobicon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Test
    @Disabled("Job DSL needs to support this")
    @ConfiguredWithCode("configuration-as-code.yml")
    void testConfigurationAsCode(JenkinsConfiguredWithCodeRule r) {
        FreeStyleProject project = r.jenkins.getItem("custom-icon", (Item) null, FreeStyleProject.class);
        assertInstanceOf(CustomJobIcon.class, project.getIcon());
        assertEquals("", ((CustomJobIcon) project.getIcon()).getIcon());

        project = r.jenkins.getItem("emoji-icon", (Item) null, FreeStyleProject.class);
        assertInstanceOf(EmojiJobIcon.class, project.getIcon());
        assertEquals("sloth", ((EmojiJobIcon) project.getIcon()).getIcon());

        project = r.jenkins.getItem("fontawesome-icon", (Item) null, FreeStyleProject.class);
        assertInstanceOf(FontAwesomeJobIcon.class, project.getIcon());
        assertEquals("regular/hand-peace", ((FontAwesomeJobIcon) project.getIcon()).getIcon());

        project = r.jenkins.getItem("ionicon-icon", (Item) null, FreeStyleProject.class);
        assertInstanceOf(IoniconJobIcon.class, project.getIcon());
        assertEquals("jenkins", ((IoniconJobIcon) project.getIcon()).getIcon());

        project = r.jenkins.getItem("oss-icon", (Item) null, FreeStyleProject.class);
        assertInstanceOf(OpenSourceJobIcon.class, project.getIcon());
        assertEquals("cdf-icon-color", ((OpenSourceJobIcon) project.getIcon()).getIcon());
    }
}
