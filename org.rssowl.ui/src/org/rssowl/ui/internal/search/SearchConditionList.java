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

package org.rssowl.ui.internal.search;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>SearchConditionList</code> is a scrolled composite that allows to
 * define a set of <code>ISearchCondition</code>s. The UI allows to add, remove
 * and edit single Conditions.
 *
 * @author bpasero
 */
public class SearchConditionList extends ScrolledComposite {
  private List<SearchConditionItem> fItems;
  private Composite fContainer;
  private LocalResourceManager fResources;
  private Image fAddIcon;
  private Image fDeleteIcon;
  private boolean fModified;
  private int fVisibleItemCount = 3;

  /**
   * @param parent The parent Composite.
   * @param style The Style as defined by SWT constants.
   * @param conditions The initial conditions this List is showing.
   */
  public SearchConditionList(Composite parent, int style, List<ISearchCondition> conditions) {
    super(parent, style | SWT.V_SCROLL);
    fItems = new ArrayList<SearchConditionItem>();
    fResources = new LocalResourceManager(JFaceResources.getResources(), this);

    initResources();
    initComponents(conditions);
  }

  /**
   * Sets the number of <code>SearchConditionItem</code>s that should be visible
   * in the List. If the number of items is higher, scrollbars will be shown
   * automatically.
   *
   * @param count the number of <code>SearchConditionItem</code>s that should be
   * visible in the List.
   */
  public void setVisibleItemCount(int count) {
    Assert.isLegal(count >= 0);
    fVisibleItemCount = count;
  }

  /**
   * Returns <code>TRUE</code> when this List has no items with a specific
   * value, and <code>FALSE</code> otherwise.
   *
   * @return <code>TRUE</code> when this List has no items with a specific
   * value, and <code>FALSE</code> otherwise.
   */
  public boolean isEmpty() {
    for (SearchConditionItem item : fItems) {
      if (item.hasValue())
        return false;
    }

    return true;
  }

  /*
   * @see org.eclipse.swt.widgets.Composite#computeSize(int, int, boolean)
   */
  @Override
  public Point computeSize(int wHint, int hHint, boolean changed) {
    Point point = super.computeSize(wHint, hHint, changed);

    /* Compute from Condition Item */
    if (fVisibleItemCount > 0 && fItems.size() > 0) {
      int itemHeight = fItems.get(0).computeSize(wHint, hHint).y + 4;
      point.y = fVisibleItemCount * itemHeight;
    }

    return point;
  }

  /**
   * Removes all but the first Items from the List.
   */
  public void reset() {
    setRedraw(false);
    try {

      /* Remove all Items */
      if (fItems.size() > 0) {
        List<SearchConditionItem> itemsToRemove = new ArrayList<SearchConditionItem>(fItems);
        for (SearchConditionItem itemToRemove : itemsToRemove) {
          itemToRemove.getParent().dispose();
          removeItem(itemToRemove);
        }
      }

      /* Add default */
      addItem(getDefaultCondition());
    } finally {
      setRedraw(true);
    }

    focusInput();
    fModified = true;
  }

  /**
   * Passes focus to the first item of this List.
   */
  public void focusInput() {
    focusInput(0);
  }

  /**
   * Passes focus to the Item in the list at the given index.
   *
   * @param index the index of the item to focus.
   */
  public void focusInput(int index) {
    if (fItems.size() > index)
      fItems.get(index).focusInput();
  }

  /**
   * @return Returns a List of <code>ISearchCondition</code> representing the
   * selected states.
   * @see SearchConditionList#createConditions(ISearchMark)
   */
  public List<ISearchCondition> createConditions() {
    return createConditions(null);
  }

  /**
   * @param searchmark The parent of the <code>ISearchCondition</code>s that are
   * being created.
   * @return Returns a List of <code>ISearchCondition</code> representing the
   * selected states.
   * @see SearchConditionList#createConditions()
   */
  public List<ISearchCondition> createConditions(ISearchMark searchmark) {
    List<ISearchCondition> conditions = new ArrayList<ISearchCondition>();

    /* For each Item */
    for (SearchConditionItem item : fItems) {
      ISearchCondition condition = item.createCondition(searchmark, true);
      if (condition != null)
        conditions.add(condition);
    }

    return conditions;
  }

  /**
   * Shows the List of <code>ISearchCondition</code> in this List.
   *
   * @param conditions the List of <code>ISearchCondition</code> to show in this
   * List.
   */
  public void showConditions(List<ISearchCondition> conditions) {
    fModified = true;
    setRedraw(false);
    try {

      /* Remove all */
      List<SearchConditionItem> itemsToRemove = new ArrayList<SearchConditionItem>(fItems);
      for (SearchConditionItem itemToRemove : itemsToRemove) {
        itemToRemove.getParent().dispose();
        removeItem(itemToRemove);
      }

      /* Add Conditions */
      if (conditions != null) {
        for (ISearchCondition condition : conditions)
          addItem(condition);
      }
    } finally {
      setRedraw(true);
    }
  }

