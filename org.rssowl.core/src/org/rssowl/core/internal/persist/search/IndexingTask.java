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

package org.rssowl.core.internal.persist.search;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.ITask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * TODO Make this generic instead of being for news events only
 * </p>
 *
 * @author ijuma
 * @author bpasero
 */
public final class IndexingTask implements ITask {
  private final Indexer fIndexer;
  private final EventType fTaskType;
  private final List<INews> fNews;
  private final Collection<NewsReference> fNewsRefs;

  IndexingTask(Indexer indexer, Set<NewsEvent> events, EventType taskType) {
    this(indexer, getNews(events), taskType);
  }

  private static List<INews> getNews(Set<NewsEvent> events) {
    List<INews> news = new ArrayList<INews>(events.size());
    for (NewsEvent event : events)
      news.add(event.getEntity());

    return news;
  }

  IndexingTask(Indexer indexer, Collection<INews> news, EventType taskType) {
    fIndexer = indexer;

    if (news instanceof List)
      fNews = (List<INews>) news;
    else
      fNews = new ArrayList<INews>(news);

    fNewsRefs = null;

    fTaskType = taskType;
  }

  IndexingTask(Indexer indexer, EventType taskType, Collection<NewsReference> newsRefs) {
    fIndexer = indexer;

    fNewsRefs = newsRefs;
    fNews = null;

    fTaskType = taskType;
  }

  /*
   * @see org.rssowl.core.util.ITask#getName()
   */
  public final String getName() {
    return "Indexing Feed";
  }

  /*
   * @see org.rssowl.core.util.ITask#getPriority()
   */
  public final Priority getPriority() {
    return Priority.DEFAULT;
  }

  /*
   * @see org.rssowl.core.util.ITask#run(org.eclipse.core.runtime.IProgressMonitor)
   */
  public final IStatus run(IProgressMonitor monitor) {
    switch (fTaskType) {

      /* Add the Entities of the Events to the Index */
      case PERSIST:
        addToIndex();
        break;

      /* Update the Entities of the Events in the Index */
      case UPDATE:
        updateIndex();
        break;

      /* Delete the Entities of the Events from the Index */
      case REMOVE:
        deleteFromIndex();
        break;
    }

    return Status.OK_STATUS;
  }

  private List<INews> getNewsFromRefs(Collection<NewsReference> newsRefs) {
    List<INews> newsList = new ArrayList<INews>(newsRefs.size());
    for (NewsReference newsRef : newsRefs) {
      INews news = InternalOwl.getDefault().getPersistenceService().getDAOService().getNewsDAO().load(newsRef.getId());
      if (news != null)
        newsList.add(news);
    }
    return newsList;
  }

  private List<NewsReference> getRefsFromNews(List<INews> newsList) {
    List<NewsReference> newsRefs = new ArrayList<NewsReference>(newsList.size());
    for (INews news : newsList)
      newsRefs.add(new NewsReference(news.getId()));

    return newsRefs;
  }

  private void addToIndex() {
    if (fNews == null) {
      fIndexer.index(getNewsFromRefs(fNewsRefs), false);
      return;
    }
    fIndexer.index(fNews, false);
  }

  private void updateIndex() {
    if (fNews == null) {
      fIndexer.index(getNewsFromRefs(fNewsRefs), true);
      return;
    }
    fIndexer.index(fNews, true);
  }

  private void deleteFromIndex() {
    try {
      if (fNews == null) {
        fIndexer.removeFromIndex(fNewsRefs);
        return;
      }
      fIndexer.removeFromIndex(getRefsFromNews(fNews));
    } catch (IOException e) {
      Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }
}