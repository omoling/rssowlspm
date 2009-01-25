package org.rssowl.ui.internal.views.statistics;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class StatisticsViewer extends TableViewer {
	private StatisticsExplorer fStatisticsExplorer = null;
	
	static final int TITLE_COL = 0;
	static final int POPULARITY_COL = 1;
	static final int READPERCENTAGE_COL = 2;
	static final int LASTACCESS_COL = 3;
	static final int LASTUPDATE_COL = 4;
	
	public StatisticsViewer(StatisticsExplorer explorer, Composite parent, int style) {
		super(parent, style);
		this.fStatisticsExplorer = explorer;
	}
	
	/**
	 * Creates the columns for the TableViewer
	 */
	public void createColumns(){
		Table table = this.getTable();

		String[] titles = {"Feed", "Popularity", "Read %", "Last visit", "Last update"};
		int[] bounds = {200, 70, 70, 160, 160};
		
		for (int i = 0; i < titles.length; i++) {
			TableViewerColumn column = new TableViewerColumn(this, SWT.NONE);
			column.getColumn().setText(titles[i]);
			column.getColumn().setWidth(bounds[i]);
			column.getColumn().setResizable(true);
		}
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
	}
}
