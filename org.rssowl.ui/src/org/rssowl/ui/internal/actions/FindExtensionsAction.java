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

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.update.search.BackLevelFilter;
import org.eclipse.update.search.EnvironmentFilter;
import org.eclipse.update.search.UpdateSearchRequest;
import org.eclipse.update.search.UpdateSearchScope;
import org.eclipse.update.ui.UpdateJob;
import org.eclipse.update.ui.UpdateManagerUI;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author bpasero
 */
public class FindExtensionsAction extends Action implements IWorkbenchWindowActionDelegate {

  /* RSSOwl.org Update Site */
  private static final String UPDATE_SITE = "http://boreal.rssowl.org"; //$NON-NLS-1$

  /* RSSOwl Category */
  private static final String RSSOWL_CATEGORY = "RSSOwl Application"; //$NON-NLS-1$

  private Shell fShell;

  /** Keep default constructor for reflection. */
  public FindExtensionsAction() {}

  @Override
  public void run() {
    BusyIndicator.showWhile(fShell.getDisplay(), new Runnable() {
      public void run() {
        UpdateJob job = new UpdateJob("Searching for RSSOwl Extensions", getSearchRequest());
        job.setUser(true);
        job.setPriority(Job.INTERACTIVE);
        UpdateManagerUI.openInstaller(fShell, job);
      }
    });
  }

  @SuppressWarnings("nls")
  UpdateSearchRequest getSearchRequest() {
    UpdateSearchScope scope = new UpdateSearchScope();
    try {
      URL url = new URL(UPDATE_SITE);
      scope.addSearchSite("RSSOwl.org", url, new String[] { RSSOWL_CATEGORY });
    } catch (MalformedURLException e) {
      // skip bad URLs
    }

    UpdateSearchRequest result = new UpdateSearchRequest(UpdateSearchRequest.createDefaultSiteSearchCategory(), scope);
    result.addFilter(new BackLevelFilter());
    result.addFilter(new EnvironmentFilter());
    return result;
  }

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {
    fShell = window.getShell();
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    run();
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {}
}