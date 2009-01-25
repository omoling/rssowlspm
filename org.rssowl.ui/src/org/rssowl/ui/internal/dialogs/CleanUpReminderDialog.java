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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * A reminder for the user to perform clean up in certain intervals.
 *
 * @author bpasero@rssowl.org
 */
public class CleanUpReminderDialog extends TitleAreaDialog {

  /* Keep the visible instance saved */
  private static CleanUpReminderDialog fVisibleInstance;

  private LocalResourceManager fResources;
  private IPreferenceScope fPreferences = Owl.getPreferenceService().getGlobalScope();

  /**
   * @param parentShell
   */
  public CleanUpReminderDialog(Shell parentShell) {
    super(parentShell);
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /**
   * @return Returns an instance of <code>CleanUpReminderDialog</code> or
   * <code>NULL</code> in case no instance is currently open.
   */
  public static CleanUpReminderDialog getVisibleInstance() {
    return fVisibleInstance;
  }

  /*
   * @see org.eclipse.jface.window.Window#open()
   */
  @Override
  public int open() {
    fVisibleInstance = this;
    return super.open();
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#close()
   */
  @Override
  public boolean close() {
    fVisibleInstance = null;
    fResources.dispose();
    return super.close();
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Reminder to perform Clean-Up");
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/cleanup_wiz.gif"));

    /* Title Message */
    showInfo();

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(1, 5, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    Label infoLabel = new Label(composite, SWT.WRAP);
    infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    infoLabel.setText("Would you like to open the clean-up wizard now?");

    Composite controlsContainer = new Composite(composite, SWT.NONE);
    controlsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    controlsContainer.setLayout(LayoutUtils.createGridLayout(3, 0, 0, 5, 0, false));
    ((GridLayout) controlsContainer.getLayout()).marginTop = 25;

    final Button reminderCheck = new Button(controlsContainer, SWT.CHECK);
    reminderCheck.setText("Remind me every ");
    reminderCheck.setSelection(fPreferences.getBoolean(DefaultPreferences.CLEAN_UP_REMINDER_STATE));

    final Spinner reminderDaysValue = new Spinner(controlsContainer, SWT.BORDER);
    reminderDaysValue.setMaximum(100);
    reminderDaysValue.setMinimum(1);
    reminderDaysValue.setSelection(fPreferences.getInteger(DefaultPreferences.CLEAN_UP_REMINDER_DAYS_VALUE));
    reminderDaysValue.setEnabled(reminderCheck.getSelection());
    reminderDaysValue.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fPreferences.putInteger(DefaultPreferences.CLEAN_UP_REMINDER_DAYS_VALUE, reminderDaysValue.getSelection());
      }
    });

    reminderCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        reminderDaysValue.setEnabled(reminderCheck.getSelection());
        fPreferences.putBoolean(DefaultPreferences.CLEAN_UP_REMINDER_STATE, reminderCheck.getSelection());
      }
    });

    Label reminderDaysLabel = new Label(controlsContainer, SWT.NONE);
    reminderDaysLabel.setText(" days.");

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    return composite;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.YES_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.NO_LABEL, false);
  }

  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    int style = SWT.TITLE | SWT.BORDER | getDefaultOrientation();

    /* Follow Apple's Human Interface Guidelines for Application Modal Dialogs */
    if (!Application.IS_MAC)
      style |= SWT.CLOSE;

    return style;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();

    Shell shell = getShell();

    /* Minimum Size */
    int minWidth = convertHorizontalDLUsToPixels(OwlUI.MIN_DIALOG_WIDTH_DLU);
    int minHeight = shell.computeSize(minWidth, SWT.DEFAULT).y;

    /* Required Size */
    Point requiredSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

    shell.setSize(Math.max(minWidth, requiredSize.x), Math.max(minHeight, requiredSize.y));
    LayoutUtils.positionShell(shell, false);
  }

  private void showInfo() {
    setErrorMessage(null);
    setMessage("As time goes on, unused bookmarks may accumulate in your feed reader.\n The Clean-up Wizard can help with this as well as optimize your system.", IMessageProvider.INFORMATION);
  }
}