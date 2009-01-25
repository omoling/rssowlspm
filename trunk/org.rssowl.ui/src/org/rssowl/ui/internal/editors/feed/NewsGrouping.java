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

package org.rssowl.ui.internal.editors.feed;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.event.ModelEvent;
import org.rssowl.core.persist.reference.FeedLinkReference;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.EntityGroupItem;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.ModelUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author bpasero
 */
public class NewsGrouping {

  /* Some Date Constants */
  private static final long DAY = 24 * 60 * 60 * 1000;
  private static final long WEEK = 7 * DAY;

  /* ID Ranges */
  private static final int TITLE_ID_BEGIN = 1000;
  private static final int LABEL_ID_BEGIN = 2000;
  private static final int CATEGORY_ID_BEGIN = 3000;
  private static final int AUTHOR_ID_BEGIN = 4000;

  /** ID of News Group Category */
  public static final String GROUP_CATEGORY_ID = "org.rssowl.ui.internal.editors.feed.NewsGrouping";

  /** Supported Grouping Types */
  public enum Type {

    /** Grouping is Disabled */
    NO_GROUPING("No Grouping"),

    /** Group by Date */
    GROUP_BY_DATE("Group by Date"),

    /** Group by State */
    GROUP_BY_STATE("Group by State"),

    /** Group by Author */
    GROUP_BY_AUTHOR("Group by Author"),

    /** Group by Category */
    GROUP_BY_CATEGORY("Group by Category"),

    /** Group by Title */
    GROUP_BY_TOPIC("Group by Topic"),

    /** Group by Feed */
    GROUP_BY_FEED("Group by Feed"),

    /** Group by Label */
    GROUP_BY_LABEL("Group by Label"),

    /** Group by Rating */
    GROUP_BY_RATING("Group by Rating"),

    /** Group by Stickyness */
    GROUP_BY_STICKY("Group by Stickyness");

    String fName;

    Type(String name) {
      fName = name;
    }

    /**
     * Returns a human-readable Name of this enum-value.
     *
     * @return A human-readable Name of this enum-value.
     */
    public String getName() {
      return fName;
    }
  }

  /** Groups */
  public enum Group {

    /** Group: No Topic */
    NO_TOPIC("No Topic"),

    /** Group: Unknown Category */
    UNKNOWN_CATEGORY("Unknown Category"),

    /** Group: Unknown Author */
    UNKNOWN_AUTHOR("Unknown Author"),

    /** Group: None (alternative default) */
    NONE("None"),

    /** Group: Today */
    TODAY("Today"),

    /** Group: Yesterday */
    YESTERDAY("Yesterday"),

    /** Group: Earlier this Week */
    EARLIER_THIS_WEEK("Earlier this Week"),

    /** Group: Last Week */
    LAST_WEEK("Last Week"),

    /** Group: Older News */
    OLDER("Older News"),

    /** Group: Fantastic */
    FANTASTIC("Fantastic"),

    /** Group: Good */
    GOOD("Good"),

    /** Group: Moderate */
    MODERATE("Moderate"),

    /** Group: Bad */
    BAD("Bad"),

    /** Group: Very Bad */
    VERY_BAD("Very Bad"),

    /** Group: New */
    NEW("New"),

    /** Group: Updated */
    UPDATED("Updated"),

    /** Group: Unread */
    UNREAD("Unread"),

    /** Group: Read */
    READ("Read"),

    /** Group: Sticky */
    STICKY("Sticky"),

    /** Group: Not Sticky */
    NOT_STICKY("Not Sticky");

    String fName;

    Group(String name) {
      fName = name;
    }

    /**
     * Returns a human-readable Name of this enum-value.
     *
     * @return A human-readable Name of this enum-value.
     */
    public String getName() {
      return fName;
    }
  }

  /* Current Type of Grouping */
  private Type fType = Type.NO_GROUPING;

  /* Get the Type of grouping as defined in the Type Enum */
  Type getType() {
    return fType;
  }

