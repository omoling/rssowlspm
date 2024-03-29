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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.RetentionStrategy;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * @author bpasero
 */
public class MarkTypesReadAction extends Action implements IWorkbenchWindowActionDelegate {
  private IStructuredSelection fSelection;
  private INewsDAO fNewsDao;

  /**
   *
   */
  public MarkTypesReadAction() {
    this(StructuredSelection.EMPTY);
  }

  /**
   * @param selection
   */
  public MarkTypesReadAction(IStructuredSelection selection) {
    fSelection = selection;
    fNewsDao = DynamicDAO.getDAO(INewsDAO.class);
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    JobRunner.runInBackgroundWithBusyIndicator(new Runnable() {
      public void run() {
        MarkTypesReadAction.this.internalRun();
      }
    });
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    JobRunner.runInBackgroundWithBusyIndicator(new Runnable() {
      public void run() {
        MarkTypesReadAction.this.internalRun();
      }
    });
  }

  private void internalRun() {

    /* Only consider Entities */
    List<IEntity> entities = ModelUtils.getEntities(fSelection);

    /* Retrieve any Folder that is to be marked read */
    Set<IFolder> folders = null;
    for (Object element : entities) {
      if (element instanceof IFolder) {
        if (folders == null)
          folders = new HashSet<IFolder>();
        folders.add((IFolder) element);
      }
    }

    /* Normalize */
    if (folders != null)
      for (IFolder folder : folders)
        ModelUtils.normalize(folder, entities);

    /* Use Map for follow-up Retention */
    Map<IBookMark, Collection<INews>> retentionHelperMap = new HashMap<IBookMark, Collection<INews>>();

    /* Retrieve affected News */
    Set<INews> news = new HashSet<INews>();
    for (IEntity element : entities) {
      if (element instanceof IFolder)
        fillNews((IFolder) element, news, retentionHelperMap);
      else if (element instanceof IBookMark)
        fillNews((IBookMark) element, news, retentionHelperMap);
      else if (element instanceof INewsMark)
        fillNews((INewsMark) element, news);
      else if (element instanceof INews)
        news.add((INews) element);
    }

    /* Only affect equivalent News if not all News are affected */
    boolean affectEquivalentNews = !equalsRootFolders(folders);

    /* Support Undo */
    if (!news.isEmpty())
      UndoStack.getInstance().addOperation(new NewsStateOperation(news, INews.State.READ, affectEquivalentNews));

    /* Apply the state to the NewsItems for Retention to handle them properly */
    for (INews newsItem : news) {
      newsItem.setState(INews.State.READ);
    }

    /* See if Retention is required for each BookMark */
    Set<Entry<IBookMark, Collection<INews>>> entries = retentionHelperMap.entrySet();
    for (Entry<IBookMark, Collection<INews>> entry : entries) {
      IBookMark bookmark = entry.getKey();

      /* Delete News that are now marked as Read */
      List<INews> deletedNews = RetentionStrategy.process(bookmark, entry.getValue());

      /*
       * This is an optimization to the process. Any News that is marked as read
       * is getting deleted here. Thus, there is no need in marking the News as
       * Read.
       */
      news.removeAll(deletedNews);
    }

    /* Mark News Read */
    if (news.size() > 0) {

      /* Mark Saved Search Service as in need for a quick Update */
      Controller.getDefault().getSavedSearchService().forceQuickUpdate();

      /* Perform Operation */
      Owl.getPersistenceService().getDAOService().getNewsDAO().setState(news, INews.State.READ, affectEquivalentNews, true);
    }
  }

  private boolean equalsRootFolders(Collection<IFolder> folders) {
    Collection<IFolder> rootFolders = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
    return folders != null && folders.equals(rootFolders);
  }

  private void fillNews(IFolder folder, Collection<INews> news, Map<IBookMark, Collection<INews>> bookMarkNewsMap) {
    List<IFolderChild> children = folder.getChildren();
    for (IFolderChild child : children) {
      if (child instanceof IBookMark && containsUnread(((IBookMark) child)))
        fillNews((IBookMark) child, news, bookMarkNewsMap);
      else if (child instanceof INewsMark)
        fillNews((INewsMark) child, news);
      else if (child instanceof IFolder)
        fillNews((IFolder) child, news, bookMarkNewsMap);
    }
  }

  private boolean containsUnread(IBookMark mark) {
    return mark.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)) > 0;
  }

  private void fillNews(IBookMark bookmark, Collection<INews> news, Map<IBookMark, Collection<INews>> bookMarkNewsMap) {
    IPreferenceScope bookMarkPrefs = Owl.getPreferenceService().getEntityScope(bookmark);
    boolean requiresRetention = bookMarkPrefs.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE);

    final EnumSet<State> enumSet = EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED);
    /* Retention on read News required, load *read* as well */
    if (requiresRetention) {
      Collection<INews> feedsNews = fNewsDao.loadAll(bookmark.getFeedLinkReference(), INews.State.getVisible());
      for (INews newsItem : feedsNews) {
        /* Do not add READ news */
        if (enumSet.contains(newsItem.getState()))
          news.add(newsItem);
      }
      bookMarkNewsMap.put(bookmark, feedsNews);
    }

    /* No retention required, just load the ones being affected */
    else {
      news.addAll(fNewsDao.loadAll(bookmark.getFeedLinkReference(), enumSet));
    }
  }

  private void fillNews(INewsMark newsmark, Collection<INews> news) {
    news.addAll(newsmark.getNews(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)));
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection)
      fSelection = (IStructuredSelection) selection;
  }

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {}
}