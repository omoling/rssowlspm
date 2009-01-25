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

package org.rssowl.ui.internal.dialogs.cleanup;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * @author bpasero
 */
public class CleanUpOptionsPage extends WizardPage {
  private Button fDeleteFeedByLastVisitCheck;
  private Spinner fMaxLastVisitSpinner;
  private Button fDeleteFeedByLastUpdateCheck;
  private Spinner fMaxLastUpdateSpinner;
  private Button fDeleteConErrorFeedCheck;
  private Button fDeleteDuplicateBookmarksCheck;
  private Button fDeleteNewsByCountCheck;
  private Spinner fMaxCountSpinner;
  private Button fDeleteNewsByAgeCheck;
  private Spinner fMaxAgeSpinner;
  private Button fDeleteReadNewsCheck;
  private Button fNeverDeleteUnreadNewsCheck;
  private IPreferenceScope fGlobalScope;

  /**
   * @param pageName
   */
  protected CleanUpOptionsPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/cleanup_wiz.gif"));
    setMessage("Please choose the desired operations that are used to clean up.");
    fGlobalScope = Owl.getPreferenceService().getGlobalScope();
  }

  /* Returns the selected clean up operations */
  CleanUpOperations getOperations() {

    /* Feed Operations */
    boolean lastVisitInDaysState = fDeleteFeedByLastVisitCheck.getSelection();
    int lastVisitInDays = fMaxLastVisitSpinner.getSelection();

    boolean lastUpdateInDaysState = fDeleteFeedByLastUpdateCheck.getSelection();
    int lastUpdateInDays = fMaxLastUpdateSpinner.getSelection();
    boolean deleteFeedsByConError = fDeleteConErrorFeedCheck.getSelection();
    boolean deleteFeedsByDuplicates = fDeleteDuplicateBookmarksCheck.getSelection();

    /* News Operations */
    boolean maxNewsCountPerFeedState = fDeleteNewsByCountCheck.getSelection();
    int maxNewsCountPerFeed = fMaxCountSpinner.getSelection();

    boolean maxNewsAgeState = fDeleteNewsByAgeCheck.getSelection();
    int maxNewsAge = fMaxAgeSpinner.getSelection();

    boolean deleteReadNews = fDeleteReadNewsCheck.getSelection();
    boolean keepUnreadNews = fNeverDeleteUnreadNewsCheck.getSelection();

    return new CleanUpOperations(lastVisitInDaysState, lastVisitInDays, lastUpdateInDaysState, lastUpdateInDays, deleteFeedsByConError, deleteFeedsByDuplicates, maxNewsCountPerFeedState, maxNewsCountPerFeed, maxNewsAgeState, maxNewsAge, deleteReadNews, keepUnreadNews);
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 10, 5, false));

    /* Options per Feed */
    createFeedOptions(container);

    /* Options per News */
    createNewsOptions(container);

    setControl(container);
  }

  private void createFeedOptions(Composite parent) {
    Group container = new Group(parent, SWT.None);
    container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    container.setLayout(LayoutUtils.createGridLayout(3));
    container.setText("Clean Up Bookmarks");

    /* 1.) Delete Feeds that have Last Visit > X Days ago */
    {
      fDeleteFeedByLastVisitCheck = new Button(container, SWT.CHECK);
      fDeleteFeedByLastVisitCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      fDeleteFeedByLastVisitCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_BM_BY_LAST_VISIT_STATE));
      fDeleteFeedByLastVisitCheck.setText("Delete bookmarks you have not displayed for ");
      fDeleteFeedByLastVisitCheck.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          fMaxLastVisitSpinner.setEnabled(fDeleteFeedByLastVisitCheck.getSelection());
        }
      });

      fMaxLastVisitSpinner = new Spinner(container, SWT.BORDER);
      fMaxLastVisitSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      fMaxLastVisitSpinner.setEnabled(fDeleteFeedByLastVisitCheck.getSelection());
      fMaxLastVisitSpinner.setMinimum(1);
      fMaxLastVisitSpinner.setMaximum(999);
      fMaxLastVisitSpinner.setSelection(fGlobalScope.getInteger(DefaultPreferences.CLEAN_UP_BM_BY_LAST_VISIT_VALUE));

      Label label = new Label(container, SWT.None);
      label.setText("days");
    }

    /* 2.) Delete Feeds that have not updated in X Days */
    {
      fDeleteFeedByLastUpdateCheck = new Button(container, SWT.CHECK);
      fDeleteFeedByLastUpdateCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      fDeleteFeedByLastUpdateCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_BM_BY_LAST_UPDATE_STATE));
      fDeleteFeedByLastUpdateCheck.setText("Delete bookmarks that have not been updated for ");
      fDeleteFeedByLastUpdateCheck.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          fMaxLastUpdateSpinner.setEnabled(fDeleteFeedByLastUpdateCheck.getSelection());
        }
      });

      fMaxLastUpdateSpinner = new Spinner(container, SWT.BORDER);
      fMaxLastUpdateSpinner.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      fMaxLastUpdateSpinner.setEnabled(fDeleteFeedByLastUpdateCheck.getSelection());
      fMaxLastUpdateSpinner.setMinimum(1);
      fMaxLastUpdateSpinner.setMaximum(999);
      fMaxLastUpdateSpinner.setSelection(fGlobalScope.getInteger(DefaultPreferences.CLEAN_UP_BM_BY_LAST_UPDATE_VALUE));

      Label label = new Label(container, SWT.None);
      label.setText("days");
    }

    /* 3.) Delete Duplicate Feeds*/
    {
      fDeleteDuplicateBookmarksCheck = new Button(container, SWT.CHECK);
      fDeleteDuplicateBookmarksCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
      fDeleteDuplicateBookmarksCheck.setText("Delete recently created duplicate bookmarks");
      fDeleteDuplicateBookmarksCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_BM_BY_DUPLICATES));
    }

    /* 4.) Delete Feeds that have Connection Error */
    {
      fDeleteConErrorFeedCheck = new Button(container, SWT.CHECK);
      fDeleteConErrorFeedCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
      fDeleteConErrorFeedCheck.setText("Delete bookmarks with a connection error");
      fDeleteConErrorFeedCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_BM_BY_CON_ERROR));
    }
  }

  private void createNewsOptions(Composite parent) {
    Group container = new Group(parent, SWT.None);
    container.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    container.setLayout(LayoutUtils.createGridLayout(2));
    container.setText("Clean Up News");

    /* 4.) Delete News that exceed a certain limit in a Feed */
    {
      fDeleteNewsByCountCheck = new Button(container, SWT.CHECK);
      fDeleteNewsByCountCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      fDeleteNewsByCountCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_NEWS_BY_COUNT_STATE));
      fDeleteNewsByCountCheck.setText("Maximum number of news to keep per feed: ");
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
      fMaxCountSpinner.setMaximum(9999);
      fMaxCountSpinner.setSelection(fGlobalScope.getInteger(DefaultPreferences.CLEAN_UP_NEWS_BY_COUNT_VALUE));
    }

    /* 5.) Delete News with an age > X Days */
    {
      fDeleteNewsByAgeCheck = new Button(container, SWT.CHECK);
      fDeleteNewsByAgeCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      fDeleteNewsByAgeCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_NEWS_BY_AGE_STATE));
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
      fMaxAgeSpinner.setMinimum(1);
      fMaxAgeSpinner.setMaximum(9999);
      fMaxAgeSpinner.setSelection(fGlobalScope.getInteger(DefaultPreferences.CLEAN_UP_NEWS_BY_AGE_VALUE));
    }

    /* 6.) Delete Read News */
    {
      fDeleteReadNewsCheck = new Button(container, SWT.CHECK);
      fDeleteReadNewsCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
      fDeleteReadNewsCheck.setText("Delete read news");
      fDeleteReadNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_READ_NEWS_STATE));
    }

    /* 7.) Do not delete Unread News */
    {
      fNeverDeleteUnreadNewsCheck = new Button(container, SWT.CHECK);
      fNeverDeleteUnreadNewsCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
      fNeverDeleteUnreadNewsCheck.setText("Do not delete unread news");
      fNeverDeleteUnreadNewsCheck.setSelection(fGlobalScope.getBoolean(DefaultPreferences.CLEAN_UP_NEVER_DEL_UNREAD_NEWS_STATE));
    }
  }
}