  /**
   * Set the Type of grouping as defined in the Type Enum
   *
   * @param type The type of grouping.
   */
  public void setType(Type type) {
    fType = type;
  }

  boolean needsRefresh(Class<? extends IEntity> entity, Set<? extends ModelEvent> events, boolean isUpdate) {

    /* In case the Grouping is not active at all */
    if (fType == Type.NO_GROUPING)
      return false;

    /* News Event */
    if (entity.equals(INews.class)) {

      /* Update if a News got Deleted or Restored */
      if (ModelUtils.gotDeleted(events) || ModelUtils.gotRestored(events))
        return true;

      /* Check in dependence of Group Type */
      if (fType == Type.GROUP_BY_STATE)
        return ModelUtils.isStateChange(events);
      else if (fType == Type.GROUP_BY_DATE)
        return ModelUtils.isDateChange(events);
      else if (fType == Type.GROUP_BY_AUTHOR)
        return ModelUtils.isAuthorChange(events);
      else if (fType == Type.GROUP_BY_CATEGORY)
        return ModelUtils.isCategoryChange(events);
      else if (fType == Type.GROUP_BY_LABEL)
        return ModelUtils.isLabelChange(events);
      else if (fType == Type.GROUP_BY_FEED && isUpdate) //TODO To be reconsidered when News can be reparented
        return false;
      else if (fType == Type.GROUP_BY_TOPIC)
        return ModelUtils.isTitleChange(events);
      else if (fType == Type.GROUP_BY_STICKY)
        return ModelUtils.isStickyStateChange(events);
      else if (fType == Type.GROUP_BY_RATING)
        return false;

      return true;
    }

    /* Return TRUE in this case for now */
    else if (entity.equals(ISearchMark.class))
      return true;

    return false;
  }

  /**
   * Group the Input based on the selected Type
   *
   * @param input The Input to Group.
   * @return The Input grouped in an array of EntityGroup, as specified by the
   * Type of Group.
   */
  public List<EntityGroup> group(Collection<INews> input) {
    Assert.isTrue(fType != Type.NO_GROUPING, "Grouping is not enabled!"); //$NON-NLS-1$

    /* In case the List is empty */
    if (input.size() == 0)
      return new ArrayList<EntityGroup>(0);

    /* Group by Date */
    if (Type.GROUP_BY_DATE == fType)
      return createDateGroups(input);

    /* Group by State */
    else if (Type.GROUP_BY_STATE == fType)
      return createStateGroups(input);

    /* Group by Author */
    else if (Type.GROUP_BY_AUTHOR == fType)
      return createAuthorGroups(input);

    /* Group by Category */
    else if (Type.GROUP_BY_CATEGORY == fType)
      return createCategoryGroups(input);

    /* Group by State */
    else if (Type.GROUP_BY_LABEL == fType)
      return createLabelGroups(input);

    /* Group by State */
    else if (Type.GROUP_BY_RATING == fType)
      return createRatingGroups(input);

    /* Group by Feed */
    else if (Type.GROUP_BY_FEED == fType)
      return createFeedGroups(input);

    /* Group by Stickyness */
    else if (Type.GROUP_BY_STICKY == fType)
      return createStickyGroups(input);

    /* Group by Title */
    else if (Type.GROUP_BY_TOPIC == fType)
      return createTopicGroups(input);

    /* Should not happen */
    return new ArrayList<EntityGroup>(0);
  }

