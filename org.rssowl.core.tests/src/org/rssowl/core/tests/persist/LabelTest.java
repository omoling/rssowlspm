package org.rssowl.core.tests.persist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.tests.util.DummyObjectUtilities;

public class LabelTest {
	private ILabel label = null;
	
	@Before
	public void setUp() throws Exception {
		//reset the DB
		Owl.getPersistenceService().recreateSchema();
		
		label = DummyObjectUtilities.createTestLabel();
	}

	@After
	public void tearDown() throws Exception {
		label = null;
	}
	
	/**
	 * Tests whether the correct retrieving of news items belonging to a label
	 * @throws NumberFormatException
	 * @throws URISyntaxException
	 */
	@Test
	public void testFindNewsWithLabel() throws NumberFormatException, URISyntaxException{
		assertNotNull("Label should not be null", label);
		
		IFeed feed = new Feed(Long.parseLong("" + 0), new URI("http://www.dummyurl.com"));
		assertNotNull(feed);
		
		DummyObjectUtilities.attachNewsItems(feed, 10);
		
		//lower bound: label added to NO news items
		assertEquals("No news items should have the label added to", 0, label.findNewsWithLabel().size()); 
		
		//label added to 5 news items
		for(int i = 0; i < 5; i++){
			DummyObjectUtilities.addLabelToNewsSetSticky(feed.getNews().get(i), label);
		}
		assertEquals("size of news items with label added to them should be 5", 5, label.findNewsWithLabel().size());
		
		//upper bound: label added to all news items
		for(INews news : feed.getNews()){
			DummyObjectUtilities.addLabelToNewsSetSticky(news, label);
		}
		assertEquals("size of news items found should be size of news items of feed", feed.getNews().size(), label.findNewsWithLabel().size());
	}
	
	/**
	 * Tests the correct counting of news items marked as sticky and belonging to a label
	 * @throws NumberFormatException
	 * @throws URISyntaxException
	 */
	@Test
	public void testGetStickyNewsCount() throws NumberFormatException, URISyntaxException{
		assertNotNull("Label should not be null", label);
		
		IFeed feed = new Feed(Long.parseLong("" + 0), new URI("http://www.dummyurl.com"));
		assertNotNull(feed);
		
		DummyObjectUtilities.attachNewsItems(feed, 10);
		
		//lower bound: no items of label are marked as sticky
		assertEquals("No sticky news items have label added to", 0, label.getStickyNewsCount()); 
		
		
		//label added to 5 news items, all set sticky
		for(int i = 0; i < 5; i++){
			DummyObjectUtilities.addLabelToNewsSetSticky(feed.getNews().get(i), label);
		}
		assertEquals("news items belonging to label and being marked as sticky should be 5", 5, label.getStickyNewsCount());
		
		//two news items NOT belonging to label are set as sticky, count does not change
		for(int i = 5; i < 7; i++){
			feed.getNews().get(i).setFlagged(true);
		}
		DynamicDAO.save(feed);
		assertEquals("news items belonging to label and being marked as sticky should be 5", 5, label.getStickyNewsCount());
		
		//adding previous news items to label and other two, without marking the latter two as sticky
		for(int i = 5; i < 9; i++){
			DummyObjectUtilities.addLabelToNews(feed.getNews().get(i), label);
		}
		assertEquals("news items belonging to label and being markes as sticky should be 7", 7, label.getStickyNewsCount());
		
		
		//upper bound: label added to all news items, all set sticky
		for(INews news : feed.getNews()){
			DummyObjectUtilities.addLabelToNewsSetSticky(news, label);
		}
		assertEquals("size of news items found should be size of news items of feed", feed.getNews().size(), label.getStickyNewsCount());
	}
	
}
