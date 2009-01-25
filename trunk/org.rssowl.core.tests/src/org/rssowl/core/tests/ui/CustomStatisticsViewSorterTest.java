package org.rssowl.core.tests.ui;


import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.tests.util.DummyObjectUtilities;
import org.rssowl.ui.internal.views.statistics.CustomStatisticsViewSorter;

public class CustomStatisticsViewSorterTest {
	private CustomStatisticsViewSorter<IBookMark> bookMarkSorter;
	
	@Before
	public void setUp() throws Exception {
		//reset DB schema
		Owl.getPersistenceService().recreateSchema();
		
		bookMarkSorter = new CustomStatisticsViewSorter<IBookMark>();
	}

	@After
	public void tearDown() throws Exception {
		bookMarkSorter = null;
	}
	
	/**
	 * tests for the correct ordering by title
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectBookMarkTitleOrder() throws URISyntaxException{
		IBookMark bookMark1 = DummyObjectUtilities.createDummyBookMark(1, "Microsoft News");
		IBookMark bookMark2 = DummyObjectUtilities.createDummyBookMark(2, "Official Google Blog");
		
		//set the sorting type
		bookMarkSorter.setType(CustomStatisticsViewSorter.Type.SORT_BY_TITLE);
		assertEquals("The sorting type should be SORT_BY_TITLE", CustomStatisticsViewSorter.Type.SORT_BY_TITLE, bookMarkSorter.getType());
		
		assertEquals("bookmark1 compared to bookmark1 should be equal", 0, bookMarkSorter.compare(bookMark1, bookMark1));
		assertEquals("bookmark1 should be before bookmark2", -1, bookMarkSorter.compare(bookMark1, bookMark2));
		assertEquals("bookmark2 should be after bookmark1", 1, bookMarkSorter.compare(bookMark2, bookMark1));
	}
	
	/**
	 * tests for the correct ordering by popularity
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectBookMarkPopularityOrder() throws URISyntaxException{
		IBookMark bookMark1 = DummyObjectUtilities.createDummyBookMark(1, "Microsoft News");
		IBookMark bookMark2 = DummyObjectUtilities.createDummyBookMark(2, "Official Google Blog");
		
		//set the popularity
		bookMark1.setPopularity(50);
		bookMark2.setPopularity(10);
		
		bookMark1.getName().compareTo(bookMark2.getName());
		
		//set the sorting type
		bookMarkSorter.setType(CustomStatisticsViewSorter.Type.SORT_BY_POPULARITY);
		assertEquals("The sorting type should be SORT_BY_POPULARITY", CustomStatisticsViewSorter.Type.SORT_BY_POPULARITY, bookMarkSorter.getType());
		
		assertEquals("bookmark1 compared to bookmark1 should be equal", 0, bookMarkSorter.compare(bookMark1, bookMark1));
		assertEquals("bookmark1 should be before bookmark2", -1, bookMarkSorter.compare(bookMark1, bookMark2));
		assertEquals("bookmark2 should be after bookmark1", 1, bookMarkSorter.compare(bookMark2, bookMark1));
	}
	
	/**
	 * tests for the correct ordering by last-access date
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectBookMarkLastAccessOrder() throws URISyntaxException, ParseException{
		IBookMark bookMark1 = DummyObjectUtilities.createDummyBookMark(1, "Microsoft News");
		IBookMark bookMark2 = DummyObjectUtilities.createDummyBookMark(2, "Official Google Blog");
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		Date date1 = dateFormat.parse("01.12.2008");
		Date date2 = dateFormat.parse("10.12.2008");
		
		bookMark1.setLastVisitDate(date1);
		bookMark2.setLastVisitDate(date2);
		
		//set the sorting type
		bookMarkSorter.setType(CustomStatisticsViewSorter.Type.SORT_BY_LASTACCESS);
		assertEquals("The sorting type should be SORT_BY_LASTACCESS", CustomStatisticsViewSorter.Type.SORT_BY_LASTACCESS, bookMarkSorter.getType());
		
		assertEquals("bookmark1 compared to bookmark1 should be equal", 0, bookMarkSorter.compare(bookMark1, bookMark1));
		assertEquals("bookmark1 should be after bookmark2", 1, bookMarkSorter.compare(bookMark1, bookMark2));
		assertEquals("bookmark2 should be before bookmark1", -1, bookMarkSorter.compare(bookMark2, bookMark1));
	}

	/**
	 *	tests for the correct ordering by read percentage
	 * @throws URISyntaxException 
	 */
	@Test
	public void testCorrectBookMarkReadPercentageOrder() throws URISyntaxException{
		IBookMark bookMark1 = DummyObjectUtilities.createDummyBookMark(1, "Microsoft News");
		IFeed feed1 = new Feed(bookMark1.getId(), bookMark1.getFeedLinkReference().getLink());
		IBookMark bookMark2 = DummyObjectUtilities.createDummyBookMark(2, "Official Google Blog");
		IFeed feed2 = new Feed(bookMark2.getId(), bookMark2.getFeedLinkReference().getLink());
		
		DummyObjectUtilities.attachNewsItems(feed1, 2);
		DummyObjectUtilities.attachNewsItems(feed2, 2);
		
		assertEquals("read percentage should be 0", 0, bookMark1.getReadPercentage());
		assertEquals("read percentage should be 0", 0, bookMark2.getReadPercentage());
		
		//set all feeds of bookmark1 to read
		for (INews newsItem : feed1.getNews()) {
			newsItem.setState(State.READ);
		}
		
		//bookmark1 should have read percentage 100
		assertEquals("read percentage should be 100", 100, bookMark1.getReadPercentage());
		
		//check ordering
		//set the sorting type
		bookMarkSorter.setType(CustomStatisticsViewSorter.Type.SORT_BY_PERCENTAGE);
		assertEquals("The sorting type should be SORT_BY_PERCENTAGE", CustomStatisticsViewSorter.Type.SORT_BY_PERCENTAGE, bookMarkSorter.getType());
		
		assertEquals("bookmark1 compared to bookmark1 should be equal", 0, bookMarkSorter.compare(bookMark1, bookMark1));
		assertEquals("bookmark1 should be after bookmark2", -1, bookMarkSorter.compare(bookMark1, bookMark2));
		assertEquals("bookmark2 should be before bookmark1", 1, bookMarkSorter.compare(bookMark2, bookMark1));
	}
	
