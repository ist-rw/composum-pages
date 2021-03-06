/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
package com.composum.pages.components.model.search;

import com.composum.pages.commons.model.Element;
import com.composum.pages.commons.model.Page;
import com.composum.pages.commons.model.Site;
import com.composum.platform.commons.content.service.PlaceholderService;
import com.composum.sling.core.BeanContext;
import com.composum.sling.platform.staging.search.SearchService;
import com.composum.sling.platform.staging.search.SearchTermParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.composum.pages.commons.service.search.PageSearchPlugin.SELECTOR_PAGE;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Models the possible configurations for the presentation of a search result. <p>Properties selector (string);
 * optional, default: 'searchItem' template (resourceType:renderer); optional.
 * <p>
 * Caution: this uses the search algorithm {@link SearchService#search}
 * for which there has to be aggregation configured in the lucene search (see the documentation of that method).
 */
public class SearchResult extends Element {

    /**
     * The request attribute where the {@link SearchService.Result} is saved during rendering.
     */
    public static final String REQUEST_ATTRIBUTE_SEARCHRESULT = "searchresult";
    /**
     * Mandatory parameter for the fulltext search expression to search for.
     */
    public static final String PARAMETER_TERM = "search.term";
    /**
     * Optional parameter that determines the offset of the first shown search result (in items, not in pages).
     */
    public static final String PARAMETER_OFFSET = "search.offset";
    /**
     * Optional parameter that determines the number of searchresults to be shown in the page.
     */
    public static final String PARAMETER_PAGESIZE = "search.pagesize";
    /**
     * Property name for {@link #getHeadline()}.
     */
    public static final String PROP_HEADLINE = "headline";
    /**
     * Property name for {@link #getSearchRoot()}.
     */
    public static final String PROP_SEARCH_ROOT = "searchRoot";
    /**
     * Property name for {@link #getSelector()}.
     */
    public static final String PROP_SELECTOR = "selector";
    /**
     * Property name for {@link #getTemplate()}.
     */
    public static final String PROP_TEMPLATE = "template";
    /**
     * Property for the default number of items per page.
     */
    public static final int DEFAULT_PAGESIZE = 25;
    /**
     * Property for the number of items per page, if not overridden by {PROP_PAGESIZE}.
     */
    public static final String PROP_PAGESIZE = "pagesize";
    /**
     * Property name for {@link #getSearchtermErrorText()}.
     */
    public static final String PROP_SEARCHTERM_ERROR_TEXT = "searchtermErrorText";
    private static final Logger LOG = getLogger(SearchResult.class);
    /**
     * @see #getSelector()
     */
    private transient String selector;
    /**
     * @see #getTemplate()
     */
    private transient String template;
    /**
     * The search results.
     */
    private List<SearchService.Result> results;
    /**
     * @see #getSearchRoot()
     */
    private transient String searchRoot;
    /**
     * @see #getSearchTerm()
     */
    private transient String searchTerm;
    /**
     * @see #getHeadline()
     */
    private transient String head;
    /**
     * @see #getOffset()
     */
    private transient Integer offset;
    /**
     * @see #getPageSize()
     */
    private transient Integer pageSize;
    /**
     * Number of the current page, starting with 1.
     */
    private transient Integer currentSearchPage;
    /**
     * True iff there are more pages than the currentPage.
     */
    private transient boolean hasMoreSearchPages = false;
    /**
     * @see #getSearchPages()
     */
    private transient List<SearchPage> searchPages;
    /**
     * @see #isHasError()
     */
    private transient Boolean hasError;
    /**
     * @see #getSearchtermErrorText()
     */
    private transient String searchtermErrorText;

    /**
     * Sling selector which renders a found resource as search result. Optional, default "searchItem".
     */
    public String getSelector() {
        if (selector == null) {
            selector = getInherited(PROP_SELECTOR, "searchItem");
        }
        return selector;
    }

    /**
     * Template for printing the search results, for example composum/pages/components/search/defaulttemplate.
     * resourceType:renderer, Optional.
     */
    public String getTemplate() {
        if (template == null) {
            template = getInherited(PROP_TEMPLATE, "");
        }
        return template;
    }

    protected String applyPlaceholders(String text) {
        BeanContext context = getContext();
        Map<String, Object> values = Collections.<String, Object>singletonMap("term", getSearchTerm());
        return context.getService(PlaceholderService.class).applyPlaceholders(context, text, values);
    }

    /**
     * The head for the search result (HTML); is applying placeholder ${term} to embed the search term.
     */
    @Override
    @Nonnull
    public String getTitle() {
        if (title == null) {
            title = applyPlaceholders(super.getTitle());
        }
        return title;
    }

    /**
     * The head for the search result (HTML); is applying placeholder ${term} to embed the search term.
     */
    public String getHeadline() {
        if (head == null) {
            head = applyPlaceholders(getProperty(PROP_HEADLINE, ""));
        }
        return head;
    }

    /**
     * The absolute path that serves as root for the search {@link #PROP_SEARCH_ROOT}. Default: site-root.
     */
    public String getSearchRoot() {
        if (searchRoot == null) {
            searchRoot = getInherited(PROP_SEARCH_ROOT, "").trim();
            if (StringUtils.isBlank(searchRoot) || !searchRoot.startsWith("/")) {
                Page page = getCurrentPage();
                if (page != null) {
                    Site site = page.getSite();
                    if (site != null) {
                        searchRoot = StringUtils.isBlank(searchRoot) ? site.getPath() : searchRoot + "/" + site.getPath();
                    }
                }
            }
        }
        return searchRoot;
    }

    /**
     * The fulltext search expression, from the request parameter {@link #PARAMETER_TERM}.
     */
    public String getSearchTerm() {
        if (searchTerm == null) {
            searchTerm = getContext().getRequest().getParameter(PARAMETER_TERM);
            if (searchTerm == null) {
                searchTerm = "";
            }
        }
        return searchTerm;
    }

    /**
     * Offset of the shown results.
     */
    public int getOffset() {
        if (offset == null) {
            String offsetString = context.getRequest().getParameter(PARAMETER_OFFSET);
            if (isNotBlank(offsetString)) {
                offset = Integer.valueOf(offsetString);
            } else offset = 0;
            if (getResults().isEmpty()) offset = 0;
        }
        return offset;
    }

    /**
     * Page size for the shown results.
     */
    public int getPageSize() {
        if (pageSize == null) {
            String parameterString = context.getRequest().getParameter(PARAMETER_PAGESIZE);
            if (isNotBlank(parameterString)) {
                pageSize = Integer.valueOf(parameterString);
            } else {
                pageSize = getInherited(PROP_PAGESIZE, DEFAULT_PAGESIZE);
            }
        }
        return pageSize;
    }

    /**
     * A text to show when the user inputs faulty search terms. Should describe the syntax: a series of words not
     * containing ", possibly starting with - as negated search term, or sequences of words quoted in ", possibly
     * starting with - before the " for negation.
     */
    public String getSearchtermErrorText() {
        if (searchtermErrorText == null) {
            searchtermErrorText = getProperty(PROP_SEARCHTERM_ERROR_TEXT, "");
        }
        return searchtermErrorText;
    }

    /**
     * Returns true if there is an error when searching for the results - should be because of errors in the search
     * terms.
     */
    public boolean isHasError() {
        if (hasError == null) {
            getResults(); // has sideeffect to set hasError
        }
        return hasError;
    }


    /**
     * Returns or fetches the search results. Not null. If there are errors, we set {@link #isHasError()}.
     */
    @Nonnull
    public List<SearchService.Result> getResults() {
        try {
            if (results == null) {
                if (isNotBlank(getSearchTerm())) {
                    SearchService searchService = context.getService(SearchService.class);
                    // easiest way to tell whether there are more pages is to fetch one more result than needed
                    results = searchService.search(context, SELECTOR_PAGE, getSearchRoot(),
                            filterTerm(getSearchTerm()),
                            null, getOffset(), getPageSize() + 1);
                    if (results.size() > getPageSize()) {
                        hasMoreSearchPages = true;
                        results = results.subList(0, getPageSize());
                    }
                } else results = Collections.emptyList();
            }
            hasError = false;
        } catch (RepositoryException | SearchTermParseException e) {
            hasError = true;
            LOG.error("Error when searching for " + getSearchTerm(), e);
            results = Collections.emptyList();
        }
        return results;
    }

    public String filterTerm(String term) {
        return term.replaceAll("[/:;#+*&%?$§\"<>~]", "");
    }

    /**
     * Returns the number of the currently displayed search page (starting with 1).
     */
    public int getCurrentPageNumber() {
        if (null == currentSearchPage) {
            currentSearchPage = getOffset() / getPageSize() + 1;
        }
        return currentSearchPage;
    }

    /**
     * The list of Page for the paging selector. Caution: if the {@link #getOffset()} isn't a multiple of {@link
     * #getPageSize()}, the number of pages is undefined.
     */
    public List<SearchPage> getSearchPages() {
        if (searchPages == null) {
            getResults(); // initialize hasMorePages
            searchPages = new ArrayList<>();
            int numberOfPagesToDisplay = getCurrentPageNumber() + (hasMoreSearchPages ? 1 : 0);
            for (int i = 1; i <= numberOfPagesToDisplay; i++) {
                searchPages.add(new SearchPage(i));
            }
        }
        return searchPages;
    }

    public SearchPage getPreviousSearchPage() {
        return 1 < getCurrentPageNumber() ? getSearchPages().get(getCurrentPageNumber() - 2) : null;
    }

    public SearchPage getNextSearchPage() {
        return getSearchPages().get(getSearchPages().size() - 1);
    }

    /**
     * Data for a page in the paging selector.
     */
    public class SearchPage {
        /**
         * @see #getNumber()
         */
        private final int number;

        protected SearchPage(int number) {
            this.number = number;
        }

        /**
         * Number of the page, starting with 1.
         */
        public int getNumber() {
            return number;
        }

        /**
         * Whether this is the first page (1).
         */
        public boolean isFirst() {
            return 1 == number;
        }

        /**
         * Whether this is the last page to display in the selector.
         */
        public boolean isLast() {
            return hasMoreSearchPages ? number == getCurrentPageNumber() + 1 : number == getCurrentPageNumber();
        }

        /**
         * Whether this is the current page, for which the {@link #getResults()} are presented.
         */
        public boolean isActive() {
            return number == currentSearchPage;
        }

        /**
         * A link to display this page.
         */
        public URI getLink() throws URISyntaxException {
            if (number == currentSearchPage) return null;
            SlingHttpServletRequest request = getContext().getRequest();
            URIBuilder builder = new URIBuilder(request.getRequestURI());
            for (Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
                for (String value : parameter.getValue()) {
                    builder.addParameter(parameter.getKey(), value);
                }
            }
            builder.setParameter(PARAMETER_OFFSET, String.valueOf(getPageSize() * (number - 1)));
            return builder.build();
        }
    }
}
