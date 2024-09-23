package io.jenkins.plugins.jobicon;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.JobIconDescriptor;
import io.jenkins.plugins.ionicons.Ionicons;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An Ionicon Job Icon.
 */
public class IoniconJobIcon extends AbstractCustomJobIcon {

    private static final String DEFAULT_ICON = "jenkins";

    /**
     * Ctor.
     * @param icon the icon to use
     */
    @DataBoundConstructor
    public IoniconJobIcon(String icon) {
        super(icon);
    }

    @Override
    public String getDefaultIcon() {
        return DEFAULT_ICON;
    }

    @Override
    public String getIconClassName() {
        return Ionicons.getIconClassName(getIcon());
    }

    @Extension
    public static class DescriptorImpl extends JobIconDescriptor {

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.IoniconJobIcon_description();
        }
    }
}
