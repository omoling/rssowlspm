package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.dialogs.LabelDialog;
import org.rssowl.ui.internal.dialogs.LabelDialog.DialogMode;

/**
 * Add a label.
 *
 */
public class AddLabelAction implements IViewActionDelegate{

	
	public void init(IViewPart view) {
	}

	public void run(IAction action) {
		LabelDialog dialog = new LabelDialog(Display.getCurrent().getActiveShell(), DialogMode.ADD, null);
		if (dialog.open() == IDialogConstants.OK_ID) {
	      String name = dialog.getName();
	      RGB color = dialog.getColor();

	      ILabel newLabel = Owl.getModelFactory().createLabel(null, name);
	      newLabel.setColor(color.red + "," + color.green + "," + color.blue);
	      DynamicDAO.save(newLabel);
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}	
}