package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.dialogs.LabelDialog;
import org.rssowl.ui.internal.dialogs.LabelDialog.DialogMode;

/**
 * Edit a label.
 *
 */
public class EditLabelAction extends Action implements IWorkbenchWindowActionDelegate {
	private IStructuredSelection fSelection;

	  public EditLabelAction() {
		  
	  }

	  public void run() {
		  ILabel label = (ILabel) fSelection.getFirstElement();
	      LabelDialog dialog = new LabelDialog(Display.getCurrent().getActiveShell(), DialogMode.EDIT, label);
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
	        }
	      }
	  }
	
	  public void dispose() {
		  // TODO Auto-generated method stub
			
	  }
		
	  public void init(IWorkbenchWindow window) {
		  // TODO Auto-generated method stub
			
	  }
		
	  public void run(IAction action) {
		  run();
	  }
		
	  public void selectionChanged(IAction action, ISelection selection) {
		  fSelection = (IStructuredSelection)selection;
	  }
}