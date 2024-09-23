package io.jenkins.plugins.jobicon;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.JobIconDescriptor;
import io.jenkins.plugins.fontawesome.FontAwesomeIcons;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * A Font Awesome Job Icon.
 */
public class FontAwesomeJobIcon extends AbstractCustomJobIcon {

    private static final String DEFAULT_ICON = "brands/jenkins";

    /**
     * Ctor.
     * @param icon the icon to use
     */
    @DataBoundConstructor
    public FontAwesomeJobIcon(String icon) {
        super(icon);
    }

    @Override
    public String getDefaultIcon() {
        return DEFAULT_ICON;
    }

    @Override
    public String getIconClassName() {
        return FontAwesomeIcons.getIconClassName(getIcon());
    }

    @Extension
    public static class DescriptorImpl extends JobIconDescriptor {

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.FontAwesomeJobIcon_description();
        }
    }
}
