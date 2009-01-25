package org.rssowl.core.tests.service;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.service.StatisticsService;
import org.rssowl.core.tests.util.DummyObjectUtilities;

public class StatisticsServiceTest {
	StatisticsService statisticService;	
	
	@Before
	public void setUp() throws Exception {
		//reset the DB
		Owl.getPersistenceService().recreateSchema();
		
		statisticService = new StatisticsService();
	}

	@After
	public void tearDown() throws Exception {
		statisticService = null;
	}
	
	/**
	 * Verifies the correct read percentage on the total amount of available bookmarks and their
	 * associated news-items
	 * @throws URISyntaxException
	 */
	@Test
	public void testComputeReadPercentage() throws URISyntaxException {
		final int numberOfBookmarks = 5;
		final int numberOfNewsPerBookmark = 5;
		
		List<IBookMark> bookmarkList = DummyObjectUtilities.createTestBookmarkArray(numberOfBookmarks);
		assertNotNull(bookmarkList);
		assertEquals("should have some elements", numberOfBookmarks, bookmarkList.size());
		
		//attach dummy news items to each bookmark
		for (IBookMark bookMark : bookmarkList) {
			IFeed feed = new Feed(bookMark.getId(), bookMark.getFeedLinkReference().getLink());
			
			DummyObjectUtilities.attachNewsItems(feed, numberOfNewsPerBookmark);
			DynamicDAO.save(feed);
		}
		
		//test: each bookmark should have "numberOfNewsPerBookmark" news associated
		for (IBookMark bookMark : bookmarkList) {
			IFeed retrievedBookmarkFeed = bookMark.getFeedLinkReference().resolve();
			assertNotNull("bookmark shouldn't be null", retrievedBookmarkFeed);
			assertEquals("should have <numberofNewsPerBookmark> newsitems", numberOfNewsPerBookmark, retrievedBookmarkFeed.getNews().size());
		}
		DynamicDAO.saveAll(bookmarkList);
		
		
		//lower bound test: no news have been read, so percentage should be 0%
		double readPercentage = statisticService.computeReadPercentage();
		assertEquals("should be 0%", 0.0, readPercentage);
		
		//set some news to read state
		DummyObjectUtilities.setNewsItemsRead(bookmarkList.get(0).getNews(), 2);
		assertEquals("1st bookmark should have 2 read items", 2 ,bookmarkList.get(0).getNewsCount(EnumSet.of(INews.State.READ)));
		
		DummyObjectUtilities.setNewsItemsRead(bookmarkList.get(3).getNews(), 3);
		assertEquals("4th bookmark should have 3 read items", 3 ,bookmarkList.get(3).getNewsCount(EnumSet.of(INews.State.READ)));
		DynamicDAO.saveAll(bookmarkList);
		
		//test for correct percentage
		readPercentage = statisticService.computeReadPercentage();
		assertEquals("should be 20%", 20.0, readPercentage);
		
		//set all news items to read
		for (IBookMark bookMark : bookmarkList) {
			//set all of the bookmark's news items to read
			DummyObjectUtilities.setNewsItemsRead(bookMark.getNews(), bookMark.getNews().size());
		}
		DynamicDAO.saveAll(bookmarkList);
		
		//upper bound test: now the read percentage should be 100%
		readPercentage = statisticService.computeReadPercentage();
		assertEquals("should be 100%", 100.0, readPercentage);
	}
}
