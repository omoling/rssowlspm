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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.actions.NewBookMarkAction;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.util.JobRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {
  private ApplicationWorkbenchAdvisor fWorkbenchAdvisor;

  /** Constant for the application being run on Windows or not */
  public static final boolean IS_WINDOWS = "win32".equals(SWT.getPlatform());

  /** Constant for the application being run on Linux or not */
  public static final boolean IS_LINUX = "gtk".equals(SWT.getPlatform());

  /** Constant for the application being run on Mac or not */
  public static final boolean IS_MAC = "carbon".equals(SWT.getPlatform());

  /*
   * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
   */
  public Object start(IApplicationContext context) throws Exception {

    /* Set Handshake-Handler to Application Server */
    ApplicationServer server = ApplicationServer.getDefault();
    server.setHandshakeHandler(new ApplicationServer.HandshakeHandler() {
      public void handle(String token) {
        if (StringUtils.isSet(token)) {
          restoreApplication();

          if (hasProtocolHandler(token))
            handleLinkSupplied(token);
        }
      }
    });

    /* Proceed normally */
    Display display = PlatformUI.createDisplay();
    try {

      /* Handle possible Link supplied after startup */
      Runnable runAfterUIStartup = new Runnable() {
        public void run() {
          String link = parseLink(Platform.getCommandLineArgs());
          if (StringUtils.isSet(link))
            handleLinkSupplied(link);
        }
      };

      /* Check Startup Status */
      IStatus startupStatus = Activator.getDefault().getStartupStatus();
      if (startupStatus.getSeverity() == IStatus.ERROR) {
        ErrorDialog.openError(new Shell(), "Fatal startup error", "There was a fatal error while starting RSSOwl.", startupStatus);
        return IApplication.EXIT_OK;
      }

      /* Create the Workbench */
      fWorkbenchAdvisor = new ApplicationWorkbenchAdvisor(runAfterUIStartup);
      int returnCode = PlatformUI.createAndRunWorkbench(display, fWorkbenchAdvisor);
      if (returnCode == PlatformUI.RETURN_RESTART)
        return IApplication.EXIT_RESTART;

      return IApplication.EXIT_OK;
    } finally {
      display.dispose();
    }
  }

  private boolean hasProtocolHandler(String link) {

    /* Is empty or null? */
    if (!StringUtils.isSet(link))
      return false;

    try {
      return Owl.getConnectionService().getHandler(new URI(link)) != null;
    } catch (ConnectionException e) {
      return false;
    } catch (URISyntaxException e) {
      return false;
    }
  }

  /*
   * @see org.eclipse.equinox.app.IApplication#stop()
   */
  public void stop() {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null)
      return;

    final Display display = workbench.getDisplay();
    display.syncExec(new Runnable() {
      public void run() {
        if (!display.isDisposed())
          workbench.close();
      }
    });
  }

  /* Return the first Link in this Array or NULL otherwise */
  private String parseLink(String[] commandLineArgs) {
    for (String arg : commandLineArgs) {
      if (hasProtocolHandler(arg))
        return arg;
    }

    return null;
  }

  /* Focus the Application */
  private void restoreApplication() {
    final Shell shell = OwlUI.getPrimaryShell();
    if (shell != null) {
      JobRunner.runInUIThread(shell, new Runnable() {
        public void run() {

          /* Restore from Tray */
          ApplicationWorkbenchWindowAdvisor advisor = fWorkbenchAdvisor.getPrimaryWorkbenchWindowAdvisor();
          if (advisor != null && advisor.isMinimizedToTray()) {
            advisor.restoreFromTray(shell);
          }

          /* Force Active and De-Iconify */
          else {
            shell.forceActive();
            shell.setMinimized(false);
          }
        }
      });
    }
  }

  /* Handle the supplied Link */
  private void handleLinkSupplied(final String link) {

    /* Need a Shell */
    final Shell shell = OwlUI.getPrimaryShell();
    if (shell == null)
      return;

    /* Check for existing BookMark */
    final IBookMark existingBookMark = getBookMark(link);
    JobRunner.runInUIThread(shell, new Runnable() {
      public void run() {

        /* Open Dialog to add this new BookMark */
        if (existingBookMark == null) {
          new NewBookMarkAction(shell, null, null, link).run(null);
        }

        /* Display selected Feed since its existing already */
        else {
          IWorkbenchPage page = OwlUI.getPage();
          if (page != null) {
            try {
              page.openEditor(new FeedViewInput(existingBookMark), FeedView.ID, OpenStrategy.activateOnOpen());
            } catch (PartInitException e) {
              Activator.getDefault().getLog().log(e.getStatus());
            }
          }
        }
      }
    });
  }

  private IBookMark getBookMark(String link) {

    /* Need a URI */
    URI linkAsURI;
    try {
      linkAsURI = new URI(link);
    } catch (URISyntaxException e) {
      return null;
    }

    /* Check if a BookMark exists for the Link */
    IFeed feed = Owl.getPersistenceService().getDAOService().getFeedDAO().load(linkAsURI);
    if (feed != null) {
      FeedLinkReference feedRef = new FeedLinkReference(feed.getLink());
      final Collection<IBookMark> bookMarks = Owl.getPersistenceService().getDAOService().getBookMarkDAO().loadAll(feedRef);
      if (bookMarks.size() > 0)
        return bookMarks.iterator().next();
    }

    return null;
  }
}