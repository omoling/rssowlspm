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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * Generic Dialog for asking for confirmation when deleting something.
 *
 * @author bpasero
 */
public class ConfirmDeleteDialog extends TitleAreaDialog {

  /* Min. width of the dialog in DLUs */
  private static final int dialogMinWidth = 320;

  private String fDialogHeaderMessage;
  private String fDialogMessage;
  private Button fNeverAskAgainCheck;
  private String fTitle;
  private LocalResourceManager fResources;
  private String fConfirmPrefKey;
  private IPreferenceScope fPreferences;

  /**
   * Instantiate a new ConfirmDeleteDialog
   *
   * @param parentShell The parent shell
   * @param title The title of the dialog
   * @param dialogHeaderMessage The info message
   * @param dialogMessage The dialog message
   * @param confirmPrefKey The key to the boolean preference to receive the
   * setting for "Never ask again"
   */
  public ConfirmDeleteDialog(Shell parentShell, String title, String dialogHeaderMessage, String dialogMessage, String confirmPrefKey) {
    super(parentShell);
    fTitle = title;
    fDialogMessage = dialogMessage;
    fDialogHeaderMessage = dialogHeaderMessage;
    fConfirmPrefKey = confirmPrefKey;
    fResources = new LocalResourceManager(JFaceResources.getResources());
    fPreferences = Owl.getPreferenceService().getGlobalScope();
  }

  /*
   * @see org.eclipse.jface.dialogs.TrayDialog#close()
   */
  @Override
  public boolean close() {
    fResources.dispose();

    return super.close();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#okPressed()
   */
  @Override
  protected void okPressed() {
    if (fNeverAskAgainCheck != null && fNeverAskAgainCheck.getSelection())
      fPreferences.putBoolean(fConfirmPrefKey, false);

    super.okPressed();
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText(fTitle);
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Composite to hold all components */
    Composite composite = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(1, 5, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "/icons/wizban/trash.gif"));

    /* Title Message */
    setMessage(fDialogHeaderMessage, IMessageProvider.WARNING);

    /* Dialog Message */
    Label dialogMessageLabel = new Label(composite, SWT.WRAP);
    dialogMessageLabel.setText(fDialogMessage);
    dialogMessageLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Spacer */
    new Label(composite, SWT.NONE);

    /* Checkbox to disable confirm dialog */
    if (fConfirmPrefKey != null) {
      fNeverAskAgainCheck = new Button(composite, SWT.CHECK);
      fNeverAskAgainCheck.setText("Never ask again");
    }

    /* Holder for the separator to the OK and Cancel buttons */
    Composite sepHolder = new Composite(parent, SWT.NONE);
    sepHolder.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    sepHolder.setLayout(LayoutUtils.createGridLayout(1, 0, 0));

    /* Separator */
    Label separator = new Label(sepHolder, SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    return composite;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Delete", true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    getButton(IDialogConstants.OK_ID).setFocus();
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();
    Point bestSize = getShell().computeSize(convertHorizontalDLUsToPixels(dialogMinWidth), SWT.DEFAULT);
    Point location = getInitialLocation(bestSize);
    getShell().setBounds(location.x, location.y, bestSize.x, bestSize.y);
  }
}