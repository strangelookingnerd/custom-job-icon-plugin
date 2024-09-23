package io.jenkins.plugins.jobicon;

import static io.jenkins.plugins.jobicon.CustomJobIconConfiguration.PLUGIN_PATH;
import static io.jenkins.plugins.jobicon.CustomJobIconConfiguration.USER_CONTENT_PATH;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobIcon;
import hudson.model.JobIconDescriptor;
import hudson.model.listeners.ItemListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import jenkins.model.Jenkins;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * A Custom Job Icon.
 */
public class CustomJobIcon extends AbstractCustomJobIcon {

    private static final Logger LOGGER = Logger.getLogger(CustomJobIcon.class.getName());

    private static final String DEFAULT_ICON_PATH = "plugin/custom-job-icon/icons/default.svg";

    /**
     * Ctor.
     * @param icon the icon to use
     */
    @DataBoundConstructor
    public CustomJobIcon(String icon) {
        super(icon);
        setIcon(icon);
    }

    @Override
    public String getDefaultIcon() {
        return DEFAULT_ICON_PATH;
    }

    /**
     * Get all icons that are currently available.
     * @return all the icons that have been uploaded, sorted descending by {@link FilePath#lastModified()}.
     */
    @NonNull
    public static Set<String> getAvailableIcons() {
        try {
            FilePath iconDir =
                    Jenkins.get().getRootPath().child(USER_CONTENT_PATH).child(PLUGIN_PATH);

            if (iconDir.exists()) {
                return iconDir.list().stream()
                        .sorted(Comparator.comparingLong((FilePath file) -> {
                                    try {
                                        return file.lastModified();
                                    } catch (IOException | InterruptedException ex) {
                                        return 0;
                                    }
                                })
                                .reversed())
                        .map(FilePath::getName)
                        .collect(Collectors.toSet());
            } else {
                return Set.of();
            }
        } catch (IOException | InterruptedException ex) {
            LOGGER.log(Level.WARNING, ex, () -> "Unable to list available icons!");
            return Set.of();
        }
    }

    @Override
    public String getImageOf(String size) {
        if (StringUtils.isNotEmpty(getIcon())) {
            return Stapler.getCurrentRequest2().getContextPath() + Jenkins.RESOURCE_PATH + "/" + USER_CONTENT_PATH + "/"
                    + PLUGIN_PATH + "/" + getIcon();
        } else {
            return Stapler.getCurrentRequest2().getContextPath() + Jenkins.RESOURCE_PATH + "/" + getDefaultIcon();
        }
    }

    @Extension
    public static class DescriptorImpl extends JobIconDescriptor {

        private static final int CHMOD = 0644;
        private static final long FILE_SIZE_MAX = 1024L * 1024L;

        @Override
        @NonNull
        public String getDisplayName() {
            return Messages.CustomJobIcon_description();
        }

        /**
         * Uploads an icon.
         * @param req  the request containing the file
         * @param item the item to configure
         * @return the filename or an error message
         */
        @RequirePOST
        public HttpResponse doUploadIcon(StaplerRequest2 req, @AncestorInPath Item item) {
            if (item != null) {
                item.checkPermission(Item.CONFIGURE);
            } else {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            }

            try {
                FileItem<?> file = req.getFileItem2("file");
                if (file == null || file.getSize() == 0) {
                    return HttpResponses.errorWithoutStack(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, Messages.Upload_invalidFile());
                } else if (file.getSize() > FILE_SIZE_MAX) {
                    return HttpResponses.errorWithoutStack(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            Messages.Upload_exceedsFileSize(file.getSize(), FILE_SIZE_MAX));
                }

                String filename = UUID.randomUUID() + ".png";
                FilePath iconDir =
                        Jenkins.get().getRootPath().child(USER_CONTENT_PATH).child(PLUGIN_PATH);
                iconDir.mkdirs();
                FilePath icon = iconDir.child(filename);
                icon.copyFrom(file.getInputStream());
                icon.chmod(CHMOD);

                return HttpResponses.text(filename);
            } catch (IOException | InterruptedException | ServletException ex) {
                LOGGER.log(Level.WARNING, "Error during Job Icon upload!", ex);
                return HttpResponses.errorWithoutStack(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            }
        }
    }

    /**
     * Item Listener to clean up unused icons when the job is deleted.
     * @author strangelookingnerd
     */
    @Extension
    public static class CustomJobIconCleanup extends ItemListener {

        @Override
        public void onDeleted(Item item) {
            if (item instanceof Job<?, ?>) {
                JobIcon icon = ((Job<?, ?>) item).getIcon();
                if (icon instanceof CustomJobIcon) {
                    String jobicon = ((CustomJobIcon) icon).getIcon();
                    if (StringUtils.isNotEmpty(jobicon)) {
                        // delete the icon only if there is no other usage
                        boolean orphan = Jenkins.get().getAllItems(Job.class).stream()
                                        .filter(job -> job.getIcon() instanceof CustomJobIcon
                                                && StringUtils.equals(
                                                        jobicon, ((CustomJobIcon) job.getIcon()).getIcon()))
                                        .limit(2)
                                        .count()
                                <= 1;

                        if (orphan) {
                            FilePath iconDir = Jenkins.get()
                                    .getRootPath()
                                    .child(USER_CONTENT_PATH)
                                    .child(PLUGIN_PATH);
                            try {
                                if (!iconDir.child(jobicon).delete()) {
                                    LOGGER.warning(() -> "Unable to delete Job Icon '" + jobicon + "' for Job '"
                                            + item.getFullName() + "'!");
                                }
                            } catch (IOException | InterruptedException ex) {
                                LOGGER.log(
                                        Level.WARNING,
                                        ex,
                                        () -> "Unable to delete Job Icon '" + jobicon + "' for Job '"
                                                + item.getFullName() + "'!");
                            }
                        }
                    }
                }
            }
        }
    }
}
