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

import org.rssowl.core.internal.persist.NewsBin;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.dao.INewsBinDAO;
import org.rssowl.core.persist.event.NewsBinEvent;
import org.rssowl.core.persist.event.NewsBinListener;

public class NewsBinDaoImpl extends AbstractEntityDAO<INewsBin, NewsBinListener, NewsBinEvent> implements INewsBinDAO {

  public NewsBinDaoImpl() {
    super(NewsBin.class, true);
  }

  @Override
  protected NewsBinEvent createSaveEventTemplate(INewsBin entity) {
    return new NewsBinEvent(entity, null, true);
  }

  @Override
  protected NewsBinEvent createDeleteEventTemplate(INewsBin entity) {
    return createSaveEventTemplate(entity);
  }
}
