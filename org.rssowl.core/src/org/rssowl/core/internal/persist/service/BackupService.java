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
import org.rssowl.core.persist.service.PersistenceException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class BackupService {

  interface BackupStrategy    {
    void backup(File originFile, File destinationFile);
  }

  interface BackupLayoutStrategy    {
    List<File> findBackupFiles();

    /**
     * Responsible for rotating back-up files to allow BackupService to save
     * a new back-up.
     *
     * <p>Note that this method is only called if maxBackupsCount
     * is higher than 1. If maxBackupsCount is equal to 1, the back-up files are
     * simply deleted to create space for the new one.</p>
     *
     * @param backupFiles files to be rotated.
     */
    void rotateBackups(List<File> backupFiles);
  }


  static class DefaultBackupLayoutStrategy implements BackupLayoutStrategy {
    private final File fBackupFile;

    DefaultBackupLayoutStrategy(File backupFile)    {
      fBackupFile = backupFile;
    }
    public List<File> findBackupFiles() {
      int index = 0;
      List<File> backupFiles = new ArrayList<File>(5);
      File tempFile = fBackupFile;
      while (tempFile.exists()) {
        backupFiles.add(tempFile);
        tempFile = new File(fBackupFile.getAbsolutePath() + "." + index++);
      }
      return backupFiles;
    }

    public void rotateBackups(List<File> backupFiles) {
      int index;
      while (backupFiles.size() > 0) {
        index = backupFiles.size() - 1;
        File fileToRename = backupFiles.remove(index);
        /*
         * index is correct here because filesToRename includes a back up file
         * with no index as well as .0 index
         */
        File newFile = new File(fBackupFile.getAbsolutePath() + "." + index);
        if (!fileToRename.renameTo(newFile)) {
          throw new PersistenceException("Failed to rename file from " + fileToRename + " to " + newFile);
        }
      }
    }
  }

  private final File fFileToBackup;
  private final String fBackupFileSuffix;
  private final int fMaxBackupsCount;
  private final File fBackupTimestampFile;
  private final Long fBackupFrequency;

  private BackupLayoutStrategy fLayoutStrategy;
  private File fFileToBackupAlias;
  private BackupStrategy fBackupStrategy;

  public BackupService(File fileToBackup, String backupFileSuffix, int maxBackupsCount) {
    this(fileToBackup, backupFileSuffix, maxBackupsCount, null, null);
  }

  public BackupService(File fileToBackup, String backupFileSuffix, int maxBackupsCount,
      File backupTimestampFile, Long backupFrequency) {
    Assert.isNotNull(fileToBackup, "fileToBackup");
    Assert.isLegal(fileToBackup.isFile(), "fileToBackup must be a file: " + fileToBackup.getAbsolutePath());
    Assert.isLegal(backupFileSuffix  != null && backupFileSuffix.length() > 0, "backupSuffix should contain a non-empty String");
    Assert.isLegal(maxBackupsCount > 0, "filesKeptCount should be higher than 0");
    if (backupFrequency != null)
      Assert.isNotNull(backupTimestampFile, "backupTimestampFile should not be null if backupFrequency is not null");

    fFileToBackup = fileToBackup;
    fBackupFileSuffix = backupFileSuffix;
    fMaxBackupsCount = maxBackupsCount;
    fBackupTimestampFile = backupTimestampFile;
    fBackupFrequency = backupFrequency;

    fLayoutStrategy = new DefaultBackupLayoutStrategy(getBackupFile());
    fBackupStrategy = new BackupStrategy() {
      public void backup(File originFile, File destinationFile) {
        DBHelper.copyFile(originFile, destinationFile);
      }
    };
  }

  public void setBackupStrategy(BackupStrategy backupStrategy) {
    fBackupStrategy = backupStrategy;
  }

  /**
   * This overrides where the back-up is made from. It's useful if fileToBackup
   * is being used, there's a copy available and the back-up file name should
   * be relative to fileToBackup.
   * @param alias
   */
  public void setFileToBackupAlias(File alias) {
    Assert.isLegal(alias.isFile(), "alias must be a file");
    fFileToBackupAlias = alias;
  }

  public void setLayoutStrategy(BackupLayoutStrategy layoutStrategy) {
    fLayoutStrategy = layoutStrategy;
  }

  public File getFileToBackup() {
    return fFileToBackup;
  }

  /**
   * Deletes old backups and rotates the existing ones as appropriate.
   */
  private void prepareBackup() {
    final File backupFile = getBackupFile();

    List<File> backupFiles = fLayoutStrategy.findBackupFiles();

    deleteOldBackups(backupFiles);

    if (!backupFiles.isEmpty())
      fLayoutStrategy.rotateBackups(backupFiles);

    Assert.isLegal(!backupFile.exists(), "backupFile should have been rotated or deleted: " + backupFile);
  }

  /**
   * Backs up the file in any of the following:
   *
   * <li>force is {@code true}.</li>
   * <li>backupTimestampFile is {@code null}.</li>
   * <li>The time since the last backup is higher or equal than backupFrequency.</li>
   *
   * @param force
   * @return {@code true} if a backup took place.
   * @throws PersistenceException if a problem occurs during back-up.
   */
  public boolean backup(boolean force) throws PersistenceException  {
    if (!shouldBackup(force))
      return false;

    prepareBackup();

    File sourceFile = fFileToBackup;
    if (fFileToBackupAlias != null)
      sourceFile = fFileToBackupAlias;

    fBackupStrategy.backup(sourceFile, getBackupFile());
    writeBackupTimestamp();
    return true;
  }

  private boolean shouldBackup(boolean force) {
    if (force)
      return true;

    if (!fBackupTimestampFile.exists()) {
      writeBackupTimestamp();
      return false;
    }

    try {
      long lastBackupTimestamp = Long.parseLong(DBHelper.readFirstLineFromFile(
          fBackupTimestampFile));
      long now = System.currentTimeMillis();
      return (now - lastBackupTimestamp) >= fBackupFrequency.longValue();
    } catch (NumberFormatException e) {
      throw new PersistenceException(fBackupTimestampFile.getAbsolutePath() + " does not contain a number for the date as expected", e);
    }
  }

  private void writeBackupTimestamp() {
    if (fBackupTimestampFile == null)
      return;

    if (!fBackupTimestampFile.exists()) {
      try {
        fBackupTimestampFile.createNewFile();
      } catch (IOException e) {
        throw new PersistenceException("Failed to create new file", e);
      }
    }
    DBHelper.writeToFile(fBackupTimestampFile, String.valueOf(System.currentTimeMillis()));
  }

  public File getBackupFile() {
    String backupFilePath = fFileToBackup.getAbsolutePath() + fBackupFileSuffix;
    File backupFile = new File(backupFilePath);
    return backupFile;
  }

  public File getBackupFile(int index) {
    List<File> backupFiles = fLayoutStrategy.findBackupFiles();
    if (index >= backupFiles.size())
      return null;

    return backupFiles.get(index);
  }

  public File getTempBackupFile() {
    return new File(getBackupFile().getAbsolutePath() + ".temp");
  }

  public File getCorruptedFile(Integer index) {
    String fileName = getFileToBackup().getAbsolutePath() + ".corrupted";
    if (index != null)
      fileName += "." + index;

    return new File(fileName);
  }

  private void deleteOldBackups(List<File> backupFiles) {
    /* We're creating a new back-up, so must leave one space available */
    while (backupFiles.size() > (fMaxBackupsCount - 1)) {
      File fileToDelete = backupFiles.remove(backupFiles.size() - 1);
      if (!fileToDelete.delete()) {
        throw new PersistenceException("Failed to delete file: " + fileToDelete);
      }
    }
  }
}
