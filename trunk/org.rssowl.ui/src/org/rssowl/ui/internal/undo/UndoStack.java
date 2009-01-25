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

package org.rssowl.ui.internal.undo;

import org.eclipse.core.runtime.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link UndoStack} keeps a list of {@link IUndoOperation} and supports
 * undo/redo of these. The stack has a maximum capacity as defined by
 * <code>MAX_SIZE</code>.
 *
 * @author bpasero
 */
public class UndoStack {
  private static final int MAX_SIZE = 20;
  private static UndoStack singleton = new UndoStack();

  private final List<IUndoOperation> fOperations = new ArrayList<IUndoOperation>();
  private int fCurrentIndex = 0;

  private UndoStack() {}

  /**
   * @return the singleton instance of the {@link UndoStack}.
   */
  public static UndoStack getInstance() {
    if (singleton == null)
      singleton = new UndoStack();

    return singleton;
  }

  /**
   * Adds the given operation to the stack.
   *
   * @param operation the operation to add to the stack.
   */
  public void addOperation(IUndoOperation operation) {
    Assert.isNotNull(operation);

    /* Handle case where User executed Undo-Operation */
    if (fCurrentIndex < (fOperations.size() - 1)) {

      /* Remove all following Undo-Operations */
      for (int i = fCurrentIndex + 1; i < fOperations.size(); i++)
        fOperations.remove(i);
    }

    /* Add operation and constrain size */
    fOperations.add(operation);
    if (fOperations.size() > MAX_SIZE) {
      for (int i = 0; i < fOperations.size() - MAX_SIZE; i++)
        fOperations.remove(i);
    }

    /* Set pointer to last element */
    fCurrentIndex = fOperations.size() - 1;
  }

  /**
   * @return Returns the name for the next undo-operation or a generic one if
   * undo is not supported currently.
   */
  public String getUndoName() {
    if (!isUndoSupported())
      return "Undo";

    return "Undo '" + fOperations.get(fCurrentIndex).getName() + "'";
  }

  /**
   * @return Returns the name for the next redo-operation or a generic one if
   * redo is not supported currently.
   */
  public String getRedoName() {
    if (!isRedoSupported())
      return "Redo";

    return "Redo '" + fOperations.get(fCurrentIndex + 1).getName() + "'";
  }

  /**
   * @return Returns <code>true</code> if undo is supported and
   * <code>false</code> otherwise.
   */
  public boolean isUndoSupported() {
    return fCurrentIndex >= 0 && !fOperations.isEmpty();
  }

  /**
   * @return Returns <code>true</code> if redo is supported and
   * <code>false</code> otherwise.
   */
  public boolean isRedoSupported() {
    return fCurrentIndex < (fOperations.size() - 1);
  }

  /**
   * Navigates backwards in the list of operations if possible and undos the
   * operation.
   */
  public void undo() {
    if (!isUndoSupported())
      return;

    fOperations.get(fCurrentIndex).undo();
    fCurrentIndex--;
  }

  /**
   * Navigates forwards in the list of operations if possible and redos the
   * operation.
   */
  public void redo() {
    if (!isRedoSupported())
      return;

    fCurrentIndex++;
    fOperations.get(fCurrentIndex).redo();
  }
}