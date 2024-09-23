package io.jenkins.plugins.jobicon;

import hudson.model.JobIcon;
import org.apache.commons.lang.StringUtils;

/**
 * The common Custom Job Icon implementation.
 */
public abstract class AbstractCustomJobIcon extends JobIcon {

    private String icon;

    /**
     * Ctor.
     * @param icon the icon to use
     */
    protected AbstractCustomJobIcon(String icon) {
        this.icon = StringUtils.defaultString(icon, getDefaultIcon());
    }

    /**
     * Provides the default icon.
     * @return the default icon
     */
    public abstract String getDefaultIcon();

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public String getImageOf(String size) {
        return null;
    }

    @Override
    public String getDescription() {
        if (getOwner() != null) {
            return getOwner().getIconColor().getDescription();
        } else {
            return null;
        }
    }
}
