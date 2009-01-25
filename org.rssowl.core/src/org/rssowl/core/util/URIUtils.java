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

import org.rssowl.core.internal.Activator;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Utility Class for working with Links.
 *
 * @author bpasero
 */
public class URIUtils {

  /** URL of Blank Website */
  public static final String ABOUT_BLANK = "about:blank"; //$NON-NLS-1$

  /* Default Encoding */
  private static final String DEFAULT_ENCODING = "UTF-8"; //$NON-NLS-1$

  /** Common Newsfeed Extensions with leading Dot */
  private static final String[] FEED_EXTENSIONS_DOTTED = new String[] { ".rss", ".rdf", ".xml", ".atom", ".feed" };

  /* This utility class constructor is hidden */
  private URIUtils() {
  // Protect default constructor
  }

  /**
   * Will create a new {@link URI} out of the given one that only contains the
   * Scheme and Host part.
   *
   * @param link The link to normalize.
   * @return the normalized link.
   */
  public static URI normalizeUri(URI link) {
    return normalizeUri(link, false);
  }

  /**
   * Will create a new {@link URI} out of the given one that only contains the
   * Scheme and Host part. If <code>withPort</code> is set to TRUE, the port
   * will be part of the normalized URI too.
   *
   * @param link The link to normalize.
   * @param withPort If set to <code>TRUE</code>, include the port in the
   * normalized URI.
   * @return the normalized link.
   */
  public static URI normalizeUri(URI link, boolean withPort) {
    try {
      if (withPort)
        return new URI(link.getScheme(), null, link.getHost(), link.getPort(), null, null, null);
      return new URI(link.getScheme(), link.getHost(), null, null);
    } catch (URISyntaxException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

    return link;
  }

  /**
   * Return TRUE in case the given String looks like a Link to a Feed.
   *
   * @param str The String to check
   * @return TRUE in case the String looks like a Link to a Feed.
   */
  public static boolean looksLikeFeedLink(String str) {
    if (!looksLikeLink(str))
      return false;

    for (String extension : FEED_EXTENSIONS_DOTTED) {
      if (str.contains(extension))
        return true;
    }

    return false;
  }

  /**
   * Return TRUE in case the given String looks like a Link.
   *
   * @param str The String to check
   * @return TRUE in case the String looks like a Link.
   */
  public static boolean looksLikeLink(String str) {

    /* Is empty or null? */
    if (!StringUtils.isSet(str))
      return false;

    /* Contains whitespaces ? */
    if (str.indexOf(' ') >= 0)
      return false;

    /* RegEx Link check */
    if (RegExUtils.isValidURL(str))
      return true;

    /* Try creating an URL object */
    try {
      new URL(str);
    } catch (MalformedURLException e) {
      return false;
    }

    /* String is an URL */
    return true;
  }

  /**
   * URLEncode the given String. Note that URLEncoder uses "+" to display any
   * spaces. But we need "%20", so we'll replace all "+" with "%20". This method
   * is used to create a "mailto:" URL that is handled by a mail application.
   * The String is HTML Encoded if the user has set so.
   *
   * @param str String to encode
   * @return String encoded String
   */
  public static String mailToUrllEncode(String str) {
    return urlEncode(str).replaceAll("\\+", "%20"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * This is a simple wrapper method for the encode() Method of the URLEncoder.
   * UTF-8 is used for encoding.
   *
   * @param str String to encode
   * @return the URL Encoded String
   */
  public static String urlEncode(String str) {

    /* Try Default encoding */
    try {
      return URLEncoder.encode(str, DEFAULT_ENCODING);
    }

    /* Return in this case */
    catch (UnsupportedEncodingException e1) {
      return str;
    }
  }

  /**
   * This is a simple wrapper method for the decode() Method of the URLDecoder.
   * UTF-8 is used for encoding.
   *
   * @param str String to decode
   * @return the URL Decoded String
   */
  public static String urlDecode(String str) {

    /* Try Default encoding */
    try {
      return URLDecoder.decode(str, DEFAULT_ENCODING);
    }

    /* Return in this case */
    catch (UnsupportedEncodingException e1) {
      return str;
    }
  }

  /**
   * Try to create an URI from the given String. The String is preprocessed to
   * work around some bugs in the implementation of Java's equals() for URIs:
   * <p>
   * <li>remove leading and trailing whitespaces</li>
   * <li>remove trailing slashes</li>
   * </p>
   *
   * @param str The String to interpret as URI.
   * @return The URI or NULL in case of the String does not match the URI
   * Syntax.
   */
  public static URI createURI(String str) {
    try {

      /* Remove surrounding whitespaces */
      str = str.trim();

      /* Take first token not containing any whitespace */
      if (str.contains(" ")) {
        String[] split = str.split(" ");
        if (split.length > 0)
          str = split[0];
      }

      /* Remove trailing slash if any */
      if (str.length() > 0 && str.charAt(str.length() - 1) == '/')
        str = str.substring(0, str.length() - 1);

      return new URI(str);
    } catch (URISyntaxException e) {
      return null;
    }
  }

  /**
   * Returns a new <code>URI</code> from the given one, that potentially
   * points to the favicon.ico.
   *
   * @param link The Link to look for a favicon.
   * @param rewriteHost If <code>TRUE</code>, change the host for a better
   * result.
   * @return Returns the <code>URI</code> from the given one, that potentially
   * points to the favicon.ico.
   * @throws URISyntaxException In case of a malformed URI.
   */
  public static URI toFaviconUrl(URI link, boolean rewriteHost) throws URISyntaxException {
    String host = link.getHost();

    if (!StringUtils.isSet(host))
      return null;

    /* Strip all but the last two segments from the Host */
    if (rewriteHost) {
      String[] hostSegments = host.split("\\."); //$NON-NLS-1$
      int len = hostSegments.length;

      /* Rewrite if conditions match */
      if (len > 2 && !"www".equals(hostSegments[0])) //$NON-NLS-1$
        host = hostSegments[len - 2] + "." + hostSegments[len - 1]; //$NON-NLS-1$

      /* Rewrite failed, avoid reloading by throwing an exception */
      else
        throw new URISyntaxException("", "");
    }

    StringBuilder buf = new StringBuilder();
    buf.append("http://"); //$NON-NLS-1$
    buf.append(host);
    buf.append("/favicon.ico"); //$NON-NLS-1$

    return new URI(buf.toString());
  }

  /**
   * Try to get the File Name of the given URI.
   *
   * @param uri The URI to parse the File from.
   * @return String The File Name or the URI in external Form.
   */
  public static String getFile(URI uri) {
    String file = uri.getPath();
    if (StringUtils.isSet(file)) {
      String parts[] = file.split("/");
      if (parts.length > 0 && StringUtils.isSet(parts[parts.length - 1]))
        return urlDecode(parts[parts.length - 1]);
    }
    return uri.toASCIIString();
  }
}