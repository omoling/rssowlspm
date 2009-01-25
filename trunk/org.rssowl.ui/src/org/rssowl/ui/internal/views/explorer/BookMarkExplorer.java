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

package org.rssowl.ui.internal.views.explorer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.IInputProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.URLTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.IPreference;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.event.FolderAdapter;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.persist.event.FolderListener;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.StatusLineUpdater;
import org.rssowl.ui.internal.actions.DeleteTypesAction;
import org.rssowl.ui.internal.actions.EntityPropertyDialogAction;
import org.rssowl.ui.internal.actions.NewBookMarkAction;
import org.rssowl.ui.internal.actions.NewFolderAction;
import org.rssowl.ui.internal.actions.NewNewsBinAction;
import org.rssowl.ui.internal.actions.NewSearchMarkAction;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.actions.RetargetActions;
import org.rssowl.ui.internal.actions.SearchInTypeAction;
import org.rssowl.ui.internal.dialogs.ManageSetsDialog;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.editors.feed.PerformAfterInputSet;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.util.ITreeNode;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.ModelTreeNode;
import org.rssowl.ui.internal.util.TreeTraversal;
import org.rssowl.ui.internal.util.WidgetTreeNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>
 * TODO The underlying TreeViewer is not yet supporting contributed
 * Label-Decorator, because the used LabelProvider is not extending
 * ILabelProvider. Add this code to provide the decorations from contributions:
 * <code>
 * ILabelDecorator decorator = PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator();
 * fViewer.setLabelProvider(new DecoratingLabelProvider(new BookMarkLabelProvider(), decorator);
 * </code>
 * </p>
 *
 * @author bpasero
 */
public class BookMarkExplorer extends ViewPart {

  /** ID of this View */
  public static final String VIEW_ID = "org.rssowl.ui.BookMarkExplorer"; //$NON-NLS-1$

  /** IDs of Action: Next Bookmark-Set */
  public static final String NEXT_SET_ACTION = "org.rssowl.ui.internal.views.explorer.NextSetAction"; //$NON-NLS-1$

  /** IDs of Action: Previous Bookmark-Set */
  public static final String PREVIOUS_SET_ACTION = "org.rssowl.ui.internal.views.explorer.PreviousSetAction"; //$NON-NLS-1$

  /* Local Setting Constants */
  private static final String PREF_SELECTED_BOOKMARK_SET = "org.rssowl.ui.internal.views.explorer.SelectedBookMarkSet"; //$NON-NLS-1$
  private static final String PREF_EXPANDED_NODES = "org.rssowl.ui.internal.views.explorer.ExpandedNodes"; //$NON-NLS-1$

  /* Local Actions */
  private static final String GROUP_ACTION = "org.rssowl.ui.internal.views.explorer.GroupAction";
  private static final String FILTER_ACTION = "org.rssowl.ui.internal.views.explorer.FilterAction";

  /* Settings */
  private IPreferenceScope fGlobalPreferences;
  private List<Long> fExpandedNodes;
  private boolean fBeginSearchOnTyping;
  private boolean fAlwaysShowSearch;
  private boolean fSortByName;
  private BookMarkFilter.Type fFilterType;
  private BookMarkGrouping.Type fGroupingType;
  private IFolder fSelectedBookMarkSet;
  private boolean fLinkingEnabled;
  private boolean fFaviconsEnabled;

  /* Viewer Classes */
  private TreeViewer fViewer;
  private IContentProvider fContentProvider;
  private BookMarkLabelProvider fLabelProvider;
  private BookMarkSorter fBookMarkComparator;
  private BookMarkFilter fBookMarkFilter;
  private BookMarkGrouping fBookMarkGrouping;

  /* Widgets */
  private Label fSeparator;
  private Composite fSearchBarContainer;
  private BookMarkSearchbar fSearchBar;
  private ToolBarManager fToolBarManager;

  /* BookMark Sets */
  private Set<IFolder> fRootFolders;

  /* Misc. */
  private IViewSite fViewSite;
  private FolderListener fFolderListener;
  private IPartListener2 fPartListener;
  private IPreferenceDAO fPrefDAO;
  private IPropertyChangeListener fPropertyChangeListener;

  /**
   * Returns the preferences key for the selected bookmark set for the given
   * workbench window.
   *
   * @param window the active workbench window.
   * @return the preferences key for the selected bookmark set for the given
   * workbench window.
   */
  public static String getSelectedBookMarkSetPref(IWorkbenchWindow window) {
    int windowIndex = OwlUI.getWindowIndex(window);
    return PREF_SELECTED_BOOKMARK_SET + windowIndex;
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  @Override
  public void createPartControl(Composite parent) {

    /* Update Parent */
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    layout.verticalSpacing = 0;

    parent.setLayout(layout);
    parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    /* Create TreeViewer */
    createViewer(parent);

    /* Restore Expanded Elements */
    restoreExpandedElements();

    /* Hook into Statusline */
    fViewer.addSelectionChangedListener(new StatusLineUpdater(getViewSite().getActionBars().getStatusLineManager()));

    /* Hook into Global Actions */
    hookGlobalActions();

    /* Hook contextual Menu */
    hookContextualMenu();

    /* Hook into Toolbar */
    hookToolBar();

    /* Hook into View Dropdown */
    hookViewMenu();

    /* Register Listeners */
    registerListeners();

    /* Propagate Selection Events */
    fViewSite.setSelectionProvider(fViewer);

    /* Create the Search Bar */
    createSearchBar(parent);

    /* Show Busy when reload occurs */
    IWorkbenchSiteProgressService service = (IWorkbenchSiteProgressService) fViewSite.getAdapter(IWorkbenchSiteProgressService.class);
    service.showBusyForFamily(Controller.getDefault().getReloadFamily());
  }

  private void createViewer(Composite parent) {

    /* TreeViewer */
    fViewer = new BookMarkViewer(this, parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    fViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.getControl().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());
    fViewer.getControl().setFont(OwlUI.getThemeFont(OwlUI.BKMRK_EXPLORER_FONT_ID, SWT.NORMAL));

    /* Setup Drag & Drop Support */
    initDragAndDrop();

    /* Add a custom Comparer */
    fViewer.setComparer(getComparer());
    fViewer.setUseHashlookup(true);

    /* Create ContentProvider */
    fContentProvider = new BookMarkContentProvider();
    fViewer.setContentProvider(fContentProvider);

    /* Create LabelProvider */
    fLabelProvider = new BookMarkLabelProvider();
    fLabelProvider.setUseFavicons(fFaviconsEnabled);
    fViewer.setLabelProvider(fLabelProvider);

    /* Apply Sorter */
    fBookMarkComparator = new BookMarkSorter();
    if (fSortByName)
      fBookMarkComparator.setType(BookMarkSorter.Type.SORT_BY_NAME);
    fViewer.setComparator(fBookMarkComparator);

    /* Apply Filter */
    fBookMarkFilter = new BookMarkFilter();
    fBookMarkFilter.setType(fFilterType);
    fViewer.addFilter(fBookMarkFilter);

    /* Create Grouper */
    fBookMarkGrouping = new BookMarkGrouping();
    fBookMarkGrouping.setType(fGroupingType);

    /* Let the ContentProvider know */
    ((BookMarkContentProvider) fContentProvider).setBookmarkFilter(fBookMarkFilter);
    ((BookMarkContentProvider) fContentProvider).setBookmarkGrouping(fBookMarkGrouping);

    /* Set the initial Input based on selected Bookmark Set */
    fViewer.setInput(fSelectedBookMarkSet);

    /* Update Saved Searches if not yet done */
    JobRunner.runInBackgroundThread(50, new Runnable() {
      public void run() {
        Controller.getDefault().getSavedSearchService().updateSavedSearches(false);
      }
    });

    /* Enable "Link to FeedView" */
    fViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        onSelectionChanged(event);
      }
    });

    /* Hook Doubleclick Support */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        onDoubleClick(event);
      }
    });

    /* Hook Open Support */
    fViewer.addOpenListener(new IOpenListener() {
      public void open(OpenEvent event) {
        OwlUI.openInFeedView(fViewSite.getPage(), (IStructuredSelection) fViewer.getSelection());
      }
    });

    /* Update List of Expanded Nodes */
    fViewer.addTreeListener(new ITreeViewerListener() {
      public void treeExpanded(TreeExpansionEvent event) {
        onTreeEvent(event.getElement(), true);
      }

      public void treeCollapsed(TreeExpansionEvent event) {
        onTreeEvent(event.getElement(), false);
      }
    });

    /* Link if enabled */
    if (fLinkingEnabled) {
      IWorkbenchPart activePart = fViewSite.getPage().getActivePart();
      if (activePart instanceof IEditorPart)
        editorActivated((IEditorPart) activePart);
    }
  }

  private void initDragAndDrop() {
    int ops = DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK;
    Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance(), URLTransfer.getInstance() };
    BookMarkDNDImpl bookmarkDND = new BookMarkDNDImpl(this, fViewer);

    /* Drag Support */
    fViewer.addDragSupport(ops, transfers, bookmarkDND);

    /* Drop Support */
    fViewer.addDropSupport(ops, transfers, bookmarkDND);
  }

  private void onSelectionChanged(SelectionChangedEvent event) {
    if (fLinkingEnabled) {
      ISelection selection = event.getSelection();
      linkToFeedView((IStructuredSelection) selection);
    }
  }

  private void onDoubleClick(DoubleClickEvent event) {
    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
    Object firstElem = selection.getFirstElement();

    /* Expand / Collapse Folders */
    if (firstElem instanceof IFolder || firstElem instanceof EntityGroup) {
      boolean expandedState = !fViewer.getExpandedState(firstElem);
      fViewer.setExpandedState(firstElem, expandedState);
      onTreeEvent(firstElem, expandedState);
    }
  }

  private void onTreeEvent(Object element, boolean expanded) {

    /* Element expanded - add to List of expanded Nodes */
    if (expanded) {
      if (element instanceof IFolder)
        fExpandedNodes.add(((IFolder) element).getId());
      else if (element instanceof EntityGroup)
        fExpandedNodes.add(((EntityGroup) element).getId());
    }

    /* Element collapsed - remove from List of expanded Nodes */
    else {
      if (element instanceof IFolder)
        fExpandedNodes.remove(((IFolder) element).getId());
      else if (element instanceof EntityGroup)
        fExpandedNodes.remove(((EntityGroup) element).getId());
    }
  }

  /*
   * This Comparer is used to optimize some operations on the Viewer being used.
   * When deleting Entities, the Delete-Event is providing a reference to the
   * deleted Entity, which can not be resolved anymore. This Comparer will
   * return <code>TRUE</code> for a reference compared with an Entity that has
   * the same ID and is belonging to the same Entity. At any time, it _must_ be
   * avoided to call add, update or refresh with passing in a Reference!
   */
  private IElementComparer getComparer() {
    return new IElementComparer() {
      public boolean equals(Object a, Object b) {

        /* Quickyly check this common case */
        if (a == b && a != null)
          return true;

        /* Specially handle this reference */
        if (a instanceof FeedLinkReference || b instanceof FeedLinkReference) {
          FeedLinkReference ref1 = null;
          FeedLinkReference ref2 = null;

          if (a instanceof IBookMark)
            ref1 = ((IBookMark) a).getFeedLinkReference();
          else if (a instanceof FeedLinkReference)
            ref1 = ((FeedLinkReference) a);

          if (b instanceof IBookMark)
            ref2 = ((IBookMark) b).getFeedLinkReference();
          else if (b instanceof FeedLinkReference)
            ref2 = ((FeedLinkReference) b);

          if (ref1 != null)
            return ref1.equals(ref2);

          return false;
        }

        /* Handle Non Feed-Link-Reference */
        long id1 = 0;
        long id2 = 0;

        if (a instanceof IEntity)
          id1 = ((IEntity) a).getId();
        else if (a instanceof ModelReference)
          id1 = ((ModelReference) a).getId();
        else if (a instanceof EntityGroup)
          id1 = ((EntityGroup) a).getId();

        if (b instanceof IEntity)
          id2 = ((IEntity) b).getId();
        else if (b instanceof ModelReference)
          id2 = ((ModelReference) b).getId();
        else if (b instanceof EntityGroup)
          id2 = ((EntityGroup) b).getId();

        return id1 == id2;
      }

      public int hashCode(Object element) {
        return element.hashCode();
      }
    };
  }

  private void linkToFeedView(IStructuredSelection selection) {

    /* Only Link if this is the active Part */
    if (this != fViewSite.getPage().getActivePart())
      return;

    /* Only Link for Single Selections */
    if (selection.size() == 1) {
      Object element = selection.getFirstElement();

      /* Find the Editor showing given Selection */
      IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      IEditorReference[] editorReferences = activePage.getEditorReferences();
      IEditorReference reference = EditorUtils.findEditor(editorReferences, element);
      if (reference != null)
        activePage.bringToTop(reference.getPart(false));
    }
  }

  private void editorActivated(IEditorPart part) {
    if (!fLinkingEnabled || !fViewSite.getPage().isPartVisible(this) || part == null)
      return;

    /* Try to select and reveal editor input in the Explorer */
    IEditorInput editorInput = part.getEditorInput();
    if (editorInput instanceof FeedViewInput) {
      FeedViewInput feedViewInput = (FeedViewInput) editorInput;
      IMark mark = feedViewInput.getMark();

      /* Change Set if required */
      IFolderChild child = mark;
      while (child.getParent() != null)
        child = child.getParent();

      if (!fSelectedBookMarkSet.equals(child))
        changeSet((IFolder) child);

      /* Set Selection */
      fViewer.setSelection(new StructuredSelection(mark), true);
    }
  }

  void changeSet(IFolder folder) {

    /* Save Expanded Elements */
    saveExpandedElements();

    /* Set new Input */
    fSelectedBookMarkSet = folder;
    fViewer.setInput(fSelectedBookMarkSet);

    /* Restore Expanded Elements */
    fExpandedNodes.clear();
    loadExpandedElements();
    fViewer.getControl().setRedraw(false);
    try {
      restoreExpandedElements();
    } finally {
      fViewer.getControl().setRedraw(true);
    }

    /* Update Set Actions */
    fViewSite.getActionBars().getToolBarManager().find(PREVIOUS_SET_ACTION).update(IAction.ENABLED);
    fViewSite.getActionBars().getToolBarManager().find(NEXT_SET_ACTION).update(IAction.ENABLED);

    /* Save the new selected Set in Preferences */
    IPreference pref = fPrefDAO.loadOrCreate(getSelectedBookMarkSetPref(fViewSite.getWorkbenchWindow()));
    pref.putLongs(fSelectedBookMarkSet.getId());
    fPrefDAO.save(pref);
  }

  private void createSearchBar(final Composite parent) {

    /* Add Separator between Tree and Search Bar */
    fSeparator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
    fSeparator.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Container for the SearchBar */
    fSearchBarContainer = new Composite(parent, SWT.NONE);
    fSearchBarContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Hide Searchbar in case settings tell so */
    ((GridData) fSeparator.getLayoutData()).exclude = !fAlwaysShowSearch;
    ((GridData) fSearchBarContainer.getLayoutData()).exclude = !fAlwaysShowSearch;

    /* Apply Layout */
    GridLayout searchBarLayout = new GridLayout(1, false);
    searchBarLayout.marginHeight = 2;
    searchBarLayout.marginWidth = 2;
    fSearchBarContainer.setLayout(searchBarLayout);

    /* Create the SearchBar */
    fSearchBar = new BookMarkSearchbar(fViewSite, fSearchBarContainer, fViewer, fBookMarkFilter);

    /* Show the SearchBar on Printable Key pressed */
    fViewer.getControl().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {

        /* Feature not Used, return */
        if (!fBeginSearchOnTyping)
          return;

        /* Transfer typed key into SearchBar */
        if (e.character > 0x20 && e.character != SWT.DEL) {
          setSearchBarVisible(true);

          fSearchBar.getControl().append(String.valueOf(e.character));
          fSearchBar.getControl().setFocus();

          /* Consume the Event */
          e.doit = false;
        }

        /* Reset any Filter if set */
        else if (e.keyCode == SWT.ESC && fSearchBar.getControl().getText().length() != 0) {
          fSearchBar.setFilterText(""); //$NON-NLS-1$
          setSearchBarVisible(fAlwaysShowSearch);
          setFocus();
        }
      }
    });

    /* Hide SearchBar if search is done */
    fSearchBar.getControl().addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {

        /* Feature not Used, return */
        if (fAlwaysShowSearch)
          return;

        /* Search is Done */
        if (fSearchBar.getControl().getText().length() == 0) {
          setSearchBarVisible(false);
          setFocus();
        }
      }
    });
  }

  void setSearchBarVisible(boolean visible) {

    /* Return if no State Change */
    if (visible != ((GridData) fSeparator.getLayoutData()).exclude)
      return;

    /* Update LayoutData and layout Parent */
    ((GridData) fSeparator.getLayoutData()).exclude = !visible;
    ((GridData) fSearchBarContainer.getLayoutData()).exclude = !visible;
    fSearchBarContainer.getParent().layout();
  }

  private void hookViewMenu() {
    IMenuManager menuManager = fViewSite.getActionBars().getMenuManager();
    menuManager.setRemoveAllWhenShown(true);
    menuManager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {

        /* Manage Bookmark Sets */
        IAction manageSets = new Action("Manage Bookmark-Sets...") {
          @Override
          public void run() {
            ManageSetsDialog instance = ManageSetsDialog.getVisibleInstance();
            if (instance == null) {
              ManageSetsDialog dialog = new ManageSetsDialog(fViewSite.getShell(), fSelectedBookMarkSet);
              dialog.open();
            } else {
              instance.getShell().forceActive();
            }
          }
        };
        manager.add(manageSets);

        /* Available Bookmark Sets */
        manager.add(new Separator());
        for (final IFolder rootFolder : fRootFolders) {
          IAction selectBookMarkSet = new Action(rootFolder.getName(), IAction.AS_RADIO_BUTTON) {
            @Override
            public void run() {
              if (!fSelectedBookMarkSet.equals(rootFolder) && isChecked())
                changeSet(rootFolder);
            }
          };
          selectBookMarkSet.setImageDescriptor(OwlUI.BOOKMARK_SET); //$NON-NLS-1$

          if (fSelectedBookMarkSet.equals(rootFolder))
            selectBookMarkSet.setChecked(true);

          manager.add(selectBookMarkSet);
        }

        /* Search Bar */
        manager.add(new Separator());
        MenuManager searchMenu = new MenuManager("Find");
        manager.add(searchMenu);

        /* Search Bar - Always Show Bar */
        IAction alwaysShow = new Action("Always Show", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            fAlwaysShowSearch = !fAlwaysShowSearch;

            /* Only Update if the Filter is not Active */
            if (fSearchBar.getControl().getText().length() == 0)
              setSearchBarVisible(fAlwaysShowSearch);
          }
        };
        alwaysShow.setChecked(fAlwaysShowSearch);
        searchMenu.add(alwaysShow);

        /* Search Bar - Begin Search when Typing */
        IAction beginWhenTyping = new Action("Begin When Typing", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            fBeginSearchOnTyping = !fBeginSearchOnTyping;
          }
        };
        beginWhenTyping.setChecked(fBeginSearchOnTyping);
        searchMenu.add(beginWhenTyping);

        /* Misc. Settings */
        manager.add(new Separator());
        IAction sortByName = new Action("Sort By Name", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            fSortByName = !fSortByName;
            if (fSortByName)
              fBookMarkComparator.setType(BookMarkSorter.Type.SORT_BY_NAME);
            else
              fBookMarkComparator.setType(BookMarkSorter.Type.DEFAULT_SORTING);
            fViewer.refresh(false);

            /* Save directly to global scope */
            fGlobalPreferences.putBoolean(DefaultPreferences.BE_SORT_BY_NAME, fSortByName);
          }
        };
        sortByName.setChecked(fSortByName);
        manager.add(sortByName);

        IAction showFavicons = new Action("Show Feed Icons", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            fFaviconsEnabled = isChecked();

            fLabelProvider.setUseFavicons(fFaviconsEnabled);
            fViewer.getTree().setRedraw(false);
            try {
              fViewer.refresh(true);
            } finally {
              fViewer.getTree().setRedraw(true);
            }
          }
        };
        showFavicons.setChecked(fFaviconsEnabled);
        manager.add(showFavicons);

        /* Allow Contributions */
        manager.add(new Separator());

        IAction linkFeedView = new Action("Link with Feed-View", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            fLinkingEnabled = isChecked();

            /* Link if enabled */
            if (fLinkingEnabled) {
              IEditorPart editor = fViewSite.getPage().getActiveEditor();
              if (editor != null)
                editorActivated(editor);
            }
          }
        };
        linkFeedView.setChecked(fLinkingEnabled);
        linkFeedView.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/synced.gif"));
        manager.add(linkFeedView);

        /* Allow Contributions */
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
      }
    });

    /* Dummy Entry to show Menu in View */
    menuManager.add(new Action("") {}); //$NON-NLS-1$
  }

  private void hookToolBar() {
    fToolBarManager = (ToolBarManager) fViewSite.getActionBars().getToolBarManager();

    /* BookMark Filter */
    final IAction bookmarkFilter = new Action("Filter Bookmarks", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {

        /* Restore Default */
        if (fBookMarkFilter.getType() != BookMarkFilter.Type.SHOW_ALL)
          doFilter(BookMarkFilter.Type.SHOW_ALL);

        /* Show Menu */
        else
          getMenuCreator().getMenu(fToolBarManager.getControl()).setVisible(true);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        if (fBookMarkFilter.getType() == BookMarkFilter.Type.SHOW_ALL)
          return OwlUI.getImageDescriptor("icons/etool16/filter.gif"); //$NON-NLS-1$

        return OwlUI.getImageDescriptor("icons/etool16/filter_active.gif"); //$NON-NLS-1$
      }
    };
    bookmarkFilter.setId(FILTER_ACTION);

    bookmarkFilter.setMenuCreator(new IMenuCreator() {
      public void dispose() {}

      public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Filter: None */
        final MenuItem showAll = new MenuItem(menu, SWT.RADIO);
        showAll.setText("Show All");
        showAll.setSelection(BookMarkFilter.Type.SHOW_ALL == fBookMarkFilter.getType());
        menu.setDefaultItem(showAll);
        showAll.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showAll.getSelection() && fBookMarkFilter.getType() != BookMarkFilter.Type.SHOW_ALL)
              doFilter(BookMarkFilter.Type.SHOW_ALL);
          }
        });

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Filter: New */
        final MenuItem showNew = new MenuItem(menu, SWT.RADIO);
        showNew.setText("Show New");
        showNew.setSelection(BookMarkFilter.Type.SHOW_NEW == fBookMarkFilter.getType());
        showNew.addSelectionListener(new SelectionAdapter() {

          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showNew.getSelection() && fBookMarkFilter.getType() != BookMarkFilter.Type.SHOW_NEW)
              doFilter(BookMarkFilter.Type.SHOW_NEW);
          }
        });

        /* Filter: Unread */
        final MenuItem showUnread = new MenuItem(menu, SWT.RADIO);
        showUnread.setText("Show Unread");
        showUnread.setSelection(BookMarkFilter.Type.SHOW_UNREAD == fBookMarkFilter.getType());
        showUnread.addSelectionListener(new SelectionAdapter() {

          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showUnread.getSelection() && fBookMarkFilter.getType() != BookMarkFilter.Type.SHOW_UNREAD)
              doFilter(BookMarkFilter.Type.SHOW_UNREAD);
          }
        });

        /* Filter: Sticky */
        final MenuItem showSticky = new MenuItem(menu, SWT.RADIO);
        showSticky.setText("Show Sticky");
        showSticky.setSelection(BookMarkFilter.Type.SHOW_STICKY == fBookMarkFilter.getType());
        showSticky.addSelectionListener(new SelectionAdapter() {

          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showSticky.getSelection() && fBookMarkFilter.getType() != BookMarkFilter.Type.SHOW_STICKY)
              doFilter(BookMarkFilter.Type.SHOW_STICKY);
          }
        });

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Filter: Erroneous */
        final MenuItem showErroneous = new MenuItem(menu, SWT.RADIO);
        showErroneous.setText("Show Erroneous");
        showErroneous.setSelection(BookMarkFilter.Type.SHOW_ERRONEOUS == fBookMarkFilter.getType());
        showErroneous.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showErroneous.getSelection() && fBookMarkFilter.getType() != BookMarkFilter.Type.SHOW_ERRONEOUS)
              doFilter(BookMarkFilter.Type.SHOW_ERRONEOUS);
          }
        });

        /* Filter: Never Visited */
        final MenuItem showNeverVisited = new MenuItem(menu, SWT.RADIO);
        showNeverVisited.setText("Show Never Visited");
        showNeverVisited.setSelection(BookMarkFilter.Type.SHOW_NEVER_VISITED == fBookMarkFilter.getType());
        showNeverVisited.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (showNeverVisited.getSelection() && fBookMarkFilter.getType() != BookMarkFilter.Type.SHOW_NEVER_VISITED)
              doFilter(BookMarkFilter.Type.SHOW_NEVER_VISITED);
          }
        });

        return menu;
      }

      public Menu getMenu(Menu parent) {
        return null;
      }
    });

    fToolBarManager.add(bookmarkFilter);

    /* Bookmark Group */
    fToolBarManager.add(new Separator());
    final IAction bookmarkGroup = new Action("Group Bookmarks", IAction.AS_DROP_DOWN_MENU) {
      @Override
      public void run() {

        /* Restore Default */
        if (fBookMarkGrouping.getType() != BookMarkGrouping.Type.NO_GROUPING)
          doGrouping(BookMarkGrouping.Type.NO_GROUPING);

        /* Show Menu */
        else
          getMenuCreator().getMenu(fToolBarManager.getControl()).setVisible(true);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        if (fBookMarkGrouping.getType() == BookMarkGrouping.Type.NO_GROUPING)
          return OwlUI.getImageDescriptor("icons/etool16/group.gif"); //$NON-NLS-1$

        return OwlUI.getImageDescriptor("icons/etool16/group_active.gif"); //$NON-NLS-1$
      }
    };
    bookmarkGroup.setId(GROUP_ACTION);

    bookmarkGroup.setMenuCreator(new IMenuCreator() {
      public void dispose() {}

      public Menu getMenu(Control parent) {
        Menu menu = new Menu(parent);

        /* Group: None */
        final MenuItem noGrouping = new MenuItem(menu, SWT.RADIO);
        noGrouping.setText("No Grouping");
        noGrouping.setSelection(BookMarkGrouping.Type.NO_GROUPING == fBookMarkGrouping.getType());
        menu.setDefaultItem(noGrouping);
        noGrouping.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (noGrouping.getSelection() && fBookMarkGrouping.getType() != BookMarkGrouping.Type.NO_GROUPING)
              doGrouping(BookMarkGrouping.Type.NO_GROUPING);
          }
        });

        /* Separator */
        new MenuItem(menu, SWT.SEPARATOR);

        /* Group: By Last Visit */
        final MenuItem groupByLastVisit = new MenuItem(menu, SWT.RADIO);
        groupByLastVisit.setText("Group by Last Visit");
        groupByLastVisit.setSelection(BookMarkGrouping.Type.GROUP_BY_LAST_VISIT == fBookMarkGrouping.getType());
        groupByLastVisit.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByLastVisit.getSelection() && fBookMarkGrouping.getType() != BookMarkGrouping.Type.GROUP_BY_LAST_VISIT)
              doGrouping(BookMarkGrouping.Type.GROUP_BY_LAST_VISIT);
          }
        });

        /* Group: By Popularity */
        final MenuItem groupByPopularity = new MenuItem(menu, SWT.RADIO);
        groupByPopularity.setText("Group by Popularity");
        groupByPopularity.setSelection(BookMarkGrouping.Type.GROUP_BY_POPULARITY == fBookMarkGrouping.getType());
        groupByPopularity.addSelectionListener(new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            if (groupByPopularity.getSelection() && fBookMarkGrouping.getType() != BookMarkGrouping.Type.GROUP_BY_POPULARITY)
              doGrouping(BookMarkGrouping.Type.GROUP_BY_POPULARITY);
          }
        });

        return menu;
      }

      public Menu getMenu(Menu parent) {
        return null;
      }
    });

    fToolBarManager.add(bookmarkGroup);

    /* BookmarkSet Navigation - TODO Consider showing dynamically */
    fToolBarManager.add(new Separator());
    IAction previousSet = new Action("Previous Bookmark-Set") {
      @Override
      public void run() {
        int index = getIndexOfRootFolder(fSelectedBookMarkSet);
        changeSet(getRootFolderAt(index - 1));
      }

      @Override
      public boolean isEnabled() {
        int index = getIndexOfRootFolder(fSelectedBookMarkSet);
        return index > 0 && fRootFolders.size() > 1;
      }
    };
    previousSet.setId(PREVIOUS_SET_ACTION);
    previousSet.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/backward.gif")); //$NON-NLS-1$
    previousSet.setDisabledImageDescriptor(OwlUI.getImageDescriptor("icons/dtool16/backward.gif")); //$NON-NLS-1$
    fToolBarManager.add(previousSet);

    IAction nextSet = new Action("Next Bookmark-Set") {
      @Override
      public void run() {
        int index = getIndexOfRootFolder(fSelectedBookMarkSet);
        changeSet(getRootFolderAt(index + 1));
      }

      @Override
      public boolean isEnabled() {
        int index = getIndexOfRootFolder(fSelectedBookMarkSet);
        return index < (fRootFolders.size() - 1) && fRootFolders.size() > 1;
      }
    };
    nextSet.setId(NEXT_SET_ACTION);
    nextSet.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/forward.gif")); //$NON-NLS-1$
    nextSet.setDisabledImageDescriptor(OwlUI.getImageDescriptor("icons/dtool16/forward.gif")); //$NON-NLS-1$
    fToolBarManager.add(nextSet);

    /* Allow Contributions */
    fToolBarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private void doFilter(BookMarkFilter.Type type) {

    /* Change Filter Type */
    fBookMarkFilter.setType(type);
    fViewer.refresh(false);

    /* Restore Expanded Elements */
    restoreExpandedElements();

    /* Update Image */
    fToolBarManager.find(FILTER_ACTION).update(IAction.IMAGE);
  }

  private void doGrouping(BookMarkGrouping.Type type) {

    /* Temporary change Sorter to reflect grouping */
    if (!fSortByName) {
      if (type.equals(BookMarkGrouping.Type.NO_GROUPING))
        fBookMarkComparator.setType(BookMarkSorter.Type.DEFAULT_SORTING);
      else if (type.equals(BookMarkGrouping.Type.GROUP_BY_LAST_VISIT))
        fBookMarkComparator.setType(BookMarkSorter.Type.SORT_BY_LAST_VISIT_DATE);
      else if (type.equals(BookMarkGrouping.Type.GROUP_BY_POPULARITY))
        fBookMarkComparator.setType(BookMarkSorter.Type.SORT_BY_POPULARITY);
    }

    /* Refresh w/o updating Labels */
    fBookMarkGrouping.setType(type);
    fViewer.refresh(false);

    /* Restore Sorter */
    fBookMarkComparator.setType(fSortByName ? BookMarkSorter.Type.SORT_BY_NAME : BookMarkSorter.Type.DEFAULT_SORTING);

    /* Restore expanded Elements */
    restoreExpandedElements();

    /* Update Image */
    fToolBarManager.find(GROUP_ACTION).update(IAction.IMAGE);
  }

  private void hookContextualMenu() {
    MenuManager manager = new MenuManager();

    /* New Menu */
    MenuManager newMenu = new MenuManager("New");
    manager.add(newMenu);

    /* New BookMark */
    newMenu.add(new Action("Bookmark...") {
      @Override
      public void run() {
        IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
        IFolder parent = getParent(selection);
        IMark position = (IMark) ((selection.getFirstElement() instanceof IMark) ? selection.getFirstElement() : null);
        new NewBookMarkAction(fViewSite.getShell(), parent, position).run(null);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.BOOKMARK;
      }
    });

    /* New NewsBin */
    newMenu.add(new Action("News Bin...") {
      @Override
      public void run() {
        IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
        IFolder parent = getParent(selection);
        IMark position = (IMark) ((selection.getFirstElement() instanceof IMark) ? selection.getFirstElement() : null);
        new NewNewsBinAction(fViewSite.getShell(), parent, position).run(null);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.NEWSBIN;
      }
    });

    /* New Saved Search */
    newMenu.add(new Action("Saved Search...") {
      @Override
      public void run() {
        IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
        IFolder parent = getParent(selection);
        IMark position = (IMark) ((selection.getFirstElement() instanceof IMark) ? selection.getFirstElement() : null);
        new NewSearchMarkAction(fViewSite.getShell(), parent, position).run(null);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.SEARCHMARK;
      }
    });

    /* New Folder */
    newMenu.add(new Separator());
    newMenu.add(new Action("Folder...") {
      @Override
      public void run() {
        IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
        IFolder parent = getParent(selection);
        IMark position = (IMark) ((selection.getFirstElement() instanceof IMark) ? selection.getFirstElement() : null);
        new NewFolderAction(fViewSite.getShell(), parent, position).run(null);
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.FOLDER;
      }
    });

    manager.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));

    /* Mark Read */
    manager.add(new Separator(OwlUI.M_MARK));

    /* Search News */
    manager.add(new Separator());
    manager.add(new SearchInTypeAction(fViewSite.getWorkbenchWindow(), fViewer));

    /* Allow Contributions */
    manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

    /* Create and Register with Workbench */
    Menu menu = manager.createContextMenu(fViewer.getControl());
    fViewer.getControl().setMenu(menu);
    fViewSite.registerContextMenu(manager, fViewer);
  }

  private void registerListeners() {

    /* Listen for Events on Root-Folders */
    fFolderListener = new FolderAdapter() {

      @Override
      public void entitiesAdded(Set<FolderEvent> events) {
        for (FolderEvent event : events) {
          if (event.getEntity().getParent() == null) {
            fRootFolders.add(event.getEntity());

            /* Show this Folder in the Explorer */
            changeSet(event.getEntity());
          }
        }
      }

      @Override
      public void entitiesDeleted(Set<FolderEvent> events) {
        for (FolderEvent event : events) {
          IFolder deletedFolder = event.getEntity();
          IFolder parentFolder = event.getEntity().getParent();
          if (parentFolder == null) {
            int index = getIndexOfRootFolder(deletedFolder);
            fRootFolders.remove(event.getEntity());

            /* In case this Bookmark set is currently showing in the Explorer */
            if (fSelectedBookMarkSet.equals(deletedFolder)) {
              if (fRootFolders.size() > index)
                changeSet(getRootFolderAt(index));
              else
                changeSet(getRootFolderAt(index - 1));
            }

            /* Otherwise make sure to update Nav-Buttons */
            else {
              fViewSite.getActionBars().getToolBarManager().find(PREVIOUS_SET_ACTION).update(IAction.ENABLED);
              fViewSite.getActionBars().getToolBarManager().find(NEXT_SET_ACTION).update(IAction.ENABLED);
            }
          }
        }
      }
    };
    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);

    /* Listen for Editors activated for the linking Feature */
    fPartListener = new IPartListener2() {
      public void partActivated(IWorkbenchPartReference ref) {
        if (ref.getPart(true) instanceof IEditorPart) {

          /* Workaround for Bug 573 */
          JobRunner.runInUIThread(50, fViewer.getTree(), new Runnable() {
            public void run() {
              editorActivated(fViewSite.getPage().getActiveEditor());
            }
          });
        }
      }

      public void partBroughtToTop(IWorkbenchPartReference ref) {
        if (ref.getPart(true) == BookMarkExplorer.this)
          editorActivated(fViewSite.getPage().getActiveEditor());
      }

      public void partOpened(IWorkbenchPartReference ref) {
        if (ref.getPart(true) == BookMarkExplorer.this)
          editorActivated(fViewSite.getPage().getActiveEditor());
      }

      public void partVisible(IWorkbenchPartReference ref) {
        if (ref.getPart(true) == BookMarkExplorer.this)
          editorActivated(fViewSite.getPage().getActiveEditor());
      }

      public void partClosed(IWorkbenchPartReference ref) {}

      public void partDeactivated(IWorkbenchPartReference ref) {}

      public void partHidden(IWorkbenchPartReference ref) {}

      public void partInputChanged(IWorkbenchPartReference ref) {
        if (ref.getPart(true) instanceof IEditorPart)
          editorActivated(fViewSite.getPage().getActiveEditor());
      }
    };

    fViewSite.getPage().addPartListener(fPartListener);

    /* Refresh Viewer when Sticky Color Changes */
    fPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        if (fViewer.getControl().isDisposed())
          return;

        if (OwlUI.STICKY_BG_COLOR_ID.equals(event.getProperty())) {
          fLabelProvider.updateResources();
          fViewer.refresh(true);
        }
      }
    };
    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fPropertyChangeListener);
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
    fViewSite.getPage().removePartListener(fPartListener);
    PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fPropertyChangeListener);
  }

  private void hookGlobalActions() {

    /* Select All */
    fViewSite.getActionBars().setGlobalActionHandler(ActionFactory.SELECT_ALL.getId(), new Action() {
      @Override
      public void run() {
        Control focusControl = fViewer.getControl().getDisplay().getFocusControl();

        /* Select All in Text Widget */
        if (focusControl instanceof Text) {
          ((Text) focusControl).selectAll();
        }

        /* Select All in Tree */
        else {
          ((Tree) fViewer.getControl()).selectAll();
          fViewer.setSelection(fViewer.getSelection());
        }
      }
    });

    /* Delete */
    fViewSite.getActionBars().setGlobalActionHandler(ActionFactory.DELETE.getId(), new Action() {
      @Override
      public void run() {
        fViewer.getControl().getParent().setRedraw(false);
        try {
          new DeleteTypesAction(fViewer.getControl().getShell(), (IStructuredSelection) fViewer.getSelection()).run();
        } finally {
          fViewer.getControl().getParent().setRedraw(true);
        }
      }
    });

    /* Reload */
    fViewSite.getActionBars().setGlobalActionHandler(RetargetActions.RELOAD, new Action() {
      @Override
      public void run() {
        new ReloadTypesAction((IStructuredSelection) fViewer.getSelection(), fViewSite.getShell()).run();
      }
    });

    /* Cut */
    fViewSite.getActionBars().setGlobalActionHandler(ActionFactory.CUT.getId(), new Action() {
      @Override
      public void run() {
        Control focusControl = fViewer.getControl().getDisplay().getFocusControl();

        /* Cut in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).cut();
      }
    });

    /* Copy */
    fViewSite.getActionBars().setGlobalActionHandler(ActionFactory.COPY.getId(), new Action() {
      @Override
      public void run() {
        Control focusControl = fViewer.getControl().getDisplay().getFocusControl();

        /* Copy in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).copy();
      }
    });

    /* Paste */
    fViewSite.getActionBars().setGlobalActionHandler(ActionFactory.PASTE.getId(), new Action() {
      @Override
      public void run() {
        Control focusControl = fViewer.getControl().getDisplay().getFocusControl();

        /* Paste in Text Widget */
        if (focusControl instanceof Text)
          ((Text) focusControl).paste();
      }
    });

    /* Properties */
    fViewSite.getActionBars().setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), new EntityPropertyDialogAction(fViewSite, fViewer));

    /* Disable some Edit-Actions at first */
    fViewSite.getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
    fViewSite.getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
    fViewSite.getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
  }

  /**
   * The user performed the "Find" action.
   */
  public void find() {
    setSearchBarVisible(true);
    fSearchBar.getControl().setFocus();
  }

  /*
   * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite)
   */
  @Override
  public void init(IViewSite site) throws PartInitException {
    super.init(site);
    fViewSite = site;
    fGlobalPreferences = Owl.getPreferenceService().getGlobalScope();
    fPrefDAO = DynamicDAO.getDAO(IPreferenceDAO.class);
    fExpandedNodes = new ArrayList<Long>();

    /* Sort Root-Folders by ID */
    fRootFolders = new TreeSet<IFolder>(new Comparator<IFolder>() {
      public int compare(IFolder f1, IFolder f2) {
        if (f1.equals(f2))
          return 0;

        return f1.getId() < f2.getId() ? -1 : 1;
      }
    });

    /* Add Root-Folders */
    fRootFolders.addAll(DynamicDAO.getDAO(IFolderDAO.class).loadRoots());

    /* Load Settings */
    loadState();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    if (ISelectionProvider.class.equals(adapter))
      return fViewer;

    if (IInputProvider.class.equals(adapter))
      return fViewer.getInput();

    return super.getAdapter(adapter);
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    fViewer.getControl().setFocus();
  }

  /*
   * @see org.eclipse.ui.part.WorkbenchPart#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    unregisterListeners();
    saveState();
  }

  /**
   * Navigate to the next/previous read or unread Feed respecting the Marks that
   * are displayed in the Tree-Viewer.
   *
   * @param newsScoped If <code>TRUE</code>, the navigation looks for News
   * and for Feeds if <code>FALSE</code>.
   * @param next If <code>TRUE</code>, move to the next item, or previous if
   * <code>FALSE</code>.
   * @param unread If <code>TRUE</code>, only move to unread items, or ignore
   * if <code>FALSE</code>.
   * @return Returns <code>TRUE</code> in case navigation found a valid item,
   * or <code>FALSE</code> otherwise.
   */
  public boolean navigate(boolean newsScoped, boolean next, boolean unread) {
    Tree explorerTree = fViewer.getTree();

    /* Nothing to Navigate to */
    if (explorerTree.isDisposed())
      return false;

    ITreeNode targetNode = null;

    /* 1.) Navigate in opened Tree */
    targetNode = navigateInTree(explorerTree, next, unread);

    /* 2.) Navigate in BookMark-Sets */
    if (targetNode == null)
      targetNode = navigateInSets(next, unread);

    /* 3.) Finally, wrap in visible Tree if next */
    if (targetNode == null && next) {
      ITreeNode startingNode = new WidgetTreeNode(fViewer.getTree(), fViewer);
      targetNode = navigate(startingNode, next, unread);
    }

    /* Perform navigation if Node was found */
    if (targetNode != null) {
      performNavigation(targetNode, newsScoped, unread);
      return true;
    }

    return false;
  }

  private void performNavigation(ITreeNode targetNode, boolean newsScoped, boolean unread) {
    INewsMark mark = (INewsMark) targetNode.getData();

    /* Set Selection to Mark */
    ISelection selection = new StructuredSelection(mark);
    fViewer.setSelection(selection);

    /* Open in FeedView */
    try {
      PerformAfterInputSet perform = null;
      if (newsScoped && unread)
        perform = PerformAfterInputSet.SELECT_UNREAD_NEWS;
      else if (newsScoped)
        perform = PerformAfterInputSet.SELECT_FIRST_NEWS;

      IEditorPart feedview = fViewSite.getPage().openEditor(new FeedViewInput(mark, perform), FeedView.ID, true);
      feedview.getSite().getPage().activate(feedview.getSite().getPart());
    } catch (PartInitException e) {
      Activator.getDefault().getLog().log(e.getStatus());
    }
  }

  private ITreeNode navigateInTree(Tree tree, boolean next, boolean unread) {
    ITreeNode resultingNode = null;

    /* Selection is Present */
    if (tree.getSelectionCount() > 0) {

      /* Try navigating from Selection */
      ITreeNode startingNode = new WidgetTreeNode(tree.getSelection()[0], fViewer);
      resultingNode = navigate(startingNode, next, unread);
      if (resultingNode != null)
        return resultingNode;
    }

    /* No Selection is Present */
    else {
      ITreeNode startingNode = new WidgetTreeNode(tree, fViewer);
      resultingNode = navigate(startingNode, next, unread);
      if (resultingNode != null)
        return resultingNode;
    }

    return resultingNode;
  }

  private ITreeNode navigateInSets(boolean next, boolean unread) {
    ITreeNode targetNode = null;

    /* Index of current visible Set */
    int index = getIndexOfRootFolder(fSelectedBookMarkSet);

    /* Look in next Sets */
    if (next) {

      /* Sets to the right */
      for (int i = index + 1; i < fRootFolders.size(); i++) {
        targetNode = navigateInSet(getRootFolderAt(i), true, unread);
        if (targetNode != null)
          return targetNode;
      }

      /* Sets to the left */
      for (int i = 0; i < index; i++) {
        targetNode = navigateInSet(getRootFolderAt(i), true, unread);
        if (targetNode != null)
          return targetNode;
      }
    }

    /* Look in previous Sets */
    else {

      /* Sets to the left */
      for (int i = index - 1; i >= 0; i--) {
        targetNode = navigateInSet(getRootFolderAt(i), true, unread);
        if (targetNode != null)
          return targetNode;
      }

      /* Sets to the right */
      for (int i = fRootFolders.size() - 1; i > index; i--) {
        targetNode = navigateInSet(getRootFolderAt(i), true, unread);
        if (targetNode != null)
          return targetNode;
      }
    }

    return targetNode;
  }

  private ITreeNode navigateInSet(IFolder set, boolean next, boolean unread) {
    ITreeNode node = new ModelTreeNode(set);
    ITreeNode targetNode = navigate(node, next, unread);
    if (targetNode != null) {
      changeSet(set);
      return targetNode;
    }

    return null;
  }

  private ITreeNode navigate(ITreeNode startingNode, boolean next, final boolean unread) {

    /* Create Traverse-Helper */
    TreeTraversal traverse = new TreeTraversal(startingNode) {
      @Override
      public boolean select(ITreeNode node) {
        return isValidNavigation(node, unread);
      }
    };

    /* Retrieve and select new Target Node */
    ITreeNode targetNode = (next ? traverse.nextNode() : traverse.previousNode());

    return targetNode;
  }

  private boolean isValidNavigation(ITreeNode node, boolean unread) {
    Object data = node.getData();

    /* Check for Unread news if required */
    if (data instanceof INewsMark) {
      INewsMark newsmark = (INewsMark) data;
      if (unread && newsmark.getNewsCount(EnumSet.of(INews.State.NEW, INews.State.UNREAD, INews.State.UPDATED)) == 0)
        return false;
    }

    /* Folders are no valid navigation nodes */
    else if (data instanceof IFolder)
      return false;

    return true;
  }

  private void saveState() {

    /* Expanded Elements */
    saveExpandedElements();

    /* Misc. Settings */
    fGlobalPreferences.putBoolean(DefaultPreferences.BE_BEGIN_SEARCH_ON_TYPING, fBeginSearchOnTyping);
    fGlobalPreferences.putBoolean(DefaultPreferences.BE_ALWAYS_SHOW_SEARCH, fAlwaysShowSearch);
    fGlobalPreferences.putBoolean(DefaultPreferences.BE_SORT_BY_NAME, fSortByName);
    fGlobalPreferences.putBoolean(DefaultPreferences.BE_ENABLE_LINKING, fLinkingEnabled);
    fGlobalPreferences.putBoolean(DefaultPreferences.BE_DISABLE_FAVICONS, !fFaviconsEnabled);
    fGlobalPreferences.putInteger(DefaultPreferences.BE_FILTER_TYPE, fBookMarkFilter.getType().ordinal());
    fGlobalPreferences.putInteger(DefaultPreferences.BE_GROUP_TYPE, fBookMarkGrouping.getType().ordinal());
  }

  private void saveExpandedElements() {
    int i = 0;
    long elements[] = new long[fExpandedNodes.size()];
    for (Object element : fExpandedNodes) {
      elements[i] = (Long) element;
      i++;
    }
    /* Add the ID of the current selected Set to make it Unique */
    String key = PREF_EXPANDED_NODES + fSelectedBookMarkSet;

    IPreference pref = fPrefDAO.loadOrCreate(key);
    pref.putLongs(elements);
    fPrefDAO.save(pref);
  }

  private void loadState() {

    /* Misc. Settings */
    fBeginSearchOnTyping = fGlobalPreferences.getBoolean(DefaultPreferences.BE_BEGIN_SEARCH_ON_TYPING);
    fAlwaysShowSearch = fGlobalPreferences.getBoolean(DefaultPreferences.BE_ALWAYS_SHOW_SEARCH);
    fSortByName = fGlobalPreferences.getBoolean(DefaultPreferences.BE_SORT_BY_NAME);
    fLinkingEnabled = fGlobalPreferences.getBoolean(DefaultPreferences.BE_ENABLE_LINKING);
    fFaviconsEnabled = !fGlobalPreferences.getBoolean(DefaultPreferences.BE_DISABLE_FAVICONS);
    fFilterType = BookMarkFilter.Type.values()[fGlobalPreferences.getInteger(DefaultPreferences.BE_FILTER_TYPE)];
    fGroupingType = BookMarkGrouping.Type.values()[fGlobalPreferences.getInteger(DefaultPreferences.BE_GROUP_TYPE)];

    String selectedBookMarkSetPref = getSelectedBookMarkSetPref(fViewSite.getWorkbenchWindow());
    IPreference pref = fPrefDAO.load(selectedBookMarkSetPref);
    Assert.isTrue(fRootFolders.size() > 0, "Could not find any Bookmark Set!"); //$NON-NLS-1$
    if (pref != null)
      fSelectedBookMarkSet = new FolderReference(pref.getLong().longValue()).resolve();
    else {
      fSelectedBookMarkSet = getRootFolderAt(0);

      /* Save this to make sure subsequent calls succeed */
      pref = Owl.getModelFactory().createPreference(selectedBookMarkSetPref);
      pref.putLongs(fSelectedBookMarkSet.getId());
      fPrefDAO.save(pref);
    }

    /* Expanded Elements */
    loadExpandedElements();
  }

  /* Expanded Elements - Use ID of selected Set to make it Unique */
  private void loadExpandedElements() {
    IPreference pref = fPrefDAO.load(PREF_EXPANDED_NODES + fSelectedBookMarkSet);
    if (pref != null) {
      for (long element : pref.getLongs())
        fExpandedNodes.add(element);
    }
  }

  void restoreExpandedElements() {
    for (Long expandedNodeId : fExpandedNodes) {
      if (fBookMarkGrouping.getType() == BookMarkGrouping.Type.NO_GROUPING)
        fViewer.setExpandedState(new FolderReference(expandedNodeId), true);
      else
        fViewer.setExpandedState(new EntityGroup(expandedNodeId, BookMarkGrouping.GROUP_CATEGORY_ID), true);
    }
  }

  private IFolder getParent(IStructuredSelection selection) {
    if (!selection.isEmpty()) {
      Object obj = selection.getFirstElement();
      if (obj instanceof IFolder)
        return (IFolder) obj;
      else if (obj instanceof IMark)
        return ((IMark) obj).getParent();
    }

    /* Default is selected Set */
    return fSelectedBookMarkSet;
  }

  private IFolder getRootFolderAt(int index) {
    int i = 0;
    for (IFolder rootFolder : fRootFolders) {
      if (i == index)
        return rootFolder;
      i++;
    }

    return null;
  }

  private int getIndexOfRootFolder(IFolder folder) {
    int i = 0;
    for (IFolder rootFolder : fRootFolders) {
      if (rootFolder.equals(folder))
        return i;
      i++;
    }

    return -1;
  }

  boolean isGroupingEnabled() {
    return fBookMarkGrouping.isActive();
  }

  boolean isSortByNameEnabled() {
    return fBookMarkComparator.getType() == BookMarkSorter.Type.SORT_BY_NAME;
  }
}