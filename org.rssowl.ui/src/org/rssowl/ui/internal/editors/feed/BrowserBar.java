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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.ActionFactory;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.ApplicationServer;
import org.rssowl.ui.internal.CBrowser;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * The <code>BrowserBar</code> is providing controls to navigate a Browser.
 * This includes common actions like 'Forward' and 'Back'. A Text-Field is
 * provided to enter URLs.
 *
 * @author bpasero
 */
public class BrowserBar {

  /* Navigate Back */
  private static final String BACK_ACTION = "org.rssowl.ui.internal.editors.feed.BackAction";

  /* Navigate Forward */
  private static final String FORWARD_ACTION = "org.rssowl.ui.internal.editors.feed.ForwardAction";

  private CBrowser fBrowser;
  private Composite fParent;
  private ToolBarManager fNavigationToolBarManager;
  private Text fLocationInput;
  private FeedView fFeedView;
  private boolean fUseExternalBrowser;

  /**
   * @param feedView
   * @param parent
   */
  public BrowserBar(FeedView feedView, Composite parent) {
    fFeedView = feedView;
    fParent = parent;

    IPreferenceScope globalScope = Owl.getPreferenceService().getGlobalScope();
    fUseExternalBrowser = globalScope.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER) || globalScope.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER);
    createControl();
  }

  boolean isVisible() {
    return !fUseExternalBrowser;
  }

  /**
   * @param browser
   */
  public void init(CBrowser browser) {
    fBrowser = browser;
    registerListeners();
  }

  private void registerListeners() {
    LocationListener locationListener = new LocationAdapter() {
      @Override
      public void changed(LocationEvent event) {
        fNavigationToolBarManager.find(BACK_ACTION).update(IAction.ENABLED);
        fNavigationToolBarManager.find(FORWARD_ACTION).update(IAction.ENABLED);
      }
    };

    ProgressListener progressListener = new ProgressListener() {
      public void changed(ProgressEvent event) {
        if (!fLocationInput.isDisposed()) {
          String url = ((Browser) event.widget).getUrl();
          if (ApplicationServer.getDefault().isNewsServerUrl(url))
            fLocationInput.setText(""); //$NON-NLS-1$
          else if (!StringUtils.isSet(fLocationInput.getText()))
            fLocationInput.setText(URIUtils.ABOUT_BLANK.equals(url) ? "" : url); //$NON-NLS-1$
        }
      }

      /* Reset progress bar on completion */
      public void completed(ProgressEvent event) {
        if (!fLocationInput.isDisposed()) {
          String url = ((Browser) event.widget).getUrl();
          if (ApplicationServer.getDefault().isNewsServerUrl(url))
            fLocationInput.setText(""); //$NON-NLS-1$
          else
            fLocationInput.setText(URIUtils.ABOUT_BLANK.equals(url) ? "" : url); //$NON-NLS-1$
        }
      }
    };

    fBrowser.getControl().addLocationListener(locationListener);
    fBrowser.getControl().addProgressListener(progressListener);
  }

  private void createControl() {
    Composite container = new Composite(fParent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 3, 0));
    ((GridLayout) container.getLayout()).marginBottom = 2;
    container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    ((GridData) container.getLayoutData()).exclude = fUseExternalBrowser;

    /* Navigation ToolBar */
    createNavigationToolBar(container);

    /* Location Field */
    createLocationInput(container);
  }

  private void createNavigationToolBar(Composite parent) {
    fNavigationToolBarManager = new ToolBarManager(SWT.FLAT);

    /* Navigate Backward */
    IAction navBackward = new Action("Back") {
      @Override
      public void run() {
        fBrowser.back();
      }

      @Override
      public boolean isEnabled() {
        return fBrowser != null && fBrowser.getControl().isBackEnabled();
      }
    };
    navBackward.setId(BACK_ACTION);
    navBackward.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/backward.gif")); //$NON-NLS-1$
    navBackward.setDisabledImageDescriptor(OwlUI.getImageDescriptor("icons/dtool16/backward.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(navBackward);

    /* Navigate Forward */
    IAction navForward = new Action("Forward") {
      @Override
      public void run() {
        fBrowser.forward();
      }

      @Override
      public boolean isEnabled() {
        return fBrowser != null && fBrowser.getControl().isForwardEnabled();
      }
    };
    navForward.setId(FORWARD_ACTION);
    navForward.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/forward.gif")); //$NON-NLS-1$
    navForward.setDisabledImageDescriptor(OwlUI.getImageDescriptor("icons/dtool16/forward.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(navForward);

    /* Stop */
    IAction stopNav = new Action("Stop") {
      @Override
      public void run() {
        fBrowser.getControl().stop();
      }
    };
    stopNav.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/cancel.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(stopNav);

    /* Reload */
    IAction reload = new Action("Reload") {
      @Override
      public void run() {
        fBrowser.getControl().refresh();
      }
    };
    reload.setImageDescriptor(OwlUI.getImageDescriptor("icons/elcl16/reload.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(reload);

    /* Home */
    IAction navHome = new Action("Home") {
      @Override
      public void run() {
        fFeedView.getNewsBrowserControl().getViewer().home();
      }
    };
    navHome.setImageDescriptor(OwlUI.getImageDescriptor("icons/etool16/home.gif")); //$NON-NLS-1$
    fNavigationToolBarManager.add(navHome);

    fNavigationToolBarManager.createControl(parent);
  }

  private void createLocationInput(Composite parent) {
    fLocationInput = new Text(parent, SWT.BORDER | SWT.SINGLE);
    fLocationInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fLocationInput.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        if (StringUtils.isSet(fLocationInput.getText())) {
          fBrowser.setUrl(fLocationInput.getText());
          fBrowser.getControl().setFocus();
        }
      }
    });

    /* Register this Input Field to Context Service */
    Controller.getDefault().getContextService().registerInputField(fLocationInput);

    fLocationInput.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(true);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(true);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(true);
      }

      public void focusLost(FocusEvent e) {
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.CUT.getId()).setEnabled(false);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.COPY.getId()).setEnabled(false);
        fFeedView.getEditorSite().getActionBars().getGlobalActionHandler(ActionFactory.PASTE.getId()).setEnabled(false);
      }
    });
  }
}