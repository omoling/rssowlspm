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
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.ModelReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.editors.feed.NewsBrowserLabelProvider;
import org.rssowl.ui.internal.editors.feed.NewsBrowserViewer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The <code>NewsServer</code> is a Singleton that serves HTML for a request
 * of News. A Browser can navigate to a local URL with Port 8795 and use some
 * special parameters to request either a List of News or complete Feeds.
 * <p>
 * TODO As more and more stuff is handled by this server, it should be
 * considered to make it extensible by allowing to register handlers for certain
 * operations.
 * </p>
 *
 * @author bpasero
 */
public class ApplicationServer {

  /* The Singleton Instance */
  private static ApplicationServer fgInstance = new ApplicationServer();

  /* Local URL Parts */
  static final String PROTOCOL = "http"; //$NON-NLS-1$
  static final String LOCALHOST = "127.0.0.1"; //$NON-NLS-1$
  static final int DEFAULT_SOCKET_PORT = 8795;

  /* Handshake Message */
  static final String STARTUP_HANDSHAKE = "org.rssowl.ui.internal.StartupHandshake";

  /* DWord controlling the startup-handshake */
  private static final String MULTI_INSTANCE_PROPERTY = "multiInstance";

  /* Identifies the Viewer providing the Content */
  private static final String ID = "id=";

  /* Used after all HTTP-Headers */
  private static final String CRLF = "\r\n"; //$NON-NLS-1$

  /* Registry of known Viewer */
  private static Map<String, ContentViewer> fRegistry = new ConcurrentHashMap<String, ContentViewer>();

  /* Supported Operations */
  private static final String OP_DISPLAY_BOOKMARK = "displayBookMark="; //$NON-NLS-1$
  private static final String OP_DISPLAY_NEWSBIN = "displayNewsBin="; //$NON-NLS-1$
  private static final String OP_DISPLAY_SEARCHMARK = "displaySearchMark="; //$NON-NLS-1$
  private static final String OP_DISPLAY_NEWS = "displayNews="; //$NON-NLS-1$

  /* Windows only: Mark of the Web */
  private static final String IE_MOTW = "<!-- saved from url=(0014)about:internet -->"; //$NON-NLS-1$

