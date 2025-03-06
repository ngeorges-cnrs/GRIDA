/* Copyright CNRS-CREATIS
 *
 * Rafael Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is a grid-enabled data-driven workflow manager and editor.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.insalyon.creatis.grida.server.business;

import fr.insalyon.creatis.grida.server.dao.CacheFileDAO;
import fr.insalyon.creatis.grida.server.dao.DAOException;
import fr.insalyon.creatis.grida.server.dao.DAOFactory;
import fr.insalyon.creatis.grida.common.bean.CachedFile;
import fr.insalyon.creatis.grida.server.Configuration;
import fr.insalyon.creatis.grida.server.dao.CacheListDAO;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class CacheBusiness {

    private static final Logger logger = Logger.getLogger(CacheBusiness.class);
    private Configuration configuration;
    private CacheFileDAO cacheFileDAO;
    private CacheListDAO cacheListDAO;

    public CacheBusiness() throws BusinessException {

        configuration = Configuration.getInstance();
        try {
            cacheFileDAO = DAOFactory.getDAOFactory().getCacheFileDAO();
            cacheListDAO = DAOFactory.getDAOFactory().getCacheListDAO();
        } catch(DAOException e) {
            throw new BusinessException(e);
        }
    }

    /**
     * Gets the cache file path of a remote file.
     * 
     * @param path
     * @return 
     */
    public String getCachedFileName(String path) throws BusinessException {

        try {
            return cacheFileDAO.getName(path);

        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    /**
     * Adds a file to the cache.
     * 
     * @param cacheName
     * @param sourcePath
     * @param remoteFilePath
     * @throws BusinessException 
     */
    public void addFileToCache(String cacheName, String sourcePath,
            String remoteFilePath) throws BusinessException {

        try {
            long sourceSize = FileUtils.sizeOf(new File(sourcePath));

            if ((double) sourceSize <= configuration.getCacheFilesMaxSize()) {

                if (cacheName == null) {
                    cacheName = configuration.getCacheFilesPath()
                            + "/" + System.nanoTime() + "-file";
                    logger.info("Adding file \"" + remoteFilePath + "\" to cache.");

                } else {
                    cacheFileDAO.delete(remoteFilePath);
                    DiskspaceManager.deleteQuietly(new File(cacheName));
                    logger.info("Updating file \"" + remoteFilePath + "\" in the cache.");
                }

                if (configuration.getCacheFilesMaxSize()
                        - cacheFileDAO.getTotalUsedSpace() <= sourceSize) {

                    for (String name : cacheFileDAO.delete(sourceSize)) {
                        DiskspaceManager.deleteQuietly(new File(name));
                    }
                }
                FileUtils.copyFile(new File(sourcePath), new File(cacheName));
                cacheFileDAO.add(new CachedFile(remoteFilePath, cacheName, sourceSize));

            } else {
                logger.warn("Unable to add file \"" + remoteFilePath
                        + "\" to the cache. File is bigger than cache size.");
            }
        } catch (IOException ex) {
            logger.error(ex);
            throw new BusinessException(ex);
        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    /**
     * Updates file frequency and last usage.
     * 
     * @param remoteFilePath
     * @throws BusinessException 
     */
    public void updateFile(String remoteFilePath) throws BusinessException {

        try {
            cacheFileDAO.update(remoteFilePath);
        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    /**
     * Gets list of cached files.
     * 
     * @return
     * @throws BusinessException 
     */
    public List<CachedFile> getCachedFiles() throws BusinessException {

        try {
            return cacheFileDAO.getFiles();

        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    /**
     * Deletes a cached file.
     * 
     * @param path
     * @throws BusinessException 
     */
    public void deleteCachedFile(String path) throws BusinessException {

        try {
            DiskspaceManager.deleteQuietly(new File(cacheFileDAO.getFile(path).getName()));
            cacheFileDAO.delete(path);

        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    /**
     * Adds a path to the cache.
     * 
     * @param path
     * @param dataList
     * @throws BusinessException 
     */
    public void addPathToCache(String path, List<String> dataList) throws BusinessException {

        try {
            if (cacheListDAO.getNumberOfEntries() == Configuration.getInstance().getCacheListMaxEntries()) {
                cacheListDAO.delete();
            }
            if (cacheListDAO.getPathList(path) == null) {
                cacheListDAO.add(path, dataList);
            } else {
                cacheListDAO.update(path, dataList);
            }
        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    /**
     * Verifies if the data in cache is still valid.
     * 
     * @param path
     * @return
     * @throws BusinessException 
     */
    public boolean hasValidCacheData(String path) throws BusinessException {

        try {
            Date lastRefresh = cacheListDAO.getLastRefresh(path);
            if (lastRefresh != null) {
                Calendar currentCal = Calendar.getInstance();
                Calendar calLastRefresh = Calendar.getInstance();
                calLastRefresh.setTime(lastRefresh);
                calLastRefresh.add(Calendar.HOUR_OF_DAY,
                        Configuration.getInstance().getCacheListMaxHours());

                return calLastRefresh.after(currentCal) ? true : false;

            } else {
                return false;
            }
        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }

    /**
     * Gets list of cached paths.
     * 
     * @param path
     * @return
     * @throws BusinessException 
     */
    public List<String> getCachedPaths(String path) throws BusinessException {

        try {
            return cacheListDAO.getPathList(path);

        } catch (DAOException ex) {
            throw new BusinessException(ex);
        }
    }
}
