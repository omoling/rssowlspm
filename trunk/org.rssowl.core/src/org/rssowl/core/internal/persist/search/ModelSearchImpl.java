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

package org.rssowl.core.internal.persist.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.NumberTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreRangeQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rssowl.core.Owl;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.service.DBHelper;
import org.rssowl.core.internal.persist.service.EntityIdsByEventType;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.persist.IFolder;
import org.rssowl.core.persist.IGuid;
import org.rssowl.core.persist.IMark;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.INewsBin;
import org.rssowl.core.persist.ISearchCondition;
import org.rssowl.core.persist.ISearchField;
import org.rssowl.core.persist.ISearchValueType;
import org.rssowl.core.persist.SearchSpecifier;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.BookMarkReference;
import org.rssowl.core.persist.reference.FolderReference;
import org.rssowl.core.persist.reference.NewsBinReference;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.IndexListener;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.SearchHit;
import org.rssowl.core.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The central interface for searching types from the persistence layer. The
 * implementation is contributable via extension-point mechanism.
 *
 * @author ijuma
 * @author bpasero
 */
public class ModelSearchImpl implements IModelSearch {

  /* Maximum Date as String */
  private static final String MAX_DATE = DateTools.dateToString(new Date(Long.MAX_VALUE), Resolution.DAY);

  /* Minimum Date as String */
  private static final String MIN_DATE = DateTools.dateToString(new Date(0), Resolution.DAY);

  /* Maximum Number as String */
  private static final String MAX_NUMBER = NumberTools.longToString(Long.MAX_VALUE);

  /* Minimum Number as String */
  private static final String MIN_NUMBER = NumberTools.longToString(Long.MIN_VALUE);

  /* One Day in Millis */
  private static final Long DAY = 1000 * 3600 * 24L;

  /* Cached News States */
  private static final INews.State[] NEWS_STATES = INews.State.values();

  /* Wildcard matching any String */
  private static final char STRING_WILDCARD = '*';

  /* Wildcard matching any Char */
  private static final char CHAR_WILDCARD = '?';

  private volatile IndexSearcher fSearcher;
  private volatile Indexer fIndexer;
  private volatile Directory fDirectory;
  private final List<IndexListener> fIndexListeners = new CopyOnWriteArrayList<IndexListener>();
  private final Map<IndexSearcher, AtomicInteger> fSearchers = new ConcurrentHashMap<IndexSearcher, AtomicInteger>(3, 0.75f, 1);