  /* RFC 1123 Date Format for the respond header */
  private static final DateFormat RFC_1123_DATE = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH); //$NON-NLS-1$

  /* Interface used to handle a startup-handshake */
  static interface HandshakeHandler {

    /**
     * Handler for the hand-shake done on startup in case the application server
     * was found already running by another RSSOwl process.
     *
     * @param token A message to pass via hand-shake.
     */
    void handle(String token);
  }

  private ServerSocket fSocket;
  private Job fServerJob;
  private int fPort;
  private HandshakeHandler fHandshakeHandler;

  /**
   * Returns the singleton instance of the ApplicationServer.
   *
   * @return the singleton instance of the ApplicationServer.
   */
  public static ApplicationServer getDefault() {
    return fgInstance;
  }

  /**
   * Attempts to start the server. Will throw an IOException in case of a
   * problem.
   *
   * @throws IOException in case of a problem starting the server.
   * @throws UnknownHostException in case the host is unknown.
   * @throws BindException in case of a failure binding the server to a port.
   */
  public void startup() throws IOException {

    /* Server already running */
    if (isRunning())
      return;

    /* Server not yet running */
    boolean usePortRange = System.getProperty(MULTI_INSTANCE_PROPERTY) != null;
    fSocket = createServerSocket(usePortRange);
    if (fSocket != null)
      listen();
  }

  /** Stop the Application Server */
  public void shutdown() {
    fServerJob.cancel();
    try {
      fSocket.close();
    } catch (IOException e) {
      if (Activator.getDefault() != null)
        Activator.getDefault().logError(e.getMessage(), e);
    }
  }

  /**
   * Check if the server is running or not.
   *
   * @return <code>TRUE</code> in case the server is running and
   * <code>FALSE</code> otherwise.
   */
  public boolean isRunning() {
    return fSocket != null;
  }

  /* Registers the Handler for Hand-Shaking on startup */
  void setHandshakeHandler(HandshakeHandler handler) {
    fHandshakeHandler = handler;
  }

  /* Attempt to create Server-Socket with retry-option */
  private ServerSocket createServerSocket(boolean usePortRange) throws IOException {

    /* Ports to try */
    List<Integer> ports = new ArrayList<Integer>();
    ports.add(DEFAULT_SOCKET_PORT);

    /* Try up to 10 different ports if set */
    if (usePortRange) {
      for (int i = 1; i < 10; i++)
        ports.add(DEFAULT_SOCKET_PORT + i);
    }

    /* Attempt to open Port */
    for (int i = 0; i < ports.size(); i++) {
      try {
        int port = ports.get(i);
        fPort = port;
        return new ServerSocket(fPort, 50, InetAddress.getByName(LOCALHOST));
      } catch (UnknownHostException e) {
        throw e;
      } catch (BindException e) {
        if (i == (ports.size() - 1))
          throw e;
      }
    }

    Activator.getDefault().logInfo("Unable to open a Port for the Application-Server");
    return null;
  }

  /**
   * Returns <code>TRUE</code> if the given URL is a local NewsServer URL.
   *
   * @param url The URL to check for being a NewsServer URL.
   * @return <code>TRUE</code> if the given URL is a local NewsServer URL.
   */
  public boolean isNewsServerUrl(String url) {
    return url.contains(LOCALHOST) && url.contains(String.valueOf(fPort));
  }

  /**
   * Registers a Viewer under a certain ID to the Registry. Viewers need to
   * register if they want to use the Server. Based on the ID, the Server is
   * asking the correct Viewer for the Content.
   *
   * @param id The unique ID under which the Viewer is stored in the registry.
   * @param viewer The Viewer to store in the registry.
   */
  public void register(String id, ContentViewer viewer) {
    fRegistry.put(id, viewer);
  }

  /**
   * Removes a Viewer from the registry.
   *
   * @param id The ID of the Viewer to remove from the registry.
   */
  public void unregister(String id) {
    fRegistry.remove(id);
  }

  /**
   * Check wether the given URL contains one of the display-operations of this
   * Server.
   *
   * @param url The URL to Test for a Display Operation.
   * @return Returns <code>TRUE</code> if the given URL is a
   * display-operation.
   */
  public boolean isDisplayOperation(String url) {
    if (!StringUtils.isSet(url))
      return false;

    return url.contains(OP_DISPLAY_BOOKMARK) || url.contains(OP_DISPLAY_NEWSBIN) || url.contains(OP_DISPLAY_NEWS) || url.contains(OP_DISPLAY_SEARCHMARK) || URIUtils.ABOUT_BLANK.equals(url);
  }

  /**
   * Creates a valid URL for the given Input
   *
   * @param id The ID of the Viewer
   * @param input The Input of the Viewer
   * @return a valid URL for the given Input
   */
  public String toUrl(String id, Object input) {

    /* Handle this Case */
    if (input == null)
      return URIUtils.ABOUT_BLANK;

    StringBuilder url = new StringBuilder();
    url.append(PROTOCOL).append("://").append(LOCALHOST).append(':').append(fPort).append("/"); //$NON-NLS-1$ //$NON-NLS-2$

    /* Append the ID */
    url.append("?").append(ID).append(id);

    /* Wrap into Object Array */
    if (!(input instanceof Object[]))
      input = new Object[] { input };

    /* Input is an Array of Objects */
    List<Long> news = new ArrayList<Long>();
    List<Long> bookmarks = new ArrayList<Long>();
    List<Long> newsbins = new ArrayList<Long>();
    List<Long> searchmarks = new ArrayList<Long>();

    /* Split into BookMarks, NewsBins, SearchMarks and News */
    for (Object obj : (Object[]) input) {
      if (obj instanceof IBookMark || obj instanceof BookMarkReference)
        bookmarks.add(getId(obj));
      else if (obj instanceof INewsBin || obj instanceof NewsBinReference)
        newsbins.add(getId(obj));
      else if (obj instanceof ISearchMark || obj instanceof SearchMarkReference)
        searchmarks.add(getId(obj));
      else if (obj instanceof INews || obj instanceof NewsReference)
        news.add(getId(obj));
      else if (obj instanceof EntityGroup) {
        List<EntityGroupItem> items = ((EntityGroup) obj).getItems();
        for (EntityGroupItem item : items) {
          IEntity entity = item.getEntity();
          if (entity instanceof INews)
            news.add(getId(entity));
        }
      }
    }

    /* Append Parameter for Bookmarks */
    if (bookmarks.size() > 0) {
      url.append("&").append(OP_DISPLAY_BOOKMARK); //$NON-NLS-1$
      for (Long bookmarkIds : bookmarks)
        url.append(bookmarkIds).append(',');

      /* Remove the last added ',' */
      url.deleteCharAt(url.length() - 1);
    }

    /* Append Parameter for Newsbins */
    if (newsbins.size() > 0) {
      url.append("&").append(OP_DISPLAY_NEWSBIN); //$NON-NLS-1$
      for (Long newsbinIds : newsbins)
        url.append(newsbinIds).append(',');

      /* Remove the last added ',' */
      url.deleteCharAt(url.length() - 1);
    }

    /* Append Parameter for SearchMarks */
    if (searchmarks.size() > 0) {
      url.append("&").append(OP_DISPLAY_SEARCHMARK); //$NON-NLS-1$
      for (Long searchmarkIds : searchmarks)
        url.append(searchmarkIds).append(',');

      /* Remove the last added ',' */
      url.deleteCharAt(url.length() - 1);
    }

    /* Append Parameter for News */
    if (news.size() > 0) {
      url.append("&").append(OP_DISPLAY_NEWS); //$NON-NLS-1$
      for (Long newsitemIds : news)
        url.append(newsitemIds).append(',');

      /* Remove the last added ',' */
      url.deleteCharAt(url.length() - 1);
    }

    return url.toString();
  }

  private Long getId(Object obj) {
    if (obj instanceof IEntity)
      return ((IEntity) obj).getId();
    else if (obj instanceof ModelReference)
      return ((ModelReference) obj).getId();

    return null;
  }

  private void listen() {

    /* Create a Job to listen for Requests */
    fServerJob = new Job("Local News Viewer Server") { //$NON-NLS-1$
      @Override
      protected IStatus run(IProgressMonitor monitor) {

        /* Listen as long not canceled */
        while (!monitor.isCanceled()) {
          BufferedReader buffReader = null;
          Socket socket = null;
          try {

            /* Blocks until Socket accepted */
            socket = fSocket.accept();

            /* Read Incoming Message */
            buffReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message = buffReader.readLine();

            /* Process Message */
            if (StringUtils.isSet(message))
              safeProcess(socket, message);
          } catch (IOException e) {
            if (Activator.getDefault() != null)
              Activator.getDefault().logInfo(e.getMessage());
          }

          /* Cleanup */
          finally {

            /* Close the Reader */
            try {
              if (buffReader != null)
                buffReader.close();
            } catch (Exception e) {
              if (Activator.getDefault() != null)
                Activator.getDefault().logInfo(e.getMessage());
            }

            /* Close the Socket */
            try {
              if (socket != null)
                socket.close();
            } catch (Exception e) {
              if (Activator.getDefault() != null)
                Activator.getDefault().logInfo(e.getMessage());
            }
          }
        }
        return Status.OK_STATUS;
      }
    };

    /* Set as System-Job and Schedule */
    fServerJob.setSystem(true);
    fServerJob.schedule();
  }

  /* Process Message in Safe-Runnable */
  private void safeProcess(final Socket socket, final String message) {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {

        /* This is a Display-Operation */
        if (isDisplayOperation(message))
          processDisplayOperation(socket, message);

        /* This is a startup handshake */
        else
          processHandshake(message);
      }
    });
  }

  /* Process Handshake-Message */
  private void processHandshake(String message) {
    if (fHandshakeHandler != null)
      fHandshakeHandler.handle(message);
  }

  /* Process Message by looking for operations */
  private void processDisplayOperation(Socket socket, String message) {
    List<Object> elements = new ArrayList<Object>();

    /* Substring to get the Parameters String */
    int start = message.indexOf('/');
    int end = message.indexOf(' ', start);
    String parameters = message.substring(start, end);

    /* Retrieve the ID */
    String viewerId = null;
    int idIndex = parameters.indexOf(ID);
    if (idIndex >= 0) {
      start = idIndex + ID.length();
      end = parameters.indexOf('&', start);
      if (end < 0)
        end = parameters.length();

      viewerId = parameters.substring(start, end);
    }

    /* Ask for ContentProvider of Viewer */
    ContentViewer viewer = fRegistry.get(viewerId);
    if (viewer instanceof NewsBrowserViewer && viewer.getContentProvider() != null) {
      IStructuredContentProvider newsContentProvider = (IStructuredContentProvider) viewer.getContentProvider();

      /* Look for BookMarks that are to displayed */
      int displayBookMarkIndex = parameters.indexOf(OP_DISPLAY_BOOKMARK);
      if (displayBookMarkIndex >= 0) {
        start = displayBookMarkIndex + OP_DISPLAY_BOOKMARK.length();
        end = parameters.indexOf('&', start);
        if (end < 0)
          end = parameters.length();

        StringTokenizer tokenizer = new StringTokenizer(parameters.substring(start, end), ",");//$NON-NLS-1$
        while (tokenizer.hasMoreElements()) {
          BookMarkReference ref = new BookMarkReference(Long.valueOf((String) tokenizer.nextElement()));
          elements.addAll(Arrays.asList(newsContentProvider.getElements(ref)));
        }
      }

      /* Look for NewsBins that are to displayed */
      int displayNewsBinsIndex = parameters.indexOf(OP_DISPLAY_NEWSBIN);
      if (displayNewsBinsIndex >= 0) {
        start = displayNewsBinsIndex + OP_DISPLAY_NEWSBIN.length();
        end = parameters.indexOf('&', start);
        if (end < 0)
          end = parameters.length();

        StringTokenizer tokenizer = new StringTokenizer(parameters.substring(start, end), ",");//$NON-NLS-1$
        while (tokenizer.hasMoreElements()) {
          NewsBinReference ref = new NewsBinReference(Long.valueOf((String) tokenizer.nextElement()));
          elements.addAll(Arrays.asList(newsContentProvider.getElements(ref)));
        }
      }

      /* Look for SearchMarks that are to displayed */
      int displaySearchMarkIndex = parameters.indexOf(OP_DISPLAY_SEARCHMARK);
      if (displaySearchMarkIndex >= 0) {
        start = displaySearchMarkIndex + OP_DISPLAY_SEARCHMARK.length();
        end = parameters.indexOf('&', start);
        if (end < 0)
          end = parameters.length();

        StringTokenizer tokenizer = new StringTokenizer(parameters.substring(start, end), ",");//$NON-NLS-1$
        while (tokenizer.hasMoreElements()) {
          SearchMarkReference ref = new SearchMarkReference(Long.valueOf((String) tokenizer.nextElement()));
          elements.addAll(Arrays.asList(newsContentProvider.getElements(ref)));
        }
      }

      /* Look for News that are to displayed */
      int displayNewsIndex = parameters.indexOf(OP_DISPLAY_NEWS);
      if (displayNewsIndex >= 0) {
        start = displayNewsIndex + OP_DISPLAY_NEWS.length();
        end = parameters.indexOf('&', start);
        if (end < 0)
          end = parameters.length();

        StringTokenizer tokenizer = new StringTokenizer(parameters.substring(start, end), ",");//$NON-NLS-1$
        while (tokenizer.hasMoreElements()) {
          NewsReference ref = new NewsReference(Long.valueOf((String) tokenizer.nextElement()));
          elements.addAll(Arrays.asList(newsContentProvider.getElements(ref)));
        }
      }
    }

    /* Reply to the Socket */
    reply(socket, viewerId, elements.toArray());
  }

  /* Create HTML out of the Elements and reply to the Socket */
  @SuppressWarnings("nls")
  private void reply(Socket socket, String viewerId, Object[] elements) {

    /* Only responsible for Viewer-Concerns */
    if (viewerId == null)
      return;

    /* Ask for sorted Elements */
    ContentViewer viewer = fRegistry.get(viewerId);
    ILabelProvider labelProvider = (ILabelProvider) viewer.getLabelProvider();
    Object[] children = new Object[0];
    if (viewer instanceof NewsBrowserViewer)
      children = ((NewsBrowserViewer) viewer).getFlattendChildren(elements);

    /* Write HTML to the Receiver */
    BufferedWriter writer = null;
    try {
      writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

      /* Send Headers (Bug on Mac: Header printed in Browser) */
      if (Application.IS_MAC)
        writer.append("<!--").append(CRLF);
      writer.append("HTTP/1.x 200 OK").append(CRLF);

      synchronized (RFC_1123_DATE) {
        writer.append("Date: ").append(RFC_1123_DATE.format(new Date())).append(CRLF);
      }

      writer.append("Server: RSSOwl Local Server").append(CRLF);
      writer.append("Content-Type: text/html; charset=UTF-8").append(CRLF);
      writer.append("Connection: close").append(CRLF);
      writer.append("Expires: 0").append(CRLF);
      if (Application.IS_MAC)
        writer.append("-->").append(CRLF);
      writer.write(CRLF);

      /* Begin HTML */
      writer.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n");

      /* Windows only: Mark of the Web */
      if (Application.IS_WINDOWS) {
        writer.write(IE_MOTW);
        writer.write("\n");
      }

      writer.write("<html>\n  <head>\n");

      /* Append Base URI if available */
      String base = getBase(children);
      if (base != null) {
        writer.write("  <base href=\"");
        writer.write(base);
        writer.write("\">");
      }

      writer.write("\n  <title></title>");
      writer.write("\n  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");

      /* CSS */
      if (labelProvider instanceof NewsBrowserLabelProvider)
        ((NewsBrowserLabelProvider) labelProvider).writeCSS(writer);

      /* Open Body */
      writer.write("  </head>\n  <body>\n");

      /* Output each Element as HTML */
      for (Object el : children) {
        String html = unicodeToEntities(labelProvider.getText(el));
        writer.write(html);
      }

      /* End HTML */
      writer.write("\n  </body>\n</html>");
    } catch (IOException e) {
      if (Activator.getDefault() != null)
        Activator.getDefault().logInfo(e.getMessage());
    }

    /* Cleanup */
    finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          if (Activator.getDefault() != null)
            Activator.getDefault().logInfo(e.getMessage());
        }
      }
    }
  }

  /* Find BASE-Information from Elements */
  private String getBase(Object elements[]) {
    for (Object object : elements) {
      if (object instanceof INews) {
        INews news = (INews) object;
        IFeed feed = news.getFeedReference().resolve();

        /* Base-Information explicitly set */
        if (feed.getBase() != null)
          return feed.getBase().toString();

        /* Use Feed's Link as fallback */
        return feed.getLink().toString();
      }
    }

    return null;
  }

  @SuppressWarnings("nls")
  private String unicodeToEntities(String str) {
    StringBuilder strBuf = new StringBuilder();

    /* For each character */
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);

      /* This is a non ASCII, non Whitespace character */
      if (!((ch >= 0x0020) && (ch <= 0x007e)) && !Character.isWhitespace(ch)) {
        strBuf.append("&#x");
        String hex = Integer.toHexString(ch & 0xFFFF);

        if (hex.length() == 2)
          strBuf.append("00");

        strBuf.append(hex).append(";");
      }

      /* This is an ASCII character */
      else {
        strBuf.append(ch);
      }
    }
    return strBuf.toString();
  }
}