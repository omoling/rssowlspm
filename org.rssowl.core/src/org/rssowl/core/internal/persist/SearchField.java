/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2008 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.core.internal.persist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.ISearchValueType;
import org.rssowl.core.persist.dao.DynamicDAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Instances of <code>ISearchField</code> describe the target field for a
 * search condition. The field is described by its identifier in the system and
 * a human-readable name, used in the UI.
 * </p>
 * <p>
 * A call to <code>getSearchValueType()</code> will give Information of the
 * data-type the field is using. This information can be used for validating the
 * search-value and to perform the search in the persistence layer.
 * </p>
 *
 * @author bpasero
 */
public class SearchField implements ISearchField {
  private int fField;
  private String fEntityName;

  /**
   * Instantiates a new SearchField that is describing the field a target type
   * needs to check for a match.
   *
   * @param field The ID of the field from the given Type, which is described by
   * one of the constants in the type's Interface.
   * @param entityName entityName The fully qualified Name of the
   * <code>IEntity</code> this <code>ISearchField</code> is referring to.
   */
  public SearchField(int field, String entityName) {
    fField = field;
    Assert.isNotNull(entityName, "The type SearchField requires a entityName that is not NULL"); //$NON-NLS-1$
    fEntityName = entityName;
  }

  /**
   * Default constructor for deserialization
   */
  protected SearchField() {
  // As per javadoc
  }

  /*
   * @see org.rssowl.core.model.search.ISearchField#getField()
   */
  public synchronized int getId() {
    return fField;
  }

  /*
   * @see org.rssowl.core.model.search.ISearchField#getEntityName()
   */
  public synchronized String getEntityName() {
    return fEntityName;
  }

  /*
   * @see org.rssowl.core.model.search.ISearchField#getName()
   */
  public synchronized String getName() {

    /* Field from the Type IAttachment */
    if (fEntityName.equals(IAttachment.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire Attachment";
        case (IAttachment.LINK):
          return "Link";
        case (IAttachment.LENGTH):
          return "Size";
        case (IAttachment.TYPE):
          return "Type";
      }
    }

    /* Field from Type IFolder */
    if (fEntityName.equals(IFolder.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire Folder";
        case (IFolder.NAME):
          return "Name";
        case (IFolder.BLOGROLL_LINK):
          return "Blogroll Link";
        case (IFolder.FOLDERS):
          return "Number of Sub-Folders";
        case (IFolder.MARKS):
          return "Number of BookMarks";
      }
    }

    /* Field from Type ILabel */
    if (fEntityName.equals(ILabel.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire Label";
        case (ILabel.NAME):
          return "Name";
        case (ILabel.COLOR):
          return "Color";
      }
    }

    /* Field from Type ICategory */
    if (fEntityName.equals(ICategory.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire Category";
        case (ICategory.NAME):
          return "Name";
        case (ICategory.DOMAIN):
          return "Domain";
      }
    }

    /* Field from Type IBookMark */
    if (fEntityName.equals(IBookMark.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire BookMark";
        case (IMark.CREATION_DATE):
          return "Date of creation";
        case (IMark.NAME):
          return "Name";
        case (IMark.LAST_VISIT_DATE):
          return "Last visit";
        case (IMark.POPULARITY):
          return "Number of Visits";
        case (IBookMark.IS_ERROR_LOADING):
          return "Error while Loading";
      }
    }

    /* Field from Type ISearchMark */
    if (fEntityName.equals(ISearchMark.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire SearchMark";
        case (IMark.CREATION_DATE):
          return "Date of creation";
        case (IMark.NAME):
          return "Name";
        case (IMark.LAST_VISIT_DATE):
          return "Last visit";
        case (IMark.POPULARITY):
          return "Number of Visits";
      }
    }

    /* Field from Type IPerson */
    if (fEntityName.equals(IPerson.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire Person";
        case (IPerson.NAME):
          return "Name";
        case (IPerson.EMAIL):
          return "EMail";
        case (IPerson.URI):
          return "URI";
      }
    }

    /* Field from Type INews */
    if (fEntityName.equals(INews.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire News";
        case (INews.TITLE):
          return "Title";
        case (INews.LINK):
          return "Link";
        case (INews.DESCRIPTION):
          return "Description";
        case (INews.PUBLISH_DATE):
          return "Date Published";
        case (INews.MODIFIED_DATE):
          return "Date Modified";
        case (INews.RECEIVE_DATE):
          return "Date Received";
        case (INews.AUTHOR):
          return "Author";
        case (INews.COMMENTS):
          return "Comments";
        case (INews.GUID):
          return "GUID";
        case (INews.SOURCE):
          return "Source";
        case (INews.HAS_ATTACHMENTS):
          return "Has Attachment";
        case (INews.ATTACHMENTS_CONTENT):
          return "Attachment";
        case (INews.CATEGORIES):
          return "Category";
        case (INews.IS_FLAGGED):
          return "Is Sticky";
        case (INews.STATE):
          return "State of News";
        case (INews.LABEL):
          return "Label";
        case (INews.RATING):
          return "Rating";
        case (INews.FEED):
          return "Feed";
        case (INews.AGE_IN_DAYS):
          return "Age in Days";
        case (INews.LOCATION):
          return "Location";
      }
    }

    /* Field from Type IFeed */
    if (fEntityName.equals(IFeed.class.getName())) {
      switch (fField) {
        case (IEntity.ALL_FIELDS):
          return "Entire News";
        case (IFeed.LINK):
          return "Link";
        case (IFeed.TITLE):
          return "Title";
        case (IFeed.PUBLISH_DATE):
          return "Date Published";
        case (IFeed.DESCRIPTION):
          return "Description";
        case (IFeed.HOMEPAGE):
          return "Homepage";
        case (IFeed.LANGUAGE):
          return "Language";
        case (IFeed.COPYRIGHT):
          return "Copyright";
        case (IFeed.DOCS):
          return "Docs";
        case (IFeed.GENERATOR):
          return "Generator";
        case (IFeed.LAST_BUILD_DATE):
          return "Date Last Built";
        case (IFeed.WEBMASTER):
          return "Webmaster";
        case (IFeed.LAST_MODIFIED_DATE):
          return "Date Last Modified";
        case (IFeed.TTL):
          return "Time To Live";
        case (IFeed.FORMAT):
          return "Format";
        case (IFeed.AUTHOR):
          return "Author";
        case (IFeed.NEWS):
          return "Number of News";
        case (IFeed.CATEGORIES):
          return "Category";
        case (IFeed.IMAGE):
          return "Image";
      }
    }

    /* Default */
    return fEntityName + "#" + fField;
  }

