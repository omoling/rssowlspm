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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.DateUtils;
import org.rssowl.ui.internal.dialogs.CleanUpReminderDialog;
import org.rssowl.ui.internal.dialogs.cleanup.CleanUpWizard;
import org.rssowl.ui.internal.util.JobRunner;

/**
 * A simple service that controls showing a reminder for clean-up if required.
 *
 * @author bpasero@rssowl.org
 */
public class CleanUpReminderService {
  private final Job fReminderJob;
  private final IPreferenceScope fPreferences = Owl.getPreferenceService().getGlobalScope();

  CleanUpReminderService() {
    fReminderJob = createJob();
    fReminderJob.setSystem(true);
    fReminderJob.setUser(false);

    initIfNecessary();
    reschedule();
  }

  /* Check if this is the first start */
  private void initIfNecessary() {
    if (fPreferences.getBoolean(DefaultPreferences.CLEAN_UP_REMINDER_STATE)) {
      long millies = fPreferences.getLong(DefaultPreferences.CLEAN_UP_REMINDER_DATE_MILLIES);
      if (millies == 0)
        storeNextReminderDate();
    }
  }

  private void reschedule() {
    if (fPreferences.getBoolean(DefaultPreferences.CLEAN_UP_REMINDER_STATE)) {
      long nextReminderDate = fPreferences.getLong(DefaultPreferences.CLEAN_UP_REMINDER_DATE_MILLIES);
      long diff = nextReminderDate - System.currentTimeMillis();

      fReminderJob.schedule(diff > 0 ? diff : 0);
    }
  }

  private Job createJob() {
    return new Job("Clean-Up Reminder Service ") {
      @Override
      protected IStatus run(final IProgressMonitor monitor) {

        /* Check if Reminder should show */
        if (!monitor.isCanceled() && Platform.isRunning()) {

          /* Check if reminder is enabled */
          if (!fPreferences.getBoolean(DefaultPreferences.CLEAN_UP_REMINDER_STATE))
            return Status.OK_STATUS;

          /* Show Reminder */
          final Shell shell = OwlUI.getPrimaryShell();
          if (shell != null && !monitor.isCanceled() && Platform.isRunning()) {
            JobRunner.runSyncedInUIThread(shell, new Runnable() {
              public void run() {
                if (monitor.isCanceled() || !Platform.isRunning())
                  return;

                if (CleanUpReminderDialog.getVisibleInstance() == null && new CleanUpReminderDialog(shell).open() == IDialogConstants.OK_ID) {
                  CleanUpWizard cleanUpWizard = new CleanUpWizard();
                  WizardDialog dialog = new WizardDialog(shell, cleanUpWizard);
                  dialog.create();
                  dialog.open();
                };
              }
            });

            /* Store Next Date and reschedule */
            if (fPreferences.getBoolean(DefaultPreferences.CLEAN_UP_REMINDER_STATE)) {
              storeNextReminderDate();
              reschedule();
            }
          }
        }

        return Status.OK_STATUS;
      }
    };
  }

  private void storeNextReminderDate() {
    int days = fPreferences.getInteger(DefaultPreferences.CLEAN_UP_REMINDER_DAYS_VALUE);
    fPreferences.putLong(DefaultPreferences.CLEAN_UP_REMINDER_DATE_MILLIES, System.currentTimeMillis() + (days * DateUtils.DAY));
  }

  void stopService() {
    fReminderJob.cancel();
  }
}