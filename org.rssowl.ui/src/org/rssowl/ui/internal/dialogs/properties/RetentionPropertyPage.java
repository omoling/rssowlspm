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

package org.rssowl.ui.internal.dialogs.properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.RetentionStrategy;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Retention Properties.
 *
 * @author bpasero
 */
public class RetentionPropertyPage implements IEntityPropertyPage {
  private List<IEntity> fEntities;
  private Spinner fMaxCountSpinner;
  private Spinner fMaxAgeSpinner;
  private Button fDeleteNewsByCountCheck;
  private Button fDeleteNewsByAgeCheck;
  private Button fDeleteReadNewsCheck;
  private Button fNeverDeleteUnreadNewsCheck;
  private boolean fSettingsChanged;

  /* Settings */
  private List<IPreferenceScope> fEntityPreferences;
  private boolean fPrefDeleteNewsByCountState;
  private int fPrefDeleteNewsByCountValue;
  private boolean fPrefDeleteNewsByAgeState;
  private int fPrefDeleteNewsByAgeValue;
  private boolean fPrefDeleteReadNews;
  private boolean fPrefNeverDeleteUnReadNews;

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite,
   * java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    Assert.isTrue(!entities.isEmpty());
    fEntities = entities;

    /* Load Entity Preferences */
    fEntityPreferences = new ArrayList<IPreferenceScope>(fEntities.size());
    for (IEntity entity : entities)
      fEntityPreferences.add(Owl.getPreferenceService().getEntityScope(entity));

