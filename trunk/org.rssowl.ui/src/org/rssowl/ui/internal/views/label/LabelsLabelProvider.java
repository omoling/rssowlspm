package org.rssowl.ui.internal.views.label;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.rssowl.core.persist.ILabel;
import org.rssowl.ui.internal.OwlUI;

public class LabelsLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	/* Resource Manager */
	private LocalResourceManager fResources;

	public LabelsLabelProvider() {
		fResources = new LocalResourceManager(JFaceResources.getResources());
	}

	public String getColumnText(Object obj, int index) {
		ILabel label = (ILabel) obj;
		int numStickyNews = label.getStickyNewsCount();
		if (numStickyNews > 0){
			return label.getName() + " (" + label.getStickyNewsCount() + ")";
		} else {
			return label.getName();
		}
	}

	public Image getColumnImage(Object obj, int index) {
		return getImage(obj);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object obj) {
		// if the object is of type Label (which should always be the case
		// assign the corresponding image
		if (obj instanceof ILabel) {
			return OwlUI.getImage(fResources, OwlUI.LABEL);
		}

		// otherwise return the default Owl error icon
		return OwlUI.getImage(fResources, OwlUI.ERROR);
	}
}
