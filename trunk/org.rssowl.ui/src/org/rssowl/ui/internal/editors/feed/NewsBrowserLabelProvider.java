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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.rssowl.core.persist.IAttachment;
import org.rssowl.core.persist.ICategory;
import org.rssowl.core.persist.ILabel;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.persist.ISource;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.URIUtils;
import org.rssowl.ui.internal.Activator;
import org.rssowl.ui.internal.EntityGroup;
import org.rssowl.ui.internal.ILinkHandler;
import org.rssowl.ui.internal.OwlUI;
import org.rssowl.ui.internal.util.ExpandingReader;
import org.rssowl.ui.internal.util.ModelUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.net.URI;
import java.text.DateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author bpasero
 */
public class NewsBrowserLabelProvider extends LabelProvider {

  /* Date Formatter for News */
  private DateFormat fDateFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT);

  /* TODO Experimenteal Search Result Highlight */
  private static final String PRE_HIGHLIGHT = "<span style=\"background-color:rgb(255,255,0)\">";
  private static final String POST_HIGHLIGHT = "</span>";

  private String fNewsFontFamily;
  private String fNormalFontCSS;
  private String fSmallFontCSS;
  private String fBiggerFontCSS;
  private String fBiggestFontCSS;
  private String fStickyBGColorCSS;
  private IPropertyChangeListener fPropertyChangeListener;
  private NewsBrowserViewer fViewer;

  /**
   * Creates a new Browser LabelProvider for News
   *
   * @param viewer
   */
  public NewsBrowserLabelProvider(NewsBrowserViewer viewer) {
    fViewer = viewer;
    createFonts();
    createColors();
    registerListeners();
  }

  /*
   * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    unregisterListeners();
  }

  private void registerListeners() {

    /* Create Property Listener */
    fPropertyChangeListener = new IPropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (OwlUI.NEWS_TEXT_FONT_ID.equals(property))
          createFonts();
        else if (OwlUI.STICKY_BG_COLOR_ID.equals(property))
          createColors();
      }
    };

    /* Add it to listen to Theme Events */
    PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fPropertyChangeListener);
  }

  private void unregisterListeners() {
    PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fPropertyChangeListener);
  }

  /* Init the Theme Font (from UI Thread) */
  private void createFonts() {
    int fontHeight = 10;
    Font newsFont = OwlUI.getThemeFont(OwlUI.NEWS_TEXT_FONT_ID, SWT.NORMAL);
    FontData[] fontData = newsFont.getFontData();
    if (fontData.length > 0) {
      fNewsFontFamily = fontData[0].getName();
      fontHeight = fontData[0].getHeight();
    }

    int normal = fontHeight;
    int small = normal - 1;
    int bigger = normal + 1;
    int biggest = bigger + 6;

    String fontUnit = "pt";
    fNormalFontCSS = "font-size: " + normal + fontUnit + ";";
    fSmallFontCSS = "font-size: " + small + fontUnit + ";";
    fBiggerFontCSS = "font-size: " + bigger + fontUnit + ";";
    fBiggestFontCSS = "font-size: " + biggest + fontUnit + ";";
  }

  /* Init the Theme Color (from UI Thread) */
  private void createColors() {
    RGB stickyRgb = OwlUI.getThemeRGB(OwlUI.STICKY_BG_COLOR_ID, new RGB(255, 255, 128));
    fStickyBGColorCSS = "background-color: rgb(" + stickyRgb.red + "," + stickyRgb.green + "," + stickyRgb.blue + ");";
  }

  /*
   * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
   */
  @Override
  @SuppressWarnings("nls")
  public String getText(Object element) {

    /* Return HTML for a Group */
    if (element instanceof EntityGroup)
      return getLabel((EntityGroup) element);

    /* Return HTML for a News */
    else if (element instanceof INews)
      return getLabel((INews) element);

    return ""; //$NON-NLS-1$
  }

  /**
   * Writes the CSS information to the given Writer.
   *
   * @param writer the writer to add the CSS information to.
   * @throws IOException In case of an error while writing.
   */
  public void writeCSS(Writer writer) throws IOException {

    /* Open CSS */
    writer.write("<style type=\"text/css\">\n");

    /* General */
    writer.append("body { overflow: auto; margin: 0 0 10px 0; font-family: ").append(fNewsFontFamily).append(",Verdanna,sans-serif; }\n");
    writer.write("a { color: #009; text-decoration: none; }\n");
    writer.write("a:hover { color: #009; text-decoration: underline; }\n");
    writer.write("a:visited { color: #009; text-decoration: none; }\n");
    writer.write("img { border: none; }\n");

    /* Group */
    writer.append("div.group { color: #678; ").append(fBiggestFontCSS).append(" font-weight: bold; padding: 0 15px 5px 15px; }\n");

    /* Main DIV per Item */
    writer.write("div.newsitem { margin: 10px 10px 30px 10px; border: dotted 1px silver; }\n");

    /* Main DIV Item Areas */
    writer.write("div.header { padding: 10px; background-color: #eee; }\n");
    writer.write("div.content { \n");
    writer.write("   padding: 15px 10px 15px 10px; border-top: dotted 1px silver; \n");
    writer.append("  background-color: #fff; clear: both; ").append(fNormalFontCSS).append("\n");
    writer.write("}\n");
    writer.write("div.footer { background-color: rgb(248,248,248); padding: 5px 10px 5px 10px; line-height: 20px; border-top: dotted 1px silver; }\n");

    /* Restrict the style of embedded Paragraphs */
    writer.write("div.content p { margin-top: 0; padding-top: 0; margin-left: 0; padding-left: 0; }\n");

    /* Title */
    writer.append("div.title { padding-bottom: 6px; ").append(fBiggerFontCSS).append(" }\n");

    writer.write("div.title a { color: #009; text-decoration: none; }\n");
    writer.write("div.title a.unread { font-weight: bold; }\n");
    writer.append("div.title a.readsticky { ").append(fStickyBGColorCSS).append(" }\n");
    writer.append("div.title a.unreadsticky { font-weight: bold; ").append(fStickyBGColorCSS).append(" }\n");
    writer.write("div.title a:hover { color: #009; text-decoration: underline; }\n");
    writer.write("div.title a:visited { color: #009; text-decoration: underline; }\n");

    writer.write("div.title span.unread { font-weight: bold; }\n");
    writer.append("div.title span.readsticky { ").append(fStickyBGColorCSS).append(" }\n");
    writer.append("div.title span.unreadsticky { font-weight: bold; ").append(fStickyBGColorCSS).append(" }\n");

    /* Date */
    writer.append("div.date { float: left; ").append(fSmallFontCSS).append(" }\n");

    /* Author */
    writer.append("div.author { text-align: right; ").append(fSmallFontCSS).append(" }\n");

    /* Attachments */
    writer.append("div.attachments { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.attachments span.label { float: left; padding-right: 5px; }\n");
    writer.write("div.attachments a { color: #009; text-decoration: none; }\n");
    writer.write("div.attachments a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.attachments a:hover { text-decoration: underline; }\n");

    /* Label */
    writer.append("div.label { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.label span.label {float: left; padding-right: 5px; }\n");

    /* Categories */
    writer.append("div.categories { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.categories span.label { float: left; padding-right: 5px; }\n");
    writer.write("div.categories a { color: #009; text-decoration: none; }\n");
    writer.write("div.categories a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.categories a:hover { text-decoration: underline; }\n");

    /* Source */
    writer.append("div.source { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.source span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.source a { color: #009; text-decoration: none; }\n");
    writer.write("div.source a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.source a:hover { text-decoration: underline; }\n");

    /* Comments */
    writer.append("div.comments { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.comments span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.comments a { color: #009; text-decoration: none; }\n");
    writer.write("div.comments a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.comments a:hover { text-decoration: underline; }\n");

    /* Search Related */
    writer.append("div.searchrelated { clear: both; ").append(fSmallFontCSS).append(" }\n");
    writer.write("div.searchrelated span.label {float: left; padding-right: 5px; }\n");
    writer.write("div.searchrelated a { color: #009; text-decoration: none; }\n");
    writer.write("div.searchrelated a:visited { color: #009; text-decoration: none; }\n");
    writer.write("div.searchrelated a:hover { text-decoration: underline; }\n");

    /* Quotes */
    writer.write("span.quote_lvl1 { color: #660066; }\n");
    writer.write("span.quote_lvl2 { color: #007777; }\n");
    writer.write("span.quote_lvl3 { color: #3377ff; }\n");
    writer.write("span.quote_lvl4 { color: #669966; }\n");

    writer.write("</style>\n");
  }

  private String getLabel(EntityGroup group) {
    StringBuilder builder = new StringBuilder();

    /* DIV: Group */
    div(builder, "group");

    builder.append(group.getName());

    /* Close: Group */
    close(builder, "div");

    return builder.toString();
  }

  private StringBuilder getBuilder(INews news, String description) {
    int capacity = 0;

    if (news.getTitle() != null)
      capacity += news.getTitle().length();

    if (description != null)
      capacity += description.length();

    return new StringBuilder(capacity);
  }

  private String getLabel(INews news) {
    String description = news.getDescription();
    StringBuilder builder = getBuilder(news, description);
    StringBuilder search = new StringBuilder();

    String newsTitle = ModelUtils.getHeadline(news);
    boolean hasLink = news.getLinkAsText() != null;
    State state = news.getState();
    boolean isUnread = (state == State.NEW || state == State.UPDATED || state == State.UNREAD);
    Set<ILabel> labels = news.getLabels();
    String color = !labels.isEmpty() ? labels.iterator().next().getColor() : null;

    /* DIV: NewsItem */
    div(builder, "newsitem");

    /* DIV: NewsItem/Header */
    div(builder, "header");

    /* News Title */
    {

      /* DIV: NewsItem/Header/Title */
      div(builder, "title");

      String cssClass = isUnread ? "unread" : "read";
      if (news.isFlagged())
        cssClass = cssClass + "sticky";

      /* Link */
      if (hasLink)
        link(builder, news.getLinkAsText(), newsTitle, cssClass, color);

      /* Normal */
      else
        span(builder, newsTitle, cssClass, color);

      /* Close: NewsItem/Header/Title */
      close(builder, "div");
    }

    /* News Date */
    {

      /* DIV: NewsItem/Header/Date */
      div(builder, "date");

      builder.append(fDateFormat.format(DateUtils.getRecentDate(news)));

      /* Close: NewsItem/Header/Date */
      close(builder, "div");
    }

    /* News Author */
    {
      IPerson author = news.getAuthor();

      /* DIV: NewsItem/Header/Author */
      div(builder, "author");

      if (author != null) {
        String name = author.getName();
        String email = (author.getEmail() != null) ? author.getEmail().toASCIIString() : null;
        if (email != null && !email.contains("mail:"))
          email = "mailto:" + email;

        /* Use name as email if valid */
        if (email == null && name.contains("@") && !name.contains(" "))
          email = name;

        if (StringUtils.isSet(name) && email != null)
          link(builder, email, name, "author");
        else if (StringUtils.isSet(name))
          builder.append(name);
        else if (email != null)
          link(builder, email, email, "author");
        else
          builder.append("&nbsp;");

        /* Add to Search */
        String value = StringUtils.isSet(name) ? name : email;
        if (StringUtils.isSet(value)) {
          String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.AUTHOR_HANDLER_ID + "?" + URIUtils.urlEncode(value);
          link(search, link, value, "searchrelated");
          search.append(", ");
        }
      } else
        builder.append("&nbsp;");

      /* Close: NewsItem/Header/Author */
      close(builder, "div");
    }

    /* Close: NewsItem/Header */
    close(builder, "div");

    /* News Content */
    {

      /* DIV: NewsItem/Content */
      div(builder, "content");

      builder.append(StringUtils.isSet(description) ? description : "This article does not provide any content.");

      /* Close: NewsItem/Content */
      close(builder, "div");
    }

    /* News Footer */
    {
      StringBuilder footer = new StringBuilder();
      boolean hasFooter = false;

      /* DIV: NewsItem/Footer */
      div(footer, "footer");

      /* Attachments */
      List<IAttachment> attachments = news.getAttachments();
      if (attachments.size() != 0) {
        hasFooter = true;

        /* DIV: NewsItem/Footer/Attachments */
        div(footer, "attachments");

        /* Label */
        span(footer, attachments.size() == 1 ? "Attachment:" : "Attachments:", "label");

        /* For each Attachment */
        boolean strip = false;
        for (IAttachment attachment : attachments) {
          if (attachment.getLink() != null) {
            strip = true;
            URI link = attachment.getLink();
            String name = URIUtils.getFile(link);
            if (!StringUtils.isSet(name))
              name = link.toASCIIString();

            //TODO Consider Attachment length and type
            link(footer, link.toASCIIString(), name, "attachment");
            footer.append(", ");
          }
        }

        if (strip)
          footer.delete(footer.length() - 2, footer.length());

        /* Close: NewsItem/Footer/Attachments */
        close(footer, "div");
      }

      /* Label */
      if (!labels.isEmpty()) {
        hasFooter = true;

        /* DIV: NewsItem/Footer/Label */
        div(footer, "label");

        /* Label */
        span(footer, "Label:", "label");

        /* Append Labels to Footer */
        for (ILabel label : labels) {
          String labelColor = label.getColor();
          span(footer, label.getName(), "label", labelColor);
        }

        /* Close: NewsItem/Footer/Label */
        close(footer, "div");

        /* Add to Search */
        for (ILabel label : labels) {
          String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.LABEL_HANDLER_ID + "?" + URIUtils.urlEncode(label.getName());
          link(search, link, label.getName(), "searchrelated");
          search.append(", ");
        }
      }

      /* Categories */
      List<ICategory> categories = news.getCategories();
      if (categories.size() > 0) {
        StringBuilder categoriesText = new StringBuilder();
        boolean hasCategories = false;

        /* DIV: NewsItem/Footer/Categories */
        div(categoriesText, "categories");

        /* Label */
        span(categoriesText, categories.size() == 1 ? "Category:" : "Categories:", "label");

        /* For each Category */
        for (ICategory category : categories) {
          String name = category.getName();
          String domain = category.getDomain();

          /* As Link */
          if (URIUtils.looksLikeLink(domain) && StringUtils.isSet(name)) {
            link(categoriesText, domain, name, "category");
            hasCategories = true;
          }

          /* As Text */
          else if (StringUtils.isSet(name)) {
            categoriesText.append(name);
            hasCategories = true;
          }

          /* Separate with colon */
          categoriesText.append(", ");

          /* Add to Search */
          if (StringUtils.isSet(name)) {
            String link = ILinkHandler.HANDLER_PROTOCOL + NewsBrowserViewer.CATEGORY_HANDLER_ID + "?" + URIUtils.urlEncode(name);
            link(search, link, name, "searchrelated");
            search.append(", ");
          }
        }

        if (hasCategories)
          categoriesText.delete(categoriesText.length() - 2, categoriesText.length());

        /* Close: NewsItem/Footer/Categories */
        close(categoriesText, "div");

        /* Append categories if provided */
        if (hasCategories) {
          hasFooter = true;
          footer.append(categoriesText);
        }
      }

      /* Source */
      ISource source = news.getSource();
      if (source != null) {
        hasFooter = true;
        String link = (source.getLink() != null) ? source.getLink().toASCIIString() : null;
        String name = source.getName();

        /* DIV: NewsItem/Footer/Source */
        div(footer, "source");

        /* Label */
        span(footer, "Source:", "label");

        if (StringUtils.isSet(name) && link != null)
          link(footer, link, name, "source");
        else if (link != null)
          link(footer, link, link, "source");
        else if (StringUtils.isSet(name))
          footer.append(name);

        /* Close: NewsItem/Footer/Source */
        close(footer, "div");
      }

      /* Comments */
      if (StringUtils.isSet(news.getComments()) && news.getComments().trim().length() > 0) {
        hasFooter = true;
        String comments = news.getComments();

        /* DIV: NewsItem/Footer/Comments */
        div(footer, "comments");

        /* Label */
        span(footer, "Comments:", "label");

        if (URIUtils.looksLikeLink(comments))
          link(footer, comments, "Read", "comments");
        else
          footer.append(comments);

        /* Close: NewsItem/Footer/Comments */
        close(footer, "div");
      }

      /* Find related News */
      if (search.length() > 0) {
        hasFooter = true;
        search.delete(search.length() - 2, search.length());

        /* DIV: NewsItem/Footer/SearchRelated */
        div(footer, "searchrelated");

        /* Label */
        span(footer, "Search related News:", "label");

        /* Append to Footer */
        footer.append(search);

        /* Close: NewsItem/Footer/SearchRelated */
        close(footer, "div");
      }

      /* Close: NewsItem/Footer */
      close(footer, "div");

      /* Append if provided */
      if (hasFooter)
        builder.append(footer);
    }

    /* Close: NewsItem */
    close(builder, "div");

    String result = builder.toString();

    /* Highlight Support */
    Collection<String> wordsToHighlight = fViewer.getHighlightedWords();
    if (!wordsToHighlight.isEmpty()) {
      StringBuilder highlightedResult = new StringBuilder(result.length());
      ExpandingReader resultHighlightReader = new ExpandingReader(new StringReader(result), wordsToHighlight, PRE_HIGHLIGHT, POST_HIGHLIGHT, true);

      int len = 0;
      char[] buf = new char[1000];
      try {
        while ((len = resultHighlightReader.read(buf)) != -1)
          highlightedResult.append(buf, 0, len);

        return highlightedResult.toString();
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }

    return result;
  }

  private void div(StringBuilder builder, String cssClass) {
    builder.append("<div class=\"").append(cssClass).append("\">\n");
  }

  private void close(StringBuilder builder, String tag) {
    builder.append("</").append(tag).append(">\n");
  }

  private void link(StringBuilder builder, String link, String content, String cssClass) {
    link(builder, link, content, cssClass, null);
  }

  private void link(StringBuilder builder, String link, String content, String cssClass, String color) {
    builder.append("<a href=\"").append(link).append("\" class=\"").append(cssClass).append("\"");

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\"");

    builder.append(">").append(content).append("</a>");
  }

  private void span(StringBuilder builder, String content, String cssClass) {
    span(builder, content, cssClass, null);
  }

  private void span(StringBuilder builder, String content, String cssClass, String color) {
    builder.append("<span class=\"").append(cssClass).append("\"");

    if (color != null)
      builder.append(" style=\"color: rgb(").append(color).append(");\"");

    builder.append(">").append(content).append("</span>\n");
  }
}