package org.rssowl.ui.internal.views.label;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.rssowl.core.Owl;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.core.persist.dao.IPreferenceDAO;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

/*
 * The content provider class is responsible for
 * providing objects to the view. It can wrap
 * existing objects in adapters or simply return
 * objects as-is. These objects may be sensitive
 * to the current input of the view, or ignore
 * it and always show the same content 
 * (like Task List, for example).
 */

public class LabelContentProvider implements IStructuredContentProvider {
	private boolean createNewFolder = false;
	
	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}

	public Object[] getElements(Object parent) {
		Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);
		for (ILabel label : labels) {
			if (label.getSearchMark() == null) {
				label.setSearchMark(createSearchMark(label.getName()));
				DynamicDAO.save(label);
			}
		}
		return labels.toArray();
	}
	
	public void setCreateNewFolder() {
		this.createNewFolder = true;
	}
	
	private ISearchMark createSearchMark(String labelName) {
		IModelFactory factory = Owl.getModelFactory();
		
  	  	ISearchField field = factory.createSearchField(INews.LABEL, INews.class.getName());
  	  	ISearchCondition condition = factory.createSearchCondition(field, SearchSpecifier.IS, labelName);
  	  	
  	  	IFolder root = null;
  	  	if (!createNewFolder) {
  	  		String selectedBookMarkSetPref = BookMarkExplorer.getSelectedBookMarkSetPref(OwlUI.getWindow());
  	  		Long selectedRootFolderID = DynamicDAO.getDAO(IPreferenceDAO.class).load(selectedBookMarkSetPref).getLong();
  	  		root = new FolderReference(selectedRootFolderID).resolve();
  	  	} else {
  	  		root = Owl.getModelFactory().createFolder(null, null, "LabelRoot");
  	  	}
	  	ISearchMark searchMark = factory.createSearchMark(null,root, labelName);
	  	searchMark.addSearchCondition(condition);
	  	return searchMark;
	}
}
