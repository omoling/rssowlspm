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

package org.rssowl.ui.internal.dialogs.bookmark;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFeedDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.persist.reference.FeedReference;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.actions.ReloadTypesAction;
import org.rssowl.ui.internal.dialogs.LoginDialog;
import org.rssowl.ui.internal.util.JobRunner;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * A {@link Wizard} to easily create a new {@link IBookMark}.
 * <p>
 * TODO Instead of showing a login dialog if the feed is protected rather have a
 * wizard page asking for username and password.
 * </p>
 * <p>
 * TODO Make loading the feed from the website independent from the checkbox
 * about loading the title from the feed.
 * </p>
 *
 * @author bpasero
 */
public class CreateBookmarkWizard extends Wizard {
  private static final String HTTP = "http://";
  private static final String PROTOCOL_IDENTIFIER = "://";

  private FeedDefinitionPage fFeedDefinitionPage;
  private KeywordSubscriptionPage fKeywordPage;
  private BookmarkDefinitionPage fBookMarkDefinitionPage;
  private final IFolder fSelection;
  private final String fInitialLink;
  private final IMark fPosition;
  private String fLastRealm;

  /**
   * @param parent
   * @param position
   * @param initialLink
   */
  public CreateBookmarkWizard(IFolder parent, IMark position, String initialLink) {
    fSelection = parent;
    fPosition = position;
    fInitialLink = initialLink;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#addPages()
   */
  @Override
  public void addPages() {
    setHelpAvailable(false);
    setWindowTitle("New Bookmark");

    /* Page 1: Enter Link or Keyword */
    fFeedDefinitionPage = new FeedDefinitionPage("Bookmark", fInitialLink);
    addPage(fFeedDefinitionPage);

    /* Page 2: Choose Provider for Keyword Subscription (optional) */
    fKeywordPage = new KeywordSubscriptionPage("Bookmark");
    addPage(fKeywordPage);

    /* Page 2: Define Name and Location */
    fBookMarkDefinitionPage = new BookmarkDefinitionPage("Bookmark", fSelection);
    addPage(fBookMarkDefinitionPage);
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#getNextPage(org.eclipse.jface.wizard.IWizardPage)
   */
  @Override
  public IWizardPage getNextPage(IWizardPage page) {

    /* Special Treatment for Feed Definition Page */
    if (page == fFeedDefinitionPage) {

      /* Keyword Subscription */
      if (fFeedDefinitionPage.isKeywordSubscription())
        return fKeywordPage;

      return fBookMarkDefinitionPage;
    }

    return super.getNextPage(page);
  }

  void loadNameFromFeed() {

    /* Keyword Subscription - Build from Search Engine */
    if (fFeedDefinitionPage.isKeywordSubscription()) {
      fBookMarkDefinitionPage.presetBookmarkName(fKeywordPage.getSelectedEngine().getLabel(fFeedDefinitionPage.getKeyword()));
    }

    /* Link Subscription - Load from Feed if requested */
    else if (fFeedDefinitionPage.loadTitleFromFeed()) {
      String linkVal = fFeedDefinitionPage.getLink();
      if (!linkVal.contains(PROTOCOL_IDENTIFIER))
        linkVal = HTTP + linkVal;

      final String linkText = linkVal;
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) {
          monitor.beginTask("Please wait while loading the title from the feed...", IProgressMonitor.UNKNOWN);

          /* Load Title from Feed */
          String feedTitle = null;
          final URI[] link = new URI[1];
          try {
            link[0] = new URI(linkText);

            /* Load Feed from Link if necessary */
            if (!URIUtils.looksLikeFeedLink(linkText)) {
              final URI feedLink = Owl.getConnectionService().getFeed(link[0]);
              if (feedLink != null)
                link[0] = feedLink;
            }

            feedTitle = Owl.getConnectionService().getLabel(link[0]);
            fLastRealm = null;
          } catch (final ConnectionException e) {

            /* Authentication Required */
            if (e instanceof AuthenticationRequiredException && handleProtectedFeed(link[0], ((AuthenticationRequiredException) e).getRealm())) {
              try {
                feedTitle = Owl.getConnectionService().getLabel(link[0]);
              } catch (ConnectionException e1) {
                Activator.getDefault().logError(e.getMessage(), e);
              }
            }
          } catch (URISyntaxException e) {
            Activator.getDefault().logError(e.getMessage(), e);
          }

          /* Update last Page with Title */
          fBookMarkDefinitionPage.presetBookmarkName(feedTitle);

          /* Update Link */
          if (link[0] != null) {
            JobRunner.runInUIThread(100, getShell(), new Runnable() {
              public void run() {
                fFeedDefinitionPage.setLink(link[0].toString());
              }
            });
          }
        }
      };

      /* Perform Runnable in separate Thread and show progress */
      try {
        getContainer().run(true, false, runnable);
      } catch (InvocationTargetException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (InterruptedException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private boolean handleProtectedFeed(final URI feedLink, final String realm) {
    fLastRealm = realm;
    final boolean[] result = new boolean[1];

    JobRunner.runSyncedInUIThread(getShell(), new Runnable() {
      public void run() {

        /* Show Login Dialog */
        LoginDialog login = new LoginDialog(getShell(), feedLink, realm);
        if (login.open() == Window.OK)
          result[0] = true;
      }
    });

    return result[0];
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#canFinish()
   */
  @Override
  public boolean canFinish() {
    IWizardPage currentPage = getContainer().getCurrentPage();

    /* Allow to finish directly if link is supplied and title grabbed from feed */
    String link = fFeedDefinitionPage.getLink();
    if (currentPage == fFeedDefinitionPage && fFeedDefinitionPage.loadTitleFromFeed() && StringUtils.isSet(link) && !HTTP.equals(link))
      return true;

    /* Require last page then */
    if (currentPage != fBookMarkDefinitionPage)
      return false;

    return super.canFinish();
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#performFinish()
   */
  @Override
  public boolean performFinish() {
    boolean res = false;

    try {
      res = internalPerformFinish();
    } catch (URISyntaxException e) {
      Activator.getDefault().logError(e.getMessage(), e);
      ((DialogPage) getContainer().getCurrentPage()).setMessage("Please enter a valid link", IMessageProvider.ERROR);
    }

    /* Remember Settings */
    IPreferenceScope globalPrefs = Owl.getPreferenceService().getGlobalScope();
    globalPrefs.putBoolean(DefaultPreferences.BM_LOAD_TITLE_FROM_FEED, fFeedDefinitionPage.loadTitleFromFeed());
    if (fFeedDefinitionPage.isKeywordSubscription())
      globalPrefs.putString(DefaultPreferences.LAST_KEYWORD_FEED, fKeywordPage.getSelectedEngine().getId());

    return res;
  }

  private boolean internalPerformFinish() throws URISyntaxException {
    final String[] title = new String[] { fBookMarkDefinitionPage.getBookmarkName() };
    final URI[] uriObj = new URI[1];
    if (fFeedDefinitionPage.isKeywordSubscription())
      uriObj[0] = new URI(fKeywordPage.getSelectedEngine().toUrl(fFeedDefinitionPage.getKeyword()));
    else {
      String linkVal = fFeedDefinitionPage.getLink();
      if (!linkVal.contains(PROTOCOL_IDENTIFIER))
        linkVal = HTTP + linkVal;
      if (linkVal.endsWith("/")) //Strip trailing slashes
        linkVal = linkVal.substring(0, linkVal.length() - 1);
      uriObj[0] = new URI(linkVal);
    }

    /* Load Title from Feed if not provided */
    if (!fFeedDefinitionPage.isKeywordSubscription() && !StringUtils.isSet(title[0])) {
      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) {
          monitor.beginTask("Please wait while loading the title from the feed...", IProgressMonitor.UNKNOWN);
          try {

            /* Load Feed from Link if necessary */
            if (!URIUtils.looksLikeFeedLink(uriObj[0].toString())) {
              final URI feedLink = Owl.getConnectionService().getFeed(uriObj[0]);
              if (feedLink != null)
                uriObj[0] = feedLink;
            }

            title[0] = Owl.getConnectionService().getLabel(uriObj[0]);
          } catch (final ConnectionException e) {

            /* Authentication Required */
            if (e instanceof AuthenticationRequiredException && handleProtectedFeed(uriObj[0], ((AuthenticationRequiredException) e).getRealm())) {
              try {
                title[0] = Owl.getConnectionService().getLabel(uriObj[0]);
              } catch (ConnectionException e1) {
                Activator.getDefault().logError(e.getMessage(), e);
              }
            }
          }
        }
      };

      /* Perform Runnable in same Thread and show progress */
      try {
        getContainer().run(true, false, runnable);
      } catch (InvocationTargetException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (InterruptedException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }

      /* Cancel creation and show warning if title failed loading */
      if (!StringUtils.isSet(title[0])) {
        getContainer().showPage(fBookMarkDefinitionPage);
        return false;
      }
    }

    IFeedDAO feedDAO = DynamicDAO.getDAO(IFeedDAO.class);

    /* Check if a Feed with the URL already exists */
    FeedReference feedRef = feedDAO.loadReference(uriObj[0]);

    /* Create a new Feed then */
    if (feedRef == null) {
      IFeed feed = Owl.getModelFactory().createFeed(null, uriObj[0]);
      feed = feedDAO.save(feed);
    }

    /* Create the BookMark */
    IFolder parent = fBookMarkDefinitionPage.getFolder();

    FeedLinkReference feedLinkRef = new FeedLinkReference(uriObj[0]);
    IBookMark bookmark = Owl.getModelFactory().createBookMark(null, parent, feedLinkRef, title[0], fPosition, fPosition != null ? true : null);

    /* Copy all Properties from Parent into this Mark */
    Map<String, Serializable> properties = parent.getProperties();

    for (Map.Entry<String, Serializable> property : properties.entrySet())
      bookmark.setProperty(property.getKey(), property.getValue());

    /* Remember Realm Property */
    if (StringUtils.isSet(fLastRealm))
      bookmark.setProperty(Controller.BM_REALM_PROPERTY, fLastRealm);

    parent = DynamicDAO.save(parent);

    /* Auto-Reload added BookMark */
    for (IMark mark : parent.getMarks()) {
      if (mark.equals(bookmark)) {
        new ReloadTypesAction(new StructuredSelection(mark), getShell()).run();
        break;
      }
    }

    return true;
  }

  /*
   * @see org.eclipse.jface.wizard.Wizard#needsProgressMonitor()
   */
  @Override
  public boolean needsProgressMonitor() {
    return true;
  }
}