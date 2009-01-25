package org.rssowl.core.tests.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.ui.internal.actions.RateAction;

public class RateActionTest {
	private RateAction action = null;
	private INews newsItem = null;
	private IStructuredSelection selection = null;
	
	@Before
	public void setUp() throws Exception {
		//reset DB schema
		Owl.getPersistenceService().recreateSchema();
		
		//construct the necessary selection
		IFeed feed = new Feed(Long.parseLong("" + 0), new URI("http://www.dummyurl.com"));
		newsItem = new News(feed);
		selection = new StructuredSelection(newsItem);
	}

	@After
	public void tearDown() throws Exception {
		selection = null;
		newsItem = null;
		action = null;
	}
	
	/**
	 * Tests the actions core parts
	 */
	@Test
	public void testAction(){
		assertNotNull(newsItem);

		//Verify that the created newsItem has a rating of 0
		assertEquals("rating should be equal to 0", 0, newsItem.getRating());

		//boundary test: empty selection -> nothing should happen, no exception etc..
		action = new RateAction(3, new StructuredSelection());
		action.run();
		
		//start the Action and set some rating
		action = new RateAction(3, selection);
		action.run();
		assertEquals("rating should be equal to 3", 3, newsItem.getRating());
		
		//decrease the rating again
		action = new RateAction(1, selection);
		action.run();
		assertEquals("rating should be equal to 1", 1, newsItem.getRating());
	}
	
}