  /*
   * @see org.rssowl.core.model.search.ISearchField#getAllowedSearchTerm()
   */
  public synchronized ISearchValueType getSearchValueType() {

    /* Field from the Type IAttachment */
    if (fEntityName.equals(IAttachment.class.getName())) {
      switch (fField) {
        case (IAttachment.LENGTH):
          return SearchValueType.INTEGER;
        case (IAttachment.LINK):
          return SearchValueType.LINK;
      }
    }

    /* Field from Type IFolder */
    else if (fEntityName.equals(IFolder.class.getName())) {
      switch (fField) {
        case (IFolder.FOLDERS):
          return SearchValueType.INTEGER;
        case (IFolder.MARKS):
          return SearchValueType.INTEGER;
      }
    }

    /* Field from Type IBookMark */
    else if (fEntityName.equals(IBookMark.class.getName())) {
      switch (fField) {
        case (IMark.CREATION_DATE):
          return SearchValueType.DATETIME;
        case (IMark.LAST_VISIT_DATE):
          return SearchValueType.DATETIME;
        case (IMark.POPULARITY):
          return SearchValueType.INTEGER;
        case (IBookMark.IS_ERROR_LOADING):
          return SearchValueType.BOOLEAN;
      }
    }

    /* Field from Type ISearchMark */
    else if (fEntityName.equals(ISearchMark.class.getName())) {
      switch (fField) {
        case (IMark.CREATION_DATE):
          return SearchValueType.DATETIME;
        case (IMark.LAST_VISIT_DATE):
          return SearchValueType.DATETIME;
        case (IMark.POPULARITY):
          return SearchValueType.INTEGER;
      }
    }

    /* Field from Type IPerson */
    else if (fEntityName.equals(IPerson.class.getName()))
      return SearchValueType.STRING;

    /* Field from Type ILabel */
    else if (fEntityName.equals(ILabel.class.getName()))
      return SearchValueType.STRING;

    /* Field from Type ICategory */
    else if (fEntityName.equals(ICategory.class.getName()))
      return SearchValueType.STRING;

    /* Field from Type INews */
    else if (fEntityName.equals(INews.class.getName())) {
      switch (fField) {
        case (INews.PUBLISH_DATE):
          return SearchValueType.DATETIME;
        case (INews.MODIFIED_DATE):
          return SearchValueType.DATETIME;
        case (INews.RECEIVE_DATE):
          return SearchValueType.DATETIME;
        case (INews.IS_FLAGGED):
          return SearchValueType.BOOLEAN;
        case (INews.STATE):
          return new SearchValueType(loadStateValues());
        case (INews.LABEL):
          return new SearchValueType(loadLabelValues());
        case (INews.RATING):
          return SearchValueType.INTEGER;
        case (INews.LINK):
          return SearchValueType.LINK;
        case (INews.FEED):
          return SearchValueType.LINK;
        case (INews.AGE_IN_DAYS):
          return SearchValueType.INTEGER;
        case (INews.SOURCE):
          return SearchValueType.LINK;
        case (INews.HAS_ATTACHMENTS):
          return SearchValueType.BOOLEAN;
      }
    }

    /* Field from Type IFeed */
    else if (fEntityName.equals(IFeed.class.getName())) {
      switch (fField) {
        case (IFeed.LINK):
          return SearchValueType.LINK;
        case (IFeed.PUBLISH_DATE):
          return SearchValueType.DATETIME;
        case (IFeed.LANGUAGE):
          return new SearchValueType(loadLanguageValues());
        case (IFeed.LAST_BUILD_DATE):
          return SearchValueType.DATETIME;
        case (IFeed.LAST_MODIFIED_DATE):
          return SearchValueType.DATETIME;
        case (IFeed.TTL):
          return SearchValueType.INTEGER;
        case (IFeed.NEWS):
          return SearchValueType.INTEGER;
      }
    }

    return SearchValueType.STRING;
  }

