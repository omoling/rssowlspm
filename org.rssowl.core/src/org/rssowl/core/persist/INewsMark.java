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
package org.rssowl.core.persist;

import org.rssowl.core.persist.reference.NewsReference;

import java.util.List;
import java.util.Set;

public interface INewsMark extends IMark    {
  boolean isGetNewsRefsEfficient();

  boolean containsNews(INews news);

  List<INews> getNews();

  List<INews> getNews(Set<INews.State> states);

  /**
   * Returns a List of all news contained in this INewsMark. To reduce the
   * memory impact of this method, the news are returned as
   * <code>NewsReference</code>.
   *
   * @return Returns a List of all news contained in this INewsMark. To reduce
   * the memory impact of this method, the news are returned as
   * <code>NewsReference</code>.
   */
  List<NewsReference> getNewsRefs();

  /**
   * Returns a List of all news contained in this INewsMark. To reduce the
   * memory impact of this method, the news are returned as
   * <code>NewsReference</code>.
   *
   * @param states A Set (typically an EnumSet) of <code>INews.State</code>
   * that the resulting news must have.
   * @return Returns a List of all news contained in this INewsMark. To reduce
   * the memory impact of this method, the news are returned as
   * <code>NewsReference</code>.
   */
  List<NewsReference> getNewsRefs(Set<INews.State> states);

  /**
   * Returns the number of news that contained in this INewsMark in the
   * provided <code>INews.State</code>s.
   *
   * @param states A Set (typically an EnumSet) of <code>INews.State</code>
   * of the INews that should be included in the count.
   * @return the number of news that contained in this INewsMark in the
   * provided <code>INews.State</code>s.
   */
  int getNewsCount(Set<INews.State> states);
}
