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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.editors.feed.PerformAfterInputSet;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.Date;

/**
 * Instance of {@link NotificationItem} to display new {@link INews} in the
 * Notifier.
 *
 * @author bpasero
 */
public class NewsNotificationItem extends NotificationItem {

  /* Max. length of the Description Excerpt */
  private static final int MAX_DESCRIPTION_LENGTH = 500;

  private final String fNewsLink;
  private final FeedLinkReference fFeedReference;
  private final NewsReference fNewsReference;
  private final Date fRecentNewsDate;
  private boolean fIsNewsSticky;
  private String fCachedDescriptionExcerpt;
  private String fCachedOrigin;

  /**
   * @param news The news that is to be displayed in the Notifier.
   */
  public NewsNotificationItem(INews news) {
    super(makeText(news), makeImage(news));

    fNewsLink = news.getLinkAsText();
    fFeedReference = news.getFeedReference();
    fNewsReference = new NewsReference(news.getId());
    fRecentNewsDate = DateUtils.getRecentDate(news);
    fIsNewsSticky = news.isFlagged();
  }

  private String extractDescriptionExcerpt(INews news) {
    String description = news.getDescription();
    if (!StringUtils.isSet(description))
      return null;

    String content = StringUtils.stripTags(description);
    content = StringUtils.normalizeString(content);
    content = StringUtils.smartTrim(content, MAX_DESCRIPTION_LENGTH);

    if (content.contains("&"))
      content = StringUtils.replaceAll(content, "&", "&&");

    return content.length() > 0 ? content : null;
  }

  private static ImageDescriptor makeImage(INews news) {
    IBookMark bookMark = ModelUtils.getBookMark(news.getFeedReference());
    if (bookMark != null) {
      ImageDescriptor favicon = OwlUI.getFavicon(bookMark);
      if (favicon != null)
        return favicon;
    }

    return OwlUI.BOOKMARK;
  }

  private static String makeText(INews news) {
    String headline = ModelUtils.getHeadline(news);
    if (headline.contains("&"))
      headline = StringUtils.replaceAll(headline, "&", "&&");

    return headline;
  }

  /*
   * @see org.rssowl.ui.internal.notifier.NotificationItem#getDescription()
   */
  @Override
  public String getDescription() {
    if (fCachedDescriptionExcerpt == null)
      fCachedDescriptionExcerpt = extractDescriptionExcerpt(fNewsReference.resolve());

    return fCachedDescriptionExcerpt;
  }

  /*
   * @see org.rssowl.ui.internal.notifier.NotificationItem#getOrigin()
   */
  @Override
  public String getOrigin() {
    if (fCachedOrigin == null) {
      IBookMark bookMark = ModelUtils.getBookMark(fFeedReference);
      if (bookMark != null)
        fCachedOrigin = bookMark.getName();
    }

    return fCachedOrigin;
  }

  /*
   * @see org.rssowl.ui.internal.notifier.NotificationItem#open()
   */
  @Override
  public void open(MouseEvent e) {

    /* Open Link in Browser if Modifier Key is pressed */
    if ((e.stateMask & SWT.MOD1) != 0) {
      new OpenInExternalBrowserAction(new StructuredSelection(fNewsLink)).run();
      return;
    }

    /* Otherwise open Feedview and select the News */
    IBookMark bookMark = ModelUtils.getBookMark(fFeedReference);
    IWorkbenchPage page = OwlUI.getPage();
    if (page != null) {

      /* Restore Window */
      OwlUI.restoreWindow(page);

      /* First try if the Bookmark is already visible */
      IEditorReference editorRef = EditorUtils.findEditor(page.getEditorReferences(), bookMark);
      if (editorRef != null) {
        IEditorPart editor = editorRef.getEditor(false);
        if (editor instanceof FeedView) {
          ((FeedView) editor).setSelection(new StructuredSelection(fNewsReference));
          page.activate(editor);
        }
      }

      /* Otherwise Open */
      else {
        boolean activateEditor = OpenStrategy.activateOnOpen();
        FeedViewInput input = new FeedViewInput(bookMark, PerformAfterInputSet.selectNews(fNewsReference));
        try {
          OwlUI.getPage().openEditor(input, FeedView.ID, activateEditor);
        } catch (PartInitException ex) {
          Activator.getDefault().getLog().log(ex.getStatus());
        }
      }
    }
  }

  /*
   * @see org.rssowl.ui.internal.notifier.NotificationItem#supportsSticky()
   */
  @Override
  public boolean supportsSticky() {
    return true;
  }

  /*
   * @see org.rssowl.ui.internal.notifier.NotificationItem#isSticky()
   */
  @Override
  public boolean isSticky() {
    return fIsNewsSticky;
  }

  /*
   * @see org.rssowl.ui.internal.notifier.NotificationItem#makeSticky(boolean)
   */
  @Override
  public void setSticky(boolean sticky) {
    fIsNewsSticky = sticky;

    INews news = fNewsReference.resolve();
    news.setFlagged(sticky);
    DynamicDAO.save(news);
  }

  /*
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(NotificationItem o) {

    /* Compare with other News Item */
    if (o instanceof NewsNotificationItem) {
      Date date1 = fRecentNewsDate;
      Date date2 = ((NewsNotificationItem) o).fRecentNewsDate;

      return date2.compareTo(date1);
    }

    /* Otherwise sort to Bottom */
    return 1;
  }

  /*
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((fNewsReference == null) ? 0 : fNewsReference.hashCode());
    return result;
  }

  /*
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;

    if (obj == null)
      return false;

    if (getClass() != obj.getClass())
      return false;

    NewsNotificationItem other = (NewsNotificationItem) obj;
    if (fNewsReference == null) {
      if (other.fNewsReference != null)
        return false;
    } else if (!fNewsReference.equals(other.fNewsReference))
      return false;

    return true;
  }
}