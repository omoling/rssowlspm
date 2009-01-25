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

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tray;
import org.eclipse.swt.widgets.TrayItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.event.NewsAdapter;
import org.rssowl.core.persist.event.NewsEvent;
import org.rssowl.core.persist.event.PreferenceEvent;
import org.rssowl.core.persist.event.PreferenceListener;
import org.rssowl.core.persist.event.runnable.EventType;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.util.JobRunner;

import java.util.Set;

/**
 * @author bpasero
 */
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

  /* WebSite class being used for the Browser on Windows only */
  private static final String SWT_BROWSER_WIN = "org.eclipse.swt.browser.WebSite";

  /** Key for Data-Slot in Controls that support this Hook */
  public static final String FOCUSLESS_SCROLL_HOOK = "org.rssowl.ui.internal.FocuslessScrollHook";

  private TrayItem fTrayItem;
  private boolean fTrayTeasing;
  private boolean fTrayEnabled;
  private boolean fMinimizedToTray;
  private ApplicationActionBarAdvisor fActionBarAdvisor;
  private LocalResourceManager fResources;
  private IPreferenceScope fPreferences;
  private boolean fBlockIconifyEvent;
  private boolean fMinimizeFromClose;

  /* Listeners */
  private NewsAdapter fNewsListener;
  private ShellListener fTrayShellListener;
  private PreferenceListener fPrefListener;

  /**
   * @param configurer
   */
  public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
    super(configurer);
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#createActionBarAdvisor(org.eclipse.ui.application.IActionBarConfigurer)
   */
  @Override
  public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
    fActionBarAdvisor = new ApplicationActionBarAdvisor(configurer);
    return fActionBarAdvisor;
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowOpen()
   */
  @Override
  public void preWindowOpen() {
    IWorkbenchWindowConfigurer configurer = getWindowConfigurer();

    /* Set Window State and define visibility of UI elements */
    configurer.setShowCoolBar(true);
    configurer.setShowPerspectiveBar(false);
    configurer.setShowStatusLine(true);
    configurer.setShowMenuBar(true);
    configurer.setShowFastViewBars(false);
    configurer.setShowProgressIndicator(true);
    configurer.setTitle("RSSOwl"); //$NON-NLS-1$

    /* Apply DND Support for Editor Area */
    configurer.addEditorAreaTransfer(LocalSelectionTransfer.getTransfer());
    configurer.configureEditorAreaDropListener(new EditorDNDImpl());

    /* Retrieve Preferences */
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    
    //Juri: show round borders on views
    PlatformUI.getPreferenceStore().setValue(IWorkbenchPreferenceConstants.SHOW_TRADITIONAL_STYLE_TABS, false); 
  }

  void setToolBarVisible(boolean visible) {
    getWindowConfigurer().setShowCoolBar(visible);

    /* Hack: To avoid cheese, update Status Line Too */
    boolean showsStatus = getWindowConfigurer().getShowStatusLine();
    getWindowConfigurer().setShowStatusLine(!showsStatus);
    getWindowConfigurer().setShowStatusLine(showsStatus);

    getWindowConfigurer().getWindow().getShell().layout();
  }

  /**
   * @param visible
   * @param layout
   */
  public void setStatusVisible(boolean visible, boolean layout) {
    getWindowConfigurer().setShowStatusLine(visible);
    getWindowConfigurer().setShowProgressIndicator(visible);

    /* Hack: To avoid cheese, update ToolBar Too */
    boolean showsToolBar = getWindowConfigurer().getShowCoolBar();
    getWindowConfigurer().setShowCoolBar(!showsToolBar);
    getWindowConfigurer().setShowCoolBar(showsToolBar);

    if (layout)
      getWindowConfigurer().getWindow().getShell().layout();
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#postWindowOpen()
   */
  @Override
  public void postWindowOpen() {
    final Shell shell = getWindowConfigurer().getWindow().getShell();

    /* Toolbar & Status Visibility */
    IPreferenceScope preferences = Owl.getPreferenceService().getGlobalScope();
    if (!preferences.getBoolean(DefaultPreferences.SHOW_TOOLBAR))
      setToolBarVisible(false);
    if (!preferences.getBoolean(DefaultPreferences.SHOW_STATUS))
      setStatusVisible(false, true);

    /* Use PNG on Windows and Linux */
    if (Application.IS_WINDOWS || Application.IS_LINUX) {
      Image[] shellImg = new Image[3];
      shellImg[0] = OwlUI.getImage(shell, "icons/product/16x16.png");
      shellImg[1] = OwlUI.getImage(shell, "icons/product/24x24.png");
      shellImg[2] = OwlUI.getImage(shell, "icons/product/32x32.png");
      shell.setImages(shellImg);
    }

    /* System Tray */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        boolean trayEnabled = false;

        /* Hook TrayItem if supported on OS and 1st Window */
        if (fPreferences.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE) || fPreferences.getBoolean(DefaultPreferences.TRAY_ON_CLOSE) || fPreferences.getBoolean(DefaultPreferences.TRAY_ON_START))
          trayEnabled = enableTray();

        /* Win only: Allow Scroll over Cursor-Control */
        if (Application.IS_WINDOWS)
          hookFocuslessScrolling(shell.getDisplay());

        /* Register Listeners */
        registerListeners();

        /* Move to Tray if set */
        if (trayEnabled && fPreferences.getBoolean(DefaultPreferences.TRAY_ON_START))
          moveToTray(shell);
      }
    });

    /* Notify Controller */
    Controller.getDefault().postWindowOpen();
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowShellClose()
   */
  @Override
  public boolean preWindowShellClose() {
    final boolean[] res = new boolean[] { true };
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {

        /* Check if Prefs tell to move to tray */
        if (ApplicationWorkbenchWindowAdvisor.this.equals(ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor) && fPreferences.getBoolean(DefaultPreferences.TRAY_ON_CLOSE)) {
          fMinimizeFromClose = true;
          getWindowConfigurer().getWindow().getShell().notifyListeners(SWT.Iconify, new Event());
          res[0] = false;
          fMinimizeFromClose = false;
        }

        /* Notify any open feedview about closing application */
        else {
          onClose();
        }
      }
    });

    return res[0];
  }

  /**
   * @return TRUE if the Window is minimized to the tray.
   */
  public boolean isMinimizedToTray() {
    return fMinimizedToTray;
  }

  /**
   * @return TRUE if the Window is minimized.
   */
  public boolean isMinimized() {
    return getWindowConfigurer().getWindow().getShell().getMinimized();
  }

  private void registerListeners() {

    /* Add Shell sListener */
    getWindowConfigurer().getWindow().getShell().addShellListener(new ShellAdapter() {
      @Override
      public void shellIconified(ShellEvent e) {
        if (!fBlockIconifyEvent)
          onMinimize();
      }
    });

    /* Listen on Preferences Changes */
    fPrefListener = new PreferenceListener() {
      public void entitiesAdded(Set<PreferenceEvent> events) {
        onPreferencesChange(events, EventType.PERSIST);
      }

      public void entitiesDeleted(Set<PreferenceEvent> events) {
        onPreferencesChange(events, EventType.REMOVE);
      }

      public void entitiesUpdated(Set<PreferenceEvent> events) {
        onPreferencesChange(events, EventType.UPDATE);
      }
    };
    DynamicDAO.getDAO(IPreferenceDAO.class).addEntityListener(fPrefListener);
  }

  private void unregisterListeners() {
    DynamicDAO.getDAO(IPreferenceDAO.class).removeEntityListener(fPrefListener);
  }

  private void onPreferencesChange(Set<PreferenceEvent> events, EventType type) {
    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();
    boolean useTray = false;
    boolean affectsTray = false;

    for (PreferenceEvent event : events) {
      String key = event.getEntity().getKey();

      /* Tray Preference Change: Tray on Minimize */
      if (DefaultPreferences.TRAY_ON_MINIMIZE.equals(key)) {
        affectsTray = true;
        if (type == EventType.REMOVE)
          useTray = defaultScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE) || fPreferences.getBoolean(DefaultPreferences.TRAY_ON_CLOSE);
        else
          useTray = event.getEntity().getBoolean() || fPreferences.getBoolean(DefaultPreferences.TRAY_ON_CLOSE);
      }

      /* Tray Preference Change: Tray on Close */
      else if (DefaultPreferences.TRAY_ON_CLOSE.equals(key)) {
        affectsTray = true;
        if (type == EventType.REMOVE)
          useTray = defaultScope.getBoolean(DefaultPreferences.TRAY_ON_CLOSE) || fPreferences.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE);
        else
          useTray = event.getEntity().getBoolean() || fPreferences.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE);
      }

      /* Enable Tray */
      if (affectsTray && useTray && !fTrayEnabled) {
        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
          public void run() {
            enableTray();
          }
        });
      }

      /* Disable Tray */
      else if (affectsTray && !useTray && fTrayEnabled) {
        PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
          public void run() {
            disableTray();
          }
        });
      }
    }
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#dispose()
   */
  @Override
  public void dispose() {
    unregisterListeners();

    if (fTrayItem != null)
      fTrayItem.dispose();

    if (fNewsListener != null)
      DynamicDAO.removeEntityListener(INews.class, fNewsListener);

    fResources.dispose();
  }

  private void onMinimize() {

    /* Mark displayed News as Read on Minimize if set in Preferences */
    IEditorPart activeEditor = OwlUI.getActiveEditor();
    if (activeEditor != null && activeEditor instanceof FeedView) {
      FeedView feedView = (FeedView) activeEditor;
      feedView.notifyUIEvent(FeedView.UIEvent.MINIMIZE);
    }
  }

  private void onClose() {

    /* Mark new News as Unread on Close */
    IEditorPart activeEditor = OwlUI.getActiveEditor();
    if (activeEditor != null && activeEditor instanceof FeedView) {
      FeedView feedView = (FeedView) activeEditor;
      feedView.notifyUIEvent(FeedView.UIEvent.CLOSE);
    }
  }

  /* Enable System-Tray Support */
  private boolean enableTray() {

    /* Avoid that this is being called redundantly */
    if (fTrayEnabled)
      return true;

    /* Only enable for Primary Window */
    IWorkbenchWindow primaryWindow = OwlUI.getPrimaryWindow();
    if (primaryWindow == null || !primaryWindow.equals(getWindowConfigurer().getWindow()))
      return false;

    final Shell shell = primaryWindow.getShell();
    final Tray tray = shell.getDisplay().getSystemTray();

    /* Tray not support on the OS */
    if (tray == null)
      return false;

    /* Create Item in Tray */
    fTrayItem = new TrayItem(tray, SWT.NONE);
    fTrayItem.setToolTipText("RSSOwl");
    fTrayEnabled = true;

    if (Application.IS_WINDOWS)
      fTrayItem.setVisible(false);

    /* Apply Image */
    fTrayItem.setImage(OwlUI.getImage(fResources, OwlUI.TRAY_OWL));

    /* Minimize to Tray on Shell Iconify if set */
    fTrayShellListener = new ShellAdapter() {

      @Override
      public void shellIconified(ShellEvent e) {
        if (!fBlockIconifyEvent && (fMinimizeFromClose || fPreferences.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE)))
          moveToTray(shell);
      }
    };
    shell.addShellListener(fTrayShellListener);

    /* Show Menu on Selection */
    fTrayItem.addListener(SWT.MenuDetect, new Listener() {
      public void handleEvent(Event event) {
        MenuManager trayMenu = new MenuManager();

        /* Restore */
        trayMenu.add(new ContributionItem() {
          @Override
          public void fill(Menu menu, int index) {
            MenuItem restoreItem = new MenuItem(menu, SWT.PUSH);
            restoreItem.setText("Restore");
            restoreItem.addSelectionListener(new SelectionAdapter() {
              @Override
              public void widgetSelected(SelectionEvent e) {
                restoreFromTray(shell);
              }
            });
            menu.setDefaultItem(restoreItem);
          }
        });

        /* Separator */
        trayMenu.add(new Separator());

        /* Other Items */
        fActionBarAdvisor.fillTrayItem(trayMenu);

        Menu menu = trayMenu.createContextMenu(shell);
        menu.setVisible(true);
      }
    });

    /* Handle DefaultSelection */
    fTrayItem.addListener(SWT.DefaultSelection, new Listener() {
      public void handleEvent(Event event) {

        /* Restore from Tray */
        if (!shell.isVisible())
          restoreFromTray(shell);

        /* Move to Tray */
        else if (!Application.IS_WINDOWS)
          moveToTray(shell);
      }
    });

    /* Indicate new News in Tray */
    fNewsListener = new NewsAdapter() {

      @Override
      public void entitiesAdded(Set<NewsEvent> events) {
        JobRunner.runInUIThread(fTrayItem, new Runnable() {
          public void run() {

            /* Update Icon only when Tray is visible and not yet teasing */
            if (!fTrayItem.getVisible() || fTrayTeasing || shell.getVisible())
              return;

            fTrayTeasing = true;
            fTrayItem.setImage(OwlUI.getImage(fResources, OwlUI.TRAY_OWL_TEASING));
          }
        });
      }
    };
    DynamicDAO.addEntityListener(INews.class, fNewsListener);

    return true;
  }

  /* Move to System Tray */
  private void moveToTray(Shell shell) {
    if (Application.IS_WINDOWS)
      fTrayItem.setVisible(true);

    /*
     * Bug in SWT: For some reason, calling setVisible(false) here will result
     * in a second Iconify Event. The fix is to disable processing of this event
     * meanwhile.
     */
    fBlockIconifyEvent = true;
    try {
      shell.setVisible(false);
    } finally {
      fBlockIconifyEvent = false;
    }

    fMinimizedToTray = true;
  }

  /**
   * @param shell
   */
  public void restoreFromTray(Shell shell) {

    /* Restore Shell */
    shell.setVisible(true);
    shell.setActive();

    /* Un-Minimize if minimized */
    if (shell.getMinimized())
      shell.setMinimized(false);

    if (Application.IS_WINDOWS)
      fTrayItem.setVisible(false);

    if (fTrayTeasing)
      fTrayItem.setImage(OwlUI.getImage(fResources, OwlUI.TRAY_OWL));

    fTrayTeasing = false;
    fMinimizedToTray = false;
  }

  /* Disable System-Tray Support */
  private void disableTray() {

    /* Avoid that this is being called redundantly */
    if (!fTrayEnabled)
      return;

    /* First make sure to have the Window restored */
    restoreFromTray(getWindowConfigurer().getWindow().getShell());

    fTrayEnabled = false;
    fMinimizedToTray = false;

    if (fTrayItem != null)
      fTrayItem.dispose();

    if (fNewsListener != null)
      DynamicDAO.removeEntityListener(INews.class, fNewsListener);

    if (fTrayShellListener != null)
      getWindowConfigurer().getWindow().getShell().removeShellListener(fTrayShellListener);
  }

  /* Support for focusless scrolling */
  private void hookFocuslessScrolling(final Display display) {
    display.addFilter(SWT.MouseWheel, new Listener() {
      public void handleEvent(Event event) {
        Control control = display.getCursorControl();

        /* Control must be non-focus undisposed */
        if (control == null || control.isDisposed() || control.isFocusControl())
          return;

        /* Pass focus to control and disable event if allowed */
        boolean isBrowser = SWT_BROWSER_WIN.equals(control.getClass().getName());
        if (isBrowser || control.getData(FOCUSLESS_SCROLL_HOOK) != null) {

          /* Break Condition */
          control.setFocus();

          /* Re-Post Event to Cursor Control */
          event.doit = false;
          event.widget = control;
          display.post(event);
        }
      }
    });
  }

  /*
   * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#getWindowConfigurer()
   */
  @Override
  public IWorkbenchWindowConfigurer getWindowConfigurer() {
    return super.getWindowConfigurer();
  }
}