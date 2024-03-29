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

import org.rssowl.core.internal.persist.Description;

import com.db4o.query.Query;

public class DescriptionDAOImpl extends AbstractPersistableDAO<Description> implements IDescriptionDAO {

  public DescriptionDAOImpl() {
    super(Description.class, true);
  }

  public Description load(long newsId) {
    Query query = fDb.query();
    query.constrain(Description.class);
    query.descend("fNewsId").constrain(newsId);
    Description description = getSingleResult(query);
    fDb.activate(description, Integer.MAX_VALUE);
    return description;
  }

  public String loadValue(long newsId) {
    Description description = load(newsId);
    return description == null ? null : description.getValue();
  }

  @Override
  protected void preCommit() {
    // Do nothing
  }
}
