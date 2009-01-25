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

package org.rssowl.ui.internal.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.persist.INews;
import org.rssowl.ui.internal.util.BrowserUtils;

import java.util.List;

/**
 * @author bpasero
 */
public class OpenInExternalBrowserAction extends Action implements
		IWorkbenchWindowActionDelegate {
	private IStructuredSelection fSelection;

	public OpenInExternalBrowserAction() {
	    this(StructuredSelection.EMPTY);
	  }
	
	/**
	 * @param selection
	 */
	public OpenInExternalBrowserAction(IStructuredSelection selection) {
		fSelection = selection;

		setText("Open in External Browser");
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		List<?> selection = fSelection.toList();
		for (Object object : selection) {
			if (object instanceof INews) {
				INews news = (INews) object;
				if (news.getLinkAsText() != null)
					BrowserUtils.openLinkExternal(news.getLinkAsText());
			}
		}
	}

	//@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	//@Override
	public void init(IWorkbenchWindow window) {
		// TODO Auto-generated method stub

	}

	//@Override
	public void run(IAction action) {
		run();
	}

	//@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof IStructuredSelection)
			fSelection = (IStructuredSelection) selection;
	}
}