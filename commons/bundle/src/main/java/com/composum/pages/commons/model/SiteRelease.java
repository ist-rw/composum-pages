package com.composum.pages.commons.model;

import com.composum.pages.commons.request.DisplayMode;
import com.composum.pages.commons.service.VersionsService;
import com.composum.pages.commons.util.PagesUtil;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.util.I18N;
import com.composum.platform.commons.request.AccessMode;
import com.composum.sling.platform.staging.Release;
import com.composum.sling.platform.staging.StagingConstants;
import com.composum.sling.platform.staging.StagingReleaseManager;
import com.composum.sling.platform.staging.impl.StagingUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.composum.pages.commons.PagesConstants.KEY_CURRENT_RELEASE;
import static com.composum.pages.commons.PagesConstants.PROP_LAST_MODIFIED;

/**
 * Models a release for a site. The actual resource is the metadata node of the release, which is located below
 * {@value StagingConstants#RELEASE_ROOT_PATH}, e.g. /var/composum/content/ist/composum/cpl:releases/r1.0/metaData .
 */
public class SiteRelease extends AbstractModel implements Comparable<SiteRelease> {

    private static final Logger LOG = LoggerFactory.getLogger(SiteRelease.class);

    protected Release stagingRelease;
    private transient Calendar creationDate;
    private transient Calendar lastModified;

    private transient SiteRelease previousRelease;
    private transient Map<String, String> nextReleaseNumbers;

    public static boolean isSiteRelease(Resource resource) {
        return resource != null && StagingUtils.RELEASE_PATH_PATTERN.matcher(resource.getPath()).matches();
    }

    public SiteRelease() {
        // empty default constructor
    }

    public SiteRelease(BeanContext context, Release release) {
        this.stagingRelease = release;
        initialize(context, release.getMetaDataNode());
    }

    public SiteRelease(BeanContext context, Resource resource) {
        initialize(context, resource);
    }

    @Nonnull
    @Override
    protected Resource determineResource(@Nullable Resource initialResource) {
        if (stagingRelease == null) {
            StagingReleaseManager releaseManager = this.context.getService(StagingReleaseManager.class);
            stagingRelease = releaseManager.findReleaseByReleaseResource(initialResource);
        }
        return Objects.requireNonNull(stagingRelease).getMetaDataNode();
    }

    @Override
    protected void initializeWithResource(@Nonnull Resource releaseMetadataNode) {
        super.initializeWithResource(releaseMetadataNode);
        creationDate = getProperty(JcrConstants.JCR_CREATED, Calendar.class);
    }

    /**
     * use requested edit mode as mode for the component rendering;
     * for the site the mode is set to 'none' in the page template to avoid container / element edit behavior
     */
    @Override
    public boolean isEditMode() {
        return DisplayMode.isEditMode(DisplayMode.requested(context));
    }

    public boolean isCurrent() {
        return KEY_CURRENT_RELEASE.equals(getKey());
    }

    public boolean isPublic() {
        return getCategories().contains(AccessMode.PUBLIC.name().toLowerCase());
    }

    public boolean isPreview() {
        return getCategories().contains(AccessMode.PREVIEW.name().toLowerCase());
    }

    public SiteRelease getPreviousRelease() {
        if (previousRelease == null) {
            try {
                Release previous = getStagingRelease().getPreviousRelease();
                if (previous != null) {
                    previousRelease = new SiteRelease(getContext(), previous);
                }
            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return previousRelease;
    }

    public Map<String, String> getNextReleaseNumbers() {
        if (nextReleaseNumbers == null) {
            nextReleaseNumbers = getContext().getService(StagingReleaseManager.class).nextRealeaseNumbers(getResource());
        }
        return nextReleaseNumbers;
    }

    @Nonnull
    @Override
    public String getPath() {
        return stagingRelease.getPath();
    }

    public String getTitleString() {
        String title = getTitle();
        return StringUtils.isNotBlank(title) ? title
                : isCurrent() ? I18N.get(getContext().getRequest(),
                "the open next release") : "-- --";
    }

    public String getKey() {
        return stagingRelease.getNumber();
    }

    /**
     * The label that is set on a document version when it is in a release.
     */
    public String getLabel() {
        return stagingRelease.getReleaseLabel();
    }

    public List<String> getCategories() {
        return stagingRelease.getMarks();
    }

    public Calendar getLastModified() {
        if (lastModified == null) {
            lastModified = getProperty(PROP_LAST_MODIFIED, null, Calendar.class);
        }
        return lastModified;
    }

    public String getLastModifiedString() {
        return PagesUtil.getTimestampString(getLastModified());
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public String getCreationDateString() {
        return PagesUtil.getTimestampString(getCreationDate());
    }

    /**
     * The underlying release info from the platform.
     */
    public Release getStagingRelease() {
        return stagingRelease;
    }

    public List<ContentVersion> getChanges() {
        return getChanges(null);
    }

    public List<ContentVersion> getChanges(@Nullable final VersionsService.ContentVersionFilter filter) {
        try {
            return getVersionsService().findReleaseChanges(getContext(), this, filter);
        } catch (RepositoryException ex) {
            LOG.error(ex.getMessage(), ex);
            return new ArrayList<>();
        }
    }

    @Override
    public int compareTo(@Nonnull SiteRelease o) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(creationDate, o.creationDate);
        builder.append(getPath(), o.getPath());
        return builder.toComparison();
    }
}
