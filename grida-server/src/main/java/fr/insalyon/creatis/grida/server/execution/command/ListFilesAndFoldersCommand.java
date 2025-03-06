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
package fr.insalyon.creatis.grida.server.execution.command;

import fr.insalyon.creatis.grida.common.Communication;
import fr.insalyon.creatis.grida.common.Constants;
import fr.insalyon.creatis.grida.common.bean.GridData;
import fr.insalyon.creatis.grida.server.Configuration;
import fr.insalyon.creatis.grida.server.business.BusinessException;
import fr.insalyon.creatis.grida.server.business.CacheBusiness;
import fr.insalyon.creatis.grida.server.business.OperationBusiness;
import fr.insalyon.creatis.grida.server.execution.Command;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 * @author Rafael Silva
 */
public class ListFilesAndFoldersCommand extends Command {

    private static final Logger logger = Logger.getLogger(ListFilesAndFoldersCommand.class);
    private String path;
    private boolean refresh;
    private OperationBusiness operationBusiness;
    private CacheBusiness cacheBusiness;

    public ListFilesAndFoldersCommand(Communication communication,
            String proxyFileName, String path, String refresh) {

        super(communication, proxyFileName);
        this.path = path;
        this.refresh = Boolean.valueOf(refresh);
        
        operationBusiness = new OperationBusiness(proxyFileName);
    }

    @Override
    public void execute() {

        try {
            cacheBusiness = Configuration.getInstance().getFeatures().hasCache ? new CacheBusiness() : null;

            if (refresh) {
                getDataList();

            } else {
                if (cacheBusiness != null && cacheBusiness.hasValidCacheData(path)) {
                    logger.info("Listing Files and Folders from cache: " + path);
                    for (String data : cacheBusiness.getCachedPaths(path)) {
                        communication.sendMessage(data);
                    }

                } else {
                    logger.info("Cache list expired for path: " + path);
                    getDataList();
                }
            }
        } catch (BusinessException ex) {
            communication.sendErrorMessage(ex.getMessage());
        }
        communication.sendEndOfMessage();
    }

    /**
     * 
     * @throws BusinessException 
     */
    private void getDataList() throws BusinessException {

        List<String> dataList = new ArrayList<String>();
        for (GridData data : operationBusiness.listFilesAndFolders(path)) {

            String dataPath = data.getType() == GridData.Type.Folder
                    ? data.getName() + Constants.MSG_SEP_2
                    + data.getType().name() + Constants.MSG_SEP_2
                    + data.getPermissions()
                    : data.getName() + Constants.MSG_SEP_2
                    + data.getType().name() + Constants.MSG_SEP_2
                    + data.getLength() + Constants.MSG_SEP_2
                    + data.getModificationDate() + Constants.MSG_SEP_2
                    + data.getReplicas() + Constants.MSG_SEP_2
                    + data.getPermissions() + Constants.MSG_SEP_2
                    + data.getComment();

            communication.sendMessage(dataPath);
            dataList.add(dataPath);
        }
        if (cacheBusiness != null) {
            cacheBusiness.addPathToCache(path, dataList);
        }
    }
}
