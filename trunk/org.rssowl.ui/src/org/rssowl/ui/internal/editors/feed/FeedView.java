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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.EditorPart;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.INewsDAO;
import org.rssowl.core.persist.dao.ISearchMarkDAO;
import org.rssowl.core.persist.event.BookMarkAdapter;
import org.rssowl.core.persist.event.BookMarkEvent;
import org.rssowl.core.persist.event.BookMarkListener;
import org.rssowl.core.persist.event.FeedAdapter;
import org.rssowl.core.persist.event.FeedEvent;
import org.rssowl.core.persist.event.FolderAdapter;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.MarkEvent;
import org.rssowl.core.persist.event.NewsBinAdapter;
import org.rssowl.core.persist.event.NewsBinEvent;
import org.rssowl.core.persist.event.NewsBinListener;
import org.rssowl.core.persist.event.SearchConditionEvent;
import org.rssowl.core.persist.event.SearchConditionListener;
import org.rssowl.core.persist.event.SearchMarkAdapter;
import org.rssowl.core.persist.event.SearchMarkEvent;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.util.RetentionStrategy;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.FolderNewsMark;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.DeleteTypesAction;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.actions.RetargetActions;
import org.rssowl.ui.internal.undo.NewsStateOperation;
import org.rssowl.ui.internal.undo.UndoStack;
import org.rssowl.ui.internal.util.ITreeNode;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.util.TreeTraversal;
import org.rssowl.ui.internal.util.UIBackgroundJob;
import org.rssowl.ui.internal.util.WidgetTreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The FeedView is an instance of <code>EditorPart</code> capable of
 * displaying News in a Table-Viewer and Browser-Viewer. It offers controls to
 * Filter and Group them.
 * <p>
 * TODO Think about storing settings in a central place in memory and flush them
 * to the DB on shutdown. Alternative: Update settings while action is invoked
 * with a short delay (in non-UI-Thread).
 * </p>
 *
 * @author bpasero
 */
public class FeedView extends EditorPart implements IReusableEditor {

  /* Cache to remember selected News-Items */
  private static Map<Integer, NewsReference> fgSelectionCache = new HashMap<Integer, NewsReference>();

  /* TODO Move this to Settings */
  private static final boolean BROWSER_SHOWS_ALL = false;

  /* Delay in ms to Mark *new* News to *unread* on Part-Deactivation */
  private static final int HANDLE_NEWS_SEEN_DELAY = 100;

  /** ID of this EditorPart */
  public static final String ID = "org.rssowl.ui.FeedView"; //$NON-NLS-1$

  /** List of UI-Events interesting for the FeedView */
  public enum UIEvent {

    /** Other Feed Displayed */
    FEED_CHANGE,

    /** Application Minimized */
    MINIMIZE,

    /** Application Closing */
    CLOSE,

    /** Tab Closed */
    TAB_CLOSE
  }

  /* Editor Data */
  private FeedViewInput fInput;
  private IEditorSite fEditorSite;

  /* Part to display News in Table */
  private NewsTableControl fNewsTableControl;

  /* Part to display News in Browser */
  private NewsBrowserControl fNewsBrowserControl;

  /* Bars */
  private FilterBar fFilterBar;
  private BrowserBar fBrowserBar;

  /* Shared Viewer classes */
  private NewsFilter fNewsFilter;
  private NewsGrouping fNewsGrouping;
  private NewsContentProvider fContentProvider;

  /* Container for the Browser Viewer */
  private Composite fBrowserViewerControlContainer;

  /* Listeners */
  private IPartListener2 fPartListener;
  private BookMarkListener fBookMarkListener;
  private SearchMarkAdapter fSearchMarkListener;
  private FeedAdapter fFeedListener;
  private SearchConditionListener fSearchConditionListener;
  private NewsBinListener fNewsBinListener;
  private FolderAdapter fFolderListener;

  /* Settings */
  NewsFilter.Type fInitialFilterType;
  NewsGrouping.Type fInitialGroupType;
  NewsFilter.SearchTarget fInitialSearchTarget;
  boolean fInitialLayoutVertical;
  private boolean fInitialBrowserMaximized;
  private int fInitialWeights[];
  private int fCacheWeights[];

  /* Global Actions */
  private IAction fReloadAction;
  private IAction fSelectAllAction;
  private IAction fDeleteAction;
  private IAction fCutAction;
  private IAction fCopyAction;
  private IAction fPasteAction;
  private IAction fPrintAction;

  /* Misc. */
  private Composite fParent;
  private SashForm fSashForm;
  private Label fTableBrowserSep;
  private LocalResourceManager fResourceManager;
  private IPreferenceScope fPreferences;
  private long fOpenTime;
  private boolean fCreated;
  private final Object fCacheJobIdentifier = new Object();
  private ImageDescriptor fTitleImageDescriptor;
  private Label fBrowserSep;
  private final INewsDAO fNewsDao = Owl.getPersistenceService().getDAOService().getNewsDAO();

  /*
   * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public void doSave(IProgressMonitor monitor) {
  /* Not Supported */
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#doSaveAs()
   */
  @Override
  public void doSaveAs() {
  /* Not Supported */
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
   * org.eclipse.ui.IEditorInput)
   */
  @Override
  public void init(IEditorSite site, IEditorInput input) {
    Assert.isTrue(input instanceof FeedViewInput);

    fEditorSite = site;
    setSite(site);
    fResourceManager = new LocalResourceManager(JFaceResources.getResources());

    /* Load Settings */
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    loadSettings((FeedViewInput) input);

    /* Apply Input */
    setInput(input);

    /* Hook into Global Actions */
    createGlobalActions();
    setGlobalActions();

    /* Register Listeners */
    registerListeners();
  }

