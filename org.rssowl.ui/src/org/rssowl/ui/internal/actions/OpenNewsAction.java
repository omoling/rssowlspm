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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.editors.feed.PerformAfterInputSet;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>OpenNewsAction</code> will open a given Selection of
 * <code>INews</code> in the <code>FeedView</code> opening the related
 * BookMark and adjusting the selection.
 *
 * @author bpasero
 */
public class OpenNewsAction extends Action {
  private IStructuredSelection fSelection;
  private Shell fShellToMinimize;

  /**
   * @param selection
   */
  public OpenNewsAction(IStructuredSelection selection) {
    this(selection, null);
  }

  /**
   * @param selection
   * @param shellToMinimize The <code>Shell</code> to minimize (e.g. a Dialog)
   * when executing this action, or <code>NULL</code> if none.
   */
  public OpenNewsAction(IStructuredSelection selection, Shell shellToMinimize) {
    Assert.isTrue(selection != null && !selection.isEmpty());
    fSelection = selection;
    fShellToMinimize = shellToMinimize;

    setText("Open");
  }

  /*
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    internalRun();
  }

  /*
   * @see org.eclipse.jface.action.Action#runWithEvent(org.eclipse.swt.widgets.Event)
   */
  @Override
  public void runWithEvent(Event event) {
    internalRun();
  }

  private void internalRun() {

    /* Require a Page */
    IWorkbenchPage page = OwlUI.getPage();
    if (page == null)
      return;

    int openedEditors = 0;
    int maxOpenEditors = EditorUtils.getOpenEditorLimit();
    IEditorPart lastOpenedEditor = null;

    /* Convert selection to List of News (1 per Feed) */
    List< ? > list = fSelection.toList();
    List<FeedLinkReference> handledFeeds = new ArrayList<FeedLinkReference>(list.size());
    List<INews> newsToOpen = new ArrayList<INews>(list.size());
    for (Object selection : list) {
      if (selection instanceof INews) {
        INews news = (INews) selection;
        FeedLinkReference feedRef = news.getFeedReference();

        /* Check if already Handled */
        if (!handledFeeds.contains(feedRef)) {
          newsToOpen.add(news);
          handledFeeds.add(feedRef);
        }
      }
    }

    /* Minimize Shell if present */
    if (newsToOpen.size() > 0 && fShellToMinimize != null)
      fShellToMinimize.setMinimized(true);

    /* Open Bookmarks belonging to the News */
    for (int i = 0; i < newsToOpen.size() && openedEditors < maxOpenEditors; i++) {
      INews news = newsToOpen.get(i);

      /* Receive the first Bookmark belonging to the News and open it */
      IBookMark bookmark = ModelUtils.getBookMark(news.getFeedReference());
      if (bookmark != null) {

        /* Select this News in the FeedView */
        PerformAfterInputSet perform = PerformAfterInputSet.selectNews(new NewsReference(news.getId()));
        perform.setActivate(false);

        /* Open this Bookmark */
        FeedViewInput fvInput = new FeedViewInput(bookmark, perform);
        try {
          FeedView feedview = null;

          /* First check if input already shown */
          IEditorPart existingEditor = page.findEditor(fvInput);
          if (existingEditor != null && existingEditor instanceof FeedView) {
            feedview = (FeedView) existingEditor;

            /* Set Selection */
            feedview.setSelection(new StructuredSelection(news));
          }

          /* Otherwise open the Input in a new Editor */
          else
            feedview = (FeedView) page.openEditor(fvInput, FeedView.ID, true);

          openedEditors++;
          lastOpenedEditor = feedview;
        } catch (PartInitException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }
    }

    /* Activate the last opened editor */
    if (lastOpenedEditor != null)
      page.activate(lastOpenedEditor);
  }
}