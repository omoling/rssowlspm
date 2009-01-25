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

package org.rssowl.ui.internal.util;

import org.eclipse.core.runtime.Assert;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An implementation of {@link Reader} that allows to surround certain words
 * with a pre- and a post-value.
 *
 * @author bpasero
 */
public class ExpandingReader extends Reader {

  /* Pushback Buffer */
  private final StringBuilder fPushed = new StringBuilder();

  /* Temp Buffer */
  private final StringBuilder fBuf = new StringBuilder();

  /* Constructor Values */
  private final Reader fIn;
  private final Collection<StringMatcher> fStringMatcher;
  private final String fPreExpand;
  private final String fPostExpand;
  private final boolean fSkipTags;

  /* Some State */
  private int fPushbackOffset;
  private boolean fInTags;

  /**
   * @param in The reader to wrap around.
   * @param words A List of words to expand.
   * @param preValue The value to put in front of the expanded word.
   * @param postValue The value to put after the expanded word.
   * @param skipTags If <code>true</code>, do not expand when inside Tags.
   */
  public ExpandingReader(Reader in, Collection<String> words, String preValue, String postValue, boolean skipTags) {
    Assert.isNotNull(in);
    Assert.isNotNull(words);
    Assert.isNotNull(preValue);
    Assert.isNotNull(postValue);

    fIn = in;
    fPreExpand = preValue;
    fPostExpand = postValue;
    fSkipTags = skipTags;
    fStringMatcher= new ArrayList<StringMatcher>(words.size());
    for (String word : words) {
      fStringMatcher.add(new StringMatcher(word, true, false));
    }
  }

  /*
   * @see java.io.Reader#read()
   */
  @Override
  public int read() throws IOException {

    /* Read next char (either from Reader or Pushback Buffer) */
    int ch = next();

    /* Still reading from Pushback Buffer, return */
    if (fPushbackOffset != 0)
      return ch;

    /* Skip Tags if required */
    if (fSkipTags && ch == '<')
      fInTags = true;
    else if (fSkipTags && ch == '>')
      fInTags = false;
    if (fSkipTags && (fInTags || ch == '>'))
      return ch;

    /* Read up to the next Word to check for expansion */
    while (true) {

      /* EOF or End of Word */
      if (ch == -1 || isWordTerminator(ch)) {
        String result = fBuf.toString();

        /* Expand if required */
        if (result.length() > 0 && shouldExpand(result))
          result = expand(result);

        /* Fill to Pushback Buffer */
        fPushed.append(result);

        /* Also add current ch, if its not EOF */
        if (ch != -1) {
          fPushed.append((char) ch);

          /* Skip Tags if required */
          if (fSkipTags && ch == '<')
            fInTags = true;
          else if (fSkipTags && ch == '>')
            fInTags = false;
        }

        /* Reset Buffer */
        fBuf.setLength(0);

        /* Read next from Pushback Buffer */
        return next();
      }

      /* Append to Bufffer */
      fBuf.append((char) ch);

      /* Read Next from Reader */
      ch = fIn.read();
    }
  }

  private boolean isWordTerminator(int c) {
    return !Character.isLetterOrDigit(c);
  }

  private boolean shouldExpand(String word) {
    for (StringMatcher matcher : fStringMatcher) {
      if (matcher.match(word))
        return true;
    }

    return false;
  }

  private String expand(String word) {
    return fPreExpand + word + fPostExpand;
  }

  private int next() throws IOException {
    int len = fPushed.length();

    /* Read from Pushback Buffer */
    if (fPushbackOffset < len) {
      int ch = fPushed.charAt(fPushbackOffset);
      fPushbackOffset++;
      return ch;
    }

    /* Reset Pushbackbuffer */
    else if (fPushbackOffset != 0) {
      fPushbackOffset = 0;
      fPushed.setLength(0);
    }

    /* Read from Reader */
    return fIn.read();
  }

  /*
   * @see java.io.Reader#close()
   */
  @Override
  public void close() throws IOException {
    fIn.close();
  }

  /*
   * @see java.io.Reader#read(char[], int, int)
   */
  @Override
  public int read(char cbuf[], int off, int len) throws IOException {
    int i = 0;

    for (i = 0; i < len; i++) {
      int ch = read();
      if (ch == -1)
        break;
      cbuf[off++] = (char) ch;
    }

    if (i == 0) {
      if (len == 0)
        return 0;
      return -1;
    }

    return i;
  }

  //  public static void main(String[] args) throws IOException {
  //    String html = "<div class=\"newsitem\">\r\n" +
  //    		"<div class=\"header\">\r\n" +
  //    		"<div class=\"title\">\r\n" +
  //    		"<a href=\"http://www.rssowl.org/node/199\" class=\"read\">Feed Security and RSSOwl</a></div>\r\n" +
  //    		"<div class=\"date\">\r\n" +
  //    		"Montag, 7. August 2006 21:37</div>\r\n" +
  //    		"<div class=\"author\">\r\n" +
  //    		"bpasero</div>\r\n" +
  //    		"</div>\r\n" +
  //    		"<div class=\"content\">\r\n" +
  //    		"As Nick already mentioned in his <a href=\"http://nick.typepad.com/blog/2006/08/feed_security_a.html\">Blog</a>,  the <a href=\"http://www.blackhat.com/html/bh-usa-06/bh-usa-06-index.html\">Black Hat USA 2006</a> brought up the topic about security issues in newsreaders. RSSOwl was <a href=\"http://news.com.com/2100-1002_3-6102171.html\">mentioned</a> to be vulnerable on malicious JavaScript coming from a newsfeed. I am going to work in that area for upcoming RSSOwl 1.2.2, but if you are really concerned about the Internet Explorer being used, you can always disable it in preferences and use an external Browser instead.\r\n" +
  //    		"\r\n" +
  //    		"For the future I am seriously looking into integrating Mozilla rather than the Internet Explorer.\r\n" +
  //    		"\r\n" +
  //    		"Ben</div>\r\n" +
  //    		"<div class=\"footer\">\r\n" +
  //    		"<div class=\"searchrelated\">\r\n" +
  //    		"<span class=\"label\">Search related News:</span>\r\n" +
  //    		"<a href=\"rssowl://org.rssowl.ui.search.Author?bpasero\" class=\"searchrelated\">bpasero</a></div>\r\n" +
  //    		"</div>\r\n" +
  //    		"</div>\r\n" +
  //    		"";
  //    StringBuilder b = new StringBuilder();
  //    ExpandingReader reader = new ExpandingReader(new StringReader(html), Arrays.asList("explorer"), "<span style=\"background-color:rgb(255,255,0)\">", "</span>", true);
  //
  //    int i = 0;
  //    while ((i = reader.read()) != -1)
  //      b.append((char) i);
  //
  //    System.out.println(b);
  //  }
}