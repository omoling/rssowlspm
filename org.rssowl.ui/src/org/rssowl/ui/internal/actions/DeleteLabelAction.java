package org.rssowl.ui.internal.actions;

import java.util.Collection;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.Controller;
import org.rssowl.ui.internal.dialogs.ConfirmDeleteDialog;

/**
 * Delete a label.
 *
 */
public class DeleteLabelAction extends Action implements IWorkbenchWindowActionDelegate {
	private IStructuredSelection fSelection;
	
	public DeleteLabelAction() {  
	}
	
	  
	public void run() {
		ILabel label = (ILabel) fSelection.getFirstElement();

		String msg = "Are you sure you want to delete the Label '" + label.getName() + "'?";
		ConfirmDeleteDialog dialog = new ConfirmDeleteDialog(Display.getCurrent().getActiveShell(), "Confirm Delete", "This action can not be undone", msg, null);
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
  		fSelection = (IStructuredSelection) selection;
  	}
}