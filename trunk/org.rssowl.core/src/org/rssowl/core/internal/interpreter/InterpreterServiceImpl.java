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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.jdom.Document;
import org.jdom.input.DOMBuilder;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.interpreter.IElementHandler;
import org.rssowl.core.interpreter.IFormatInterpreter;
import org.rssowl.core.interpreter.IInterpreterService;
import org.rssowl.core.interpreter.INamespaceHandler;
import org.rssowl.core.interpreter.ITypeImporter;
import org.rssowl.core.interpreter.IXMLParser;
import org.rssowl.core.interpreter.InterpreterException;
import org.rssowl.core.interpreter.ParserException;
import org.rssowl.core.interpreter.UnknownFormatException;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.util.ExtensionUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Main class of the Interpreter. A contributed or default-JDKs XML-Parser is
 * used to parse the given InputStream into a <code>org.jdom.Document</code>.
 * The Document is then passed to the Contribution responsible for the given
 * Format.
 * </p>
 * The following kind of Extensions are possible:
 * <ul>
 * <li>SAXParser allows to contribute the XML Parser to be used</li>
 * <li>FormatInterpreter allow to contribute Interpreters based on a XML Format</li>
 * <li>NamespaceHandler allow to contribute processing for Namespaces</li>
 * <li>ElementHandler allow to contribute custom processing for Elements</li>
 * </ul>
 *
 * @author bpasero
 */
public class InterpreterServiceImpl implements IInterpreterService {

  /* ID for SAXParser Contribution */
  private static final String SAXPARSER_EXTENSION_POINT = "org.rssowl.core.XMLParser"; //$NON-NLS-1$

  /* ID for FormatInterpreter Contributions */
  private static final String FORMATINTERPRETER_EXTENSION_POINT = "org.rssowl.core.FormatInterpreter"; //$NON-NLS-1$

  /* ID for TypeImporter Contributions */
  private static final String TYPEIMPORTER_EXTENSION_POINT = "org.rssowl.core.TypeImporter"; //$NON-NLS-1$

  /* ID for NamespaceHandler Contributions */
  private static final String NSHANDLER_EXTENSION_POINT = "org.rssowl.core.NamespaceHandler"; //$NON-NLS-1$

  /* ID for ElementHandler Contributions */
  private static final String ELHANDLER_EXTENSION_POINT = "org.rssowl.core.ElementHandler"; //$NON-NLS-1$

  private volatile Map<String, IFormatInterpreter> fFormatInterpreters;
  private volatile Map<String, ITypeImporter> fTypeImporters;
  private volatile Map<String, INamespaceHandler> fNamespaceHandlers;
  private volatile Map<String, IElementHandler> fElementHandlers;
  private volatile IXMLParser fXMLParserImpl;

