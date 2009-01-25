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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.AuthState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.osgi.service.url.URLStreamHandlerService;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.IConditionalGetCompatible;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.connection.IProxyCredentials;
import org.rssowl.core.connection.NotModifiedException;
import org.rssowl.core.connection.ProxyAuthenticationRequiredException;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * The <code>DefaultFeedHandler</code> is an implementation of
 * <code>IProtocolHandler</code> that works on HTTP, HTTPS and the FILE
 * Protocol. After loading the Inputstream of the given URL, the stream is
 * passed to the Interpreter-Component to interpret it as one of the supported
 * XML-Formats for Newsfeeds.
 *
 * @author bpasero
 */
public class DefaultProtocolHandler implements IProtocolHandler {

  /* Mime Types for Feeds */
  private static final String[] FEED_MIME_TYPES = new String[] { "application/rss+xml", "application/atom+xml", "application/rdf+xml" };
  private static final String HREF = "href=";

  /* Http Status Codes */
  private static final int HTTP_ERRORS = 400;
  private static final int HTTP_STATUS_NOT_MODIFIED = 304;
  private static final int HTTP_ERROR_AUTH_REQUIRED = 401;
  private static final int HTTP_ERROR_PROXY_AUTH_REQUIRED = 407;

  /* Response Header */
  private static final String HEADER_RESPOND_USER_AGENT = "User-Agent"; //$NON-NLS-1$
  private static final String HEADER_RESPOND_ACCEPT_ENCODING = "Accept-Encoding"; //$NON-NLS-1$
  private static final String HEADER_RESPOND_IF_NONE_MATCH = "If-None-Match"; //$NON-NLS-1$
  private static final String HEADER_RESPOND_IF_MODIFIED_SINCE = "If-Modified-Since"; //$NON-NLS-1$

  /* The Default Connection Timeout */
  private static final int DEFAULT_CON_TIMEOUT = 30000;

  /* Timeout for loading a Favicon */
  private static final int FAVICON_CON_TIMEOUT = 5000;

