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

package org.rssowl.core.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;

import java.net.URI;

/**
 * Testing the <code>RSSOwlUI</code> facade.
 *
 * @author bpasero
 */
public class MiscUITests {

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFavicon() throws Exception {

    /* Delete previously stored favicons */
    for (int i = 0; i < 5; i++)
      OwlUI.deleteImage(i);

    IFeed feed = new Feed(new URI("http://www.rssowl.org/node/feed"));
    IFolder root = new Folder(null, null, "Root");
    IBookMark bookmark = new BookMark(null, root, new FeedLinkReference(feed.getLink()), "Bookmark");
    root.addMark(bookmark, null, false);

    feed = DynamicDAO.save(feed);
    DynamicDAO.save(root);

    assertEquals(null, OwlUI.getFavicon(bookmark));

    Controller.getDefault().reload(bookmark, null, new NullProgressMonitor());

    assertNotNull(OwlUI.getFavicon(bookmark));

    DynamicDAO.delete(bookmark);

    assertEquals(null, OwlUI.getFavicon(bookmark));
  }
}