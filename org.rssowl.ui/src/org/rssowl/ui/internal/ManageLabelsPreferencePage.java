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

import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.dialogs.ConfirmDeleteDialog;
import org.rssowl.ui.internal.dialogs.LabelDialog;
import org.rssowl.ui.internal.dialogs.LabelDialog.DialogMode;
import org.rssowl.ui.internal.util.LayoutUtils;

/**
 * @author bpasero
 */
public class ManageLabelsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

  /** ID of the Page */
  public static final String ID = "org.rssowl.ui.ManageLabels";

  private LocalResourceManager fResources;
  private TreeViewer fViewer;

  /** Leave for reflection */
  public ManageLabelsPreferencePage() {
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /*
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  public void init(IWorkbench workbench) {
    noDefaultAndApplyButton();
  }

  /*
   * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createContents(Composite parent) {
    Composite container = createContainer(parent);

    /* Label */
    Label infoLabel = new Label(container, SWT.None);
    infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1));
    infoLabel.setText("Labels can be used to categorize and prioritize your news.");

    /* Label Viewer */
    createViewer(container);

    /* Button Box */
    createButtons(container);

    return container;
  }

  private void createButtons(Composite container) {
    Composite buttonBox = new Composite(container, SWT.None);
    buttonBox.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    buttonBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

    Button addButton = new Button(buttonBox, SWT.PUSH);
    addButton.setText("&Add...");
    setButtonLayoutData(addButton);
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onAdd();
      }
    });

    final Button editButton = new Button(buttonBox, SWT.PUSH);
    editButton.setText("&Edit...");
    editButton.setEnabled(!fViewer.getSelection().isEmpty());
    setButtonLayoutData(editButton);
    editButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onEdit();
      }
    });

    final Button deleteButton = new Button(buttonBox, SWT.PUSH);
    deleteButton.setText("&Delete");
    deleteButton.setEnabled(!fViewer.getSelection().isEmpty());
    setButtonLayoutData(deleteButton);
    deleteButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onDelete();
      }
    });

    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        editButton.setEnabled(!event.getSelection().isEmpty());
        deleteButton.setEnabled(!event.getSelection().isEmpty());
      }
    });
  }

  private void onAdd() {
    LabelDialog dialog = new LabelDialog(getShell(), DialogMode.ADD, null);
    if (dialog.open() == IDialogConstants.OK_ID) {
      String name = dialog.getName();
      RGB color = dialog.getColor();

      ILabel newLabel = Owl.getModelFactory().createLabel(null, name);
      newLabel.setColor(color.red + "," + color.green + "," + color.blue);
      DynamicDAO.save(newLabel);

      fViewer.refresh();
      fViewer.setSelection(new StructuredSelection(newLabel));
    }
    fViewer.getTree().setFocus();
  }

  private void onEdit() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (!selection.isEmpty()) {
      ILabel label = (ILabel) selection.getFirstElement();
      LabelDialog dialog = new LabelDialog(getShell(), DialogMode.EDIT, label);
      if (dialog.open() == IDialogConstants.OK_ID) {
        boolean changed = false;
        String name = dialog.getName();
        RGB color = dialog.getColor();

        if (!label.getName().equals(name)) {
          label.setName(name);
          if (label.getSearchMark() != null) {
        	  label.getSearchMark().setName(name);
          }
          changed = true;
        }

        String colorStr = color.red + "," + color.green + "," + color.blue;
        if (!label.getColor().equals(colorStr)) {
          label.setColor(colorStr);
          changed = true;
        }

        /* Save Label */
        if (changed) {
          Controller.getDefault().getSavedSearchService().forceQuickUpdate();
          DynamicDAO.save(label);
          fViewer.update(label, null);
        }
      }
    }
    fViewer.getTree().setFocus();
  }

  private void onDelete() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (!selection.isEmpty()) {
      ILabel label = (ILabel) selection.getFirstElement();

      String msg = "Are you sure you want to delete the Label '" + label.getName() + "'?";
      ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(getShell(), "Confirm Delete", "This action can not be undone", msg, null);
      if (dialog.open() == IDialogConstants.OK_ID) {

        /* Remove Label from any News containing it */
        Collection<INews> affectedNews = label.findNewsWithLabel();
        for (INews news : affectedNews) {
          news.removeLabel(label);
        }
        
        Controller.getDefault().getSavedSearchService().forceQuickUpdate();
        DynamicDAO.saveAll(affectedNews);

        /* Delete associated SearchMark of Label from DB */
        if (label.getSearchMark() != null) {
        	DynamicDAO.delete(label.getSearchMark());
        }
        
        /* Delete Label from DB */
        DynamicDAO.delete(label);
        fViewer.refresh();
      }
    }
    fViewer.getTree().setFocus();
  }

  private void createViewer(Composite container) {
    fViewer = new TreeViewer(container, SWT.FULL_SELECTION | SWT.BORDER | SWT.SINGLE);
    fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    /* Content Provider */
    fViewer.setContentProvider(new ITreeContentProvider() {
      public Object[] getElements(Object inputElement) {
        return DynamicDAO.loadAll(ILabel.class).toArray();
      }

      public Object[] getChildren(Object parentElement) {
        return null;
      }

      public Object getParent(Object element) {
        return null;
      }

      public boolean hasChildren(Object element) {
        return false;
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Label Provider */
    fViewer.setLabelProvider(new CellLabelProvider() {
      @Override
      public void update(ViewerCell cell) {
        ILabel label = (ILabel) cell.getElement();

        /* Text */
        cell.setText(label.getName());

        /* Color */
        cell.setForeground(OwlUI.getColor(fResources, label));
      }
    });

    /* Set dummy Input */
    fViewer.setInput(new Object());

    /* Edit on Doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        onEdit();
      }
    });
  }

  private Composite createContainer(Composite parent) {
    Composite composite = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(2, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
    composite.setFont(parent.getFont());
    return composite;
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    fResources.dispose();
  }
}