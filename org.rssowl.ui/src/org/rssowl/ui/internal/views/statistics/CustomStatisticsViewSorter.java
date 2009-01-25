package org.rssowl.ui.internal.views.statistics;

import java.util.Comparator;
import java.util.Date;
import org.rssowl.core.persist.IBookMark;

public class CustomStatisticsViewSorter<T extends IBookMark> implements Comparator<T> {

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
	
	public void setType(CustomStatisticsViewSorter.Type type) {
		fType = type;
	}

	public CustomStatisticsViewSorter.Type getType() {
		return fType;
	}  
	
	/**
	 * Compares two IBookMark objects
	 */
	public int compare(IBookMark bookMark1, IBookMark bookMark2) {
		/* Sort by Popularity descending */
	    if (fType == Type.SORT_BY_POPULARITY) {
	      int p1 = bookMark1.getPopularity();
	      int p2 = bookMark2.getPopularity();

	      if (p1 != p2)
	        return p1 > p2 ? -1 : 1;
	    }
	    
	    /* Sort by Title ascending */
	    else if(fType == Type.SORT_BY_TITLE){
	    	int result = bookMark1.getName().compareToIgnoreCase(bookMark2.getName());
	    	if(result < 0)
	    		return -1;
	    	else if(result == 0)
	    		return 0;
	    	else
	    		return 1;
	    } 
	    
	    /* Sort by Percentage descending */
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
