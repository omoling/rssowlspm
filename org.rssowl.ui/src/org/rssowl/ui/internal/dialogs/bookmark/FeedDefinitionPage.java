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

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.ICategoryDAO;
import org.rssowl.core.persist.dao.ILabelDAO;
import org.rssowl.core.persist.pref.IPreferenceScope;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author bpasero
 */
public class FeedDefinitionPage extends WizardPage {
  private static final String HTTP = "http://";

  private Text fFeedLinkInput;
  private Text fKeywordInput;
  private Button fLoadTitleFromFeedButton;
  private Button fFeedByLinkButton;
  private Button fFeedByKeywordButton;
  private String fInitialLink;
  private IPreferenceScope fGlobalScope = Owl.getPreferenceService().getGlobalScope();
  private boolean fIsAutoCompleteKeywordHooked;
  private Map<String, IBookMark> fExistingFeeds = new HashMap<String, IBookMark>();

  /**
   * @param pageName
   * @param initialLink
   */
  protected FeedDefinitionPage(String pageName, String initialLink) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/bkmrk_wiz.gif"));
    setMessage("Create a new bookmark to read news from a feed.");
    fInitialLink = initialLink;

    Collection<IBookMark> bookmarks = DynamicDAO.loadAll(IBookMark.class);
    for (IBookMark bookMark : bookmarks) {
      fExistingFeeds.put(bookMark.getFeedLinkReference().getLinkAsText(), bookMark);
    }
  }

  boolean loadTitleFromFeed() {
    return fLoadTitleFromFeedButton.getSelection();
  }

  private String loadInitialLinkFromClipboard() {
    String initial = HTTP;

    Clipboard cb = new Clipboard(getShell().getDisplay());
    TextTransfer transfer = TextTransfer.getInstance();
    String data = (String) cb.getContents(transfer);
    data = (data != null) ? data.trim() : null;
    cb.dispose();

    if (URIUtils.looksLikeLink(data)) {
      if (!data.contains("://"))
        data = initial + data;
      initial = data;
    }

    return initial;
  }

  String getLink() {
    return fFeedByLinkButton.getSelection() ? fFeedLinkInput.getText().trim() : null;
  }

  void setLink(String link) {
    fFeedLinkInput.setText(link);
    onLinkChange();
  }

  String getKeyword() {
    return fFeedByKeywordButton.getSelection() ? fKeywordInput.getText() : null;
  }

  boolean isKeywordSubscription() {
    return StringUtils.isSet(getKeyword());
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    if (visible && !isKeywordSubscription())
      fFeedLinkInput.setFocus();
    else if (visible)
      fKeywordInput.setFocus();
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {

    /* Checked for proper Link */
    if (fFeedByLinkButton.getSelection())
      return fFeedLinkInput.getText().length() > 0;

    /* Check for Keyword */
    return fKeywordInput.getText().length() > 0;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* 1) Feed by Link */
    if (!StringUtils.isSet(fInitialLink))
      fInitialLink = loadInitialLinkFromClipboard();

    fFeedByLinkButton = new Button(container, SWT.RADIO);
    fFeedByLinkButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFeedByLinkButton.setText("Create a feed by supplying the website or direct link:");
    fFeedByLinkButton.setSelection(true);
    fFeedByLinkButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fFeedLinkInput.setEnabled(fFeedByLinkButton.getSelection());
        fLoadTitleFromFeedButton.setEnabled(fFeedByLinkButton.getSelection());
        fFeedLinkInput.setFocus();
        getContainer().updateButtons();
      }
    });

    Composite textIndent = new Composite(container, SWT.NONE);
    textIndent.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    textIndent.setLayout(new GridLayout(1, false));
    ((GridLayout) textIndent.getLayout()).marginLeft = 10;
    ((GridLayout) textIndent.getLayout()).marginBottom = 10;

    fFeedLinkInput = new Text(textIndent, SWT.BORDER);
    fFeedLinkInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    GC gc = new GC(fFeedLinkInput);
    gc.setFont(JFaceResources.getDialogFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    int entryFieldWidth = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.ENTRY_FIELD_WIDTH);
    gc.dispose();

    ((GridData) fFeedLinkInput.getLayoutData()).widthHint = entryFieldWidth; //Required to avoid large spanning dialog for long Links
    fFeedLinkInput.setFocus();

    if (StringUtils.isSet(fInitialLink) && !fInitialLink.equals(HTTP)) {
      fFeedLinkInput.setText(fInitialLink);
      fFeedLinkInput.selectAll();
      onLinkChange();
    } else {
      fFeedLinkInput.setText(HTTP);
      fFeedLinkInput.setSelection(HTTP.length());
    }

    fFeedLinkInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        getContainer().updateButtons();
        onLinkChange();
      }
    });

    fLoadTitleFromFeedButton = new Button(textIndent, SWT.CHECK);
    fLoadTitleFromFeedButton.setText("Use the title of the feed as name for the bookmark");
    fLoadTitleFromFeedButton.setSelection(fGlobalScope.getBoolean(DefaultPreferences.BM_LOAD_TITLE_FROM_FEED));
    fLoadTitleFromFeedButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        getContainer().updateButtons();
      }
    });

    /* 2) Feed by Keyword */
    fFeedByKeywordButton = new Button(container, SWT.RADIO);
    fFeedByKeywordButton.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFeedByKeywordButton.setText("Create a feed by typing a keyword or phrase describing your interest:");
    fFeedByKeywordButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        fKeywordInput.setEnabled(fFeedByKeywordButton.getSelection());

        if (fKeywordInput.isEnabled())
          hookKeywordAutocomplete();

        fKeywordInput.setFocus();
        getContainer().updateButtons();
      }
    });

    textIndent = new Composite(container, SWT.NONE);
    textIndent.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    textIndent.setLayout(new GridLayout(1, false));
    ((GridLayout) textIndent.getLayout()).marginLeft = 10;

    fKeywordInput = new Text(textIndent, SWT.BORDER);
    fKeywordInput.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fKeywordInput.setEnabled(false);
    fKeywordInput.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        getContainer().updateButtons();
      }
    });

    setControl(container);
  }

  private void onLinkChange() {
    IBookMark existingBookMark = fExistingFeeds.get(fFeedLinkInput.getText());

    if (existingBookMark != null)
      setMessage("A bookmark named '" + existingBookMark.getName() + "' with the same link already exists.", WARNING);
    else
      setMessage("Create a new bookmark to read news from a feed.");
  }

  private void hookKeywordAutocomplete() {

    /* Only perform once */
    if (fIsAutoCompleteKeywordHooked)
      return;
    fIsAutoCompleteKeywordHooked = true;

    /* Auto-Activate on Key-Down */
    KeyStroke activationKey = KeyStroke.getInstance(SWT.ARROW_DOWN);

    /* Create Content Proposal Adapter */
    final SimpleContentProposalProvider proposalProvider = new SimpleContentProposalProvider(new String[0]);
    proposalProvider.setFiltering(true);
    final ContentProposalAdapter adapter = new ContentProposalAdapter(fKeywordInput, new TextContentAdapter(), proposalProvider, activationKey, null);
    adapter.setPropagateKeys(true);
    adapter.setAutoActivationDelay(500);
    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

    /*
     * TODO: This is a hack but there doesnt seem to be any API to set the size
     * of the popup to match the actual size of the Text widget being used.
     */
    fKeywordInput.getDisplay().timerExec(100, new Runnable() {
      public void run() {
        if (!fKeywordInput.isDisposed()) {
          adapter.setPopupSize(new Point(fKeywordInput.getSize().x, 100));
        }
      }
    });

    /* Load proposals in the Background */
    JobRunner.runDelayedInBackgroundThread(new Runnable() {
      public void run() {
        if (!fKeywordInput.isDisposed()) {
          Set<String> values = new HashSet<String>();

          values.addAll(DynamicDAO.getDAO(ICategoryDAO.class).loadAllNames());

          Collection<ILabel> labels = DynamicDAO.getDAO(ILabelDAO.class).loadAll();
          for (ILabel label : labels) {
            values.add(label.getName());
          }

          /* Apply Proposals */
          if (!fKeywordInput.isDisposed())
            applyProposals(values, proposalProvider, adapter);
        }
      }
    });

    /* Show UI Hint that Content Assist is available */
    ControlDecoration controlDeco = new ControlDecoration(fKeywordInput, SWT.LEFT | SWT.TOP);
    controlDeco.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL).getImage());
    controlDeco.setDescriptionText("Content Assist Available (Press Arrow-Down Key)");
    controlDeco.setShowOnlyOnFocus(true);
  }

  private void applyProposals(Collection<String> values, SimpleContentProposalProvider provider, ContentProposalAdapter adapter) {

    /* Extract Proposals */
    final String[] proposals = new String[values.size()];
    Set<Character> charSet = new HashSet<Character>();
    int i = 0;
    for (String value : values) {
      proposals[i] = value;

      char c = value.charAt(0);
      charSet.add(Character.toLowerCase(c));
      charSet.add(Character.toUpperCase(c));
      i++;
    }

    /* Auto-Activate on first Key typed */
    char[] activationChars = new char[charSet.size()];
    i = 0;
    for (char c : charSet) {
      activationChars[i] = c;
      i++;
    }

    /* Apply proposals and auto-activation chars */
    provider.setProposals(proposals);
    adapter.setAutoActivationCharacters(activationChars);
  }
}