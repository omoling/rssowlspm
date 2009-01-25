package org.rssowl.core.tests.ui;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.tests.util.DummyObjectUtilities;
import org.rssowl.ui.internal.views.statistics.ReadPercentageChart;


public class ReadPercentageChartTest {
	private ReadPercentageChart readPercentageChart;

	@Before
	public void setUp() throws Exception {
		//reset DB schema
		Owl.getPersistenceService().recreateSchema();
		
		readPercentageChart = new ReadPercentageChart();
	}

	@After
	public void tearDown() throws Exception {
		readPercentageChart = null;
	}

	/**
	 * tests whether the pie chart shows the correct overall read / unread percentages
	 * @throws URISyntaxException
	 */
	@Test
	public void testGetPieChart() throws URISyntaxException {
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
		
		JFreeChart pieChart = readPercentageChart.getPieChart();
		assertNotNull("Pie chart should not be null", pieChart);
		
		//lower bound test: no news have been read, so pie chart should contain 0% for read and 100% for unread news items
		double unreadPercentage = ((PiePlot)pieChart.getPlot()).getDataset().getValue("Unread news").doubleValue();
		double readPercentage = ((PiePlot)pieChart.getPlot()).getDataset().getValue("Read news").doubleValue();
		assertEquals("should be 0%", 0.0, readPercentage);
		assertEquals("should be 100%", 100.0, unreadPercentage);
		
		//set some news to read state
		DummyObjectUtilities.setNewsItemsRead(bookmarkList.get(0).getNews(), 2);
		DummyObjectUtilities.setNewsItemsRead(bookmarkList.get(3).getNews(), 3);
		
		DynamicDAO.saveAll(bookmarkList);
		
		//test whether the pie chart shows the correct percentage
		pieChart = readPercentageChart.getPieChart();
		assertNotNull("pie chart should not be null", pieChart);
		
		unreadPercentage = ((PiePlot)pieChart.getPlot()).getDataset().getValue("Unread news").doubleValue();
		readPercentage = ((PiePlot)pieChart.getPlot()).getDataset().getValue("Read news").doubleValue();
		assertEquals("should be 20%", 20.0, readPercentage);
		assertEquals("should be 80%", 80.0, unreadPercentage);
		
		//set all news items to read
		for (IBookMark bookMark : bookmarkList) {
			//set all of the bookmark's news items to read
			DummyObjectUtilities.setNewsItemsRead(bookMark.getNews(), bookMark.getNews().size());
		}
		DynamicDAO.saveAll(bookmarkList);
		
		//upper bound test: all news items have been read, so pie chart should contain 100% for read and 0% for unread news items
		pieChart = readPercentageChart.getPieChart();
		assertNotNull("pie chart should not be null", pieChart);
		
		unreadPercentage = ((PiePlot)pieChart.getPlot()).getDataset().getValue("Unread news").doubleValue();
		readPercentage = ((PiePlot)pieChart.getPlot()).getDataset().getValue("Read news").doubleValue();
		assertEquals("should be 100%", 100.0, readPercentage);
		assertEquals("should be 0%", 0.0, unreadPercentage);
	}
	
	/**
	 * tests whether the pie chart is correctly converted to an Image object 
	*/
	@Test
	public void testCreateChartImage() {
		JFreeChart pieChart = readPercentageChart.getPieChart();
		assertNotNull("Pie chart should not be null", pieChart);
		
		int width = 500;
		int height = 300;
		
		Image image = readPercentageChart.createChartImage(pieChart, width, height);
		assertNotNull("image should not be null", image);
		
		assertEquals("heights should be equal", height, image.getBounds().height);
		assertEquals("widths should be equal", width, image.getBounds().width);
	}
}
