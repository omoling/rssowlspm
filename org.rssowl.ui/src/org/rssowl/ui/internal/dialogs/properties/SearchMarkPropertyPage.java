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

package org.rssowl.ui.internal.dialogs.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.util.ReparentInfo;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.FolderChooser;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.search.SearchConditionList;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class SearchMarkPropertyPage implements IEntityPropertyPage {
  private IPropertyDialogSite fSite;
  private Text fNameInput;
  private Button fMatchAllRadio;
  private SearchConditionList fSearchConditionList;
  private Button fMatchAnyRadio;
  private FolderChooser fFolderChooser;
  private boolean fSearchChanged;
  private List<IEntity> fEntities;

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite,
   * java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    fSite = site;
    fEntities = entities;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {

    /* Create contents for single selection */
    if (fEntities.size() == 1)
      return createContentsSingleSearch(parent);

    /* Create contents for multi selection */
    return createContentsMultiSearch(parent);
  }

  private Control createContentsSingleSearch(Composite parent) {
    ISearchMark mark = (ISearchMark) fEntities.get(0);

    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    /* Name */
    Label nameLabel = new Label(container, SWT.None);
    nameLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    nameLabel.setText("Name: ");

    Composite nameContainer = new Composite(container, SWT.BORDER);
    nameContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    nameContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    nameContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    fNameInput = new Text(nameContainer, SWT.NONE);
    fNameInput.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fNameInput.setText(mark.getName());

    ToolBar generateTitleBar = new ToolBar(nameContainer, SWT.FLAT);
    generateTitleBar.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    ToolItem generateTitleItem = new ToolItem(generateTitleBar, SWT.PUSH);
    generateTitleItem.setImage(OwlUI.getImage(fSite.getResourceManager(), "icons/etool16/info.gif"));
    generateTitleItem.setToolTipText("Create name from conditions");
    generateTitleItem.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onGenerateName();
      }
    });

    /* Location */
    Label locationLabel = new Label(container, SWT.None);
    locationLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    locationLabel.setText("Location: ");

    fFolderChooser = new FolderChooser(container, mark.getParent(), SWT.BORDER, true);
    fFolderChooser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
    fFolderChooser.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 5, false));
    fFolderChooser.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));

    Composite radioContainer = new Composite(container, SWT.None);
    radioContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    radioContainer.setLayout(LayoutUtils.createGridLayout(2, 5, 0));
    ((GridLayout) radioContainer.getLayout()).marginTop = 10;

    fMatchAllRadio = new Button(radioContainer, SWT.RADIO);
    fMatchAllRadio.setText("Match all conditions");
    fMatchAllRadio.setSelection(mark.matchAllConditions());

    fMatchAnyRadio = new Button(radioContainer, SWT.RADIO);
    fMatchAnyRadio.setText("Match any condition");
    fMatchAnyRadio.setSelection(!mark.matchAllConditions());

    Composite conditionsContainer = new Composite(container, SWT.BORDER);
    conditionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    conditionsContainer.setLayout(LayoutUtils.createGridLayout(1));
    conditionsContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    conditionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);

    /* Search Conditions List */
    fSearchConditionList = new SearchConditionList(conditionsContainer, SWT.None, mark.getSearchConditions());
    fSearchConditionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fSearchConditionList.setVisibleItemCount(3);

    return container;
  }

  private Control createContentsMultiSearch(Composite parent) {
    boolean separateFromTop = false;

    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(LayoutUtils.createGridLayout(2, 10, 10));

    /* Location */
    IFolder sameParent = getSameParent(fEntities);
    if (sameParent != null) {
      separateFromTop = true;

      Label locationLabel = new Label(container, SWT.None);
      locationLabel.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
      locationLabel.setText("Location: ");

      fFolderChooser = new FolderChooser(container, sameParent, null, SWT.BORDER, true);
      fFolderChooser.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      fFolderChooser.setLayout(LayoutUtils.createGridLayout(1, 0, 0, 2, 5, false));
      fFolderChooser.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    }

    /* Other Settings */
    Composite otherSettingsContainer = new Composite(container, SWT.NONE);
    otherSettingsContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    otherSettingsContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, true, 2, 1));

    if (separateFromTop)
      ((GridLayout) otherSettingsContainer.getLayout()).marginTop = 15;

    /* Name */
    Label nameLabel = new Label(otherSettingsContainer, SWT.None);
    nameLabel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, true, false, 2, 1));
    nameLabel.setText("Add search conditions to all of the " + fEntities.size() + " selected saved searches: ");

    Composite conditionsContainer = new Composite(otherSettingsContainer, SWT.BORDER);
    conditionsContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
    conditionsContainer.setLayout(LayoutUtils.createGridLayout(1));
    conditionsContainer.setBackground(container.getDisplay().getSystemColor(SWT.COLOR_WHITE));
    conditionsContainer.setBackgroundMode(SWT.INHERIT_FORCE);

    /* Search Conditions List */
    fSearchConditionList = new SearchConditionList(conditionsContainer, SWT.None, getDefaultConditions());
    fSearchConditionList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fSearchConditionList.setVisibleItemCount(3);

    return container;
  }

  private IFolder getSameParent(List<IEntity> entities) {
    IFolder parent = null;

    for (IEntity entity : entities) {
      if (!(entity instanceof IFolderChild))
        return null;

      IFolderChild folderChild = (IFolderChild) entity;
      IFolder folder = folderChild.getParent();
      if (parent == null)
        parent = folder;
      else if (parent != folder)
        return null;
    }

    return parent;
  }

  private List<ISearchCondition> getDefaultConditions() {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>(1);
    IModelFactory factory = Owl.getModelFactory();

    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "");

    conditions.add(condition);

    return conditions;
  }

  void onGenerateName() {
    List<ISearchCondition> conditions = fSearchConditionList.createConditions();
    String name = ModelUtils.getName(conditions, fMatchAllRadio.getSelection());

    if (name.length() > 0) {
      fNameInput.setText(name);
      fNameInput.selectAll();
    }
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#setFocus()
   */
  public void setFocus() {
    if (fNameInput != null) {
      fNameInput.setFocus();
      fNameInput.selectAll();
    }
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {

    /* Perform OK for single selection */
    if (fEntities.size() == 1)
      return performOkSingleSearch(entitiesToSave);

    /* Perform OK for multi selection */
    return performOkMultiSearch(entitiesToSave);
  }

  private boolean performOkSingleSearch(Set<IEntity> entitiesToSave) {
    ISearchMark mark = (ISearchMark) fEntities.get(0);

    /* Require a Name */
    if (fNameInput.getText().length() == 0) {
      fSite.select(this);
      fNameInput.setFocus();
      fSite.setMessage("Please enter a name for the saved search.", IPropertyDialogSite.MessageType.ERROR);

      return false;
    }

    /* Require a Condition */
    if (fSearchConditionList.isEmpty()) {
      fSite.select(this);
      fNameInput.setFocus();
      fSite.setMessage("Please specify your search by defining some conditions.", IPropertyDialogSite.MessageType.ERROR);

      return false;
    }

    /* Check for changed Name */
    if (!mark.getName().equals(fNameInput.getText())) {
      mark.setName(fNameInput.getText());
      entitiesToSave.add(mark);
    }

    /* Update match-all-condition */
    if (mark.matchAllConditions() != fMatchAllRadio.getSelection()) {
      mark.setMatchAllConditions(fMatchAllRadio.getSelection());
      entitiesToSave.add(mark);
      fSearchChanged = true;
    }

    /* Update Conditions (TODO Could be optimized to not replace all conditions) */
    if (fSearchConditionList.isModified()) {
      entitiesToSave.add(mark);
      fSearchChanged = true;

      /* Remove Old Conditions */
      List<ISearchCondition> oldConditions = mark.getSearchConditions();
      for (ISearchCondition oldCondition : oldConditions) {
        mark.removeSearchCondition(oldCondition);
      }

      /* Add New Conditions */
      fSearchConditionList.createConditions(mark);
    }

    /* Re-Run search if conditions changed */
    if (fSearchChanged)
      Controller.getDefault().getSavedSearchService().updateSavedSearches(Collections.singleton(mark), true);

    return true;
  }

  private boolean performOkMultiSearch(Set<IEntity> entitiesToSave) {
    Set<ISearchMark> searchesToUpdate = new HashSet<ISearchMark>(fEntities.size());

    for (IEntity entity : fEntities) {
      ISearchMark mark = (ISearchMark) entity;
      List<ISearchCondition> conditions = fSearchConditionList.createConditions(mark);

      if (!conditions.isEmpty()) {
        entitiesToSave.add(entity);
        searchesToUpdate.add(mark);
      }
    }

    /* Force Update if changed */
    if (!searchesToUpdate.isEmpty())
      Controller.getDefault().getSavedSearchService().updateSavedSearches(searchesToUpdate, true);

    return true;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {

    /* Reparent if necessary */
    for (IEntity entity : fEntities) {
      ISearchMark mark = (ISearchMark) entity;
      if (mark.getParent() != fFolderChooser.getFolder()) {
        ReparentInfo<IFolderChild, IFolder> reparent = new ReparentInfo<IFolderChild, IFolder>(mark, fFolderChooser.getFolder(), null, null);
        Owl.getPersistenceService().getDAOService().getFolderDAO().reparent(Collections.singletonList(reparent));
      }
    }
  }
}