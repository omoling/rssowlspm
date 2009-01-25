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

package org.rssowl.core.tests.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.IConditionalGetCompatible;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.IConnectionService;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.ICredentialsProvider;
import org.rssowl.core.connection.IProxyCredentials;
import org.rssowl.core.connection.NotModifiedException;
import org.rssowl.core.internal.connection.DefaultProtocolHandler;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.Pair;
import org.rssowl.core.util.URIUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This TestCase covers use-cases for the Connection Plugin.
 *
 * @author bpasero
 */
public class ConnectionTests {

  /**
   * Test contribution of Credentials Provider.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testProxyCredentialProvider() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rdf.xml");
    IFeed feed = new Feed(feedUrl);

    IProxyCredentials proxyCredentials = conManager.getProxyCredentials(feed.getLink());

    assertEquals("", proxyCredentials.getDomain());
    assertEquals("bpasero", proxyCredentials.getUsername());
    assertEquals("admin", proxyCredentials.getPassword());
    assertEquals("127.0.0.1", proxyCredentials.getHost());
    assertEquals(0, proxyCredentials.getPort());
  }

  /**
   * Test a protected Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testProtectedFeed() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl1 = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rss.xml");
    URI feedUrl2 = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rss_copy.xml");
    ICredentialsProvider credProvider = conManager.getCredentialsProvider(feedUrl1);

    IFeed feed1 = new Feed(feedUrl1);
    IFeed feed2 = new Feed(feedUrl2);
    AuthenticationRequiredException e = null;

    try {
      new DefaultProtocolHandler().openStream(feed1.getLink(), null);
    } catch (AuthenticationRequiredException e1) {
      e = e1;
    }

    assertNotNull(e);
    e = null;

    ICredentials credentials = new ICredentials() {
      public String getDomain() {
        return null;
      }

      public String getPassword() {
        return "admin";
      }

      public String getUsername() {
        return "bpasero";
      }
    };

    credProvider.setAuthCredentials(credentials, feedUrl1, null);

    InputStream inS = new DefaultProtocolHandler().openStream(feed1.getLink(), null);
    assertNotNull(inS);

    Owl.getInterpreter().interpret(inS, feed1);
    assertEquals("RSS 2.0", feed1.getFormat());

    /* Test authentication by other realm is not working */
    credProvider.setAuthCredentials(credentials, URIUtils.normalizeUri(feedUrl2, true), "Other Directory");

    try {
      new DefaultProtocolHandler().openStream(feed2.getLink(), null);
    } catch (AuthenticationRequiredException e1) {
      e = e1;
    }

    assertNotNull(e);

    /* Test authentication by realm is working */
    credProvider.setAuthCredentials(credentials, URIUtils.normalizeUri(feedUrl2, true), "Restricted Directory");

    inS = new DefaultProtocolHandler().openStream(feed2.getLink(), null);
    assertNotNull(inS);

