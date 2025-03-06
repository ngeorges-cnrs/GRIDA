/* Copyright CNRS-CREATIS
 *
 * Rafael Ferreira da Silva
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
package fr.insalyon.creatis.grida.server.dao;

import fr.insalyon.creatis.grida.server.Configuration;
import fr.insalyon.creatis.grida.server.dao.h2.CacheFileData;
import fr.insalyon.creatis.grida.server.dao.h2.CacheListData;
import fr.insalyon.creatis.grida.server.dao.h2.PoolData;
import fr.insalyon.creatis.grida.server.dao.h2.ZombieFilesData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Ferreira da Silva
 */
public class H2DAOFactory extends DAOFactory {

    private static final Logger logger = Logger.getLogger(H2DAOFactory.class);
    private static H2DAOFactory instance;
    private final String DRIVER = "org.h2.Driver";
    private final String DBURL = "jdbc:h2:db/vlet-agent.db";
    private Connection connection;

    public static H2DAOFactory getInstance()
            throws DAOException {
        if (instance == null) {
            if (!Configuration.getInstance().getFeatures().hasPool) {
                throw new DAOException("Can't create H2DAOFactory: pool is disabled");
            }
            instance = new H2DAOFactory();
        }
        return instance;
    }

    private H2DAOFactory() {
        super();
    }

    @Override
    protected void connect() {
        try {
            Class.forName(DRIVER);
            connection = DriverManager.getConnection(DBURL + ";create=true");
            connection.setAutoCommit(true);

        } catch (SQLException ex) {
            try {
                connection = DriverManager.getConnection(DBURL);
                connection.setAutoCommit(true);

            } catch (SQLException ex1) {
                logException(ex);
            }
        } catch (ClassNotFoundException ex) {
            logException(ex);
        }
    }

    @Override
    protected void createTables() {
        try {
            Statement stat = connection.createStatement();
            stat.executeUpdate("CREATE TABLE Pool ("
                    + "id VARCHAR(100), "
                    + "registration TIMESTAMP, "
                    + "source CLOB, "
                    + "dest VARCHAR(255), "
                    + "operation VARCHAR(50), "
                    + "status VARCHAR(50), "
                    + "username VARCHAR(255), "
                    + "proxy VARCHAR(100), "
                    + "retrycount INT, "
                    + "size DOUBLE, "
                    + "PRIMARY KEY (id)"
                    + ")");
            stat.executeUpdate("CREATE INDEX user_idx ON Pool(username)");

        } catch (SQLException ex) {
            logger.info("Table Pool already exists!");
        }

        try {
            Statement stat = connection.createStatement();
            stat.executeUpdate(
                    "CREATE TABLE CacheList ("
                    + "path VARCHAR(255), "
                    + "list CLOB, "
                    + "frequency INT, "
                    + "last_usage TIMESTAMP, "
                    + "last_refresh TIMESTAMP, "
                    + "PRIMARY KEY (path)"
                    + ")");

        } catch (SQLException ex) {
            logger.info("Table CacheList already exists!");
        }

        try {
            Statement stat = connection.createStatement();
            stat.executeUpdate(
                    "CREATE TABLE CacheFile ("
                    + "path VARCHAR(255), "
                    + "name VARCHAR(255), "
                    + "size DOUBLE, "
                    + "frequency INT, "
                    + "last_usage TIMESTAMP, "
                    + "PRIMARY KEY (path)"
                    + ")");

        } catch (SQLException ex) {
            logger.info("Table CacheFile already exists!");
        }

        try {
            Statement stat = connection.createStatement();
            stat.executeUpdate(
                    "CREATE TABLE ZombieFiles ("
                    + "srm VARCHAR(255), "
                    + "registration TIMESTAMP, "
                    + "PRIMARY KEY(srm)"
                    + ")");

        } catch (SQLException ex) {
            logger.info("Table already exists!");
        }
    }

    @Override
    public PoolDAO getPoolDAO() {
        return PoolData.getInstance(connection);
    }

    @Override
    public CacheListDAO getCacheListDAO() {
        return CacheListData.getInstance(connection);
    }

    @Override
    public CacheFileDAO getCacheFileDAO() {
        return CacheFileData.getInstance(connection);
    }

    @Override
    public ZombieFilesDAO getZombieFilesDAO() {
        return ZombieFilesData.getInstance(connection);
    }

    private void logException(Exception ex) {
        logger.error(ex);
        if (logger.isDebugEnabled()) {
            for (StackTraceElement stack : ex.getStackTrace()) {
                logger.debug(stack);
            }
        }
    }
}
