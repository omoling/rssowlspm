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

import org.rssowl.core.internal.persist.Mark;
import org.rssowl.core.internal.persist.NewsContainer;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsReference;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * TODO Remove this workaround until a real virtual solution is implemented.
 *
 * @author bpasero
 */
@SuppressWarnings("restriction")
public class FolderNewsMark extends Mark implements INewsMark {
  private NewsContainer fNewsContainer;
  private final IFolder fFolder;

  /**
   * @param folder
   */
  public FolderNewsMark(IFolder folder) {
    super(folder.getId(), folder.getParent(), folder.getName());
    fFolder = folder;

    fNewsContainer = new NewsContainer(Collections.<INews.State, Boolean> emptyMap());
    fillNews(folder);
  }

  /**
   * @return the {@link IFolder} that serves as input to this {@link INewsMark}.
   */
  public IFolder getFolder() {
    return fFolder;
  }

  private void fillNews(IFolder folder) {
    List<IFolderChild> children = folder.getChildren();
    for (IFolderChild child : children) {

      if (child instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) child;
        List<INews> news = bookmark.getNews(INews.State.getVisible());
        for (INews newsitem : news) {
          fNewsContainer.addNews(newsitem);
        }
      }

      /* Recursively treat Folders */
      if (child instanceof IFolder)
        fillNews((IFolder) child);
    }
  }

  /**
   * @param news
   * @return <code>true</code> if the given News belongs to any
   * {@link IBookMark} of the given {@link IFolder}.
   */
  public boolean isRelatedTo(INews news) {
    FeedLinkReference feedRef = news.getFeedReference();
    return isRelatedTo(fFolder, feedRef);
  }

  private boolean isRelatedTo(IFolder folder, FeedLinkReference ref) {
    List<IFolderChild> children = folder.getChildren();

    for (IFolderChild child : children) {
      if (child instanceof IFolder)
        return isRelatedTo((IFolder) child, ref);
      else if (child instanceof IBookMark) {
        IBookMark bookmark = (IBookMark) child;
        if (bookmark.getFeedLinkReference().equals(ref))
          return true;
      }
    }

    return false;
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#containsNews(org.rssowl.core.persist.INews)
   */
  public synchronized boolean containsNews(INews news) {
    return fNewsContainer.containsNews(news);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNews()
   */
  public synchronized List<INews> getNews() {
    return getNews(EnumSet.allOf(INews.State.class));
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNews(java.util.Set)
   */
  public List<INews> getNews(Set<State> states) {
    List<NewsReference> newsRefs;
    synchronized (this) {
      newsRefs = fNewsContainer.getNews(states);
    }
    return getNews(newsRefs, false);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsCount(java.util.Set)
   */
  public synchronized int getNewsCount(Set<State> states) {
    return fNewsContainer.getNewsCount(states);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsRefs()
   */
  public synchronized List<NewsReference> getNewsRefs() {
    return fNewsContainer.getNews();
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#getNewsRefs(java.util.Set)
   */
  public synchronized List<NewsReference> getNewsRefs(Set<State> states) {
    return fNewsContainer.getNews(states);
  }

  /*
   * @see org.rssowl.core.persist.INewsMark#isGetNewsRefsEfficient()
   */
  public boolean isGetNewsRefsEfficient() {
    return true;
  }

  /*
   * @see org.rssowl.core.persist.IEntity#toReference()
   */
  public ModelReference toReference() {
    return new ModelReference(0, FolderNewsMark.class) {};
  }
}