  private List<EntityGroup> createTopicGroups(Collection<INews> input) {

    /* Default Group */
    EntityGroup gDefault = new EntityGroup(Group.NO_TOPIC.ordinal(), GROUP_CATEGORY_ID, Group.NO_TOPIC.getName());

    Map<String, EntityGroup> groupCache = new HashMap<String, EntityGroup>();
    groupCache.put(Group.NO_TOPIC.getName(), gDefault);

    /* Group Input */
    int nextId = Group.NO_TOPIC.ordinal() + TITLE_ID_BEGIN;
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;
        EntityGroup group = gDefault;

        /* Normalize Title */
        String normalizedTitle = ModelUtils.getHeadline(news);
        normalizedTitle = ModelUtils.normalizeTitle(normalizedTitle);

        /* Determine Group ID */
        String groupId;
        if (StringUtils.isSet(news.getInReplyTo()))
          groupId = news.getInReplyTo();
        else
          groupId = normalizedTitle;

        /* Add or Create Group */
        if (StringUtils.isSet(groupId)) {
          group = groupCache.get(groupId);

          /* Create new Group */
          if (group == null) {
            group = new EntityGroup(nextId++, GROUP_CATEGORY_ID, normalizedTitle);
            groupCache.put(groupId, group);
          }
        }

        /* Add to Group */
        new EntityGroupItem(group, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(groupCache.values().toArray(new EntityGroup[groupCache.values().size()]))));
  }

  private List<EntityGroup> createStickyGroups(Collection<INews> input) {

    /* Build Groups */
    EntityGroup gSticky = new EntityGroup(Group.STICKY.ordinal(), GROUP_CATEGORY_ID, Group.STICKY.getName());
    EntityGroup gNotSticky = new EntityGroup(Group.NOT_STICKY.ordinal(), GROUP_CATEGORY_ID, Group.NOT_STICKY.getName());

    /* Group Input */
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;

        /* Sticky */
        if (news.isFlagged())
          new EntityGroupItem(gSticky, news);

        /* Not Sticky */
        else
          new EntityGroupItem(gNotSticky, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(new EntityGroup[] { gSticky, gNotSticky })));
  }

  private List<EntityGroup> createFeedGroups(Collection<INews> input) {
    Map<Long, EntityGroup> groupCache = new HashMap<Long, EntityGroup>();

    /* Group Input */
    int nextId = 0;
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;
        IFeed feed = news.getFeedReference().resolve();

        /* TODO Workaround for Bug 751: NPE when deleting feed */
        if (feed == null)
          return Collections.emptyList();

        EntityGroup group = groupCache.get(feed.getId());

        if (group == null) {
          String name = StringUtils.isSet(feed.getTitle()) ? feed.getTitle() : feed.getLink().toString();
          group = new EntityGroup(nextId++, GROUP_CATEGORY_ID, name);

          if (!InternalOwl.TESTING) {
            ImageDescriptor feedIcon = null;
            IBookMark bookMark = ModelUtils.getBookMark(new FeedLinkReference(feed.getLink()));
            if (bookMark != null)
              feedIcon = OwlUI.getFavicon(bookMark);
            group.setImage(feedIcon != null ? feedIcon : OwlUI.BOOKMARK);
          }

          /* Cache */
          groupCache.put(feed.getId(), group);
        }

        new EntityGroupItem(group, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(groupCache.values().toArray(new EntityGroup[groupCache.values().size()]))));
  }

  private List<EntityGroup> createRatingGroups(Collection<INews> input) {

    /* Build Groups */
    EntityGroup gFantastic = new EntityGroup(Group.FANTASTIC.ordinal(), GROUP_CATEGORY_ID, Group.FANTASTIC.getName());
    EntityGroup gGood = new EntityGroup(Group.GOOD.ordinal(), GROUP_CATEGORY_ID, Group.GOOD.getName());
    EntityGroup gModerate = new EntityGroup(Group.MODERATE.ordinal(), GROUP_CATEGORY_ID, Group.MODERATE.getName());
    EntityGroup gBad = new EntityGroup(Group.BAD.ordinal(), GROUP_CATEGORY_ID, Group.BAD.getName());
    EntityGroup gVeryBad = new EntityGroup(Group.VERY_BAD.ordinal(), GROUP_CATEGORY_ID, Group.VERY_BAD.getName());

    /* Group Input */
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;

        if (news.getRating() >= 80)
          new EntityGroupItem(gFantastic, news);
        else if (news.getRating() >= 60)
          new EntityGroupItem(gGood, news);
        else if (news.getRating() >= 40)
          new EntityGroupItem(gModerate, news);
        else if (news.getRating() >= 20)
          new EntityGroupItem(gBad, news);
        else
          new EntityGroupItem(gVeryBad, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(new EntityGroup[] { gFantastic, gGood, gModerate, gBad, gVeryBad })));
  }

  private List<EntityGroup> createLabelGroups(Collection<INews> input) {

    /* Default Group */
    EntityGroup gDefault = new EntityGroup(Group.NONE.ordinal(), GROUP_CATEGORY_ID, Group.NONE.getName());

    Map<String, EntityGroup> groupCache = new HashMap<String, EntityGroup>();
    groupCache.put(Group.NONE.getName(), gDefault);

    /* Group Input */
    int nextId = Group.NONE.ordinal() + LABEL_ID_BEGIN;
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;
        Set<ILabel> labels = news.getLabels();
        EntityGroup group = gDefault;

        if (!labels.isEmpty()) {
          String name = labels.iterator().next().getName();
          group = groupCache.get(name);
          if (group == null) {
            group = new EntityGroup(nextId++, GROUP_CATEGORY_ID, name);
            groupCache.put(name, group);
          }
        }

        new EntityGroupItem(group, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(groupCache.values().toArray(new EntityGroup[groupCache.values().size()]))));
  }

  private List<EntityGroup> createCategoryGroups(Collection<INews> input) {

    /* Default Group */
    EntityGroup gDefault = new EntityGroup(Group.UNKNOWN_CATEGORY.ordinal(), GROUP_CATEGORY_ID, Group.UNKNOWN_CATEGORY.getName());

    Map<String, EntityGroup> groupCache = new HashMap<String, EntityGroup>();
    groupCache.put(Group.UNKNOWN_CATEGORY.getName(), gDefault);

    /* Group Input */
    int nextId = Group.UNKNOWN_CATEGORY.ordinal() + CATEGORY_ID_BEGIN;
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;
        List<ICategory> categories = news.getCategories();
        EntityGroup group = gDefault;

        if (categories.size() > 0) {
          String name = categories.get(0).getName();
          if (!StringUtils.isSet(name))
            name = Group.UNKNOWN_CATEGORY.getName();

          group = groupCache.get(name);
          if (group == null) {
            group = new EntityGroup(nextId++, GROUP_CATEGORY_ID, name);
            groupCache.put(name, group);
          }
        }

        new EntityGroupItem(group, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(groupCache.values().toArray(new EntityGroup[groupCache.values().size()]))));
  }

  private List<EntityGroup> createAuthorGroups(Collection<INews> input) {

    /* Default Group */
    EntityGroup gDefault = new EntityGroup(Group.UNKNOWN_AUTHOR.ordinal(), GROUP_CATEGORY_ID, Group.UNKNOWN_AUTHOR.getName());

    Map<String, EntityGroup> groupCache = new HashMap<String, EntityGroup>();
    groupCache.put(Group.UNKNOWN_AUTHOR.getName(), gDefault);

    /* Group Input */
    int nextId = Group.UNKNOWN_AUTHOR.ordinal() + AUTHOR_ID_BEGIN;
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;
        IPerson author = news.getAuthor();
        EntityGroup group = gDefault;

        if (author != null) {
          String name = author.getName();
          if (!StringUtils.isSet(name))
            name = Group.UNKNOWN_AUTHOR.getName();

          group = groupCache.get(name);
          if (group == null) {
            group = new EntityGroup(nextId++, GROUP_CATEGORY_ID, name);
            groupCache.put(name, group);
          }
        }

        new EntityGroupItem(group, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(groupCache.values().toArray(new EntityGroup[groupCache.values().size()]))));
  }

  private List<EntityGroup> createStateGroups(Collection<INews> input) {

    /* Build Groups */
    EntityGroup gNew = new EntityGroup(Group.NEW.ordinal(), GROUP_CATEGORY_ID, Group.NEW.getName());
    EntityGroup gUpdated = new EntityGroup(Group.UPDATED.ordinal(), GROUP_CATEGORY_ID, Group.UPDATED.getName());
    EntityGroup gUnread = new EntityGroup(Group.UNREAD.ordinal(), GROUP_CATEGORY_ID, Group.UNREAD.getName());
    EntityGroup gRead = new EntityGroup(Group.READ.ordinal(), GROUP_CATEGORY_ID, Group.READ.getName());

    /* Group Input */
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;

        /* New */
        if (INews.State.NEW.equals(news.getState()))
          new EntityGroupItem(gNew, news);

        /* Updated */
        else if (INews.State.UPDATED.equals(news.getState()))
          new EntityGroupItem(gUpdated, news);

        /* Unread */
        else if (INews.State.UNREAD.equals(news.getState()))
          new EntityGroupItem(gUnread, news);

        /* Read */
        else if (INews.State.READ.equals(news.getState()))
          new EntityGroupItem(gRead, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(new EntityGroup[] { gNew, gUpdated, gUnread, gRead })));
  }

  private List<EntityGroup> createDateGroups(Collection<INews> input) {

    /* Today */
    Calendar today = DateUtils.getToday();
    long todayMillis = today.getTimeInMillis();

    /* Yesterday */
    Date yesterday = new Date(todayMillis - DAY);

    /* Earlier this Week */
    today.set(Calendar.DAY_OF_WEEK, today.getFirstDayOfWeek());
    Date earlierThisWeek = today.getTime();

    /* Last Week */
    Date lastWeek = new Date(earlierThisWeek.getTime() - WEEK);

    /* Build Groups */
    EntityGroup gToday = new EntityGroup(Group.TODAY.ordinal(), GROUP_CATEGORY_ID, Group.TODAY.getName());
    EntityGroup gYesterday = new EntityGroup(Group.YESTERDAY.ordinal(), GROUP_CATEGORY_ID, Group.YESTERDAY.getName());
    EntityGroup gEarlierThisWeek = new EntityGroup(Group.EARLIER_THIS_WEEK.ordinal(), GROUP_CATEGORY_ID, Group.EARLIER_THIS_WEEK.getName());
    EntityGroup gLastWeek = new EntityGroup(Group.LAST_WEEK.ordinal(), GROUP_CATEGORY_ID, Group.LAST_WEEK.getName());
    EntityGroup gOlder = new EntityGroup(Group.OLDER.ordinal(), GROUP_CATEGORY_ID, Group.OLDER.getName());

    /* Group Input */
    for (Object object : input) {
      if (object instanceof INews) {
        INews news = (INews) object;
        Date date = DateUtils.getRecentDate(news);

        /* Feed was visited Today */
        if (date.getTime() > todayMillis)
          new EntityGroupItem(gToday, news);

        /* Feed was visited Yesterday */
        else if (date.after(yesterday))
          new EntityGroupItem(gYesterday, news);

        /* Feed was visited Two Weeks Ago */
        else if (date.after(earlierThisWeek))
          new EntityGroupItem(gEarlierThisWeek, news);

        /* Feed was visited Last Week */
        else if (date.after(lastWeek))
          new EntityGroupItem(gLastWeek, news);

        /* Feed was visited more than a Week ago */
        else
          new EntityGroupItem(gOlder, news);
      }
    }

    /* Select all that are non empty */
    return maskEmpty(new ArrayList<EntityGroup>(Arrays.asList(new EntityGroup[] { gToday, gYesterday, gEarlierThisWeek, gLastWeek, gOlder })));
  }

  private List<EntityGroup> maskEmpty(List<EntityGroup> items) {
    List<EntityGroup> maskedItems = new ArrayList<EntityGroup>();
    for (EntityGroup item : items) {
      if (item.size() > 0)
        maskedItems.add(item);
    }

    return maskedItems;
  }

  boolean isActive() {
    return fType != Type.NO_GROUPING;
  }
}