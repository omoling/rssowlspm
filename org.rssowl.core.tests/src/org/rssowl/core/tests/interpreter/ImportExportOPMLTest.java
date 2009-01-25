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

package org.rssowl.core.tests.interpreter;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.DateUtils;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.actions.ExportFeedsAction;
import org.rssowl.ui.internal.util.ModelUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests a full export and import using OPML including Folders, Bookmarks, Saved
 * Searches and News Bins. For saved searches most combinations of Field,
 * Specifier and Value are used to make sure everything works as expected.
 *
 * @author bpasero
 */
public class ImportExportOPMLTest {
  private ExportFeedsAction fExportAction;
  private File fTmpFile;
  private Controller fController;
  private IModelFactory fFactory;
  private IFolder fDefaultSet;
  private IFolder fCustomSet;
  private IFolder fDefaultFolder1;
  private IBookMark fBookMark1;
  private IFolder fDefaultFolder2;
  private IFolder fCustomFolder2;
  private INewsBin fNewsBin;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Owl.getPersistenceService().recreateSchema();

    fFactory = Owl.getModelFactory();
    fController = Controller.getDefault();
    fExportAction = new ExportFeedsAction();
    fTmpFile = File.createTempFile("rssowl", "opml");
    fTmpFile.deleteOnExit();

    /* Fill Defaults */
    fillDefaults();
    DynamicDAO.getDAO(IFolderDAO.class).save(fDefaultSet);
    DynamicDAO.getDAO(IFolderDAO.class).save(fCustomSet);

    /* Export */
    Set<IFolder> rootFolders = new HashSet<IFolder>();
    rootFolders.add(fDefaultSet);
    rootFolders.add(fCustomSet);
    fExportAction.exportToOPML(fTmpFile, rootFolders);

    /* Clear */
    Owl.getPersistenceService().recreateSchema();

