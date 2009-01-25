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

package org.rssowl.ui.internal.dialogs;

import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IFolderChild;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IFolderDAO;
import org.rssowl.core.persist.event.FolderAdapter;
import org.rssowl.core.persist.event.FolderEvent;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.ReparentInfo;
import org.rssowl.ui.internal.Application;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.actions.DeleteTypesAction;
import org.rssowl.ui.internal.actions.EntityPropertyDialogAction;
import org.rssowl.ui.internal.actions.NewFolderAction;
import org.rssowl.ui.internal.util.LayoutUtils;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * The <code>ManageSetsDialog</code> allows to manage bookmark-sets. These are
 * root-leveld Folders containing other Folders and Marks.
 *
 * @author bpasero
 */
public class ManageSetsDialog extends TitleAreaDialog {

  /* Keep the visible instance saved */
  private static ManageSetsDialog fVisibleInstance;

  private LocalResourceManager fResources;
  private TableViewer fViewer;
  private Label fStatusLabel;
  private Button fEditButton;
  private Button fDeleteButton;
  private IFolder fSelectedSet;
  private FolderAdapter fFolderListener;

  /**
   * @param parentShell
   * @param selectedSet
   */
  public ManageSetsDialog(Shell parentShell, IFolder selectedSet) {
    super(parentShell);
    fSelectedSet = selectedSet;
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  /**
   * @return Returns an instance of <code>ManageSetsDialog</code> or
   * <code>NULL</code> in case no instance is currently open.
   */
  public static ManageSetsDialog getVisibleInstance() {
    return fVisibleInstance;
  }

  /*
   * @see org.eclipse.jface.window.Window#open()
   */
  @Override
  public int open() {
    fVisibleInstance = this;
    registerListeners();
    return super.open();
  }

  private void registerListeners() {
    fFolderListener = new FolderAdapter() {
      @Override
      public void entitiesAdded(Set<FolderEvent> events) {
        for (FolderEvent folderEvent : events) {
          IFolder folder = folderEvent.getEntity();
          if (folder.getParent() == null) {
            fViewer.add(folder);
            fViewer.setSelection(new StructuredSelection(folder));
          }
        }
      }
    };

    DynamicDAO.addEntityListener(IFolder.class, fFolderListener);
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#close()
   */
  @Override
  public boolean close() {
    unregisterListeners();
    fVisibleInstance = null;
    fResources.dispose();
    return super.close();
  }

  private void unregisterListeners() {
    DynamicDAO.removeEntityListener(IFolder.class, fFolderListener);
  }

  /*
   * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
   */
  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText("Manage Bookmark-Sets");
  }

  /*
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected Control createDialogArea(Composite parent) {

    /* Title Image */
    setTitleImage(OwlUI.getImage(fResources, "icons/wizban/bkmrk_set_title.gif"));

    /* Title Message */
    showInfo();

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Composite to hold all components */
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(LayoutUtils.createGridLayout(2, 5, 10));
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

    /* TableViewer to display Bookmark-Sets */
    fViewer = new TableViewer(composite, SWT.BORDER);
    fViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    ((GridData) fViewer.getTable().getLayoutData()).heightHint = fViewer.getTable().getItemHeight() * 7;

    /* Drag and Drop */
    initDragAndDrop();

    /* ContentProvider returns Root-Folders */
    fViewer.setContentProvider(new IStructuredContentProvider() {
      public Object[] getElements(Object inputElement) {
        return DynamicDAO.getDAO(IFolderDAO.class).loadRoots().toArray();
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* Simple LabelProvider */
    fViewer.setLabelProvider(new LabelProvider() {

      @Override
      public String getText(Object element) {
        return ((IFolder) element).getName();
      }

      @Override
      public Image getImage(Object element) {
        return OwlUI.getImage(fResources, OwlUI.BOOKMARK_SET);
      }
    });

    /* Sort by ID to show latest Set at bottom */
    fViewer.setComparator(new ViewerComparator() {
      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        IFolder folder1 = (IFolder) e1;
        IFolder folder2 = (IFolder) e2;

        return folder1.getId().compareTo(folder2.getId());
      }
    });

    /* Set input (ignored by ContentProvider anyways) */
    fViewer.setInput(this);

    /* Container for the Buttons to Manage Sets */
    Composite buttonContainer = new Composite(composite, SWT.None);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(1, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

    /* Adds a new Bookmark Set */
    Button addButton = new Button(buttonContainer, SWT.PUSH);
    addButton.setText("&New...");
    setButtonLayoutData(addButton);
    addButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onAdd();
      }
    });

    /* Edits a selected Bookmark Set */
    fEditButton = new Button(buttonContainer, SWT.PUSH);
    fEditButton.setText("&Edit...");
    setButtonLayoutData(fEditButton);
    fEditButton.setEnabled(false);
    fEditButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onEdit();
      }
    });

    /* Deletes the selected Bookmark Set */
    fDeleteButton = new Button(buttonContainer, SWT.PUSH);
    fDeleteButton.setText("&Delete...");
    setButtonLayoutData(fDeleteButton);
    ((GridData) fDeleteButton.getLayoutData()).verticalAlignment = SWT.END;
    ((GridData) fDeleteButton.getLayoutData()).grabExcessVerticalSpace = true;
    fDeleteButton.setEnabled(false);
    fDeleteButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onDelete();
      }
    });

    /* Container for the status-message */
    Composite statusContainer = new Composite(composite, SWT.None);
    statusContainer.setLayout(LayoutUtils.createGridLayout(1, 5, 0));
    statusContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, false, false, 2, 1));

    fStatusLabel = new Label(statusContainer, SWT.NONE);
    fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    /* Update Status Label when selection changes */
    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        onSelectionChange();
      }
    });

    /* Pre-Select the current visible Set */
    Collection<IFolder> rootFolders = DynamicDAO.getDAO(IFolderDAO.class).loadRoots();
    for (IFolder rootFolder : rootFolders) {
      if (rootFolder.equals(fSelectedSet)) {
        fViewer.setSelection(new StructuredSelection(rootFolder));
        break;
      }
    }

    /* Separator */
    new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    return composite;
  }

  private void initDragAndDrop() {
    int ops = DND.DROP_COPY | DND.DROP_MOVE;
    Transfer[] transfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };

    ViewerDropAdapter dropAdapter = new ViewerDropAdapter(fViewer) {
      @Override
      public boolean validateDrop(final Object target, int operation, TransferData transferType) {
        if (LocalSelectionTransfer.getTransfer().isSupportedType(transferType)) {
          final boolean[] result = new boolean[] { false };
          SafeRunner.run(new LoggingSafeRunnable() {
            public void run() throws Exception {
              ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
              if (selection instanceof IStructuredSelection) {
                List<?> draggedObjects = ((IStructuredSelection) selection).toList();
                result[0] = isValidDrop(draggedObjects, target);
              }
            }
          });

          return result[0];
        }

        return false;
      }

      @Override
      public boolean performDrop(final Object data) {
        if (data instanceof IStructuredSelection) {
          SafeRunner.run(new LoggingSafeRunnable() {
            public void run() throws Exception {
              IStructuredSelection selection = (IStructuredSelection) data;
              List<?> draggedObjects = selection.toList();
              perfromDrop(draggedObjects, getCurrentTarget());
            }
          });

          return true;
        }

        return false;
      }
    };

    dropAdapter.setFeedbackEnabled(false);
    fViewer.addDropSupport(ops, transfers, dropAdapter);
  }

  private boolean isValidDrop(List<?> draggedObjects, Object dropTarget) {

    /* Require Folder as Target */
    if (!(dropTarget instanceof IFolder))
      return false;

    /* Check validity for each dragged Object */
    IFolder dropFolder = (IFolder) dropTarget;
    for (Object draggedObject : draggedObjects) {

      /* Dragged Folder */
      if (draggedObject instanceof IFolder) {
        IFolder draggedFolder = (IFolder) draggedObject;
        if (ModelUtils.hasChildRelation(dropFolder, draggedFolder))
          return false;
      }

      /* Dragged Mark */
      else if (draggedObject instanceof IMark) {
        IMark draggedMark = (IMark) draggedObject;
        if (ModelUtils.hasChildRelation(dropFolder, draggedMark))
          return false;
      }
    }

    return true;
  }

  private void perfromDrop(List<?> draggedObjects, Object dropTarget) {

    /* Require a Folder as drop target */
    if (!(dropTarget instanceof IFolder) || draggedObjects.isEmpty())
      return;

    IFolder dropFolder = (IFolder) dropTarget;

    List<ReparentInfo<IFolderChild, IFolder>> reparenting = new ArrayList<ReparentInfo<IFolderChild, IFolder>>(draggedObjects.size());

    /* For each dragged Object */
    for (Object object : draggedObjects) {
      if (object instanceof IFolder || object instanceof IMark) {
        IFolderChild draggedFolderChild = (IFolderChild) object;
        reparenting.add(ReparentInfo.create(draggedFolderChild, dropFolder, null, null));
      }
    }

    /* Perform reparenting */
    Owl.getPersistenceService().getDAOService().getFolderDAO().reparent(reparenting);
    fViewer.setSelection(fViewer.getSelection());
  }

  private void onAdd() {
    showInfo();
    NewFolderAction newFolderAction = new NewFolderAction(getShell(), null, null);
    newFolderAction.setRootMode(true);
    newFolderAction.run(null);
    fViewer.refresh();

    /* Select and Focus the added Set */
    Table table = fViewer.getTable();
    Object lastItem = table.getItem(table.getItemCount() - 1).getData();
    fViewer.setSelection(new StructuredSelection(lastItem));
    table.setFocus();
  }

  private void onEdit() {
    showInfo();

    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (!selection.isEmpty()) {
      new EntityPropertyDialogAction(this, fViewer).run();
      fViewer.refresh();
    }
  }

  private void onDelete() {
    showInfo();

    /* Require at least 1 Set to remain undeleted */
    if (fViewer.getTable().getItemCount() == 1) {
      setErrorMessage("It is not possible to delete the last Bookmark-Set. Please create a new Set first.");
      return;
    }

    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (!selection.isEmpty()) {
      DeleteTypesAction deleteAction = new DeleteTypesAction(getShell(), selection);
      deleteAction.run();

      if (deleteAction.isConfirmed())
        fViewer.remove(selection.getFirstElement());
    }
  }

  private void onSelectionChange() {
    updateStatusLabel();

    ISelection selection = fViewer.getSelection();
    fEditButton.setEnabled(!selection.isEmpty());
    fDeleteButton.setEnabled(!selection.isEmpty());
  }

  /*
   * @see org.eclipse.jface.window.Window#getShellStyle()
   */
  @Override
  protected int getShellStyle() {
    int style = SWT.TITLE | SWT.BORDER | getDefaultOrientation();

    /* Follow Apple's Human Interface Guidelines for Application Modal Dialogs */
    if (!Application.IS_MAC)
      style |= SWT.CLOSE;

    return style;
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
   */
  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }

  /*
   * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
   */
  @Override
  protected void initializeBounds() {
    super.initializeBounds();

    Shell shell = getShell();

    /* Minimum Size */
    int minWidth = convertHorizontalDLUsToPixels(OwlUI.MIN_DIALOG_WIDTH_DLU);
    int minHeight = shell.computeSize(minWidth, SWT.DEFAULT).y;

    /* Required Size */
    Point requiredSize = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);

    shell.setSize(Math.max(minWidth, requiredSize.x), Math.max(minHeight, requiredSize.y));
    LayoutUtils.positionShell(shell, false);
  }

  private void showInfo() {
    setErrorMessage(null);
    setMessage("Please select a Bookmark-Set to manage.", IMessageProvider.INFORMATION);
  }

  private void updateStatusLabel() {
    IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
    if (selection.isEmpty())
      fStatusLabel.setText("");
    else {
      IFolder bookmarkSet = (IFolder) selection.getFirstElement();
      int counter[] = new int[4];

      count(bookmarkSet, counter);

      StringBuilder str = new StringBuilder("Set contains ");
      if (counter[0] > 0)
        str.append(counter[0] == 1 ? "1 folder, " : (counter[0] + " folders, "));
      if (counter[1] > 0)
        str.append(counter[1] == 1 ? "1 bookmark, " : (counter[1] + " bookmarks, "));
      if (counter[2] > 0)
        str.append(counter[2] == 1 ? "1 saved search, " : (counter[2] + " saved searches, "));
      if (counter[3] > 0)
        str.append(counter[3] == 1 ? "1 news bin, " : (counter[3] + " news bins, "));

      /* Set is Empty */
      if (counter[0] == 0 && counter[1] == 0 && counter[2] == 0 && counter[3] == 0)
        str = new StringBuilder("Set is empty.");
      else
        str.delete(str.length() - 2, str.length()).append(".");

      fStatusLabel.setText(str.toString());
    }
  }

  private void count(IFolder folder, int[] counter) {

    /* Count Marks */
    List<IMark> marks = folder.getMarks();
    for (IMark mark : marks) {
      if (mark instanceof IBookMark)
        counter[1]++;
      else if (mark instanceof ISearchMark)
        counter[2]++;
      else if (mark instanceof INewsBin)
        counter[3]++;
    }

    /* Count in Sub-Folders */
    List<IFolder> childs = folder.getFolders();
    counter[0] += childs.size();
    for (IFolder child : childs)
      count(child, counter);
  }
}