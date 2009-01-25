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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.rssowl.core.persist.INews;
import org.rssowl.ui.internal.util.LayoutUtils;

import java.util.EnumSet;

/**
 * The <code>StateConditionControl</code> is a <code>Composite</code>
 * providing the UI to define State-Conditions for a Search.
 * <p>
 * TODO This class is currently only working on INews.
 * </p>
 * <p>
 * TODO Enable support for *deleted* state again.
 * </p>
 *
 * @author bpasero
 */
public class StateConditionControl extends Composite {
  private Button fNewState;
  private Button fUnreadState;
  private Button fUpdatedState;
  private Button fReadState;

  /**
   * @param parent The parent Composite.
   * @param style The Style as defined by SWT constants.
   */
  StateConditionControl(Composite parent, int style) {
    super(parent, style);

    initComponents();
  }

  EnumSet<INews.State> getSelection() {
    EnumSet<INews.State> set = null;

    if (fNewState.getSelection()) {
      set = EnumSet.of(INews.State.NEW);
    }

    if (fUnreadState.getSelection()) {
      if (set == null)
        set = EnumSet.of(INews.State.UNREAD);
      else
        set.add(INews.State.UNREAD);
    }

    if (fUpdatedState.getSelection()) {
      if (set == null)
        set = EnumSet.of(INews.State.UPDATED);
      else
        set.add(INews.State.UPDATED);
    }

    if (fReadState.getSelection()) {
      if (set == null)
        set = EnumSet.of(INews.State.READ);
      else
        set.add(INews.State.READ);
    }

    return set;
  }

  /**
   * Selects the given States in the Control. Will deselect all states if the
   * field is <code>NULL</code>.
   *
   * @param selectedStates the news states to select in the Control or
   * <code>NULL</code> if none.
   */
  void select(EnumSet<INews.State> selectedStates) {
    fNewState.setSelection(selectedStates != null && selectedStates.contains(INews.State.NEW));
    fUnreadState.setSelection(selectedStates != null && selectedStates.contains(INews.State.UNREAD));
    fUpdatedState.setSelection(selectedStates != null && selectedStates.contains(INews.State.UPDATED));
    fReadState.setSelection(selectedStates != null && selectedStates.contains(INews.State.READ));
  }

  private void initComponents() {

    /* Apply Gridlayout */
    setLayout(LayoutUtils.createGridLayout(4, 0, 0));

    /* State: New */
    fNewState = new Button(this, SWT.CHECK);
    fNewState.setText("New");
    fNewState.setToolTipText("News that have not yet been seen");
    fNewState.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    /* State: Unread */
    fUnreadState = new Button(this, SWT.CHECK);
    fUnreadState.setText("Unread");
    fUnreadState.setToolTipText("News that have been seen but not read");
    fUnreadState.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    /* State: Updated */
    fUpdatedState = new Button(this, SWT.CHECK);
    fUpdatedState.setText("Updated");
    fUpdatedState.setToolTipText("News with updated content");
    fUpdatedState.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    /* State: Read */
    fReadState = new Button(this, SWT.CHECK);
    fReadState.setText("Read");
    fReadState.setToolTipText("News that have been read");
    fReadState.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, true));

    /* Selection Listener to issue modify events */
    SelectionListener selectionListener = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        notifyListeners(SWT.Modify, new Event());
      }
    };
    fNewState.addSelectionListener(selectionListener);
    fUnreadState.addSelectionListener(selectionListener);
    fUpdatedState.addSelectionListener(selectionListener);
    fReadState.addSelectionListener(selectionListener);
  }
}