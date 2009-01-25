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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.event.SearchMarkAdapter;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.EntityGroupItem;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.UIBackgroundJob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class NewsContentProvider implements ITreeContentProvider {
  private final NewsBrowserViewer fBrowserViewer;
  private final NewsTableViewer fTableViewer;
  private final NewsGrouping fGrouping;
  private NewsListener fNewsListener;
  private SearchMarkAdapter fSearchMarkListener;
  private INewsMark fInput;
  private final FeedView fFeedView;
  private boolean fDisposed;

  /* Cache displayed News */
  private Set<INews> fCachedNews;

  /**
   * @param tableViewer
   * @param browserViewer
   * @param feedView
   */
  public NewsContentProvider(NewsTableViewer tableViewer, NewsBrowserViewer browserViewer, FeedView feedView) {
    fTableViewer = tableViewer;
    fBrowserViewer = browserViewer;
    fFeedView = feedView;
    fGrouping = feedView.getGrouper();
  }

  /*
   * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {
    List<Object> elements = new ArrayList<Object>();

    /* Wrap into Object Array */
    if (!(inputElement instanceof Object[]))
      inputElement = new Object[] { inputElement };

    /* Foreach Object */
    Object[] objects = (Object[]) inputElement;
    for (Object object : objects) {

      /* This is a News */
      if (object instanceof INews && ((INews) object).isVisible()) {
        elements.add(object);
      }

      /* This is a NewsReference */
      else if (object instanceof NewsReference) {
        NewsReference newsRef = (NewsReference) object;
        INews news = obtainFromCache(newsRef);
        if (news != null)
          elements.add(news);
      }

      /* This is a FeedReference */
      else if (object instanceof FeedLinkReference) {
        synchronized (NewsContentProvider.this) {
          Collection<INews> news = fCachedNews;
          if (news != null) {
            if (fGrouping.getType() == NewsGrouping.Type.NO_GROUPING)
              elements.addAll(news);
            else
              elements.addAll(fGrouping.group(news));
          }
        }
      }

      /* This is a class that implements IMark */
      else if (object instanceof ModelReference) {
        Class<? extends IEntity> entityClass = ((ModelReference) object).getEntityClass();
        if (IMark.class.isAssignableFrom(entityClass)) {
          synchronized (NewsContentProvider.this) {
            Collection<INews> news = fCachedNews;
            if (news != null) {
              if (fGrouping.getType() == NewsGrouping.Type.NO_GROUPING)
                elements.addAll(news);
              else
                elements.addAll(fGrouping.group(news));
            }
          }
        }
      }

      /* This is a EntityGroup */
      else if (object instanceof EntityGroup) {
        EntityGroup group = (EntityGroup) object;

        List<EntityGroupItem> items = group.getItems();
        for (EntityGroupItem item : items) {
          if (((INews) item.getEntity()).isVisible())
            elements.add(item.getEntity());
        }
      }
    }

    return elements.toArray();
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
   */
  public Object[] getChildren(Object parentElement) {
    List<Object> children = new ArrayList<Object>();

    /* Handle EntityGroup */
    if (parentElement instanceof EntityGroup) {
      List<EntityGroupItem> items = ((EntityGroup) parentElement).getItems();
      for (EntityGroupItem item : items)
        children.add(item.getEntity());
    }

    return children.toArray();
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
   */
  public Object getParent(Object element) {

    /* Handle Grouping specially */
    if (fGrouping.isActive() && element instanceof INews) {
      List<EntityGroup> groups = fGrouping.group(Collections.singletonList((INews) element));
      if (groups.size() == 1)
        return groups.get(0);
    }

    return null;
  }

  /*
   * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
   */
  public boolean hasChildren(Object element) {
    return element instanceof EntityGroup;
  }

  /*
   * @see org.eclipse.jface.viewers.IContentProvider#dispose()
   */
  public synchronized void dispose() {
    fDisposed = true;
    unregisterListeners();
  }

  /*
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
   * java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  /* Ignore - Input changes are handled via refreshCache(Object input) */
  }

  boolean isGroupingEnabled() {
    return fGrouping.getType() != NewsGrouping.Type.NO_GROUPING;
  }

  /* Returns the news that have been added since the last refresh */
  synchronized List<INews> refreshCache(INewsMark input, boolean onlyAdd) throws PersistenceException {
    List<INews> addedNews = Collections.emptyList();

    /* Update Input */
    fInput = input;

    /* Register Listeners if not yet done */
    if (fNewsListener == null)
      registerListeners();

    /* Clear old Data if required */
    if (fCachedNews == null)
      fCachedNews = new HashSet<INews>();
    else if (!onlyAdd)
      fCachedNews.clear();

    /* Check if ContentProvider was already disposed */
    if (fDisposed)
      return addedNews;

    /* Obtain the News */
    addedNews = new ArrayList<INews>();

    /* Special-case marks that can retrieve newsRefs cheaply */
    if (input.isGetNewsRefsEfficient()) {
      for (NewsReference newsRef : input.getNewsRefs(INews.State.getVisible())) {

        /* Avoid to resolve an already shown News */
        if (onlyAdd && hasCachedNews(newsRef))
          continue;

        /* Resolve and Add News */
        INews resolvedNews = newsRef.resolve();
        if (resolvedNews != null) //TODO Remove once Bug 173 is fixed
          addedNews.add(resolvedNews);
      }
    }

    else
      addedNews.addAll(input.getNews(INews.State.getVisible()));

    /* Add into Cache */
    fCachedNews.addAll(addedNews);

    return addedNews;
  }

  synchronized INewsMark getInput() {
    return fInput;
  }

  synchronized Set<INews> getCachedNews() {
    return fCachedNews;
  }

  synchronized boolean hasCachedNews() {
    return fCachedNews != null && !fCachedNews.isEmpty();
  }

  private synchronized boolean hasCachedNews(NewsReference ref) {
    if (fCachedNews == null)
      return false;

    for (INews news : fCachedNews) {
      if (ref.references(news))
        return true;
    }

    return false;
  }

  private synchronized boolean hasCachedNews(INews news) {
    if (fCachedNews == null)
      return false;

    for (INews cachedNews : fCachedNews) {
      if (cachedNews.equals(news))
        return true;
    }

    return false;
  }

  private synchronized INews obtainFromCache(NewsReference ref) {
    for (INews cachedNews : fCachedNews) {
      if (ref.references(cachedNews))
        return cachedNews;
    }

    return null;
  }

  private void registerListeners() {

    /* Saved Search Listener */
    fSearchMarkListener = new SearchMarkAdapter() {
      @Override
      public void resultsChanged(final Set<SearchMarkEvent> events) {
        for (SearchMarkEvent event : events) {
          ISearchMark searchMark = event.getEntity();

          if (fInput.equals(searchMark)) {
            JobRunner.runUIUpdater(new UIBackgroundJob(fFeedView.getEditorControl()) {
              private List<INews> fAddedNews;

              @Override
              protected void runInBackground(IProgressMonitor monitor) {
                fAddedNews = refreshCache(fInput, true);
              }

              @Override
              protected void runInUI(IProgressMonitor monitor) {

                /* Event not interesting for us or we are disposed */
                if (fAddedNews == null || fAddedNews.size() == 0)
                  return;

                /* Refresh Viewer to reflect changes */
                fFeedView.refresh(true, true); //TODO Seems some JFace Caching Problem here

                /* Add to Browser-Viewer if showing entire Feed */
                if (fBrowserViewer.getInput() instanceof BookMarkReference)
                  fBrowserViewer.add(fBrowserViewer.getInput(), fAddedNews.toArray());
              }
            });

            /* Done */
            return;
          }
        }
      }
    };

    DynamicDAO.addEntityListener(ISearchMark.class, fSearchMarkListener);

    /* News Listener */
    fNewsListener = new NewsAdapter() {

      /* News got Added */
      @Override
      public void entitiesAdded(final Set<NewsEvent> events) {
        JobRunner.runInUIThread(fFeedView.getEditorControl(), new Runnable() {
          public void run() {
            Set<NewsEvent> addedNews = null;

            /* Filter News which are from a different Feed than displayed */
            for (NewsEvent event : events) {
              if (isInputRelatedTo(event.getEntity(), EventType.PERSIST)) {
                if (addedNews == null)
                  addedNews = new HashSet<NewsEvent>();

                addedNews.add(event);
              }
            }

            /* Event not interesting for us or we are disposed */
            if (addedNews == null || addedNews.size() == 0)
              return;

            /* Handle */
            boolean refresh = handleAddedNews(addedNews);
            if (refresh)
              fFeedView.refresh(true, false);
          }
        });
      }

      /* News got Updated */
      @Override
      public void entitiesUpdated(final Set<NewsEvent> events) {
        JobRunner.runInUIThread(fFeedView.getEditorControl(), new Runnable() {
          public void run() {
            Set<NewsEvent> restoredNews = null;
            Set<NewsEvent> updatedNews = null;
            Set<NewsEvent> deletedNews = null;

            /* Filter News which are from a different Feed than displayed */
            for (NewsEvent event : events) {
              if (isInputRelatedTo(event.getEntity(), EventType.UPDATE)) {
                INews news = event.getEntity();
                INews.State oldState = event.getOldNews().getState();

                /* News got Deleted */
                if (!news.isVisible()) {
                  if (deletedNews == null)
                    deletedNews = new HashSet<NewsEvent>();

                  deletedNews.add(event);
                }

                /* News got Restored */
                else if (news.isVisible() && (oldState == INews.State.HIDDEN || oldState == INews.State.DELETED)) {
                  if (restoredNews == null)
                    restoredNews = new HashSet<NewsEvent>();

                  restoredNews.add(event);
                }

                /* News got Updated */
                else {
                  if (updatedNews == null)
                    updatedNews = new HashSet<NewsEvent>();

                  updatedNews.add(event);
                }
              }
            }

            boolean refresh = false;
            boolean updateSelectionFromDelete = false;

            /* Handle Restored News */
            if (restoredNews != null && !restoredNews.isEmpty())
              refresh = handleAddedNews(restoredNews);

            /* Handle Updated News */
            if (updatedNews != null && !updatedNews.isEmpty())
              refresh = handleUpdatedNews(updatedNews);

            /* Handle Deleted News */
            if (deletedNews != null && !deletedNews.isEmpty()) {
              refresh = handleDeletedNews(deletedNews);
              updateSelectionFromDelete = refresh;
            }

            /* Refresh and update selection due to deletion */
            if (updateSelectionFromDelete) {
              fTableViewer.updateSelectionAfterDelete(new Runnable() {
                public void run() {
                  fFeedView.refresh(false, false);
                }
              });
            }

            /* Normal refresh w/o deletion */
            else if (refresh)
              fFeedView.refresh(false, false);
          }
        });
      }

      /* News got Deleted */
      @Override
      public void entitiesDeleted(final Set<NewsEvent> events) {
        JobRunner.runInUIThread(fFeedView.getEditorControl(), new Runnable() {
          public void run() {
            Set<NewsEvent> deletedNews = null;

            /* Filter News which are from a different Feed than displayed */
            for (NewsEvent event : events) {
              INews news = event.getEntity();
              if ((news.isVisible() || news.getParentId() != 0) && isInputRelatedTo(news, EventType.REMOVE)) {
                if (deletedNews == null)
                  deletedNews = new HashSet<NewsEvent>();

                deletedNews.add(event);
              }
            }

            /* Event not interesting for us or we are disposed */
            if (deletedNews == null || deletedNews.size() == 0)
              return;

            /* Handle Deleted News */
            boolean refresh = handleDeletedNews(deletedNews);
            if (refresh)
              fFeedView.refresh(false, false);
          }
        });
      }
    };

    DynamicDAO.addEntityListener(INews.class, fNewsListener);
  }

  private boolean handleAddedNews(Set<NewsEvent> events) {

    /* Receive added News */
    List<INews> addedNews = new ArrayList<INews>(events.size());
    for (NewsEvent event : events) {
      addedNews.add(event.getEntity());
    }

    /* Add to Cache */
    synchronized (NewsContentProvider.this) {
      fCachedNews.addAll(addedNews);
    }

    /* Return early if a refresh is required anyways */
    if (fGrouping.needsRefresh(INews.class, events, false))
      return true;

    /* Add to Table-Viewer if Visible */
    if (fFeedView.isTableViewerVisible())
      fTableViewer.add(fTableViewer.getInput(), addedNews.toArray());

    /* Add to Browser-Viewer if showing entire Feed */
    if (fBrowserViewer.getInput() instanceof BookMarkReference)
      fBrowserViewer.add(fBrowserViewer.getInput(), addedNews.toArray());

    return false;
  }

  private boolean handleUpdatedNews(Set<NewsEvent> events) {

    /* Receive updated News */
    List<INews> updatedNews = new ArrayList<INews>(events.size());
    for (NewsEvent event : events) {
      updatedNews.add(event.getEntity());
    }

    /* Return early if refresh is required anyways */
    if (fGrouping.needsRefresh(INews.class, events, true))
      return true;

    /* Update in Table-Viewer */
    if (fFeedView.isTableViewerVisible())
      fTableViewer.update(updatedNews.toArray(), null);

    /* Update in Browser-Viewer */
    if (contains(fBrowserViewer.getInput(), updatedNews))
      fBrowserViewer.update(updatedNews.toArray(), null);

    return false;
  }

  private boolean handleDeletedNews(Set<NewsEvent> events) {

    /* Receive deleted News */
    List<INews> deletedNews = new ArrayList<INews>(events.size());
    for (NewsEvent event : events) {
      deletedNews.add(event.getEntity());
    }

    /* Remove from Cache */
    synchronized (NewsContentProvider.this) {
      fCachedNews.removeAll(deletedNews);
    }

    /* Return early if refresh is required anyways */
    if (fGrouping.needsRefresh(INews.class, events, false))
      return true;

    /* Grouping Disabled */
    if (!isGroupingEnabled()) {

      /* Remove from Table-Viewer */
      if (fFeedView.isTableViewerVisible())
        fTableViewer.remove(deletedNews.toArray());

      /* Remove from Browser-Viewer */
      if (contains(fBrowserViewer.getInput(), deletedNews))
        fBrowserViewer.remove(deletedNews.toArray());
    }

    return false;
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(INews.class, fNewsListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
  }

  private boolean isInputRelatedTo(INews news, EventType type) {

    /* Check if BookMark references the News' Feed and is not a copy */
    if (fInput instanceof IBookMark) {
      IBookMark bookmark = (IBookMark) fInput;
      if (news.getParentId() == 0 && bookmark.getFeedLinkReference().equals(news.getFeedReference()))
        return true;
    }

    /* Check if Saved Search contains the given News */
    else if (type != EventType.PERSIST && fInput instanceof ISearchMark) {

      /*
       * Workaround a race condition in a safe way: When a News gets updated or deleted from a
       * Searchmark, the Indexer is the first to process this event. Since the SavedSearchService
       * updates all Searchmarks instantly as a result of that, the Searchmark at this point could no
       * longer contain the affected News and isInputRelated() would return false. The fix is
       * to check the cache for the News instead of the potential modified Searchmark.
       */
      return hasCachedNews(news);
    }

    /* Update / Remove: Check if News points to this Bin */
    else if (fInput instanceof INewsBin) {
      return news.getParentId() == fInput.getId();
    }

    /* In Memory Folder News Mark (aggregated news) */
    else if (fInput instanceof FolderNewsMark) {

      /* Ignore copied News */
      if (news.getParentId() != 0)
        return false;

      /* News Added/Updated: Check if its part of the Folder */
      if (type == EventType.PERSIST || type == EventType.UPDATE) {
        return ((FolderNewsMark) fInput).isRelatedTo(news);
      }

      /* Remove: Check if news was cached */
      return hasCachedNews(news);
    }

    return false;
  }

  private boolean contains(Object input, List<INews> list) {

    /* Can only belong to this Feed since filtered before already */
    if (input instanceof BookMarkReference)
      return true;

    /* TODO Handle searchmarks properly */
    else if (input instanceof SearchMarkReference)
      return true;

    else if (input instanceof INews)
      return list.contains(input);

    else if (input instanceof EntityGroup) {
      List<EntityGroupItem> items = ((EntityGroup) input).getItems();
      for (EntityGroupItem item : items) {
        if (list.contains(item.getEntity()))
          return true;
      }
    }

    else if (input instanceof Object[]) {
      Object inputNews[] = (Object[]) input;
      for (Object inputNewsItem : inputNews) {
        if (list.contains(inputNewsItem))
          return true;
      }
    }

    return false;
  }
}