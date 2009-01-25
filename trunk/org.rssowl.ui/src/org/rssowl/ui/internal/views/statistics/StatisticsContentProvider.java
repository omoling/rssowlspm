package org.rssowl.ui.internal.views.statistics;

import java.util.Collection;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.rssowl.core.persist.IBookMark;

public class StatisticsContentProvider implements IStructuredContentProvider {

	//@Override
	public Object[] getElements(Object inputElement) {
		if(inputElement != null)
			return ((Collection<IBookMark>)inputElement).toArray();
		else
			return null;
	}

	//@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// TODO Auto-generated method stub
		
	}



}
