package io.jenkins.plugins.jobicon;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.JobIconDescriptor;
import io.jenkins.plugins.oss.symbols.OpenSourceSymbols;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An Open Source Job Icon.
 */
public class OpenSourceJobIcon extends AbstractCustomJobIcon {

    private static final String DEFAULT_ICON = "cdf-icon-color";

    /**
     * Ctor.
     * @param icon the icon to use
     */
    @DataBoundConstructor
    public OpenSourceJobIcon(String icon) {
        super(icon);
    }

    @Override
    public String getDefaultIcon() {
        return DEFAULT_ICON;
    }

    @Override
    public String getIconClassName() {
        return OpenSourceSymbols.getIconClassName(getIcon());
    }

    @Extension
    public static class DescriptorImpl extends JobIconDescriptor {

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.OpenSourceJobIcon_description();
        }
    }
}
