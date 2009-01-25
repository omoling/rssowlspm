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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MessageBox;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.util.BrowserUtils;
import org.rssowl.ui.internal.util.JobRunner;

/**
 * Instances of <code>CBrowser</code> wrap around the Browser-Widget and enhance
 * it by some means.
 *
 * @author bpasero
 */
public class CBrowser {

  /* JavaScript: print() Method */
  private static final String JAVA_SCRIPT_PRINT = "window.print();";

  /* System Properties to configure proxy with XULRunner */
  private static final String XULRUNNER_PROXY_HOST = "network.proxy_host";
  private static final String XULRUNNER_PROXY_PORT = "network.proxy_port";

  /* Flag to check if Mozilla is available on Windows */
  private static boolean fgMozillaAvailable = true;

  private Browser fBrowser;
  private boolean fBlockNavigation;
  private IPreferenceScope fPreferences;
  private IPreferenceScope fEclipsePreferences;
  private Map<String, ILinkHandler> fLinkHandler;

  /**
   * @param parent The Parent Composite of this Browser.
   * @param style The Style to use for the Browser-
   */
  public CBrowser(Composite parent, int style) {
    fPreferences = Owl.getPreferenceService().getGlobalScope();
    fEclipsePreferences = Owl.getPreferenceService().getEclipseScope();
    try {
      fBrowser = createBrowser(parent, style);
    } catch (SWTError e) {
      MessageBox box = new MessageBox(parent.getShell(), SWT.ICON_ERROR | SWT.OK | SWT.CANCEL);
      box.setText("Error Creating Browser");
      box.setMessage("RSSOwl was unable to create a browser for reading news. Please refer to the FAQ for further help.\n\nClick 'Ok' to open the FAQ now.");
      if (box.open() == SWT.OK)
        BrowserUtils.openLinkExternal("http://boreal.rssowl.org/#faq");

      throw e;
    }
    fLinkHandler = new HashMap<String, ILinkHandler>();
    hookListeners();

    /* Add custom Context Menu on OS where this is not supported */
    if (Application.IS_LINUX || fgMozillaAvailable)
      hookMenu();
  }

  /**
   * Adds the given Handler to this instance responsible for the given Command.
   *
   * @param commandId The ID of the Command the provided Handler is responsible
   * for.
   * @param handler The Handler responsible for the fiven ID.
   */
  public void addLinkHandler(String commandId, ILinkHandler handler) {
    fLinkHandler.put(commandId, handler);
  }

  private Browser createBrowser(Composite parent, int style) {
    Browser browser = null;

    /* Properly configure Proxy for Firefox/XULRunner if required */
    if (Application.IS_LINUX || (Application.IS_WINDOWS && fgMozillaAvailable)) {
      String proxyHost = fEclipsePreferences.getString(DefaultPreferences.ECLIPSE_PROXY_HOST);
      String proxyPort = fEclipsePreferences.getString(DefaultPreferences.ECLIPSE_PROXY_PORT);
      if (StringUtils.isSet(proxyHost) && StringUtils.isSet(proxyPort)) {
        System.setProperty(XULRUNNER_PROXY_HOST, proxyHost);
        System.setProperty(XULRUNNER_PROXY_PORT, proxyPort);
      }
    }

    /* Try Mozilla over IE on Windows */
    if (Application.IS_WINDOWS && fgMozillaAvailable) {
      try {
        browser = new Browser(parent, style | SWT.MOZILLA);
      } catch (SWTError e) {
        fgMozillaAvailable = false;

        if (!"No more handles [Could not detect registered XULRunner to use]".equals(e.getMessage())) //This happens too often to log it
          Activator.getDefault().getLog().log(Activator.getDefault().createInfoStatus(e.getMessage(), null));
      }
    }

    /* Any other OS, or Mozilla unavailable, use default */
    if (browser == null)
      browser = new Browser(parent, style);

    /* Add Focusless Scroll Hook on Windows */
    if (Application.IS_WINDOWS)
      browser.setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, true);

    /* Clear all Link Handlers upon disposal */
    browser.addDisposeListener(new DisposeListener() {
      public void widgetDisposed(DisposeEvent e) {
        fLinkHandler.clear();
      }
    });

