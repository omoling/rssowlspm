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

package org.rssowl.ui.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ColorDescriptor;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.DeviceResourceException;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.persist.pref.DefaultPreferences;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.INewsMark;
import org.rssowl.core.persist.ISearchMark;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.SearchMarkReference;
import org.rssowl.ui.internal.editors.feed.FeedView;
import org.rssowl.ui.internal.editors.feed.FeedViewInput;
import org.rssowl.ui.internal.util.EditorUtils;
import org.rssowl.ui.internal.views.explorer.BookMarkExplorer;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central Facade for UI-related tasks.
 *
 * @author bpasero
 */
public class OwlUI {

  /** Top-Level Menu ID for "Tools" */
  public static final String M_TOOLS = "tools"; //$NON-NLS-1$

  /** Top-Level Menu ID for "Mark" */
  public static final String M_MARK = "mark"; //$NON-NLS-1$

  /** Default */
  public static final ImageDescriptor UNKNOWN = Activator.getImageDescriptor("icons/obj16/default.gif"); //$NON-NLS-1$

  /** Folder */
  public static final ImageDescriptor FOLDER = Activator.getImageDescriptor("icons/obj16/folder.gif"); //$NON-NLS-1$

  /** Folder with new News */
  public static final ImageDescriptor FOLDER_NEW = Activator.getImageDescriptor("icons/obj16/folder_new.gif"); //$NON-NLS-1$

  /** Bookmark Set */
  public static final ImageDescriptor BOOKMARK_SET = Activator.getImageDescriptor("icons/obj16/bkmrk_set.gif"); //$NON-NLS-1$

  /** BookMark */
  public static final ImageDescriptor BOOKMARK = Activator.getImageDescriptor("icons/obj16/bookmark.gif"); //$NON-NLS-1$

  /** BookMark (Error) */
  public static final ImageDescriptor BOOKMARK_ERROR = Activator.getImageDescriptor("icons/obj16/bkmrk_error.gif"); //$NON-NLS-1$

  /** NewsBin */
  public static final ImageDescriptor NEWSBIN = Activator.getImageDescriptor("icons/obj16/newsbin.gif"); //$NON-NLS-1$

  /** NewsBin (New) */
  public static final ImageDescriptor NEWSBIN_NEW = Activator.getImageDescriptor("icons/obj16/newsbin_new.gif"); //$NON-NLS-1$

  /** NewsBin (Empty) */
  public static final ImageDescriptor NEWSBIN_EMPTY = Activator.getImageDescriptor("icons/obj16/newsbin_empty.gif"); //$NON-NLS-1$

  /** SearchMark */
  public static final ImageDescriptor SEARCHMARK = Activator.getImageDescriptor("icons/obj16/searchmark.gif"); //$NON-NLS-1$

  /** SearchMark (New) */
  public static final ImageDescriptor SEARCHMARK_NEW = Activator.getImageDescriptor("icons/obj16/searchmark_new.gif"); //$NON-NLS-1$

  /** SearchMark (Empty) */
  public static final ImageDescriptor SEARCHMARK_EMPTY = Activator.getImageDescriptor("icons/obj16/searchmark_empty.gif"); //$NON-NLS-1$
  
  /** Label */
  public static final ImageDescriptor LABEL = Activator.getImageDescriptor("icons/obj16/label_obj.gif"); //$NON-NLS-1$

  /** Group */
  public static final ImageDescriptor GROUP = Activator.getImageDescriptor("icons/obj16/group.gif"); //$NON-NLS-1$

  /** News: Unread */
  public static final ImageDescriptor NEWS_STATE_UNREAD = Activator.getImageDescriptor("icons/obj16/news_unread.gif"); //$NON-NLS-1$

  /** News: Read */
  public static final ImageDescriptor NEWS_STATE_READ = Activator.getImageDescriptor("icons/obj16/news_read.gif"); //$NON-NLS-1$

  /** News: New */
  public static final ImageDescriptor NEWS_STATE_NEW = Activator.getImageDescriptor("icons/obj16/news_new.gif"); //$NON-NLS-1$

  /** News: Updated */
  public static final ImageDescriptor NEWS_STATE_UPDATED = Activator.getImageDescriptor("icons/obj16/news_updated.gif"); //$NON-NLS-1$

