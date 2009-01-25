package org.rssowl.ui.internal.views.statistics;

import java.util.Date;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.rssowl.core.persist.IBookMark;

public class StatisticsViewSorter extends ViewerSorter{

	/* Return this if the sort should be skipped for two elements */
	private static final int SKIP_SORT = 0;
	
	/** Sort Type */
	public enum Type {

		SORT_BY_TITLE,
		
		/** Sort by popularity (descending) */
		SORT_BY_POPULARITY,
		
		/** Sort by read percentage (descending) */
		SORT_BY_PERCENTAGE,
		
		/** Sort by last-access date (descending) */
		SORT_BY_LASTACCESS,
		
		/** Sort by last-update date (descending) */
		SORT_BY_LASTUPDATE
	
	}
	
	private Type fType = Type.SORT_BY_POPULARITY;

	public void setType(StatisticsViewSorter.Type type) {
		fType = type;
	}

	public StatisticsViewSorter.Type getType() {
		return fType;
	}  
	  
	@Override
	public int compare(Viewer viewer, Object e1, Object e2){

		/* Oder by name (default sorting) */
	    if (e1 instanceof IBookMark && e2 instanceof IBookMark){
	      return compareBookMarks((IBookMark) e1, (IBookMark) e2);
	    }
	    
	    return super.compare(viewer, e1, e2);
	}

	/**
	 * Compares the two bookmarks
	 * @param bookMark1
	 * @param bookMark2
	 * @return
	 */
	private int compareBookMarks(IBookMark bookMark1, IBookMark bookMark2) {
		
		/* Sort by Popularity */
	    if (fType == Type.SORT_BY_POPULARITY) {
	      int p1 = bookMark1.getPopularity();
	      int p2 = bookMark2.getPopularity();

	      if (p1 != p2)
	        return p1 > p2 ? -1 : 1;
	    }
	    
	    /* Sort by Title */
	    else if(fType == Type.SORT_BY_TITLE){
	    	return bookMark1.getName().compareToIgnoreCase(bookMark2.getName());
	    } 
	    
	    /* Sort by Percentage */
	    else if(fType == Type.SORT_BY_PERCENTAGE) {
	    	double p1 = bookMark1.getReadPercentage();
	    	double p2 = bookMark2.getReadPercentage();
	    	
	    	if (p1 != p2) 
	    		return p1 > p2 ? -1 : 1;
	    }
	    
	    /* Sort by Last Access Date */
	    else if (fType == Type.SORT_BY_LASTACCESS) {
	    	Date d1 = bookMark1.getLastVisitDate();
	    	Date d2 = bookMark2.getLastVisitDate();
	    	if (d1 != null || d2 != null) {
	    		return compareDates(d1, d2);
	    	}
	    }
	    
	    /* Sort by Last Update Date */
	    else if (fType == Type.SORT_BY_LASTUPDATE) {
	    	Date d1 = bookMark1.getMostRecentNewsDate();
	    	Date d2 = bookMark2.getMostRecentNewsDate();
	    	if (d1 != null || d2 != null) {
	    		return compareDates(d1, d2);
	    	}
	    }

	    return SKIP_SORT;
	}
	
	/**
	 * Compares the two dates
	 * 
	 * @param d1
	 * @param d2
	 * @return
	 */
	private int compareDates(Date d1, Date d2) {
		if (d1 != null && d2 != null)
			return d2.compareTo(d1);
		else if (d1 == null)
			return 1;
		else
			return -1;
	}
	
}
