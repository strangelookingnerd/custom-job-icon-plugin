package io.jenkins.plugins.jobicon;

import static io.jenkins.plugins.jobicon.utils.TestUtils.createCustomIconFile;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import hudson.FilePath;
import hudson.model.FreeStyleProject;
import java.time.Duration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.htmlunit.WebAssert;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlOption;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Various UI Tests.
 */
@WithJenkins
class UITest {

    /**
     * Test behavior of the CustomJobIconConfiguration.
     *
     * @throws Throwable in case anything goes wrong
     */
    @Test
    void testCustomJobIconGlobalConfiguration(JenkinsRule r) throws Throwable {
        FilePath file = createCustomIconFile(r);

        try (JenkinsRule.WebClient webClient = r.createWebClient()) {
            HtmlPage appearance = webClient.goTo("manage/appearance");
            WebAssert.assertTextPresent(appearance, "Custom Job Icons");
            WebAssert.assertTextPresent(
                    appearance, "Disk usage of icons:   " + FileUtils.byteCountToDisplaySize(file.length()));

            appearance.getElementsByTagName("input").stream()
                    .filter(input -> StringUtils.equals(input.getAttribute("value"), "Cleanup unused icons"))
                    .findFirst()
                    .orElseThrow(() -> fail("Unable to cleanup unused icons"))
                    .click();

            assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
                while (file.exists()) {
                    Thread.onSpinWait();
                }
            });
            assertFalse(file.exists());

            appearance = (HtmlPage) appearance.refresh();
            WebAssert.assertTextPresent(appearance, "Disk usage of icons:   " + FileUtils.byteCountToDisplaySize(0L));
        }
    }

    /**
     * Test behavior of the job icon option selection.
     *
     * @throws Throwable in case anything goes wrong
     */
    @Test
    void testCustomJobIconOption(JenkinsRule r) throws Throwable {
        selectJobIconOption(r, Messages.CustomJobIcon_description());
    }

    /**
     * Test behavior of the job icon option selection.
     *
     * @throws Throwable in case anything goes wrong
     */
    @Test
    void testEmojiJobIconOption(JenkinsRule r) throws Throwable {
        selectJobIconOption(r, Messages.EmojiJobIcon_description());
    }

    /**
     * Test behavior of the job icon option selection.
     *
     * @throws Throwable in case anything goes wrong
     */
    @Test
    void testFontAwesomeJobIconOption(JenkinsRule r) throws Throwable {
        selectJobIconOption(r, Messages.FontAwesomeJobIcon_description());
    }

    /**
     * Test behavior of the job icon option selection.
     *
     * @throws Throwable in case anything goes wrong
     */
    @Test
    void testIoniconJobIconOption(JenkinsRule r) throws Throwable {
        selectJobIconOption(r, Messages.IoniconJobIcon_description());
    }

    /**
     * Test behavior of the job icon option selection.
     *
     * @throws Throwable in case anything goes wrong
     */
    @Test
    void testOpenSourceJobIconOption(JenkinsRule r) throws Throwable {
        selectJobIconOption(r, Messages.OpenSourceJobIcon_description());
    }

    private static void selectJobIconOption(JenkinsRule r, String jobIcon) throws Throwable {
        FreeStyleProject project = r.jenkins.createProject(FreeStyleProject.class, "job");

        try (JenkinsRule.WebClient webClient = r.createWebClient()) {
            HtmlPage configure = webClient.getPage(project, "configure");
            HtmlForm form = configure.getFormByName("config");

            HtmlOption selection = (HtmlOption) configure.getElementsByTagName("option").stream()
                    .filter(option -> StringUtils.equals(option.getTextContent(), jobIcon))
                    .findFirst()
                    .orElseThrow(() -> fail("Unable to select job icon option " + jobIcon));

            assertFalse(selection.isSelected());
            configure = selection.click();
            assertTrue(selection.isSelected());
            r.submit(form);

            configure = (HtmlPage) configure.refresh();
            selection = (HtmlOption) configure.getElementsByTagName("option").stream()
                    .filter(option -> StringUtils.equals(option.getTextContent(), jobIcon))
                    .findFirst()
                    .orElseThrow(() -> fail("Unable to select job icon option " + jobIcon));

            assertTrue(selection.isSelected());
        }
    }
}
