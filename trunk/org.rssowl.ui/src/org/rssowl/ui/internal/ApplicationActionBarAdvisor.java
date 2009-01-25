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

package org.rssowl.ui.internal;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.actions.CopyLinkAction;
import org.rssowl.ui.internal.actions.FindAction;
import org.rssowl.ui.internal.actions.LabelAction;
import org.rssowl.ui.internal.actions.MakeNewsStickyAction;
import org.rssowl.ui.internal.actions.MarkAllNewsReadAction;
import org.rssowl.ui.internal.actions.MoveCopyNewsToBinAction;
import org.rssowl.ui.internal.actions.OpenInExternalBrowserAction;
import org.rssowl.ui.internal.actions.RedoAction;
import org.rssowl.ui.internal.actions.ReloadAllAction;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.actions.SearchNewsAction;
import org.rssowl.ui.internal.actions.SendLinkAction;
import org.rssowl.ui.internal.actions.ToggleReadStateAction;
import org.rssowl.ui.internal.actions.UndoAction;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

  /** Name of the "Manage Extensions" SubMenu */
  public static final String M_MANAGE_EXTENSIONS = "manageExtensions"; //$NON-NLS-1$

  /** Name of the View Top Menu */
  public static final String M_VIEW = "view";

  /** Start of the View Top Menu */
  public static final String M_VIEW_START = "viewStart";

  /** End of the View Top Menu */
  public static final String M_VIEW_END = "viewEnd";

  private IContributionItem fOpenWindowsItem;
  private IContributionItem fShowViewMenu;
  private IContributionItem fReopenEditors;
  private FindAction fFindAction;

  /**
   * @param configurer
   */
  public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
    super(configurer);
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#makeActions(org.eclipse.ui.IWorkbenchWindow)
   */
  @Override
  protected void makeActions(IWorkbenchWindow window) {

    /* Menu: File */
    register(ActionFactory.CLOSE.create(window)); // TODO ActionSet?
    register(ActionFactory.CLOSE_ALL.create(window)); // TODO ActionSet?
    register(ActionFactory.PRINT.create(window)); // TODO ActionSet?
    register(ActionFactory.QUIT.create(window));

    fReopenEditors = ContributionItemFactory.REOPEN_EDITORS.create(window);

    /* Menu: Edit */
    register(ActionFactory.CUT.create(window));
    register(ActionFactory.COPY.create(window));
    register(ActionFactory.PASTE.create(window));
    register(ActionFactory.DELETE.create(window));
    register(ActionFactory.SELECT_ALL.create(window));
    register(ActionFactory.PROPERTIES.create(window));

    fFindAction = new FindAction();
    register(fFindAction);

    /* Menu: Tools */
    register(ActionFactory.PREFERENCES.create(window));

    /* Menu: Window */
    register(ActionFactory.OPEN_NEW_WINDOW.create(window));
    getAction(ActionFactory.OPEN_NEW_WINDOW.getId()).setText("&New Window");
    //    register(ActionFactory.TOGGLE_COOLBAR.create(window));
    //    register(ActionFactory.RESET_PERSPECTIVE.create(window));
    //    register(ActionFactory.EDIT_ACTION_SETS.create(window));
    //    register(ActionFactory.ACTIVATE_EDITOR.create(window));
    //    register(ActionFactory.MAXIMIZE.create(window));
    //    register(ActionFactory.MINIMIZE.create(window));
    //    register(ActionFactory.NEXT_EDITOR.create(window));
    //    register(ActionFactory.PREVIOUS_EDITOR.create(window));
    //    register(ActionFactory.PREVIOUS_PART.create(window));
    //    register(ActionFactory.NEXT_PART.create(window));
    //    register(ActionFactory.SHOW_EDITOR.create(window));
    //    register(ActionFactory.SHOW_OPEN_EDITORS.create(window));
    //    register(ActionFactory.SHOW_WORKBOOK_EDITORS.create(window));
    //    register(ActionFactory.SHOW_PART_PANE_MENU.create(window));
    //    register(ActionFactory.SHOW_VIEW_MENU.create(window));

    fOpenWindowsItem = ContributionItemFactory.OPEN_WINDOWS.create(window);
    fShowViewMenu = ContributionItemFactory.VIEWS_SHORTLIST.create(window);

    /* Menu: Help */
    // register(ActionFactory.INTRO.create(window)); TODO Enable
    register(ActionFactory.ABOUT.create(window));
    getAction(ActionFactory.ABOUT.getId()).setText("&About RSSOwl");

    /* CoolBar: Contextual Menu */
    register(ActionFactory.LOCK_TOOL_BAR.create(window));
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillMenuBar(org.eclipse.jface.action.IMenuManager)
   */
  @Override
  protected void fillMenuBar(IMenuManager menuBar) {

    /* File Menu */
    createFileMenu(menuBar);

    /* Edit Menu */
    createEditMenu(menuBar);

    /* View Menu */
    createViewMenu(menuBar);

    /* Go Menu */
    createGoMenu(menuBar);

    /* News Menu */
    createNewsMenu(menuBar);

    /* Allow Top-Level Menu Contributions here */
    menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    /* Menu: Tools */
    createToolsMenu(menuBar);

    /* Window Menu */
    createWindowMenu(menuBar);

    /* Menu: Help */
    createHelpMenu(menuBar);
  }

  /* Menu: File */
  private void createFileMenu(IMenuManager menuBar) {
    MenuManager fileMenu = new MenuManager("&File", IWorkbenchActionConstants.M_FILE);
    menuBar.add(fileMenu);

    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.NEW_EXT));
    fileMenu.add(new Separator());

    fileMenu.add(getAction(ActionFactory.CLOSE.getId()));
    fileMenu.add(getAction(ActionFactory.CLOSE_ALL.getId()));
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.CLOSE_EXT));
    fileMenu.add(new Separator());
    fileMenu.add(getAction(ActionFactory.PRINT.getId()));

    fileMenu.add(new Separator());
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    fileMenu.add(fReopenEditors); // TODO Consider moving into a "Go" Menu!

    fileMenu.add(new Separator());
    fileMenu.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));
    fileMenu.add(new Separator());

    fileMenu.add(getAction(ActionFactory.QUIT.getId()));
  }

  /* Menu: Edit */
  private void createEditMenu(IMenuManager menuBar) {
    MenuManager editMenu = new MenuManager("&Edit", IWorkbenchActionConstants.M_EDIT);
    editMenu.add(getAction(ActionFactory.COPY.getId())); //Dummy action
    menuBar.add(editMenu);

    editMenu.setRemoveAllWhenShown(true);
    editMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager editMenu) {
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
        editMenu.add(new Separator());

        editMenu.add(new UndoAction());
        editMenu.add(new RedoAction());
        editMenu.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
        editMenu.add(new Separator());

        editMenu.add(getAction(ActionFactory.CUT.getId()));
        editMenu.add(getAction(ActionFactory.COPY.getId()));
        editMenu.add(getAction(ActionFactory.PASTE.getId()));
        editMenu.add(new Separator());
        editMenu.add(getAction(ActionFactory.DELETE.getId()));
        editMenu.add(getAction(ActionFactory.SELECT_ALL.getId()));

        editMenu.add(new Separator());

        editMenu.add(new SearchNewsAction(OwlUI.getWindow()));
        editMenu.add(fFindAction);

        editMenu.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
        editMenu.add(new Separator());

        editMenu.add(getAction(ActionFactory.PROPERTIES.getId()));
      }
    });
  }

  /* Menu: View */
  private void createViewMenu(IMenuManager menuBar) {
    final IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();

    MenuManager viewMenu = new MenuManager("&View", M_VIEW);
    viewMenu.setRemoveAllWhenShown(true);
    menuBar.add(viewMenu);

    /* Add dummy action to show the top level menu */
    viewMenu.add(new Action("") {
      @Override
      public void run() {}
    });

    /* Build Menu dynamically */
    viewMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        manager.add(new GroupMarker(M_VIEW_START));

        /* Toggle State of Toolbar Visibility */
        manager.add(new Action("Toolbar", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;

            boolean isToolBarVisible = preferences.getBoolean(DefaultPreferences.SHOW_TOOLBAR);
            configurer.setToolBarVisible(!isToolBarVisible);
            preferences.putBoolean(DefaultPreferences.SHOW_TOOLBAR, !isToolBarVisible);
          }

          @Override
          public boolean isChecked() {
            return preferences.getBoolean(DefaultPreferences.SHOW_TOOLBAR);
          }
        });

        /* Toggle State of Status Bar Visibility */
        manager.add(new Action("Status", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;

            boolean isStatusVisible = preferences.getBoolean(DefaultPreferences.SHOW_STATUS);
            configurer.setStatusVisible(!isStatusVisible, true);
            preferences.putBoolean(DefaultPreferences.SHOW_STATUS, !isStatusVisible);
          }

          @Override
          public boolean isChecked() {
            return preferences.getBoolean(DefaultPreferences.SHOW_STATUS);
          }
        });

        /* Toggle State of Bookmarks Visibility */
        manager.add(new Action("Bookmarks", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            IWorkbenchPage page = OwlUI.getPage();
            if (page != null) {
              IViewPart explorerView = page.findView(BookMarkExplorer.VIEW_ID);

              /* Hide Bookmarks */
              if (explorerView != null)
                page.hideView(explorerView);

              /* Show Bookmarks */
              else {
                try {
                  page.showView(BookMarkExplorer.VIEW_ID);
                } catch (PartInitException e) {
                  Activator.getDefault().logError(e.getMessage(), e);
                }
              }
            }
          }

          @Override
          public boolean isChecked() {
            IWorkbenchPage page = OwlUI.getPage();
            if (page != null)
              return page.findView(BookMarkExplorer.VIEW_ID) != null;

            return false;
          }
        });

        /* Fullscreen Mode */
        manager.add(new Separator());
        manager.add(new Action("Full Screen", IAction.AS_CHECK_BOX) {
          @Override
          public void run() {
            Shell shell = OwlUI.getActiveShell();
            if (shell != null) {
              shell.setFullScreen(!shell.getFullScreen());

              /* Shell got restored */
              if (!shell.getFullScreen()) {
                ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;
                configurer.setStatusVisible(preferences.getBoolean(DefaultPreferences.SHOW_STATUS), false);

                shell.layout(); //Need to layout to avoid screen cheese
              }

              /* Shell got fullscreen */
              else {
                ApplicationWorkbenchWindowAdvisor configurer = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;
                configurer.setStatusVisible(false, true);
              }
            }
          }

          @Override
          public String getActionDefinitionId() {
            return "org.rssowl.ui.FullScreenCommand";
          }

          @Override
          public String getId() {
            return "org.rssowl.ui.FullScreenCommand";
          }

          @Override
          public boolean isChecked() {
            Shell shell = OwlUI.getActiveShell();
            if (shell != null)
              return shell.getFullScreen();

            return super.isChecked();
          }
        });

        manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
        manager.add(new GroupMarker(M_VIEW_START));
      }
    });
  }

  /* Menu: Go */
  private void createGoMenu(IMenuManager menuBar) {
    MenuManager viewMenu = new MenuManager("&Go", IWorkbenchActionConstants.M_NAVIGATE);
    menuBar.add(viewMenu);

    viewMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  /* Menu News */
  private void createNewsMenu(IMenuManager menuBar) {
    final IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();

    final MenuManager newsMenu = new MenuManager("&News", "news");
    menuBar.add(newsMenu);
    newsMenu.setRemoveAllWhenShown(true);

    newsMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        final IStructuredSelection selection;

        FeedView activeFeedView = OwlUI.getActiveFeedView();
        FeedViewInput activeInput = null;
        if (activeFeedView != null) {
          selection = (IStructuredSelection) activeFeedView.getSite().getSelectionProvider().getSelection();
          activeInput = (FeedViewInput) activeFeedView.getEditorInput();
        } else
          selection = StructuredSelection.EMPTY;

        /* Open */
        {
          manager.add(new Separator("open"));

          /* Open News in Browser */
          manager.add(new Action("Open in Browser") {
            @Override
            public void run() {
              new OpenInExternalBrowserAction(selection).run();
            }

            @Override
            public boolean isEnabled() {
              return !selection.isEmpty();
            }
          });
        }

        /* Move To / Copy To */
        if (!selection.isEmpty()) {
          manager.add(new Separator("movecopy"));

          /* Load all news bins and sort by name */
          List<INewsBin> newsbins = new ArrayList<INewsBin>(DynamicDAO.loadAll(INewsBin.class));

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
            if (activeInput != null && activeInput.getMark().equals(bin))
              continue;

            moveMenu.add(new MoveCopyNewsToBinAction(selection, bin, true));
          }

          moveMenu.add(new Separator("movetonewbin"));
          moveMenu.add(new MoveCopyNewsToBinAction(selection, null, true));

          /* Copy To */
          MenuManager copyMenu = new MenuManager("Copy To", "copyto");
          manager.add(copyMenu);

          for (INewsBin bin : newsbins) {
            if (activeInput != null && activeInput.getMark().equals(bin))
              continue;

            copyMenu.add(new MoveCopyNewsToBinAction(selection, bin, false));
          }

          copyMenu.add(new Separator("copytonewbin"));
          copyMenu.add(new MoveCopyNewsToBinAction(selection, null, false));
        }

        /* Mark / Label */
        {
          manager.add(new Separator("mark"));

          /* Mark */
          {
            MenuManager markMenu = new MenuManager("Mark", "mark");
            manager.add(markMenu);

            /* Mark as Read */
            IAction action = new ToggleReadStateAction(selection);
            action.setEnabled(!selection.isEmpty());
            markMenu.add(action);

            /* Mark All Read */
            action = new MarkAllNewsReadAction();
            action.setEnabled(activeFeedView != null);
            markMenu.add(action);

            /* Sticky */
            markMenu.add(new Separator());
            action = new MakeNewsStickyAction(selection);
            action.setEnabled(!selection.isEmpty());
            markMenu.add(action);
          }

          /* Label */
          {
            Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);

            MenuManager labelMenu = new MenuManager("Label");
            manager.add(labelMenu);

            /* Retrieve Labels that all selected News contain */
            Set<ILabel> selectedLabels = ModelUtils.getLabelsForAll(selection);

            LabelAction removeAllLabels = new LabelAction(null, selection);
            removeAllLabels.setEnabled(!labels.isEmpty());
            labelMenu.add(removeAllLabels);
            labelMenu.add(new Separator());

            for (final ILabel label : labels) {
              LabelAction labelAction = new LabelAction(label, selection);
              labelAction.setChecked(selectedLabels.contains(label));
              labelMenu.add(labelAction);
            }

            labelMenu.add(new Separator());
            labelMenu.add(new Action("Organize...") {
              @Override
              public void run() {
                PreferencesUtil.createPreferenceDialogOn(getActionBarConfigurer().getWindowConfigurer().getWindow().getShell(), ManageLabelsPreferencePage.ID, null, null).open();
              }
            });
          }
        }

        /* Update */
        {
          manager.add(new Separator("reload"));

          /* Update */
          manager.add(new Action("Update") {
            @Override
            public void run() {
              IActionDelegate action = new ReloadTypesAction();
              action.selectionChanged(null, selection);
              action.run(null);
            }

            @Override
            public ImageDescriptor getImageDescriptor() {
              return OwlUI.getImageDescriptor("icons/elcl16/reload.gif");
            }

            @Override
            public ImageDescriptor getDisabledImageDescriptor() {
              return OwlUI.getImageDescriptor("icons/dlcl16/reload.gif");
            }

            @Override
            public boolean isEnabled() {
              return !selection.isEmpty();
            }
          });

          /* Update All */
          manager.add(new ReloadAllAction());
        }

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

        /* Edit */
        {
          manager.add(new Separator("edit"));

          /* Copy Link */
          manager.add(new Action("Copy Link") {
            @Override
            public void run() {
              IActionDelegate action = new CopyLinkAction();
              action.selectionChanged(null, selection);
              action.run(null);
            }

            @Override
            public boolean isEnabled() {
              return !selection.isEmpty();
            }
          });

          /* Send Link */
          manager.add(new Action("Send Link") {
            @Override
            public void run() {
              IActionDelegate action = new SendLinkAction();
              action.selectionChanged(null, selection);
              action.run(null);
            }

            @Override
            public boolean isEnabled() {
              return !selection.isEmpty();
            }
          });
        }

      }
    });
  }

  /* Menu: Tools */
  private void createToolsMenu(IMenuManager menuBar) {
    MenuManager toolsMenu = new MenuManager("&Tools", OwlUI.M_TOOLS);
    menuBar.add(toolsMenu);

    toolsMenu.add(new GroupMarker("begin"));
    toolsMenu.add(new Separator());
    toolsMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    toolsMenu.add(new Separator());

    toolsMenu.add(getAction(ActionFactory.PREFERENCES.getId()));
  }

  /* Menu: Window */
  private void createWindowMenu(IMenuManager menuBar) {
    MenuManager windowMenu = new MenuManager("&Window", IWorkbenchActionConstants.M_WINDOW);
    menuBar.add(windowMenu);

    windowMenu.add(getAction(ActionFactory.OPEN_NEW_WINDOW.getId()));
    windowMenu.add(new Separator());

    MenuManager showViewMenu = new MenuManager("&Show View");
    windowMenu.add(showViewMenu);
    showViewMenu.add(fShowViewMenu);
    //    windowMenu.add(new Separator());

    //    windowMenu.add(getAction(ActionFactory.EDIT_ACTION_SETS.getId()));
    //    windowMenu.add(getAction(ActionFactory.RESET_PERSPECTIVE.getId()));
    //    windowMenu.add(new Separator());
    //
    //    MenuManager navigationMenu = new MenuManager("&Navigation");
    //    windowMenu.add(navigationMenu);
    //
    //    navigationMenu.add(getAction(ActionFactory.SHOW_PART_PANE_MENU.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_VIEW_MENU.getId()));
    //    navigationMenu.add(new Separator());
    //    navigationMenu.add(getAction(ActionFactory.MAXIMIZE.getId()));
    //    navigationMenu.add(getAction(ActionFactory.MINIMIZE.getId()));
    //    navigationMenu.add(new Separator());
    //    navigationMenu.add(getAction(ActionFactory.ACTIVATE_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.NEXT_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.PREVIOUS_EDITOR.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_OPEN_EDITORS.getId()));
    //    navigationMenu.add(getAction(ActionFactory.SHOW_WORKBOOK_EDITORS.getId()));
    //    navigationMenu.add(new Separator());
    //    navigationMenu.add(getAction(ActionFactory.NEXT_PART.getId()));
    //    navigationMenu.add(getAction(ActionFactory.PREVIOUS_PART.getId()));

    windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    windowMenu.add(fOpenWindowsItem);
  }

  /* Menu: Help */
  private void createHelpMenu(IMenuManager menuBar) {
    MenuManager helpMenu = new MenuManager("&Help", IWorkbenchActionConstants.M_HELP);
    menuBar.add(helpMenu);

    helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));

    /* Link to boreal.rssowl.org */
    helpMenu.add(new Action("&Boreal Owl Help") {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("http://boreal.rssowl.org");
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.getImageDescriptor("icons/elcl16/help.gif");
      }
    });

    helpMenu.add(new Separator());

    /* Report Bugs */
    helpMenu.add(new Action("&Report Bugs") {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("http://dev.rssowl.org");
      }

      @Override
      public ImageDescriptor getImageDescriptor() {
        return OwlUI.getImageDescriptor("icons/elcl16/bug.gif");
      }
    });

    /* Homepage */
    helpMenu.add(new Action("&Homepage") {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("http://www.rssowl.org");
      }
    });

    /* Mailing List */
    helpMenu.add(new Action("&Mailinglist") {
      @Override
      public void run() {
        BrowserUtils.openLinkExternal("https://lists.sourceforge.net/lists/listinfo/rssowl-boreal");
      }
    });

    // helpMenu.add(getAction(ActionFactory.INTRO.getId())); TODO Enable
    helpMenu.add(new Separator());

    helpMenu.add(new Separator());
    helpMenu.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));
    helpMenu.add(new Separator());

    helpMenu.add(getAction(ActionFactory.ABOUT.getId()));
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillStatusLine(org.eclipse.jface.action.IStatusLineManager)
   */
  @Override
  protected void fillStatusLine(IStatusLineManager statusLine) {
    super.fillStatusLine(statusLine);
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillActionBars(int)
   */
  @Override
  public void fillActionBars(int flags) {
    super.fillActionBars(flags);
  }

  /**
   * @param trayItem
   */
  protected void fillTrayItem(IMenuManager trayItem) {
    trayItem.add(new ReloadAllAction());
    trayItem.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    trayItem.add(getAction(ActionFactory.QUIT.getId()));
  }

  /*
   * @see org.eclipse.ui.application.ActionBarAdvisor#fillCoolBar(org.eclipse.jface.action.ICoolBarManager)
   */
  @Override
  protected void fillCoolBar(ICoolBarManager coolBar) {

    /* CoolBar Context Menu */
    MenuManager coolBarContextMenuManager = new MenuManager(null, "org.rssowl.ui.CoolBarContextMenu"); //$NON-NLS-1$
    coolBar.setContextMenuManager(coolBarContextMenuManager);
    coolBarContextMenuManager.add(getAction(ActionFactory.LOCK_TOOL_BAR.getId()));
    //    coolBarContextMenuManager.add(getAction(ActionFactory.EDIT_ACTION_SETS.getId()));
    coolBarContextMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
  }
}