  /** News: Pin */
  public static final ImageDescriptor NEWS_PIN = Activator.getImageDescriptor("icons/obj16/news_pin.gif"); //$NON-NLS-1$

  /** News: Pinned */
  public static final ImageDescriptor NEWS_PINNED = Activator.getImageDescriptor("icons/obj16/news_pinned.gif"); //$NON-NLS-1$
  
  /** News: Stars rating */
  public static final ImageDescriptor NEWS_STARON_0 = Activator.getImageDescriptor("icons/obj16/starsOn0.gif"); //$NON-NLS-1$  
  public static final ImageDescriptor NEWS_STARON_1 = Activator.getImageDescriptor("icons/obj16/starsOn1.gif"); //$NON-NLS-1$
  public static final ImageDescriptor NEWS_STARON_2 = Activator.getImageDescriptor("icons/obj16/starsOn2.gif"); //$NON-NLS-1$
  public static final ImageDescriptor NEWS_STARON_3 = Activator.getImageDescriptor("icons/obj16/starsOn3.gif"); //$NON-NLS-1$
  public static final ImageDescriptor NEWS_STARON_4 = Activator.getImageDescriptor("icons/obj16/starsOn4.gif"); //$NON-NLS-1$
  public static final ImageDescriptor NEWS_STARON_5 = Activator.getImageDescriptor("icons/obj16/starsOn5.gif"); //$NON-NLS-1$

  /** Tray Icon: Not Teasing */
  public static final ImageDescriptor TRAY_OWL = Activator.getImageDescriptor("icons/elcl16/trayowl.png"); //$NON-NLS-1$

  /** Tray Icon: Teasing */
  public static final ImageDescriptor TRAY_OWL_TEASING = Activator.getImageDescriptor("icons/elcl16/trayowl_tease.png"); //$NON-NLS-1$

  /** Info */
  public static final ImageDescriptor INFO = Activator.getImageDescriptor("icons/obj16/info.gif"); //$NON-NLS-1$

  /** Warning */
  public static final ImageDescriptor WARNING = Activator.getImageDescriptor("icons/obj16/warning.gif"); //$NON-NLS-1$

  /** Error */
  public static final ImageDescriptor ERROR = Activator.getImageDescriptor("icons/obj16/error.gif"); //$NON-NLS-1$

  /** Group Foreground Color */
  public static final RGB GROUP_FG_COLOR = new RGB(0, 0, 128);

  /** Group Background Color (non Custom Owner Drawn) */
  public static final RGB GROUP_BG_COLOR = new RGB(235, 235, 235);

  /** Group Gradient Foreground Color */
  public static final RGB GROUP_GRADIENT_FG_COLOR = new RGB(250, 250, 250);

  /** Group Gradient Background Color */
  public static final RGB GROUP_GRADIENT_BG_COLOR = new RGB(220, 220, 220);

  /** Group Gradient End Color */
  public static final RGB GROUP_GRADIENT_END_COLOR = new RGB(200, 200, 200);

  /** Minimum width of Dialogs in Dialog Units */
  public static final int MIN_DIALOG_WIDTH_DLU = 320;

  /** News-Text Font Id */
  public static final String NEWS_TEXT_FONT_ID = "org.rssowl.ui.NewsTextFont";

  /** Headlines Font Id */
  public static final String HEADLINES_FONT_ID = "org.rssowl.ui.HeadlinesFont";

  /** BookMark Explorer Font Id */
  public static final String BKMRK_EXPLORER_FONT_ID = "org.rssowl.ui.BookmarkExplorerFont";

  /** Notification Popup Font Id */
  public static final String NOTIFICATION_POPUP_FONT_ID = "org.rssowl.ui.NotificationPopupFont";

  /** Sticky Background Color */
  public static final String STICKY_BG_COLOR_ID = "org.rssowl.ui.StickyBGColor";

  /* Used to cache Image-Descriptors for Favicons */
  private static final Map<Long, ImageDescriptor> FAVICO_CACHE = new HashMap<Long, ImageDescriptor>();

  /* Used to cache Image-Descriptors obtained from a file-path */
  private static final Map<String, ImageDescriptor> DESCRIPTOR_CACHE = new HashMap<String, ImageDescriptor>();

  /* Name of Folder for storing Icons */
  private static final String ICONS_FOLDER = "icons";

