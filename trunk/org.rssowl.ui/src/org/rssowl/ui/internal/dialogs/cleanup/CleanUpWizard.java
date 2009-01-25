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

package org.rssowl.ui.internal.dialogs.cleanup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.dialogs.ConfirmDeleteDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author bpasero
 */
public class CleanUpWizard extends Wizard {
  private CleanUpOptionsPage fCleanUpOptionsPage;
  private FeedSelectionPage fFeedSelectionPage;
  private CleanUpSummaryPage fCleanUpSummaryPage;

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setWindowTitle("Clean Up");
    setHelpAvailable(false);

    /* Choose Feeds for Clean-Up */
    fFeedSelectionPage = new FeedSelectionPage("Choose Bookmarks");
    addPage(fFeedSelectionPage);

    /* Clean Up Options */
    fCleanUpOptionsPage = new CleanUpOptionsPage("Clean Up Operations");
    addPage(fCleanUpOptionsPage);

    /* Clean Up Summary */
    fCleanUpSummaryPage = new CleanUpSummaryPage("Summary");
    addPage(fCleanUpSummaryPage);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @SuppressWarnings("restriction")
  @Override
  public boolean performFinish() {
    final INewsDAO newsDao = DynamicDAO.getDAO(INewsDAO.class);
    final CleanUpOperations operations = fCleanUpOptionsPage.getOperations();

    /* Receive Tasks */
    final List<CleanUpTask> tasks = fCleanUpSummaryPage.getTasks();

    /* Show final confirmation prompt */
    int bmCounter = 0;
    int newsCounter = 0;
    for (CleanUpTask task : tasks) {
      if (task instanceof BookMarkTask)
        bmCounter++;
      else if (task instanceof NewsTask)
        newsCounter += ((NewsTask) task).getNews().size();
    }

    if (bmCounter != 0 || newsCounter != 0) {
      String msg = "Are you sure you want to delete ";
      String bmMsg = bmCounter > 1 ? bmCounter + " bookmarks" : bmCounter + " bookmark";
      if (bmCounter != 0 && newsCounter != 0)
        msg += bmMsg + " and " + newsCounter + " news?";
      else if (bmCounter != 0)
        msg += bmMsg + "?";
      else
        msg += newsCounter + " news?";

      ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(getShell(), "Confirm Delete", "This action can not be undone", msg, null);
      if (dialog.open() != Window.OK)
        return false;
    }

    /* Runnable that performs the tasks */
    IRunnableWithProgress runnable = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) {
        boolean optimizeSearch = false;
        monitor.beginTask("Please wait while cleaning up...", IProgressMonitor.UNKNOWN);

        /* Perform Tasks */
        List<IBookMark> bookmarks = new ArrayList<IBookMark>();
        for (CleanUpTask task : tasks) {

          /* Delete Bookmark Task */
          if (task instanceof BookMarkTask)
            bookmarks.add(((BookMarkTask) task).getMark());

          /* Delete News Task */
          else if (task instanceof NewsTask) {
            Collection<NewsReference> news = ((NewsTask) task).getNews();
            List<INews> resolvedNews = new ArrayList<INews>(news.size());
            for (NewsReference newsRef : news)
              resolvedNews.add(newsRef.resolve());

            newsDao.setState(resolvedNews, INews.State.DELETED, false, false);
          }

          /* Optimize Lucene */
          else if (task instanceof OptimizeSearchTask)
            optimizeSearch = true;

          /* Defrag Database */
          else if (task instanceof DefragDatabaseTask)
            Owl.getPersistenceService().optimizeOnNextStartup();
        }

        /* Delete BookMarks */
        Controller.getDefault().getSavedSearchService().forceQuickUpdate();
        DynamicDAO.deleteAll(bookmarks);

        /* Optimize Search */
        if (optimizeSearch)
          Owl.getPersistenceService().getModelSearch().optimize();

        /* Save Operations */
        IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();

        preferences.putBoolean(DefaultPreferences.CLEAN_UP_BM_BY_LAST_VISIT_STATE, operations.deleteFeedByLastVisit());
        preferences.putInteger(DefaultPreferences.CLEAN_UP_BM_BY_LAST_VISIT_VALUE, operations.getLastVisitDays());

        preferences.putBoolean(DefaultPreferences.CLEAN_UP_BM_BY_LAST_UPDATE_STATE, operations.deleteFeedByLastUpdate());
        preferences.putInteger(DefaultPreferences.CLEAN_UP_BM_BY_LAST_UPDATE_VALUE, operations.getLastUpdateDays());

        preferences.putBoolean(DefaultPreferences.CLEAN_UP_BM_BY_CON_ERROR, operations.deleteFeedsByConError());
        preferences.putBoolean(DefaultPreferences.CLEAN_UP_BM_BY_DUPLICATES, operations.deleteFeedsByDuplicates());

        preferences.putBoolean(DefaultPreferences.CLEAN_UP_NEWS_BY_AGE_STATE, operations.deleteNewsByAge());
        preferences.putInteger(DefaultPreferences.CLEAN_UP_NEWS_BY_AGE_VALUE, operations.getMaxNewsAge());

        preferences.putBoolean(DefaultPreferences.CLEAN_UP_NEWS_BY_COUNT_STATE, operations.deleteNewsByCount());
        preferences.putInteger(DefaultPreferences.CLEAN_UP_NEWS_BY_COUNT_VALUE, operations.getMaxNewsCountPerFeed());

        preferences.putBoolean(DefaultPreferences.CLEAN_UP_READ_NEWS_STATE, operations.deleteReadNews());
        preferences.putBoolean(DefaultPreferences.CLEAN_UP_NEVER_DEL_UNREAD_NEWS_STATE, operations.keepUnreadNews());

        monitor.done();
      }
    };

    /* Perform Runnable in separate Thread and show progress */
    try {
      getContainer().run(true, false, runnable);
    } catch (InvocationTargetException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    } catch (InterruptedException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

    return true;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return true;
  }
}