    /* Add Default Set */
    DynamicDAO.getDAO(IFolderDAO.class).save(fFactory.createFolder(null, null, "Default"));
  }

  private void fillDefaults() throws URISyntaxException {

    /* Set: Default */
    fillDefaultSet();

    /* Set: Custom */
    fillCustomSet();

    DynamicDAO.getDAO(IFolderDAO.class).save(fDefaultSet);
    DynamicDAO.getDAO(IFolderDAO.class).save(fCustomSet);

    /* Default > List of SearchMarks */
    fillSearchMarks(fDefaultSet);

    /* Default > Folder 2 > List of SearchMarks */
    fillSearchMarks(fDefaultFolder2);

    /* Custom > List of SearchMarks */
    fillSearchMarks(fCustomSet);

    /* Custom > Folder 2 > List of SearchMarks */
    fillSearchMarks(fCustomFolder2);
  }

  private void fillDefaultSet() throws URISyntaxException {
    fDefaultSet = fFactory.createFolder(null, null, "Default");

    fDefaultFolder1 = fFactory.createFolder(null, fDefaultSet, "Default Folder 1");

    fDefaultFolder2 = fFactory.createFolder(null, fDefaultSet, "Default Folder 2");

    /* Default > BookMark 1 */
    IFeed feed1 = fFactory.createFeed(null, new URI("feed1"));
    fBookMark1 = fFactory.createBookMark(null, fDefaultSet, new FeedLinkReference(feed1.getLink()), "Bookmark 1");

    /* Default > Folder 1 > BookMark 3 */
    IFeed feed3 = fFactory.createFeed(null, new URI("feed3"));
    fFactory.createBookMark(null, fDefaultFolder1, new FeedLinkReference(feed3.getLink()), "Bookmark 3");

    /* Default > News Bin 1 */
    fNewsBin = fFactory.createNewsBin(null, fDefaultSet, "Bin 1");
  }

  private void fillCustomSet() throws URISyntaxException {
    fCustomSet = fFactory.createFolder(null, null, "Custom");

    /* Custom > Folder 1 */
    IFolder folder1 = fFactory.createFolder(null, fCustomSet, "Custom Folder 1");

    fCustomFolder2 = fFactory.createFolder(null, fCustomSet, "Custom Folder 2");

    /* Custom > BookMark 2 */
    IFeed feed2 = fFactory.createFeed(null, new URI("feed2"));
    fFactory.createBookMark(null, fCustomSet, new FeedLinkReference(feed2.getLink()), "Bookmark 2");

    /* Custom > Folder 1 > BookMark 4 */
    IFeed feed4 = fFactory.createFeed(null, new URI("feed4"));
    fFactory.createBookMark(null, folder1, new FeedLinkReference(feed4.getLink()), "Bookmark 4");

  }

  private void fillSearchMarks(IFolder parent) {
    String newsName = INews.class.getName();

    /* 1) State IS *new* */
    {
      ISearchField field = fFactory.createSearchField(INews.STATE, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.NEW));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 2) State IS *new* *unread* *updated* */
    {
      ISearchField field = fFactory.createSearchField(INews.STATE, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 3) Entire News CONTAINS foo?bar */
    {
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo?bar");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 4) Age in Days is > 5 */
    {
      ISearchField field = fFactory.createSearchField(INews.AGE_IN_DAYS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_GREATER_THAN, 5);

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 5) Publish Date is 26.12.1981 */
    {
      Calendar cal = DateUtils.getToday();
      cal.set(Calendar.YEAR, 1981);
      cal.set(Calendar.MONTH, Calendar.DECEMBER);
      cal.set(Calendar.DATE, 26);

      ISearchField field = fFactory.createSearchField(INews.PUBLISH_DATE, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, cal.getTime());

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 6) Feed Links is not http://www.rssowl.org/node/feed */
    {
      ISearchField field = fFactory.createSearchField(INews.FEED, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS_NOT, "http://www.rssowl.org/node/feed");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 7) Has Attachments is TRUE */
    {
      ISearchField field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, true);

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /*
     * 8) Entire News CONTAINS foo?bar AND State IS *new* AND Has Attachments is
     * TRUE
     */
    {
      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");

      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "foo?bar");
      searchmark.addSearchCondition(condition);

      field = fFactory.createSearchField(INews.HAS_ATTACHMENTS, newsName);
      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, true);
      searchmark.addSearchCondition(condition);

      field = fFactory.createSearchField(INews.STATE, newsName);
      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, EnumSet.of(State.NEW));
      searchmark.addSearchCondition(condition);

      searchmark.setMatchAllConditions(true);
    }

    /* 9) Location is Default Set */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultSet })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 10) Location is Default Set OR Location is Custom Set */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultSet, fCustomSet })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 11) Location is Folder 1 */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultFolder1 })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 12) Location is BookMark 1 */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fBookMark1 })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /*
     * 13) Location is Default Set OR Location is Custom Set OR Location is
     * BookMark1
     */
    {
      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");

      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);

      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fDefaultSet })));
      searchmark.addSearchCondition(condition);

      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fCustomSet })));
      searchmark.addSearchCondition(condition);

      condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fBookMark1 })));
      searchmark.addSearchCondition(condition);
    }

    /* 14) Location is Bin 1 */
    {
      ISearchField field = fFactory.createSearchField(INews.LOCATION, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.IS, ModelUtils.toPrimitive(Arrays.asList(new IFolderChild[] { fNewsBin })));

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 15) Entire News CONTAINS_ALL foo?bar */
    {
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "foo?bar");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }

    /* 16) Entire News CONTAINS_NOT foo?bar */
    {
      ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, newsName);
      ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "foo?bar");

      ISearchMark searchmark = fFactory.createSearchMark(null, parent, "Search");
      searchmark.addSearchCondition(condition);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings( { "nls", "null" })
  public void testExportImportCompleteOPML() throws Exception {

    /* Import */
    fController.importFeeds(fTmpFile.getAbsolutePath());

    /* Validate */
    Collection<IFolder> rootFolders = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();

    assertEquals(2, rootFolders.size());

    IFolder defaultSet = null;
    IFolder customSet = null;
    for (IFolder rootFolder : rootFolders) {
      if (rootFolder.getName().equals("Default"))
        defaultSet = rootFolder;
      else if (rootFolder.getName().equals("Custom"))
        customSet = rootFolder;
    }

    assertNotNull(defaultSet);
    assertNotNull(customSet);

    List<IFolder> defaultFolders = defaultSet.getFolders();
    assertEquals(2, defaultFolders.size());

    IFolder defaultFolder1 = null;
    IFolder defaultFolder2 = null;

    for (IFolder defaultFolder : defaultFolders) {
      if (defaultFolder.getName().equals("Default Folder 1"))
        defaultFolder1 = defaultFolder;
      else if (defaultFolder.getName().equals("Default Folder 2"))
        defaultFolder2 = defaultFolder;
    }

    assertNotNull(defaultFolder1);
    assertNotNull(defaultFolder2);

    List<IFolder> customFolders = customSet.getFolders();
    assertEquals(2, customFolders.size());

    IFolder customFolder1 = null;
    IFolder customFolder2 = null;

    for (IFolder customFolder : customFolders) {
      if (customFolder.getName().equals("Custom Folder 1"))
        customFolder1 = customFolder;
      else if (customFolder.getName().equals("Custom Folder 2"))
        customFolder2 = customFolder;
    }

    assertNotNull(customFolder1);
    assertNotNull(customFolder2);

    List<IMark> defaultMarks = defaultSet.getMarks();
    assertEquals(18, defaultMarks.size());

    IBookMark bookmark1 = null;
    for (IMark mark : defaultMarks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 1"))
        bookmark1 = (IBookMark) mark;
    }

    assertNotNull(bookmark1);
    assertEquals("feed1", bookmark1.getFeedLinkReference().getLink().toString());

    INewsBin bin = null;
    for (IMark mark : defaultMarks) {
      if (mark instanceof INewsBin && mark.getName().equals("Bin 1"))
        bin = (INewsBin) mark;
    }

    assertNotNull(bin);

    List<IMark> customMarks = customSet.getMarks();
    assertEquals(17, customMarks.size());

    IBookMark bookmark2 = null;
    for (IMark mark : customMarks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 2"))
        bookmark2 = (IBookMark) mark;
    }

    assertNotNull(bookmark2);
    assertEquals("feed2", bookmark2.getFeedLinkReference().getLink().toString());

    List<IMark> marks = defaultFolder1.getMarks();
    assertEquals(1, marks.size());

    IBookMark bookmark3 = null;
    for (IMark mark : marks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 3"))
        bookmark3 = (IBookMark) mark;
    }

    assertNotNull(bookmark3);
    assertEquals("feed3", bookmark3.getFeedLinkReference().getLink().toString());

    marks = customFolder1.getMarks();
    assertEquals(1, marks.size());

    IBookMark bookmark4 = null;
    for (IMark mark : marks) {
      if (mark instanceof IBookMark && mark.getName().equals("Bookmark 4"))
        bookmark4 = (IBookMark) mark;
    }

    assertNotNull(bookmark4);
    assertEquals("feed4", bookmark4.getFeedLinkReference().getLink().toString());

    assertSearchMarks(defaultSet);
    assertSearchMarks(customSet);
    assertSearchMarks(defaultFolder2);
    assertSearchMarks(customFolder2);
  }

  private void assertSearchMarks(IFolder folder) {
    List<IMark> marks = folder.getMarks();
    List<ISearchMark> searchmarks = new ArrayList<ISearchMark>();
    for (IMark mark : marks) {
      if (mark instanceof ISearchMark)
        searchmarks.add((ISearchMark) mark);
    }

    /* 1) State IS *new* */
    ISearchMark searchmark = searchmarks.get(0);
    assertEquals("Search", searchmark.getName());
    List<ISearchCondition> conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.STATE, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(EnumSet.of(INews.State.NEW), conditions.get(0).getValue());

    /* 2) State IS *new* *unread* *updated* */
    searchmark = searchmarks.get(1);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.STATE, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED), conditions.get(0).getValue());

    /* 3) Entire News CONTAINS foo?bar */
    searchmark = searchmarks.get(2);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(IEntity.ALL_FIELDS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.CONTAINS, conditions.get(0).getSpecifier());
    assertEquals("foo?bar", conditions.get(0).getValue());

    /* 4) Age in Days is > 5 */
    searchmark = searchmarks.get(3);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.AGE_IN_DAYS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS_GREATER_THAN, conditions.get(0).getSpecifier());
    assertEquals(5, conditions.get(0).getValue());

    /* 5) Publish Date is 26.12.1981 */
    Calendar cal = DateUtils.getToday();
    cal.set(Calendar.YEAR, 1981);
    cal.set(Calendar.MONTH, Calendar.DECEMBER);
    cal.set(Calendar.DATE, 26);

    searchmark = searchmarks.get(4);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.PUBLISH_DATE, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(cal.getTime(), conditions.get(0).getValue());

    /* 6) Feed Links is not http://www.rssowl.org/node/feed */
    searchmark = searchmarks.get(5);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.FEED, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS_NOT, conditions.get(0).getSpecifier());
    assertEquals("http://www.rssowl.org/node/feed", conditions.get(0).getValue());

    /* 7) Has Attachments is TRUE */
    searchmark = searchmarks.get(6);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.HAS_ATTACHMENTS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(true, conditions.get(0).getValue());

    /*
     * 8) Entire News CONTAINS foo?bar AND State IS *new* AND Has Attachments is
     * TRUE
     */
    searchmark = searchmarks.get(7);
    conditions = searchmark.getSearchConditions();
    assertEquals(3, conditions.size());
    assertEquals(true, searchmark.matchAllConditions());

    for (ISearchCondition condition : conditions) {
      switch (condition.getField().getId()) {
        case IEntity.ALL_FIELDS:
          assertEquals(SearchSpecifier.CONTAINS, condition.getSpecifier());
          assertEquals("foo?bar", condition.getValue());
          break;

        case INews.STATE:
          assertEquals(SearchSpecifier.IS, condition.getSpecifier());
          assertEquals(EnumSet.of(INews.State.NEW), condition.getValue());
          break;

        case INews.HAS_ATTACHMENTS:
          assertEquals(SearchSpecifier.IS, condition.getSpecifier());
          assertEquals(true, condition.getValue());
          break;

        default:
          fail();
      }
    }

    /* 9) Location is Default Set */
    searchmark = searchmarks.get(8);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(Arrays.asList(new IFolderChild[] { fDefaultSet }), ModelUtils.toEntities((Long[][]) conditions.get(0).getValue()));

    /* 10) Location is Default Set OR Location is Custom Set */
    searchmark = searchmarks.get(9);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    List<IFolderChild> locations = ModelUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    assertEquals(2, locations.size());
    assertContains("Default", locations);
    assertContains("Custom", locations);

    /* 11) Location is Folder 1 */
    searchmark = searchmarks.get(10);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    locations = ModelUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(1, locations.size());
    assertEquals(true, locations.get(0) instanceof IFolder);
    assertEquals("Default Folder 1", locations.get(0).getName());

    /* 12) Location is BookMark 1 */
    searchmark = searchmarks.get(11);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    locations = ModelUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(1, locations.size());
    assertEquals(true, locations.get(0) instanceof IBookMark);
    assertEquals("Bookmark 1", locations.get(0).getName());

    /*
     * 13) Location is Default Set OR Location is Custom Set OR Location is
     * BookMark1
     */
    searchmark = searchmarks.get(12);
    conditions = searchmark.getSearchConditions();
    assertEquals(3, conditions.size());

    locations = new ArrayList<IFolderChild>();

    for (ISearchCondition condition : conditions) {
      assertEquals(INews.LOCATION, condition.getField().getId());
      assertEquals(SearchSpecifier.IS, condition.getSpecifier());

      locations.addAll(ModelUtils.toEntities((Long[][]) condition.getValue()));
    }

    assertEquals(3, locations.size());
    assertContains("Default", locations);
    assertContains("Custom", locations);
    assertContains("Bookmark 1", locations);

    /* 14) Location is Bin 1 */
    searchmark = searchmarks.get(13);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(INews.LOCATION, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.IS, conditions.get(0).getSpecifier());
    locations = ModelUtils.toEntities((Long[][]) conditions.get(0).getValue());
    assertEquals(1, locations.size());
    assertEquals(true, locations.get(0) instanceof INewsBin);
    assertEquals(fNewsBin.getName(), locations.get(0).getName());

    /* 15) Entire News CONTAINS_ALL foo?bar */
    searchmark = searchmarks.get(14);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(IEntity.ALL_FIELDS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.CONTAINS_ALL, conditions.get(0).getSpecifier());
    assertEquals("foo?bar", conditions.get(0).getValue());

    /* 16) Entire News CONTAINS_NOT foo?bar */
    searchmark = searchmarks.get(15);
    conditions = searchmark.getSearchConditions();
    assertEquals(1, conditions.size());
    assertEquals(IEntity.ALL_FIELDS, conditions.get(0).getField().getId());
    assertEquals(SearchSpecifier.CONTAINS_NOT, conditions.get(0).getSpecifier());
    assertEquals("foo?bar", conditions.get(0).getValue());
  }

  private void assertContains(String name, List<IFolderChild> childs) {
    boolean found = false;
    for (IFolderChild child : childs) {
      if (child.getName().equals(name)) {
        found = true;
        break;
      }
    }

    assertEquals(true, found);
  }
}