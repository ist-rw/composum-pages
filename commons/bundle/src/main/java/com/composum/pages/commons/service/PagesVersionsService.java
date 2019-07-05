package com.composum.pages.commons.service;

import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.model.PageVersion;
import com.composum.pages.commons.model.SiteRelease;
import com.composum.sling.core.BeanContext;
import com.composum.sling.platform.staging.ReleasedVersionable;
import com.composum.sling.platform.staging.StagingReleaseManager;
import com.composum.sling.platform.staging.versions.PlatformVersionsService;
import org.apache.commons.collections4.SetUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Versions Service"
        }
)
public class PagesVersionsService implements VersionsService {

    private static final Logger LOG = LoggerFactory.getLogger(PagesVersionsService.class);

    @Reference
    protected PageManager pageManager;

    @Reference
    protected StagingReleaseManager releaseManager;

    @Reference
    protected PlatformVersionsService platformVersionsService;

    @Override
    public boolean isModified(final Page page) {
        try {
            if (page.getContent() == null || page.getContent().getResource() == null)
                return false;
            PlatformVersionsService.Status status = platformVersionsService.getStatus(page.getContent().getResource(), null);
            if (status == null)
                return false;
            switch (status.getActivationState()) {
                case activated:
                case deactivated:
                    return false;
                case initial:
                case modified:
                    return true;
            }
            throw new IllegalStateException("Unknown state " + status.getActivationState()); // impossible
        } catch (RepositoryException e) {
            LOG.error("Unexpected error", e);
            return false;
        }
    }

    @Override
    public void restoreVersion(final BeanContext context, String path, String versionName)
            throws RepositoryException {
        VersionManager manager = getVersionManager(context);
        if (LOG.isInfoEnabled()) {
            LOG.info("restoreVersion(" + path + "," + versionName + ")");
        }
        manager.restore(path, versionName, false);
        // TODO(hps,2019-05-20) removing everything that came later is wrong from a users perspective.
        // Unfortunately, the VersionManager does not offer any way to copy out an old version, and if we just
        // restore an old version, we'll another branch when checking in again, which would be ... inconvenient. ...
        VersionHistory history = manager.getVersionHistory(path);
        final VersionIterator allVersions = history.getAllVersions();
        while (allVersions.hasNext()) {
            final Version version = allVersions.nextVersion();
            if (version.getName().equals(versionName)) {
                break;
            }
        }
        while (allVersions.hasNext()) {
            final Version version = allVersions.nextVersion();
            if (LOG.isDebugEnabled()) {
                LOG.debug("restoreVersion.remove(" + path + "," + version.getName() + ")");
            }
            history.removeVersion(version.getName());
        }
        manager.checkout(path);
    }

    /**
     * @return a collection of all versionables which are changed in a release in comparision to the release before
     */
    @Override
    public List<PageVersion> findReleaseChanges(@Nonnull final BeanContext context,
                                                @Nullable final SiteRelease siteRelease) throws RepositoryException {
        List<PageVersion> result = new ArrayList<>();
        if (siteRelease != null) {
            StagingReleaseManager.Release release = siteRelease.getStagingRelease();
            List<PlatformVersionsService.Status> changes = platformVersionsService.findReleaseChanges(release);
            for (PlatformVersionsService.Status status : changes) {
                PageVersion pv = new PageVersion(siteRelease, status);
                result.add(pv);
            }
            result.sort(Comparator.comparing(PageVersion::getPath));
        }
        return result;
    }

    @Override
    public Collection<Page> findModifiedPages(final BeanContext context, final Resource root) {
        List<ReleasedVersionable> currentContents = releaseManager.listCurrentContents(root);
        StagingReleaseManager.Release currentRelease = platformVersionsService.getDefaultRelease(root);
        List<Page> result = currentContents.stream()
                .map(releasedVersionable -> root.getChild(releasedVersionable.getRelativePath()))
                .map(resource -> pageManager.getContainingPage(context, resource))
                .filter(this::isModified)
                .sorted(Comparator.comparing(Page::getPath))
                .collect(Collectors.toList());
        return result;
    }

    protected VersionManager getVersionManager(final BeanContext context)
            throws RepositoryException {
        SlingHttpServletRequest request = context.getRequest();
        VersionManager versionManager = (VersionManager) request.getAttribute(VersionManager.class.getName());
        if (versionManager == null) {
            final JackrabbitSession session = Objects.requireNonNull(
                    (JackrabbitSession) context.getResolver().adaptTo(Session.class));
            versionManager = session.getWorkspace().getVersionManager();
            request.setAttribute(VersionManager.class.getName(), versionManager);
        }
        return versionManager;
    }

}
