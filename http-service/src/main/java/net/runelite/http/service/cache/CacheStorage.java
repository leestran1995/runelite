/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.http.service.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.runelite.cache.fs.Archive;
import net.runelite.cache.fs.Index;
import net.runelite.cache.fs.Storage;
import net.runelite.cache.fs.Store;
import net.runelite.cache.index.FileData;
import net.runelite.http.service.cache.beans.ArchiveEntry;
import net.runelite.http.service.cache.beans.CacheEntry;
import net.runelite.http.service.cache.beans.FileEntry;
import net.runelite.http.service.cache.beans.IndexEntry;
import org.sql2o.Connection;
import org.sql2o.ResultSetIterable;

public class CacheStorage implements Storage
{
	private CacheEntry cacheEntry;
	private final CacheDAO cacheDao;
	private final Connection con;

	public CacheStorage(CacheEntry cacheEntry, CacheDAO cacheDao, Connection con)
	{
		this.cacheEntry = cacheEntry;
		this.cacheDao = cacheDao;
		this.con = con;
	}

	public CacheEntry getCacheEntry()
	{
		return cacheEntry;
	}

	public void setCacheEntry(CacheEntry cacheEntry)
	{
		this.cacheEntry = cacheEntry;
	}

	@Override
	public void init(Store store) throws IOException
	{
	}

	@Override
	public void close() throws IOException
	{
	}

	@Override
	public void load(Store store) throws IOException
	{
		List<IndexEntry> indexes = cacheDao.findIndexesForCache(con, cacheEntry);
		List<FileData> fileData = new ArrayList<>();
		for (IndexEntry indexEntry : indexes)
		{
			Index index = store.addIndex(indexEntry.getIndexId());
			index.setCrc(indexEntry.getCrc());
			index.setRevision(indexEntry.getRevision());

			try (ResultSetIterable<ArchiveEntry> archives = cacheDao.findArchivesForIndex(con, indexEntry))
			{
				for (ArchiveEntry archiveEntry : archives)
				{
					Archive archive = index.addArchive(archiveEntry.getArchiveId());
					archive.setNameHash(archiveEntry.getNameHash());
					archive.setCrc(archiveEntry.getCrc());
					archive.setRevision(archiveEntry.getRevision());
					archive.setHash(archiveEntry.getHash());

					try (ResultSetIterable<FileEntry> files = cacheDao.findFilesForArchive(con, archiveEntry))
					{
						fileData.clear();

						for (FileEntry fileEntry : files)
						{
							FileData file = new FileData();
							file.setId(fileEntry.getFileId());
							file.setNameHash(fileEntry.getNameHash());
							fileData.add(file);
						}

						FileData[] fileDataArray = new FileData[fileData.size()];
						fileData.toArray(fileDataArray);
						archive.setFileData(fileDataArray);
					}
				}
			}
		}
	}

	@Override
	public void save(Store store) throws IOException
	{
		for (Index index : store.getIndexes())
		{
			IndexEntry entry = cacheDao.findOrCreateIndex(con, cacheEntry, index.getId(), index.getCrc(), index.getRevision());
			// this assumes nothing is associated to the cache yet
			cacheDao.associateIndexToCache(con, cacheEntry, entry);

			for (Archive archive : index.getArchives())
			{
				ArchiveEntry archiveEntry = cacheDao.findArchive(con, entry, archive.getArchiveId(),
					archive.getNameHash(), archive.getCrc(), archive.getRevision());
				if (archiveEntry == null)
				{
					byte[] hash = archive.getHash();
					archiveEntry = cacheDao.createArchive(con, entry, archive.getArchiveId(),
						archive.getNameHash(), archive.getCrc(), archive.getRevision(), hash);

					for (FileData file : archive.getFileData())
					{
						cacheDao.associateFileToArchive(con, archiveEntry, file.getId(), file.getNameHash());
					}
				}
				cacheDao.associateArchiveToIndex(con, archiveEntry, entry);
			}
		}
	}

	@Override
	public byte[] loadArchive(Archive archive) throws IOException
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void saveArchive(Archive archive, byte[] data) throws IOException
	{
		throw new UnsupportedOperationException();
	}

}
