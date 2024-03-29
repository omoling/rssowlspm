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

package org.rssowl.ui.internal.dialogs.bookmark;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.FolderChooser;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * @author bpasero
 */
public class BookmarkDefinitionPage extends WizardPage {
  private Text fNameInput;
  private FolderChooser fFolderChooser;
  private final IFolder fSelectedFolder;

  String getBookmarkName() {
    return fNameInput.getText();
  }

  IFolder getFolder() {
    return fFolderChooser.getFolder();
  }

  /**
   * @param name
   */
  public void presetBookmarkName(final String name) {
    JobRunner.runInUIThread(fNameInput, new Runnable() {
      public void run() {
        if (StringUtils.isSet(name)) {
          setMessage("Create a new Bookmark to read News from a Feed.");
          fNameInput.setText(name);
          fNameInput.selectAll();
        } else {
          setMessage("Unable to load the title from the feed", IMessageProvider.WARNING);
        }
      }
    });
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {
    return fNameInput.getText().length() > 0;
  }

  /**
   * @param pageName
   * @param selectedFolder
   */
  protected BookmarkDefinitionPage(String pageName, IFolder selectedFolder) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/bkmrk_wiz.gif"));
    fSelectedFolder = selectedFolder;
    setMessage("Create a new Bookmark to read News from a Feed.");
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    if (visible)
      ((CreateBookmarkWizard) getWizard()).loadNameFromFeed();

    super.setVisible(visible);
    fNameInput.setFocus();
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(2, false));

    /* Name */
    Label nameLabel = new Label(container, SWT.None);
    nameLabel.setText("Name: ");

    fNameInput = new Text(container, SWT.BORDER);
    fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fNameInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        getContainer().updateButtons();
      }
    });

    /* Location */
    Composite labelContainer = new Composite(container, SWT.None);
    labelContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 3));
    labelContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false));

    Label locationLabel = new Label(labelContainer, SWT.None);
    locationLabel.setText("Location: ");

    fFolderChooser = new FolderChooser(container, fSelectedFolder, SWT.BORDER, false);
    fFolderChooser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fFolderChooser.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 5, false));
    fFolderChooser.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    setControl(container);
  }
}