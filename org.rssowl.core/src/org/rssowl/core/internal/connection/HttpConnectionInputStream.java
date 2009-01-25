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

package org.rssowl.core.internal.connection;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.connection.IConditionalGetCompatible;
import org.rssowl.core.connection.MonitorCanceledException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * This kind of FilterInputStream makes sure that the GetMethod responsible for
 * the given InputStream is releasing its connection after the stream has been
 * closed. This class is also keeping two important headers to be used for the
 * Conditional GET Mechanism of HTTP.
 * </p>
 * <p>
 * Passing an instance of <code>IProgressMonitor</code> into the class allows
 * for early cancelation by throwing an Exception from the various
 * stream-methods as soon as the monitor is canceled.
 * </p>
 * 
 * @author bpasero
 */
public class HttpConnectionInputStream extends FilterInputStream implements IConditionalGetCompatible {

  /* Request Header */
  private static final String HEADER_RESPONSE_ETAG = "ETag"; //$NON-NLS-1$
  private static final String HEADER_RESPONSE_LAST_MODIFIED = "Last-Modified"; //$NON-NLS-1$

  private final GetMethod fGetMethod;
  private final IProgressMonitor fMonitor;
  private String fIfModifiedSince;
  private String fIfNoneMatch;

  /**
   * Creates a <code>HttpConnectionInputStream</code> by assigning the
   * argument <code>inS</code> to the field <code>this.in</code> so as to
   * remember it for later use.
   * 
   * @param getMethod The Method holding the connection of the given Stream.
   * @param monitor A ProgressMonitor to support early cancelation, or
   * <code>NULL</code> if no monitor is being used.
   * @param inS the underlying input Stream.
   */
  public HttpConnectionInputStream(GetMethod getMethod, IProgressMonitor monitor, InputStream inS) {
    super(inS);
    fGetMethod = getMethod;
    fMonitor = monitor;

    /* Keep some important Headers */
    Header headerLastModified = getMethod.getResponseHeader(HEADER_RESPONSE_LAST_MODIFIED);
    if (headerLastModified != null)
      setIfModifiedSince(headerLastModified.getValue());

    Header headerETag = getMethod.getResponseHeader(HEADER_RESPONSE_ETAG);
    if (headerETag != null)
      setIfNoneMatch(headerETag.getValue());
  }

  /*
   * @see org.rssowl.core.connection.internal.http.IConditionalGetCompatible#getIfModifiedSince()
   */
  public String getIfModifiedSince() {
    return fIfModifiedSince;
  }

  /*
   * @see org.rssowl.core.connection.internal.http.IConditionalGetCompatible#getIfNoneMatch()
   */
  public String getIfNoneMatch() {
    return fIfNoneMatch;
  }

  /*
   * @see org.rssowl.core.connection.internal.http.IConditionalGetCompatible#setIfModifiedSince(java.lang.String)
   */
  public void setIfModifiedSince(String ifModifiedSince) {
    fIfModifiedSince = ifModifiedSince;
  }

  /*
   * @see org.rssowl.core.connection.internal.http.IConditionalGetCompatible#setIfNoneMatch(java.lang.String)
   */
  public void setIfNoneMatch(String ifNoneMatch) {
    fIfNoneMatch = ifNoneMatch;
  }

  @Override
  public void close() throws IOException {
    super.close();
    fGetMethod.releaseConnection();
  }

  @Override
  public int read() throws IOException {

    /* Support early cancelation */
    if (fMonitor != null && fMonitor.isCanceled())
      throw new MonitorCanceledException("Connection canceled"); //$NON-NLS-1$

    return super.read();
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {

    /* Support early cancelation */
    if (fMonitor != null && fMonitor.isCanceled())
      throw new MonitorCanceledException("Connection canceled"); //$NON-NLS-1$

    return super.read(b, off, len);
  }

  @Override
  public int available() throws IOException {

    /* Support early cancelation */
    if (fMonitor != null && fMonitor.isCanceled())
      throw new MonitorCanceledException("Connection canceled"); //$NON-NLS-1$

    return super.available();
  }
}