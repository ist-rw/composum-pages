package com.composum.pages.commons.service;

import com.composum.pages.commons.model.Folder;
import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.model.Release;
import com.composum.sling.core.BeanContext;
import com.composum.sling.platform.staging.StagingUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Pages Versions Service"
        }
)
public class PagesVersionsService implements VersionsService {

    private static final Logger LOG = LoggerFactory.getLogger(PagesVersionsService.class);

    @Reference
    protected PageManager pageManager;

    @Override
    public boolean isModified(final Page page) {
        try {
            Calendar lastModified = page.getContent().getLastModified();
            if (lastModified != null) {
                VersionManager versionManager = getVersionManager(page.getContext());
                Version currentVersion = versionManager.getBaseVersion(page.getContent().getPath());
                Calendar currentVersionCreated = currentVersion.getCreated();
                return (lastModified.after(currentVersionCreated) || currentVersion.getName().equals("jcr:rootVersion"));
            }
            return false;
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    public void setVersionLabel(final BeanContext context, String path, String versionName, String label)
            throws RepositoryException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("setVersionLabel(" + path + "," + versionName + "," + label + ")");
        }
        final VersionHistory history = getVersionHistory(context, path);
        history.addVersionLabel(versionName, label, true);
    }

    @Override
    public void removeVersionLabel(final BeanContext context, String path, String label)
            throws RepositoryException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("removeVersionLabel(" + path + "," + label + ")");
        }
        final VersionHistory history = getVersionHistory(context, path);
        history.removeVersionLabel(label);
    }

    @Override
    public void restoreVersion(final BeanContext context, String path, String versionName)
            throws RepositoryException {
        VersionManager manager = getVersionManager(context);
        if (LOG.isInfoEnabled()) {
            LOG.info("restoreVersion(" + path + "," + versionName + ")");
        }
        manager.restore(path, versionName, false);
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

    @Override
    public Collection<Page> findModifiedPages(final BeanContext context, final Resource root) {
        List<Page> result = new ArrayList<>();
        findModifiedPages(context, root, result);
        return result;
    }

    protected void findModifiedPages(final BeanContext context, final Resource parent,
                                     List<Page> result) {
        final Iterable<Resource> children = parent.getChildren();
        for (Resource resource : children) {
            if (Page.isPage(resource)) {
                final Page page = pageManager.createBean(context, resource);
                if (isModified(page)) {
                    result.add(page);
                }
                findModifiedPages(context, page.getResource(), result);
            } else if (Folder.isFolder(resource)) {
                findModifiedPages(context, resource, result);
            }
        }
    }

    @Override
    public Collection<Page> findUnreleasedPages(final BeanContext context, final Resource root, final Release release)
            throws RepositoryException {
        List<Page> result = new ArrayList<>();
        findUnreleasedPages(context, root, release != null ? release : new NoRelease(context, root.getPath()), result);
        return result;
    }

    protected void findUnreleasedPages(final BeanContext context, final Resource parent, final Release release,
                                       List<Page> result)
            throws RepositoryException {
        final Iterable<Resource> children = parent.getChildren();
        for (Resource resource : children) {
            if (Page.isPage(resource)) {
                final Page page = pageManager.createBean(context, resource);
                VersionManager versionManager = getVersionManager(context);
                if (StagingUtils.isVersionable(page.getContent().getResource())) {
                    Version currentVersion = versionManager.getBaseVersion(page.getContent().getPath());
                    Calendar currentVersionCreated = currentVersion.getCreated();
                    final VersionHistory versionHistory = versionManager.getVersionHistory(page.getContent().getPath());
                    try {
                        final Version versionByLabel = versionHistory.getVersionByLabel(release.getLabel());
                        final Calendar labeledCreated = versionByLabel.getCreated();
                        if (currentVersionCreated.after(labeledCreated)) {
                            result.add(page);
                        }
                    } catch (VersionException e) {
                        // no label
                        if (!currentVersion.getName().equals("jcr:rootVersion")) {
                            result.add(page);
                        }
                    }
                }
                findUnreleasedPages(context, page.getResource(), release, result);
            } else if (Folder.isFolder(resource)) {
                findUnreleasedPages(context, resource, release, result);
            }
        }
    }

    public VersionManager getVersionManager(final BeanContext context)
            throws RepositoryException {
        SlingHttpServletRequest request = context.getRequest();
        VersionManager versionManager = (VersionManager) request.getAttribute("");
        if (versionManager == null) {
            final JackrabbitSession session = (JackrabbitSession) context.getResolver().adaptTo(Session.class);
            versionManager = session.getWorkspace().getVersionManager();
            request.setAttribute("", versionManager);
        }
        return versionManager;
    }

    public VersionHistory getVersionHistory(final BeanContext context, final String path)
            throws RepositoryException {
        return getVersionManager(context).getVersionHistory(path);
    }

    protected class NoRelease extends Release {

        NoRelease(BeanContext context, String path) {
            super(context, new NonExistingResource(context.getResolver(), path));
        }

        @Override
        public String getLabel() {
            //no existing label
            return "w81t5l6pSYAeeN5c";
        }
    }
}
