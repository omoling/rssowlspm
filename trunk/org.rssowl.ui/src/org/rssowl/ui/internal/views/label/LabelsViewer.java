package org.rssowl.ui.internal.views.label;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;

public class LabelsViewer extends TableViewer {
	private LabelsExplorer fLabelsExplorer = null;

	public LabelsViewer(LabelsExplorer explorer,Composite parent, int style) {
		super(parent, style);
		this.fLabelsExplorer = explorer;
	}

}