  /* Shared Clipboard instance */
  private static Clipboard fgClipboard;

  /* Cache the OSTheme once retrieved */
  private static OSTheme fgCachedOSTheme;

  /** An enumeration of Operating System Themes */
  enum OSTheme {

    /** Windows XP Blue */
    WINDOWS_BLUE,

    /** Windows XP Silver */
    WINDOWS_SILVER,

    /** Windows XP Olive */
    WINDOWS_OLIVE,

    /** Windows Classic */
    WINDOWS_CLASSIC,

    /** Any other Theme */
    OTHER
  }

  /**
   * Returns the <code>OSTheme</code> that is currently being used.
   *
   * @param display An instance of the SWT <code>Display</code> used for
   * determining the used theme.
   * @return Returns the <code>OSTheme</code> that is currently being used.
   */
  public static OSTheme getOSTheme(Display display) {

    /* Check Cached version first */
    if (fgCachedOSTheme != null)
      return fgCachedOSTheme;

    RGB widgetBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND).getRGB();
    RGB listSelection = display.getSystemColor(SWT.COLOR_LIST_SELECTION).getRGB();

    /* Theme: Windows Blue */
    if (widgetBackground.equals(new RGB(236, 233, 216)) && listSelection.equals(new RGB(49, 106, 197)))
      fgCachedOSTheme = OSTheme.WINDOWS_BLUE;

    /* Theme: Windows Classic */
    else if (widgetBackground.equals(new RGB(212, 208, 200)) && listSelection.equals(new RGB(10, 36, 106)))
      fgCachedOSTheme = OSTheme.WINDOWS_CLASSIC;

    /* Theme: Windows Silver */
    else if (widgetBackground.equals(new RGB(224, 223, 227)) && listSelection.equals(new RGB(178, 180, 191)))
      fgCachedOSTheme = OSTheme.WINDOWS_SILVER;

    /* Theme: Windows Olive */
    else if (widgetBackground.equals(new RGB(236, 233, 216)) && listSelection.equals(new RGB(147, 160, 112)))
      fgCachedOSTheme = OSTheme.WINDOWS_OLIVE;

    /* Any other Theme */
    else
      fgCachedOSTheme = OSTheme.OTHER;

