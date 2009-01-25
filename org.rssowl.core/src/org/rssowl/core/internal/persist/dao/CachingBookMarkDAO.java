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

package org.rssowl.core.internal.persist.dao;

import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.dao.IBookMarkDAO;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.BookMarkListener;
import org.rssowl.core.persist.reference.FeedLinkReference;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class CachingBookMarkDAO extends CachingDAO<BookMarkDAOImpl, IBookMark, BookMarkListener, BookMarkEvent> implements IBookMarkDAO {

  public CachingBookMarkDAO() {
    super(new BookMarkDAOImpl());
  }

  @Override
  protected BookMarkListener createEntityListener() {
    return new BookMarkListener() {
      public void entitiesAdded(Set<BookMarkEvent> events) {
        for (BookMarkEvent event : events)
          getCache().put(event.getEntity().getId(), event.getEntity());

      }

      public void entitiesDeleted(Set<BookMarkEvent> events) {
        for (BookMarkEvent event : events)
          getCache().remove(event.getEntity().getId(), event.getEntity());

      }

      public void entitiesUpdated(Set<BookMarkEvent> events) {
      /* No action needed */
      }
    };
  }

  public Collection<IBookMark> loadAll(FeedLinkReference feedRef) {
    //TODO Check if this is faster than the db query
    Set<IBookMark> marks = new HashSet<IBookMark>(1);
    for (IBookMark mark : getCache().values()) {
      if (mark.getFeedLinkReference().equals(feedRef))
        marks.add(mark);
    }
    return marks;
  }

}