  private void registerListeners() {
    fPartListener = new IPartListener2() {

      /* Mark *new* News as *unread* or *read* */
      public void partHidden(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          if (System.currentTimeMillis() - fOpenTime > HANDLE_NEWS_SEEN_DELAY)
            notifyUIEvent(UIEvent.FEED_CHANGE);
      }

      /* Hook into Global Actions for this Editor */
      public void partBroughtToTop(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false))) {
          setGlobalActions();
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
        }
      }

      public void partClosed(IWorkbenchPartReference partRef) {
        IEditorReference[] editors = partRef.getPage().getEditorReferences();
        boolean equalsThis = FeedView.this.equals(partRef.getPart(false));
        if (editors.length == 0 && equalsThis)
          OwlUI.updateWindowTitle(null);

        if (equalsThis)
          notifyUIEvent(UIEvent.TAB_CLOSE);
      }

      public void partDeactivated(IWorkbenchPartReference partRef) {}

      public void partActivated(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }

      public void partInputChanged(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }

      public void partOpened(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false))) {
          fOpenTime = System.currentTimeMillis();
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
        }
      }

      public void partVisible(IWorkbenchPartReference partRef) {
        if (FeedView.this.equals(partRef.getPart(false)))
          OwlUI.updateWindowTitle(fInput != null ? new IMark[] { fInput.getMark() } : null);
      }
    };

    fEditorSite.getPage().addPartListener(fPartListener);

    /* React on Bookmark Events */
    fBookMarkListener = new BookMarkAdapter() {
      @Override
      public void entitiesDeleted(Set<BookMarkEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<BookMarkEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(IBookMark.class, fBookMarkListener);

    /* React on Folder Events */
    fFolderListener = new FolderAdapter() {
      @Override
      public void entitiesDeleted(Set<FolderEvent> events) {
        onFoldersDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<FolderEvent> events) {
        onNewsFoldersUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);

    /* React on Searchmark Events */
    fSearchMarkListener = new SearchMarkAdapter() {
      @Override
      public void entitiesDeleted(Set<SearchMarkEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<SearchMarkEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(ISearchMark.class, fSearchMarkListener);

    /* Refresh on Condition Changes if SearchMark showing */
    fSearchConditionListener = new SearchConditionListener() {
      public void entitiesAdded(Set<SearchConditionEvent> events) {
        refreshIfRequired(events);
      }

      public void entitiesDeleted(Set<SearchConditionEvent> events) {
        refreshIfRequired(events);
      }

      public void entitiesUpdated(Set<SearchConditionEvent> events) {
        refreshIfRequired(events);
      }

      private void refreshIfRequired(Set<SearchConditionEvent> events) {
        ISearchMarkDAO dao = DynamicDAO.getDAO(ISearchMarkDAO.class);
        for (SearchConditionEvent event : events) {
          ISearchCondition condition = event.getEntity();
          ISearchMark searchMark = dao.load(condition);
          if (searchMark != null && searchMark.equals(fInput.getMark())) {
            JobRunner.runUIUpdater(new UIBackgroundJob(fParent) {
              @Override
              protected void runInBackground(IProgressMonitor monitor) {
                fContentProvider.refreshCache(fInput.getMark(), false);
              }

              @Override
              protected void runInUI(IProgressMonitor monitor) {
                refresh(true, true);
              }

              @Override
              public boolean belongsTo(Object family) {
                return fCacheJobIdentifier.equals(family);
              }
            });

            break;
          }
        }
      }
    };
    DynamicDAO.addEntityListener(ISearchCondition.class, fSearchConditionListener);

    /* React on Newsbin Events */
    fNewsBinListener = new NewsBinAdapter() {
      @Override
      public void entitiesDeleted(Set<NewsBinEvent> events) {
        onNewsMarksDeleted(events);
      }

      @Override
      public void entitiesUpdated(Set<NewsBinEvent> events) {
        onNewsMarksUpdated(events);
      }
    };
    DynamicDAO.addEntityListener(INewsBin.class, fNewsBinListener);

    /* Listen if Title Image is changing */
    fFeedListener = new FeedAdapter() {
      @Override
      public void entitiesUpdated(Set<FeedEvent> events) {

        /* Only supported for BookMarks */
        if (!(fInput.getMark() instanceof IBookMark) || events.size() == 0)
          return;

        /* Check if Feed-Event affecting us */
        for (FeedEvent event : events) {
          FeedLinkReference feedRef = ((IBookMark) fInput.getMark()).getFeedLinkReference();
          if (feedRef.references(event.getEntity())) {
            ImageDescriptor imageDesc = fInput.getImageDescriptor();

            /* Title Image Change - Update! */
            if (!fTitleImageDescriptor.equals(imageDesc)) {
              fTitleImageDescriptor = imageDesc;

              JobRunner.runInUIThread(fParent, new Runnable() {
                public void run() {
                  setTitleImage(OwlUI.getImage(fResourceManager, fTitleImageDescriptor));
                }
              });
            }

            break;
          }
        }
      }
    };
    DynamicDAO.addEntityListener(IFeed.class, fFeedListener);
  }

  private void onNewsFoldersUpdated(Set<FolderEvent> events) {
    if (!(fInput.getMark() instanceof FolderNewsMark))
      return;

    FolderNewsMark folderNewsMark = (FolderNewsMark) (fInput.getMark());
    for (FolderEvent event : events) {
      final IFolder folder = event.getEntity();
      if (folder.equals(folderNewsMark.getFolder())) {
        JobRunner.runInUIThread(fParent, new Runnable() {
          public void run() {
            setPartName(folder.getName());
            OwlUI.updateWindowTitle(new IMark[] { fInput.getMark() });
          }
        });

        break;
      }
    }
  }

  private void onFoldersDeleted(Set<FolderEvent> events) {
    if (!(fInput.getMark() instanceof FolderNewsMark))
      return;

    FolderNewsMark folderNewsMark = (FolderNewsMark) (fInput.getMark());
    for (FolderEvent event : events) {
      final IFolder folder = event.getEntity();
      if (folder.equals(folderNewsMark.getFolder())) {
        fInput.setDeleted();
        fEditorSite.getPage().closeEditor(FeedView.this, false);
        break;
      }
    }
  }

  private void onNewsMarksUpdated(Set<? extends MarkEvent> events) {
    for (MarkEvent event : events) {
      final IMark mark = event.getEntity();
      if (mark.getId().equals(fInput.getMark().getId())) {
        JobRunner.runInUIThread(fParent, new Runnable() {
          public void run() {
            setPartName(mark.getName());
            OwlUI.updateWindowTitle(new IMark[] { fInput.getMark() });
          }
        });

        break;
      }
    }
  }

  private void onNewsMarksDeleted(Set<? extends MarkEvent> events) {
    for (MarkEvent event : events) {
      IMark mark = event.getEntity();
      if (fInput.getMark().getId().equals(mark.getId())) {
        fInput.setDeleted();
        fEditorSite.getPage().closeEditor(FeedView.this, false);
        break;
      }
    }
  }

  private void loadSettings(FeedViewInput input) {

    /* Filter Settings */
    IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(input.getMark());
    int iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_FILTERING);
    if (iVal >= 0)
      fInitialFilterType = NewsFilter.Type.values()[iVal];
    else
      fInitialFilterType = NewsFilter.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_FILTER_TYPE)];

    /* Group Settings */
    iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_GROUPING);
    if (iVal >= 0)
      fInitialGroupType = NewsGrouping.Type.values()[iVal];
    else
      fInitialGroupType = NewsGrouping.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_GROUP_TYPE)];

    /* Other Settings */
    fInitialBrowserMaximized = fPreferences.getBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED);
    fInitialLayoutVertical = fPreferences.getBoolean(DefaultPreferences.FV_LAYOUT_VERTICAL);
    fInitialWeights = fPreferences.getIntegers(DefaultPreferences.FV_SASHFORM_WEIGHTS);
    fInitialSearchTarget = NewsFilter.SearchTarget.values()[fPreferences.getInteger(DefaultPreferences.FV_SEARCH_TARGET)];
  }

  private void saveSettings() {

    /* Update Settings in DB */
    if (fCacheWeights != null && !Arrays.equals(fCacheWeights, new int[] { 50, 50 })) {
      JobRunner.runInBackgroundThread(new Runnable() {
        public void run() {
          int strWeights[] = new int[] { fCacheWeights[0], fCacheWeights[1] };
          fPreferences.putIntegers(DefaultPreferences.FV_SASHFORM_WEIGHTS, strWeights);
        }
      });
    }
  }

  private void createGlobalActions() {

    /* Hook into Reload */
    fReloadAction = new Action() {
      @Override
      public void run() {
        new ReloadTypesAction(new StructuredSelection(fInput.getMark()), getEditorSite().getShell()).run();
      }
    };

    /* Select All */
    fSelectAllAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Select All in Text Widget */
        if (focusControl instanceof Text) {
          ((Text) focusControl).selectAll();
        }

        /* Select All in Viewer Tree */
        else {
          ((Tree) fNewsTableControl.getViewer().getControl()).selectAll();
          fNewsTableControl.getViewer().setSelection(fNewsTableControl.getViewer().getSelection());
        }
      }
    };

    /* Delete */
    fDeleteAction = new Action() {
      @Override
      public void run() {
        new DeleteTypesAction(fParent.getShell(), (IStructuredSelection) fNewsTableControl.getViewer().getSelection()).run();
      }
    };

    /* Cut */
    fCutAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Cut in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).cut();
      }
    };

    /* Copy */
    fCopyAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Copy in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).copy();
      }
    };

    /* Paste */
    fPasteAction = new Action() {
      @Override
      public void run() {
        Control focusControl = fEditorSite.getShell().getDisplay().getFocusControl();

        /* Paste in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).paste();
      }
    };

    /* Print */
    fPrintAction = new Action() {
      @Override
      public void run() {
        if (fNewsBrowserControl != null)
          fNewsBrowserControl.getViewer().getBrowser().print();
      }
    };
  }

  /**
   * The user performed the "Find" action.
   */
  public void find() {
    if (fFilterBar != null)
      fFilterBar.focusQuickSearch();
  }

  private void setGlobalActions() {

    /* Define Retargetable Global Actions */
    fEditorSite.getActionBars().setGlobalActionHandler(RetargetActions.RELOAD, fReloadAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), fSelectAllAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), fDeleteAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), fCutAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), fCopyAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), fPasteAction);
    fEditorSite.getActionBars().setGlobalActionHandler(ActionFactory.PRINT.getId(), fPrintAction);

    /* Disable some Edit-Actions at first */
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
    fEditorSite.getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
  }

  /**
   * Sets the given <code>IStructuredSelection</code> to the News-Table
   * showing in the FeedView. Will ignore the selection, if the Table is
   * minimized.
   *
   * @param selection The Selection to show in the News-Table.
   */
  public void setSelection(IStructuredSelection selection) {

    /* Return if Table is not visible */
    if (!isTableViewerVisible())
      return;

    /* Remove Filter if selection is hidden */
    if (fNewsFilter.getType() != NewsFilter.Type.SHOW_ALL) {
      boolean unfilter = false;
      List<?> elements = selection.toList();
      for (Object element : elements) {

        /* Resolve the actual News */
        if (element instanceof NewsReference)
          element = ((NewsReference) element).resolve();

        /* This Element is filtered */
        if (!fNewsFilter.select(fNewsTableControl.getViewer(), null, element)) {
          unfilter = true;
          break;
        }
      }

      /* Remove Filter if selection is hidden */
      if (unfilter) {
        fNewsBrowserControl.getViewer().setBlockRefresh(true);
        try {
          fFilterBar.doFilter(NewsFilter.Type.SHOW_ALL, true, false);
        } finally {
          fNewsBrowserControl.getViewer().setBlockRefresh(false);
        }
      }
    }

    /* Apply selection to Table */
    fNewsTableControl.getViewer().setSelection(selection);
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
   */
  @Override
  public void setInput(IEditorInput input) {
    Assert.isTrue(input instanceof FeedViewInput);
    super.setInput(input);

    /* Handle Old being hidden now */
    if (fInput != null) {
      notifyUIEvent(UIEvent.FEED_CHANGE);
      rememberSelection();
    }

    /* Remember New */
    fInput = (FeedViewInput) input;

    /* Update UI of Feed-View if new Editor */
    if (!fCreated)
      updateTab(fInput);

    /* Clear Filter Bar */
    if (fFilterBar != null)
      fFilterBar.clearQuickSearch(false);

    /* Editor is being reused */
    if (fCreated) {
      firePropertyChange(PROP_INPUT);

      /* Load Filter Settings for this Mark if present */
      updateFilterAndGrouping(false);

      /* Cancel any previous running Cache-Job */
      Job.getJobManager().cancel(fCacheJobIdentifier);

      /* Re-Create the ContentProvider to avoid concurrency problems with Cache */
      fContentProvider = new NewsContentProvider(fNewsTableControl.getViewer(), fNewsBrowserControl.getViewer(), this);
      fNewsTableControl.getViewer().setContentProvider(fContentProvider);
      fNewsTableControl.onInputChanged(fInput);
      fNewsBrowserControl.getViewer().setContentProvider(fContentProvider);
      fNewsBrowserControl.onInputChanged(fInput);

      /* Reset the Quicksearch if active */
      if (fNewsFilter.isPatternSet())
        fNewsFilter.setPattern("");

      /* Update news mark in filter */
      fNewsFilter.setNewsMark(fInput.getMark());

      /* Apply Input */
      setInput(fInput.getMark(), true);
    }
  }

  /* Update Title and Image of the FeedView's Tab */
  private void updateTab(FeedViewInput input) {
    setPartName(input.getName());
    fTitleImageDescriptor = input.getImageDescriptor();
    setTitleImage(OwlUI.getImage(fResourceManager, fTitleImageDescriptor));
  }

  /**
   * Load Filter Settings for the Mark that is set as input if present
   * <p>
   * TODO Find a better solution once its possible to add listeners to
   * {@link IPreferenceScope} and then listen to changes of display-properties.
   * </p>
   *
   * @param refresh If TRUE, refresh the Viewer, FALSE otherwise.
   */
  public void updateFilterAndGrouping(boolean refresh) {
    IPreferenceScope preferences = Owl.getPreferenceService().getEntityScope(fInput.getMark());
    int iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_FILTERING);
    if (iVal >= 0)
      fFilterBar.doFilter(NewsFilter.Type.values()[iVal], refresh, false);
    else
      fFilterBar.doFilter(NewsFilter.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_FILTER_TYPE)], refresh, false);

    /* Load Group Settings for this Mark if present */
    iVal = preferences.getInteger(DefaultPreferences.BM_NEWS_GROUPING);
    if (iVal >= 0)
      fFilterBar.doGrouping(NewsGrouping.Type.values()[iVal], refresh, false);
    else
      fFilterBar.doGrouping(NewsGrouping.Type.values()[fPreferences.getInteger(DefaultPreferences.FV_GROUP_TYPE)], refresh, false);
  }

  /**
   * Notifies this editor about a UI-Event just occured. In dependance of the
   * event, the Editor might want to update the state on the displayed News.
   *
   * @param event The UI-Event that just occured as described in the
   * <code>UIEvent</code> enumeration.
   */
  public void notifyUIEvent(final UIEvent event) {
    final IMark inputMark = fInput.getMark();
    final Collection<INews> news = fContentProvider.getCachedNews();
    IPreferenceScope inputPreferences = Owl.getPreferenceService().getEntityScope(inputMark);

    /*
     * News can be NULL at this moment, if the Job that is to refresh the cache
     * in the Content Provider was never scheduled. This can happen when quickly
     * navigating between feeds. Also, the input could have been deleted and the
     * editor closed. Thereby do not react.
     */
    if (news == null || !fInput.exists())
      return;

    /* Input could have been deleted */
    if (!fInput.exists())
      return;

    final boolean markReadOnFeedChange = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_CHANGE);
    final boolean markReadOnTabClose = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_TAB_CLOSE);
    final boolean markReadOnMinimize = inputPreferences.getBoolean(DefaultPreferences.MARK_READ_ON_MINIMIZE);

    /* Mark *new* News as *unread* when closing the entire application */
    if (event == UIEvent.CLOSE) {

      /* Perform the State Change */
      List<INews> newsToUpdate = new ArrayList<INews>();
      for (INews newsItem : news) {
        if (newsItem.getState() == INews.State.NEW)
          newsToUpdate.add(newsItem);
      }

      /* Support Undo */
      if (!newsToUpdate.isEmpty())
        UndoStack.getInstance().addOperation(new NewsStateOperation(newsToUpdate, INews.State.UNREAD, true));

      /* Perform Operation */
      fNewsDao.setState(newsToUpdate, INews.State.UNREAD, true, false);
    }

    /* Handle seen News: Feed Change (also closing the feed view), Closing or Minimize Event */
    else if (event == UIEvent.FEED_CHANGE || event == UIEvent.MINIMIZE || event == UIEvent.TAB_CLOSE) {

      /* Return in this case because FEED_CHANGE is also called when closing a Tab */
      if (event == UIEvent.TAB_CLOSE && !markReadOnTabClose)
        return;

      /*
       * TODO This is a workaround to avoid potential race-conditions when closing a Tab. The problem
       * is that both FEED_CHANGE (due to hiding the tab) and TAB_CLOSE (due to actually closing
       * the tab) get sent when the user closes a tab. The workaround is to delay the processing of
       * TAB_CLOSE a bit to minimize the chance of a race condition.
       */
      int delay = HANDLE_NEWS_SEEN_DELAY;
      if (event == UIEvent.TAB_CLOSE)
        delay += 50;

      JobRunner.runInBackgroundThread(delay, new Runnable() {
        public void run() {

          /* Application might be in process of closing */
          if (Controller.getDefault().isShuttingDown())
            return;

          /* Check settings if mark as read should be performed */
          boolean markRead = false;
          switch (event) {
            case FEED_CHANGE:
              markRead = markReadOnFeedChange;
              break;

            case TAB_CLOSE:
              markRead = markReadOnTabClose;
              break;

            case MINIMIZE:
              markRead = markReadOnMinimize;
              break;
          }

          /* Perform the State Change */
          List<INews> newsToUpdate = new ArrayList<INews>();
          for (INews newsItem : news) {
            if (newsItem.getState() == INews.State.NEW)
              newsToUpdate.add(newsItem);
            else if (markRead && (newsItem.getState() == INews.State.UPDATED || newsItem.getState() == INews.State.UNREAD))
              newsToUpdate.add(newsItem);
          }

          /* Force quick update on Feed-Change or Tab Close */
          if ((event == UIEvent.FEED_CHANGE || event == UIEvent.TAB_CLOSE) && !newsToUpdate.isEmpty())
            Controller.getDefault().getSavedSearchService().forceQuickUpdate();

          /* Support Undo */
          if (!newsToUpdate.isEmpty())
            UndoStack.getInstance().addOperation(new NewsStateOperation(newsToUpdate, markRead ? INews.State.READ : INews.State.UNREAD, true));

          /* Perform Operation */
          fNewsDao.setState(newsToUpdate, markRead ? INews.State.READ : INews.State.UNREAD, true, false);

          /* Retention Strategy */
          if (inputMark instanceof IBookMark)
            RetentionStrategy.process((IBookMark) inputMark, news);
        }
      });
    }
  }

  /* React on the Input being set */
  private void onInputSet() {

    /* Check if an action is to be performed */
    PerformAfterInputSet perform = fInput.getPerformOnInputSet();
    if (perform != null) {

      /* Select first News */
      if (perform.getType() == PerformAfterInputSet.Types.SELECT_FIRST_NEWS)
        navigate(false, true, true, false);

      /* Select first unread News */
      else if (perform.getType() == PerformAfterInputSet.Types.SELECT_UNREAD_NEWS)
        navigate(false, true, true, true);

      /* Select specific News */
      else if (perform.getType() == PerformAfterInputSet.Types.SELECT_SPECIFIC_NEWS)
        setSelection(new StructuredSelection(perform.getNewsToSelect()));

      /* Make sure to activate this FeedView in case of an action */
      if (perform.shouldActivate())
        fEditorSite.getPage().activate(fEditorSite.getPart());
    }

    /* DB Roundtrips done in the background */
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        if (fInput == null)
          return;

        IMark mark = fInput.getMark();

        /* Trigger a reload if this is the first time open */
        if (mark instanceof IBookMark) {
          IBookMark bookmark = (IBookMark) mark;
          if ((bookmark.getLastVisitDate() == null && !fContentProvider.hasCachedNews()))
            new ReloadTypesAction(new StructuredSelection(mark), getEditorSite().getShell()).run();
        }

        /* Update some fields due to displaying the mark */
        if (mark instanceof ISearchMark) {
          DynamicDAO.getDAO(ISearchMarkDAO.class).visited((ISearchMark) mark);
        }

        /* TODO Fixme once IBookMarkDAO.visited() is implemented */
        else if (!(mark instanceof FolderNewsMark)) {
          mark.setPopularity(mark.getPopularity() + 1);
          mark.setLastVisitDate(new Date(System.currentTimeMillis()));
          DynamicDAO.save(mark);
        }
      }
    });
  }

  /* Set Input to Viewers */
  private void setInput(final INewsMark mark, final boolean reused) {

    /* Update Cache in Background and then apply to UI */
    JobRunner.runUIUpdater(new UIBackgroundJob(fParent) {
      private IProgressMonitor fBgMonitor;

      @Override
      public boolean belongsTo(Object family) {
        return fCacheJobIdentifier.equals(family);
      }

      @Override
      protected void runInBackground(IProgressMonitor monitor) {
        fBgMonitor = monitor;
        if (!monitor.isCanceled())
          fContentProvider.refreshCache(mark, false);
      }

      @Override
      protected void runInUI(IProgressMonitor monitor) {
        IStructuredSelection oldSelection = null;
        Object value = fgSelectionCache.get(fInput.hashCode());
        if (value != null)
          oldSelection = new StructuredSelection(value);

        /* Set input to News-Table if Visible */
        if (!fBgMonitor.isCanceled() && isTableViewerVisible())
          stableSetInputToNewsTable(mark, oldSelection);

        /* Clear old Input from Table */
        else if (!fBgMonitor.isCanceled() && reused)
          fNewsTableControl.setPartInput(null);

        /* Set input to News-Browser if visible */
        if (!fBgMonitor.isCanceled() && (!isTableViewerVisible() || (BROWSER_SHOWS_ALL && oldSelection == null)))
          fNewsBrowserControl.setPartInput(mark);

        /* Reset old Input to Browser if availabel */
        else if (!fBgMonitor.isCanceled() && oldSelection != null)
          fNewsBrowserControl.setPartInput(oldSelection.getFirstElement());

        /* Clear old Input from Browser */
        else if (!fBgMonitor.isCanceled() && reused)
          fNewsBrowserControl.setPartInput(null);

        /* Update Tab now */
        if (reused)
          updateTab(fInput);

        /* Handle Input being set now */
        onInputSet();
      }
    });
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#isDirty()
   */
  @Override
  public boolean isDirty() {
    return false;
  }

  /*
   * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
   */
  @Override
  public boolean isSaveAsAllowed() {
    return false;
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    if (isTableViewerVisible())
      fNewsTableControl.setFocus();
    else
      fNewsBrowserControl.setFocus();
  }

  @Override
  public void dispose() {
    saveSettings();
    unregisterListeners();

    super.dispose();
    fContentProvider.dispose();
    fNewsTableControl.dispose();
    fNewsBrowserControl.dispose();
    fResourceManager.dispose();
  }

  private void unregisterListeners() {
    fEditorSite.getPage().removePartListener(fPartListener);
    DynamicDAO.removeEntityListener(IBookMark.class, fBookMarkListener);
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
    DynamicDAO.removeEntityListener(ISearchMark.class, fSearchMarkListener);
    DynamicDAO.removeEntityListener(IFeed.class, fFeedListener);
    DynamicDAO.removeEntityListener(ISearchCondition.class, fSearchConditionListener);
    DynamicDAO.removeEntityListener(INewsBin.class, fNewsBinListener);
  }

  /**
   * Toggle between vertical and horizontal Layout.
   * <p>
   * TODO This needs to be optimized!
   * </p>
   */
  void toggleLayout() {
    int orientation = fSashForm.getOrientation();

    /* Horizontal Alignment */
    if ((orientation & SWT.VERTICAL) != 0) {
      fBrowserSep.setVisible(fBrowserBar.isVisible());
      fSashForm.setOrientation(SWT.HORIZONTAL);
      fSashForm.setBackground(fSashForm.getDisplay().getSystemColor(SWT.COLOR_GRAY));
      ((GridData) fTableBrowserSep.getLayoutData()).exclude = true;
      fTableBrowserSep.getParent().layout();
    }

    /* Vertical Alignment (default) */
    else {
      fBrowserSep.setVisible(true);
      fSashForm.setOrientation(SWT.VERTICAL);
      fSashForm.setBackground(null);
      ((GridData) fTableBrowserSep.getLayoutData()).exclude = false;
      fTableBrowserSep.getParent().layout();
    }

    /* Update Settings */
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        fPreferences.putBoolean(DefaultPreferences.FV_LAYOUT_VERTICAL, (fSashForm.getOrientation() & SWT.VERTICAL) != 0);
      }
    });
  }

  /**
   * Toggle between maximized and normal Browser-Control.
   */
  void toggleNewsViewMaximized() {
    final boolean isMaximized = !isTableViewerVisible();

    /* Maximize Browser */
    if (!isMaximized) {
      fSashForm.setMaximizedControl(fBrowserViewerControlContainer);
      fNewsTableControl.getViewer().setSelection(StructuredSelection.EMPTY);
      fNewsBrowserControl.setPartInput(fInput.getMark());
      fNewsTableControl.setPartInput(null);
      fNewsBrowserControl.setFocus();
    }

    /* Restore Table */
    else {
      ((GridData) fBrowserSep.getLayoutData()).exclude = false;
      fSashForm.setMaximizedControl(null);
      fNewsTableControl.setPartInput(fInput.getMark());
      expandNewsTableViewerGroups(true, StructuredSelection.EMPTY);
      fNewsBrowserControl.setPartInput(null);
      fNewsTableControl.setFocus();
    }

    /* Update Settings */
    JobRunner.runInBackgroundThread(new Runnable() {
      public void run() {
        fPreferences.putBoolean(DefaultPreferences.FV_BROWSER_MAXIMIZED, !isMaximized);
      }
    });
  }

  /**
   * Refreshes all parts of this editor.
   *
   * @param delayRedraw If <code>TRUE</code> delay redraw until operation is
   * done.
   * @param updateLabels If <code>TRUE</code> update all Labels.
   */
  void refresh(boolean delayRedraw, boolean updateLabels) {
    refreshTableViewer(delayRedraw, updateLabels);
    refreshBrowserViewer();
  }

  /**
   * A special key was pressed from the Quicksearch Input-Field. Handle it.
   *
   * @param traversal The Traversal that occured from the quicksearch.
   */
  void handleQuicksearchTraversalEvent(int traversal) {

    /* Enter was hit */
    if ((traversal & SWT.TRAVERSE_RETURN) != 0) {

      /* Select and Focus TreeViewer */
      if (isTableViewerVisible()) {
        Tree tree = (Tree) fNewsTableControl.getViewer().getControl();
        if (tree.getItemCount() > 0) {
          fNewsTableControl.getViewer().setSelection(new StructuredSelection(tree.getItem(0).getData()));
          fNewsTableControl.setFocus();
        }
      }

      /* Move Focus into BrowserViewer */
      else {
        fNewsBrowserControl.setFocus();
      }
    }

    /* Page Up / Down was hit */
    else if ((traversal & SWT.TRAVERSE_PAGE_NEXT) != 0 || (traversal & SWT.TRAVERSE_PAGE_PREVIOUS) != 0) {
      setFocus();
    }
  }

  /* Refresh Table-Viewer if visible */
  private void refreshTableViewer(boolean delayRedraw, boolean updateLabels) {
    if (isTableViewerVisible()) {
      boolean groupingEnabled = fNewsGrouping.getType() != NewsGrouping.Type.NO_GROUPING;

      /* Remember Selection if grouping enabled */
      ISelection selection = StructuredSelection.EMPTY;
      if (groupingEnabled)
        selection = fNewsTableControl.getViewer().getSelection();

      /* Delay redraw operations if requested */
      if (delayRedraw)
        fNewsTableControl.getViewer().getControl().getParent().setRedraw(false);
      try {

        /* Refresh */
        fNewsTableControl.getViewer().refresh(updateLabels);

        /* Expand all Groups if grouping is enabled */
        if (groupingEnabled)
          expandNewsTableViewerGroups(false, selection);
      }

      /* Redraw now if delayed before */
      finally {
        if (delayRedraw)
          fNewsTableControl.getViewer().getControl().getParent().setRedraw(true);
      }
    }
  }

  private void expandNewsTableViewerGroups(boolean delayRedraw, ISelection oldSelection) {
    TreeViewer viewer = fNewsTableControl.getViewer();
    Tree tree = (Tree) viewer.getControl();

    /* Remember TopItem if required */
    TreeItem topItem = oldSelection.isEmpty() ? tree.getTopItem() : null;

    /* Expand All & Restore Selection with redraw false */
    if (delayRedraw)
      tree.getParent().setRedraw(false);
    try {
      viewer.expandAll();

      /* Restore selection if required */
      if (!oldSelection.isEmpty() && viewer.getSelection().isEmpty())
        viewer.setSelection(oldSelection, true);
      else if (topItem != null)
        tree.setTopItem(topItem);
    } finally {
      if (delayRedraw)
        tree.getParent().setRedraw(true);
    }
  }

  /* TODO This is a Workaround until Eclipse Bug #159586 is fixed */
  private void stableSetInputToNewsTable(Object input, ISelection oldSelection) {
    TreeViewer viewer = fNewsTableControl.getViewer();
    Tree tree = (Tree) viewer.getControl();

    /* Set Input & Restore Selection with redraw false */
    tree.getParent().setRedraw(false);
    try {
      fNewsTableControl.setPartInput(input);

      /* Restore selection if required */
      if (oldSelection != null) {
        fNewsTableControl.setBlockNewsStateTracker(true);
        try {
          viewer.setSelection(oldSelection);
        } finally {
          fNewsTableControl.setBlockNewsStateTracker(false);
        }
      }

      /* Set Top Item */
      if (tree.getItemCount() > 0)
        tree.setTopItem(tree.getItem(0));
    } finally {
      tree.getParent().setRedraw(true);
    }
  }

  private void rememberSelection() {
    IStructuredSelection sel = (IStructuredSelection) fNewsTableControl.getViewer().getSelection();
    if (!sel.isEmpty()) {
      Object obj = sel.getFirstElement();
      if (obj instanceof INews)
        fgSelectionCache.put(fInput.hashCode(), new NewsReference(((INews) obj).getId()));
      else
        fgSelectionCache.remove(fInput.hashCode());
    }
  }

  /* Refresh Browser-Viewer */
  private void refreshBrowserViewer() {
    fNewsBrowserControl.getViewer().refresh();
  }

  /**
   * Check wether the News-Table-Part of this Editor is visible or not
   * (minmized).
   *
   * @return TRUE if the News-Table-Part is visible, FALSE otherwise.
   */
  boolean isTableViewerVisible() {
    return fSashForm.getMaximizedControl() == null;
  }

  /**
   * Get the shared ViewerFilter used to filter News.
   *
   * @return the shared ViewerFilter used to filter News.
   */
  NewsFilter getFilter() {
    return fNewsFilter;
  }

  /**
   * Get the shared Viewer-Grouper used to group News.
   *
   * @return the shared Viewer-Grouper used to group News.
   */
  NewsGrouping getGrouper() {
    return fNewsGrouping;
  }

  NewsBrowserControl getNewsBrowserControl() {
    return fNewsBrowserControl;
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    fCreated = true;
    fParent = parent;

    /* Shared Viewer Helper */
    fNewsFilter = new NewsFilter();
    fNewsFilter.setType(fInitialFilterType);
    fNewsFilter.setSearchTarget(fInitialSearchTarget);
    fNewsFilter.setNewsMark(fInput.getMark());

    fNewsGrouping = new NewsGrouping();
    fNewsGrouping.setType(fInitialGroupType);

    /* Top-Most root Composite in Editor */
    Composite rootComposite = new Composite(fParent, SWT.NONE);
    rootComposite.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    ((GridLayout) rootComposite.getLayout()).verticalSpacing = 0;

    /* FilterBar */
    fFilterBar = new FilterBar(this, rootComposite);

    /* Separate from SashForm */
    Label sep = new Label(rootComposite, SWT.SEPARATOR | SWT.HORIZONTAL);
    sep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* SashForm dividing Feed and News View */
    fSashForm = new SashForm(rootComposite, (fInitialLayoutVertical ? SWT.VERTICAL : SWT.HORIZONTAL) | SWT.SMOOTH);
    fSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    if (!fInitialLayoutVertical)
      fSashForm.setBackground(fSashForm.getDisplay().getSystemColor(SWT.COLOR_GRAY));

    /* Table-Viewer to display headlines */
    NewsTableViewer tableViewer;
    {
      Composite container = new Composite(fSashForm, SWT.None);
      container.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 5, false));
      container.addControlListener(new ControlAdapter() {
        @Override
        public void controlResized(ControlEvent e) {
          fCacheWeights = fSashForm.getWeights();
        }
      });

      fNewsTableControl = new NewsTableControl();
      fNewsTableControl.init(fEditorSite);
      fNewsTableControl.onInputChanged(fInput);

      /* Create Viewer */
      tableViewer = fNewsTableControl.createViewer(container);

      /* Clear any quicksearch when ESC is hit from the Tree */
      tableViewer.getControl().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ESC)
            fFilterBar.clearQuickSearch(true);
        }
      });

      /* Separate from Browser-Viewer */
      fTableBrowserSep = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
      fTableBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      ((GridData) fTableBrowserSep.getLayoutData()).exclude = !fInitialLayoutVertical;
    }

    /* Browser-Viewer to display news */
    NewsBrowserViewer browserViewer;
    {
      fBrowserViewerControlContainer = new Composite(fSashForm, SWT.None);
      fBrowserViewerControlContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 0, 5, false));
      fBrowserViewerControlContainer.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

      /* Browser Bar for Navigation */
      fBrowserBar = new BrowserBar(this, fBrowserViewerControlContainer);

      /* Separate to Browser */
      fBrowserSep = new Label(fBrowserViewerControlContainer, SWT.SEPARATOR | SWT.HORIZONTAL);
      fBrowserSep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      ((GridData) fBrowserSep.getLayoutData()).exclude = fInitialBrowserMaximized && !fBrowserBar.isVisible();
      fBrowserSep.setVisible(fBrowserBar.isVisible() || (fSashForm.getOrientation() & SWT.VERTICAL) != 0);

      fNewsBrowserControl = new NewsBrowserControl();
      fNewsBrowserControl.init(fEditorSite);
      fNewsBrowserControl.onInputChanged(fInput);

      /* Create Viewer */
      browserViewer = fNewsBrowserControl.createViewer(fBrowserViewerControlContainer);

      /* Clear any quicksearch when ESC is hit from the Tree */
      browserViewer.getControl().addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if (e.keyCode == SWT.ESC)
            fFilterBar.clearQuickSearch(true);
        }
      });

      /* Init the Browser Bar with the CBrowser */
      fBrowserBar.init(browserViewer.getBrowser());
    }

    /* SashForm weights */
    fSashForm.setWeights(fInitialWeights);
    if (fInitialBrowserMaximized)
      fSashForm.setMaximizedControl(fBrowserViewerControlContainer);

    /* Create the shared Content-Provider */
    fContentProvider = new NewsContentProvider(tableViewer, browserViewer, this);

    /* Init all Viewers */
    fNewsTableControl.initViewer(fContentProvider, fNewsFilter);
    fNewsBrowserControl.initViewer(fContentProvider, fNewsFilter);

    /* Set Input to Viewers */
    setInput(fInput.getMark(), false);
  }

  /**
   * Navigate to the next/previous read or unread News respecting the News-Items
   * that are displayed in the NewsTableControl.
   *
   * @param respectSelection If <code>TRUE</code>, respect the current
   * selected Item from the Tree as starting-node for the navigation, or
   * <code>FALSE</code> otherwise.
   * @param newsScoped If <code>TRUE</code>, the navigation looks for News
   * and for Feeds if <code>FALSE</code>.
   * @param next If <code>TRUE</code>, move to the next item, or previous if
   * <code>FALSE</code>.
   * @param unread If <code>TRUE</code>, only move to unread items, or ignore
   * if <code>FALSE</code>.
   * @return Returns <code>TRUE</code> in case navigation found a valid item,
   * or <code>FALSE</code> otherwise.
   */
  public boolean navigate(boolean respectSelection, boolean newsScoped, boolean next, boolean unread) {

    /* Check for unread counter */
    if (unread && fInput.getMark().getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)) == 0) {
      return false;
    }

    /* Don't update Selection in Tree, since this is not News-Scoped */
    if (!newsScoped)
      return true;

    /* TODO Better support for maximized Browser */
    if (!isTableViewerVisible())
      return true;

    Tree newsTree = fNewsTableControl.getViewer().getTree();

    /* Nothing to Navigate to */
    if (newsTree.getItemCount() == 0 || newsTree.isDisposed())
      return false;

    /* Navigate */
    return navigate(newsTree, respectSelection, next, unread);
  }

  private boolean navigate(Tree tree, boolean respectSelection, boolean next, boolean unread) {

    /* Selection is Present */
    if (respectSelection && tree.getSelectionCount() > 0) {

      /* Try navigating from Selection */
      ITreeNode startingNode = new WidgetTreeNode(tree.getSelection()[0], fNewsTableControl.getViewer());
      if (navigate(startingNode, next, unread))
        return true;
    }

    /* No Selection is Present */
    else {
      ITreeNode startingNode = new WidgetTreeNode(tree, fNewsTableControl.getViewer());
      return navigate(startingNode, true, unread);
    }

    return false;
  }

  private boolean navigate(ITreeNode startingNode, boolean next, final boolean unread) {

    /* Create Traverse-Helper */
    TreeTraversal traverse = new TreeTraversal(startingNode) {
      @Override
      public boolean select(ITreeNode node) {
        return isValidNavigation(node, unread);
      }
    };

    /* Retrieve and select new Target Node */
    ITreeNode targetNode = (next ? traverse.nextNode() : traverse.previousNode());
    if (targetNode != null) {
      ISelection selection = new StructuredSelection(targetNode.getData());
      fNewsTableControl.getViewer().setSelection(selection);
      return true;
    }

    return false;
  }

  private boolean isValidNavigation(ITreeNode node, boolean unread) {
    Object data = node.getData();

    /* Require a News */
    if (!(data instanceof INews))
      return false;

    /* Check if News is unread if set as flag */
    INews news = (INews) data;
    if (unread && !ModelUtils.isUnread(news.getState()))
      return false;

    return true;
  }

  /**
   * Returns the <code>Composite</code> that is the Parent Control of this
   * Editor Part.
   *
   * @return The <code>Composite</code> that is the Parent Control of this
   * Editor Part.
   */
  Composite getEditorControl() {
    return fParent;
  }
}