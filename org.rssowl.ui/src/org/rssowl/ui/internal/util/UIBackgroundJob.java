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

package org.rssowl.ui.internal.util;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.progress.UIJob;
import org.rssowl.core.util.LoggingSafeRunnable;

/**
 * The <code>UIBackgroundJob</code> is capable of performing an operation in
 * the Background and updating the UI in the UI-Thread once this operation is
 * done.
 * 
 * @author bpasero
 */
public abstract class UIBackgroundJob extends Job {
  private static final String NAME = "UI-Updater"; //$NON-NLS-1$
  private final Control fControl;

  /**
   * Creates a new instance of this kind. Use it like any other instance of
   * <code>Job</code>. Asks the given Control for its disposed state as soon
   * as the UI-Job is running, in order to cancel the Job when the Control is
   * disposed.
   * 
   * @param control Used to cancel the UI-Job in case the given Control is
   * disposed at that time.
   */
  public UIBackgroundJob(Control control) {
    super(NAME);
    fControl = control;
  }

  /**
   * The task that is to run in the Background-Thread.
   * 
   * @param monitor Progress-Monitor of the Job performing this task.
   */
  protected abstract void runInBackground(IProgressMonitor monitor);

  /**
   * The task that is to run in the UI-Thread.
   * 
   * @param monitor Progress-Monitor of the Job performing this task.
   */
  protected abstract void runInUI(IProgressMonitor monitor);

  @Override
  protected IStatus run(final IProgressMonitor monitor) {

    /* If Control is provided, check disposed State */
    if (fControl != null && fControl.isDisposed())
      return Status.OK_STATUS;

    /* Only run if not canceld yet */
    if (!monitor.isCanceled())
      synchronizedSafeRunInBackground(monitor);

    /* Schdule UIJob now */
    UIJob uiJob = new UIJob(NAME) {
      @Override
      public IStatus runInUIThread(IProgressMonitor monitor) {

        /* If Control is provided, check disposed State */
        if (fControl != null && fControl.isDisposed())
          return Status.OK_STATUS;

        /* Run UI-Task */
        synchronizedSafeRunInUI(monitor);

        return Status.OK_STATUS;
      }
    };

    uiJob.setSystem(true);
    uiJob.setUser(false);

    /* Only run if not canceld yet */
    if (!monitor.isCanceled())
      uiJob.schedule();

    monitor.done();

    return Status.OK_STATUS;
  }

  private synchronized void synchronizedSafeRunInBackground(final IProgressMonitor monitor) {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        runInBackground(monitor);
      }
    });
  }

  private synchronized void synchronizedSafeRunInUI(final IProgressMonitor monitor) {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        runInUI(monitor);
      }
    });
  }
}