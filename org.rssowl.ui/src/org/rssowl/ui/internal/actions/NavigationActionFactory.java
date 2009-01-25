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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

/**
 * The <code>NavigationActionFactory</code> is providing a list of common
 * Actions to navigate to News-Items or Feeds.
 * 
 * @author bpasero
 */
public class NavigationActionFactory implements IExecutableExtensionFactory, IExecutableExtension {
  private String fId;

  /* Actual Action for the Navigation */
  private static class NavigationAction implements IWorkbenchWindowActionDelegate {
    private Actions fType;

    NavigationAction(Actions type) {
      fType = type;
    }

    public void dispose() {}

    public void init(IWorkbenchWindow window) {}

    public void run(IAction action) {

      /* 1.) Navigate in opened FeedViews */
      if (navigateOnActiveFeedView())
        return;

      /* 2.) Navigate in opened Explorer */
      if (navigateOnOpenExplorer())
        return;

      /* 3.) Navigate on entire Model */
      if (navigateOnModel())
        return;
    }

    private boolean navigateOnActiveFeedView() {

      /* Get active FeedView if any */
      FeedView activeFeedView = OwlUI.getActiveFeedView();

      /* Run on active FeedView if any */
      if (fType.isNewsScoped() && activeFeedView != null) {
        if (activeFeedView.navigate(true, fType.isNewsScoped(), fType.isNext(), fType.isUnread())) {
          IWorkbenchPage page = activeFeedView.getSite().getPage();
          page.activate(activeFeedView.getSite().getPart());
          page.activate(activeFeedView);

          return true;
        }
      }

      return false;
    }

    private boolean navigateOnOpenExplorer() {

      /* Try finding the open Explorer for BookMarks */
      BookMarkExplorer bookmarkExplorer = OwlUI.getOpenBookMarkExplorer();
      if (bookmarkExplorer == null)
        return false;

      /* Navigate on Explorer */
      if (bookmarkExplorer.navigate(fType.isNewsScoped(), fType.isNext(), fType.isUnread()))
        return true;

      return false;
    }

    private boolean navigateOnModel() {
      // TODO Implement this!
      return false;
    }

    public void selectionChanged(IAction action, ISelection selection) {}
  }

  /** Enumeration with all possible types of NavigationAction */
  public enum Actions {

    /** Action: Go to the next News */
    NEXT_NEWS("nextNews", true, true, false),

    /** Action: Go to the next unread News */
    NEXT_UNREAD_NEWS("nextUnreadNews", true, true, true),

    /** Action: Go to the next Feed */
    NEXT_FEED("nextFeed", false, true, false),

    /** Action: Go to the next unread Feed */
    NEXT_UNREAD_FEED("nextUnreadFeed", false, true, true),

    /** Action: Go to the previous News */
    PREVIOUS_NEWS("previousNews", true, false, false),

    /** Action: Go to the previous unread News */
    PREVIOUS_UNREAD_NEWS("previousUnreadNews", true, false, true),

    /** Action: Go to the previous Feed */
    PREVIOUS_FEED("previousFeed", false, false, false),

    /** Action: Go to the previous unread Feed */
    PREVIOUS_UNREAD_FEED("previousUnreadFeed", false, false, true);

    String fId;
    boolean fIsNewsScoped;
    boolean fIsNext;
    boolean fIsUnread;

    Actions(String id, boolean isNewsScoped, boolean isNext, boolean isUnread) {
      fId = id;
      fIsNewsScoped = isNewsScoped;
      fIsNext = isNext;
      fIsUnread = isUnread;
    }

    String getId() {
      return fId;
    }

    boolean isNewsScoped() {
      return fIsNewsScoped;
    }

    boolean isUnread() {
      return fIsUnread;
    }

    boolean isNext() {
      return fIsNext;
    }
  };

  /** Keep for reflection */
  public NavigationActionFactory() {}

  /*
   * @see org.eclipse.core.runtime.IExecutableExtensionFactory#create()
   */
  public Object create() {
    if (Actions.NEXT_NEWS.getId().equals(fId))
      return new NavigationAction(Actions.NEXT_NEWS);

    if (Actions.NEXT_UNREAD_NEWS.getId().equals(fId))
      return new NavigationAction(Actions.NEXT_UNREAD_NEWS);

    if (Actions.NEXT_FEED.getId().equals(fId))
      return new NavigationAction(Actions.NEXT_FEED);

    if (Actions.NEXT_UNREAD_FEED.getId().equals(fId))
      return new NavigationAction(Actions.NEXT_UNREAD_FEED);

    if (Actions.PREVIOUS_NEWS.getId().equals(fId))
      return new NavigationAction(Actions.PREVIOUS_NEWS);

    if (Actions.PREVIOUS_UNREAD_NEWS.getId().equals(fId))
      return new NavigationAction(Actions.PREVIOUS_UNREAD_NEWS);

    if (Actions.PREVIOUS_FEED.getId().equals(fId))
      return new NavigationAction(Actions.PREVIOUS_FEED);

    if (Actions.PREVIOUS_UNREAD_FEED.getId().equals(fId))
      return new NavigationAction(Actions.PREVIOUS_UNREAD_FEED);

    return null;
  }

  /*
   * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement,
   * java.lang.String, java.lang.Object)
   */
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
    if (data instanceof String)
      fId = (String) data;
    else
      throw new CoreException(Activator.getDefault().createErrorStatus("Data argument must be a String for " + getClass(), null));
  }
}