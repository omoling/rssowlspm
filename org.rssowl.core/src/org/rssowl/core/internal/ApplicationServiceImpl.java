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

package org.rssowl.core.internal;

import org.rssowl.core.IApplicationService;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.MergeResult;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.SortedLongArrayList;
import org.rssowl.core.internal.persist.search.ModelSearchImpl;
import org.rssowl.core.internal.persist.service.DB4OIDGenerator;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.internal.persist.service.DBManager;
import org.rssowl.core.internal.persist.service.DatabaseEvent;
import org.rssowl.core.internal.persist.service.DatabaseListener;
import org.rssowl.core.internal.persist.service.EventManager;
import org.rssowl.core.internal.persist.service.EventsMap;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.runnable.NewsEventRunnable;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IDGenerator;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.RetentionStrategy;

import com.db4o.ObjectContainer;
import com.db4o.ext.Db4oException;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * db4o implementation of IApplicationService
 */
public class ApplicationServiceImpl implements IApplicationService {
  private volatile ObjectContainer fDb;
  private volatile ReadWriteLock fLock;
  private volatile Lock fWriteLock;

  /**
   * Creates an instance of this class.
   */
  public ApplicationServiceImpl() {
    DBManager.getDefault().addEntityStoreListener(new DatabaseListener() {
      public void databaseOpened(DatabaseEvent event) {
        fDb = event.getObjectContainer();
        fLock = event.getLock();
        fWriteLock = fLock.writeLock();
      }

      public void databaseClosed(DatabaseEvent event) {
        fDb = null;
      }
    });
  }

  /*
   * @see org.rssowl.core.model.dao.IApplicationLayer#handleFeedReload(org.rssowl.core.model.persist.IBookMark,
   * org.rssowl.core.model.persist.IFeed,
   * org.rssowl.core.model.persist.IConditionalGet, boolean)
   */
  public final void handleFeedReload(IBookMark bookMark, IFeed emptyFeed, IConditionalGet conditionalGet, boolean deleteConditionalGet) {
    fWriteLock.lock();
    MergeResult mergeResult = null;
    try {
      /* Resolve reloaded Feed */
      IFeed feed = bookMark.getFeedLinkReference().resolve();

      /* Feed could have been deleted meanwhile! */
      if (feed == null)
        return;

      /* Copy over Properties to reloaded Feed to keep them */
      Map<String, Serializable> feedProperties = feed.getProperties();
      if (feedProperties != null) {
        feedProperties.entrySet();
        for (Map.Entry<String, Serializable> entry : feedProperties.entrySet())
          emptyFeed.setProperty(entry.getKey(), entry.getValue());
      }

      /* Merge with existing */
      mergeResult = feed.mergeAndCleanUp(emptyFeed);
      List<INews> newNewsAdded = getNewNewsAdded(feed);

      /* Update Date of last added news in Bookmark */
      if (!newNewsAdded.isEmpty()) {
        Date mostRecentDate = DateUtils.getRecentDate(newNewsAdded);
        Date previousMostRecentDate = bookMark.getMostRecentNewsDate();
        if (previousMostRecentDate == null || mostRecentDate.after(previousMostRecentDate)) {
          bookMark.setMostRecentNewsDate(mostRecentDate);
          fDb.set(bookMark);
        }
      }

      updateStateOfUnsavedNewNews(newNewsAdded);

      /* Retention Policy */
      List<INews> deletedNews = RetentionStrategy.process(bookMark, feed, newNewsAdded.size());

      for (INews news : deletedNews)
        mergeResult.addUpdatedObject(news);

      for (INews news : newNewsAdded) {
        String description = ((News) news).getTransientDescription();
        if (description != null) {
          IDGenerator generator = Owl.getPersistenceService().getIDGenerator();
          long id;
          if (generator instanceof DB4OIDGenerator)
            id = ((DB4OIDGenerator) generator).getNext(false);
          else
            id = generator.getNext();

          news.setId(id);
          mergeResult.addUpdatedObject(new Description(news, description));
        }
      }

      try {
        lockNewsObjects(mergeResult);
        saveFeed(mergeResult);

        /* Update Conditional GET */
        if (conditionalGet != null) {
          if (deleteConditionalGet)
            fDb.delete(conditionalGet);
          else
            fDb.ext().set(conditionalGet, 1);
        }
        DBHelper.preCommit(fDb);
        fDb.commit();
      } finally {
        unlockNewsObjects(mergeResult);
      }
    } catch (Db4oException e) {
      DBHelper.rollbackAndPE(fDb, e);
    } finally {
      fWriteLock.unlock();
    }
    DBHelper.cleanUpAndFireEvents();
  }

