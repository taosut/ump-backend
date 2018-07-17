package vn.ssdc.vnpt.devices.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.ssdc.vnpt.AcsClient;
import vn.ssdc.vnpt.devices.model.DiagnosticTask;
import vn.ssdc.vnpt.devices.model.Parameter;
import vn.vnpt.ssdc.core.SsdcCrudService;
import vn.vnpt.ssdc.jdbc.factories.RepositoryFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Admin on 2/17/2017.
 */
@Service
public class DiagnosticService extends SsdcCrudService<Long, DiagnosticTask> {
    @Autowired
    private AcsClient acsClient;
    @Autowired
    public DiagnosticService(RepositoryFactory repositoryFactory) {
        this.repository = repositoryFactory.create(DiagnosticTask.class);
    }

    public List<DiagnosticTask> findByPk(String deviceId, int offset, int limit, String mode) {
        List<DiagnosticTask> listTask = null;
        String whereExp = "device_id=? order by id desc limit ?,? ";
        if (("all").equals(mode)) {
            whereExp = "device_id=? order by id desc ";
            listTask = this.repository.search(whereExp, deviceId);
        }
        else if(("0").equals(mode)) {
            whereExp = "device_id=? and status is null order by id desc ";
            listTask = this.repository.search(whereExp, deviceId);
        }
        else if(("1").equals(mode) || ("2").equals(mode)) {
            whereExp = "device_id=? and status=? order by id desc ";
            listTask = this.repository.search(whereExp, deviceId, mode);
        }
        else listTask = this.repository.search(whereExp, deviceId, (offset - 1) * limit, limit);
        return listTask;
    }

    public DiagnosticTask findByTaskId(String taskId){
        List<DiagnosticTask> list = this.repository.search("task_id=?",taskId);
        if(list.size()>0){
            return list.get(0);
        }
        return null;
    }

    private void createDiagnostic(DiagnosticTask dm) {
        this.create(dm);
    }

    public void updateResult(Long id){
        DiagnosticTask diagnosticTask = this.get(id);
        List<String> listPath = new ArrayList<String>();
        for (Map.Entry<String, Parameter> entry : diagnosticTask.parameterFull.entrySet())
        {
            listPath.add(entry.getValue().path);
        }

        Map<String,String> listCurrentValues = this.acsClient.getDeviceParameters(diagnosticTask.deviceId,listPath);
        boolean checkResult = true;
        for (Map.Entry<String, String> entry : diagnosticTask.request.entrySet())
        {
            if(!entry.getKey().contains("DiagnosticsState")){
                if(!entry.getValue().equals(listCurrentValues.get(entry.getKey()))){
                    checkResult = false;
                }
            }
        }
        if(checkResult) {
            //Code check diagsnotic here
            diagnosticTask.result = listCurrentValues;
            diagnosticTask.completed = System.currentTimeMillis();
            diagnosticTask.status = 2;
            for (Map.Entry<String, String> entry : listCurrentValues.entrySet()) {
                if (entry.getKey().contains("DiagnosticsState")) {
                    if (entry.getValue().equals("Complete")) {
                        diagnosticTask.status = 1;
                    }
                }
            }
            this.update(diagnosticTask.id, diagnosticTask);
        }
    }

    public DiagnosticTask findInProcess(String deviceId){
        String whereExp = "completed is null AND device_id = '"+deviceId+"' ORDER BY created DESC LIMIT 1";
        List<DiagnosticTask> taskList = this.repository.search(whereExp);
        if(taskList.size()>0){
            return taskList.get(0);
        }
        return null;
    }
}