    return browser;
  }

  private void hookMenu() {
    MenuManager manager = new MenuManager();
    manager.setRemoveAllWhenShown(true);
    manager.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {

        /* Back */
        manager.add(new Action("Back") {
          @Override
          public void run() {
            fBrowser.back();
          }

          @Override
          public boolean isEnabled() {
            return fBrowser.isBackEnabled();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/etool16/backward.gif");
          }
        });

        /* Forward */
        manager.add(new Action("Forward") {
          @Override
          public void run() {
            fBrowser.forward();
          }

          @Override
          public boolean isEnabled() {
            return fBrowser.isForwardEnabled();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/etool16/forward.gif");
          }
        });

        /* Reload */
        manager.add(new Separator());
        manager.add(new Action("Reload") {
          @Override
          public void run() {
            fBrowser.refresh();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/elcl16/reload.gif");
          }
        });

        /* Stop */
        manager.add(new Action("Stop") {
          @Override
          public void run() {
            fBrowser.stop();
          }

          @Override
          public ImageDescriptor getImageDescriptor() {
            return OwlUI.getImageDescriptor("icons/etool16/cancel.gif");
          }
        });
      }
    });

    Menu menu = manager.createContextMenu(fBrowser);
    fBrowser.setMenu(menu);
  }

  /**
   * Returns the Browser-Widget this class is wrapping.
   *
   * @return The Browser-Widget this class is wrapping.
   */
  public Browser getControl() {
    return fBrowser;
  }

  /**
   * Browse to the given URL.
   *
   * @param url The URL to browse to.
   */
  public void setUrl(String url) {
    fBlockNavigation = false;
    fBrowser.setUrl(url);
  }

  /**
   * Navigate to the previous session history item.
   *
   * @return <code>true</code> if the operation was successful and
   * <code>false</code> otherwise
   */
  public boolean back() {
    fBlockNavigation = false;
    return fBrowser.back();
  }

  /**
   * Navigate to the next session history item.
   *
   * @return <code>true</code> if the operation was successful and
   * <code>false</code> otherwise
   */
  public boolean forward() {
    fBlockNavigation = false;
    return fBrowser.forward();
  }

  /**
   * Print the Browser using the JavaScript print() method
   *
   * @return <code>TRUE</code> in case of success, <code>FALSE</code> otherwise
   */
  public boolean print() {
    return fBrowser.execute(JAVA_SCRIPT_PRINT);
  }

  private void hookListeners() {
    
	//TODO Userstory 2: Listener is no longer needed since it is not possible to open the internal browser
    // Listen to Open-Window-Changes
    /*fBrowser.addOpenWindowListener(new OpenWindowListener() {
      public void open(WindowEvent event) {

        // Do not handle when external Browser is being used
        if (useExternalBrowser())
          return;

        // Open Browser in new Tab
        WebBrowserView browserView = BrowserUtils.openLinkInternal(URIUtils.ABOUT_BLANK);
        if (browserView != null)
          event.browser = browserView.getBrowser().getControl();
      }
    });*/

    /* Listen to Location-Changes */
    fBrowser.addLocationListener(new LocationListener() {
      public void changed(LocationEvent event) {
        if (event.top && useExternalBrowser())
          fBlockNavigation = true;
      }

      public void changing(LocationEvent event) {

        /* Handle Application Protocol */
        if (event.location != null && event.location.contains(ILinkHandler.HANDLER_PROTOCOL)) {
          try {
            final URI link = new URI(event.location);
            final String host = link.getHost();
            if (StringUtils.isSet(host) && fLinkHandler.containsKey(host)) {

              /* See Bug 747 - run asynced */
              JobRunner.runInUIThread(0, true, getControl(), new Runnable() {
                public void run() {
                  fLinkHandler.get(host).handle(host, link);
                }
              });

              event.doit = false;
              return;
            }
          } catch (URISyntaxException e) {
            Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
          }
        }
        
        //TODO Userstory 2: Method useExternalBrowser() returns always true and therefore condition is always false
        /* Feature not enabled */
        /*if (!useExternalBrowser())
          return;
        */

        /*
         * Bug on Mac: Safari puts out links from images as event.location
         * resulting in RSSOwl to open a browser although its not necessary
         * Workaround is to disable this feature on Mac until its fixed. (see
         * Bug #1068304).
         */
        if (Application.IS_MAC)
          return;

        /* Only proceed if navigation should not be blocked */
        if (!fBlockNavigation)
          return;

        /* Let local ApplicationServer URLs open */
        if (ApplicationServer.getDefault().isNewsServerUrl(event.location))
          return;

        /* The URL must not be empty or about:blank (Problem on Linux) */
        if (!StringUtils.isSet(event.location) || URIUtils.ABOUT_BLANK.equals(event.location))
          return;

        /* Finally, cancel event and open URL external */
        event.doit = false;
        BrowserUtils.openLinkExternal(event.location);
      }
    });
  }

  private boolean useExternalBrowser() {
    return fPreferences.getBoolean(DefaultPreferences.USE_DEFAULT_EXTERNAL_BROWSER) || fPreferences.getBoolean(DefaultPreferences.USE_CUSTOM_EXTERNAL_BROWSER);
  }
}