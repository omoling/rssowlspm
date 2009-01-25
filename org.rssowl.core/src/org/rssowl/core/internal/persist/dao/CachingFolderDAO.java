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

import org.rssowl.core.internal.persist.service.DatabaseEvent;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.FolderListener;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.ReparentInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingFolderDAO extends CachingDAO<FolderDAOImpl, IFolder, FolderListener, FolderEvent> implements IFolderDAO {

  /* Dummy value to associate with an Object in the maps */
  private static final Object PRESENT = new Object();
  private final ConcurrentMap<IFolder, Object> fRootFolders;

  public CachingFolderDAO() {
    super(new FolderDAOImpl());
    fRootFolders = new ConcurrentHashMap<IFolder, Object>(4, 0.75f, 1);
  }

  @Override
  protected void onDatabaseClosed(DatabaseEvent event) {
    super.onDatabaseClosed(event);
    fRootFolders.clear();
  }

  @Override
  protected void onDatabaseOpened(DatabaseEvent event) {
    super.onDatabaseOpened(event);
    for (IFolder folder : getDAO().loadRoots())
      fRootFolders.put(folder, PRESENT);
  }

  @Override
  protected FolderListener createEntityListener() {
    return new FolderListener() {
      public void entitiesAdded(Set<FolderEvent> events) {
        for (FolderEvent folderEvent : events) {
          IFolder folder = folderEvent.getEntity();
          getCache().put(folder.getId(), folder);
          if (folder.getParent() == null)
            fRootFolders.put(folder, PRESENT);
        }
      }

      public void entitiesDeleted(Set<FolderEvent> events) {
        for (FolderEvent folderEvent : events) {
          IFolder folder = folderEvent.getEntity();
          getCache().remove(folder.getId(), folder);
          if (folder.getParent() == null)
            fRootFolders.remove(folder);
        }
      }

      public void entitiesUpdated(Set<FolderEvent> events) {
        for (FolderEvent folderEvent : events) {
          if (folderEvent.getOldParent() != null) {
            IFolder folder = folderEvent.getEntity();
            if (folder.getParent() == null)
              fRootFolders.put(folder, PRESENT);
          }
        }
      }
    };
  }

  public Collection<IFolder> loadRoots() throws PersistenceException {
    return Collections.unmodifiableSet(fRootFolders.keySet());
  }

  public void reparent(List<ReparentInfo<IFolderChild, IFolder>> reparentInfos) throws PersistenceException {
    getDAO().reparent(reparentInfos);
  }

}
