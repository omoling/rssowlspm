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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.ui.internal.OwlUI;

import java.util.List;

/**
 * Copy the Link of the given Elements into the Clipboard.. E.g. the Link of a
 * BookMark's Feed.
 *
 * @author bpasero
 */
public class CopyLinkAction implements IObjectActionDelegate {
  private IStructuredSelection fSelection;

  /*
   * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
   * org.eclipse.ui.IWorkbenchPart)
   */
  public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  public void run(IAction action) {
    StringBuilder str = new StringBuilder();

    /* Build Contents */
    if (!fSelection.isEmpty()) {
      List< ? > list = fSelection.toList();
      int i = 0;
      for (Object element : list) {
        if (element instanceof IBookMark) {
          str.append(i > 0 ? "\n" : "").append(((IBookMark) element).getFeedLinkReference().getLinkAsText());
          i++;
        } else if (element instanceof INews) {
          INews news = (INews) element;
          if (news.getLinkAsText() != null) {
            str.append(i > 0 ? "\n" : "").append(news.getLinkAsText());
            i++;
          }
        }
      }
    }

    /* Set Contents to Clipboard */
    if (str.length() > 0)
      OwlUI.getClipboard().setContents(new Object[] { str.toString() }, new Transfer[] { TextTransfer.getInstance() });
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
   * org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection)
      fSelection = (IStructuredSelection) selection;
  }
}