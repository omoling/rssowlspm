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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.util.DateUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Sorts the elements of the feed view based on the choices provided by the
 * user.
 *
 * @author Ismael Juma (ismael@juma.me.uk)
 * @author bpasero
 */
public class NewsComparator extends ViewerComparator implements Comparator<INews> {
  private NewsTableControl.Columns fSortBy;
  private boolean fAscending;

  /**
   * @return Returns the ascending.
   */
  public boolean isAscending() {
    return fAscending;
  }

  /**
   * @param ascending The ascending to set.
   */
  public void setAscending(boolean ascending) {
    fAscending = ascending;
  }

  /**
   * @return Returns the sortBy.
   */
  public NewsTableControl.Columns getSortBy() {
    return fSortBy;
  }

  /**
   * @param sortBy The sortBy to set.
   */
  public void setSortBy(NewsTableControl.Columns sortBy) {
    fSortBy = sortBy;
  }

  /*
   * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer,
   * java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {

    /* Can only be an EntityGroup then */
    if (!(e1 instanceof INews) || !(e2 instanceof INews))
      return 0;

    /* Proceed comparing News */
    return compare((INews) e1, (INews) e2);
  }

  /*
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  public int compare(INews news1, INews news2) {
    int result = 0;

    /* Sort by Date */
    if (fSortBy == NewsTableControl.Columns.DATE)
      return compareByDate(news1, news2, false);

    /* Sort by Title */
    else if (fSortBy == NewsTableControl.Columns.TITLE)
      result = compareByTitle(ModelUtils.getHeadline(news1), ModelUtils.getHeadline(news2));

    /* Sort by Author */
    else if (fSortBy == NewsTableControl.Columns.AUTHOR)
      result = compareByAuthor(news1.getAuthor(), news2.getAuthor());

    /* Sort by Category */
    else if (fSortBy == NewsTableControl.Columns.CATEGORY)
      result = compareByCategory(news1.getCategories(), news2.getCategories());

    /* Sort by Stickyness */
    else if (fSortBy == NewsTableControl.Columns.STICKY)
      result = compareByStickyness(news1.isFlagged(), news2.isFlagged());

    /* Sort by Feed */
    else if (fSortBy == NewsTableControl.Columns.FEED)
      result = compareByFeed(news1.getFeedLinkAsText(), news2.getFeedLinkAsText());

    /* Sort by Score */
    else if (fSortBy == NewsTableControl.Columns.RATING)
        result = compareByScore(news1.getRating(), news2.getRating());

    /* Fall Back to default sort if result is 0 */
    if (result == 0)
      result = compareByDate(news1, news2, true);

    return result;
  }

  private int compareByScore(int rating1, int rating2) {
	  int result = Integer.valueOf(rating1).compareTo(Integer.valueOf(rating2));
	  
	  /* Respect ascending / descending Order */
	  return fAscending ? result : result * -1;
}

private int compareByFeed(String feedLink1, String feedLink2) {
    int result = feedLink1.compareTo(feedLink2);

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByDate(INews news1, INews news2, boolean forceDescending) {
    int result = 0;

    Date date1 = DateUtils.getRecentDate(news1);
    Date date2 = DateUtils.getRecentDate(news2);

    result = date1.compareTo(date2);

    /* Respect ascending / descending Order */
    return fAscending && !forceDescending ? result : result * -1;
  }

  private int compareByTitle(String title1, String title2) {
    int result = compareByString(title1, title2);

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByAuthor(IPerson author1, IPerson author2) {
    int result = 0;

    if (author1 != null && author2 != null) {
      String value1 = author1.getName();
      if (value1 == null && author1.getEmail() != null)
        value1 = author1.getEmail().toString();
      else if (value1 == null && author1.getUri() != null)
        value1 = author1.getUri().toString();

      String value2 = author2.getName();
      if (value2 == null && author2.getEmail() != null)
        value2 = author2.getEmail().toString();
      else if (value2 == null && author2.getUri() != null)
        value2 = author2.getUri().toString();

      result = compareByString(value1, value2);
    }

    else if (author1 != null)
      result = -1;

    else if (author2 != null)
      result = 1;

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByCategory(List<ICategory> categories1, List<ICategory> categories2) {
    int result = 0;

    if (categories1 != null && categories1.size() > 0 && categories2 != null && categories2.size() > 0) {
      ICategory category1 = categories1.get(0);
      ICategory category2 = categories2.get(0);

      String value1 = category1.getName();
      if (value1 == null)
        value1 = category1.getDomain();

      String value2 = category2.getName();
      if (value2 == null)
        value2 = category2.getName();

      result = compareByString(value1, value2);
    }

    else if (categories1 != null && categories1.size() > 0)
      result = -1;

    else if (categories2 != null && categories2.size() > 0)
      result = 1;

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByStickyness(boolean sticky1, boolean sticky2) {
    int result = 0;

    if (sticky1 && !sticky2)
      result = 1;

    else if (!sticky1 && sticky2)
      result = -1;

    /* Respect ascending / descending Order */
    return fAscending ? result : result * -1;
  }

  private int compareByString(String str1, String str2) {
    if (str1 != null && str2 != null)
      return str1.compareTo(str2);
    else if (str1 != null)
      return -1;

    return 1;
  }
}