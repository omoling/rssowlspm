package org.rssowl.ui.internal.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.dao.DynamicDAO;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.ModelUtils;

public class RateAction extends Action {
	private final int fRating;
	private IStructuredSelection fSelection;

	/**
	 * @param rating
	 * @param selection
	 */
	public RateAction(int rating, IStructuredSelection selection) {
		//hack: have to use a button because otherwise the star images won't show up on Linux (Ubuntu)
		super("", AS_PUSH_BUTTON);
		fRating = rating;
		fSelection = selection;

		setImageDescriptor(createImageDescriptor());
	}

	/**
	 * Creates the correct ImageDescriptor depending on the given rating
	 * 
	 * @return
	 */
	private ImageDescriptor createImageDescriptor() {
		ImageDescriptor imgDescriptor = null;
		switch (fRating) {
		case 0:
			imgDescriptor = OwlUI.NEWS_STARON_0;
			break;
		case 1:
			imgDescriptor = OwlUI.NEWS_STARON_1;
			break;
		case 2:
			imgDescriptor = OwlUI.NEWS_STARON_2;
			break;
		case 3:
			imgDescriptor = OwlUI.NEWS_STARON_3;
			break;
		case 4:
			imgDescriptor = OwlUI.NEWS_STARON_4;
			break;
		case 5:
			imgDescriptor = OwlUI.NEWS_STARON_5;
			break;

		default:
			break;
		}

		return imgDescriptor;
	}

	/*
	 * @see org.eclipse.jface.action.Action#getText()
	 */
	@Override
	public String getText() {
		// return no text, since the images of the stars will be displayed
		return "";
	}

	/*
	 * @see org.eclipse.jface.action.Action#isEnabled()
	 */
	@Override
	public boolean isEnabled() {
		return !fSelection.isEmpty();
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		List<INews> newsList = ModelUtils.getEntities(fSelection, INews.class);
		if (newsList.isEmpty())
			return;

		/* For each News */
		for (INews newsItem : newsList) {
			newsItem.setRating(fRating);
		}

		/* Save */
		DynamicDAO.saveAll(newsList);
	}
}
