package org.rssowl.core.tests.ui;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.tests.util.DummyObjectUtilities;
import org.rssowl.ui.internal.views.label.LabelContentProvider;

public class LabelContentProviderTest {
	private LabelContentProvider contentProvider;
	
	
	@Before
	public void setUp() throws Exception {
		//reset the DB
		Owl.getPersistenceService().recreateSchema();
		
		contentProvider = new LabelContentProvider();
		contentProvider.setCreateNewFolder();
	}

	@After
	public void tearDown() throws Exception {
		contentProvider = null;
	}
	
	/**
	 *	tests for the correct amount of labels 
	 */
	@Test
	public void testCorrectLabelAmount(){
		List<ILabel> dummyLabels = DummyObjectUtilities.createTestLabelArray(5);
		DynamicDAO.saveAll(dummyLabels);
		
		assertNotNull(contentProvider);
		assertEquals("elements returned from ContentProvider should match local array", dummyLabels.size(), contentProvider.getElements(null).length);
		
		dummyLabels.add(new Label(Long.parseLong("10"), "lastTest"));
		DynamicDAO.saveAll(dummyLabels);
		assertEquals("elements returned from ContentProvider should be one more", dummyLabels.size(), contentProvider.getElements(null).length);
	}
	
	/**
	 * Tests the associated {@link SearchMarks} and their search conditions
	 */
	@Test
	public void testLabelSearchMarks(){
		List<ILabel> dummyLabels = DummyObjectUtilities.createTestLabelArray(5);
		
		DynamicDAO.saveAll(dummyLabels);
		
		assertNotNull(contentProvider);
				
		Object[] result = contentProvider.getElements(null);
		assertNotNull("elements array returned from ContentProvider shouldn't be null",result);
		
		for (Object object : result) {
			ILabel label = (ILabel)object;
			assertNotNull("Label object shouldn't be null", label);
			
			ISearchMark searchMark = label.getSearchMark();
			assertNotNull("Label's searchmark shouldn't be null",searchMark);
			
			assertEquals("Should have just one searchcondition", 1, searchMark.getSearchConditions().size());
			
			//check search condition
			ISearchCondition searchCondition = searchMark.getSearchConditions().get(0);
			assertNotNull("Search condition shouldn't be null", searchCondition);
			assertEquals("Should be INews.Label", INews.LABEL, searchCondition.getField().getId());
			assertEquals("Should be INews entity", INews.class.getName(), searchCondition.getField().getEntityName());
		}
	}

}
