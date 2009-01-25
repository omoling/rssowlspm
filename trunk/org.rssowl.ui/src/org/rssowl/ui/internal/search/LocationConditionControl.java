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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkLabelProvider;
import org.rssowl.ui.internal.views.explorer.BookMarkSorter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The <code>LocationConditionControl</code> is a <code>Composite</code>
 * providing the UI to define Location-Conditions for a Search.
 * <p>
 * TODO This class is currently only working on INews.
 * </p>
 *
 * @author bpasero
 */
public class LocationConditionControl extends Composite {
  private Link fConditionLabel;
  private List<IFolderChild> fSelection;

  /* A Dialog to select Folders and Childs */
  private static class FolderChildChooserDialog extends Dialog {
    private CheckboxTreeViewer fViewer;
    private List<IFolderChild> fCheckedElements;
    private IFolderChild fSelectedElement;

    FolderChildChooserDialog(Shell parentShell, IFolderChild selectedElement, List<IFolderChild> checkedElements) {
      super(parentShell);
      fSelectedElement = selectedElement;
      fCheckedElements = checkedElements;
    }

    List<IFolderChild> getCheckedElements() {
      return fCheckedElements;
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
      Object[] checkedObjects = fViewer.getCheckedElements();
      IStructuredSelection selection = new StructuredSelection(checkedObjects);

      List<IFolderChild> entities = ModelUtils.getFoldersBookMarksBins(selection);
      List<IEntity> entitiesTmp = new ArrayList<IEntity>(entities);

      /* Normalize */
      for (IEntity entity : entitiesTmp) {
        if (entity instanceof IFolder)
          ModelUtils.normalize((IFolder) entity, entities);
      }

      fCheckedElements = entities;

      super.okPressed();
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
      Composite composite = new Composite(parent, SWT.NONE);
      composite.setLayout(LayoutUtils.createGridLayout(1, 10, 10));
      composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

      Label label = new Label(composite, SWT.None);
      label.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
      label.setText("Please choose the locations to search in:");

      fViewer = new CheckboxTreeViewer(composite, SWT.BORDER);
      fViewer.setAutoExpandLevel(2);
      fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      fViewer.getTree().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());

      int viewerHeight = fViewer.getTree().getItemHeight() * 20 + 12;
      ((GridData) composite.getLayoutData()).heightHint = viewerHeight;

      /* Sort by Name if set so */
      if (Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.BE_SORT_BY_NAME)) {
        BookMarkSorter sorter = new BookMarkSorter();
        sorter.setType(BookMarkSorter.Type.SORT_BY_NAME);
        fViewer.setComparator(sorter);
      }

      fViewer.setContentProvider(new ITreeContentProvider() {
        public Object[] getElements(Object inputElement) {
          Collection<IFolder> rootFolders = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
          return rootFolders.toArray();
        }

        public Object[] getChildren(Object parentElement) {
          if (parentElement instanceof IFolder) {
            IFolder folder = (IFolder) parentElement;
            return folder.getChildren().toArray();
          }

          return new Object[0];
        }

        public Object getParent(Object element) {
          if (element instanceof IFolder) {
            IFolder folder = (IFolder) element;
            return folder.getParent();
          }

          return null;
        }

        public boolean hasChildren(Object element) {
          if (element instanceof IFolder) {
            IFolder folder = (IFolder) element;
            return !folder.isEmpty();
          }

          return false;
        }

        public void dispose() {}

        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
      });

      fViewer.setLabelProvider(new BookMarkLabelProvider(false));

