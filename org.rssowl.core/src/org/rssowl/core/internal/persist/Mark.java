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

package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.reference.NewsReference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The abstract super-type of <code>BookMark</code> and
 * <code>SearchMark</code>. Used to associate Bookmarks and Searchmarks with
 * a Folder. These Elements are considered to be leaves of the Tree.
 *
 * @author bpasero
 */
public abstract class Mark extends AbstractEntity implements IMark {

  /* Attributes */
  private String fName;
  private Date fCreationDate;
  private Date fLastVisitDate;
  private int fPopularity;
  private IFolder fParent;

  /**
   * Store ID, Name and Folder for this Mark.
   *
   * @param id The unique id of this type.
   * @param parent The Folder this Mark belongs to.
   * @param name The Name of this Mark.
   */
  protected Mark(Long id, IFolder parent, String name) {
    super(id);
    Assert.isNotNull(parent, "The type Mark requires a Folder that is not NULL"); //$NON-NLS-1$
    fParent = parent;
    Assert.isNotNull(name, "The type Mark requires a Name that is not NULL"); //$NON-NLS-1$
    fName = name;
    fCreationDate = new Date();
  }

  /**
   * Default constructor for deserialization
   */
  protected Mark() {
  // As per javadoc
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#getLastVisitDate()
   */
  public synchronized Date getLastVisitDate() {
    return fLastVisitDate;
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#setLastVisitDate(java.util.Date)
   */
  public synchronized void setLastVisitDate(Date lastVisitDate) {
    fLastVisitDate = lastVisitDate;
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#getPopularity()
   */
  public synchronized int getPopularity() {
    return fPopularity;
  }

  /*
   * @see org.rssowl.core.model.types.IFeed#setPopularity(int)
   */
  public synchronized void setPopularity(int popularity) {
    fPopularity = popularity;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#getCreationDate()
   */
  public synchronized Date getCreationDate() {
    return fCreationDate;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#setCreationDate(java.util.Date)
   */
  public synchronized void setCreationDate(Date creationDate) {
    fCreationDate = creationDate;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#getName()
   */
  public synchronized String getName() {
    return fName;
  }

  /*
   * @see org.rssowl.core.model.types.IMark#setName(java.lang.String)
   */
  public synchronized void setName(String name) {
    Assert.isNotNull(name, "The type Mark requires a Name that is not NULL"); //$NON-NLS-1$
    fName = name;
  }

  public synchronized IFolder getParent() {
    return fParent;
  }

  public synchronized void setParent(IFolder parent) {
    Assert.isNotNull(parent, "parent");
    fParent = parent;
  }

  protected static List<INews> getNews(List<NewsReference> newsRefs, boolean ignoreNullResolve) {
    List<INews> news = new ArrayList<INews>(newsRefs.size());

    for (NewsReference newsRef : newsRefs) {
      INews newsItem = newsRef.resolve();

      if (!ignoreNullResolve)
        Assert.isNotNull(newsItem, "newsItem");

      if (newsItem != null)
        news.add(newsItem);
    }
    return news;
  }

  /**
   * @return a uncached reference to the parent folder.
   */
  protected FolderReference getFolderReference() {
    return getParent() == null ? null : new FolderReference(getParent().getId());
  }

  @Override
  @SuppressWarnings("nls")
  public synchronized String toString() {
    return super.toString() + "Name = " + fName + ", ";
  }

  /**
   * Returns a String describing the state of this Entity.
   *
   * @return A String describing the state of this Entity.
   */
  @SuppressWarnings("nls")
  public synchronized String toLongString() {
    String retValue = super.toString() + "Name = " + fName + ", Creation Date = " + fCreationDate + ", Popularity: " + getPopularity();
    if (getLastVisitDate() != null)
      retValue = retValue + (DateFormat.getDateTimeInstance().format(getLastVisitDate()));

    return retValue + ", Belongs to Folder = " + fParent.getId() + ", ";
  }
}