package org.rssowl.core.tests.ui;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.tests.util.DummyObjectUtilities;
import org.rssowl.ui.internal.views.statistics.StatisticsContentProvider;

public class StatisticsContentProviderTest {
	private IStructuredContentProvider contentProvider = null;

	@Before
	public void setUp() throws Exception {
		//reset the DB
		Owl.getPersistenceService().recreateSchema();
		
		contentProvider = new StatisticsContentProvider();
	}

	@After
	public void tearDown() throws Exception {
		contentProvider = null;
	}
	
	/**
	 * Tests the correct number of feeds displayed on the StatisticsView
	 * @throws URISyntaxException
	 */
	@Test
	public void testGetElements() throws URISyntaxException{
		List<IBookMark> list = DummyObjectUtilities.createTestBookmarkArray(5);
		
		DynamicDAO.saveAll(list);
		
		assertNotNull("ContentProvider shouldn't be null", contentProvider);
		Object[] returnedList = contentProvider.getElements(list);
		assertNotNull("elements from ContentProvider shouldn't be null", returnedList);
		
		//check the length
		assertEquals("Length of returned elements should be correct", list.size(), returnedList.length);
		
		list.add(DummyObjectUtilities.createDummyBookMark(10, "Bookmark 10"));
		DynamicDAO.saveAll(list);
		
		assertEquals("Length of returned element should be equal", list.size(), contentProvider.getElements(list).length);
	}
	
	/**
	 * Tests the percentage calculation
	 * @throws URISyntaxException
	 */
	@Test
	public void testCorrectReadPercentage() throws URISyntaxException{
		int numberOfNewsItems = 10;
		IBookMark bookMark = DummyObjectUtilities.createDummyBookMark(1, "TestBookMark");
		IFeed feed = new Feed(bookMark.getId(), bookMark.getFeedLinkReference().getLink());
		
		//attach news items to the feed
		DummyObjectUtilities.attachNewsItems(feed, numberOfNewsItems);
		
		//test lower boundary
		assertEquals("Read percentage should be 0", 0, bookMark.getReadPercentage());
		
		//retrieve the feed and check the number of items
		IFeed retrievedFeed = bookMark.getFeedLinkReference().resolve();
		assertEquals("The number of news should be equal", numberOfNewsItems, retrievedFeed.getNews().size());
		
		//set 3 news items to read status
		for(int i=0; i<numberOfNewsItems; i++){
			INews newsItem = feed.getNews().get(i);
			if(i<3)
				newsItem.setState(State.READ);
		}
		
		DynamicDAO.save(retrievedFeed);
		
		//the read percentage should now be on 30%
		assertEquals("Read percentage should be 30%", 30, bookMark.getReadPercentage());
		
		for(int i=0; i<numberOfNewsItems; i++){
			INews newsItem = feed.getNews().get(i);
			newsItem.setState(State.READ);
		}
		
		DynamicDAO.save(retrievedFeed);
		
		//test upper boundary
		assertEquals("Read percentage should be 100%", 100, bookMark.getReadPercentage());
	}

}
