package org.rssowl.ui.internal.views.statistics;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.BookMarkAdapter;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.BookMarkListener;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.core.util.PageableList;
import org.rssowl.ui.internal.util.JobRunner;


public class StatisticsExplorer extends ViewPart{
	public static final String VIEW_ID = "org.rssowl.ui.StatisticsExplorer"; //$NON-NLS-1$

	private TableViewer fViewer;
	private IContentProvider fContentProvider;
	private StatisticsLabelProvider fLabelProvider;
	private CustomStatisticsViewSorter<IBookMark> fBookMarkSorter;
	private CustomStatisticsViewSorter.Type fSortingType = CustomStatisticsViewSorter.Type.SORT_BY_POPULARITY;
	
	private BookMarkListener fBookMarkListener;
	private NewsListener fNewsListener;
	
	private ReadPercentageChart fChart;
	private Label fChartLabel;
	
	//should be layed out to composite for reusability
	private Button prevButton;
	private Button nextButton;
	
	private PageableList<IBookMark> pageableBookmarkList;

	/**
	 * The constructor.
	 */
	public StatisticsExplorer() {
		fChart = new ReadPercentageChart();
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		createViewer(parent);
	}
	

	private void createViewer(Composite parent) {
		parent.setLayout(new FillLayout());
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		
		Composite container = new Composite(scrolledComposite, SWT.NONE);
		GridLayout layout = new GridLayout(1,true);
		container.setLayout(layout);
		
		fViewer = new StatisticsViewer(this, container, SWT.MULTI | SWT.NO_SCROLL);
		
		//create the columns
		((StatisticsViewer)fViewer).createColumns();
		
		fContentProvider = new StatisticsContentProvider();
		fViewer.setContentProvider(fContentProvider);
		
		fLabelProvider = new StatisticsLabelProvider();
		fViewer.setLabelProvider(fLabelProvider);
		
		fBookMarkSorter = new CustomStatisticsViewSorter<IBookMark>();
		
		//load elements
		pageableBookmarkList = new PageableList<IBookMark>();
		pageableBookmarkList.addAll(DynamicDAO.loadAll(IBookMark.class));
		
		//do the default sorting
		Collections.sort(pageableBookmarkList, fBookMarkSorter);
		
		//set the first page as the input
		fViewer.setInput(pageableBookmarkList.getCurrentPage());
		
		//create the composite containing the paging controls
		Composite pagingControls = new Composite(container, SWT.NONE);
		layout = new GridLayout(2,true);
		pagingControls.setLayout(layout);
		pagingControls.setSize(200, 100);
		prevButton = new Button(pagingControls, SWT.PUSH);
		prevButton.setText("previous");
		nextButton = new Button(pagingControls, SWT.PUSH);
		nextButton.setText("next");
		
		//add the label diagram
		fChartLabel = new Label(container, SWT.NONE);
		initializeChartLabel();
		
		scrolledComposite.setContent(container);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				
		Listener sortListener = createSortListener();
		
		for (TableColumn column : fViewer.getTable().getColumns()) {
			column.addListener(SWT.Selection, sortListener);
		}
		
		registerListeners();
	}
	
	private void initializeChartLabel() {
		fChartLabel.setImage(fChart.createChartImage(fChart.getPieChart(), 420, 270));
	}
	
	
	private void reloadAndRefresh(){
		JobRunner.runInUIThread(fViewer.getTable(), new Runnable() {
	          public void run() {
	        	pageableBookmarkList.clear();
	      		pageableBookmarkList.addAll(DynamicDAO.loadAll(IBookMark.class));
	      		restoreSorting();
	      		
	      		fViewer.setInput(pageableBookmarkList.getCurrentPage());
	      		fViewer.refresh();
	      		
	      		initializeChartLabel();
	          }
		});
	}
	
	/**
	 * Sorts the list according to the previously used sorting type
	 */
	private void restoreSorting(){
		//restore the previously used sorting type
		fBookMarkSorter.setType(fSortingType);
		
		//do the sorting
		Collections.sort(pageableBookmarkList, fBookMarkSorter);
	}
	
