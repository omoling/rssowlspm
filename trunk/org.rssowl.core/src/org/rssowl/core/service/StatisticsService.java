package org.rssowl.core.service;

import java.util.Collection;
import java.util.EnumSet;

import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.dao.DynamicDAO;

public class StatisticsService {

	public StatisticsService() {
	}

	/**
	 * computes the read percentage on the total amount of available
	 * bookmarks
	 * @return
	 */
	public double computeReadPercentage() {
		Collection<IBookMark> allBookmarks = DynamicDAO.loadAll(IBookMark.class);

		int read = 0;
		int total = 0;

		for (IBookMark bookMark : allBookmarks) {
			read = read + bookMark.getNewsCount(EnumSet.of(State.READ));
			total = total + bookMark.getNews().size();
		}

		double percentage = 0;
		if (total > 0) {
			percentage = (read * 100) / (double) total;
			percentage = Math.round(percentage*100)/100;
		}

		return percentage;
	}
}
