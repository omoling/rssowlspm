package org.rssowl.core.genericTests;


import java.net.URISyntaxException;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.tests.util.DummyObjectUtilities;
import org.rssowl.core.util.PageableList;

public class PageableListTest {
	private PageableList<IBookMark> pageableBookmarkList;
	
	@Before
	public void setUp() throws Exception {
		pageableBookmarkList = new PageableList<IBookMark>();
	}

	@After
	public void tearDown() throws Exception {
		pageableBookmarkList = null;
	}
	
	/**
	 * Tests the basic functionalities. It just ensures that the PageableList object works
	 * @throws URISyntaxException
	 */
	@Test
	public void testAddRetrieveElements() throws URISyntaxException{
		IBookMark bookmark = DummyObjectUtilities.createDummyBookMark(1, "Bookmark 1");
		
		assertEquals("size should be 0", 0, pageableBookmarkList.size());
		
		pageableBookmarkList.add(bookmark);
		
		assertEquals("size should be 1", 1, pageableBookmarkList.size());
		
		IBookMark retrieved = pageableBookmarkList.get(0);
		assertNotNull("retrieved object shouldn't be null", retrieved);
	}
	
	/**
	 * Tests the paging functionality of switching to the next page
	 * @throws URISyntaxException 
	 */
	@Test
	public void testNextPage() throws URISyntaxException{
		final int maxItems = 25;
		pageableBookmarkList = new PageableList<IBookMark>(DummyObjectUtilities.createTestBookmarkArray(maxItems));
		assertNotNull("list should be instantiated", pageableBookmarkList);
		assertEquals("List should contain items", maxItems, pageableBookmarkList.size());
		
		//retrieve the first page and check
		List<IBookMark> list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain 10 items", 10, list.size());
		assertEquals("the 1st item of the 1st page should have ID 0", 0, list.get(0).getId());
		assertEquals("the last item of the 1st page should have ID 9", 9, list.get(list.size()-1).getId());
		
		pageableBookmarkList.next();
		list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain items", 10, list.size());
		assertEquals("the 1st item of the 2nd page should have ID 10",10, list.get(0).getId());
		assertEquals("the last item of the 2nd page should have ID 19", 19, list.get(list.size()-1).getId());
		
		//upper bound test
		pageableBookmarkList.next();
		list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain the last 5 items", 5, list.size());
		assertEquals("the 1st item of the 3rd page should have ID 20",20, list.get(0).getId());
		assertEquals("the last item of the 1st page should have ID 24", 24, list.get(list.size()-1).getId());
	}
	
	/**
	 * Tests the paging functionality of switching to the previous page
	 * @throws URISyntaxException 
	 */
	@Test
	public void testPreviousPage() throws URISyntaxException{
		final int maxItems = 25;
		pageableBookmarkList = new PageableList<IBookMark>(DummyObjectUtilities.createTestBookmarkArray(maxItems));
		assertNotNull("list should be instantiated", pageableBookmarkList);
		assertEquals("List should contain items", maxItems, pageableBookmarkList.size());
		
		//retrieve the first page
		List<IBookMark> list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain items", 10, list.size());
		assertEquals(0, list.get(0).getId());
		assertEquals(9, list.get(list.size()-1).getId());
		
		//do steps till the last page in order to perform "previous" steps 
		//and check whether we are on the last page
		pageableBookmarkList.next();
		pageableBookmarkList.next();
		list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain the last 5 items", 5, list.size());
		assertEquals(20, list.get(0).getId());
		assertEquals(24, list.get(list.size()-1).getId());
		
		//start with previous page testing
		pageableBookmarkList.previous();
		list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain 10 items", 10, list.size());
		assertEquals("the 1st item of the 2nd page should have ID 10", 10, list.get(0).getId());
		assertEquals("the last item of the 2nd page should have ID 19", 19, list.get(list.size()-1).getId());

		pageableBookmarkList.previous();
		list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain 10 items", 10, list.size());
		assertEquals("the 1st item of the 1st page should have ID 0", 0, list.get(0).getId());
		assertEquals("the last item of the 1st page should have ID 9", 9, list.get(list.size()-1).getId());
		
		//lower bound test: this would go over the lower bound (negative index)
		pageableBookmarkList.previous();
		list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain 10 items", 10, list.size());
		assertEquals("the 1st item of the 1st page should have ID 0", 0, list.get(0).getId());
		assertEquals("the last item of the 1st page should have ID 9", 9, list.get(list.size()-1).getId());
	}
	
	/**
	 * Tests the paging functionality when the list gets modified (i.e. elements are added/removed)
	 * during paging
	 * @throws URISyntaxException 
	 */
	@Test
	public void testPagingWhenModifyingList() throws URISyntaxException{
		final int maxItems = 25;
		pageableBookmarkList = new PageableList<IBookMark>(DummyObjectUtilities.createTestBookmarkArray(maxItems));
		assertNotNull("list should be instantiated", pageableBookmarkList);
		assertEquals("List should contain items", maxItems, pageableBookmarkList.size());
		
		//retrieve the first page
		List<IBookMark> list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain items", 10, list.size());
		assertEquals(0, list.get(0).getId());
		assertEquals(9, list.get(list.size()-1).getId());
		
		pageableBookmarkList.next();
		list = pageableBookmarkList.getCurrentPage();
		assertNotNull("list should be instantiated", list);
		assertEquals("List should contain items", 10, list.size());
		assertEquals("the 1st item of the 2nd page should have ID 10",10, list.get(0).getId());
		assertEquals("the last item of the 2nd page should have ID 19", 19, list.get(list.size()-1).getId());
		
		//remove an item
		pageableBookmarkList.remove(15);
		list = pageableBookmarkList.getCurrentPage();
		assertEquals("List should contain items", 10, list.size());
		assertEquals("the 1st item of the 2nd page should have ID 10",10, list.get(0).getId());
		assertEquals("the last item of the 2nd page should have NOW ID 20 instead of 19", 20, list.get(list.size()-1).getId());
		
		pageableBookmarkList.previous();
		list = pageableBookmarkList.getCurrentPage();
		assertEquals("List should contain items", 10, list.size());
		assertEquals("the 1st item of the 1st page should have ID 0",0, list.get(0).getId());
		assertEquals("the last item of the 1st page should have ID 9", 9, list.get(list.size()-1).getId());
	}
	
}