package org.rssowl.ui.internal.views.label;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.rssowl.core.persist.ILabel;

public class LabelsViewSorter extends ViewerSorter{

	/* Return this is the sort should be skipped for two elements */
	private static final int SKIP_SORT = 0;
	
	/** Sort Type */
	public enum Type {

		/** Apply Default Sorting: by name */
		DEFAULT_SORTING,

		//TODO: may be used later to sort by # of unread items...?
		//** Sort by Unread News */
		//SORT_BY_UNREAD,
	}
	
	private Type fType = Type.DEFAULT_SORTING;

	public void setType(LabelsViewSorter.Type type) {
		fType = type;
	}

	LabelsViewSorter.Type getType() {
		return fType;
	}  
	  
	@Override
	public int compare(Viewer viewer, Object e1, Object e2){

		/* Oder by name (default sorting) */
	    if (fType == Type.DEFAULT_SORTING && e1 instanceof ILabel && e2 instanceof ILabel)
	      return compareLabels((ILabel) e1, (ILabel) e2);

	    return super.compare(viewer, e1, e2);
	}
	
	private int compareLabels(ILabel label1, ILabel label2) {

	    /* Sort by Name */
	    if (fType == Type.DEFAULT_SORTING)
	      return label1.getName().toLowerCase().compareTo(label2.getName().toLowerCase());

	    return SKIP_SORT;
	}
	
}
