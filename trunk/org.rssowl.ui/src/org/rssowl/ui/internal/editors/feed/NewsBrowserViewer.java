/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearch;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.CBrowser;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.dialogs.SearchNewsDialog;
import org.rssowl.ui.internal.util.ModelUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * @author bpasero
 * @author Kay Patzwald <kay_patzwald@users.sourceforge.net>
 */
public class NewsBrowserViewer extends ContentViewer implements ILinkHandler {

  /* ID for Link Handlers */
  static final String AUTHOR_HANDLER_ID = "org.rssowl.ui.search.Author";
  static final String CATEGORY_HANDLER_ID = "org.rssowl.ui.search.Category";
  static final String LABEL_HANDLER_ID = "org.rssowl.ui.search.Label";

  private Object fInput;
  private CBrowser fBrowser;
  private ApplicationServer fServer;
  private String fId;
  private boolean fBlockRefresh;
  private IModelFactory fFactory;
  private IPreferenceScope fPreferences = Owl.getPreferenceService().getGlobalScope();

  /* This viewer's sorter. <code>null</code> means there is no sorter. */
  private ViewerComparator fSorter;

  /* This viewer's filters (element type: <code>ViewerFilter</code>). */
  private List<ViewerFilter> fFilters;
  private NewsFilter fNewsFilter;

  /**
   * @param parent
   * @param style
   */
  public NewsBrowserViewer(Composite parent, int style) {
    this(new CBrowser(parent, style));
  }

  /**
   * @param browser
   */
  public NewsBrowserViewer(CBrowser browser) {
    hookControl(browser.getControl());
    fBrowser = browser;
    fId = String.valueOf(hashCode());
    fServer = ApplicationServer.getDefault();
    fServer.register(fId, this);
    fFactory = Owl.getModelFactory();

    /* Register Link Handler */
    fBrowser.addLinkHandler(AUTHOR_HANDLER_ID, this);
    fBrowser.addLinkHandler(CATEGORY_HANDLER_ID, this);
    fBrowser.addLinkHandler(LABEL_HANDLER_ID, this);
  }

  void setBlockRefresh(boolean block) {
    fBlockRefresh = block;
  }

  /*
   * @see org.rssowl.ui.internal.ILinkHandler#handle(java.lang.String,
   * java.net.URI)
   */
  public void handle(String id, URI link) {

    /* Extract Query Part */
    String query = link.getQuery();
    if (!StringUtils.isSet(query))
      return;

    /* Decode Value */
    query = URIUtils.urlDecode(query).trim();

    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
    String entity = INews.class.getName();

    /* Search on Author */
    if (AUTHOR_HANDLER_ID.equals(id)) {
      ISearchField field = fFactory.createSearchField(INews.AUTHOR, entity);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, query);
      conditions.add(condition);
    }

