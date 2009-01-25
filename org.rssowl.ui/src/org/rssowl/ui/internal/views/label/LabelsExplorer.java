package org.rssowl.ui.internal.views.label;


import java.util.Set;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.event.LabelAdapter;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.event.LabelListener;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.NewsListener;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;

/**
 * This is the view for showing all of the user-defined labels
 *
 */
public class LabelsExplorer extends ViewPart {
	public static final String VIEW_ID = "org.rssowl.ui.LabelsExplorer"; //$NON-NLS-1$

	private TableViewer fViewer;
	private IContentProvider fContentProvider;
	private LabelsLabelProvider fLabelProvider;
	private LabelsViewSorter fViewSorter;
	
	private LabelListener fLabelListener;
	private NewsListener fNewsListener;

	/**
	 * The constructor.
	 */
	public LabelsExplorer() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		createViewer(parent);
		hookContextMenu();
	}
	
	private void createViewer(Composite parent){
		fViewer = new LabelsViewer(this, parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		
		fContentProvider = new LabelContentProvider();
		fViewer.setContentProvider(fContentProvider);
		
		fLabelProvider = new LabelsLabelProvider();
		fViewer.setLabelProvider(fLabelProvider);
		
		fViewSorter = new LabelsViewSorter();
		fViewer.setSorter(fViewSorter);
		
		fViewer.setInput(getViewSite());
		
		 /* Hook Open Support */
	    fViewer.addOpenListener(new IOpenListener() {
	      public void open(OpenEvent event) {
	    	  OwlUI.openInFeedView(getViewSite().getPage(), (IStructuredSelection)fViewer.getSelection());
	      	  }
	      });
	    
	    registerListeners();
	}
	
	private void registerListeners() {
		fLabelListener = new LabelAdapter() {
			@Override
			public void entitiesAdded(Set<LabelEvent> events) {
				refresh();
			}
			
			@Override
			public void entitiesDeleted(Set<LabelEvent> events) {
				refresh();
			}
			
			@Override
			public void entitiesUpdated(Set<LabelEvent> events) {
				refresh();
			}
		};
		DynamicDAO.addEntityListener(ILabel.class, fLabelListener);
		
		fNewsListener = new NewsAdapter() {
			@Override
			public void entitiesDeleted(Set<NewsEvent> events) {
				refresh();
			}
			
			@Override
			public void entitiesUpdated(Set<NewsEvent> events) {
				refresh();
			}
		};
		DynamicDAO.addEntityListener(INews.class, fNewsListener);
	}
	
	private void unregisterListeners() {
		DynamicDAO.removeEntityListener(ILabel.class, fLabelListener);
		DynamicDAO.removeEntityListener(INews.class, fNewsListener);
	}
	
	@Override
	public void dispose() {
		unregisterListeners();
		super.dispose();
	}
	
	private void refresh() {
		JobRunner.runInUIThread(fViewer.getTable(), new Runnable() {
	          public void run() {
	            fViewer.refresh(true);
	          }
		});
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fViewer);
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