  /*
   * @see org.rssowl.core.model.search.IModelSearch#startup()
   */
  public void startup() throws PersistenceException {
    try {
      if (fDirectory == null) {
        String path = Activator.getDefault().getStateLocation().toOSString();
        LockFactory lockFactory = new NativeFSLockFactory(path);
        fDirectory = FSDirectory.getDirectory(path, lockFactory);
      }

      if (fIndexer == null)
        fIndexer = new Indexer(this, fDirectory);

      fIndexer.initIfNecessary();

      synchronized (this) {
        if (fSearcher == null)
          fSearcher = createIndexSearcher();
      }
    } catch (IOException e) {
      Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
    }
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#shutdown()
   */
  public void shutdown(boolean emergency) throws PersistenceException {
    try {
      /*
       * Close fIndexer first because it's more important (reduces the chance of
       * a corrupt index). Can be null if exception thrown during start-up
       */
      if (fIndexer != null)
        fIndexer.shutdown(emergency);

      /*
       * We don't bother to close searchers if it's an emergency. They will be
       * released when the process exits.
       */
      if (emergency)
        return;

      synchronized (this) {
        /* We first close all the searchers whose refCount is 0 */
        for (Map.Entry<IndexSearcher, AtomicInteger> mapEntry : fSearchers.entrySet()) {
          if (mapEntry.getValue().get() == 0)
            dispose(mapEntry.getKey());
        }
        while (!fSearchers.isEmpty()) {
          try {
            /*
             * We sleep with a lock held because the Threads that we're waiting
             * to make progress don't acquire a lock
             */
            Thread.sleep(50);
          } catch (InterruptedException e) {
            /* If interrupted, we just leave the rest of the searchers open */
            return;
          }
          /* Try again for the ones that are left */
          for (Map.Entry<IndexSearcher, AtomicInteger> mapEntry : fSearchers.entrySet()) {
            if (mapEntry.getValue().get() == 0)
              dispose(mapEntry.getKey());
          }
        }
        fSearcher = null;
      }
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  private BooleanClause createIsCopyTermQuery(boolean copy) {
    String field = String.valueOf(INews.PARENT_ID);
    TermQuery termQuery = new TermQuery(new Term(field, NumberTools.longToString(0)));
    Occur occur = copy ? Occur.MUST_NOT : Occur.MUST;
    return new BooleanClause(termQuery, occur);
  }

  private static final class SimpleHitCollector extends HitCollector {

    private final IndexSearcher fSearcher;
    private final List<NewsReference> fResultList;

    SimpleHitCollector(IndexSearcher searcher, List<NewsReference> resultList) {
      fSearcher = searcher;
      fResultList = resultList;
    }

    @Override
    public void collect(int doc, float score) {
      try {
        Document document = fSearcher.doc(doc);

        /* Receive Stored Fields */
        long newsId = Long.parseLong(document.get(SearchDocument.ENTITY_ID_TEXT));

        /* Add to List */
        fResultList.add(new NewsReference(newsId));
      } catch (IOException e) {
        Activator.getDefault().logError(e.getMessage(), e);
      }
    }
  }

  /**
   * @param guids the List of {@link IGuid} to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search and
   * grouped by {@link IGuid}.
   */
  public Map<IGuid, List<NewsReference>> searchNewsByGuids(List<IGuid> guids, boolean copy) {
    Map<IGuid, List<NewsReference>> linkToRefs = new HashMap<IGuid, List<NewsReference>>(guids.size());
    IndexSearcher currentSearcher = getCurrentSearcher();
    try {
      for (IGuid guid : guids) {
        BooleanQuery query = createGuidQuery(guid, copy);
        List<NewsReference> newsRefs = simpleSearch(currentSearcher, query);
        if (!newsRefs.isEmpty())
          linkToRefs.put(guid, newsRefs);
      }
      return linkToRefs;
    } finally {
      disposeIfNecessary(currentSearcher);
    }
  }

  /**
   * @param links The Links to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search and
   * grouped by the {@link URI}.
   */
  public Map<URI, List<NewsReference>> searchNewsByLinks(List<URI> links, boolean copy) {
    Map<URI, List<NewsReference>> linkToRefs = new HashMap<URI, List<NewsReference>>(links.size());
    IndexSearcher currentSearcher = getCurrentSearcher();
    try {
      for (URI link : links) {
        BooleanQuery query = createNewsByLinkBooleanQuery(link, copy);
        List<NewsReference> newsRefs = simpleSearch(currentSearcher, query);
        if (!newsRefs.isEmpty())
          linkToRefs.put(link, newsRefs);
      }
      return linkToRefs;
    } finally {
      disposeIfNecessary(currentSearcher);
    }
  }

  /**
   * @param link The Link to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search.
   */
  public List<NewsReference> searchNewsByLink(URI link, boolean copy) {
    Assert.isNotNull(link, "link");
    BooleanQuery query = createNewsByLinkBooleanQuery(link, copy);
    return simpleSearch(query);
  }

  private BooleanQuery createNewsByLinkBooleanQuery(URI link, boolean copy) {
    BooleanQuery query = new BooleanQuery(true);
    query.add(new TermQuery(new Term(String.valueOf(INews.LINK), link.toString().toLowerCase())), Occur.MUST);
    query.add(createIsCopyTermQuery(copy));
    return query;
  }

  /**
   * @param guid the {@link IGuid} to search news for.
   * @param copy If <code>true</code>, only consider copied News.
   * @return a List of {@link NewsReference} matching the given search.
   */
  public List<NewsReference> searchNewsByGuid(IGuid guid, boolean copy) {
    Assert.isNotNull(guid, "guid");
    BooleanQuery query = createGuidQuery(guid, copy);
    return simpleSearch(query);
  }

  private BooleanQuery createGuidQuery(IGuid guid, boolean copy) {
    BooleanQuery query = new BooleanQuery(true);
    query.add(new TermQuery(new Term(String.valueOf(INews.GUID), guid.getValue().toLowerCase())), Occur.MUST);
    query.add(createIsCopyTermQuery(copy));
    return query;
  }

  private List<NewsReference> simpleSearch(BooleanQuery query) {
    /* Make sure the searcher is in sync */
    IndexSearcher currentSearcher = getCurrentSearcher();
    try {
      List<NewsReference> newsRefs = simpleSearch(currentSearcher, query);
      return newsRefs;
    } finally {
      disposeIfNecessary(currentSearcher);
    }
  }

  private List<NewsReference> simpleSearch(IndexSearcher currentSearcher, BooleanQuery query) {
    List<NewsReference> resultList = new ArrayList<NewsReference>(2);

    try {
      /* Use custom hit collector for performance reasons */
      /* Perform the Search */
      currentSearcher.search(query, new SimpleHitCollector(currentSearcher, resultList));
      return resultList;
    } catch (IOException e) {
      throw new PersistenceException(e);
    }
  }

  private void disposeIfNecessary(IndexSearcher currentSearcher) {
    AtomicInteger referenceCount = fSearchers.get(currentSearcher);
    if (referenceCount.decrementAndGet() == 0 && fSearcher != currentSearcher) {
      try {
        /*
         * May be called by getCurrentSearcher at the same time, but safe
         * because dispose is safe to be called many times for the same
         * searcher.
         */
        dispose(currentSearcher);
      } catch (IOException e) {
        throw new PersistenceException(e);
      }
    }
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#searchNews(java.util.List,
   * boolean)
   */
  public List<SearchHit<NewsReference>> searchNews(Collection<ISearchCondition> conditions, boolean matchAllConditions) throws PersistenceException {
    try {
      return doSearchNews(conditions, matchAllConditions);
    }

    /* Too Many Clauses - Increase Clauses Limit */
    catch (TooManyClauses e) {

      /* Disable Clauses Limit */
      if (BooleanQuery.getMaxClauseCount() != Integer.MAX_VALUE) {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
        return doSearchNews(conditions, matchAllConditions);
      }

      /* Maximum reached */
      throw new PersistenceException("Please avoid using the wildcards '*' and '?' as standalone terms.", e);
    }
  }

  private List<SearchHit<NewsReference>> doSearchNews(Collection<ISearchCondition> conditions, boolean matchAllConditions) throws PersistenceException {

    /* Perform the search */
    try {
      BooleanQuery bQuery = new BooleanQuery();

      /* Handle State-Field separately (group) */
      BooleanQuery statesQuery = null;
      for (ISearchCondition condition : conditions) {
        if (requiresStateGrouping(condition)) {

          /* Create and add new BooleanQuery for State */
          if (statesQuery == null) {
            statesQuery = new BooleanQuery();
            bQuery.add(statesQuery, matchAllConditions ? Occur.MUST : Occur.SHOULD);
          }

          /* Add Boolean Clause per State */
          addStateClause(statesQuery, condition);
        }
      }

      /* Create a Query for each condition */
      BooleanQuery fieldQuery = null;
      Analyzer analyzer = Indexer.createAnalyzer();
      for (ISearchCondition condition : conditions) {

        /* State Queries already handled */
        if (requiresStateGrouping(condition))
          continue;

        /* Create and add new BooleanQuery for other Fields */
        if (fieldQuery == null) {
          fieldQuery = new BooleanQuery();
          bQuery.add(fieldQuery, matchAllConditions ? Occur.MUST : Occur.SHOULD);
        }

        /* Create the Clause */
        BooleanClause clause = null;
        if (condition.getField().getId() == IEntity.ALL_FIELDS)
          clause = createAllNewsFieldsClause(analyzer, condition, matchAllConditions);
        else
          clause = createBooleanClause(analyzer, condition, matchAllConditions);

        /* Check if the Clause has any valid Query */
        Query query = clause.getQuery();
        if (query instanceof BooleanQuery && ((BooleanQuery) query).clauses().isEmpty())
          continue;

        /*
         * Specially treat this case where the specifier is a negation but any
         * of the supplied conditions should match in the result set.
         */
        if (condition.getSpecifier().isNegation() && !matchAllConditions) {
          BooleanQuery nestedquery = new BooleanQuery();
          nestedquery.add(clause);
          nestedquery.add(new BooleanClause(new MatchAllDocsQuery(), Occur.MUST));
          fieldQuery.add(new BooleanClause(nestedquery, Occur.SHOULD));
        }

        /* Normal Case */
        else {
          fieldQuery.add(clause);
        }
      }

      /* Add the MatchAllDocsQuery (MUST_NOT is used, All Conditions match) */
      if (fieldQuery != null && matchAllConditions) {
        boolean requireAllDocsQuery = true;
        BooleanClause[] clauses = fieldQuery.getClauses();
        for (BooleanClause clause : clauses) {
          if (clause.getOccur() != Occur.MUST_NOT) {
            requireAllDocsQuery = false;
            break;
          }
        }

        /* Add if required */
        if (requireAllDocsQuery)
          fieldQuery.add(new BooleanClause(new MatchAllDocsQuery(), Occur.MUST));
      }

      /* Make sure the searcher is in sync */
      final IndexSearcher currentSearcher = getCurrentSearcher();
      final List<SearchHit<NewsReference>> resultList = new ArrayList<SearchHit<NewsReference>>();

      /* Use custom hit collector for performance reasons */
      HitCollector collector = new HitCollector() {
        @Override
        public void collect(int doc, float score) {
          try {
            Document document = currentSearcher.doc(doc);

            /* Receive Stored Fields */
            long newsId = Long.parseLong(document.get(SearchDocument.ENTITY_ID_TEXT));
            INews.State newsState = NEWS_STATES[Integer.parseInt(document.get(NewsDocument.STATE_ID_TEXT))];

            Map<Integer, INews.State> data = new HashMap<Integer, INews.State>(1);
            data.put(INews.STATE, newsState);

            /* Add to List */
            resultList.add(new SearchHit<NewsReference>(new NewsReference(newsId), score, data));
          } catch (IOException e) {
            Activator.getDefault().logError(e.getMessage(), e);
          }
        }
      };

      /* Perform the Search */
      try {
        currentSearcher.search(bQuery, collector);
        return resultList;
      } finally {
        disposeIfNecessary(currentSearcher);
      }
    } catch (IOException e) {
      throw new PersistenceException("Error searching news", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void addStateClause(BooleanQuery statesQuery, ISearchCondition condition) {
    String fieldName = String.valueOf(INews.STATE);
    Occur occur = condition.getSpecifier().isNegation() ? Occur.MUST_NOT : Occur.SHOULD;
    EnumSet<INews.State> newsStates = (EnumSet<State>) condition.getValue();
    for (INews.State state : newsStates) {
      String value = String.valueOf(state.ordinal());
      TermQuery stateQuery = new TermQuery(new Term(fieldName, value));
      statesQuery.add(new BooleanClause(stateQuery, occur));
    }

    /* Check if the match-all-docs query is required */
    if (condition.getSpecifier().isNegation())
      statesQuery.add(new BooleanClause(new MatchAllDocsQuery(), Occur.MUST));
  }

  private boolean requiresStateGrouping(ISearchCondition condition) {
    return condition.getField().getId() == INews.STATE;
  }

  private BooleanClause createAllNewsFieldsClause(Analyzer analyzer, ISearchCondition condition, boolean matchAllConditions) throws IOException {
    IModelFactory factory = Owl.getModelFactory();
    BooleanQuery allFieldsQuery = new BooleanQuery();

    /* Require all words to be contained or not contained */
    if (condition.getSpecifier() == SearchSpecifier.CONTAINS_ALL) {
      List<ISearchCondition> tokenConditions = new ArrayList<ISearchCondition>();

      List<String> tokens = StringUtils.tokenizePhraseAware((String) condition.getValue());
      for (String token : tokens) {
        ISearchCondition tokenCondition = factory.createSearchCondition(condition.getField(), condition.getSpecifier(), token);

        /* Rewrite Specifier */
        if (condition.getSpecifier() == SearchSpecifier.CONTAINS_ALL)
          tokenCondition.setSpecifier(SearchSpecifier.CONTAINS);
        else
          tokenCondition.setSpecifier(SearchSpecifier.CONTAINS_NOT);

        tokenConditions.add(tokenCondition);
      }

      /* Build custom Query out of Conditions */
      for (ISearchCondition tokenCondition : tokenConditions) {
        BooleanClause tokenClause = createAllNewsFieldsClause(analyzer, tokenCondition, matchAllConditions);

        /* Ignore empty clauses (e.g. due to Stop Words) */
        if (tokenClause.getQuery() instanceof BooleanQuery && ((BooleanQuery) tokenClause.getQuery()).getClauses().length == 0)
          continue;

        tokenClause.setOccur(Occur.MUST);
        allFieldsQuery.add(tokenClause);
      }
    }

    /* Require any word to be contained or not contained */
    else {
      List<ISearchCondition> allFieldsConditions = new ArrayList<ISearchCondition>(5);

      /* Title */
      ISearchField field = factory.createSearchField(INews.TITLE, condition.getField().getEntityName());
      allFieldsConditions.add(factory.createSearchCondition(field, condition.getSpecifier(), condition.getValue()));

      /* Description */
      field = factory.createSearchField(INews.DESCRIPTION, condition.getField().getEntityName());
      allFieldsConditions.add(factory.createSearchCondition(field, condition.getSpecifier(), condition.getValue()));

      /* Author */
      field = factory.createSearchField(INews.AUTHOR, condition.getField().getEntityName());
      allFieldsConditions.add(factory.createSearchCondition(field, condition.getSpecifier(), condition.getValue()));

      /* Category */
      field = factory.createSearchField(INews.CATEGORIES, condition.getField().getEntityName());
      StringTokenizer tokenizer = new StringTokenizer(condition.getValue().toString());
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        allFieldsConditions.add(factory.createSearchCondition(field, condition.getSpecifier().isNegation() ? SearchSpecifier.IS_NOT : SearchSpecifier.IS, token));
      }
      allFieldsConditions.add(factory.createSearchCondition(field, condition.getSpecifier().isNegation() ? SearchSpecifier.IS_NOT : SearchSpecifier.IS, condition.getValue()));

      /* Attachment Content */
      field = factory.createSearchField(INews.ATTACHMENTS_CONTENT, condition.getField().getEntityName());
      allFieldsConditions.add(factory.createSearchCondition(field, condition.getSpecifier(), condition.getValue()));

      /* Create Clauses out of Conditions */
      boolean anyClauseIsEmpty = false;
      List<BooleanClause> clauses = new ArrayList<BooleanClause>();
      for (ISearchCondition allFieldCondition : allFieldsConditions) {
        BooleanClause clause = createBooleanClause(analyzer, allFieldCondition, matchAllConditions);
        clause.setOccur(Occur.SHOULD);
        clauses.add(clause);

        /* Ignore empty clauses (e.g. due to Stop Words) */
        if (clause.getQuery() instanceof BooleanQuery && ((BooleanQuery) clause.getQuery()).getClauses().length == 0)
          anyClauseIsEmpty = true;
      }

      /* Only add if none of the clauses is empty */
      if (!anyClauseIsEmpty) {
        for (BooleanClause clause : clauses) {
          allFieldsQuery.add(clause);
        }
      }
    }

    /* Determine Occur (MUST, SHOULD, MUST NOT) */
    Occur occur = getOccur(condition.getSpecifier(), matchAllConditions);
    return new BooleanClause(allFieldsQuery, occur);
  }

  /*
   * Will fallback to a TermQuery if the search-term is not valid for a
   * WildcardQuery
   */
  private Query createWildcardQuery(String field, String term) {
    if (String.valueOf(INews.LABEL).equals(field) || isValidWildcardTerm(term))
      return new WildcardQuery(new Term(field, term));

    return new TermQuery(new Term(field, term));
  }

  private boolean isValidWildcardTerm(String term) {
    for (int i = 0; i < term.length(); i++) {
      char charAtIndex = term.charAt(i);
      if (charAtIndex != STRING_WILDCARD && charAtIndex != CHAR_WILDCARD)
        return true;
    }

    return false;
  }

  private BooleanClause createBooleanClause(Analyzer analyzer, ISearchCondition condition, boolean matchAllConditions) throws IOException {
    Query query = null;

    /* Separately handle this dynamic Query */
    if (condition.getField().getId() == INews.AGE_IN_DAYS)
      query = createAgeClause(condition);

    /* Separately handle this dynamic Query */
    else if (condition.getField().getId() == INews.LOCATION)
      query = createLocationClause(condition);

    /* Other Fields */
    else {
      try {
        switch (condition.getField().getSearchValueType().getId()) {

          /* Boolean: Simple Term-Query */
          case ISearchValueType.BOOLEAN:
            query = createTermQuery(condition);
            break;

          /* String / Link / Enum: String Query */
          case ISearchValueType.ENUM:
          case ISearchValueType.STRING:
          case ISearchValueType.LINK:
            query = createStringQuery(analyzer, condition);
            break;

          /* Date / Time / DateTime: Date Query (Ranged) */
          case ISearchValueType.DATE:
          case ISearchValueType.TIME:
          case ISearchValueType.DATETIME:
            query = createDateQuery(condition);
            break;

          /* Number / Integer: Number Query (Ranged) */
          case ISearchValueType.NUMBER:
          case ISearchValueType.INTEGER:
            query = createNumberQuery(condition);
        }
      } catch (ParseException e) {
        Activator.getDefault().getLog().log(Activator.getDefault().createErrorStatus(e.getMessage(), e));
      }
    }

    /* In case of the Query not being created, fallback to Term-Query */
    if (query == null) {
      query = createTermQuery(condition);
    }

    /* Determine Occur (MUST, SHOULD, MUST NOT) */
    Occur occur = getOccur(condition.getSpecifier(), matchAllConditions);
    return new BooleanClause(query, occur);
  }

  /* This Clause needs to be generated dynamically */
  private Query createAgeClause(ISearchCondition condition) {
    Integer age = (Integer) condition.getValue();
    String fieldname = String.valueOf(condition.getField().getId());

    /* Calculate Desired Date */
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis() - age * DAY);
    String value = DateTools.dateToString(cal.getTime(), Resolution.DAY);

    switch (condition.getSpecifier()) {
      case IS: {
        return new TermQuery(new Term(fieldname, value));
      }

      case IS_GREATER_THAN: {
        Term lowerBound = new Term(fieldname, MIN_DATE);
        Term upperBound = new Term(fieldname, value);

        return new ConstantScoreRangeQuery(fieldname, lowerBound.text(), upperBound.text(), false, false);
      }

      case IS_LESS_THAN: {
        Term lowerBound = new Term(fieldname, value);
        Term upperBound = new Term(fieldname, MAX_DATE);

        return new ConstantScoreRangeQuery(fieldname, lowerBound.text(), upperBound.text(), false, false);
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Age Query");
  }

  /* This Clause needs to be generated dynamically */
  private Query createLocationClause(ISearchCondition condition) {
    BooleanQuery bQuery = new BooleanQuery();
    Long[][] value = (Long[][]) condition.getValue();

    /* Receive Folders */
    for (int i = 0; value[0] != null && i < value[0].length; i++) {
      if (value[0][i] != null) {
        IFolder folder = new FolderReference(value[0][i]).resolve();
        if (folder != null)
          addFolderLocationClause(bQuery, folder);
      }
    }

    /* Receive BookMarks */
    for (int i = 0; value[1] != null && i < value[1].length; i++) {
      if (value[1][i] != null) {
        IBookMark bookmark = new BookMarkReference(value[1][i]).resolve();
        if (bookmark != null)
          addBookMarkLocationClause(bQuery, bookmark);
      }
    }

    /* Receive NewsBins */
    if (value.length == 3) {
      for (int i = 0; value[2] != null && i < value[2].length; i++) {
        if (value[2][i] != null) {
          INewsBin newsbin = new NewsBinReference(value[2][i]).resolve();
          if (newsbin != null)
            addNewsBinLocationClause(bQuery, newsbin);
        }
      }
    }

    /* The folder could be empty, make sure to add at least 1 Clause */
    if (bQuery.clauses().isEmpty())
      bQuery.add(new TermQuery(new Term(String.valueOf(INews.FEED), "")), Occur.SHOULD);

    return bQuery;
  }

  private void addFolderLocationClause(BooleanQuery bQuery, IFolder folder) {
    if (folder != null) {
      List<IFolder> folders = folder.getFolders();
      List<IMark> marks = folder.getMarks();

      /* Child Folders */
      for (IFolder childFolder : folders)
        addFolderLocationClause(bQuery, childFolder);

      /* BookMarks and Newsbins */
      for (IMark mark : marks)
        if (mark instanceof IBookMark)
          addBookMarkLocationClause(bQuery, (IBookMark) mark);
        else if (mark instanceof INewsBin)
          addNewsBinLocationClause(bQuery, (INewsBin) mark);
    }
  }

  private void addBookMarkLocationClause(BooleanQuery bQuery, IBookMark bookmark) {
    if (bookmark != null) {
      String feed = bookmark.getFeedLinkReference().getLinkAsText().toLowerCase();
      bQuery.add(new TermQuery(new Term(String.valueOf(INews.FEED), feed)), Occur.SHOULD);
    }
  }

  private void addNewsBinLocationClause(BooleanQuery bQuery, INewsBin newsbin) {
    if (newsbin != null)
      bQuery.add(new TermQuery(new Term(String.valueOf(INews.PARENT_ID), NumberTools.longToString(newsbin.getId()))), Occur.SHOULD);
  }

  private Query createStringQuery(Analyzer analyzer, ISearchCondition condition) throws ParseException, IOException {
    SearchSpecifier specifier = condition.getSpecifier();
    String fieldname = String.valueOf(condition.getField().getId());

    /* Retrieve Value */
    String value;
    if (condition.getValue() instanceof Enum)
      value = String.valueOf(((Enum<?>) condition.getValue()).ordinal());
    else
      value = String.valueOf(condition.getValue());

    switch (specifier) {

      /* Create Wildcard-Query */
      case IS:
      case IS_NOT: {
        return createWildcardQuery(fieldname, value.toLowerCase());
      }

        /* Let Query-Parser handle this */
      case CONTAINS:
      case CONTAINS_ALL:
      case CONTAINS_NOT: {
        QueryParser parser = new QueryParser(fieldname, analyzer);
        Operator operator = (specifier == SearchSpecifier.CONTAINS || specifier == SearchSpecifier.CONTAINS_NOT) ? QueryParser.OR_OPERATOR : QueryParser.AND_OPERATOR;
        parser.setDefaultOperator(operator);
        parser.setAllowLeadingWildcard(true);

        /* Prepare the value for parsing */
        value = prepareForParsing(value);

        /* Parse */
        return parser.parse(value);
      }

        /* Wildcard-Query with trailing '*' */
      case BEGINS_WITH: {
        value = value.toLowerCase() + "*";
        return createWildcardQuery(fieldname, value);
      }

        /* Wildcard-Query with leading '*' */
      case ENDS_WITH: {
        value = "*" + value.toLowerCase();
        return createWildcardQuery(fieldname, value);
      }

        /* Fuzzy Query */
      case SIMILIAR_TO: {
        BooleanQuery similarityQuery = new BooleanQuery();

        LowercaseWhitespaceAnalyzer similarAnalyzer = new LowercaseWhitespaceAnalyzer();
        TokenStream tokenStream = similarAnalyzer.tokenStream(String.valueOf(IEntity.ALL_FIELDS), new StringReader(value));
        Token token = null;
        while ((token = tokenStream.next()) != null) {
          String termText = new String(token.termBuffer(), 0, token.termLength());
          Term term = new Term(fieldname, termText);
          similarityQuery.add(new BooleanClause(new FuzzyQuery(term), Occur.MUST));
        }

        return similarityQuery;
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Parsed Queries");
  }

  private Query createTermQuery(ISearchCondition condition) {
    String value;
    if (condition.getValue() instanceof Enum)
      value = String.valueOf(((Enum<?>) condition.getValue()).ordinal());
    else
      value = String.valueOf(condition.getValue());

    String fieldname = String.valueOf(condition.getField().getId());

    Term term = new Term(fieldname, value);
    return new TermQuery(term);
  }

  private Query createDateQuery(ISearchCondition condition) {
    SearchSpecifier specifier = condition.getSpecifier();
    String value = DateTools.dateToString((Date) condition.getValue(), Resolution.DAY);
    String fieldname = String.valueOf(condition.getField().getId());

    switch (specifier) {
      case IS:
      case IS_NOT:
        return new TermQuery(new Term(fieldname, value));

      case IS_AFTER: {
        Term lowerBound = new Term(fieldname, value);
        Term upperBound = new Term(fieldname, MAX_DATE);

        return new ConstantScoreRangeQuery(fieldname, lowerBound.text(), upperBound.text(), false, false);
      }

      case IS_BEFORE: {
        Term lowerBound = new Term(fieldname, MIN_DATE);
        Term upperBound = new Term(fieldname, value);

        return new ConstantScoreRangeQuery(fieldname, lowerBound.text(), upperBound.text(), false, false);
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Date/Time Queries");
  }

  private Query createNumberQuery(ISearchCondition condition) {
    SearchSpecifier specifier = condition.getSpecifier();
    String value = NumberTools.longToString((Integer) condition.getValue());
    String fieldname = String.valueOf(condition.getField().getId());

    switch (specifier) {
      case IS:
      case IS_NOT:
        return new TermQuery(new Term(fieldname, value));

      case IS_GREATER_THAN: {
        Term lowerBound = new Term(fieldname, value);
        Term upperBound = new Term(fieldname, MAX_NUMBER);

        return new ConstantScoreRangeQuery(fieldname, lowerBound.text(), upperBound.text(), false, false);
      }

      case IS_LESS_THAN: {
        Term lowerBound = new Term(fieldname, MIN_NUMBER);
        Term upperBound = new Term(fieldname, value);

        return new ConstantScoreRangeQuery(fieldname, lowerBound.text(), upperBound.text(), false, false);
      }
    }

    throw new UnsupportedOperationException("Unsupported Specifier for Number Queries");
  }

  private IndexSearcher createIndexSearcher() throws CorruptIndexException, IOException {
    IndexSearcher searcher = new IndexSearcher(IndexReader.open(fDirectory));
    fSearchers.put(searcher, new AtomicInteger(0));
    return searcher;
  }

  private IndexSearcher getCurrentSearcher() throws PersistenceException {
    try {
      boolean flushed = fIndexer.flushIfNecessary();

      /* Get the current searcher before acquiring lock in case we block */
      IndexSearcher currentSearcher = fSearcher;

      synchronized (this) {
        /*
         * If there are changes and currentSearcher == fSearcher, it means we
         * won the race for the lock, so we reopen the searcher. If flushed is
         * true, but currentSearcher != fSearcher it means that another thread
         * has reopened the reader while we were blocked waiting for the lock.
         */
        if (flushed && currentSearcher == fSearcher) {
          IndexReader currentReader = fSearcher.getIndexReader();
          IndexReader newReader = currentReader.reopen();
          if (newReader != currentReader) {

            IndexSearcher newSearcher = new IndexSearcher(newReader);
            fSearchers.put(newSearcher, new AtomicInteger(1));

            /*
             * Assign to field before we check the referenceCount to ensure that
             * disposeIfNecessary will dispose the searcher if it has the last
             * reference, is yet to check if fSearcher has been changed (if this
             * was done after referenceCount.get() == 0, we could leak a
             * searcher).
             */
            fSearcher = newSearcher;

            AtomicInteger referenceCount = fSearchers.get(currentSearcher);
            if (referenceCount != null && referenceCount.get() == 0) {
              /*
               * May be called by disposeIfNecessary at the same time, but safe
               * because dispose is safe to be called many times for the same
               * searcher.
               */
              dispose(currentSearcher);
            }

            return fSearcher;
          }
        }
        fSearchers.get(fSearcher).incrementAndGet();
        return fSearcher;
      }
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  private Occur getOccur(SearchSpecifier specifier, boolean matchAllConditions) {
    switch (specifier) {
      case IS_NOT:
      case CONTAINS_NOT:
        return Occur.MUST_NOT;

      default:
        return matchAllConditions ? Occur.MUST : Occur.SHOULD;
    }
  }

  /**
   * Can be called multiple times safely because: - close is safe to be called
   * many times in IndexReader and IndexSearcher - No IndexSearcher is ever
   * added again into the fSearchers map so calling remove two or more times is
   * harmless.
   */
  private void dispose(IndexSearcher searcher) throws IOException {
    fSearchers.remove(searcher);
    searcher.close();
    searcher.getIndexReader().close();
  }

  /*
   * @see org.rssowl.core.model.search.IModelSearch#clearIndex()
   */
  public void clearIndex() throws PersistenceException {
    try {
      synchronized (this) {
        IndexSearcher currentSearcher = fSearcher;
        fIndexer.clearIndex();
        fSearcher = createIndexSearcher();

        /*
         * We block until the current reader has been closed or can be closed.
         * Most times we should be able to succeed without having to sleep.
         */
        while (true) {
          AtomicInteger refCount = fSearchers.get(currentSearcher);
          if (refCount == null)
            break;
          else if (refCount.get() == 0) {
            /*
             * This may be called at the same time from disposeIfNecessary, but
             * that's fine.
             */
            dispose(currentSearcher);
            break;
          } else {
            try {
              /*
               * We sleep with a lock held because the Threads that we're
               * waiting to make progress don't acquire a lock
               */
              Thread.sleep(100);
            } catch (InterruptedException e) {
              throw new PersistenceException("Failed to close IndexSearcher: " + fSearcher);
            }
          }
        }
      }
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  private static String prepareForParsing(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      /* Escape Special Characters being used in Lucene */
      if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '{' || c == '}' || c == '~')
        sb.append('\\');

      sb.append(c);
    }
    return sb.toString();
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IModelSearch#addIndexListener(org.rssowl
   * .core.persist.service.IndexListener)
   */
  public void addIndexListener(IndexListener listener) {
    fIndexListeners.add(listener);
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IModelSearch#removeIndexListener(org.rssowl
   * .core.persist.service.IndexListener)
   */
  public void removeIndexListener(IndexListener listener) {
    fIndexListeners.remove(listener);
  }

  /*
   * @see org.rssowl.core.persist.service.IModelSearch#optimize()
   */
  public void optimize() {
    try {
      fIndexer.optimize();
    } catch (IOException e) {
      throw new PersistenceException(e.getMessage(), e);
    }
  }

  void notifyIndexUpdated(int docCount) {
    for (IndexListener listener : fIndexListeners) {
      listener.indexUpdated(docCount);
    }
  }

  /*
   * @see
   * org.rssowl.core.persist.service.IModelSearch#reindexAll(org.eclipse.core
   * .runtime.IProgressMonitor)
   */
  public void reindexAll(IProgressMonitor monitor) throws PersistenceException {
    /* May be used before Owl is completely set-up */
    Collection<INews> newsList = InternalOwl.getDefault().getPersistenceService().getDAOService().getNewsDAO().loadAll();

    monitor.beginTask("Re-Indexing all News", newsList.size());

    EntityIdsByEventType entitiesToBeIndexed = DBHelper.getEntitiesToBeIndexedDAO().load();

    /* Ensure that we don't lose entities on dirty shutdown */
    synchronized (entitiesToBeIndexed) {
      for (INews news : newsList)
        entitiesToBeIndexed.addUpdatedEntity(news);
    }

    DBHelper.getEntitiesToBeIndexedDAO().save(entitiesToBeIndexed);
    /* Lock the indexer for the duration of the reindexing */
    synchronized (fIndexer) {
      /* Delete the Index first */
      clearIndex();

      /*
       * Re-Index all Entities: News. newsList is a LazyList so news are only
       * activated on retrieval
       */
      for (INews news : newsList) {
        if (monitor.isCanceled())
          break;

        /* We don't pass the whole list at once to be able to report progress. */
        List<INews> indexList = new ArrayList<INews>(1);
        indexList.add(news);
        fIndexer.index(indexList, false);
        monitor.worked(1);
      }
      /* Commit in order to avoid first search slowdown */
      fIndexer.flushIfNecessary();
    }

    /* Finished */
    monitor.done();
  }
}