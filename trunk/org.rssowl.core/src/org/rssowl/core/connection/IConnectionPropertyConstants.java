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

package org.rssowl.core.connection;

/**
 * Constants for connection related properties. Can be used as keys inside a
 * <code>HashMap</code> when calling
 * <code>ConnectionManager#load(URL url, HashMap properties)</code>
 * 
 * @author bpasero
 */
public interface IConnectionPropertyConstants {

  /** Key for storing the If-Modified-Since Header as <code>String</code> */
  public static final String IF_MODIFIED_SINCE = "IF_MODIFIED_SINCE"; //$NON-NLS-1$

  /** Key for storing the If-None-Match Header as <code>String</code> */
  public static final String IF_NONE_MATCH = "IF_NONE_MATCH"; //$NON-NLS-1$

  /** Key for storing the state of using a Proxy as <code>Boolean</code> */
  public static final String USE_PROXY = "USE_PROXY"; //$NON-NLS-1$

  /** Key for storing the Connection Timeout in millis as <code>Integer</code> */
  public static final String CON_TIMEOUT = "CON_TIMEOUT"; //$NON-NLS-1$

  /**
   * Key for storing an instance of <code>IProgressMonitor</code> to support
   * early cancelation while a Stream is read from the Connection. See
   * <code>HttpConnectionInputStream</code> for a concrete usecase.
   */
  public static final String PROGRESS_MONITOR = "PROGRESS_MONITOR"; //$NON-NLS-1$
}