  /** */
  public InterpreterServiceImpl() {
    startup();
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterService#interpret(java.io.InputStream,
   * org.rssowl.core.model.persist.IFeed)
   */
  public void interpret(InputStream inS, IFeed feed) throws ParserException, InterpreterException {
    Document document = fXMLParserImpl.parse(inS);

    interpretJDomDocument(document, feed);
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterService#interpretW3CDocument(org.w3c.dom.Document,
   * org.rssowl.core.model.persist.IFeed)
   */
  public void interpretW3CDocument(org.w3c.dom.Document w3cDocument, IFeed feed) throws InterpreterException {
    DOMBuilder domBuilder = new DOMBuilder();
    Document jDomDocument = domBuilder.build(w3cDocument);

    interpretJDomDocument(jDomDocument, feed);
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterService#interpretJDomDocument(org.jdom.Document,
   * org.rssowl.core.model.persist.IFeed)
   */
  public void interpretJDomDocument(Document document, IFeed feed) throws InterpreterException {

    /* A Root Element is required */
    if (!document.hasRootElement())
      throw new InterpreterException(Activator.getDefault().createErrorStatus("Document has no Root Element set!", null)); //$NON-NLS-1$

    /* Determine Format of the Feed */
    String format = document.getRootElement().getName().toLowerCase();

    /* A Interpreter is required */
    if (!fFormatInterpreters.containsKey(format))
      throw new UnknownFormatException(Activator.getDefault().createErrorStatus("No Interpreter found for Format \"" + format + "\"", null)); //$NON-NLS-1$//$NON-NLS-2$

    /* Interpret Document into a Feed */
    fFormatInterpreters.get(format).interpret(document, feed);
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterService#importFrom(java.io.InputStream)
   */
  public List< ? extends IEntity> importFrom(InputStream inS) throws InterpreterException, ParserException {
    Document document = fXMLParserImpl.parse(inS);

    /* A Root Element is required */
    if (!document.hasRootElement())
      throw new InterpreterException(Activator.getDefault().createErrorStatus("Document has no Root Element set!", null)); //$NON-NLS-1$

    /* Determine Format of the Feed */
    String format = document.getRootElement().getName().toLowerCase();

    /* An Importer is required */
    if (!fTypeImporters.containsKey(format))
      throw new UnknownFormatException(Activator.getDefault().createErrorStatus("No Importer found for Format \"" + format + "\"", null)); //$NON-NLS-1$//$NON-NLS-2$

    /* Import Type from the Document */
    return fTypeImporters.get(format).importFrom(document);
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterService#getNamespaceHandler(java.lang.String)
   */
  public INamespaceHandler getNamespaceHandler(String namespaceUri) {
    return fNamespaceHandlers.get(namespaceUri);
  }

  /*
   * @see org.rssowl.core.interpreter.IInterpreterService#getElementHandler(java.lang.String,
   * java.lang.String)
   */
  public IElementHandler getElementHandler(String elementName, String rootName) {
    if (fElementHandlers != null)
      return fElementHandlers.get(elementName.toLowerCase() + rootName.toLowerCase());
    return null;
  }

  private void startup() {

    /* Load and Init XMLParser */
    fXMLParserImpl = loadXMLParserImpl();
    Assert.isNotNull(fXMLParserImpl);
    SafeRunner.run(new ISafeRunnable() {

      /* Use Default XML Parser Impl */
      public void handleException(Throwable exception) {
        fXMLParserImpl = new DefaultSaxParserImpl();
        try {
          fXMLParserImpl.init();
        } catch (ParserException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }

      /* Try Contribution */
      public void run() throws Exception {
        fXMLParserImpl.init();
      }
    });

    /* Load Format Interpreters */
    fFormatInterpreters = new HashMap<String, IFormatInterpreter>();
    loadFormatInterpreters();

    /* Load Type Importers */
    fTypeImporters = new HashMap<String, ITypeImporter>();
    loadTypeImporters();

    /* Load Namespace Handlers */
    fNamespaceHandlers = new HashMap<String, INamespaceHandler>();
    loadNamespaceHandlers();

    /* Load Element Handlers */
    loadElementHandlers();
  }

  private void loadNamespaceHandlers() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(NSHANDLER_EXTENSION_POINT);

    for (IConfigurationElement element : elements) {
      try {
        String namespaceUri = element.getAttribute("namespaceURI"); //$NON-NLS-1$

        /* Let 3d-Party contributions override our contributions */
        if (fNamespaceHandlers.containsKey(namespaceUri) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
          continue;

        fNamespaceHandlers.put(namespaceUri, (INamespaceHandler) element.createExecutableExtension("class"));//$NON-NLS-1$
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  private void loadElementHandlers() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(ELHANDLER_EXTENSION_POINT);

    if (elements.length > 0)
      fElementHandlers = new HashMap<String, IElementHandler>();

    for (IConfigurationElement element : elements) {
      String elementName = element.getAttribute("elementName").toLowerCase(); //$NON-NLS-1$
      String rootName = element.getAttribute("rootElement").toLowerCase(); //$NON-NLS-1$

      /* Let 3d-Party contributions override our contributions */
      if (fElementHandlers.containsKey(elementName + rootName) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
        continue;

      try {
        fElementHandlers.put(elementName + rootName, (IElementHandler) element.createExecutableExtension("class"));//$NON-NLS-1$
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  private void loadFormatInterpreters() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(FORMATINTERPRETER_EXTENSION_POINT);

    for (IConfigurationElement element : elements) {
      try {
        String format = element.getAttribute("rootElement").toLowerCase(); //$NON-NLS-1$

        /* Let 3d-Party contributions override our contributions */
        if (fFormatInterpreters.containsKey(format) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
          continue;

        fFormatInterpreters.put(format, (IFormatInterpreter) element.createExecutableExtension("class")); //$NON-NLS-1$
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  private void loadTypeImporters() {
    IExtensionRegistry reg = Platform.getExtensionRegistry();
    IConfigurationElement elements[] = reg.getConfigurationElementsFor(TYPEIMPORTER_EXTENSION_POINT);

    for (IConfigurationElement element : elements) {
      try {
        String format = element.getAttribute("rootElement").toLowerCase(); //$NON-NLS-1$

        /* Let 3d-Party contributions override our contributions */
        if (fTypeImporters.containsKey(format) && element.getNamespaceIdentifier().contains(ExtensionUtils.RSSOWL_NAMESPACE))
          continue;

        fTypeImporters.put(format, (ITypeImporter) element.createExecutableExtension("class")); //$NON-NLS-1$
      } catch (InvalidRegistryObjectException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      } catch (CoreException e) {
        Activator.getDefault().getLog().log(e.getStatus());
      }
    }
  }

  /* Load XML Parser contribution */
  private IXMLParser loadXMLParserImpl() {
    return (IXMLParser) ExtensionUtils.loadSingletonExecutableExtension(SAXPARSER_EXTENSION_POINT);
  }
}