    return fgCachedOSTheme;
  }

  /**
   * Get the shared instance of <code>Clipboard</code>.
   *
   * @return the shared instance of <code>Clipboard</code>.
   */
  public static Clipboard getClipboard() {
    if (fgClipboard == null)
      fgClipboard = new Clipboard(PlatformUI.getWorkbench().getDisplay());

    return fgClipboard;
  }

  /**
   * Returns an ImageDescriptor for that matches the given Object, or the
   * default image in case the Object is unknown.
   *
   * @param obj The Object to retreive an Image for.
   * @return ImageDescriptor
   */
  @SuppressWarnings("nls")
  public static ImageDescriptor getImageDescriptor(Object obj) {
    if (obj instanceof IFolder) {
      IFolder folder = (IFolder) obj;
      if (folder.getParent() == null)
        return BOOKMARK_SET;

      return FOLDER;
    }

    if (obj instanceof FolderReference)
      return FOLDER;

    if (obj instanceof IBookMark || obj instanceof BookMarkReference)
      return BOOKMARK;

    if (obj instanceof INewsBin || obj instanceof NewsBinReference)
      return NEWSBIN;

    if (obj instanceof ISearchMark || obj instanceof SearchMarkReference)
      return SEARCHMARK;

    if (obj instanceof EntityGroup)
      return GROUP;

    return UNKNOWN;
  }

  /**
   * @param path
   * @return ImageDescriptor
   */
  public static ImageDescriptor getImageDescriptor(String path) {
    ImageDescriptor desc = DESCRIPTOR_CACHE.get(path);
    if (desc == null) {
      desc = Activator.getImageDescriptor(path);
      DESCRIPTOR_CACHE.put(path, desc);
    }

    return desc;
  }

  /**
   * @param manager
   * @param descriptor
   * @return Image
   */
  public static Image getImage(ResourceManager manager, ImageDescriptor descriptor) {
    try {
      return manager.createImage(descriptor);
    } catch (DeviceResourceException e) {
      return getDefaultImage(manager);
    } catch (SWTException e) {
      return getDefaultImage(manager);
    }
  }

  /* Returns the default Image or NULL if unable to create */
  private static Image getDefaultImage(ResourceManager manager) {
    try {
      return manager.createImage(UNKNOWN);
    } catch (DeviceResourceException e1) {
      return null; // Should not happen
    }
  }

  /**
   * @param manager
   * @param obj
   * @return Image
   */
  public static Image getImage(ResourceManager manager, Object obj) {
    return getImage(manager, getImageDescriptor(obj));
  }

  /**
   * @param manager
   * @param path
   * @return Image
   */
  public static Image getImage(ResourceManager manager, String path) {
    return getImage(manager, getImageDescriptor(path));
  }

  /**
   * @param owner
   * @param path
   * @return Image
   */
  public static Image getImage(Control owner, String path) {
    LocalResourceManager manager = new LocalResourceManager(JFaceResources.getResources(), owner);
    return getImage(manager, path);
  }

  /**
   * @param manager
   * @param rgb
   * @return Color
   */
  public static Color getColor(ResourceManager manager, RGB rgb) {
    try {
      return manager.createColor(rgb);
    } catch (DeviceResourceException e) {
      return manager.getDevice().getSystemColor(SWT.COLOR_BLACK);
    }
  }

  /**
   * @param manager
   * @param descriptor
   * @return Color
   */
  public static Color getColor(ResourceManager manager, ColorDescriptor descriptor) {
    try {
      return manager.createColor(descriptor);
    } catch (DeviceResourceException e) {
      return manager.getDevice().getSystemColor(SWT.COLOR_BLACK);
    }
  }

  /**
   * @param resources
   * @param label
   * @return Color
   */
  public static Color getColor(ResourceManager resources, ILabel label) {
    RGB rgb = getRGB(label);

    return getColor(resources, rgb);
  }

  /**
   * @param label
   * @return RGB
   */
  public static RGB getRGB(ILabel label) {
    String color[] = label.getColor().split(",");
    return new RGB(Integer.parseInt(color[0]), Integer.parseInt(color[1]), Integer.parseInt(color[2]));
  }

  /**
   * @param key
   * @return Font
   */
  public static Font getFont(String key) {
    return JFaceResources.getFontRegistry().get(key);
  }

  /**
   * @param key
   * @return Font
   */
  public static Font getBold(String key) {
    return JFaceResources.getFontRegistry().getBold(key);
  }

  /**
   * @param key
   * @return Font
   */
  public static Font getItalic(String key) {
    return JFaceResources.getFontRegistry().getItalic(key);
  }

  /**
   * @param key
   * @param style
   * @return Font
   */
  public static Font getThemeFont(String key, int style) {
    FontRegistry fontRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getFontRegistry();
    if (fontRegistry != null) {
      if (style == SWT.NORMAL)
        return fontRegistry.get(key);
      else if ((style & SWT.BOLD) != 0)
        return fontRegistry.getBold(key);
      else if ((style & SWT.ITALIC) != 0)
        return fontRegistry.getItalic(key);
    }

    return getFont(key);
  }

  /**
   * @param key
   * @param manager
   * @param defaultColor
   * @return Font
   */
  public static Color getThemeColor(String key, ResourceManager manager, RGB defaultColor) {
    ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
    if (colorRegistry != null)
      return getColor(manager, colorRegistry.getColorDescriptor(key));

    return getColor(manager, defaultColor);
  }

  /**
   * @param key
   * @param defaultRGB
   * @return Font
   */
  public static RGB getThemeRGB(String key, RGB defaultRGB) {
    ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
    if (colorRegistry != null)
      return colorRegistry.getRGB(key);

    return defaultRGB;
  }

  /**
   * @param drawable
   * @param text
   * @param font
   * @return The size of the Text as Point.
   */
  public static Point getTextSize(Composite drawable, Font font, String text) {
    GC gc = new GC(drawable);
    gc.setFont(font);
    Point p = gc.textExtent(text);
    gc.dispose();

    return p;
  }

  /**
   * @param bookmark
   * @return ImageDescriptor
   */
  public static ImageDescriptor getFavicon(IBookMark bookmark) {
    Assert.isNotNull(bookmark.getId());

    /* 1.) Check if ImageDescriptor exists in Memory */
    ImageDescriptor descriptor = FAVICO_CACHE.get(bookmark.getId());
    if (descriptor != null)
      return descriptor;

    /* 2.) Check if ImageDescriptor exists in File System */
    File favicon = getImageFile(bookmark.getId());
    if (favicon != null && favicon.exists()) {
      try {
        descriptor = ImageDescriptor.createFromURL(favicon.toURI().toURL());
        FAVICO_CACHE.put(bookmark.getId(), descriptor);
        return descriptor;
      } catch (MalformedURLException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return null;
  }

  /**
   * @param id
   */
  public static void deleteImage(long id) {
    boolean res = false;

    /* Delete from Cache */
    FAVICO_CACHE.remove(id);

    /* Delete from Disk */
    File file = getImageFile(id);
    if (file != null && file.exists())
      res = file.delete();
    else
      res = true;

    if (!res)
      Activator.getDefault().logInfo("Unable to delete image with ID " + id);
  }

  /**
   * @param id
   * @param bytes
   * @param defaultImage
   * @param wHint
   * @param hHint
   */
  public static void storeImage(long id, byte[] bytes, ImageDescriptor defaultImage, int wHint, int hHint) {
    Assert.isNotNull(defaultImage);
    Assert.isLegal(wHint > 0);
    Assert.isLegal(hHint > 0);

    ImageData imgData = null;

    /* Bytes Provided */
    if (bytes != null && bytes.length > 0) {
      ByteArrayInputStream inS = null;
      try {
        inS = new ByteArrayInputStream(bytes);
        ImageLoader loader = new ImageLoader();
        ImageData[] imageDatas = loader.load(inS);

        /* Look for the Icon with the best quality */
        if (imageDatas != null)
          imgData = getBestQuality(imageDatas, wHint, hHint);
      } catch (SWTException e) {
        /* Ignore any Image-Format exceptions */
      } finally {
        if (inS != null) {
          try {
            inS.close();
          } catch (IOException e) {
            Activator.getDefault().logError(e.getMessage(), e);
          }
        }
      }
    }

    /* Use default Image if img-data is null */
    if (imgData == null)
      imgData = defaultImage.getImageData();

    /* Save Image into Cache-Area on File-System */
    if (imgData != null) {
      File imageFile = getImageFile(id);
      if (imageFile == null)
        return;

      /* Scale if required */
      if (imgData.width != 16 || imgData.height != 16)
        imgData = imgData.scaledTo(16, 16);

      /* Try using native Image Format */
      try {
        if (storeImage(imgData, imageFile, imgData.type))
          return;
      } catch (SWTException e) {
        /* Ignore any Image-Format exceptions */
      }

      /* Try using various other Image-Formats */
      int formats[] = new int[] { SWT.IMAGE_PNG, SWT.IMAGE_ICO, SWT.IMAGE_GIF, SWT.IMAGE_BMP };
      for (int format : formats) {
        if (format != imgData.type) {
          try {
            if (storeImage(imgData, imageFile, format))
              return;
          } catch (SWTException e) {
            /* Ignore any Image-Format exceptions */
          }
        }
      }
    }
  }

  /* Returns the ImageData with best Depth or Size */
  private static ImageData getBestQuality(ImageData datas[], int wHint, int hHint) {
    ImageData bestSize = null;
    ImageData bestDepth = null;
    int maxDepth = -1;
    int maxSize = -1;

    /* Foreach Image: Check best Depth */
    for (ImageData data : datas) {
      if (data.depth > maxDepth) {
        maxDepth = data.depth;
        bestDepth = data;
      }
    }

    /* Foreach Image: Check best Size */
    for (ImageData data : datas) {

      /* Only consider best depth */
      if (data.depth == maxDepth) {

        /* Return if Size matches Hint */
        if (data.width == wHint && data.height == hHint)
          return data;

        /* Otherwise look for bigges */
        if (data.width * data.height > maxSize) {
          maxSize = data.width * data.height;
          bestSize = data;
        }
      }
    }

    return (bestDepth != null) ? bestDepth : bestSize;
  }

  /* Saves the Image to the given File with the given Image-Format */
  private static boolean storeImage(ImageData imgData, File file, int format) {
    ImageLoader loader = new ImageLoader();
    loader.data = new ImageData[] { imgData };
    FileOutputStream fOs = null;
    try {
      fOs = new FileOutputStream(file);
      loader.save(fOs, format);
    } catch (FileNotFoundException e) {
      Activator.getDefault().logError(e.getMessage(), e);
    } finally {
      if (fOs != null)
        try {
          fOs.close();
        } catch (IOException e) {
          Activator.getDefault().logError(e.getMessage(), e);
        }
    }

    return true;
  }

  private static File getImageFile(long id) {
    boolean res = false;

    IPath path = new Path(Activator.getDefault().getStateLocation().toOSString());
    path = path.append(ICONS_FOLDER);
    File root = new File(path.toOSString());
    if (!root.exists())
      res = root.mkdir();
    else
      res = true;

    path = path.append(id + ".ico");

    if (!res) {
      Activator.getDefault().logInfo("Unable to get image file with ID " + id);
      return null;
    }

    return new File(path.toOSString());
  }

  /**
   * Attempts to find the primary <code>IWorkbenchWindow</code> from the
   * PlatformUI facade. Otherwise, returns <code>NULL</code> if none.
   *
   * @return the primary <code>IWorkbenchWindow</code> from the PlatformUI
   * facade or <code>NULL</code> if none.
   */
  public static IWorkbenchWindow getPrimaryWindow() {

    /* Return the first Window of the Workbench */
    IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
    if (windows.length > 0)
      return windows[0];

    return null;
  }

  /**
   * Attempts to find the first <code>IWorkbenchWindow</code> from the
   * PlatformUI facade. Otherwise, returns <code>NULL</code> if none.
   *
   * @return the first <code>IWorkbenchWindow</code> from the PlatformUI facade
   * or <code>NULL</code> if none.
   */
  public static IWorkbenchWindow getWindow() {

    /* First try active Window */
    IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (activeWorkbenchWindow != null)
      return activeWorkbenchWindow;

    /* Finally try any Window */
    IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
    if (windows.length > 0)
      return windows[0];

    return null;
  }

  /**
   * Attempts to find the <code>IWorkbenchWindow</code> from the PlatformUI
   * facade that is located at where the mouse is pointing at. Otherwise,
   * returns <code>NULL</code> if none.
   *
   * @return the first <code>IWorkbenchWindow</code> from the PlatformUI facade
   * that is located at where the mouse is pointing at or <code>NULL</code> if
   * none.
   */
  public static IWorkbenchWindow getWindowAtCursor() {

    /* Get the Control at the Cursor position */
    Control cursorControl = Display.getDefault().getCursorControl();
    if (cursorControl == null)
      return null;

    /* Return Window that belongs to Cursor-Shell */
    Shell cursorShell = cursorControl.getShell();
    IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
    for (IWorkbenchWindow workbenchWindow : windows) {
      if (workbenchWindow.getShell().equals(cursorShell))
        return workbenchWindow;
    }

    return null;
  }

  /**
   * Attempts to find the first <code>IWorkbenchPage</code> from the PlatformUI
   * facade. Otherwise, returns <code>NULL</code> if none.
   *
   * @return the first <code>IWorkbenchPage</code> from the PlatformUI facade or
   * <code>NULL</code> if none.
   */
  public static IWorkbenchPage getPage() {
    IWorkbenchWindow window = getWindow();
    if (window != null) {

      /* First try active Page */
      if (window.getActivePage() != null)
        return window.getActivePage();

      /* Finally try any Page */
      IWorkbenchPage[] pages = window.getPages();
      if (pages.length > 0)
        return pages[0];
    }

    return null;
  }

  /**
   * Attempts to return the index of the given workbench window or
   * <code>-1</code> if none.
   *
   * @param window the {@link IWorkbenchWindow} to get the index in the stack of
   * windows that are open.
   * @return the index of the given workbench window or <code>-1</code> if none.
   */
  public static int getWindowIndex(IWorkbenchWindow window) {
    if (window != null) {
      IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
      for (int i = 0; i < windows.length; i++)
        if (windows[i].equals(window))
          return i;
    }

    return 0;
  }

  /**
   * Attempts to find the <code>IWorkbenchPage</code> from the Workbench-Window
   * the mouse is currently over from the PlatformUI facade. Otherwise, returns
   * <code>NULL</code> if none.
   *
   * @return the first <code>IWorkbenchPage</code> from the Workbench-Window the
   * mouse is currently over from the PlatformUI facade or <code>NULL</code> if
   * none.
   */
  public static IWorkbenchPage getPageAtCursor() {
    IWorkbenchWindow window = getWindowAtCursor();
    if (window != null) {

      /* First try active Page */
      if (window.getActivePage() != null)
        return window.getActivePage();

      /* Finally try any Page */
      IWorkbenchPage[] pages = window.getPages();
      if (pages.length > 0)
        return pages[0];
    }

    return null;
  }

  /**
   * Attempts to find the first active <code>IEditorPart</code> from the
   * PlatformUI facade. Otherwise, returns <code>NULL</code> if none.
   *
   * @return the first active <code>IEditorPart</code> from the PlatformUI
   * facade or <code>NULL</code> if none.
   */
  public static IEditorPart getActiveEditor() {
    IWorkbenchPage page = getPage();
    if (page != null)
      return page.getActiveEditor();

    return null;
  }

  /**
   * Attempts to find the first active <code>FeedView</code> from the PlatformUI
   * facade. Otherwise, returns <code>NULL</code> if none.
   *
   * @return the first active <code>FeedView</code> from the PlatformUI facade
   * or <code>NULL</code> if none.
   */
  public static FeedView getActiveFeedView() {
    IWorkbenchPage page = getPage();
    if (page != null) {
      IEditorPart activeEditor = page.getActiveEditor();
      if (activeEditor != null && activeEditor instanceof FeedView)
        return (FeedView) activeEditor;
    }

    return null;
  }

  /**
   * Attempts to find the selection from the first active <code>FeedView</code>
   * from the PlatformUI facade. Otherwise, returns <code>NULL</code> if none.
   *
   * @return the selection from the first active <code>FeedView</code> from the
   * PlatformUI facade or <code>NULL</code> if none.
   */
  public static IStructuredSelection getActiveFeedViewSelection() {
    FeedView feedview = getActiveFeedView();
    if (feedview == null)
      return null;

    ISelectionProvider selectionProvider = feedview.getSite().getSelectionProvider();
    if (selectionProvider == null)
      return null;

    return (IStructuredSelection) selectionProvider.getSelection();
  }

  /**
   * Attempts to find the first <code>FeedView</code> from the active Workbench
   * Window of the PlatformUI facade. Otherwise, returns <code>NULL</code> if
   * none.
   *
   * @return the first <code>FeedView</code> from the active Workbench Window of
   * the PlatformUI facade or <code>NULL</code> if none.
   */
  public static FeedView getFirstActiveFeedView() {
    IWorkbenchPage page = getPage();
    if (page != null) {
      IEditorReference[] editorReferences = page.getEditorReferences();
      for (IEditorReference editorReference : editorReferences) {
        try {
          if (editorReference.getEditorInput() instanceof FeedViewInput)
            return (FeedView) editorReference.getEditor(true);
        } catch (PartInitException e) {
          /* Ignore Silently */
        }
      }
    }

    return null;
  }

  /**
   * Attempts to find the opened <code>BookMarkExplorer</code> from the
   * PlatformUI facade. Otherwise, returns <code>NULL</code> if none.
   *
   * @return the <code>BookMarkExplorer</code> from the PlatformUI facade or
   * <code>NULL</code> if not opened.
   */
  public static BookMarkExplorer getOpenBookMarkExplorer() {
    IWorkbenchPage page = getPage();
    if (page != null) {
      IViewReference[] viewReferences = page.getViewReferences();
      for (IViewReference viewRef : viewReferences) {
        if (viewRef.getId().equals(BookMarkExplorer.VIEW_ID)) {
          IViewPart view = viewRef.getView(true);
          if (view instanceof BookMarkExplorer)
            return (BookMarkExplorer) view;
        }
      }
    }

    return null;
  }

  /**
   * Attempts to find the primary <code>Shell</code> from the PlatformUI facade.
   * Otherwise, returns <code>NULL</code> if none.
   *
   * @return the primary <code>Shell</code> from the PlatformUI facade or
   * <code>NULL</code> if none.
   */
  public static Shell getPrimaryShell() {
    IWorkbenchWindow window = getPrimaryWindow();
    if (window != null)
      return window.getShell();

    return null;
  }

  /**
   * Attempts to find the active <code>Shell</code> from the PlatformUI facade.
   * Otherwise, returns <code>NULL</code> if none.
   *
   * @return the active <code>Shell</code> from the PlatformUI facade or
   * <code>NULL</code> if none.
   */
  public static Shell getActiveShell() {
    IWorkbenchWindow window = getWindow();
    if (window != null)
      return window.getShell();

    return null;
  }

  /**
   * Update the current active window title based on the given array of {@link
   * IMark}.
   *
   * @param shownInput the input that is currently visible in RSSOwl.
   */
  public static void updateWindowTitle(IMark[] shownInput) {
    IWorkbenchWindow window = getWindow();
    if (window != null) {
      String title = "RSSOwl";
      if (shownInput != null && shownInput.length > 0)
        title = shownInput[0].getName() + " - " + title;

      window.getShell().setText(title);
    }
  }

  /**
   * A helper method that can be used to restore the application when its
   * minimized.
   *
   * @param page the workbench page the application is running in.
   */
  public static void restoreWindow(IWorkbenchPage page) {
    Shell applicationShell = page.getWorkbenchWindow().getShell();

    /* Restore from Tray or Minimization if required */
    ApplicationWorkbenchWindowAdvisor advisor = ApplicationWorkbenchAdvisor.fgPrimaryApplicationWorkbenchWindowAdvisor;
    if (advisor != null && advisor.isMinimizedToTray())
      advisor.restoreFromTray(applicationShell);
    else if (applicationShell.getMinimized()) {
      applicationShell.setMinimized(false);
      applicationShell.forceActive();
    }
  }

  /**
   * @return the current selected {@link IFolder} of the bookmark explorer or
   * the parent of the current selected {@link IMark} or <code>null</code> if
   * none.
   */
  public static IFolder getBookMarkExplorerSelection() {
    IWorkbenchPage page = getPage();
    if (page != null) {
      IViewPart viewPart = page.findView(BookMarkExplorer.VIEW_ID);
      if (viewPart != null) {
        IStructuredSelection selection = (IStructuredSelection) viewPart.getSite().getSelectionProvider().getSelection();
        if (!selection.isEmpty()) {
          Object selectedEntity = selection.iterator().next();
          if (selectedEntity instanceof IFolder)
            return (IFolder) selectedEntity;
          else if (selectedEntity instanceof IMark)
            return ((IMark) selectedEntity).getParent();
        }
      }
    }

    return null;
  }

  /**
   * Opens a selection of {@link INewsMark} inside the feed view.
   *
   * @param page
   * @param selection
   */
  public static void openInFeedView(IWorkbenchPage page, IStructuredSelection selection) {
    List<?> list = selection.toList();
    boolean activateEditor = OpenStrategy.activateOnOpen();
    int openedEditors = 0;
    int maxOpenEditors = EditorUtils.getOpenEditorLimit();
    boolean reuseFeedView = Owl.getPreferenceService().getGlobalScope().getBoolean(DefaultPreferences.ALWAYS_REUSE_FEEDVIEW);

    /* Open Editors for the given Selection */
    for (int i = 0; i < list.size() && openedEditors < maxOpenEditors; i++) {
      Object object = list.get(i);
      if (object instanceof ILabel) {
    	  object = ((ILabel)object).getSearchMark();
      }
      if (object instanceof INewsMark) {
        INewsMark mark = ((INewsMark) object);

        /* Open in existing Feedview if set */
        if (reuseFeedView) {
          FeedView activeFeedView = OwlUI.getFirstActiveFeedView();
          if (activeFeedView != null) {
            activeFeedView.setInput(new FeedViewInput(mark));
            break;
          }
        }

        /* Otherwise simply open */
        try {
          page.openEditor(new FeedViewInput(mark), FeedView.ID, activateEditor);
          openedEditors++;
        } catch (PartInitException e) {
          Activator.getDefault().getLog().log(e.getStatus());
        }
      }
    }
  }
  
  /**
   * Set's the checked state of all visible items to the suplied one.
   *
   * @param tree
   * @param state
   */
  public static void setAllChecked(Tree tree, boolean state) {
    setAllChecked(state, tree.getItems());
  }

  private static void setAllChecked(boolean state, TreeItem[] items) {
    for (int i = 0; i < items.length; i++) {
      items[i].setChecked(state);
      TreeItem[] children = items[i].getItems();
      setAllChecked(state, children);
    }
  }
}