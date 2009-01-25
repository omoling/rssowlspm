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
package org.rssowl.core.internal.persist.service;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.rssowl.core.internal.Activator;
import org.rssowl.core.internal.InternalOwl;
import org.rssowl.core.internal.persist.AbstractEntity;
import org.rssowl.core.internal.persist.BookMark;
import org.rssowl.core.internal.persist.ConditionalGet;
import org.rssowl.core.internal.persist.Description;
import org.rssowl.core.internal.persist.Feed;
import org.rssowl.core.internal.persist.Folder;
import org.rssowl.core.internal.persist.Label;
import org.rssowl.core.internal.persist.News;
import org.rssowl.core.internal.persist.NewsBin;
import org.rssowl.core.internal.persist.Preference;
import org.rssowl.core.internal.persist.migration.MigrationResult;
import org.rssowl.core.internal.persist.migration.Migrations;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.NewsCounter;
import org.rssowl.core.persist.NewsCounterItem;
import org.rssowl.core.persist.INews.State;
import org.rssowl.core.persist.reference.NewsReference;
import org.rssowl.core.persist.service.DiskFullException;
import org.rssowl.core.persist.service.IModelSearch;
import org.rssowl.core.persist.service.InsufficientFilePermissionException;
import org.rssowl.core.persist.service.PersistenceException;
import org.rssowl.core.util.LoggingSafeRunnable;
import org.rssowl.core.util.LongOperationMonitor;

