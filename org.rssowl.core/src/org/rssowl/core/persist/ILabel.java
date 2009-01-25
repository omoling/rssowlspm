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

package org.rssowl.core.persist;

import java.util.Collection;

import org.rssowl.core.persist.reference.LabelReference;

/**
 * A Label for a News. Some predefined Labels could be "Important", "Work",
 * "Personal", "Todo". Labels should be added by the user and be shown in a
 * custom Color. Labels could also be used to represent AmphetaRate ratings.
 *
 * @author bpasero
 */
public interface ILabel extends IEntity {

  /** One of the fields in this type described as constant */
  public static final int NAME = 0;

  /** One of the fields in this type described as constant */
  public static final int COLOR = 1;

  /**
   * The format to use here is "R,G,B" for example "255,255,127".
   *
   * @return The Color of the Label as RGB.
   */
  String getColor();

  /**
   * Get the Name of this Label.
   *
   * @return The name of this Label.
   */
  String getName();

  /**
   * The format to use here is "R,G,B" for example "255,255,127".
   *
   * @param color The Color of the Label as RGB.
   */
  void setColor(String color);

  /**
   * Set the Name of this Label.
   *
   * @param name The name of this Label.
   */
  void setName(String name);

  LabelReference toReference();
  
  //TODO Userstory 4: searchMark added to ILabel (setter)
  void setSearchMark(ISearchMark searchMark);
  
  //TODO Userstory 4: searchMark added to ILabel (setter)
  ISearchMark getSearchMark();
  
  /**
   * This method returns the number of sticky news items belonging to this Label.
   * 
   * @return The count of sticky news items for this Label.
   */
  int getStickyNewsCount();
  
  
  /**
   * This method returns all the News that have been marked with this Label.
   * 
   * @return The News for this Label.
   */
  public Collection<INews> findNewsWithLabel();
}