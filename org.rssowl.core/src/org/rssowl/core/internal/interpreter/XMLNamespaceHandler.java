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

package org.rssowl.core.internal.interpreter;

import org.jdom.Attribute;
import org.jdom.Element;
import org.rssowl.core.interpreter.INamespaceHandler;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPersistable;
import org.rssowl.core.util.URIUtils;

import java.net.URI;

/**
 * Handler for the XML Namespace.
 * <p>
 * Namespace Prefix: xml<br>
 * Namespace URI: http://www.w3.org/XML/1998/namespace
 * </p>
 * 
 * @author bpasero
 */
public class XMLNamespaceHandler implements INamespaceHandler {

  /*
   * @see org.rssowl.core.interpreter.INamespaceHandler#processElement(org.jdom.Element,
   * org.rssowl.core.model.types.IExtendableType)
   */
  public void processElement(Element element, IPersistable type) {
  /* The XML Namespace is not defined for Elements */
  }

  /*
   * @see org.rssowl.core.interpreter.INamespaceHandler#processAttribute(org.jdom.Attribute,
   * org.rssowl.core.model.types.IExtendableType)
   */
  public void processAttribute(Attribute attribute, IPersistable type) {

    /* Language */
    if ("lang".equals(attribute.getName()) && type instanceof IFeed) //$NON-NLS-1$
      ((IFeed) type).setLanguage(attribute.getValue());

    /* Base URI */
    if ("base".equals(attribute.getName())) { //$NON-NLS-1$
      URI baseUri = URIUtils.createURI(attribute.getValue());
      if (baseUri != null && type instanceof IFeed)
        ((IFeed) type).setBase(baseUri);
      else if (baseUri != null && type instanceof INews)
        ((INews) type).setBase(baseUri);
    }
  }
}