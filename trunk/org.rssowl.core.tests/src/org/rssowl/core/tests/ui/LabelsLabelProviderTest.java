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
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.tests.util.DummyObjectUtilities;
import org.rssowl.ui.internal.views.label.LabelsLabelProvider;


public class LabelsLabelProviderTest {
	private LabelsLabelProvider labelProvider;
	
	@Before
	public void setUp() throws Exception {
		//reset the DB
		Owl.getPersistenceService().recreateSchema();
		
		labelProvider = new LabelsLabelProvider();
	}

	@After
	public void tearDown() throws Exception {
		labelProvider = null;
	}
	
	/**
	 *	tests whether the labels are correctly shown
	 * @throws URISyntaxException 
	 */
	@Test
	public void testGetColumnText() throws URISyntaxException {
		assertNotNull(labelProvider);
		
		ILabel label = DummyObjectUtilities.createTestLabel();
		DynamicDAO.save(label);
		
		assertNotNull(label);
		assertNotNull(label.getSearchMark());
	
		List<IBookMark> bookMarks = DummyObjectUtilities.createTestBookmarkArrayWithNews(2);
		
		//lower bound: initially now news item have been labeled and set to sticky
		assertEquals("string shown by label provider should be the same as the label name", label.getName(), labelProvider.getColumnText(label, 0));
	
		//one news item has been labeled and marked as sticky
		INews news = bookMarks.get(0).getNews().get(0);
		DummyObjectUtilities.addLabelToNewsSetSticky(news, label);
		
		DynamicDAO.save(label);
		
		assertEquals("string shown by label provider should be the same as the label name followed by 1 as sticky-news count", label.getName() + " (1)", labelProvider.getColumnText(label, 0));
		
		//upper bound: all news items have been labeled and set as sticky
		int totalNumberOfNews = 0;
		for (IBookMark bookMark : bookMarks) {
			for (INews newsItem : bookMark.getNews()) {
				totalNumberOfNews++;
				DummyObjectUtilities.addLabelToNewsSetSticky(newsItem, label);
			}
		}
		
		assertEquals("string shown by label provider should be the same as the label name followed by the total number of news", label.getName() + " (" + totalNumberOfNews + ")", labelProvider.getColumnText(label, 0));	
	}
}	
