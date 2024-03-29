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

package org.rssowl.core.util;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ISafeRunnable;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;

/**
 * Abstract implementation of <code>ISafeRunnable</code> that is logging any
 * exception using the Plugin's Log Mechanism.
 * <p>
 * TODO Create ExceptionListener that allows to listen for these Runtime
 * Exceptions. That would allow the UI to show an Error Dialog with Details of
 * the Error.
 * </p>
 * <p>
 * TODO Note that the SafeRunner is already logging any exception to the log
 * before it calls the handleException() method!
 * </p>
 */
public abstract class LoggingSafeRunnable implements ISafeRunnable {
  public void handleException(Throwable e) {

    /* In Testing, rethrow any RuntimeException */
    if (InternalOwl.TESTING && e instanceof RuntimeException)
      throw (RuntimeException) e;
    else if (InternalOwl.TESTING)
      throw new RuntimeException(e.getMessage(), e);

    /* Log Exception */
    if (e instanceof CoreException)
      Activator.getDefault().getLog().log(((CoreException) e).getStatus());
    // else if (e instanceof Exception)
    // Activator.getDefault().logError(e.getMessage(), e);
    // else
    // Activator.getDefault().logError(e.getMessage(), null);
  }
}