import com.db4o.Db4o;
import com.db4o.ObjectContainer;
import com.db4o.config.Configuration;
import com.db4o.config.ObjectClass;
import com.db4o.config.ObjectField;
import com.db4o.config.QueryEvaluationMode;
import com.db4o.ext.DatabaseFileLockedException;
import com.db4o.query.Query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DBManager {
  private static final int MAX_BACKUPS_COUNT = 2;
  private static final String FORMAT_FILE_NAME = "format2";
  private static DBManager fInstance;
  private ObjectContainer fObjectContainer;
  private final ReadWriteLock fLock = new ReentrantReadWriteLock();
  private final List<DatabaseListener> fEntityStoreListeners = new CopyOnWriteArrayList<DatabaseListener>();
  private IStatus startupStatus;

  /**
   * @return The Singleton Instance.
   */
  public static DBManager getDefault() {
    if (fInstance == null)
      fInstance = new DBManager();
    return fInstance;
  }

  /**
   * Load and initialize the contributed DataBase.
   * @param monitor
   *
   * @throws PersistenceException In case of an error while initializing and loading the
   * contributed DataBase.
   */
  public void startup(LongOperationMonitor monitor) throws PersistenceException {
    /* Initialise */
    EventManager.getInstance();

    createDatabase(monitor);
  }

  public void addEntityStoreListener(DatabaseListener listener) {
    if (listener instanceof EventManager)
      fEntityStoreListeners.add(0, listener);
    else if (listener instanceof DB4OIDGenerator) {
      if (fEntityStoreListeners.get(0) instanceof EventManager)
        fEntityStoreListeners.add(1, listener);
      else
        fEntityStoreListeners.add(0, listener);
    } else
      fEntityStoreListeners.add(listener);
  }

  private void fireDatabaseEvent(DatabaseEvent event, boolean storeOpened) {
    for (DatabaseListener listener : fEntityStoreListeners) {
      if (storeOpened) {
        listener.databaseOpened(event);
      } else {
        listener.databaseClosed(event);
      }
    }
  }

  /**
   * There was an error creating the
   */
  private void createEmptyObjectContainer(Configuration config, IStatus status) {
    /* Log in case there's also an exception creating an empty object container */
    Activator.getDefault().getLog().log(status);
    fObjectContainer = Db4o.openFile(config, getDBFilePath());
  }

  private IStatus createObjectContainer(Configuration config) {
    IStatus status = null;
    try {
      fObjectContainer = Db4o.openFile(config, getDBFilePath());
      status = Status.OK_STATUS;
    } catch (Throwable e) {
      if (e instanceof Error)
        throw (Error) e;

      File file = new File(getDBFilePath());

      if (!file.exists())
        throw new DiskFullException("Failed to create an empty database. This seems to indicate that the disk is full. Please fix the issue and restart RSSOwl.", e);

      if (!file.canRead() || (!file.canWrite()))
        throw new InsufficientFilePermissionException("Current user has no permission to read and/or write file: " + file + ". Please fix the issue and restart RSSOwl.", null);


      BackupService backupService = createOnlineBackupService();
      if (backupService == null || e instanceof DatabaseFileLockedException)
        throw new PersistenceException(e);

      BackupService scheduledBackupService = createScheduledBackupService(null);
      File currentDbCorruptedFile = backupService.getCorruptedFile(null);
      DBHelper.rename(backupService.getFileToBackup(), currentDbCorruptedFile);
      /*
       * There was no online back-up file. This could only happen if the problem
       * happened on the first start-up or if the user never used the application
       * for more than 10 minutes.
       */
      if (backupService.getBackupFile(0) == null) {
        status = Activator.getDefault().createErrorStatus("Database file is corrupted and no back-up could be found. The corrupted file has been saved to: " + currentDbCorruptedFile.getAbsolutePath(), e);
        createEmptyObjectContainer(config, status);
      } else {
        status = restoreFromBackup(config, e, currentDbCorruptedFile, backupService, scheduledBackupService);
      }
    }

    Assert.isNotNull(status, "status");
    final BackupService backupService = createOnlineBackupService();
    Job job = new Job("Back-up service") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        backupService.backup(true);
        schedule(getOnlineBackupDelay(false));
        return Status.OK_STATUS;
      }
    };
    job.setSystem(true);
    job.schedule(getOnlineBackupDelay(true));
    return status;
  }

  private void checkDirPermissions() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    if (!dir.canRead() || (!dir.canWrite()))
      throw new InsufficientFilePermissionException("Current user has no permission to read from and/or write to directory: " + dir + "Please fix the issue and restart RSSOwl.", null);
  }

  private IStatus restoreFromBackup(Configuration config, Throwable startupException, File currentDbCorruptedFile, BackupService... backupServices) {
    Assert.isNotNull(backupServices, "backupServices");
    Assert.isLegal(backupServices.length > 0, "backupServices should have at least one element");
    Assert.isNotNull(backupServices[0].getBackupFile(0), "backupServices[0] should contain at least one back-up");
    long lastModified = -1;
    boolean foundSuitableBackup = false;
    for (BackupService backupService : backupServices) {
      for (int i = 0;; ++i) {
        File backupFile = backupService.getBackupFile(i);
        /* Always false in first iteration */
        if (backupFile == null)
          break;

        lastModified = backupFile.lastModified();

        DBHelper.rename(backupFile, backupService.getFileToBackup());
        try {
          fObjectContainer = Db4o.openFile(config, getDBFilePath());
          foundSuitableBackup = true;
          break;
        } catch (Throwable e1) {
          Activator.getDefault().logError("Back-up database corrupted: " + backupFile, e1);
          DBHelper.rename(new File(getDBFilePath()), backupService.getCorruptedFile(i));
        }
      }
      if (foundSuitableBackup)
        break;
    }

    if (foundSuitableBackup) {
      String message = createRecoveredFromCorruptedDatabaseMessage(currentDbCorruptedFile, lastModified);
      return Activator.getDefault().createErrorStatus(message, startupException);
    }

    IStatus status = Activator.getDefault().createErrorStatus("Database file and its back-ups are all corrupted. The corrupted database file has been saved to: " + currentDbCorruptedFile.getAbsolutePath(), startupException);
    createEmptyObjectContainer(config, status);
    return status;
  }

  private String createRecoveredFromCorruptedDatabaseMessage(File corruptedFile, long lastModified) {
    String date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(lastModified));
    return "There was a problem opening the database file. RSSOwl has reverted to the last working back-up (from " + date + "). The corrupted file has been saved to: " + corruptedFile.getAbsolutePath();
  }

  private boolean shouldReindex(MigrationResult migrationResult, IStatus startupStatus) {
    boolean shouldReindex = migrationResult.isReindex() || (!startupStatus.isOK());
    if (shouldReindex)
      return true;

    return Boolean.getBoolean("rssowl.reindex");
  }

  private long getOnlineBackupDelay(boolean initial) {
    if (initial)
      return 1000 * 60 * 10;

    return getLongProperty("rssowl.onlinebackup.interval", 1000 * 60 * 60 * 4);
  }

  private long getLongProperty(String propertyName, long defaultValue) {
    String backupIntervalText = System.getProperty(propertyName);

    if (backupIntervalText != null) {
      try {
        long backupInterval = Long.parseLong(backupIntervalText);
        if (backupInterval > 0) {
          return backupInterval;
        }
      } catch (NumberFormatException e) {
        /* Let it fall through and use default */
      }
    }
    return defaultValue;
  }

  private BackupService createOnlineBackupService() {
    File file = new File(getDBFilePath());

    /* No database file exists, so no back-up can exist */
    if (!file.exists())
      return null;

    BackupService backupService = new BackupService(file, ".onlinebak", 2);
    backupService.setBackupStrategy(new BackupService.BackupStrategy() {
      public void backup(File originFile, File destinationFile) {
        try {
          /* Relies on fObjectContainer being set before calling backup */
          fObjectContainer.ext().backup(destinationFile.getAbsolutePath());
        } catch (IOException e) {
          throw new PersistenceException(e);
        }
      }
    });
    return backupService;
  }

  /**
   * @return the File indicating whether defragment should be run or not.
   */
  public File getDefragmentFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    return new File(dir, "defragment");
  }

  /**
   * Internal method, exposed for tests only.
   * @return the path to the db file.
   */
  public static final String getDBFilePath() {
    String filePath = Activator.getDefault().getStateLocation().toOSString() + File.separator + "rssowl.db"; //$NON-NLS-1$
    return filePath;
  }

  private File getDBFormatFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File formatFile = new File(dir, FORMAT_FILE_NAME);
    return formatFile;
  }

  public void removeEntityStoreListener(DatabaseListener listener) {
    fEntityStoreListeners.remove(listener);
  }

  public void createDatabase(LongOperationMonitor progressMonitor) throws PersistenceException {
    checkDirPermissions();
    Configuration config = createConfiguration();
    int workspaceVersion = getWorkspaceFormatVersion();
    MigrationResult migrationResult = new MigrationResult(false, false, false);

    SubMonitor subMonitor = null;
    try {
      if (workspaceVersion != getCurrentFormatVersion()) {
        progressMonitor.beginLongOperation();
        subMonitor = SubMonitor.convert(progressMonitor, "Please wait while RSSOwl migrates data to the new version", 100);
        //TODO Have a better way to allocate the ticks to the child. We need
        //to be able to do it dynamically based on whether a reindex is required or not.
        migrationResult = migrate(workspaceVersion, getCurrentFormatVersion(), subMonitor.newChild(70));
      }

      if (!defragmentIfNecessary(progressMonitor, subMonitor)) {
        if (migrationResult.isDefragmentDatabase())
          defragment(progressMonitor, subMonitor);
        /*
         * We only run the time-based back-up if a defragment has not taken
         * place because we always back-up during defragment.
         */
        else
          scheduledBackup();
      }

      startupStatus = createObjectContainer(config);

      if (startupStatus.isOK())
        fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), true);

      boolean shouldReindex = shouldReindex(migrationResult, startupStatus);
      if (subMonitor == null && shouldReindex) {
        progressMonitor.beginLongOperation();
        subMonitor = SubMonitor.convert(progressMonitor, "Please wait while RSSOwl reindexes your data", 20);
      }

      IModelSearch modelSearch = InternalOwl.getDefault().getPersistenceService().getModelSearch();
      if (shouldReindex || migrationResult.isOptimizeIndex()) {
        modelSearch.startup();
        if (shouldReindex)
          modelSearch.reindexAll(subMonitor.newChild(20));
        if (migrationResult.isOptimizeIndex())
          modelSearch.optimize();
      }

    } finally {
      /*
       * If we perform the migration, the subMonitor is not null. Otherwise we
       * don't show progress.
       */
      if (subMonitor != null)
        progressMonitor.done();
    }
  }

  private BackupService createScheduledBackupService(Long backupFrequency) {
    return new BackupService(new File(getDBFilePath()), ".backup", MAX_BACKUPS_COUNT, getDBLastBackUpFile(), backupFrequency);
  }

  private void scheduledBackup() {
    if (!new File(getDBFilePath()).exists())
      return;

    long sevenDays = getLongProperty("rssowl.offlinebackup.interval", 1000 * 60 * 60 * 24 * 7);
    createScheduledBackupService(sevenDays).backup(false);
  }

  public File getDBLastBackUpFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File lastBackUpFile = new File(dir, "lastbackup");
    return lastBackUpFile;
  }

  private MigrationResult migrate(final int workspaceFormat, int currentFormat, IProgressMonitor progressMonitor) {
    ConfigurationFactory configFactory = new ConfigurationFactory() {
      public Configuration createConfiguration() {
        return DBManager.createConfiguration();
      }
    };
    Migration migration = new Migrations().getMigration(workspaceFormat, currentFormat);
    if (migration == null) {
      throw new PersistenceException("It was not possible to migrate your data to the current version of RSSOwl. Migrations are supported between final versions and between consecutive milestones. In other words, 2.0M7 to 2.0M8 and 2.0 to 2.1 are supported but 2.0M6 to 2.0M8 is not supported. In the latter case, you would need to launch 2.0M7 and then 2.0M8 to be able to use that version. Migration was attempted from originFormat: " + workspaceFormat + " to destinationFormat: " + currentFormat);
    }

    final File dbFile = new File(getDBFilePath());
    final String backupFileSuffix = ".mig.";

    /*
     * Copy the db file to a permanent back-up where the file name includes the
     * workspaceFormat number. This will only be deleted after another migration.
     */
    final BackupService backupService = new BackupService(dbFile, backupFileSuffix + workspaceFormat, 1);
    backupService.setLayoutStrategy(new BackupService.BackupLayoutStrategy() {
      public List<File> findBackupFiles() {
        List<File> backupFiles = new ArrayList<File>(3);
        for (int i = workspaceFormat; i >= 0; --i) {
          File file = new File(dbFile.getAbsoluteFile() + backupFileSuffix + i);
          if (file.exists())
            backupFiles.add(file);
        }
        return backupFiles;
      }

      public void rotateBackups(List<File> backupFiles) {
        throw new UnsupportedOperationException("No rotation supported because maxBackupCount is 1");
      }
    });
    backupService.backup(true);

    /* Create a copy of the db file to use for the migration */
    File migDbFile = backupService.getTempBackupFile();
    DBHelper.copyFile(dbFile, migDbFile);

    /* Migrate the copy */
    MigrationResult migrationResult = migration.migrate(configFactory, migDbFile.getAbsolutePath(), progressMonitor);

    File dbFormatFile = getDBFormatFile();
    File migFormatFile = new File(dbFormatFile.getAbsolutePath() + ".mig.temp");
    try {
      if (!migFormatFile.exists()) {
        migFormatFile.createNewFile();
      }
      if (!dbFormatFile.exists()) {
        dbFormatFile.createNewFile();
      }
    } catch (IOException ioe) {
      throw new PersistenceException("Failed to migrate data", ioe); //$NON-NLS-1$
    }
    setFormatVersion(migFormatFile);

    DBHelper.rename(migFormatFile, dbFormatFile);

    /* Finally, rename the actual db file */
    DBHelper.rename(migDbFile, dbFile);

    //TODO Remove this after M9
    if (getOldDBFormatFile().exists())
      getOldDBFormatFile().delete();

    return migrationResult;
  }

  private File getOldDBFormatFile() {
    File dir = new File(Activator.getDefault().getStateLocation().toOSString());
    File formatFile = new File(dir, "format");
    return formatFile;
  }

  private int getWorkspaceFormatVersion() {
    boolean dbFileExists = new File(getDBFilePath()).exists();
    File formatFile = getDBFormatFile();
    boolean formatFileExists = formatFile.exists();

    //TODO Remove this after M9 release and change the code to assume that if
    //no format2 file exists, then the version is lower than M8
    if (!formatFileExists && getOldDBFormatFile().exists()) {
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(getOldDBFormatFile()));
        String text = reader.readLine();
        DBHelper.writeToFile(formatFile, text);
        formatFileExists = true;
      } catch (IOException e) {
        throw new PersistenceException(e);
      } finally {
        DBHelper.closeQuietly(reader);
      }
    }

    if (dbFileExists) {
      /* Assume that it's M5a if no format file exists, but a db file exists */
      if (!formatFileExists)
        return 0;

      String versionText = DBHelper.readFirstLineFromFile(formatFile);
      try {
        int version = Integer.parseInt(versionText);
        return version;
      } catch (NumberFormatException e) {
        throw new PersistenceException("Format file does not contain a number as the version", e);
      }
    }
    /*
     * In case there is no database file, we just set the version as the current
     * version.
     */
    if (!formatFileExists) {
      try {
        formatFile.createNewFile();
      } catch (IOException ioe) {
        throw new PersistenceException("Error creating database", ioe); //$NON-NLS-1$
      }
    }
    setFormatVersion(formatFile);
    return getCurrentFormatVersion();
  }

  private void setFormatVersion(File formatFile) {
    DBHelper.writeToFile(formatFile, String.valueOf(getCurrentFormatVersion()));
  }

  private int getCurrentFormatVersion() {
    return 5;
  }

  private boolean defragmentIfNecessary(LongOperationMonitor progressMonitor, SubMonitor subMonitor) {
    File defragmentFile = getDefragmentFile();
    if (!defragmentFile.exists()) {
      return false;
    }
    if (!defragmentFile.delete()) {
      Activator.getDefault().logError("Failed to delete defragment file", null);
    }
    defragment(progressMonitor, subMonitor);
    return true;
  }

  private void defragment(LongOperationMonitor progressMonitor, SubMonitor subMonitor) {
    SubMonitor monitor;
    if (subMonitor == null) {
      progressMonitor.beginLongOperation();
      String monitorText = "Please wait while RSSOwl cleans up the database";
      subMonitor = SubMonitor.convert(progressMonitor, monitorText, 100);
      monitor = subMonitor.newChild(100);

      /*
       * This should not be needed, but things don't work properly when it's
       * not called.
       */
      monitor.beginTask(monitorText, 100);
    } else {
      monitor = subMonitor.newChild(10);
      monitor.setWorkRemaining(100);
    }

    BackupService backupService = createScheduledBackupService(null);
    File file = new File(getDBFilePath());
    File defragmentedFile = backupService.getTempBackupFile();
    copyDatabase(file, defragmentedFile, monitor);
    backupService.backup(true);
    DBHelper.rename(defragmentedFile, file);
  }

  /**
   * Internal method. Made public for testing.
   *
   * Creates a copy of the database that has all essential data structures.
   * At the moment, this means not copying NewsCounter and
   * IConditionalGets since they will be re-populated eventually.
   * @param source
   * @param destination
   * @param monitor
   *
   */
  public final static void copyDatabase(File source, File destination, IProgressMonitor monitor) {
    ObjectContainer sourceDb = Db4o.openFile(createConfiguration(), source.getAbsolutePath());
    ObjectContainer destinationDb = Db4o.openFile(createConfiguration(), destination.getAbsolutePath());

    /*
     * Keep labels in memory to avoid duplicate copies when cascading feed.
     */
    List<Label> labels = new ArrayList<Label>();
    for (Label label : sourceDb.query(Label.class)) {
      labels.add(label);
      sourceDb.activate(label, Integer.MAX_VALUE);
      destinationDb.ext().set(label, Integer.MAX_VALUE);
    }

    monitor.worked(5);
    for (Folder type : sourceDb.query(Folder.class)) {
      sourceDb.activate(type, Integer.MAX_VALUE);
      if (type.getParent() == null) {
        destinationDb.ext().set(type, Integer.MAX_VALUE);
      }
    }
    monitor.worked(15);

    /*
     * We use destinationDb for the query here because we have already copied
     * the NewsBins at this stage and we may need to fix the NewsBin in case
     * it contains stale news refs.
     */
    for (NewsBin newsBin : destinationDb.query(NewsBin.class)) {
      destinationDb.activate(newsBin, Integer.MAX_VALUE);
      List<NewsReference> staleNewsRefs = new ArrayList<NewsReference>(0);
      for (NewsReference newsRef : newsBin.getNewsRefs()) {
        Query query = sourceDb.query();
        query.constrain(News.class);
        query.descend("fId").constrain(newsRef.getId());
        Iterator<?> newsIt = query.execute().iterator();
        if (!newsIt.hasNext()) {
          Activator.getDefault().logError("NewsBin " + newsBin + " has reference to news with id: " + newsRef.getId() + ", but that news does not exist.", null);
          staleNewsRefs.add(newsRef);
          continue;
        }
        Object news = newsIt.next();
        sourceDb.activate(news, Integer.MAX_VALUE);
        destinationDb.ext().set(news, Integer.MAX_VALUE);
      }
      if (!staleNewsRefs.isEmpty()) {
        newsBin.removeNewsRefs(staleNewsRefs);
        destinationDb.ext().set(newsBin, Integer.MAX_VALUE);
      }
    }

    monitor.worked(25);

    int feedCounter = 0;
    NewsCounter newsCounter = new NewsCounter();
    for (Feed feed : sourceDb.query(Feed.class)) {
      sourceDb.activate(feed, Integer.MAX_VALUE);
      addNewsCounterItem(newsCounter, feed);
      destinationDb.ext().set(feed, Integer.MAX_VALUE);

      ++feedCounter;
      if (feedCounter % 40 == 0)
        System.gc();
    }
    System.gc();

    destinationDb.ext().set(newsCounter, Integer.MAX_VALUE);
    monitor.worked(30);

    int descriptionCounter = 0;
    for (Description description : sourceDb.query(Description.class)) {
      sourceDb.activate(description, Integer.MAX_VALUE);
      destinationDb.ext().set(description, Integer.MAX_VALUE);

      ++descriptionCounter;
      if (descriptionCounter % 600 == 0)
        System.gc();
    }
    monitor.worked(10);

    for (Preference pref : sourceDb.query(Preference.class)) {
      sourceDb.activate(pref, Integer.MAX_VALUE);
      destinationDb.ext().set(pref, Integer.MAX_VALUE);
    }
    monitor.worked(5);
    List<Counter> counterSet = sourceDb.query(Counter.class);
    Counter counter = counterSet.iterator().next();
    sourceDb.activate(counter, Integer.MAX_VALUE);
    destinationDb.ext().set(counter, Integer.MAX_VALUE);

    sourceDb.close();
    destinationDb.commit();
    destinationDb.close();
    System.gc();
    monitor.worked(10);
  }

  private static void addNewsCounterItem(NewsCounter newsCounter, Feed feed) {
    Map<State, Integer> stateToCountMap = feed.getNewsCount();
    int unreadCount = getCount(stateToCountMap, EnumSet.of(State.NEW, State.UNREAD, State.UPDATED));
    Integer newCount = stateToCountMap.get(INews.State.NEW);
    newsCounter.put(feed.getLink(), new NewsCounterItem(newCount, unreadCount, feed.getStickyCount()));
  }

  private static int getCount(Map<State, Integer> stateToCountMap, Set<State> states) {
    int count = 0;
    for (State state : states) {
      count += stateToCountMap.get(state);
    }
    return count;
  }

  /**
   * Internal method, exposed for tests only.
   *
   * @return
   */
  public static final Configuration createConfiguration() {
    Configuration config = Db4o.newConfiguration();
    //TODO We can use dbExists to configure our parameters for a more
    //efficient startup. For example, the following could be used. We'd have
    //to include a file when we need to evolve the schema or something similar
    //config.detectSchemaChanges(false)

//    config.blockSize(8);
//    config.bTreeCacheHeight(0);
//    config.bTreeNodeSize(100);
//    config.diagnostic().addListener(new DiagnosticListener() {
//      public void onDiagnostic(Diagnostic d) {
//        System.out.println(d);
//      }
//    });
//    config.messageLevel(3);

    config.lockDatabaseFile(false);
    config.queries().evaluationMode(QueryEvaluationMode.IMMEDIATE);
    config.automaticShutDown(false);
	config.callbacks(false);
    config.activationDepth(2);
    config.flushFileBuffers(false);
    config.callConstructors(true);
    config.exceptionsOnNotStorable(true);
    configureAbstractEntity(config);
    config.objectClass(BookMark.class).objectField("fFeedLink").indexed(true); //$NON-NLS-1$
    config.objectClass(ConditionalGet.class).objectField("fLink").indexed(true); //$NON-NLS-1$
    configureFeed(config);
    configureNews(config);
    configureFolder(config);
    config.objectClass(Description.class).objectField("fNewsId").indexed(true);
    config.objectClass(NewsCounter.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).cascadeOnDelete(true);
    config.objectClass(Preference.class).objectField("fKey").indexed(true); //$NON-NLS-1$
    return config;
  }

  private static void configureAbstractEntity(Configuration config) {
    ObjectClass abstractEntityClass = config.objectClass(AbstractEntity.class);
    ObjectField idField = abstractEntityClass.objectField("fId");
    idField.indexed(true);
    idField.cascadeOnActivate(true);
    abstractEntityClass.objectField("fProperties").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private static void configureFolder(Configuration config) {
    ObjectClass oc = config.objectClass(Folder.class);
    oc.objectField("fChildren").cascadeOnUpdate(true); //$NON-NLS-1$
  }

  private static void configureNews(Configuration config) {
    ObjectClass oc = config.objectClass(News.class);

    /* Indexes */
    oc.objectField("fParentId").indexed(true);
    oc.objectField("fFeedLink").indexed(true); //$NON-NLS-1$
    oc.objectField("fStateOrdinal").indexed(true); //$NON-NLS-1$
  }

  private static void configureFeed(Configuration config)  {
    ObjectClass oc = config.objectClass(Feed.class);

    ObjectField linkText = oc.objectField("fLinkText"); //$NON-NLS-1$
    linkText.indexed(true);
    linkText.cascadeOnActivate(true);

    oc.objectField("fTitle").cascadeOnActivate(true); //$NON-NLS-1$
  }

  /**
   * Shutdown the contributed Database.
   *
   * @throws PersistenceException In case of an error while shutting down the contributed
   * DataBase.
   */
  public void shutdown() throws PersistenceException {
    fLock.writeLock().lock();
    try {
      fireDatabaseEvent(new DatabaseEvent(fObjectContainer, fLock), false);
      if (fObjectContainer != null)
        while (!fObjectContainer.close());
    } finally {
      fLock.writeLock().unlock();
    }
  }

  public void dropDatabase() throws PersistenceException {
    SafeRunner.run(new LoggingSafeRunnable() {
      public void run() throws Exception {
        shutdown();
        if (!new File(getDBFilePath()).delete()) {
          Activator.getDefault().logError("Failed to delete db file", null); //$NON-NLS-1$
        }
        if (!getDBFormatFile().delete()) {
          Activator.getDefault().logError("Failed to delete db format file", null); //$NON-NLS-1$
        }
      }
    });
  }

  public final ObjectContainer getObjectContainer() {
    return fObjectContainer;
  }

  public IStatus getStartupStatus() {
    return startupStatus;
  }
}
