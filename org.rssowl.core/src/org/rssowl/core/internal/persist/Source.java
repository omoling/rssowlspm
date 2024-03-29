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
import org.rssowl.core.persist.ISource;
import org.rssowl.core.util.MergeUtils;

import java.net.URI;

/**
 * Simple Implementation of this Type. Data is kept in fields and all Methods
 * are functional to set/get this Data.
 *
 * @author bpasero
 */
public class Source extends Persistable implements ISource {

  /* Attributes */
  private String fLink;
  private String fName;

  /**
   * Constructor used by <code>DefaultModelFactory</code>
   */
  public Source() {}

  /**
   * Creates a new Source Type.
   *
   * @param link The Link of the Feed that this News came from.
   */
  public Source(URI link) {
    if (link != null)
      fLink = link.toString();
  }

  public Source(ISource source) {
    synchronized (source)  {
      setLink(source.getLink());
      setName(source.getName());
    }
  }

  /*
   * @see org.rssowl.core.model.types.ISource#setName(java.lang.String)
   */
  public synchronized void setName(String name) {
    fName = name;
  }

  /*
   * @see org.rssowl.core.model.types.ISource#setLink(java.net.URI)
   */
  public synchronized void setLink(URI link) {
    if (link != null)
      fLink = link.toString();
  }

  /*
   * @see org.rssowl.core.model.types.ISource#getName()
   */
  public synchronized String getName() {
    return fName;
  }

  /*
   * @see org.rssowl.core.model.types.ISource#getLink()
   */
  public synchronized URI getLink() {
    return createURI(fLink);
  }

  @Override
  public synchronized boolean equals(Object source) {
    if (this == source)
      return true;

    if (!(source instanceof Source))
      return false;

    synchronized (source) {
      Source s = (Source) source;

      return (fLink == null ? s.fLink == null : getLink().equals(s.getLink())) &&
          (fName == null ? s.fName == null : fName.equals(s.fName));
    }
  }

  @Override
  public synchronized int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((fName == null) ? 0 : fName.hashCode());
    result = PRIME * result + ((fLink == null) ? 0 : fLink.hashCode());
    return result;
  }

  @Override
  @SuppressWarnings("nls")
  public synchronized String toString() {
    return super.toString() + "Link = " + fLink + ", Name = " + fName + ")";
  }

  public synchronized MergeResult merge(ISource objectToMerge) {
    Assert.isNotNull(objectToMerge);
    synchronized (objectToMerge) {
      boolean updated = !MergeUtils.equals(fName, objectToMerge.getName());
      fName = objectToMerge.getName();
      updated |= !MergeUtils.equals(getLink(), objectToMerge.getLink());
      setLink(objectToMerge.getLink());

      MergeResult mergeResult = new MergeResult();
      if (updated)
        mergeResult.addUpdatedObject(this);

      return mergeResult;
    }
  }
}