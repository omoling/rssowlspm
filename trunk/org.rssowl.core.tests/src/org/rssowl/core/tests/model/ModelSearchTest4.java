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

package org.rssowl.core.tests.model;

import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.tests.TestUtils;
import org.rssowl.core.util.SearchHit;

import java.net.URI;
import java.util.List;

/**
 * Test searching types from the persistence layer.
 *
 * @author bpasero
 */
public class ModelSearchTest4 extends AbstractModelSearchTest {

  /**
   * @throws Exception
   */
  @Test
  public void testPhraseSearch_CONTAINS() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      /* Title */
      INews news1 = createNews(feed, "Johnny lives hungry Depp", "http://www.news.com/news1.html", State.NEW);

      /* Description */
      INews news2 = createNews(feed, "News2", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer name like Michael Jackson.");

      /* Author */
      INews news3 = createNews(feed, "News3", "http://www.news.com/news3.html", State.NEW);
      IPerson author = fFactory.createPerson(null, news3);
      author.setName("Arnold Schwarzenegger");

      /* Category */
      INews news4 = createNews(feed, "lives", "http://www.news.com/news4.html", State.NEW);
      ICategory category = fFactory.createCategory(null, news4);
      category.setName("Roberts");

      /* Attachment Content */
      INews news5 = createNews(feed, "hungry", "http://www.news.com/news5.html", State.NEW);
      IAttachment attachment = fFactory.createAttachment(null, news5);
      attachment.setLink(new URI("http://www.attachment.com/att1news2.file"));
      attachment.setType("Hasselhoff");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), false);
        assertSame(result, news1, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), true);
        assertTrue(result.isEmpty());
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news4, news5);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPhraseSearch_CONTAINS_ALL() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      /* Title */
      INews news1 = createNews(feed, "Johnny lives hungry Depp", "http://www.news.com/news1.html", State.NEW);

      /* Description */
      INews news2 = createNews(feed, "News2", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer name like Michael Jackson.");

      /* Author */
      INews news3 = createNews(feed, "News3", "http://www.news.com/news3.html", State.NEW);
      IPerson author = fFactory.createPerson(null, news3);
      author.setName("Arnold Schwarzenegger");

      /* Category */
      INews news4 = createNews(feed, "lives", "http://www.news.com/news4.html", State.NEW);
      ICategory category = fFactory.createCategory(null, news4);
      category.setName("Roberts");

      /* Attachment Content */
      INews news5 = createNews(feed, "hungry", "http://www.news.com/news5.html", State.NEW);
      IAttachment attachment = fFactory.createAttachment(null, news5);
      attachment.setLink(new URI("http://www.attachment.com/att1news2.file"));
      attachment.setType("Hasselhoff");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), false);
        assertSame(result, news1, news2);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), true);
        assertTrue(result.isEmpty());
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "\"lives hungry\" lives hung");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertTrue(result.isEmpty());
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPhraseSearch_CONTAINS_NOT() throws Exception {
    try {

      /* First add some Types */
      IFeed feed = fFactory.createFeed(null, new URI("http://www.feed.com/feed.xml"));

      /* Title */
      INews news1 = createNews(feed, "Johnny lives hungry Depp", "http://www.news.com/news1.html", State.NEW);

      /* Description */
      INews news2 = createNews(feed, "News2", "http://www.news.com/news2.html", State.NEW);
      news2.setDescription("This is a longer name like Michael Jackson.");

      /* Author */
      INews news3 = createNews(feed, "News3", "http://www.news.com/news3.html", State.NEW);
      IPerson author = fFactory.createPerson(null, news3);
      author.setName("Arnold Schwarzenegger");

      /* Category */
      INews news4 = createNews(feed, "lives", "http://www.news.com/news4.html", State.NEW);
      ICategory category = fFactory.createCategory(null, news4);
      category.setName("Roberts");

      /* Attachment Content */
      INews news5 = createNews(feed, "hungry", "http://www.news.com/news5.html", State.NEW);
      IAttachment attachment = fFactory.createAttachment(null, news5);
      attachment.setLink(new URI("http://www.attachment.com/att1news2.file"));
      attachment.setType("Hasselhoff");

      DynamicDAO.save(feed);

      /* Wait for Indexer */
      waitForIndexer();

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.DESCRIPTION, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news1, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), false);
        assertSame(result, news1, news2, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition1 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"longer name like\"");
        ISearchCondition condition2 = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"Johnny lives hungry Depp\"");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition1, condition2), true);
        assertSame(result, news3, news4, news5);
      }

      {
        ISearchField field = fFactory.createSearchField(INews.TITLE, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);
      }

      {
        ISearchField field = fFactory.createSearchField(IEntity.ALL_FIELDS, fNewsEntityName);
        ISearchCondition condition = fFactory.createSearchCondition(field, SearchSpecifier.CONTAINS_NOT, "\"lives hungry\" lives hungry");

        List<SearchHit<NewsReference>> result = fModelSearch.searchNews(list(condition), false);
        assertSame(result, news2, news3);
      }
    } catch (PersistenceException e) {
      TestUtils.fail(e);
    }
  }
}