  /* Return human-readable list of News-States */
  private List<String> loadStateValues() {
    return new ArrayList<String>(Arrays.asList(new String[] { "New", "Read", "Unread", "Updated", "Deleted" }));
  }

  /* TODO */
  private List<String> loadLanguageValues() {
    return new ArrayList<String>();
  }

  /* Return the Label Values */
  private List<String> loadLabelValues() {
    Collection<ILabel> labels = DynamicDAO.loadAll(ILabel.class);

    List<String> values = new ArrayList<String>(labels.size());
    for (ILabel label : labels) {
      values.add(label.getName());
    }

    return values;
  }

  /**
   * Returns an object which is an instance of the given class associated with
   * this object. Returns <code>null</code> if no such object can be found.
   * <p>
   * This implementation of the method declared by <code>IAdaptable</code>
   * passes the request along to the platform's adapter manager; roughly
   * <code>Platform.getAdapterManager().getAdapter(this, adapter)</code>.
   * Subclasses may override this method (however, if they do so, they should
   * invoke the method on their superclass to ensure that the Platform's adapter
   * manager is consulted).
   * </p>
   *
   * @param adapter the class to adapt to
   * @return the adapted object or <code>null</code>
   * @see IAdaptable#getAdapter(Class)
   * @see Platform#getAdapterManager()
   */
  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    return Platform.getAdapterManager().getAdapter(this, adapter);
  }

  @Override
  public synchronized boolean equals(Object obj) {
    if (this == obj)
      return true;

    if ((obj == null) || (obj.getClass() != getClass()))
      return false;

    synchronized (obj) {
      SearchField f = (SearchField) obj;
      return fField == f.fField && fEntityName.equals(f.fEntityName);
    }
  }

  @Override
  public synchronized int hashCode() {
    int typeHashCode = fEntityName == null ? 0 : fEntityName.hashCode();
    return (((fField + 2) * typeHashCode + 17)) * 37;
  }

  @Override
  @SuppressWarnings("nls")
  public synchronized String toString() {
    return fEntityName + ": " + fField;
  }

  /**
   * Returns a String describing the state of this Entity.
   *
   * @return A String describing the state of this Entity.
   */
  @SuppressWarnings("nls")
  public synchronized String toLongString() {
    return super.toString() + "(Field = " + fField + ", Class = " + fEntityName + ", Name = " + getName() + ", Search-Value-Type = " + getSearchValueType() + ")";
  }
}