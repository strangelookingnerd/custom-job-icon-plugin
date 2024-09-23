package io.jenkins.plugins.jobicon;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.JobIconDescriptor;
import jenkins.plugins.foldericon.Emojis;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * An Emoji Job Icon.
 */
public class EmojiJobIcon extends AbstractCustomJobIcon {

    private static final String DEFAULT_ICON = "sloth";

    /**
     * Ctor.
     * @param icon the icon to use
     */
    @DataBoundConstructor
    public EmojiJobIcon(String icon) {
        super(icon);
    }

    @Override
    public String getDefaultIcon() {
        return DEFAULT_ICON;
    }

    @Override
    public String getIconClassName() {
        return Emojis.getIconClassName(getIcon());
    }

    @Extension
    public static class DescriptorImpl extends JobIconDescriptor {

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.EmojiJobIcon_description();
        }
    }
}
