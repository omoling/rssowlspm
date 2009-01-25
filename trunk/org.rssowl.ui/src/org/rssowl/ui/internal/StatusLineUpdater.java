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

package org.rssowl.ui.internal;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchMark;

/**
 * Add the <code>StatusLineUpdater</code> to your ViewPart to have the
 * statusbar describing the selected elements.
 *
 * @author bpasero
 */
public class StatusLineUpdater implements ISelectionChangedListener {
  private IStatusLineManager fStatusLineManager;

  /**
   * @param statusLineManager
   */
  public StatusLineUpdater(IStatusLineManager statusLineManager) {
    fStatusLineManager = statusLineManager;
  }

  /*
   * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
   */
  public void selectionChanged(SelectionChangedEvent event) {
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    String text = formatElements(selection.toArray());

    /* Replace & with && */
    text = text.replaceAll("&", "&&"); //$NON-NLS-1$//$NON-NLS-2$

    /* Show Message */
    fStatusLineManager.setMessage(text);
  }

  /* TODO Use MessageFormat to support I18N here */
  private String formatElements(Object elements[]) {

    /* No Element selected */
    if (elements.length == 0)
      return ""; //$NON-NLS-1$

    /* Only 1 Element selected */
    if (elements.length == 1) {
      Object element = elements[0];
      if (element instanceof IFolder)
        return ((IFolder) element).getName();
      else if (element instanceof IMark)
        return ((IMark) element).getName();
      else if (element instanceof EntityGroup)
        return ((EntityGroup) element).getName();
      else if (element instanceof INews) // Ignore This
        return "";

      return "Item selected";
    }

    /* More than 1 Element selected */
    int newsCount = 0;
    int folderCount = 0;
    int bookMarkCount = 0;
    int newsBinCount = 0;
    int searchMarkCount = 0;
    int viewerGroupCount = 0;

    for (Object element : elements) {
      if (element instanceof IFolder)
        folderCount++;
      else if (element instanceof IBookMark)
        bookMarkCount++;
      else if (element instanceof INewsBin)
        newsBinCount++;
      else if (element instanceof ISearchMark)
        searchMarkCount++;
      else if (element instanceof EntityGroup)
        viewerGroupCount++;
      else if (element instanceof INews)
        newsCount++;
    }

    StringBuilder buf = new StringBuilder();
    buf.append(elements.length);
    buf.append(" items selected (");

    if (folderCount > 0)
      buf.append(folderCount).append(folderCount == 1 ? " Folder, " : " Folders, ");

    if (bookMarkCount > 0)
      buf.append(bookMarkCount).append(bookMarkCount == 1 ? " Bookmark, " : " Bookmarks, ");

    if (newsBinCount > 0)
      buf.append(newsBinCount).append(newsBinCount == 1 ? " News Bin, " : " News Bins, ");

    if (searchMarkCount > 0)
      buf.append(searchMarkCount).append(searchMarkCount == 1 ? " Saved Search, " : " Saved Searches, ");

    if (viewerGroupCount > 0)
      buf.append(viewerGroupCount).append(viewerGroupCount == 1 ? " Group, " : " Groups, ");

    if (newsCount > 0)
      buf.append(newsCount).append(" News, ");

    buf.delete(buf.length() - 2, buf.length());
    buf.append(")");
    return buf.toString();
  }
}