	private void registerListeners() {
		fBookMarkListener = new BookMarkAdapter() {
			@Override
			public void entitiesAdded(Set<BookMarkEvent> events) {
				reloadAndRefresh();
			}
			
			@Override
			public void entitiesDeleted(Set<BookMarkEvent> events) {
				reloadAndRefresh();
			}
			
			@Override
			public void entitiesUpdated(Set<BookMarkEvent> events) {
				reloadAndRefresh();
			}
		};
		DynamicDAO.addEntityListener(IBookMark.class, fBookMarkListener);	
		
		fNewsListener = new NewsAdapter() {
			@Override
			public void entitiesAdded(Set<NewsEvent> events) {
				reloadAndRefresh();
			}
			@Override
			public void entitiesDeleted(Set<NewsEvent> events) {
				reloadAndRefresh();
			}
			
			@Override
			public void entitiesUpdated(Set<NewsEvent> events) {
				reloadAndRefresh();
			}
		};
		DynamicDAO.addEntityListener(INews.class, fNewsListener);
		
		nextButton.addSelectionListener(new SelectionListener() {

		      public void widgetSelected(SelectionEvent event) {
		    	  //page to the next
		    	  pageableBookmarkList.next();
		    	  fViewer.setInput(pageableBookmarkList.getCurrentPage());
		    	  fViewer.refresh();
		      }

		      public void widgetDefaultSelected(SelectionEvent event) {
		    	  	
		      }
		    });
		prevButton.addSelectionListener(new SelectionListener() {

		      public void widgetSelected(SelectionEvent event) {	
		    	  //page to the previous
		    	  pageableBookmarkList.previous();
		    	  fViewer.setInput(pageableBookmarkList.getCurrentPage());
		    	  fViewer.refresh();
		      }

		      public void widgetDefaultSelected(SelectionEvent event) {
		    	  	
		      }
		    });
	}
	
	private void unregisterListeners() {
		DynamicDAO.removeEntityListener(IBookMark.class, fBookMarkListener);
		DynamicDAO.removeEntityListener(INews.class, fNewsListener);
	}
	
	/**
	 * Create a Listener object for listening to sorting changes, i.e. when
	 * the user clicks on a table column header
	 * @return
	 */
	private Listener createSortListener(){
		Listener sortListener = new Listener(){
			//@Override
			public void handleEvent(Event event) {
				TableColumn sortColumn = fViewer.getTable().getSortColumn();
				TableColumn currentColumn = (TableColumn)event.widget;
				
				if(sortColumn != currentColumn){
					fViewer.getTable().setSortColumn(currentColumn);
					
					CustomStatisticsViewSorter.Type sortType = CustomStatisticsViewSorter.Type.SORT_BY_TITLE;
					
					if(currentColumn == fViewer.getTable().getColumn(StatisticsViewer.TITLE_COL))
						sortType = CustomStatisticsViewSorter.Type.SORT_BY_TITLE;
					else if(currentColumn == fViewer.getTable().getColumn(StatisticsViewer.POPULARITY_COL))
						sortType = CustomStatisticsViewSorter.Type.SORT_BY_POPULARITY;
					else if(currentColumn == fViewer.getTable().getColumn(StatisticsViewer.READPERCENTAGE_COL))
						sortType = CustomStatisticsViewSorter.Type.SORT_BY_PERCENTAGE;
					else if(currentColumn == fViewer.getTable().getColumn(StatisticsViewer.LASTACCESS_COL))
						sortType = CustomStatisticsViewSorter.Type.SORT_BY_LASTACCESS;
					else if(currentColumn == fViewer.getTable().getColumn(StatisticsViewer.LASTUPDATE_COL))
						sortType = CustomStatisticsViewSorter.Type.SORT_BY_LASTUPDATE;
					
					//remember last sorting type
					fSortingType = sortType;
					fBookMarkSorter.setType(sortType);
				}
				
				//do the sorting
				Collections.sort(pageableBookmarkList, fBookMarkSorter);
				
				//reset the paging indices (i.e. go to the first page)
				pageableBookmarkList.resetIndices();
				
				fViewer.setInput(pageableBookmarkList);
				fViewer.refresh();
			}
		};
		
		return sortListener;
	}
	
	@Override
	public void dispose() {
		unregisterListeners();
		super.dispose();
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		fViewer.getControl().setFocus();
	}
	
	public TableViewer getTableViewer() {
		return fViewer;
	}
}