	/**
	 * tests for the correct ordering by last-update date
	 * 
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectBookMarkLastUpdateOrder() throws URISyntaxException, ParseException{
		IBookMark bookMark1 = DummyObjectUtilities.createDummyBookMark(1, "Microsoft News");
		IBookMark bookMark2 = DummyObjectUtilities.createDummyBookMark(2, "Official Google Blog");
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		Date date1 = dateFormat.parse("01.12.2008");
		Date date2 = dateFormat.parse("10.12.2008");
		
		//set the sorting type
		bookMarkSorter.setType(CustomStatisticsViewSorter.Type.SORT_BY_LASTUPDATE);
		assertEquals("The sorting type should be SORT_BY_LASTUPDATE", CustomStatisticsViewSorter.Type.SORT_BY_LASTUPDATE, bookMarkSorter.getType());
		
		//set same date to both bookmarks
		bookMark1.setMostRecentNewsDate(date1);
		bookMark2.setMostRecentNewsDate(date1);
		
		assertEquals("bookmarks should have same date", 0, bookMarkSorter.compare(bookMark1, bookMark2));
		
		//set bookmark 2 to other date
		bookMark2.setMostRecentNewsDate(date2);
		
		assertEquals("bookmark1 should be before bookmark2", 1, bookMarkSorter.compare(bookMark1, bookMark2));
		assertEquals("bookmark2 should be after bookmark1", -1, bookMarkSorter.compare(bookMark2, bookMark1));
	}
}
