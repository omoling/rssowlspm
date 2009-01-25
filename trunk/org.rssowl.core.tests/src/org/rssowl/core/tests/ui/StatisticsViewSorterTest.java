package org.rssowl.core.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.tests.util.DummyObjectUtilities;
import org.rssowl.ui.internal.views.statistics.StatisticsViewSorter;

public class StatisticsViewSorterTest {
	private StatisticsViewSorter viewSorter;

	@Before
	public void setUp() throws Exception {
		//reset DB schema
		Owl.getPersistenceService().recreateSchema();
		
		viewSorter = new StatisticsViewSorter();
	}

	@After
	public void tearDown() throws Exception {
		viewSorter = null;
	}

	/**
	 * tests for the correct ordering by title
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectBookMarkTitleOrder() throws URISyntaxException {
		List<IBookMark> list = DummyObjectUtilities.createTestBookmarkArray(2);

		assertNotNull("viewSorter should not be null", viewSorter);
		assertEquals(list.size(), 2);

		// set SORT_BY_TITLE
		viewSorter.setType(StatisticsViewSorter.Type.SORT_BY_TITLE);
		assertEquals(viewSorter.getType(),
				StatisticsViewSorter.Type.SORT_BY_TITLE);

		assertEquals(viewSorter.compare(null, list.get(0), list.get(1)), -1);
		assertEquals(viewSorter.compare(null, list.get(1), list.get(0)), 1);
		assertEquals(viewSorter.compare(null, list.get(1), list.get(1)), 0);
	}

	/**
	 * tests for the correct ordering by title
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectBookMarkPopularityOrder() throws URISyntaxException {
		List<IBookMark> list = DummyObjectUtilities.createTestBookmarkArray(2);

		assertNotNull("viewSorter should not be null", viewSorter);
		assertEquals(list.size(), 2);

		// set SORT_BY_POPULARITY
		viewSorter.setType(StatisticsViewSorter.Type.SORT_BY_POPULARITY);
		assertEquals(viewSorter.getType(),
				StatisticsViewSorter.Type.SORT_BY_POPULARITY);

		assertEquals(viewSorter.compare(null, list.get(0), list.get(1)), 1);
		assertEquals(viewSorter.compare(null, list.get(1), list.get(0)), -1);
		assertEquals(viewSorter.compare(null, list.get(1), list.get(1)), 0);
	}

	/**
	 * tests for the correct ordering by last-access date
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectBookMarkLastAccessOrder() throws URISyntaxException {
		List<IBookMark> list = DummyObjectUtilities.createTestBookmarkArray(2);

		assertNotNull("viewSorter should not be null", viewSorter);
		assertEquals(list.size(), 2);

		// set SORT_BY_LASTACCESS
		viewSorter.setType(StatisticsViewSorter.Type.SORT_BY_LASTACCESS);
		assertEquals(viewSorter.getType(),
				StatisticsViewSorter.Type.SORT_BY_LASTACCESS);

		assertEquals(viewSorter.compare(null, list.get(0), list.get(1)), -1);
		assertEquals(viewSorter.compare(null, list.get(1), list.get(0)), 1);
		assertEquals(viewSorter.compare(null, list.get(1), list.get(1)), 0);
	}
	
	/**
	 *	tests for the correct ordering by read percentage
	 * @throws URISyntaxException 
	 */
	@Test
	public void testCorrectBookMarkReadPercentageOrder() throws URISyntaxException{
		int amount = 2;
		List<IBookMark> list = DummyObjectUtilities.createTestBookmarkArrayWithNews(amount);
		
		assertNotNull("viewSorter should not be null", viewSorter);
		assertEquals(list.size(), amount);
		
		//set SORT_BY_PERCENTAGE
		viewSorter.setType(StatisticsViewSorter.Type.SORT_BY_PERCENTAGE);
		assertEquals(viewSorter.getType(), StatisticsViewSorter.Type.SORT_BY_PERCENTAGE);
		
		//create bookMark1 and feed1
		IBookMark bookMark1 = (IBookMark)list.get(0);
		assertNotNull(bookMark1);
		IFeed feed1 = bookMark1.getFeedLinkReference().resolve();
		assertNotNull(feed1);
		
		//create bookMark2 and feed2
		IBookMark bookMark2 = (IBookMark)list.get(1);
		assertNotNull(bookMark2);
		IFeed feed2 = bookMark2.getFeedLinkReference().resolve();
		assertNotNull(feed2);
		
		//assert # of elements
		assertEquals(1, feed1.getNews().size());
		assertEquals(2, feed2.getNews().size());
		
		assertEquals("order should be equal", 0, viewSorter.compare(null, bookMark1, bookMark2));
		
		//set 1 news item of bookmark2 to read
		DummyObjectUtilities.setNewsItemsRead(bookMark2.getNews(), 1);
		
		//oder has changed
		assertEquals(1, viewSorter.compare(null, bookMark1, bookMark2));
		
		//set only news item of bookmark1 to read so that read-percentage is 100%
		DummyObjectUtilities.setNewsItemsRead(bookMark1.getNews(), 1);
		assertEquals(-1, viewSorter.compare(null, bookMark1, bookMark2));
		
		//set all news items of bookMark2 to read to that read-percentage is 100%
		DummyObjectUtilities.setNewsItemsRead(bookMark2.getNews(), bookMark2.getNews().size());
		assertEquals(0, viewSorter.compare(null, bookMark1, bookMark2));
	}
}
