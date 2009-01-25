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

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * Container for all Preferences that have not yet been categorized.
 *
 * @author bpasero
 */
public class MiscPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  /** ID of this Preferences Page */
  public static final String ID = "org.rssowl.ui.MiscPreferences";

  private IPreferenceScope fGlobalScope;
  private IPreferenceScope fEclipseScope;
  private Button fMinimizeToTray;
  private Button fMoveToTrayOnStart;
  private Button fMoveToTrayOnExit;
  private Text fCustomBrowserInput;
  private Button fUseCustomExternalBrowser;
  private Button fUseDefaultExternalBrowser;
  //TODO UserStory 2: This Button has been removed
  //private Button fUseInternalBrowser;
  private Button fCustomBrowserSearchButton;
  private Spinner fAutoCloseTabsSpinner;
  private Button fAutoCloseTabsCheck;
  private Button fUseMultipleTabsCheck;
  private Button fReopenFeedsOnStartupCheck;
  private Button fAlwaysReuseFeedView;
  //TODO Userstory 2: Choice box "Load browser tab in background" is not needed anymore
  //private Button fLoadBrowserTabInBackground;

  /** Leave for reflection */
  public MiscPreferencePage() {
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
    fEclipseScope = Owl.getPreferenceService().getEclipseScope();
  }

  /**
   * @param title
   */
  public MiscPreferencePage(String title) {
    super(title);
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {}

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createComposite(parent);

    /* Browser Options */
    createBrowserOptions(container);

    /* View Options */
    createViewOptions(container);

    /* System Tray Options */
    createTrayOptions(container);

    return container;
  }

  private void createViewOptions(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setText("View");
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* View Group */
    Composite viewGroup = new Composite(container, SWT.None);
    viewGroup.setLayout(LayoutUtils.createGridLayout(1, 10, 5));
    viewGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fReopenFeedsOnStartupCheck = new Button(viewGroup, SWT.CHECK);
    fReopenFeedsOnStartupCheck.setText("Re-Open last opened feeds on startup");
    fReopenFeedsOnStartupCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS));

    fAlwaysReuseFeedView = new Button(viewGroup, SWT.CHECK);
    fAlwaysReuseFeedView.setText("Always open feeds in the same tab");
    fAlwaysReuseFeedView.setSelection(fGlobalScope.getBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW));

    //TODO Userstory 2: Choice box has been removed
    /*fLoadBrowserTabInBackground = new Button(viewGroup, SWT.CHECK);
    fLoadBrowserTabInBackground.setText("Open browser tabs in the background");
    fLoadBrowserTabInBackground.setSelection(fGlobalScope.getBoolean(DefaultPreferences.OPEN_BROWSER_IN_BACKGROUND));
	*/

    fUseMultipleTabsCheck = new Button(viewGroup, SWT.CHECK);
    fUseMultipleTabsCheck.setText("Show multiple tabs side by side");
    fUseMultipleTabsCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS));
    //TODO Userstory 2: Removed selection listener
    /*fUseMultipleTabsCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fLoadBrowserTabInBackground.setEnabled(fUseInternalBrowser.getSelection() && fUseMultipleTabsCheck.getSelection());
      }
    });*/

    Composite autoCloseTabsContainer = new Composite(viewGroup, SWT.None);
    autoCloseTabsContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0, 0, 2, false));

    fAutoCloseTabsCheck = new Button(autoCloseTabsContainer, SWT.CHECK);
    fAutoCloseTabsCheck.setText("Never show more than  ");
    fAutoCloseTabsCheck.setSelection(fEclipseScope.getBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS));
    fAutoCloseTabsCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fAutoCloseTabsSpinner.setEnabled(fAutoCloseTabsCheck.getSelection());
      }
    });

    fAutoCloseTabsSpinner = new Spinner(autoCloseTabsContainer, SWT.BORDER);
    fAutoCloseTabsSpinner.setMinimum(1);
    fAutoCloseTabsSpinner.setMaximum(100);
    fAutoCloseTabsSpinner.setSelection(fEclipseScope.getInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD));
    fAutoCloseTabsSpinner.setEnabled(fAutoCloseTabsCheck.getSelection());

    label = new Label(autoCloseTabsContainer, SWT.None);
    label.setText(fAutoCloseTabsSpinner.getSelection() == 1 ? " tab" : " tabs");

    //TODO Userstory 2: Checkbox "Open browser tab in the background" has been removed
    //fLoadBrowserTabInBackground.setEnabled(fUseInternalBrowser.getSelection() && fUseMultipleTabsCheck.getSelection());
  }

  private void createTrayOptions(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setText("Move to the System Tray");
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* System Tray Group */
    Composite trayGroup = new Composite(container, SWT.None);
    trayGroup.setLayout(LayoutUtils.createGridLayout(1, 10, 5));
    trayGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Enable / Disable Tray */
    fMinimizeToTray = new Button(trayGroup, SWT.CHECK);
    fMinimizeToTray.setText("when minimizing RSSOwl");
    fMinimizeToTray.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE));

    /* Move to Tray on Start */
    fMoveToTrayOnStart = new Button(trayGroup, SWT.CHECK);
    fMoveToTrayOnStart.setText("when starting RSSOwl");
    fMoveToTrayOnStart.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_START));

    /* Move to Tray on Close */
    fMoveToTrayOnExit = new Button(trayGroup, SWT.CHECK);
    fMoveToTrayOnExit.setText("when closing RSSOwl");
    fMoveToTrayOnExit.setSelection(fGlobalScope.getBoolean(DefaultPreferences.TRAY_ON_CLOSE));
  }

  private void createBrowserOptions(Composite container) {
    Label label = new Label(container, SWT.NONE);
    label.setText("Browser");
    label.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

    /* Browser Group */
    Composite browserGroup = new Composite(container, SWT.None);
    browserGroup.setLayout(LayoutUtils.createGridLayout(2, 10, 5));
    browserGroup.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    //TODO Userstory 2: Radio button "Open in Internal Browser" has been removed
    /* Use internal Browser */
    /*fUseInternalBrowser = new Button(browserGroup, SWT.RADIO);
    fUseInternalBrowser.setText("Use internal Browser");
    fUseInternalBrowser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fUseInternalBrowser.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fLoadBrowserTabInBackground.setEnabled(fUseInternalBrowser.getSelection() && fUseMultipleTabsCheck.getSelection());
      }
    });*/

    /* Use default external Browser */
    fUseDefaultExternalBrowser = new Button(browserGroup, SWT.RADIO);
    fUseDefaultExternalBrowser.setText("Use default external Browser");
    fUseDefaultExternalBrowser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fUseDefaultExternalBrowser.setSelection(fGlobalScope.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER));

    /* Use custom external Browser */
    fUseCustomExternalBrowser = new Button(browserGroup, SWT.RADIO);
    fUseCustomExternalBrowser.setText("Use the following external Browser:");
    fUseCustomExternalBrowser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));
    fUseCustomExternalBrowser.setSelection(fGlobalScope.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER));
    fUseCustomExternalBrowser.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fCustomBrowserInput.setEnabled(fUseCustomExternalBrowser.getSelection());
        fCustomBrowserSearchButton.setEnabled(fUseCustomExternalBrowser.getSelection());
      }
    });

    //TODO Userstory 2: Button has been removed
    //fUseInternalBrowser.setSelection(!fUseDefaultExternalBrowser.getSelection() && !fUseCustomExternalBrowser.getSelection());

    fCustomBrowserInput = new Text(browserGroup, SWT.BORDER);
    fCustomBrowserInput.setEnabled(fUseCustomExternalBrowser.getSelection());
    fCustomBrowserInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

    String customBrowserValue = fGlobalScope.getString(DefaultPreferences.CUSTOM_BROWSER_PATH);
    if (customBrowserValue != null)
      fCustomBrowserInput.setText(customBrowserValue);

    fCustomBrowserSearchButton = new Button(browserGroup, SWT.PUSH);
    fCustomBrowserSearchButton.setText("Search...");
    fCustomBrowserSearchButton.setEnabled(fUseCustomExternalBrowser.getSelection());
    fCustomBrowserSearchButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
        dialog.setFileName(fCustomBrowserInput.getText());
        String path = dialog.open();
        if (path != null)
          fCustomBrowserInput.setText(path);
      }
    });
  }

  private Composite createComposite(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());
    return composite;
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
  public boolean performOk() {
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS, fReopenFeedsOnStartupCheck.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW, fAlwaysReuseFeedView.getSelection());
    
    //TODO Userstory 2: Preference is not needed anymore
    //fGlobalScope.putBoolean(DefaultPreferences.OPEN_BROWSER_IN_BACKGROUND, fLoadBrowserTabInBackground.getSelection());
    
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS, fUseMultipleTabsCheck.getSelection());
    fEclipseScope.putBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS, fAutoCloseTabsCheck.getSelection());
    fEclipseScope.putInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD, fAutoCloseTabsSpinner.getSelection());

    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_MINIMIZE, fMinimizeToTray.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_START, fMoveToTrayOnStart.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.TRAY_ON_CLOSE, fMoveToTrayOnExit.getSelection());

    fGlobalScope.putBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER, fUseDefaultExternalBrowser.getSelection());
    fGlobalScope.putBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER, fUseCustomExternalBrowser.getSelection());
    fGlobalScope.putString(DefaultPreferences.CUSTOM_BROWSER_PATH, fCustomBrowserInput.getText());

    return super.performOk();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
  protected void performDefaults() {
    super.performDefaults();

    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();

    fReopenFeedsOnStartupCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.ECLIPSE_RESTORE_TABS));
    fAlwaysReuseFeedView.setSelection(defaultScope.getBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW));
    
    //TODO Userstory 2: Preference does not exist anymore
    //fLoadBrowserTabInBackground.setSelection(defaultScope.getBoolean(DefaultPreferences.OPEN_BROWSER_IN_BACKGROUND));
    
    fUseMultipleTabsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.ECLIPSE_MULTIPLE_TABS));
    fAutoCloseTabsCheck.setSelection(defaultScope.getBoolean(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS));
    fAutoCloseTabsSpinner.setSelection(defaultScope.getInteger(DefaultPreferences.ECLIPSE_AUTOCLOSE_TABS_THRESHOLD));
    fAutoCloseTabsSpinner.setEnabled(fAutoCloseTabsCheck.getSelection());

    fMinimizeToTray.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_MINIMIZE));
    fMoveToTrayOnStart.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_START));
    fMoveToTrayOnExit.setSelection(defaultScope.getBoolean(DefaultPreferences.TRAY_ON_CLOSE));

    fUseDefaultExternalBrowser.setSelection(defaultScope.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER));
    fUseCustomExternalBrowser.setSelection(defaultScope.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER));
    
    //TODO Userstory 2: Radio button has been removed
    //fUseInternalBrowser.setSelection(!fUseDefaultExternalBrowser.getSelection() && !fUseDefaultExternalBrowser.getSelection());

    fCustomBrowserInput.setEnabled(fUseCustomExternalBrowser.getSelection());
    fCustomBrowserSearchButton.setEnabled(fUseCustomExternalBrowser.getSelection());

    //TODO Userstory 2: Checkbox has been removed
    //fLoadBrowserTabInBackground.setEnabled(fUseInternalBrowser.getSelection() && fUseMultipleTabsCheck.getSelection());
  }
}