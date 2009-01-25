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

package org.rssowl.core.tests.persist.service;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.service.Counter;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.internal.persist.service.DBManager;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.query.Query;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests defragmentation of db.
 */
public class DefragmentTest {

  private URL fPluginLocation;
  private IModelFactory fFactory = Owl.getModelFactory();

  /**
   *
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();
    fPluginLocation = FileLocator.toFileURL(Platform.getBundle("org.rssowl.core.tests").getEntry("/"));

    ILabel label = fFactory.createLabel(null, "Label 0");
    DynamicDAO.save(label);

    List<IFeed> feeds = saveFeeds();
    IFeed feed = feeds.get(1);
    INews news = feed.getNews().get(2);
    news.addLabel(label);
    DynamicDAO.save(news);
    INews anotherNews = feeds.get(feeds.size() - 1).getNews().get(0);
    anotherNews.addLabel(label);
    DynamicDAO.save(anotherNews);

    IFolder folder = fFactory.createFolder(null, null, "Folder");
    fFactory.createBookMark(null, folder, new FeedLinkReference(feed.getLink()), "BookMark");
    fFactory.createSearchMark(null, folder, "SM");
    INewsBin bin = fFactory.createNewsBin(null, folder, "NewsBin");
    DynamicDAO.save(folder);
    INews newsCopy = fFactory.createNews(news, bin);
    DynamicDAO.save(newsCopy);
    DynamicDAO.save(bin);
    Preference pref = new Preference("key");
    pref.putLongs(2, 3, 4);
    DynamicDAO.save(pref);
  }

  /**
   * Tests defragment.
   */
  @Test
  public void testDefragment() {
	String dbPath = DBManager.getDBFilePath();
	File originDbFile = new File(dbPath + ".origin");
    DBHelper.copyFile(new File(dbPath), originDbFile);
    File defragmentedDbFile = new File(dbPath + ".dest");
    DBManager.copyDatabase(originDbFile, defragmentedDbFile,
        new NullProgressMonitor());

    System.gc();
    ObjectContainer db = Db4o.openFile(DBManager.createConfiguration(), originDbFile.getAbsolutePath());
    ObjectContainer defragmentedDb = Db4o.openFile(DBManager.createConfiguration(),
        defragmentedDbFile.getAbsolutePath());

    List<IEntity> entities = db.query(IEntity.class);
    assertEquals(entities.size(), defragmentedDb.query(IEntity.class).size());
    for (IEntity entity : entities) {
      Query query = defragmentedDb.query();
      query.constrain(entity.getClass());
      query.descend("fId").constrain(Long.valueOf(entity.getId())); //$NON-NLS-1$
      List<?> result = query.execute();
      assertEquals(1, result.size());
      assertEquals(entity, result.get(0));
    }

    List<INews> newsList = db.query(INews.class);
    assertEquals(newsList.size(), defragmentedDb.query(INews.class).size());
    for (INews news : newsList) {
      Query query = defragmentedDb.query();
      query.constrain(news.getClass());
      query.descend("fId").constrain(Long.valueOf(news.getId())); //$NON-NLS-1$
      @SuppressWarnings("unchecked")
      List<INews> result = query.execute();
      assertEquals(1, result.size());
      assertEquals(news.getTitle(), result.get(0).getTitle());
    }

    List<Description> descriptions = db.query(Description.class);
    assertEquals(descriptions.size(), defragmentedDb.query(Description.class).size());
    for (Description description : descriptions) {
      Query query = defragmentedDb.query();
      query.constrain(description.getClass());
      query.descend("fNewsId").constrain(Long.valueOf(description.getNews().getId())); //$NON-NLS-1$
      @SuppressWarnings("unchecked")
      List<Description> result = query.execute();
      assertEquals(1, result.size());
      assertEquals(description.getValue(), result.get(0).getValue());
    }

    List<INewsBin> newsBins = db.query(INewsBin.class);
    assertEquals(newsBins.size(), defragmentedDb.query(INewsBin.class).size());
    for (INewsBin newsBin : newsBins) {
      Query query = defragmentedDb.query();
      query.constrain(newsBin.getClass());
      query.descend("fId").constrain(Long.valueOf(newsBin.getId())); //$NON-NLS-1$
      @SuppressWarnings("unchecked")
      List<INewsBin> result = query.execute();
      assertEquals(1, result.size());
      assertEquals(newsBin.getNews(), result.get(0).getNews());
    }

    assertEquals(db.query(ILabel.class).size(), defragmentedDb.query(ILabel.class).size());
    assertEquals(db.query(Counter.class).get(0).getValue(), defragmentedDb.query(Counter.class).get(0).getValue());
  }

  private List<IFeed> saveFeeds() throws Exception {
    List<IFeed> feeds = new ArrayList<IFeed>();
    for (int i = 1; i < 40; i++) {
      URI feedLink = new URI(fPluginLocation.toExternalForm().replace(" ", "%20")).resolve("data/performance/" + i + ".xml").toURL().toURI();
      IFeed feed = new Feed(feedLink);

      InputStream inS = new BufferedInputStream(new FileInputStream(new File(feed.getLink())));
      Owl.getInterpreter().interpret(inS, feed);

      feeds.add(feed);
    }
    DynamicDAO.saveAll(feeds);
    return feeds;
  }
}