  private static final String USER_AGENT = getOwlAgent();
  private static boolean fgSSLInitialized;
  private static boolean fgFeedProtocolInitialized;

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#reload(java.net.URI,
   * org.eclipse.core.runtime.IProgressMonitor, java.util.Map)
   */
  public Pair<IFeed, IConditionalGet> reload(URI link, IProgressMonitor monitor, Map<Object, Object> properties) throws CoreException {
    IModelFactory typesFactory = Owl.getModelFactory();

    /* Create a new empty feed from the existing one */
    IFeed feed = typesFactory.createFeed(null, link);

    /* Add Monitor to support early cancelation */
    if (properties != null)
      properties.put(IConnectionPropertyConstants.PROGRESS_MONITOR, monitor);

    /* Retrieve the InputStream out of the Feed's Link */
    InputStream inS = openStream(link, properties);

    /* Retrieve Conditional Get if present */
    IConditionalGet conditionalGet = getConditionalGet(link, inS);

    /* Return on Cancelation or Shutdown */
    if (monitor.isCanceled()) {
      try {
        if (inS != null)
          inS.close();
      } catch (IOException e) {
        /* Ignore */
      }
      return null;
    }

    /* Pass the Stream to the Interpreter */
    Owl.getInterpreter().interpret(inS, feed);

    return Pair.create(feed, conditionalGet);
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#getFeedIcon(java.net.URI)
   */
  public byte[] getFeedIcon(URI link) {
    return loadFavicon(link, false);
  }

  private IConditionalGet getConditionalGet(URI link, InputStream inS) {
    IModelFactory typesFactory = Owl.getModelFactory();

    if (inS instanceof IConditionalGetCompatible) {
      String ifModifiedSince = ((IConditionalGetCompatible) inS).getIfModifiedSince();
      String ifNoneMatch = ((IConditionalGetCompatible) inS).getIfNoneMatch();

      if (ifModifiedSince != null || ifNoneMatch != null)
        return typesFactory.createConditionalGet(ifModifiedSince, link, ifNoneMatch);
    }

    return null;
  }

  /* Load a possible Favicon from the given Feed */
  byte[] loadFavicon(URI link, boolean rewriteHost) {
    try {

      /* Define Properties for Connection */
      Map<Object, Object> properties = new HashMap<Object, Object>();
      properties.put(IConnectionPropertyConstants.CON_TIMEOUT, FAVICON_CON_TIMEOUT);

      /* Load Favicon */
      URI faviconLink = URIUtils.toFaviconUrl(link, rewriteHost);
      if (faviconLink == null)
        return null;

      InputStream fis = openStream(faviconLink, properties);

      ByteArrayOutputStream fos = new ByteArrayOutputStream();
      byte buffer[] = new byte[0xffff];
      int nbytes;

      while ((nbytes = fis.read(buffer)) != -1)
        fos.write(buffer, 0, nbytes);

      return fos.toByteArray();
    } catch (URISyntaxException e) {
      /* Ignore */
    } catch (ConnectionException e) {

      /* Try rewriting the Host to obtain the Favicon */
      if (!rewriteHost) {
        String exceptionName = e.getClass().getName();

        /* Only retry in case this is a generic ConnectionException */
        if (ConnectionException.class.getName().equals(exceptionName))
          return loadFavicon(link, true);
      }
    } catch (IOException e) {
      /* Ignore */
    }

    return null;
  }

  /*
   * Do not override default URLStreamHandler of HTTP/HTTPS and therefor return
   * NULL.
   * @see org.rssowl.core.connection.IProtocolHandler#getURLStreamHandler()
   */
  public URLStreamHandlerService getURLStreamHandler() {
    return null;
  }

  /**
   * Load the Contents of the given URL by connecting to it. The additional
   * properties may be used in conjunction with the
   * <code>IConnectionPropertyConstants</code> to define connection related
   * properties..
   *
   * @param link The URL to load.
   * @param properties Connection related properties as defined in
   * <code>IConnectionPropertyConstants</code> for example, or <code>NULL</code>
   * if none.
   * @return The Content of the URL as InputStream.
   * @throws ConnectionException Checked Exception to be used in case of any
   * Exception.
   * @see AuthenticationRequiredException
   * @see NotModifiedException
   */
  public InputStream openStream(URI link, Map<Object, Object> properties) throws ConnectionException {

    /* Retrieve the InputStream out of the Link */
    try {
      return internalOpenStream(link, link, null, properties);
    }

    /* Handle Authentication Required */
    catch (AuthenticationRequiredException e) {

      /* Realm required from here on */
      if (e.getRealm() == null)
        throw e;

      /* Try to load credentials using Host / Port / Realm */
      URI normalizedUri = URIUtils.normalizeUri(link, true);
      ICredentials authCredentials = Owl.getConnectionService().getAuthCredentials(normalizedUri, e.getRealm());

      /* Credentials based on Host / Port / Realm provided */
      if (authCredentials != null) {

        /* Store for plain URI too */
        ICredentialsProvider credProvider = Owl.getConnectionService().getCredentialsProvider(link);
        credProvider.setAuthCredentials(authCredentials, link, null);

        /* Reopen Stream */
        try {
          return internalOpenStream(link, normalizedUri, e.getRealm(), properties);
        } catch (AuthenticationRequiredException ex) {
          Owl.getConnectionService().getCredentialsProvider(normalizedUri).deleteAuthCredentials(normalizedUri, e.getRealm());
          throw ex;
        }
      }

      /* Otherwise throw exception to callee */
      throw e;
    }
  }

  private InputStream internalOpenStream(URI link, URI authLink, String authRealm, Map<Object, Object> properties) throws ConnectionException {

    /* Handle File Protocol at first */
    if ("file".equals(link.getScheme()))
      return loadFileProtocol(link);

    /* SSL Support */
    if ("https".equals(link.getScheme())) //$NON-NLS-1$
      initSSLProtocol();

    /* Feed Support */
    if ("feed".equals(link.getScheme()))
      initFeedProtocol();

    /* Init Client */
    HttpClient client = initClient(properties);

    /* Init the connection */
    GetMethod getMethod = null;
    InputStream inS = null;
    try {
      getMethod = initConnection(link, properties);

      /* Proxy if required */
      setupProxy(link, client);

      /* Authentication if required */
      setupAuthentication(authLink, authRealm, client, getMethod);

      /* Open the connection */
      inS = openConnection(client, getMethod);

      /* Try to pipe the resulting stream into a GZipInputStream */
      if (inS != null)
        inS = pipeStream(inS, getMethod);
    } catch (IOException e) {
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }

    /* In case authentication required / failed */
    if (getMethod.getStatusCode() == HTTP_ERROR_AUTH_REQUIRED) {
      AuthState hostAuthState = getMethod.getHostAuthState();
      throw new AuthenticationRequiredException(hostAuthState != null ? hostAuthState.getRealm() : null, Activator.getDefault().createErrorStatus("Authentication required!", null)); //$NON-NLS-1$
    }

    /* In case proxy-authentication required / failed */
    if (getMethod.getStatusCode() == HTTP_ERROR_PROXY_AUTH_REQUIRED)
      throw new ProxyAuthenticationRequiredException(Activator.getDefault().createErrorStatus("Proxy-Authentication required!", null)); //$NON-NLS-1$

    /* If status code is 4xx, throw an IOException with the status code included */
    if (getMethod.getStatusCode() >= HTTP_ERRORS)
      throw new ConnectionException(Activator.getDefault().createErrorStatus("Server returned HTTP Status " + String.valueOf(getMethod.getStatusCode()), null)); //$NON-NLS-1$

    /* In case the Feed has not been modified since */
    if (getMethod.getStatusCode() == HTTP_STATUS_NOT_MODIFIED)
      throw new NotModifiedException(Activator.getDefault().createInfoStatus("Feed has not been modified since!", null)); //$NON-NLS-1$

    /* In case response body is not available */
    if (inS == null)
      throw new ConnectionException(Activator.getDefault().createErrorStatus("Response Stream is not available!", null)); //$NON-NLS-1$

    /* Check wether a Progress Monitor is provided to support early cancelation */
    IProgressMonitor monitor = null;
    if (properties != null && properties.containsKey(IConnectionPropertyConstants.PROGRESS_MONITOR))
      monitor = (IProgressMonitor) properties.get(IConnectionPropertyConstants.PROGRESS_MONITOR);

    /* Return a Stream that releases the connection once closed */
    return new HttpConnectionInputStream(getMethod, monitor, inS);
  }

  private InputStream loadFileProtocol(URI link) throws ConnectionException {
    try {
      File file = new File(link);
      return new BufferedInputStream(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      throw new ConnectionException(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }

  private void setupAuthentication(URI link, String realm, HttpClient client, GetMethod getMethod) throws URIException, CredentialsException {
    ICredentials authCredentials = Owl.getConnectionService().getAuthCredentials(link, realm);
    if (authCredentials != null) {
      client.getParams().setAuthenticationPreemptive(true);

      /* Require Host */
      String host = getMethod.getURI().getHost();

      /* Create the UsernamePasswordCredentials */
      NTCredentials userPwCreds = new NTCredentials(authCredentials.getUsername(), authCredentials.getPassword(), host, (authCredentials.getDomain() != null) ? authCredentials.getDomain() : ""); //$NON-NLS-1$

      /* Authenticate to the Server */
      client.getState().setCredentials(AuthScope.ANY, userPwCreds);
      getMethod.setDoAuthentication(true);
    }
  }

  private void setupProxy(URI link, HttpClient client) throws CredentialsException {
    IProxyCredentials creds = Owl.getConnectionService().getProxyCredentials(link);
    if (creds != null) {

      /* Apply Proxy Config to HTTPClient */
      client.getParams().setAuthenticationPreemptive(true);
      client.getHostConfiguration().setProxy(creds.getHost(), creds.getPort());

      /* Authenticate if required */
      if (creds.getUsername() != null || creds.getPassword() != null) {
        String user = StringUtils.isSet(creds.getUsername()) ? creds.getUsername() : "";
        String pw = StringUtils.isSet(creds.getPassword()) ? creds.getPassword() : "";

        AuthScope proxyAuthScope = new AuthScope(creds.getHost(), creds.getPort());

        /* Use NTLM Credentials if Domain is set */
        if (creds.getDomain() != null)
          client.getState().setProxyCredentials(proxyAuthScope, new NTCredentials(user, pw, creds.getHost(), creds.getDomain()));

        /* Use normal Credentials if Domain is not set */
        else
          client.getState().setProxyCredentials(proxyAuthScope, new UsernamePasswordCredentials(user, pw));
      }
    }
  }

  private synchronized void initSSLProtocol() {
    if (fgSSLInitialized)
      return;

    /* Register Easy Protocol Socket Factory with HTTPS */
    Protocol easyHttpsProtocol = new Protocol("https", (ProtocolSocketFactory) Owl.getConnectionService().getSecureProtocolSocketFactory(), 443); //$NON-NLS-1$
    Protocol.registerProtocol("https", easyHttpsProtocol); //$NON-NLS-1$

    fgSSLInitialized = true;
  }

  private synchronized void initFeedProtocol() {
    if (fgFeedProtocolInitialized)
      return;

    Protocol feed = new Protocol("feed", new DefaultProtocolSocketFactory(), 80);
    Protocol.registerProtocol("feed", feed);

    fgFeedProtocolInitialized = true;
  }

  private void setHeaders(Map<Object, Object> properties, GetMethod getMethod) {
    getMethod.setRequestHeader(HEADER_RESPOND_ACCEPT_ENCODING, "gzip, *"); //$NON-NLS-1$
    getMethod.setRequestHeader(HEADER_RESPOND_USER_AGENT, USER_AGENT);

    /* Add Conditional GET Headers if present */
    if (properties != null) {
      String ifModifiedSince = (String) properties.get(IConnectionPropertyConstants.IF_MODIFIED_SINCE);
      String ifNoneMatch = (String) properties.get(IConnectionPropertyConstants.IF_NONE_MATCH);

      if (ifModifiedSince != null)
        getMethod.setRequestHeader(HEADER_RESPOND_IF_MODIFIED_SINCE, ifModifiedSince);

      if (ifNoneMatch != null)
        getMethod.setRequestHeader(HEADER_RESPOND_IF_NONE_MATCH, ifNoneMatch);
    }
  }

  private HttpClient initClient(Map<Object, Object> properties) {

    /* Retrieve Connection Timeout from Properties if set */
    int conTimeout = DEFAULT_CON_TIMEOUT;
    if (properties != null && properties.containsKey(IConnectionPropertyConstants.CON_TIMEOUT))
      conTimeout = (Integer) properties.get(IConnectionPropertyConstants.CON_TIMEOUT);

    /* Create a new HttpClient */
    HttpClient client = new HttpClient();

    /* Socket Timeout - Max. time to wait for an answer */
    client.getHttpConnectionManager().getParams().setSoTimeout(conTimeout);

    /* Connection Timeout - Max. time to wait for a connection */
    client.getHttpConnectionManager().getParams().setConnectionTimeout(conTimeout);

    return client;
  }

  private GetMethod initConnection(URI link, Map<Object, Object> properties) throws IOException {

    /* Create the Get Method. Wrap any RuntimeException into an IOException */
    GetMethod getMethod = null;
    try {
      getMethod = new GetMethod();
      getMethod.setURI(new org.apache.commons.httpclient.URI(link.toString(), false, getMethod.getParams().getUriCharset()));
    } catch (RuntimeException e) {
      throw new IOException(e.getMessage());
    }

    /* Ignore Cookies */
    getMethod.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);

    /* Set Headers */
    setHeaders(properties, getMethod);

    /* Follow Redirects */
    getMethod.setFollowRedirects(true);

    return getMethod;
  }

  private InputStream openConnection(HttpClient client, GetMethod getMethod) throws HttpException, IOException {

    /* Execute the GET Method */
    client.executeMethod(getMethod);

    /* Finally retrieve the InputStream from the respond body */
    return getMethod.getResponseBodyAsStream();
  }

  private InputStream pipeStream(InputStream inputStream, GetMethod getMethod) throws IOException {
    Assert.isNotNull(inputStream);

    /* Retrieve the Content Encoding */
    String contentEncoding = getMethod.getResponseHeader("Content-Encoding") != null ? getMethod.getResponseHeader("Content-Encoding").getValue() : null; //$NON-NLS-1$ //$NON-NLS-2$
    boolean isGzipStream = false;

    /*
     * Return in case the Content Encoding is not given and the InputStream does
     * not support mark() and reset()
     */
    if ((contentEncoding == null || !contentEncoding.equals("gzip")) && !inputStream.markSupported()) //$NON-NLS-1$
      return inputStream;

    /* Content Encoding is set to gzip, so use the GZipInputStream */
    if (contentEncoding != null && contentEncoding.equals("gzip")) { //$NON-NLS-1$
      isGzipStream = true;
    }

    /* Detect if the Stream is gzip encoded */
    else if (inputStream.markSupported()) {
      inputStream.mark(2);
      int id1 = inputStream.read();
      int id2 = inputStream.read();
      inputStream.reset();

      /* Check for GZip Magic Numbers (See RFC 1952) */
      if (id1 == 0x1F && id2 == 0x8B)
        isGzipStream = true;
    }

    /* Create the GZipInputStream then */
    if (isGzipStream)
      return new GZIPInputStream(inputStream);
    return inputStream;
  }

  private static String getOwlAgent() {
    String version = Activator.getDefault().getVersion();
    if ("win32".equals(Platform.getOS())) //$NON-NLS-1$
      return "RSSOwl/" + version + " (Windows; U; " + "en)"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
    else if ("gtk".equals(Platform.getOS())) //$NON-NLS-1$
      return "RSSOwl/" + version + " (X11; U; " + "en)"; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
    else if ("carbon".equals(Platform.getOS())) //$NON-NLS-1$
      return "RSSOwl/" + version + " (Macintosh; U; " + "en)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    return "RSSOwl/" + version; //$NON-NLS-1$
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#getLabel(java.net.URI)
   */
  public String getLabel(URI link) throws ConnectionException {
    String title = "";

    InputStream inS = openStream(link, null);
    try {

      /* Buffered Stream to support mark and reset */
      BufferedInputStream bufIns = new BufferedInputStream(inS);
      bufIns.mark(8192);

      /* Try to read Encoding out of XML Document */
      String encoding = getEncodingFromXML(new InputStreamReader(bufIns));

      /* Avoid lowercase UTF-8 notation */
      if ("utf-8".equalsIgnoreCase(encoding))
        encoding = "UTF-8";

      /* Reset the Stream to its beginning */
      bufIns.reset();

      /* Grab Title using supplied Encoding */
      if (StringUtils.isSet(encoding))
        title = getTitleFromFeed(new BufferedReader(new InputStreamReader(bufIns, encoding)));

      /* Grab Title using Default Encoding */
      else
        title = getTitleFromFeed(new BufferedReader(new InputStreamReader(bufIns)));

      /* Remove the title tags (also delete attributes in title tag) */
      title = title.replaceAll("<title[^>]*>", "");
      title = title.replaceAll("</title>", "");

      /* Remove potential CDATA Tags */
      title = title.replaceAll(Pattern.quote("<![CDATA["), "");
      title = title.replaceAll(Pattern.quote("]]>"), "");
    } catch (IOException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

    /* Finally close the Stream */
    finally {

      /* Close Stream */
      try {
        inS.close();
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return title.trim();
  }

  /* Tries to read the encoding information from the given InputReader */
  private String getEncodingFromXML(InputStreamReader inputReader) throws IOException {
    String encoding = null;

    /* Read the first line or until the Tag is closed */
    StringBuilder strBuf = new StringBuilder();
    int c;
    while ((c = inputReader.read()) != -1) {
      char character = (char) c;

      /* Append all Characters, except for closing Tag or CR */
      if (character != '>' && character != '\n' && character != '\r')
        strBuf.append(character);

      /* Closing Tag is the last one to append */
      else if (character == '>') {
        strBuf.append(character);
        break;
      }

      /* End of Line or Tag reached */
      else
        break;
    }

    /* Save the first Line */
    String firstLine = strBuf.toString();

    /* Look if Encoding is supplied */
    if (firstLine.indexOf("encoding") >= 0) {

      /* Extract the Encoding Value */
      String regEx = "<\\?.*encoding=[\"']([^\\s]*)[\"'].*\\?>";
      Pattern pattern = Pattern.compile(regEx);
      Matcher match = pattern.matcher(firstLine);

      /* Get first matching String */
      if (match.find())
        return match.group(1);
    }
    return encoding;
  }

  /* Tries to find the title information from the given Reader */
  private String getTitleFromFeed(BufferedReader inputReader) throws IOException {
    String title = "";
    String firstLine;
    boolean titleFound = false;

    /* Read the file until the Title is found or EOF is reached */
    while (true) {

      /* Will throw an IOException on EOF reached */
      firstLine = inputReader.readLine();

      /* EOF reached */
      if (firstLine == null)
        break;

      /* If the line contains the title, break loop */
      if (firstLine.indexOf("<title") >= 0 && firstLine.indexOf("</title>") >= 0) {
        title = firstLine.trim();
        titleFound = true;
        break;
      }
    }

    /* Return if no title was found */
    if (!titleFound)
      return title;

    /* Extract the title String */
    String regEx = "<title[^>]*>[^<]*</title>";
    Pattern pattern = Pattern.compile(regEx);
    Matcher match = pattern.matcher(title);

    /* Get first matching String */
    if (match.find())
      title = match.group();

    // TODO Decode possible XML special chars (entities)

    return title;
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#getFeed(java.net.URI)
   */
  public URI getFeed(final URI website) throws ConnectionException {
    URI feed = null;

    BufferedInputStream bufIns = new BufferedInputStream(openStream(website, null));
    BufferedReader reader = new BufferedReader(new InputStreamReader(bufIns));

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        for (String feedMimeType : FEED_MIME_TYPES) {
          int index = line.indexOf(feedMimeType);

          /* Mime Type Found */
          if (index > -1) {

            /* Set index to where the Link Element starts */
            for (int i = index; i >= 0; i--) {
              if (line.charAt(i) == '<') {
                index = i;
                break;
              }
            }

            /* Find the HREF */
            index = line.indexOf(HREF, index);
            if (index > -1) {
              StringBuilder str = new StringBuilder();
              for (int i = index + HREF.length(); i < line.length(); i++) {
                char c = line.charAt(i);

                if (c == '\"' || c == '\'')
                  continue;

                if (Character.isWhitespace(c) || c == '>')
                  break;

                str.append(c);
              }

              /* Handle relative Links */
              String linkVal = str.toString();
              if (!linkVal.contains("://"))
                linkVal = linkVal.startsWith("/") ? website.toString() + linkVal : website.toString() + "/" + linkVal;

              return new URI(linkVal);
            }
          }
        }
      }
    } catch (IOException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    } catch (URISyntaxException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    }

    /* Finally close the Reader */
    finally {
      try {
        reader.close();
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return feed;
  }
}