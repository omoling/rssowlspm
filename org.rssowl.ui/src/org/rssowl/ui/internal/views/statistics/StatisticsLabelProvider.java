package org.rssowl.ui.internal.views.statistics;

import java.text.DateFormat;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.ui.internal.OwlUI;

public class StatisticsLabelProvider extends LabelProvider implements
ITableLabelProvider {
	
	/** Resource Manager to use */
	protected LocalResourceManager fResources;
	
	public StatisticsLabelProvider(){
		fResources = new LocalResourceManager(JFaceResources.getResources());
	}
	
	//@Override
	public Image getColumnImage(Object element, int columnIndex) {

		if (columnIndex == StatisticsViewer.TITLE_COL) {
			IBookMark bookMark = (IBookMark) element;
			
			if (bookMark != null) {
				ImageDescriptor favicon = OwlUI.getFavicon(bookMark);
				return OwlUI.getImage(fResources, favicon != null ? favicon
						: OwlUI.BOOKMARK);
			} else {
				return OwlUI.getImage(fResources, OwlUI.BOOKMARK);
			}
		} else
			return null;
	}

	//@Override
	public String getColumnText(Object element, int columnIndex) {
		IBookMark bookMark = (IBookMark) element;
		
		if (columnIndex == StatisticsViewer.TITLE_COL)
			return " " + bookMark.getName();
		else if (columnIndex == StatisticsViewer.POPULARITY_COL)
			return String.valueOf(bookMark.getPopularity());
		else if (columnIndex == StatisticsViewer.READPERCENTAGE_COL)
			return String.valueOf(bookMark.getReadPercentage());
		else if(columnIndex == StatisticsViewer.LASTACCESS_COL){
			if (bookMark.getLastVisitDate() != null)
				return DateFormat.getDateTimeInstance().format(bookMark.getLastVisitDate());
			else
				return "[never visited]";
		}
		else if(columnIndex == StatisticsViewer.LASTUPDATE_COL){
			if (bookMark.getMostRecentNewsDate() != null)
				return DateFormat.getDateTimeInstance().format(bookMark.getMostRecentNewsDate());
			else
				return "[never updated]";	
		}
		else
			throw new RuntimeException("There is no data defined for column index " + columnIndex);
	}
}