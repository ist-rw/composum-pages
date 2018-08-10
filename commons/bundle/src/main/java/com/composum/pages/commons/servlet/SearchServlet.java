package com.composum.pages.commons.servlet;

import com.composum.pages.commons.PagesConfiguration;
import com.composum.pages.commons.service.SearchService;
import com.composum.pages.commons.service.search.SearchTermParseException;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * this servlet is searching content and answering in various formats depending on selector keys additional
 * to the operation selectors, e.g. '/bin/cpm/pages/search.page.tile.html' is rendering an HTML list with
 * found pages rendered as tiles ('edit/tile' subtype; see PageTileSearchResult service) with a link around
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Pages Search Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/pages/search",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET
        })
public class SearchServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(SearchServlet.class);

    public static final String PARAM_FILTER = "filter";
    public static final String DEFAULT_FILTER = "page";

    //
    // Servlet operations
    //

    public enum Extension {
        html, json
    }

    public enum Operation {
        page, element, asset, component
    }

    protected BundleContext bundleContext;

    @Reference
    protected PagesConfiguration pagesConfiguration;

    @Reference
    protected SearchService searchService;

    protected Map<String, SearchResultRenderer> searchResultRenderers =
            Collections.synchronizedMap(new HashMap<String, SearchResultRenderer>());

    protected PagesSearchOperationSet operations = new PagesSearchOperationSet();

    @Override
    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    // operation instances

    protected SearchPageOperation searchPageOperation = new SearchPageOperation();
    protected SearchPageElementOperation searchPageElementOperation = new SearchPageElementOperation();

    /** setup of the servlet operation set for this servlet instance */
    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.page, searchPageOperation);
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html,
                Operation.page, searchPageOperation);
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json,
                Operation.element, searchPageElementOperation);
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html,
                Operation.element, searchPageElementOperation);
    }

    public class PagesSearchOperationSet extends ServletOperationSet<Extension, Operation> {

        public PagesSearchOperationSet() {
            super(Extension.json);
        }
    }

    protected abstract class SearchOperation implements ServletOperation {

        protected SearchResultRenderer getRenderer(SlingHttpServletRequest request) {
            RequestPathInfo pathInfo = request.getRequestPathInfo();
            List<String> selectors = new ArrayList<>(Arrays.asList(pathInfo.getSelectors()));
            SearchResultRenderer renderer = null;
            while (renderer == null && selectors.size() > 0) {
                renderer = searchResultRenderers.get(StringUtils.join(selectors, ".") + "." + pathInfo.getExtension());
                selectors.remove(selectors.size() - 1);
            }
            return renderer;
        }

        protected void renderResult(SearchRequest searchRequest, List<SearchService.Result> result)
                throws IOException {
            SearchResultRenderer renderer = getRenderer(searchRequest.context.getRequest());
            if (renderer != null) {
                renderer.sendResult(searchRequest, result);
            }
        }

        protected void badRequest(SlingHttpServletRequest request, SlingHttpServletResponse response,
                                  Exception exception) throws IOException {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getLocalizedMessage());
        }
    }

    protected class SearchPageOperation extends SearchOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws IOException {

            try {
                BeanContext context = new BeanContext.Servlet(getServletContext(), bundleContext, request, response);
                SearchRequest searchRequest = new SearchRequest(context, resource.getPath());

                if (searchRequest.isValid()) {
                    List<SearchService.Result> result = searchService.searchPages(context,
                            searchRequest.rootPath, searchRequest.term, searchRequest.offset, searchRequest.limit);
                    renderResult(searchRequest, result);
                }

            } catch (SearchTermParseException | RepositoryException ex) {
                badRequest(request, response, ex);
            }
        }
    }

    protected class SearchPageElementOperation extends SearchOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource)
                throws IOException {

            try {
                BeanContext context = new BeanContext.Servlet(getServletContext(), bundleContext, request, response);
                SearchRequest searchRequest = new SearchRequest(context, resource.getPath());
                ResourceFilter filter = pagesConfiguration.getRequestNodeFilter(request, PARAM_FILTER, DEFAULT_FILTER);

                if (searchRequest.isValid()) {
                    List<SearchService.Result> result = searchService.search(context,
                            searchRequest.rootPath, searchRequest.term, filter, searchRequest.offset, searchRequest.limit);
                    renderResult(searchRequest, result);
                }

            } catch (SearchTermParseException | RepositoryException ex) {
                badRequest(request, response, ex);
            }
        }
    }

    // search result renderer registry

    @Reference(
            service = SearchResultRenderer.class,
            policy = ReferencePolicy.DYNAMIC,
            cardinality = ReferenceCardinality.MULTIPLE
    )
    protected void addSearchResultRenderer(@Nonnull final SearchResultRenderer renderer) {
        LOG.info("addSearchResultRenderer: {}", renderer.getClass().getSimpleName());
        searchResultRenderers.put(renderer.rendererKey(), renderer);
    }

    protected void removeSearchResultRenderer(@Nonnull final SearchResultRenderer renderer) {
        LOG.info("removeSearchResultRenderer: {}", renderer.getClass().getSimpleName());
        searchResultRenderers.remove(renderer.rendererKey());
    }

    @Activate
    private void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