  private void lockNewsObjects(MergeResult mergeResult) {
    for (Object object : mergeResult.getUpdatedObjects()) {
      if (object instanceof News) {
        ((News) object).acquireReadLockSpecial();
      }
    }
  }

  private void unlockNewsObjects(MergeResult mergeResult) {
    if (mergeResult != null) {
      for (Object object : mergeResult.getUpdatedObjects()) {
        if (object instanceof News) {
          News news = (News) object;
          news.releaseReadLockSpecial();
          news.clearTransientDescription();
        }
      }
    }
  }

  private List<INews> getNewNewsAdded(IFeed feed) {
    List<INews> newsList = feed.getNewsByStates(EnumSet.of(INews.State.NEW));

    for (ListIterator<INews> it = newsList.listIterator(newsList.size()); it.hasPrevious(); ) {
      INews news = it.previous();
      /* Relies on the fact that news added during merge have no id assigned yet. */
      if (news.getId() != null)
        it.remove();
    }
    return newsList;
  }

  private void updateStateOfUnsavedNewNews(List<INews> news) {
    if (news.isEmpty())
      return;

    List<URI> links = new ArrayList<URI>();
    List<IGuid> guids = new ArrayList<IGuid>();
    for (INews newsItem : news) {
      if (newsItem.getGuid() != null)
        guids.add(newsItem.getGuid());
      else if (newsItem.getLink() != null)
        links.add(newsItem.getLink());
    }

    ModelSearchImpl modelSearch = (ModelSearchImpl) Owl.getPersistenceService().getModelSearch();
    Map<URI, List<NewsReference>> linkToNewsRefs = modelSearch.searchNewsByLinks(links, false);
    Map<IGuid, List<NewsReference>> guidToNewsRefs = modelSearch.searchNewsByGuids(guids, false);
    for (INews newsItem : news) {
      List<NewsReference> equivalentNewsRefs = guidToNewsRefs.get(newsItem.getGuid());
      if (equivalentNewsRefs != null)
        newsItem.setState(equivalentNewsRefs.get(0).resolve().getState());
      else {
        equivalentNewsRefs = linkToNewsRefs.get(newsItem.getLink());
        if (equivalentNewsRefs != null)
          newsItem.setState(equivalentNewsRefs.get(0).resolve().getState());
      }
    }
  }

  private void saveFeed(MergeResult mergeResult) {
    SortedLongArrayList descriptionUpdatedIds = new SortedLongArrayList(10);
    for (Object o : mergeResult.getRemovedObjects()) {
      /* We know that in these cases, the parent entity will be updated */
      if (o instanceof INews)
        EventManager.getInstance().addItemBeingDeleted(((INews) o).getFeedReference());
      else if (o instanceof IAttachment)
        EventManager.getInstance().addItemBeingDeleted(((IAttachment) o).getNews());
      else if (o instanceof Description)
        descriptionUpdatedIds.add(((Description) o).getNews().getId());

      fDb.delete(o);
    }

    List<Object> otherObjects = new ArrayList<Object>();
    for (Object o : mergeResult.getUpdatedObjects()) {
      if (o instanceof INews)
        DBHelper.saveNews(fDb, (INews) o);
      else {
        if (o instanceof Description)
          descriptionUpdatedIds.add(((Description) o).getNews().getId());

        otherObjects.add(o);
      }
    }

    for (Object o : otherObjects) {
      if (o instanceof IFeed) {
        fDb.ext().set(o, 2);
      }
      else
        fDb.ext().set(o, 1);
    }

    NewsEventRunnable eventRunnables = DBHelper.getNewsEventRunnables(EventsMap.getInstance().getEventRunnables());
    if (eventRunnables != null) {
      for (NewsEvent event : eventRunnables.getAllEvents())
        descriptionUpdatedIds.removeByElement(event.getEntity().getId().longValue());
    }

    INewsDAO newsDao = DynamicDAO.getDAO(INewsDAO.class);
    for (int i = 0, c = descriptionUpdatedIds.size(); i < c; ++i) {
      long newsId = descriptionUpdatedIds.get(i);
      INews news = newsDao.load(newsId);
      INews oldNews = DBHelper.peekPersistedNews(fDb, news);
      EventsMap.getInstance().putUpdateEvent(new NewsEvent(oldNews, news, false));
    }
  }
}