  /**
   * Optimization: In order to check weather conditions in the list have been
   * modified, this method can be used. Note that this method will also return
   * <code>TRUE</code> if a condition was modified and then reset to its initial
   * value.
   *
   * @return Returns <code>TRUE</code> if the list of conditions was potentially
   * modified (conditions added, removed or updated) and <code>FALSE</code>
   * otherwise.
   */
  public boolean isModified() {
    if (fModified)
      return true;

    for (SearchConditionItem item : fItems) {
      if (item.isModified())
        return true;
    }

    return false;
  }

  private void initResources() {
    fAddIcon = OwlUI.getImage(fResources, "icons/etool16/add.gif");
    fDeleteIcon = OwlUI.getImage(fResources, "icons/etool16/remove.gif");
  }

  private void initComponents(List<ISearchCondition> conditions) {

    /* Adjust Scrolled Composite */
    setLayout(new GridLayout(1, false));
    setExpandHorizontal(true);
    setExpandVertical(true);
    if (getVerticalBar() != null)
      getVerticalBar().setIncrement(10);

    /* Create the Container */
    fContainer = new Composite(this, SWT.NONE);
    fContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    fContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    setContent(fContainer);

    /* Add Conditions */
    if (conditions != null) {
      for (ISearchCondition condition : conditions)
        addItem(condition);
    }

    /* Update Size */
    updateSize();
  }

  private ISearchCondition getDefaultCondition() {
    IModelFactory factory = Owl.getModelFactory();

    ISearchField field = factory.createSearchField(IEntity.ALL_FIELDS, INews.class.getName());
    ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.CONTAINS_ALL, "");

