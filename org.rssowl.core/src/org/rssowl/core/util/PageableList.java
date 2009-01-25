package org.rssowl.core.util;

import java.util.ArrayList;
import java.util.List;

public class PageableList<T> extends ArrayList<T> {
	private static final long serialVersionUID = 1L;

	public static int ITEMS_PER_PAGE = 10;
	private int currentIndex = 0;

	/**
	 * Default constructor
	 */
	public PageableList(){
	}
	
	public PageableList(List<T> list){
		super(list);
	}
	
	public List<T> getCurrentPage() {
		int nextIndex = currentIndex + ITEMS_PER_PAGE;
		
		//this check is needed when the page is not full
		//i.e. we have 25 items, so the last page will contain 5 items. In this case the paging index would
		//go from 20 to 30 (with page size 10). This would produce an IndexOutOfBounds exception and so this check
		//will guarantee that in such a case we just go to the end of the page.
		if (nextIndex >= this.size()) 
			nextIndex = this.size();
		
		//this check is needed when we are on the last page that previously contained only one item, but in 
		//the meanwhile another (or this item) has been deleted and therefore the last page would have 
		//become empty.
		if (currentIndex == nextIndex) {
			if (this.size() > 0) {
				currentIndex = currentIndex - ITEMS_PER_PAGE;
			} else {
				currentIndex = 0;
			}
		}
		return this.subList(currentIndex, nextIndex);
	}
	
	/**
	 * Resets the indices to the first page
	 */
	public void resetIndices(){
		currentIndex = 0;
	}
	
	/**
	 * Updates the current index s.t. it points to the beginning
	 * of the next page
	 */
	public void next(){
		int newIndex = currentIndex + ITEMS_PER_PAGE;
		if(newIndex < this.size()){
			currentIndex = newIndex;
		}
	}
	
	/**
	 * Updates the current index s.t. it points to the beginning
	 * of the previous page
	 */
	public void previous(){
		int newIndex = currentIndex - ITEMS_PER_PAGE;
		if(newIndex >= 0){
			currentIndex = newIndex;
		}
	}

	/**
	 * Returns the current position of the index
	 * @return
	 */
	public int getCurrentIndex() {
		return this.currentIndex;
	}
	
	public void setCurrentIndex(int value){
		this.currentIndex = value;
	}
}