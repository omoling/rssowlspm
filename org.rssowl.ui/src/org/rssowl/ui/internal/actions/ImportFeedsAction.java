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
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.util.JobRunner;

/**
 * TODO This is just for Developers Purposes!
 *
 * @author bpasero
 */
public class ImportFeedsAction extends Action implements IWorkbenchWindowActionDelegate {
  private Shell fShell;

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

  @Override
  public void run() {
    FileDialog dialog = new FileDialog(fShell);
    dialog.setText("Import Feeds from OPML");
    dialog.setFilterExtensions(new String[] { "*.opml", "*.xml", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    String string = dialog.open();
    if (string != null) {
      try {
        Controller.getDefault().importFeeds(string);
      } catch (Exception e) {
        Activator.getDefault().logError(e.getMessage(), e);
        ErrorDialog.openError(fShell, "Error importing Feeds", "RSSOwl was unable to import '" + string + "'", Activator.getDefault().createErrorStatus(e.getMessage(), e));
      }

      /* Force to rerun saved searches */
      JobRunner.runDelayedInBackgroundThread(new Runnable() {
        public void run() {
          Controller.getDefault().getSavedSearchService().updateSavedSearches(true);
        }
      });
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {}
}