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

package org.rssowl.core.interpreter;

import org.jdom.Document;
import org.rssowl.core.persist.IEntity;

import java.util.List;

/**
 * This interface allows to contribute Importers for various XML Formats. The
 * application is deciding which Importer to use based on the name of the root
 * Element of the XML.
 *
 * @author bpasero
 */
public interface ITypeImporter {

  /** Key to store the actual ID of an {@link IEntity} if required */
  public static final String ID_KEY = "org.rssowl.core.interpreter.typeimporter.EntityId";

  /**
   * Import a Type from the given Document. A very common usecase is importing
   * an <code>IFolder</code> from an OPML or other XML Document.
   *
   * @param document The document to import a Type from.
   * @return Returns the Types imported from the Document.
   * @throws InterpreterException Checked Exception to be used in case of any
   * Exception.
   */
  List<? extends IEntity> importFrom(Document document) throws InterpreterException;
}