      /* Filter out any Search Marks */
      fViewer.addFilter(new ViewerFilter() {
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
          return !(element instanceof ISearchMark);
        }
      });

      fViewer.addDoubleClickListener(new IDoubleClickListener() {
        public void doubleClick(DoubleClickEvent event) {
          IStructuredSelection selection = (IStructuredSelection) event.getSelection();
          IFolder folder = selection.getFirstElement() instanceof IFolder ? (IFolder) selection.getFirstElement() : null;

          /* Expand / Collapse Folder */
          if (folder != null && !folder.isEmpty()) {
            boolean expandedState = !fViewer.getExpandedState(folder);
            fViewer.setExpandedState(folder, expandedState);

            if (expandedState && fViewer.getChecked(folder))
              setChildsChecked(folder, true, true);
          }
        }
      });

      fViewer.setInput(new Object());

      /* Apply checked elements */
      if (fCheckedElements != null) {
        for (IFolderChild child : fCheckedElements) {
          setParentsExpanded(child);
          fViewer.setChecked(child, true);
          setChildsChecked(child, true, true);
        }
      }

      /* Update Checks on Selection */
      fViewer.getTree().addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if (e.detail == SWT.CHECK) {
            TreeItem item = (TreeItem) e.item;
            setChildsChecked((IFolderChild) item.getData(), item.getChecked(), false);

            if (!item.getChecked())
              setParentsChecked((IFolderChild) item.getData(), false);
          }
        }
      });

      /* Update Checks on Expand */
      fViewer.addTreeListener(new ITreeViewerListener() {
        public void treeExpanded(TreeExpansionEvent event) {
          boolean isChecked = fViewer.getChecked(event.getElement());
          if (isChecked)
            setChildsChecked((IFolderChild) event.getElement(), isChecked, false);
        }

        public void treeCollapsed(TreeExpansionEvent event) {}
      });

      /* Select and Show Selection */
      if (fSelectedElement != null) {
        fViewer.setSelection(new StructuredSelection(fSelectedElement));
        fViewer.getTree().showSelection();
      }

      /* Select All / Deselect All */
      Composite buttonContainer = new Composite(composite, SWT.NONE);
      buttonContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
      buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

      Button selectAll = new Button(buttonContainer, SWT.PUSH);
      selectAll.setText("&Select All");
      setButtonLayoutData(selectAll);
      selectAll.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          OwlUI.setAllChecked(fViewer.getTree(), true);
        }
      });

      Button deselectAll = new Button(buttonContainer, SWT.PUSH);
      deselectAll.setText("&Deselect All");
      setButtonLayoutData(deselectAll);
      deselectAll.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          OwlUI.setAllChecked(fViewer.getTree(), false);
        }
      });

      return composite;
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#createButtonBar(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createButtonBar(Composite parent) {

      /* Separator */
      Label sep = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
      sep.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));

      return super.createButtonBar(parent);
    }

    private void setChildsChecked(IFolderChild folderChild, boolean checked, boolean onlyExpanded) {
      if (folderChild instanceof IFolder && (!onlyExpanded || fViewer.getExpandedState(folderChild))) {
        List<IFolderChild> children = ((IFolder) folderChild).getChildren();
        for (IFolderChild child : children) {
          fViewer.setChecked(child, checked);
          setChildsChecked(child, checked, onlyExpanded);
        }
      }
    }

    private void setParentsChecked(IFolderChild folderChild, boolean checked) {
      IFolder parent = folderChild.getParent();
      if (parent != null) {
        fViewer.setChecked(parent, checked);
        setParentsChecked(parent, checked);
      }
    }

    private void setParentsExpanded(IFolderChild folderChild) {
      IFolder parent = folderChild.getParent();
      if (parent != null) {
        fViewer.setExpandedState(parent, true);
        setParentsExpanded(parent);
      }
    }

    /*
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Search Location");
    }

    /*
     * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
     */
    @Override
    protected void initializeBounds() {
      super.initializeBounds();
      Point bestSize = getShell().computeSize(convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH), SWT.DEFAULT);
      getShell().setSize(bestSize);
      LayoutUtils.positionShell(getShell(), false);
    }
  }

  /**
   * @param parent
   * @param style
   */
  public LocationConditionControl(Composite parent, int style) {
    super(parent, style);

    initComponents();
  }

  Long[][] getSelection() {
    return fSelection != null ? ModelUtils.toPrimitive(fSelection) : null;
  }

  void select(Long[][] selection) {
    fSelection = ModelUtils.toEntities(selection);
    fConditionLabel.setText(getLabel(fSelection));
  }

  private void initComponents() {

    /* Apply Gridlayout */
    setLayout(LayoutUtils.createGridLayout(1, 5, 1));

    fConditionLabel = new Link(this, SWT.NONE);
    fConditionLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
    fConditionLabel.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IFolderChild selectedChild = null;
        if (e.text != null && e.text.length() > 0)
          selectedChild = fSelection.get(Integer.valueOf(e.text));

        onChangeCondition(selectedChild);
      }
    });

    fConditionLabel.setText(getLabel(fSelection));
  }

  private void onChangeCondition(IFolderChild selectedChild) {
    FolderChildChooserDialog dialog = new FolderChildChooserDialog(getShell(), selectedChild, fSelection);
    if (dialog.open() == IDialogConstants.OK_ID) {
      List<IFolderChild> checkedElements = dialog.getCheckedElements();
      fSelection = checkedElements;
      fConditionLabel.setText(getLabel(fSelection));
      notifyListeners(SWT.Modify, new Event());

      /* Link might require more space now */
      getShell().layout(true, true);
    }
  }

  private String getLabel(List<IFolderChild> entities) {
    if (entities == null || entities.size() == 0)
      return "<a href=\"\">Choose Location...</a>";

    StringBuilder strB = new StringBuilder();

    for (int i = 0; i < entities.size(); i++) {
      strB.append("<a href=\"" + i + "\">").append(entities.get(i).getName()).append("</a>").append(", ");
    }

    if (strB.length() > 0)
      strB.delete(strB.length() - 2, strB.length());

    return strB.toString();
  }
}