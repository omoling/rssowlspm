package org.rssowl.core.tests.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;

public class DummyObjectUtilities {
	
	/**
	 * Creates a dummy Bookmark for testing
	 * @param id identifier of the bookmark
	 * @param name name of the bookmark
	 * @return IBookMark
	 * @throws URISyntaxException
	 */
	public static IBookMark createDummyBookMark(long id, String name) throws URISyntaxException{
		return createDummyBookMark(id, name, 0);
	}
	
	/**
	 * Creates a dummy BookMark for testing
	 * @param id identifier of the bookmark
	 * @param name name of the bookmark
	 * @param popularity popularity of the bookmark
	 * @return IBookMark
	 * @throws URISyntaxException
	 */
	public static IBookMark createDummyBookMark(long id, String name, int popularity) throws URISyntaxException{
		URI dummyURL = new URI("http://www.dummyURL" + id + ".com");
		IFolder folder = new Folder(Long.parseLong("" + 1), null, "dummyroot");
		
		IBookMark bookMark = new BookMark(id, folder, new FeedLinkReference(dummyURL), name);
		bookMark.setPopularity(popularity);
		bookMark.setLastVisitDate(new Date(System.currentTimeMillis() - id * 10));
		
		return bookMark;
	} 
	
	/**
	 * Returns an array of bookmarks used for testing
	 * @param amount amount of bookmars, i.e. the size of the final array
	 * @return {@link IBookMark}[amount]
	 * @throws URISyntaxException 
	 */
	public static List<IBookMark> createTestBookmarkArray(int amount) throws URISyntaxException{
		List<IBookMark> bookmarks = new ArrayList<IBookMark>();
		
		for(int i=0; i< amount; i++){
			bookmarks.add(createDummyBookMark(Long.parseLong("" + i), "Bookmark " + i, i));
		}
		
		return bookmarks;
	}
	
	/**
	 * Returns an array of bookmarks with news used for testing. Each bookmark
	 * has as many News items associated to as it's ID, which is be increasing in the array.
	 * @param amount amount of bookmarks, i.e. the size of the final array
	 * @return {@link IBookMark}[amount]
	 * @throws URISyntaxException
	 */
	public static List<IBookMark> createTestBookmarkArrayWithNews(int amount) throws URISyntaxException{
		List<IBookMark> bookMarks = createTestBookmarkArray(amount);
		
		for(IBookMark bookMark : bookMarks){
			IFeed feed = new Feed(bookMark.getId(), bookMark.getFeedLinkReference().getLink());
			
			for(int i = 0 ; i < (Integer.parseInt("" + bookMark.getId()) + 1); i++){
				INews newsItem = new News(feed);
				feed.addNews(newsItem);	
			}
			bookMark.setFeedLinkReference(new FeedLinkReference(feed.getLink()));
			
			DynamicDAO.save(feed);
		}
		
		return bookMarks;
	}
	
	/**
	 * Returns an array of labels used for testing
	 * @param amount amount of labels, i.e. the size of the final array
	 * @return ILabel[amount]
	 */
	public static List<ILabel> createTestLabelArray(int amount){
		List<ILabel> labels = new ArrayList<ILabel>();
		for(int i=0; i< amount; i++){
			labels.add(new Label(Long.parseLong("" + i), "Label " + i));
		}
		
		return labels;
	}
	
	/**
	 * Returns an search mark for a certain label
	 * @param labelName the name of the label
	 * @return ISearchMark
	 */
	private static ISearchMark createSearchMark(String labelName) {
		IModelFactory factory = Owl.getModelFactory();
		
  	  	ISearchField field = factory.createSearchField(INews.LABEL, INews.class.getName());
  	  	ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.IS, labelName);
  	  	
  	  	IFolder root = Owl.getModelFactory().createFolder(null, null, "LabelRoot");
  	  	
	  	ISearchMark searchMark = factory.createSearchMark(null,root, labelName);
	  	searchMark.addSearchCondition(condition);
	  	return searchMark;
	}
	
	
	/**
	 * Returns a single label used for testing
	 * @return ILabel
	 */
	public static ILabel createTestLabel() {
		ILabel label = new Label(new Long(0), "Label");
		label.setSearchMark(createSearchMark(label.getName()));
		return label;
	}
	
	public static void attachNewsItems(IFeed feed, int numberOfNewsItems){
		for(int i=0; i< numberOfNewsItems; i++){
			INews newsItem = new News(feed);
			feed.addNews(newsItem);
		}
		
		DynamicDAO.save(feed);
	}
	
	/**
	 * Sets a given number of items on the passed list of news to "read" state
	 * @param newsMarks list containing news items
	 * @param numberOfItems number of items which should be set to read
	 */
	public static void setNewsItemsRead(List<INews> newsMarks, int numberOfItems){
		for(int i=0; i < newsMarks.size() && i < numberOfItems; i++){
			newsMarks.get(i).setState(INews.State.READ);
		}
	}
	
	/**
	 * Adds the given label to the news item and sets it as sticky
	 * @param news the news item to add the label to
	 * @param label the label to be added
	 */
	public static void addLabelToNewsSetSticky(INews news, ILabel label){
		news.setFlagged(true);
		addLabelToNews(news, label);
	}
	
	/**
	 * Adds the given label to the news item
	 * @param news the news item to add the label to
	 * @param label the label to be added
	 */
	public static void addLabelToNews(INews news, ILabel label){
		news.addLabel(label);
		DynamicDAO.save(news);
	}
	
}
