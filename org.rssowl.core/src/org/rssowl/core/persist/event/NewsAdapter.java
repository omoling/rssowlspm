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

package org.rssowl.core.persist.event;

import java.util.Set;

/**
 * Provides an empty implementation of <code>NewsListener</code>. Useful if
 * the client only needs to implement a subset of the interface.
 * 
 * @author bpasero
 */
public class NewsAdapter implements NewsListener {

  /*
   * @see org.rssowl.core.model.events.NewsListener#entitiesAdded(java.util.List)
   */
  public void entitiesAdded(Set<NewsEvent> events) { }

  /*
   * @see org.rssowl.core.model.events.NewsListener#newsDeleted(java.util.List)
   */
  public void entitiesDeleted(Set<NewsEvent> events) { }

  /*
   * @see org.rssowl.core.model.events.NewsListener#newsUpdated(java.util.List)
   */
  public void entitiesUpdated(Set<NewsEvent> events) { }

}