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

package org.rssowl.core.tests.controller;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsCounterDAO;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FeedReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.ui.internal.Controller;

import java.net.URI;

/**
 * This TestCase covers use-cases for the Controller (network only).
 *
 * @author bpasero
 */
public class ControllerTestNetwork {

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
  }

  /**
   * Reload a Feed.
   *
   * @throws Exception
   */
  @Test
  public void testReloadFeed() throws Exception {
    IFeed feed = new Feed(new URI("http://www.rssowl.org/rssowl2dg/tests/manager/rss_2_0.xml")); //$NON-NLS-1$
    feed = DynamicDAO.save(feed);
    Controller.getDefault().reload(createBookMark(feed), null, new NullProgressMonitor());

    assertEquals(new FeedReference(feed.getId()).resolve().getFormat(), "RSS 2.0"); //$NON-NLS-1$
  }

  /**
   * Reload a BookMark.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testReloadBookMark() throws Exception {
    IFeed feed = new Feed(new URI("http://www.rssowl.org/rssowl2dg/tests/manager/rss_2_0.xml"));
    feed = DynamicDAO.save(feed);

    IFolder folder = Owl.getModelFactory().createFolder(null, null, "Folder");
    folder = DynamicDAO.save(folder);
    IBookMark bookmark = Owl.getModelFactory().createBookMark(1L, folder, new FeedLinkReference(feed.getLink()), "BookMark");

    Controller.getDefault().reload(bookmark, null, new NullProgressMonitor());

    assertEquals(new FeedReference(feed.getId()).resolve().getFormat(), "RSS 2.0"); //$NON-NLS-1$
  }

  /**
   * Reload a BookMark that points to an unavailable Feed.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testReloadBookMarkWithError() throws Exception {
    IFeed feed = new Feed(new URI("http://www.rssowl.org/rssowl2dg/tests/not_existing.xml"));
    feed = DynamicDAO.save(feed);

    IFolder folder = Owl.getModelFactory().createFolder(null, null, "Folder");
    folder = DynamicDAO.save(folder);
    IBookMark bookmark = Owl.getModelFactory().createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "BookMark");

    Controller.getDefault().reload(bookmark, null, new NullProgressMonitor());

    assertEquals(true, new BookMarkReference(bookmark.getId()).resolve().isErrorLoading());
  }

  private NewsCounter loadNewsCounter() {
    return DynamicDAO.getDAO(INewsCounterDAO.class).load();
  }

  private int getNewCount(IFeed feed) {
    return loadNewsCounter().getNewCount(feed.getLink());
  }

  private int getUnreadCount(IFeed feed) {
    return loadNewsCounter().getUnreadCount(feed.getLink());
  }

  private int getStickyCount(IFeed feed) {
    return loadNewsCounter().getStickyCount(feed.getLink());
  }

  /**
   * Test the News-Service with Reload.
   *
   * @throws Exception
   */
  @SuppressWarnings("nls")
  @Test
  public void testNewsServiceWithReloadBookMark() throws Exception {
    IFeed feed = new Feed(new URI("http://www.rssowl.org/rssowl2dg/tests/manager/rss_2_0.xml"));
    feed = DynamicDAO.save(feed);

    IFolder folder = Owl.getModelFactory().createFolder(null, null, "Folder");
    folder = DynamicDAO.save(folder);
    IBookMark bookmark = Owl.getModelFactory().createBookMark(1L, folder, new FeedLinkReference(feed.getLink()), "BookMark");

    Controller.getDefault().reload(bookmark, null, new NullProgressMonitor());

    feed.getNews().get(0).setFlagged(true);
    DynamicDAO.save(feed);

    int unreadCounter = getUnreadCount(feed);
    int newCounter = getNewCount(feed);
    int stickyCounter = getStickyCount(feed);

    Controller.getDefault().reload(bookmark, null, new NullProgressMonitor());

    assertEquals(unreadCounter, getUnreadCount(feed));
    assertEquals(newCounter, getNewCount(feed));
    assertEquals(stickyCounter, getStickyCount(feed));
  }

  private IBookMark createBookMark(IFeed feed) throws PersistenceException {
    IFolder folder = DynamicDAO.save(Owl.getModelFactory().createFolder(null, null, "Root"));

    return DynamicDAO.save(Owl.getModelFactory().createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "BookMark"));
  }
}