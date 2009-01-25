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

package org.rssowl.lib.httpclient.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * Activator of the HTTPClient bundle.
 * 
 * @author bpasero
 */
public class Activator extends Plugin {
  private static Activator fPlugin;

  /**
   * The constructor.
   */
  public Activator() {
    fPlugin = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    /* Use the LogBridge as Logger */
    System.setProperty("org.apache.commons.logging.Log", "org.rssowl.lib.httpclient.internal.LogBridge");
  }

  /**
   * Returns the shared instance.
   * 
   * @return the shared instance
   */
  public static Activator getDefault() {
    return fPlugin;
  }

  /**
   * Create a Error IStatus out of the given message and exception.
   * 
   * @param msg The message describing the error.
   * @param e The Exception that occured.
   * @return An IStatus out of the given message and exception.
   */
  public IStatus createErrorStatus(String msg, Exception e) {
    return new Status(IStatus.ERROR, getBundle().getSymbolicName(), IStatus.ERROR, msg, e);
  }

  /**
   * Log an Error Message.
   * 
   * @param msg The message to log as Error.
   * @param e The occuring Exception to log.
   */
  public void logError(String msg, Throwable e) {
    if (msg == null)
      msg = ""; //$NON-NLS-1$
    getLog().log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), IStatus.ERROR, msg, e));
  }
}