    return condition;
  }

  SearchConditionItem addItem(ISearchCondition condition) {
    return addItem(condition, fItems.size());
  }

  SearchConditionItem addItem(ISearchCondition condition, int index) {
    boolean wasScrollbarShowing = getVerticalBar() != null ? getVerticalBar().isVisible() : false;

    /* Container for Item */
    final Composite itemContainer = new Composite(fContainer, SWT.NONE);
    itemContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0, 0, 0, false));
    itemContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Create Item */
    final SearchConditionItem item = new SearchConditionItem(itemContainer, SWT.NONE, condition);
    item.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Create Button Box */
    final ToolBar buttonBar = new ToolBar(itemContainer, SWT.FLAT);
    buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

    /* Button to add Condition */
    ToolItem addButton = new ToolItem(buttonBar, SWT.DROP_DOWN);
    addButton.setImage(fAddIcon);
    addButton.setToolTipText("Add Condition");

    /* Add Menu */
    final Menu conditionMenu = new Menu(buttonBar);
    createConditionMenu(conditionMenu, item);
    addButton.addListener(SWT.Selection, new Listener() {
      public void handleEvent(Event event) {
        if (event.detail == SWT.ARROW) {
          Rectangle rect = item.getBounds();
          Point pt = new Point(rect.x, rect.y + rect.height);
          pt = buttonBar.toDisplay(pt);
          conditionMenu.setLocation(pt.x, pt.y);
          conditionMenu.setVisible(true);
        } else
          onAdd(item);
      }
    });

    /* Button to delete Condition */
    ToolItem deleteButton = new ToolItem(buttonBar, SWT.PUSH);
    deleteButton.setImage(fDeleteIcon);
    deleteButton.setToolTipText("Delete Condition");
    deleteButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        JobRunner.runInUIThread(0, true, buttonBar, new Runnable() {
          public void run() {
            onDelete(item, itemContainer);
          }
        });
      }
    });

    /* Add to the End */
    if (index == fItems.size())
      fItems.add(item);

    /* Add to specific Index */
    else {
      SearchConditionItem oldItem = fItems.get(index);
      fItems.add(index, item);
      item.getParent().moveAbove(oldItem.getParent());
    }

    /* Force Layout */
    layout(true, true);
    update();

    /* Update Size */
    updateSize();
    adjustSizeForScrollbar(wasScrollbarShowing);

    return item;
  }

  private void adjustSizeForScrollbar(boolean wasScrollbarShowing) {
    ScrollBar verticalBar = getVerticalBar();
    if (verticalBar == null)
      return;

    /* Ignore for application window */
    if (getShell().getParent() == null)
      return;

    int barWidth = verticalBar.getSize().x;

    if (wasScrollbarShowing != verticalBar.isVisible()) {
      Rectangle shellBounds = getShell().getBounds();

      /* Increase if Scrollbar now Visible */
      if (!wasScrollbarShowing)
        getShell().setBounds(shellBounds.x, shellBounds.y, shellBounds.width + barWidth, shellBounds.height);

      /* Reduce if Scrollbar now Invisible */
      else
        getShell().setBounds(shellBounds.x, shellBounds.y, shellBounds.width - barWidth, shellBounds.height);
    }
  }

  private void createConditionMenu(Menu menu, SearchConditionItem item) {
    IModelFactory factory = Owl.getModelFactory();
    String news = INews.class.getName();

    MenuItem mItem = new MenuItem(menu, SWT.PUSH);
    mItem.setText("Entire News");
    hookSelectionListener(mItem, item, factory.createSearchField(IEntity.ALL_FIELDS, news));

    mItem = new MenuItem(menu, SWT.SEPARATOR);

    mItem = new MenuItem(menu, SWT.PUSH);
    mItem.setText("State");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.STATE, news));

    mItem = new MenuItem(menu, SWT.PUSH);
    mItem.setText("Location");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.LOCATION, news));

    new MenuItem(menu, SWT.SEPARATOR);

    mItem = new MenuItem(menu, SWT.PUSH);
    mItem.setText("Title");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.TITLE, news));

    mItem = new MenuItem(menu, SWT.PUSH);
    mItem.setText("Description");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.DESCRIPTION, news));

    mItem = new MenuItem(menu, SWT.PUSH);
    mItem.setText("Author");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.AUTHOR, news));

    mItem = new MenuItem(menu, SWT.PUSH);
    mItem.setText("Category");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.CATEGORIES, news));

    mItem = new MenuItem(menu, SWT.CASCADE);
    mItem.setText("Date");

    Menu dateMenu = new Menu(mItem);
    mItem.setMenu(dateMenu);

    mItem = new MenuItem(dateMenu, SWT.PUSH);
    mItem.setText("Date Modified");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.MODIFIED_DATE, news));

    mItem = new MenuItem(dateMenu, SWT.PUSH);
    mItem.setText("Date Published");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.PUBLISH_DATE, news));

    mItem = new MenuItem(dateMenu, SWT.PUSH);
    mItem.setText("Date Received");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.RECEIVE_DATE, news));

    new MenuItem(dateMenu, SWT.SEPARATOR);

    mItem = new MenuItem(dateMenu, SWT.PUSH);
    mItem.setText("Age in Days");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.AGE_IN_DAYS, news));

    mItem = new MenuItem(menu, SWT.SEPARATOR);

    mItem = new MenuItem(menu, SWT.CASCADE);
    mItem.setText("Other");

    Menu otherMenu = new Menu(mItem);
    mItem.setMenu(otherMenu);

    mItem = new MenuItem(otherMenu, SWT.PUSH);
    mItem.setText("Has Attachments");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.HAS_ATTACHMENTS, news));

    mItem = new MenuItem(otherMenu, SWT.PUSH);
    mItem.setText("Attachment");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.ATTACHMENTS_CONTENT, news));

    new MenuItem(otherMenu, SWT.SEPARATOR);

    mItem = new MenuItem(otherMenu, SWT.PUSH);
    mItem.setText("Source");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.SOURCE, news));

    mItem = new MenuItem(otherMenu, SWT.PUSH);
    mItem.setText("Link");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.LINK, news));

    mItem = new MenuItem(otherMenu, SWT.PUSH);
    mItem.setText("Is Sticky");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.IS_FLAGGED, news));

    mItem = new MenuItem(otherMenu, SWT.PUSH);
    mItem.setText("Feed");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.FEED, news));

    mItem = new MenuItem(otherMenu, SWT.PUSH);
    mItem.setText("Label");
    hookSelectionListener(mItem, item, factory.createSearchField(INews.LABEL, news));
  }

  private void hookSelectionListener(MenuItem item, final SearchConditionItem condition, final ISearchField field) {
    item.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onAdd(condition, field);
      }
    });
  }

  int indexOf(SearchConditionItem item) {
    return fItems.indexOf(item);
  }

  void onAdd(SearchConditionItem selectedItem, ISearchField field) {
    ISearchCondition condition = createCondition(selectedItem.createCondition(null, false));
    condition.setField(field);

    SearchConditionItem addedItem = addItem(condition, indexOf(selectedItem) + 1);
    addedItem.focusInput();

    fModified = true;
  }

  void onAdd(SearchConditionItem selectedItem) {
    ISearchCondition condition = createCondition(selectedItem.createCondition(null, false));
    SearchConditionItem addedItem = addItem(condition, indexOf(selectedItem) + 1);
    addedItem.focusInput();

    fModified = true;
  }

  void onDelete(final SearchConditionItem item, final Composite itemContainer) {
    boolean wasScrollbarShowing = getVerticalBar() != null ? getVerticalBar().isVisible() : false;

    /* Delete */
    itemContainer.dispose();
    removeItem(item);
    fModified = true;

    /* Restore Default if required */
    if (fItems.size() == 0)
      addItem(getDefaultCondition()).focusInput();

    adjustSizeForScrollbar(wasScrollbarShowing);
  }

  private ISearchCondition createCondition(ISearchCondition current) {
    IModelFactory factory = Owl.getModelFactory();
    ISearchField field = factory.createSearchField(current.getField().getId(), current.getField().getEntityName());
    return factory.createSearchCondition(field, current.getSpecifier(), "");
  }

  void removeItem(SearchConditionItem item) {

    /* Dispose and Remove */
    item.dispose();
    fItems.remove(item);

    /* Force Layout */
    layout(true, true);
    update();

    /* Update Size */
    updateSize();
  }

  private void updateSize() {
    setMinSize(fContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
  }
}