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

package org.rssowl.core.internal;

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.rssowl.core.util.LoggingSafeRunnable;

/**
 * The main plugin class to be used in the desktop.
 */
public class Activator extends Plugin {

  private static final String CORE_NET_BUNDLE = "org.eclipse.core.net";

  private static Activator fPlugin;
  private BundleContext fContext;
  private String fVersion;
  private IProxyService fProxyService;

  /**
   * The constructor.
   */
  public Activator() {
    fPlugin = this;
  }

  /**
   * This method is called upon plug-in activation
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    fContext = context;
    fVersion = (String) fPlugin.getBundle().getHeaders().get("Bundle-Version"); //$NON-NLS-1$

    /* Load the Proxy Service */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        fProxyService = loadProxyService();
      }
    });
  }

  private IProxyService loadProxyService() {
    Bundle bundle = Platform.getBundle(CORE_NET_BUNDLE);
    if (bundle != null) {
      ServiceReference ref = bundle.getBundleContext().getServiceReference(IProxyService.class.getName());
      if (ref != null)
        return (IProxyService) bundle.getBundleContext().getService(ref);
    }

    if (fProxyService == null)
      logError("Proxy service could not be found.", null);

    return null;
  }

  /**
   * @return Returns the <code>IProxyService</code> providing access to proxy
   * related settings.
   */
  public IProxyService getProxyService() {
    return fProxyService;
  }

  /**
   * This method is called when the plug-in is stopped
   */
  @Override
  public void stop(BundleContext context) throws Exception {

    /* Stop Internal Owl */
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        InternalOwl.getDefault().shutdown();
      }
    });

    /* Proceed */
    super.stop(context);
    fContext = null;
    fPlugin = null;
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
   * Get this Plugins BundleContext.
   *
   * @return this Plugins BundleContext.
   */
  public BundleContext getContext() {
    return fContext;
  }

  /**
   * Returns this Bundles Version Number.
   *
   * @return this Bundles Version Number.
   */
  public String getVersion() {
    return fVersion;
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

  /**
   * Create a IStatus out of the given message and exception.
   *
   * @param msg The message describing the information.
   * @param e The Exception that occured.
   * @return An IStatus out of the given message and exception.
   */
  public IStatus createInfoStatus(String msg, Exception e) {
    return new Status(IStatus.INFO, Activator.getDefault().getBundle().getSymbolicName(), IStatus.INFO, msg, e);
  }

  /**
   * Create a Warning IStatus out of the given message and exception.
   *
   * @param msg The message describing the error.
   * @param e The Exception that occured.
   * @return An IStatus out of the given message and exception.
   */
  public IStatus createWarningStatus(String msg, Exception e) {
    return new Status(IStatus.WARNING, getBundle().getSymbolicName(), IStatus.WARNING, msg, e);
  }

  /**
   * Create a Error IStatus out of the given message and exception.
   *
   * @param msg The message describing the error.
   * @param e The Exception that occurred.
   * @return An IStatus out of the given message and exception.
   */
  public IStatus createErrorStatus(String msg, Throwable e) {
    return new Status(IStatus.ERROR, getBundle().getSymbolicName(), IStatus.ERROR, msg, e);
  }
}