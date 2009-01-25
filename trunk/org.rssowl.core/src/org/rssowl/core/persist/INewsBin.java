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

import org.eclipse.core.runtime.Assert;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;

import java.util.Collection;
import java.util.Set;


public interface INewsBin extends INewsMark {

  public static final class StatesUpdateInfo  {
    private final INews.State fOldState;
    private final INews.State fNewState;
    private final NewsReference fNewsReference;
    public StatesUpdateInfo(State oldState, State newState, NewsReference newsReference) {
      Assert.isNotNull(newState, "newState");
      this.fOldState = oldState;
      this.fNewState = newState;
      this.fNewsReference = newsReference;
    }
    public INews.State getOldState() {
      return fOldState;
    }
    public INews.State getNewState() {
      return fNewState;
    }
    public NewsReference getNewsReference() {
      return fNewsReference;
    }

  }

  void addNews(INews news);

  boolean updateNewsStates(Collection<StatesUpdateInfo> statesUpdateInfos);

  void removeNews(INews news);

  Collection<NewsReference> removeNews(Set<INews.State> states);

  NewsBinReference toReference();
}