    /* Load initial Settings */
    loadInitialSettings();
  }

  private void loadInitialSettings() {

    /* Take the first scope as initial values */
    IPreferenceScope firstScope = fEntityPreferences.get(0);
    fPrefDeleteNewsByCountState = firstScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE);
    fPrefDeleteNewsByCountValue = firstScope.getInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE);
    fPrefDeleteNewsByAgeState = firstScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE);
    fPrefDeleteNewsByAgeValue = firstScope.getInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE);
    fPrefDeleteReadNews = firstScope.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE);
    fPrefNeverDeleteUnReadNews = firstScope.getBoolean(DefaultPreferences.NEVER_DEL_UNREAD_NEWS_STATE);

    /* For any other scope not sharing the initial values, use the default */
    IPreferenceScope defaultScope = Owl.getPreferenceService().getDefaultScope();
    for (int i = 1; i < fEntityPreferences.size(); i++) {
      IPreferenceScope otherScope = fEntityPreferences.get(i);

      if (otherScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE) != fPrefDeleteNewsByCountState)
        fPrefDeleteNewsByCountState = defaultScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE);

      if (otherScope.getInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE) != fPrefDeleteNewsByCountValue)
        fPrefDeleteNewsByCountValue = defaultScope.getInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE);

      if (otherScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE) != fPrefDeleteNewsByAgeState)
        fPrefDeleteNewsByAgeState = defaultScope.getBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE);

      if (otherScope.getInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE) != fPrefDeleteNewsByAgeValue)
        fPrefDeleteNewsByAgeValue = defaultScope.getInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE);

      if (otherScope.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE) != fPrefDeleteReadNews)
        fPrefDeleteReadNews = defaultScope.getBoolean(DefaultPreferences.DEL_READ_NEWS_STATE);

      if (otherScope.getBoolean(DefaultPreferences.NEVER_DEL_UNREAD_NEWS_STATE) != fPrefNeverDeleteUnReadNews)
        fPrefNeverDeleteUnReadNews = defaultScope.getBoolean(DefaultPreferences.NEVER_DEL_UNREAD_NEWS_STATE);
    }
  }

  /*
   * @see org.rssowl.ui.internal.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 10, 10, 5, false));

    /* Explanation Label */
    Label explanationLabel = new Label(container, SWT.WRAP);
    explanationLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
    explanationLabel.setText("To recover disk space, old news can be permanently deleted.");

    /* Delete by Count */
    fDeleteNewsByCountCheck = new Button(container, SWT.CHECK);
    fDeleteNewsByCountCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fDeleteNewsByCountCheck.setSelection(fPrefDeleteNewsByCountState);
    fDeleteNewsByCountCheck.setText("Maximum number of news to keep: ");
    fDeleteNewsByCountCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fMaxCountSpinner.setEnabled(fDeleteNewsByCountCheck.getSelection());
      }
    });

    fMaxCountSpinner = new Spinner(container, SWT.BORDER);
    fMaxCountSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fMaxCountSpinner.setEnabled(fDeleteNewsByCountCheck.getSelection());
    fMaxCountSpinner.setMinimum(0);
    fMaxCountSpinner.setMaximum(99999);
    fMaxCountSpinner.setSelection(fPrefDeleteNewsByCountValue);

    /* Delete by Age */
    fDeleteNewsByAgeCheck = new Button(container, SWT.CHECK);
    fDeleteNewsByAgeCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fDeleteNewsByAgeCheck.setSelection(fPrefDeleteNewsByAgeState);
    fDeleteNewsByAgeCheck.setText("Maximum age of news in days: ");
    fDeleteNewsByAgeCheck.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fMaxAgeSpinner.setEnabled(fDeleteNewsByAgeCheck.getSelection());
      }
    });

    fMaxAgeSpinner = new Spinner(container, SWT.BORDER);
    fMaxAgeSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
    fMaxAgeSpinner.setEnabled(fDeleteNewsByAgeCheck.getSelection());
    fMaxAgeSpinner.setMinimum(0);
    fMaxAgeSpinner.setMaximum(99999);
    fMaxAgeSpinner.setSelection(fPrefDeleteNewsByAgeValue);

    /* Delete by State */
    fDeleteReadNewsCheck = new Button(container, SWT.CHECK);
    fDeleteReadNewsCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
    fDeleteReadNewsCheck.setText("Always delete read news");
    fDeleteReadNewsCheck.setSelection(fPrefDeleteReadNews);

    /* Never Delete Unread News State */
    fNeverDeleteUnreadNewsCheck = new Button(container, SWT.CHECK);
    fNeverDeleteUnreadNewsCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
    fNeverDeleteUnreadNewsCheck.setText("Never delete unread news");
    fNeverDeleteUnreadNewsCheck.setSelection(fPrefNeverDeleteUnReadNews);

    return container;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#setFocus()
   */
  public void setFocus() {}

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {

    /* Update this Entity */
    for (IPreferenceScope scope : fEntityPreferences) {
      if (updatePreferences(scope)) {
        IEntity entityToSave = fEntities.get(fEntityPreferences.indexOf(scope));
        entitiesToSave.add(entityToSave);
        fSettingsChanged = true;
      }
    }

    /* Update changes in all Childs as well if Folder */
    for (IEntity entity : fEntities) {
      if (fSettingsChanged && entity instanceof IFolder)
        updateChildPreferences((IFolder) entity);
    }

    return true;
  }

  private void updateChildPreferences(IFolder folder) {

    /* Update changes to Child-BookMarks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark) {
        IPreferenceScope scope = Owl.getPreferenceService().getEntityScope(mark);
        updatePreferences(scope);
      }
    }

    /* Update changes to Child-Folders */
    List<IFolder> folders = folder.getFolders();
    for (IFolder childFolder : folders) {
      IPreferenceScope scope = Owl.getPreferenceService().getEntityScope(childFolder);
      updatePreferences(scope);

      /* Recursively Proceed */
      updateChildPreferences(childFolder);
    }
  }

  private boolean updatePreferences(IPreferenceScope scope) {
    boolean changed = false;

    /* Delete by Count */
    boolean bVal = fDeleteNewsByCountCheck.getSelection();
    if (fPrefDeleteNewsByCountState != bVal) {
      scope.putBoolean(DefaultPreferences.DEL_NEWS_BY_COUNT_STATE, bVal);
      changed = true;
    }

    int iVal = fMaxCountSpinner.getSelection();
    if (fPrefDeleteNewsByCountValue != iVal) {
      scope.putInteger(DefaultPreferences.DEL_NEWS_BY_COUNT_VALUE, iVal);
      changed = true;
    }

    /* Delete by Age */
    bVal = fDeleteNewsByAgeCheck.getSelection();
    if (fPrefDeleteNewsByAgeState != bVal) {
      scope.putBoolean(DefaultPreferences.DEL_NEWS_BY_AGE_STATE, bVal);
      changed = true;
    }

    iVal = fMaxAgeSpinner.getSelection();
    if (fPrefDeleteNewsByAgeValue != iVal) {
      scope.putInteger(DefaultPreferences.DEL_NEWS_BY_AGE_VALUE, iVal);
      changed = true;
    }

    /* Delete Read News */
    bVal = fDeleteReadNewsCheck.getSelection();
    if (fPrefDeleteReadNews != bVal) {
      scope.putBoolean(DefaultPreferences.DEL_READ_NEWS_STATE, bVal);
      changed = true;
    }

    /* Never Delete Unread News */
    bVal = fNeverDeleteUnreadNewsCheck.getSelection();
    if (fPrefNeverDeleteUnReadNews != bVal) {
      scope.putBoolean(DefaultPreferences.NEVER_DEL_UNREAD_NEWS_STATE, bVal);
      changed = true;
    }

    return changed;
  }

  /*
   * @see org.rssowl.ui.internal.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {

    /* Run Retention since settings changed */
    if (fSettingsChanged) {
      Job retentionJob = new Job("Performing clean-up...") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            monitor.beginTask("Performing clean-up...", fEntities.size());

            for (IEntity entity : fEntities) {
              if (entity instanceof IBookMark)
                RetentionStrategy.process((IBookMark) entity);
              else if (entity instanceof IFolder)
                RetentionStrategy.process((IFolder) entity);

              monitor.worked(1);
            }
          } finally {
            monitor.done();
          }

          return Status.OK_STATUS;
        }
      };

      retentionJob.schedule();
    }
  }
}