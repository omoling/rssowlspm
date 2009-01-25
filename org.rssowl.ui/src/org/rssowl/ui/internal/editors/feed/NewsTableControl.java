/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.event.LabelAdapter;
import org.rssowl.core.persist.event.LabelEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.util.ITask;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.TaskAdapter;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.CColumnLayoutData;
import org.rssowl.ui.internal.CTree;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.ManageLabelsPreferencePage;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.StatusLineUpdater;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.OpenNewsAction;
import org.rssowl.ui.internal.actions.RateAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.JobTracker;
import org.rssowl.ui.internal.util.ModelUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Part of the FeedView to display News in a TableViewer.
 * 
 * @author bpasero
 */
public class NewsTableControl implements IFeedViewPart {

	/* ID to associate a Column with its ID */
	static final String COL_ID = "org.rssowl.ui.internal.editors.feed.ColumnIdentifier";

	/* Workaround for unknown Dateo-Col Width */
	private static int DATE_COL_WIDTH = -1;

	/* TODO Developer's flag to enable / disable COD */
	static final boolean USE_CUSTOM_OWNER_DRAWN = true;

	/* Default Asecnding Sort Order value */
	private static final boolean INITIAL_ASCENDING = false;

	/* Indices of Columns in the Tree-Viewer */
	static final int COL_TITLE = 0;
	static final int COL_FEED = 1;
	static final int COL_PUBDATE = 2;
	static final int COL_AUTHOR = 3;
	static final int COL_CATEGORY = 4;
	static final int COL_STICKY = 5;
	static final int COL_RATING = 6;

	/** Supported Columns of the Viewer */
	public enum Columns {

		/** Title of the News */
		TITLE(true, true),

		/** Date of the News */
		DATE(false, true),

		/** Author of the News */
		AUTHOR(true, true),

		/** Category of the News */
		CATEGORY(true, true),

		/** Sticky-State of the News */
		STICKY(false, false),

		/** Rating of a News */
		RATING(false, true),

		/** Score of a News */
		SCORE(false, false),

		/** Feed of a News */
		FEED(true, false);

		boolean fPrefersAcending;
		boolean fShowSortIndicator;

		Columns(boolean prefersAscending, boolean showSortIndicator) {
			fPrefersAcending = prefersAscending;
			fShowSortIndicator = showSortIndicator;
		}

		/**
		 * @return Returns <code>TRUE</code> if this Column prefers to be sorted
		 *         ascending and <code>FALSE</code> otherwise.
		 */
		public boolean prefersAscending() {
			return fPrefersAcending;
		}

		/**
		 * @return Returns <code>TRUE</code> if this Column prefers showing a
		 *         sort-indicator and <code>FALSE</code> otherwise.
		 */
		public boolean showSortIndicator() {
			return fShowSortIndicator;
		}
	}

	/* Tracker to Mark selected news as Read */
	private class MarkReadTracker extends JobTracker {
		private boolean fUpdateDelayDynamically;

		MarkReadTracker(int delay, boolean showProgress) {
			super(delay, showProgress, ITask.Priority.INTERACTIVE);
		}

		@Override
		public int getDelay() {
			if (fUpdateDelayDynamically)
				return fInputPreferences
						.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS);

			return super.getDelay();
		}

