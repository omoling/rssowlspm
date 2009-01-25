package org.rssowl.core.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.ui.internal.editors.feed.NewsComparator;
import org.rssowl.ui.internal.editors.feed.NewsTableControl;


public class NewsComparatorTest {
	private NewsComparator newsComparator = null;
	private INews newsItem1 = null;
	private INews newsItem2 = null;
	
	@Before
	public void setUp() throws Exception {
		newsComparator = new NewsComparator();
		IFeed feed = new Feed(Long.parseLong("" + 0), new URI("http://www.dummyurl.com"));
		newsItem1 = new News(feed);
		newsItem2 = new News(feed);
	}

	@After
	public void tearDown() throws Exception {
		newsComparator = null;
		newsItem1 = null;
		newsItem2 = null;
	}
	
	/**
	 * Tests for the correct ordering by rating
	 */
	@Test
	public void testCompareByScore(){
		assertNotNull("newsComparator should not be null", newsComparator);

		//Verify that the created newsItems have a rating of 0
		assertEquals("rating should be equal to 0", 0, newsItem1.getRating());
		assertEquals("rating should be equal to 0", 0, newsItem2.getRating());
		
		// set SORT_BY_RATING
		newsComparator.setSortBy(NewsTableControl.Columns.RATING);
		assertEquals(newsComparator.getSortBy(),
				NewsTableControl.Columns.RATING);
	
		assertEquals(newsComparator.compare(newsItem1, newsItem2), 0);
		
		//set rating of newsItem1 to 5
		newsItem1.setRating(5);
		assertEquals("rating should be equal to 0", 5, newsItem1.getRating());
		
		//set sort direction to ascending
		newsComparator.setAscending(true);
		assertEquals("isAscending should return true", newsComparator.isAscending(), true);
		
		assertEquals(newsComparator.compare(newsItem1, newsItem2), 1);
		assertEquals(newsComparator.compare(newsItem2, newsItem1), -1);
		
		//set sort direction to descending
		newsComparator.setAscending(false);
		assertEquals("isAscending should return false", newsComparator.isAscending(), false);
		
		assertEquals(newsComparator.compare(newsItem1, newsItem2), -1);
		assertEquals(newsComparator.compare(newsItem2, newsItem1), 1);
	}

}
