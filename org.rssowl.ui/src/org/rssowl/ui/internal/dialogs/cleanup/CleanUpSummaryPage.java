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

package org.rssowl.ui.internal.dialogs.cleanup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scrollable;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.ApplicationWorkbenchWindowAdvisor;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.JobRunner;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class CleanUpSummaryPage extends WizardPage {
  private CheckboxTreeViewer fViewer;
  private ResourceManager fResources;
  private Button fSelectAll;
  private Button fDeselectAll;

  /* Summary Label Provider */
  class SummaryLabelProvider extends LabelProvider implements IFontProvider, IColorProvider {
    private Color fGradientFgColor;
    private Color fGradientBgColor;
    private Color fGradientEndColor;
    private Color fGroupFgColor;

    SummaryLabelProvider() {
      fGradientFgColor = OwlUI.getColor(fResources, OwlUI.GROUP_GRADIENT_FG_COLOR);
      fGradientBgColor = OwlUI.getColor(fResources, OwlUI.GROUP_GRADIENT_BG_COLOR);
      fGradientEndColor = OwlUI.getColor(fResources, OwlUI.GROUP_GRADIENT_END_COLOR);
      fGroupFgColor = OwlUI.getColor(fResources, OwlUI.GROUP_FG_COLOR);
    }

    @Override
    public String getText(Object element) {
      if (element instanceof CleanUpGroup)
        return ((CleanUpGroup) element).getLabel();

      return ((CleanUpTask) element).getLabel();
    }

    @Override
    public Image getImage(Object element) {
      if (element instanceof CleanUpGroup)
        return OwlUI.getImage(fResources, OwlUI.GROUP);

      return OwlUI.getImage(fResources, ((CleanUpTask) element).getImage());
    }

    public Font getFont(Object element) {
      if (element instanceof CleanUpGroup)
        return OwlUI.getBold(JFaceResources.DEFAULT_FONT);

      return null;
    }

    public Color getBackground(Object element) {
      return null;
    }

    public Color getForeground(Object element) {
      if (element instanceof CleanUpGroup)
        return fGroupFgColor;

      return null;
    }

    void eraseGroup(Event event) {
      Scrollable scrollable = (Scrollable) event.widget;
      GC gc = event.gc;

      Rectangle area = scrollable.getClientArea();
      Rectangle rect = event.getBounds();

      /* Paint the selection beyond the end of last column */
      expandRegion(event, scrollable, gc, area);

      /* Draw Gradient Rectangle */
      Color oldForeground = gc.getForeground();
      Color oldBackground = gc.getBackground();

      /* Gradient */
      gc.setForeground(fGradientFgColor);
      gc.setBackground(fGradientBgColor);
      gc.fillGradientRectangle(0, rect.y, area.width, rect.height, true);

      /* Bottom Line */
      gc.setForeground(fGradientEndColor);
      gc.drawLine(0, rect.y + rect.height - 1, area.width, rect.y + rect.height - 1);

      gc.setForeground(oldForeground);
      gc.setBackground(oldBackground);

      /* Mark as Background being handled */
      event.detail &= ~SWT.BACKGROUND;
    }

    private void expandRegion(Event event, Scrollable scrollable, GC gc, Rectangle area) {
      int columnCount;
      if (scrollable instanceof Table)
        columnCount = ((Table) scrollable).getColumnCount();
      else
        columnCount = ((Tree) scrollable).getColumnCount();

      if (event.index == columnCount - 1 || columnCount == 0) {
        int width = area.x + area.width - event.x;
        if (width > 0) {
          Region region = new Region();
          gc.getClipping(region);
          region.add(event.x, event.y, width, event.height);
          gc.setClipping(region);
          region.dispose();
        }
      }
    }
  }

  /**
   * @param pageName
   */
  protected CleanUpSummaryPage(String pageName) {
    super(pageName, pageName, OwlUI.getImageDescriptor("icons/wizban/cleanup_wiz.gif"));
    setMessage("Please review and approve the suggested operations.");
    fResources = new LocalResourceManager(JFaceResources.getResources());
  }

  List<CleanUpTask> getTasks() {
    Object[] checkedElements = fViewer.getCheckedElements();
    List<CleanUpTask> tasks = new ArrayList<CleanUpTask>(checkedElements.length);

    for (Object checkedElement : checkedElements)
      if (checkedElement instanceof CleanUpTask)
        tasks.add((CleanUpTask) checkedElement);

    return tasks;
  }

  /*
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    /* Viewer to select particular Tasks */
    fViewer = new CheckboxTreeViewer(container, SWT.BORDER | SWT.FULL_SELECTION);
    fViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fViewer.getTree().setData(ApplicationWorkbenchWindowAdvisor.FOCUSLESS_SCROLL_HOOK, new Object());

    /* ContentProvider */
    fViewer.setContentProvider(new ITreeContentProvider() {
      public Object[] getElements(Object inputElement) {
        if (inputElement instanceof List<?>)
          return ((List<?>) inputElement).toArray();

        return new Object[0];
      }

      public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof CleanUpGroup) {
          CleanUpGroup group = (CleanUpGroup) parentElement;
          return group.getTasks().toArray();
        }

        return new Object[0];
      }

      public Object getParent(Object element) {
        if (element instanceof CleanUpTask)
          return ((CleanUpTask) element).getGroup();

        return null;
      }

      public boolean hasChildren(Object element) {
        return element instanceof CleanUpGroup;
      }

      public void dispose() {}

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
    });

    /* LabelProvider */
    final SummaryLabelProvider summaryLabelProvider = new SummaryLabelProvider();
    fViewer.setLabelProvider(summaryLabelProvider);

    /* Custom Owner Drawn Category */
    fViewer.getControl().addListener(SWT.EraseItem, new Listener() {
      public void handleEvent(Event event) {
        Object element = event.item.getData();
        if (element instanceof CleanUpGroup)
          summaryLabelProvider.eraseGroup(event);
      }
    });

    /* Listen on Doubleclick */
    fViewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        IStructuredSelection selection = (IStructuredSelection) event.getSelection();
        CleanUpGroup group = selection.getFirstElement() instanceof CleanUpGroup ? (CleanUpGroup) selection.getFirstElement() : null;

        /* Expand / Collapse Folder */
        if (group != null) {
          boolean expandedState = !fViewer.getExpandedState(group);
          fViewer.setExpandedState(group, expandedState);

          if (expandedState && fViewer.getChecked(group))
            setChildsChecked(group, true, true);
        }
      }
    });

    /* Update Checks on Selection */
    fViewer.getTree().addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (e.detail == SWT.CHECK) {
          TreeItem item = (TreeItem) e.item;

          if (item.getData() instanceof CleanUpGroup)
            setChildsChecked((CleanUpGroup) item.getData(), item.getChecked(), true);

          if (!item.getChecked() && item.getData() instanceof CleanUpTask)
            setParentsChecked((CleanUpTask) item.getData(), false);
        }
      }
    });

    /* Update Checks on Expand */
    fViewer.addTreeListener(new ITreeViewerListener() {
      public void treeExpanded(TreeExpansionEvent event) {
        boolean isChecked = fViewer.getChecked(event.getElement());
        if (isChecked)
          setChildsChecked((CleanUpGroup) event.getElement(), isChecked, false);
      }

      public void treeCollapsed(TreeExpansionEvent event) {}
    });

    /* Select All / Deselect All */
    Composite buttonContainer = new Composite(container, SWT.NONE);
    buttonContainer.setLayout(LayoutUtils.createGridLayout(2, 0, 0));
    buttonContainer.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

    fSelectAll = new Button(buttonContainer, SWT.PUSH);
    fSelectAll.setText("&Select All");
    setButtonLayoutData(fSelectAll);
    fSelectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        OwlUI.setAllChecked(fViewer.getTree(), true);
      }
    });

    fDeselectAll = new Button(buttonContainer, SWT.PUSH);
    fDeselectAll.setText("&Deselect All");
    setButtonLayoutData(fDeselectAll);
    fDeselectAll.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        OwlUI.setAllChecked(fViewer.getTree(), false);
      }
    });

    setControl(container);
  }

  private void setChildsChecked(CleanUpGroup cleanUpGroup, boolean checked, boolean onlyExpanded) {
    if (!onlyExpanded || fViewer.getExpandedState(cleanUpGroup)) {
      List<CleanUpTask> children = cleanUpGroup.getTasks();
      for (CleanUpTask child : children)
        fViewer.setChecked(child, checked);
    }
  }

  private void setParentsChecked(CleanUpTask cleanUpTask, boolean checked) {
    CleanUpGroup parent = cleanUpTask.getGroup();
    if (parent != null)
      fViewer.setChecked(parent, checked);
  }

  /*
   * @see org.eclipse.jface.dialogs.DialogPage#setVisible(boolean)
   */
  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);

    /* Generate the Summary */
    if (visible) {
      CleanUpOptionsPage cleanUpOptionsPage = (CleanUpOptionsPage) getPreviousPage();
      FeedSelectionPage feedSelectionPage = (FeedSelectionPage) cleanUpOptionsPage.getPreviousPage();

      final Set<IBookMark> selection = feedSelectionPage.getSelection();
      final CleanUpOperations operations = cleanUpOptionsPage.getOperations();

      IRunnableWithProgress runnable = new IRunnableWithProgress() {
        public void run(IProgressMonitor monitor) {
          monitor.beginTask("Please wait while generating the preview...", IProgressMonitor.UNKNOWN);
          onGenerateSummary(operations, selection);
        }
      };

      try {
        getContainer().run(true, false, runnable);
      } catch (InvocationTargetException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (InterruptedException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  private void onGenerateSummary(CleanUpOperations operations, Set<IBookMark> selection) {
    final CleanUpModel model = new CleanUpModel(operations, selection);
    model.generate();

    /* Show in Viewer */
    JobRunner.runInUIThread(fViewer.getTree(), new Runnable() {
      public void run() {
        fViewer.setInput(model.getTasks());
        fViewer.expandAll();
        OwlUI.setAllChecked(fViewer.getTree(), true);
      }
    });
  }

  /*
   * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
   */
  @Override
  public boolean isPageComplete() {
    return isCurrentPage();
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