    /* Search on Category */
    else if (CATEGORY_HANDLER_ID.equals(id)) {
      ISearchField field = fFactory.createSearchField(INews.CATEGORIES, entity);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, query);
      conditions.add(condition);
    }

    /* Search on Label */
    else if (LABEL_HANDLER_ID.equals(id)) {
      ISearchField field = fFactory.createSearchField(INews.LABEL, entity);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, query);
      conditions.add(condition);
    }

    /* Open Dialog and Search */
    if (conditions.size() >= 1 && !fBrowser.getControl().isDisposed()) {
      boolean matchAllConditions = AUTHOR_HANDLER_ID.equals(id);
      SearchNewsDialog dialog = new SearchNewsDialog(fBrowser.getControl().getShell(), conditions, matchAllConditions, true);
      dialog.open();
    }
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setContentProvider(org.eclipse.jface.viewers.IContentProvider)
   */
  @Override
  public void setContentProvider(IContentProvider contentProvider) {
    fBlockRefresh = true;
    try {
      super.setContentProvider(contentProvider);
    } finally {
      fBlockRefresh = false;
    }
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#refresh()
   */
  @Override
  public void refresh() {
    if (!fBlockRefresh)
      fBrowser.getControl().refresh();
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#handleDispose(org.eclipse.swt.events.DisposeEvent)
   */
  @Override
  protected void handleDispose(DisposeEvent event) {
    fServer.unregister(fId);
    super.handleDispose(event);
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#setInput(java.lang.Object)
   */
  @Override
  public void setInput(Object input) {
    setInput(input, false);
  }

  private void setInput(Object input, boolean force) {

    /* Ignore this Input if its already set */
    if (!force && sameInput(input))
      return;

    /* Remember Input */
    fInput = input;

    /* Stop any other Website if required */
    String url = fBrowser.getControl().getUrl();
    if (!"".equals(url)) //$NON-NLS-1$
      fBrowser.getControl().stop();

    /* Input is a URL - display it */
    if (input instanceof String) {
      fBrowser.getControl().setUrl((String) input);
      return;
    }

    /* Set URL if its not already showing and contains a display-operation */
    String inputUrl = fServer.toUrl(fId, input);
    if (fServer.isDisplayOperation(inputUrl) && !inputUrl.equals(url))
      fBrowser.setUrl(inputUrl);

    /* Hide the Browser as soon as the input is set to Null */
    if (input == null && fBrowser.getControl().getVisible())
      fBrowser.getControl().setVisible(false);
  }

  /* Checks wether the given Input is same to the existing one */
  private boolean sameInput(Object input) {
    if (fInput instanceof Object[])
      return input instanceof Object[] && Arrays.equals((Object[]) fInput, (Object[]) input);

    if (fInput != null)
      return fInput.equals(input);

    return false;
  }

  /*
   * @see org.eclipse.jface.viewers.ContentViewer#getInput()
   */
  @Override
  public Object getInput() {
    return fInput;
  }

  /**
   * Adds the given filter to this viewer, and triggers refiltering and
   * resorting of the elements.
   *
   * @param filter a viewer filter
   */
  public void addFilter(ViewerFilter filter) {
    if (fFilters == null)
      fFilters = new ArrayList<ViewerFilter>();

    fFilters.add(filter);
    if (filter instanceof NewsFilter)
      fNewsFilter = (NewsFilter) filter;

    refresh();
  }

  /**
   * Removes the given filter from this viewer, and triggers refiltering and
   * resorting of the elements if required. Has no effect if the identical
   * filter is not registered.
   *
   * @param filter a viewer filter
   */
  public void removeFilter(ViewerFilter filter) {
    Assert.isNotNull(filter);
    if (fFilters != null) {
      for (Iterator<ViewerFilter> i = fFilters.iterator(); i.hasNext();) {
        Object o = i.next();
        if (o == filter) {
          i.remove();
          refresh();
          if (fFilters.size() == 0)
            fFilters = null;

          return;
        }
      }
    }

    if (filter == fNewsFilter)
      fNewsFilter = null;
  }

  /**
   * @param comparator
   */
  public void setComparator(ViewerComparator comparator) {
    if (fSorter != comparator) {
      fSorter = comparator;
      refresh();
    }
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#getControl()
   */
  @Override
  public Control getControl() {
    return fBrowser.getControl();
  }

  /**
   * @return The wrapped Browser (CBrowser).
   */
  public CBrowser getBrowser() {
    return fBrowser;
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#getSelection()
   */
  @Override
  public ISelection getSelection() {
    /* Not Supported */
    return null;
  }

  /*
   * @see org.eclipse.jface.viewers.Viewer#setSelection(org.eclipse.jface.viewers.ISelection,
   * boolean)
   */
  @Override
  public void setSelection(ISelection selection, boolean reveal) {
  /* Not Supported */
  }

  /**
   * Shows the intial Input in the Browser.
   */
  public void home() {
    setInput(fInput, true);
  }

  private Object[] getSortedChildren(Object parent) {
    Object[] result = getFilteredChildren(parent);
    if (fSorter != null) {

      /* be sure we're not modifying the original array from the model */
      result = result.clone();
      fSorter.sort(this, result);
    }
    return result;
  }

  private Object[] getFilteredChildren(Object parent) {
    Object[] result = getRawChildren(parent);

    /* Never filter a selected News, thereby return here */
    if (fInput instanceof INews)
      return result;

    /* Run Filters over result */
    if (fFilters != null) {
      for (Object filter : fFilters) {
        ViewerFilter f = (ViewerFilter) filter;
        result = f.filter(this, parent, result);
      }
    }
    return result;
  }

  private Object[] getRawChildren(Object parent) {
    Object[] result = null;
    if (parent != null) {
      IStructuredContentProvider cp = (IStructuredContentProvider) getContentProvider();
      if (cp != null)
        result = cp.getElements(parent);
    }
    return (result != null) ? result : new Object[0];
  }

  /**
   * @param input Can either be an Array of Feeds or News
   * @return An flattend array of Objects.
   */
  public Object[] getFlattendChildren(Object input) {

    /* Using NewsContentProvider */
    if (input != null && getContentProvider() instanceof NewsContentProvider) {
      NewsContentProvider cp = (NewsContentProvider) getContentProvider();

      /*
       * Flatten Children since Grouping is Enabled and the Parent is not
       * containing just News (so either Feed or ViewerGroups).
       */
      if (cp.isGroupingEnabled() && !isNews(input)) {
        List<Object> flatList = new ArrayList<Object>();

        /* Wrap into Object-Array */
        if (!(input instanceof Object[]))
          input = new Object[] { input };

        /* For each Group retrieve Children (sorted and filtered) */
        Object groups[] = (Object[]) input;
        for (Object group : groups) {

          /* Make sure this child has children */
          if (cp.hasChildren(group)) {
            Object sortedChilds[] = getSortedChildren(group);

            /* Only add if there are Childs */
            if (sortedChilds.length > 0) {
              flatList.add(group);
              flatList.addAll(Arrays.asList(sortedChilds));
            }
          }

          /* Otherwise just add */
          else {
            flatList.add(group);
          }
        }

        return flatList.toArray();
      }

      /* Grouping is not enabled, just return sorted Children */
      return getSortedChildren(input);
    }

    /* Structured ContentProvider */
    else if (input != null && getContentProvider() instanceof IStructuredContentProvider)
      return getSortedChildren(input);

    /* No Element to show */
    return new Object[0];
  }

  /* Returns TRUE if the Input consists only of INews */
  private boolean isNews(Object input) {
    if (input instanceof Object[]) {
      Object elements[] = (Object[]) input;
      for (Object element : elements) {
        if (!(element instanceof INews))
          return false;
      }
    } else if (!(input instanceof INews))
      return false;

    return true;
  }

  /**
   * @param parentElement
   * @param childElements
   */
  public void add(Object parentElement, Object[] childElements) {
    Assert.isNotNull(parentElement);
    assertElementsNotNull(childElements);

    refresh(); // TODO Optimize
  }

  /**
   * @param parentElement
   * @param childElement
   */
  public void add(Object parentElement, Object childElement) {
    Assert.isNotNull(parentElement);
    Assert.isNotNull(childElement);

    refresh(); // TODO Optimize
  }

  /**
   * @param elements
   * @param properties
   */
  public void update(Object[] elements, @SuppressWarnings("unused") String[] properties) {
    assertElementsNotNull(elements);

    /*
     * The update-event could have been sent out a lot faster than the Browser
     * having a chance to react. In this case, rather then refreshing a possible
     * blank page (or wrong page), re-set the input.
     */
    String inputUrl = fServer.toUrl(fId, fInput);
    String browserUrl = fBrowser.getControl().getUrl();
    boolean resetInput = browserUrl.length() == 0 || URIUtils.ABOUT_BLANK.equals(browserUrl);
    if (inputUrl.equals(browserUrl))
      refresh(); // TODO Optimize
    else if (fServer.isDisplayOperation(inputUrl) && resetInput)
      fBrowser.setUrl(inputUrl);
  }

  /**
   * @param element
   * @param properties
   */
  public void update(Object element, @SuppressWarnings("unused") String[] properties) {
    Assert.isNotNull(element);

    refresh(); // TODO Optimize
  }

  /**
   * @param objects
   */
  public void remove(Object[] objects) {
    assertElementsNotNull(objects);

    refresh(); // TODO Optimize
  }

  /**
   * @param element
   */
  public void remove(Object element) {
    Assert.isNotNull(element);

    refresh(); // TODO Optimize
  }

  private void assertElementsNotNull(Object[] elements) {
    Assert.isNotNull(elements);
    for (Object element : elements) {
      Assert.isNotNull(element);
    }
  }

  /**
   * TODO This is temporary API until Bug 627 is fixed using Lucene Token
   * Positions.
   *
   * @return Returns a List of Strings that should get highlighted per News that
   * is displayed.
   */
  protected Collection<String> getHighlightedWords() {
    if (getContentProvider() instanceof NewsContentProvider && fPreferences.getBoolean(DefaultPreferences.FV_HIGHLIGHT_SEARCH_RESULTS)) {
      INewsMark mark = ((NewsContentProvider) getContentProvider()).getInput();
      Set<String> extractedWords;

      /* Extract from Conditions if any */
      if (mark instanceof ISearch) {
        List<ISearchCondition> conditions = ((ISearch) mark).getSearchConditions();
        extractedWords = ModelUtils.extractWords(conditions, false, true);
      } else
        extractedWords = new HashSet<String>(1);

      /* Fill Pattern if set */
      if (fNewsFilter != null && StringUtils.isSet(fNewsFilter.getPatternString())) {
        StringTokenizer tokenizer = new StringTokenizer(fNewsFilter.getPatternString());
        while (tokenizer.hasMoreElements())
          extractedWords.add(tokenizer.nextToken());
      }

      return extractedWords;
    }

    return Collections.emptyList();
  }
}