		public void setUpdateDelayDynamically(boolean updateDelayDynamically) {
			fUpdateDelayDynamically = updateDelayDynamically;
		}
	}

	/* Custom Tooltip Support for Feed Column */
	private static class FeedColumnToolTipSupport extends
			ColumnViewerToolTipSupport {
		FeedColumnToolTipSupport(ColumnViewer viewer, int style) {
			super(viewer, style, false);
		}

		/*
		 * @see
		 * org.eclipse.jface.viewers.ColumnViewerToolTipSupport#getToolTipArea
		 * (org.eclipse.swt.widgets.Event)
		 */
		@Override
		protected Object getToolTipArea(Event event) {
			Tree tree = (Tree) event.widget;
			Point point = new Point(event.x, event.y);
			TreeItem item = tree.getItem(point);

			/* Only valid for Feed Column */
			if (item != null) {
				if (item.getBounds(COL_FEED).contains(point))
					return super.getToolTipArea(event);
			}

			return null;
		}

		public static void enableFor(ColumnViewer viewer) {
			new FeedColumnToolTipSupport(viewer, ToolTip.NO_RECREATE);
		}
	}

	private IEditorSite fEditorSite;
	private MarkReadTracker fNewsStateTracker;
	private MarkReadTracker fInstantMarkUnreadTracker;
	private NewsTableViewer fViewer;
	private ISelectionChangedListener fSelectionChangeListener;
	private IPropertyChangeListener fPropertyChangeListener;
	private CTree fCustomTree;
	private LocalResourceManager fResources;
	private NewsComparator fNewsSorter;
	private Cursor fHandCursor;
	private boolean fShowsHandCursor;
	private final AtomicBoolean fBlockNewsStateTracker = new AtomicBoolean(
			false);
	private LabelAdapter fLabelListener;
	private IPreferenceScope fInputPreferences;
	private final INewsDAO fNewsDao = Owl.getPersistenceService()
			.getDAOService().getNewsDAO();

	/* Settings */
	private IPreferenceScope fGlobalPreferences;
	private final Columns fInitialSortColumn = Columns.DATE;

	/*
	 * @see
	 * org.rssowl.ui.internal.editors.feed.IFeedViewPart#init(org.eclipse.ui
	 * .IEditorSite)
	 */
	public void init(IEditorSite editorSite) {
		fEditorSite = editorSite;
		fGlobalPreferences = Owl.getPreferenceService().getGlobalScope();
		fResources = new LocalResourceManager(JFaceResources.getResources());
		fInstantMarkUnreadTracker = new MarkReadTracker(0, false);
	}

	/*
	 * @see
	 * org.rssowl.ui.internal.editors.feed.IFeedViewPart#onInputChanged(org.
	 * rssowl.ui.internal.editors.feed.FeedViewInput)
	 */
	public void onInputChanged(FeedViewInput input) {
		fInputPreferences = Owl.getPreferenceService().getEntityScope(
				input.getMark());

		if (fNewsStateTracker != null)
			fNewsStateTracker.cancel();

		fInstantMarkUnreadTracker.cancel();

		fNewsStateTracker = new MarkReadTracker(fInputPreferences
				.getInteger(DefaultPreferences.MARK_READ_IN_MILLIS), false);
		fNewsStateTracker.setUpdateDelayDynamically(true);
	}

	IPreferenceScope getInputPreferences() {
		return fInputPreferences;
	}

	/*
	 * @see
	 * org.rssowl.ui.internal.editors.feed.IFeedViewPart#createViewer(org.eclipse
	 * .swt.widgets.Composite)
	 */
	public NewsTableViewer createViewer(Composite parent) {
		int style = SWT.MULTI | SWT.FULL_SELECTION;

		fCustomTree = new CTree(parent, style);
		fCustomTree.getControl().setHeaderVisible(true);

		fViewer = new NewsTableViewer(fCustomTree.getControl());
		fViewer.getControl().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		fViewer.setUseHashlookup(true);
		fViewer.getControl().setData(
				ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK,
				new Object());
		fViewer.getControl().setFont(
				OwlUI.getThemeFont(OwlUI.HEADLINES_FONT_ID, SWT.NORMAL));

		/* Custom Tooltip for Feed Column */
		FeedColumnToolTipSupport.enableFor(fViewer);

		/* TODO This is a Workaround until we remember expanded Groups */
		fViewer.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);

		fHandCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);

		/* Drag and Drop */
		initDragAndDrop();

		return fViewer;
	}

	private void initDragAndDrop() {
		int ops = DND.DROP_COPY | DND.DROP_MOVE;
		Transfer[] transfers = new Transfer[] {
				LocalSelectionTransfer.getTransfer(),
				TextTransfer.getInstance(), URLTransfer.getInstance() };

		/* Drag Support */
		fViewer.addDragSupport(ops, transfers, new DragSourceListener() {
			public void dragStart(final DragSourceEvent event) {
				SafeRunner.run(new LoggingSafeRunnable() {
					public void run() throws Exception {
						LocalSelectionTransfer.getTransfer().setSelection(
								fViewer.getSelection());
						LocalSelectionTransfer.getTransfer()
								.setSelectionSetTime(event.time & 0xFFFFFFFFL);
						event.doit = true;
					}
				});
			}

			public void dragSetData(final DragSourceEvent event) {
				SafeRunner.run(new LoggingSafeRunnable() {
					public void run() throws Exception {

						/* Set Selection using LocalSelectionTransfer */
						if (LocalSelectionTransfer.getTransfer()
								.isSupportedType(event.dataType))
							event.data = LocalSelectionTransfer.getTransfer()
									.getSelection();

						/* Set Text using Text- or URLTransfer */
						else if (TextTransfer.getInstance().isSupportedType(
								event.dataType)
								|| URLTransfer.getInstance().isSupportedType(
										event.dataType))
							setTextData(event);
					}
				});
			}

			public void dragFinished(DragSourceEvent event) {
				SafeRunner.run(new LoggingSafeRunnable() {
					public void run() throws Exception {
						LocalSelectionTransfer.getTransfer().setSelection(null);
						LocalSelectionTransfer.getTransfer()
								.setSelectionSetTime(0);
					}
				});
			}
		});
	}

	private void setTextData(DragSourceEvent event) {
		IStructuredSelection selection = (IStructuredSelection) LocalSelectionTransfer
				.getTransfer().getSelection();
		Set<INews> news = ModelUtils.normalize(selection.toList());

		if (!news.isEmpty()) {
			String linkAsText = news.iterator().next().getLinkAsText();
			if (StringUtils.isSet(linkAsText))
				event.data = linkAsText;
		}
	}

	/*
	 * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#getViewer()
	 */
	public NewsTableViewer getViewer() {
		return fViewer;
	}

	/*
	 * @see
	 * org.rssowl.ui.internal.editors.feed.IFeedViewPart#initViewer(org.eclipse
	 * .jface.viewers.IStructuredContentProvider,
	 * org.eclipse.jface.viewers.ViewerFilter)
	 */
	public void initViewer(IStructuredContentProvider contentProvider,
			ViewerFilter filter) {

		/* Headline Column */
		TreeViewerColumn col = new TreeViewerColumn(fViewer, SWT.LEFT);
		fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(
				CColumnLayoutData.Size.FILL, 70), "Title", null, false, true);
		col.getColumn().setData(COL_ID, Columns.TITLE);
		col.getColumn().setMoveable(false);
		if (fInitialSortColumn == Columns.TITLE) {
			fCustomTree.getControl().setSortColumn(col.getColumn());
			fCustomTree.getControl().setSortDirection(
					INITIAL_ASCENDING ? SWT.UP : SWT.DOWN);
		}

		/* Feed Column (visible only for saved searches) */
		col = new TreeViewerColumn(fViewer, SWT.LEFT);
		fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(
				CColumnLayoutData.Size.FIXED, Application.IS_LINUX ? 20 : 18),
				null, null, false, false);
		col.getColumn().setData(COL_ID, NewsTableControl.Columns.FEED);
		col.getColumn().setToolTipText("Feed");
		if (fInitialSortColumn == NewsTableControl.Columns.FEED) {
			fCustomTree.getControl().setSortColumn(col.getColumn());
			fCustomTree.getControl().setSortDirection(
					INITIAL_ASCENDING ? SWT.UP : SWT.DOWN);
		}
		fCustomTree.setVisible(col.getColumn(), false, false);

		/* Date Column */
		int width = getInitialDateColumnWidth();
		col = new TreeViewerColumn(fViewer, SWT.LEFT);
		fCustomTree
				.manageColumn(col.getColumn(), new CColumnLayoutData(
						CColumnLayoutData.Size.FIXED, width), "Date", null,
						false, true);
		col.getColumn().setData(COL_ID, Columns.DATE);
		col.getColumn().setMoveable(false);
		if (fInitialSortColumn == Columns.DATE) {
			fCustomTree.getControl().setSortColumn(col.getColumn());
			fCustomTree.getControl().setSortDirection(
					INITIAL_ASCENDING ? SWT.UP : SWT.DOWN);
		}

		/* Author Column */
		col = new TreeViewerColumn(fViewer, SWT.LEFT);
		fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(
				CColumnLayoutData.Size.FILL, 20), "Author", null, false, true);
		col.getColumn().setData(COL_ID, Columns.AUTHOR);
		col.getColumn().setMoveable(false);
		if (fInitialSortColumn == Columns.AUTHOR) {
			fCustomTree.getControl().setSortColumn(col.getColumn());
			fCustomTree.getControl().setSortDirection(
					INITIAL_ASCENDING ? SWT.UP : SWT.DOWN);
		}

		/* Category Column */
		col = new TreeViewerColumn(fViewer, SWT.LEFT);
		fCustomTree
				.manageColumn(col.getColumn(), new CColumnLayoutData(
						CColumnLayoutData.Size.FILL, 20), "Category", null,
						false, true);
		col.getColumn().setData(COL_ID, Columns.CATEGORY);
		col.getColumn().setMoveable(false);
		if (fInitialSortColumn == Columns.CATEGORY) {
			fCustomTree.getControl().setSortColumn(col.getColumn());
			fCustomTree.getControl().setSortDirection(
					INITIAL_ASCENDING ? SWT.UP : SWT.DOWN);
		}

		/* Sticky Column */
		col = new TreeViewerColumn(fViewer, SWT.LEFT);
		fCustomTree.manageColumn(col.getColumn(), new CColumnLayoutData(
				CColumnLayoutData.Size.FIXED, 18), null, null, false, false);
		col.getColumn().setData(COL_ID, Columns.STICKY);
		col.getColumn().setMoveable(false);
		col.getColumn().setToolTipText("Sticky State");

		/* Rating Column */
		col = new TreeViewerColumn(fViewer, SWT.LEFT);
		fCustomTree.manageColumn(col.getColumn(), 
				new CColumnLayoutData(CColumnLayoutData.Size.FIXED, 80), "Rating", null, false, true);
		col.getColumn().setData(COL_ID, Columns.RATING);
		col.getColumn().setMoveable(false);
		col.getColumn().setToolTipText("Rating");

		/* Apply ContentProvider */
		fViewer.setContentProvider(contentProvider);

		/* Create LabelProvider (Custom Owner Drawn enabled!) */
		final NewsTableLabelProvider newsTableLabelProvider = new NewsTableLabelProvider(
				fViewer);
		if (USE_CUSTOM_OWNER_DRAWN) {
			fViewer.getControl().addListener(SWT.EraseItem, new Listener() {
				public void handleEvent(Event event) {
					Object element = event.item.getData();
					newsTableLabelProvider.erase(event, element);
				}
			});
			
			fViewer.getControl().addListener(SWT.PaintItem, new Listener() {

				public void handleEvent(Event event) {
					if (event.index == COL_RATING) {
						if (event.item.getData() instanceof INews) {
							INews news = (INews)event.item.getData();
							int newsRating = news.getRating();
							Image image = null;
							switch (newsRating) {
							case 0:
								image = OwlUI.getImage(fResources, OwlUI.NEWS_STARON_0);
								break;
							case 1:
								image = OwlUI.getImage(fResources, OwlUI.NEWS_STARON_1);
								break;
							case 2:
								image = OwlUI.getImage(fResources, OwlUI.NEWS_STARON_2);
								break;
							case 3:
								image = OwlUI.getImage(fResources, OwlUI.NEWS_STARON_3);
								break;
							case 4:
								image = OwlUI.getImage(fResources, OwlUI.NEWS_STARON_4);
								break;
							case 5:
								image = OwlUI.getImage(fResources, OwlUI.NEWS_STARON_5);
								break;
							default:
								break;
							}
							int itemHeight = fViewer.getTree().getItemHeight();
							int imageHeight = image.getBounds().height;
						    int y = event.y + (itemHeight - imageHeight) / 2;
						    event.gc.drawImage(image, event.x, y);
						}
					}
				}
			});
		}

		// OwnerDrawLabelProvider.setUpOwnerDraw(fViewer); Not being used due to
		// performance reasons
		fViewer.setLabelProvider(newsTableLabelProvider);

		/* Create Sorter */
		fNewsSorter = new NewsComparator();
		fNewsSorter.setAscending(INITIAL_ASCENDING);
		fNewsSorter.setSortBy(fInitialSortColumn);
		fViewer.setComparator(fNewsSorter);

		/* Set Comparer */
		fViewer.setComparer(getComparer());

		/* Add Filter */
		fViewer.addFilter(filter);

		/* Hook Contextual Menu */
		hookContextualMenu();

		/* Register Listeners */
		registerListeners();

		/* Propagate Selection Events */
		fEditorSite.setSelectionProvider(fViewer);
	}

	private int getInitialDateColumnWidth() {

		/* Check if Cached already */
		if (DATE_COL_WIDTH > 0)
			return DATE_COL_WIDTH;

		/* Calculate and Cache */
		DateFormat dF = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT);
		Calendar cal = Calendar.getInstance();
		cal.set(2006, Calendar.DECEMBER, 12, 12, 12, 12);
		String sampleDate = dF.format(cal.getTime());

		DATE_COL_WIDTH = OwlUI.getTextSize(fCustomTree.getControl(), OwlUI
				.getBold(JFaceResources.DEFAULT_FONT), sampleDate).x;
		DATE_COL_WIDTH += 30; // Bounds of TableColumn requires more space

		return DATE_COL_WIDTH;
	}

	private void registerListeners() {

		/* Open selected News Links in Browser on doubleclick */
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				onMouseDoubleClick(event);
			}
		});

		/* Hook into Statusline */
		fViewer.addSelectionChangedListener(new StatusLineUpdater(fEditorSite
				.getActionBars().getStatusLineManager()));

		/* Track Selections in the Viewer */
		fSelectionChangeListener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				onSelectionChanged(event);
			}
		};
		fViewer.addPostSelectionChangedListener(fSelectionChangeListener);

		/* Perform Action on Mouse-Down */
		fCustomTree.getControl().addListener(SWT.MouseDown, new Listener() {
			public void handleEvent(Event event) {
				onMouseDown(event);
			}
		});

		/* Update Cursor on Mouse-Move */
		fCustomTree.getControl().addListener(SWT.MouseMove, new Listener() {
			public void handleEvent(Event event) {
				onMouseMove(event);
			}
		});

		/* Enable Sorting adding listeners to Columns */
		TreeColumn[] columns = fCustomTree.getControl().getColumns();
		for (final TreeColumn column : columns) {
			column.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Columns oldSortBy = fNewsSorter.getSortBy();
					Columns newSortBy = (Columns) column.getData(COL_ID);
					boolean defaultAscending = newSortBy.prefersAscending();
					boolean ascending = (oldSortBy != newSortBy) ? defaultAscending
							: !fNewsSorter.isAscending();

					fNewsSorter.setSortBy(newSortBy);
					fNewsSorter.setAscending(ascending);

					/*
					 * Indicate Sort-Column in UI for Columns that have a
					 * certain width
					 */
					if (newSortBy.showSortIndicator()) {
						fCustomTree.getControl().setSortColumn(column);
						fCustomTree.getControl().setSortDirection(
								ascending ? SWT.UP : SWT.DOWN);
					} else {
						fCustomTree.getControl().setSortColumn(null);
					}

					fViewer.refresh(false);
				}
			});
		}

		/* Redraw on Label update */
		fLabelListener = new LabelAdapter() {
			@Override
			public void entitiesUpdated(Set<LabelEvent> events) {
				JobRunner.runInUIThread(fViewer.getTree(), new Runnable() {
					public void run() {
						fViewer.refresh(true);
					}
				});
			}
		};
		DynamicDAO.addEntityListener(ILabel.class, fLabelListener);

		/* Refresh Viewer when Sticky Color Changes */
		fPropertyChangeListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (fViewer.getControl().isDisposed())
					return;

				if (OwlUI.STICKY_BG_COLOR_ID.equals(event.getProperty())) {
					((NewsTableLabelProvider) fViewer.getLabelProvider())
							.updateResources();
					fViewer.refresh(true);
				}
			}
		};
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(
				fPropertyChangeListener);
	}

	private void onMouseDoubleClick(DoubleClickEvent event) {
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();
		if (selection.isEmpty())
			return;

		Object firstElem = selection.getFirstElement();

		/* Open News */
		if (firstElem instanceof INews)
			// TODO UserStory 1: Changed this from OpenInBrowser to
			// OpenInExternalBrowser
			// new OpenInBrowserAction(selection).run();
			new OpenInExternalBrowserAction(selection).run(); // opens directy
																// in external
																// browser

		/* Toggle expanded State of Group */
		else if (firstElem instanceof EntityGroup)
			fViewer.setExpandedState(firstElem, !fViewer
					.getExpandedState(firstElem));
	}

	private void onSelectionChanged(SelectionChangedEvent event) {

		/* Check Flag and only consider Structured Selections */
		if (fBlockNewsStateTracker.get()
				|| !(event.getSelection() instanceof IStructuredSelection))
			return;

		/* Retrieve all NewsReferences of the Selection */
		IStructuredSelection selection = (IStructuredSelection) event
				.getSelection();

		/* Only responsible for single Selection of a News */
		if (selection.size() != 1
				|| !(selection.getFirstElement() instanceof INews)) {
			fNewsStateTracker.cancel();
			fInstantMarkUnreadTracker.cancel();
			return;
		}

		/* Trigger the Tracker if news is not read already */
		final INews selectedNews = (INews) selection.getFirstElement();
		if (selectedNews.getState() != INews.State.READ
				&& selectedNews.isVisible()) {
			final boolean markRead = fInputPreferences
					.getBoolean(DefaultPreferences.MARK_READ_STATE);
			final int delay = fNewsStateTracker.getDelay();

			/* Instantly mark asunread if required */
			if ((!markRead || delay > 0)
					&& selectedNews.getState() != INews.State.UNREAD) {
				fInstantMarkUnreadTracker.run(new TaskAdapter() {
					public IStatus run(IProgressMonitor monitor) {
						setNewsState(selectedNews, INews.State.UNREAD);
						return Status.OK_STATUS;
					}
				});
			}

			/* Mark Read after Delay */
			if (markRead) {
				fNewsStateTracker.run(new TaskAdapter() {
					public IStatus run(IProgressMonitor monitor) {
						setNewsState(selectedNews, INews.State.READ);
						return Status.OK_STATUS;
					}
				});
			}
		}

		/* Cancel any possible running JobTracker */
		else if (selectedNews.getState() == INews.State.READ) {
			fNewsStateTracker.cancel();
			fInstantMarkUnreadTracker.cancel();
		}
	}

	private void onMouseDown(Event event) {
		boolean disableTrackerTemporary = false;
		Point p = new Point(event.x, event.y);
		TreeItem item = fCustomTree.getControl().getItem(p);

		/* Problem - return */
		if (item == null || item.isDisposed())
			return;

		/* Mouse-Up over Read-State-Column */
		if (event.button == 1 && item.getImageBounds(COL_TITLE).contains(p)) {
			Object data = item.getData();

			/* Toggle State between Read / Unread */
			if (data instanceof INews) {
				INews news = (INews) data;
				disableTrackerTemporary = (news.getState() == INews.State.READ);
				INews.State newState = (news.getState() == INews.State.READ) ? INews.State.UNREAD
						: INews.State.READ;
				setNewsState(news, newState);
			}
		}

		/* Mouse-Up over Sticky-State-Column */
		else if (event.button == 1
				&& item.getImageBounds(COL_STICKY).contains(p)) {
			Object data = item.getData();

			/* Toggle State between Sticky / Not Sticky */
			if (data instanceof INews) {
				disableTrackerTemporary = false;
				new MakeNewsStickyAction(new StructuredSelection(data)).run();
			}
		}

		/*
		 * This is a workaround: Immediately after the mouse-down-event has been
		 * issued, a selection-event is triggered. This event is resulting in
		 * the news-state-tracker to run and mark the selected news as read
		 * again. To avoid this, we disable the tracker for a short while and
		 * set it back to enabled again.
		 */
		if (disableTrackerTemporary)
			JobRunner.runDelayedFlagInversion(200, fBlockNewsStateTracker);
	}

	private void onMouseMove(Event event) {
		Point p = new Point(event.x, event.y);
		TreeItem item = fCustomTree.getControl().getItem(p);

		/* Problem / Group hovered - reset */
		if (item == null || item.isDisposed()
				|| item.getData() instanceof EntityGroup) {
			if (fShowsHandCursor && !fCustomTree.getControl().isDisposed()) {
				fCustomTree.getControl().setCursor(null);
				fShowsHandCursor = false;
			}
			return;
		}

		/* Show Hand-Cursor if action can be performed */
		boolean changeToHandCursor = item.getImageBounds(COL_TITLE).contains(p)
				|| item.getImageBounds(COL_STICKY).contains(p);
		if (!fShowsHandCursor && changeToHandCursor) {
			fCustomTree.getControl().setCursor(fHandCursor);
			fShowsHandCursor = true;
		} else if (fShowsHandCursor && !changeToHandCursor) {
			fCustomTree.getControl().setCursor(null);
			fShowsHandCursor = false;
		}
	}

	/*
	 * This Comparer is used to optimize some operations on the Viewer being
	 * used. When deleting Entities, the Delete-Event is providing a reference
	 * to the deleted Entity, which can not be resolved anymore. This Comparer
	 * will return <code>TRUE</code> for a reference compared with an Entity
	 * that has the same ID and is belonging to the same Entity. At any time, it
	 * _must_ be avoided to call add, update or refresh with passing in a
	 * Reference!
	 */
	private IElementComparer getComparer() {
		return new IElementComparer() {
			public boolean equals(Object a, Object b) {

				/* Quickyly check this common case */
				if (a == b)
					return true;

				if (a instanceof ModelReference && b instanceof IEntity)
					return ((ModelReference) a).references((IEntity) b);

				if (b instanceof ModelReference && a instanceof IEntity)
					return ((ModelReference) b).references((IEntity) a);

				return a.equals(b);
			}

			public int hashCode(Object element) {
				return element.hashCode();
			}
		};
	}

	private void hookContextualMenu() {
		MenuManager manager = new MenuManager();
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				final IStructuredSelection selection = (IStructuredSelection) fViewer
						.getSelection();

				/* Open */
				{
					manager.add(new Separator("open"));

					// TODO UserStory 1: Removed action for launching in
					// internal browser here
					/* Show only when internal browser is used */
					// if (!selection.isEmpty() &&
					// !fGlobalPreferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER)
					// &&
					// !fGlobalPreferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER))
					// manager.add(new OpenInExternalBrowserAction(selection));
				}

				/* Move To / Copy To */
				if (!selection.isEmpty()) {
					manager.add(new Separator("movecopy"));

					/* Load all news bins and sort by name */
					List<INewsBin> newsbins = new ArrayList<INewsBin>(
							DynamicDAO.loadAll(INewsBin.class));

					Comparator<INewsBin> comparator = new Comparator<INewsBin>() {
						public int compare(INewsBin o1, INewsBin o2) {
							return o1.getName().compareTo(o2.getName());
						};
					};

					Collections.sort(newsbins, comparator);

					/* Move To */
					MenuManager moveMenu = new MenuManager("Move To", "moveto");
					manager.add(moveMenu);

					for (INewsBin bin : newsbins) {
						if (fViewer.getInput() instanceof NewsBinReference
								&& bin.getId().equals(
										((NewsBinReference) fViewer.getInput())
												.getId()))
							continue;

						moveMenu.add(new MoveCopyNewsToBinAction(selection,
								bin, true));
					}

					moveMenu.add(new Separator("movetonewbin"));
					moveMenu.add(new MoveCopyNewsToBinAction(selection, null,
							true));

					/* Copy To */
					MenuManager copyMenu = new MenuManager("Copy To", "copyto");
					manager.add(copyMenu);

					for (INewsBin bin : newsbins) {
						if (fViewer.getInput() instanceof NewsBinReference
								&& bin.getId().equals(
										((NewsBinReference) fViewer.getInput())
												.getId()))
							continue;

						copyMenu.add(new MoveCopyNewsToBinAction(selection,
								bin, false));
					}

					copyMenu.add(new Separator("copytonewbin"));
					copyMenu.add(new MoveCopyNewsToBinAction(selection, null,
							false));
				}

				/* Mark / Label */
				{
					manager.add(new Separator("mark"));

					/* Mark */
					MenuManager markMenu = new MenuManager("Mark", "mark");
					manager.add(markMenu);

					/* Mark as Read */
					IAction action = new ToggleReadStateAction(selection);
					action.setEnabled(!selection.isEmpty());
					markMenu.add(action);

					/* Mark All Read */
					action = new MarkAllNewsReadAction();
					markMenu.add(action);

					/* Sticky */
					markMenu.add(new Separator());
					action = new MakeNewsStickyAction(selection);
					action.setEnabled(!selection.isEmpty());
					markMenu.add(action);

					/* Label */
					if (!selection.isEmpty()) {
						Collection<ILabel> labels = DynamicDAO
								.loadAll(ILabel.class);

						/* Label */
						MenuManager labelMenu = new MenuManager("Label");
						manager.appendToGroup("mark", labelMenu);

						/* Retrieve Labels that all selected News contain */
						Set<ILabel> selectedLabels = ModelUtils
								.getLabelsForAll(selection);

						LabelAction removeAllLabels = new LabelAction(null,
								selection);
						removeAllLabels.setEnabled(!labels.isEmpty());
						labelMenu.add(removeAllLabels);
						labelMenu.add(new Separator());

						for (final ILabel label : labels) {
							LabelAction labelAction = new LabelAction(label,
									selection);
							labelAction.setChecked(selectedLabels
									.contains(label));
							labelMenu.add(labelAction);
						}

						labelMenu.add(new Separator());
						labelMenu.add(new Action("Organize...") {
							@Override
							public void run() {
								PreferencesUtil.createPreferenceDialogOn(
										fViewer.getTree().getShell(),
										ManageLabelsPreferencePage.ID, null,
										null).open();
							}
						});
					}
				}

				/* Rate */
				{
					if (!selection.isEmpty()) {
						/* Rate */
						manager.add(new Separator("rate"));

						MenuManager rateMenu = new MenuManager("Rate");
						manager.appendToGroup("rate", rateMenu);

						for (int i = 0; i <= 5; i++) {
							RateAction rateAction = new RateAction(i, selection);
							rateMenu.add(rateAction);
						}
					}
				}

				manager.add(new Separator("edit"));
				manager.add(new Separator("copy"));
				manager.add(new Separator(
						IWorkbenchActionConstants.MB_ADDITIONS));

				/* Need a good Selection here */
				if (selection.isEmpty()
						|| (selection.size() == 1 && selection
								.getFirstElement() instanceof EntityGroup))
					return;

				/* Show in Feed (only for searchmarks) */
				if (fViewer.getInput() instanceof SearchMarkReference) {
					OpenNewsAction showInFeedAction = new OpenNewsAction(
							selection);
					showInFeedAction.setText("Show in Feed");
					manager.appendToGroup("open", showInFeedAction);
				}
			}
		});

		/* Create and Register with Workbench */
		Menu menu = manager.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);
		fEditorSite.registerContextMenu(manager, fViewer);
	}

	/*
	 * @see
	 * org.rssowl.ui.internal.editors.feed.IFeedViewPart#setInput(java.lang.
	 * Object)
	 */
	public void setPartInput(Object input) {
		Object oldInput = fViewer.getInput();
		Tree tree = fCustomTree.getControl();
		TreeColumn feedColumn = tree.getColumn(COL_FEED);

		boolean oldInputShowsFeedColumn = !(oldInput instanceof BookMarkReference);
		boolean newInputShowsFeedColumn = (input instanceof ISearchMark)
				|| (input instanceof INewsBin)
				|| (input instanceof FolderNewsMark);

		/* Reveal Feed Column */
		if ((!oldInputShowsFeedColumn && newInputShowsFeedColumn)
				|| newInputShowsFeedColumn)
			fCustomTree.setVisible(feedColumn, true, true);

		/* Hide Feed Column */
		else if (oldInputShowsFeedColumn && !newInputShowsFeedColumn)
			fCustomTree.setVisible(feedColumn, false, true);

		/* Set Input to Viewer */
		if (input instanceof IEntity)
			fViewer.setInput(((IEntity) input).toReference());
		else
			fViewer.setInput(input);
	}

	/*
	 * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#setFocus()
	 */
	public void setFocus() {
		fViewer.getControl().setFocus();
	}

	/*
	 * @see org.rssowl.ui.internal.editors.feed.IFeedViewPart#dispose()
	 */
	public void dispose() {
		fNewsStateTracker.cancel();
		fInstantMarkUnreadTracker.cancel();
		fResources.dispose();
		unregisterListeners();
	}

	void setBlockNewsStateTracker(boolean block) {
		fBlockNewsStateTracker.set(block);
	}

	private void unregisterListeners() {
		fViewer.removePostSelectionChangedListener(fSelectionChangeListener);
		DynamicDAO.removeEntityListener(ILabel.class, fLabelListener);
		PlatformUI.getWorkbench().getThemeManager()
				.removePropertyChangeListener(fPropertyChangeListener);
	}

	/* We run this in the UI Thread to avoid race conditions */
	private void setNewsState(final INews news, final INews.State state) {
		JobRunner.runInUIThread(fViewer.getControl(), new Runnable() {
			public void run() {

				/*
				 * The news might have been marked as hidden/deleted meanwhile,
				 * so return
				 */
				if (!news.isVisible())
					return;

				Set<INews> singleNewsSet = Collections.singleton(news);

				/* Add to UndoStack */
				UndoStack.getInstance().addOperation(
						new NewsStateOperation(singleNewsSet, state, true));

				/* Perform Operation */
				fNewsDao.setState(singleNewsSet, state, true, false);
			}
		});
	}
}