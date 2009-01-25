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

package org.rssowl.ui.internal.notifier;

import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.persist.event.SearchMarkAdapter;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.event.SearchMarkListener;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.BatchedBuffer;
import org.rssowl.ui.internal.ApplicationWorkbenchAdvisor;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The <code>NotificationService</code> listens on News being downloaded and
 * opens the <code>NotificationPopup</code> to show them in case the
 * preferences are set to show notifications.
 *
 * @author bpasero
 */
public class NotificationService {

  /* Batch News-Events for every 5 seconds */
  private static final int BATCH_INTERVAL = 5000;

  private final NewsListener fNewsListener;
  private SearchMarkListener fSearchMarkListener;
  private final IPreferenceScope fGlobalPreferences;
  private final BatchedBuffer<NotificationItem> fBatchedBuffer;

  /* Singleton instance */
  private static NotificationPopup fgNotificationPopup;

  /** Creates a new Notification Service */
  public NotificationService() {

    /* Process Events batched */
    BatchedBuffer.Receiver<NotificationItem> receiver = new BatchedBuffer.Receiver<NotificationItem>() {
      public void receive(Set<NotificationItem> items) {
        showItems(items);
      }
    };

    fBatchedBuffer = new BatchedBuffer<NotificationItem>(receiver, BATCH_INTERVAL);
    fGlobalPreferences = Owl.getPreferenceService().getGlobalScope();
    fNewsListener = registerNewsListener();
    fSearchMarkListener = registerSearchMarkListener();
  }

  /** Shutdown this Service */
  public void stopService() {
    DynamicDAO.removeEntityListener(INews.class, fNewsListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
  }

  private boolean isPopupVisible() {
    return fgNotificationPopup != null;
  }

  /* Listen on News Events */
  private NewsListener registerNewsListener() {
    NewsListener listener = new NewsAdapter() {
      @Override
      public void entitiesAdded(final Set<NewsEvent> events) {
        onNewsAdded(events);
      }
    };

    DynamicDAO.addEntityListener(INews.class, listener);
    return listener;
  }

  /* Listen on Search Mark Events */
  private SearchMarkListener registerSearchMarkListener() {
    SearchMarkListener listener = new SearchMarkAdapter() {
      @Override
      public void resultsChanged(Set<SearchMarkEvent> events) {
        onResultsChanged(events);
      }
    };

    DynamicDAO.addEntityListener(ISearchMark.class, listener);
    return listener;
  }

  private void onResultsChanged(Set<SearchMarkEvent> events) {

    /* Return if Notification is disabled */
    if (!fGlobalPreferences.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP))
      return;

    /* Filter Events if user decided to show Notifier only for selected Elements */
    Set<SearchMarkEvent> filteredEvents = new HashSet<SearchMarkEvent>(events.size());
    if (fGlobalPreferences.getBoolean(DefaultPreferences.LIMIT_NOTIFIER_TO_SELECTION)) {
      for (SearchMarkEvent event : events) {

        /* Check for new *new* News matching now */
        if (!event.isAddedNewNews())
          continue;

        /* Check for explicit selection */
        IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(event.getEntity());
        if (prefs.getBoolean(DefaultPreferences.ENABLE_NOTIFIER))
          filteredEvents.add(event);
      }
    }

    /* Filter Events based on other criterias otherwise */
    else {
      for (SearchMarkEvent event : events) {
        ISearchMark searchmark = event.getEntity();
        List<ISearchCondition> conditions = searchmark.getSearchConditions();

        /* Check for new *new* News matching now */
        if (!event.isAddedNewNews())
          continue;

        /* Look for a String search condition that is not Label */
        for (ISearchCondition condition : conditions) {
          if (condition.getValue() instanceof String && condition.getField().getId() != INews.LABEL) {
            filteredEvents.add(event);
            break;
          }
        }
      }
    }

    /* Create Items */
    List<NotificationItem> items = new ArrayList<NotificationItem>(filteredEvents.size());
    for (SearchMarkEvent event : filteredEvents)
      items.add(new SearchNotificationItem(event.getEntity(), event.getEntity().getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED))));

    /* Add into Buffer */
    if (!isPopupVisible())
      fBatchedBuffer.addAll(items);

    /* Show Directly */
    else
      showItems(items);
  }

  private void onNewsAdded(Set<NewsEvent> events) {

    /* Return if Notification is disabled */
    if (!fGlobalPreferences.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP))
      return;

    /* Filter Events if user decided to show Notifier only for selected Elements */
    if (fGlobalPreferences.getBoolean(DefaultPreferences.LIMIT_NOTIFIER_TO_SELECTION)) {
      List<FeedLinkReference> enabledFeeds = new ArrayList<FeedLinkReference>();

      /* TODO This can be slow, try to optimize performance! */
      Collection<IBookMark> bookMarks = DynamicDAO.loadAll(IBookMark.class);
      for (IBookMark bookMark : bookMarks) {
        IPreferenceScope prefs = Owl.getPreferenceService().getEntityScope(bookMark);
        if (prefs.getBoolean(DefaultPreferences.ENABLE_NOTIFIER))
          enabledFeeds.add(bookMark.getFeedLinkReference());
      }

      events = filterEvents(events, enabledFeeds);
    }

    /* Create Items */
    List<NotificationItem> items = new ArrayList<NotificationItem>(events.size());
    for (NewsEvent event : events)
      items.add(new NewsNotificationItem(event.getEntity()));

    /* Add into Buffer */
    if (!isPopupVisible())
      fBatchedBuffer.addAll(items);

    /* Show Directly */
    else
      showItems(items);
  }

  private Set<NewsEvent> filterEvents(Set<NewsEvent> events, List<FeedLinkReference> enabledFeeds) {
    Set<NewsEvent> filteredEvents = new HashSet<NewsEvent>();

    for (NewsEvent event : events) {
      if (enabledFeeds.contains(event.getEntity().getFeedReference()))
        filteredEvents.add(event);
    }

    return filteredEvents;
  }

  /* Show Notification in UI Thread */
  private void showItems(final Collection<NotificationItem> items) {

    /* Ignore empty lists */
    if (items.isEmpty())
      return;

    /* Make sure to run in UI Thread */
    JobRunner.runInUIThread(OwlUI.getPrimaryShell(), new Runnable() {
      public void run() {

        /* Return if Notification should only show when minimized */
        ApplicationWorkbenchWindowAdvisor advisor = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;
        boolean minimized = advisor != null && (advisor.isMinimizedToTray() || advisor.isMinimized());
        if (!minimized && fGlobalPreferences.getBoolean(DefaultPreferences.SHOW_NOTIFICATION_POPUP_ONLY_WHEN_MINIMIZED))
          return;

        /* Show News in Popup */
        synchronized (this) {
          if (fgNotificationPopup == null) {
            fgNotificationPopup = new NotificationPopup(items.size()) {
              @Override
              public boolean doClose() {
                fgNotificationPopup = null;
                return super.doClose();
              }
            };
            fgNotificationPopup.open(items);
          }

          /* Notifier already opened - Show Items */
          else
            fgNotificationPopup.makeVisible(items);
        }
      }
    });
  }
}