    Owl.getInterpreter().interpret(inS, feed2);
    assertEquals("RSS 2.0", feed2.getFormat());
  }

  /**
   * Test a normal Feed via HTTP Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testHTTPFeed() throws Exception {
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/rss_2_0.xml");
    IFeed feed = new Feed(feedUrl);

    InputStream inS = new DefaultProtocolHandler().openStream(feed.getLink(), null);
    assertNotNull(inS);

    Owl.getInterpreter().interpret(inS, feed);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via HTTP Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testHTTPSFeed() throws Exception {
    URI feedUrl = new URI("https://sourceforge.net/export/rss2_projnews.php?group_id=141424&rss_fulltext=1");
    IFeed feed = new Feed(feedUrl);

    InputStream inS = new DefaultProtocolHandler().openStream(feed.getLink(), null);
    assertNotNull(inS);

    Owl.getInterpreter().interpret(inS, feed);
    assertEquals("RSS 2.0", feed.getFormat());
  }

  /**
   * Test a normal Feed via FILE Protocol.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testFILEFeed() throws Exception {
    URL pluginLocation = FileLocator.toFileURL(Platform.getBundle("org.rssowl.core.tests").getEntry("/"));
    IConnectionService conManager = Owl.getConnectionService();
    URL feedUrl = new URI(pluginLocation.toExternalForm().replace(" ", "%20")).resolve("data/interpreter/feed_rss.xml").toURL();
    IFeed feed = new Feed(feedUrl.toURI());

    Pair<IFeed, IConditionalGet> result = conManager.reload(feed.getLink(), null, null);

    assertEquals("RSS 2.0", result.getFirst().getFormat());
  }

  /**
   * Test Conditional GET with a compatible Feed.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testConditionalGet() throws Exception {
    URI feedUrl = new URI("http://rss.slashdot.org/Slashdot/slashdot/to");
    IFeed feed = new Feed(feedUrl);
    NotModifiedException e = null;

    InputStream inS = new DefaultProtocolHandler().openStream(feed.getLink(), null);
    assertNotNull(inS);

    String ifModifiedSince = null;
    String ifNoneMatch = null;
    if (inS instanceof IConditionalGetCompatible) {
      ifModifiedSince = ((IConditionalGetCompatible) inS).getIfModifiedSince();
      ifNoneMatch = ((IConditionalGetCompatible) inS).getIfNoneMatch();
    }
    IConditionalGet conditionalGet = Owl.getModelFactory().createConditionalGet(ifModifiedSince, feedUrl, ifNoneMatch);

    Map<Object, Object> conProperties = new HashMap<Object, Object>();
    ifModifiedSince = conditionalGet.getIfModifiedSince();
    if (ifModifiedSince != null)
      conProperties.put(IConnectionPropertyConstants.IF_MODIFIED_SINCE, ifModifiedSince);

    ifNoneMatch = conditionalGet.getIfNoneMatch();
    if (ifNoneMatch != null)
      conProperties.put(IConnectionPropertyConstants.IF_NONE_MATCH, ifNoneMatch);

    try {
      new DefaultProtocolHandler().openStream(feed.getLink(), conProperties);
    } catch (NotModifiedException e1) {
      e = e1;
    }

    assertNotNull(e);
  }

  /**
   * Test contribution of Credentials Provider.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testAuthCredentialProviderContribution() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rdf.xml");
    IFeed feed = new Feed(feedUrl);
    AuthenticationRequiredException e = null;

    try {
      conManager.getCredentialsProvider(feedUrl).deleteProxyCredentials(feedUrl); //Disable Proxy
      new DefaultProtocolHandler().openStream(feed.getLink(), null);
    } catch (AuthenticationRequiredException e1) {
      e = e1;
    }

    assertNull(e);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testCredentialsDeleted() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI("http://www.rssowl.org/rssowl2dg/tests/connection/authrequired/feed_rdf.xml");
    IFeed feed = new Feed(feedUrl);

    DynamicDAO.save(feed);

    ICredentials authCreds = new ICredentials() {
      public String getDomain() {
        return null;
      }

      public String getPassword() {
        return null;
      }

      public String getUsername() {
        return null;
      }
    };

    conManager.getCredentialsProvider(feedUrl).setAuthCredentials(authCreds, feedUrl, null);

    assertNotNull(conManager.getAuthCredentials(feedUrl, null));

    DynamicDAO.delete(new FeedLinkReference(feedUrl).resolve());

    assertNull(conManager.getAuthCredentials(feedUrl, null));
  }

  /**
   * @throws Exception
   */
  @Test
  @SuppressWarnings("nls")
  public void testLoadFeedFromWebsite() throws Exception {
    IConnectionService conManager = Owl.getConnectionService();
    URI feedUrl = new URI("http://www.planeteclipse.org");

    assertEquals("http://www.planeteclipse.org/rss20.xml", conManager.getFeed(feedUrl).toString());
  }
}