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

import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.dao.INewsBinDAO;
import org.rssowl.core.persist.event.NewsBinEvent;
import org.rssowl.core.persist.event.NewsBinListener;

import java.util.Set;

public final class CachingNewsBinDAO extends CachingDAO<NewsBinDaoImpl, INewsBin, NewsBinListener, NewsBinEvent> implements INewsBinDAO {

  public CachingNewsBinDAO() {
    super(new NewsBinDaoImpl());
  }

  @Override
  protected NewsBinListener createEntityListener() {
    return new NewsBinListener() {
      public void entitiesAdded(Set<NewsBinEvent> events) {
        for (NewsBinEvent event : events)
          getCache().put(event.getEntity().getId(), event.getEntity());
      }

      public void entitiesDeleted(Set<NewsBinEvent> events) {
        for (NewsBinEvent event : events)
          getCache().remove(event.getEntity().getId(), event.getEntity());
      }

      public void entitiesUpdated(Set<NewsBinEvent> events) {
        /* No action needed */
      }
    };
  }


}
