package vn.ssdc.vnpt.policy.model;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import vn.ssdc.vnpt.policy.services.PolicyJobService;

/**
 * Created by THANHLX on 12/27/2017.
 */
public class DeletePolicyQuartzJob implements Job {
    public static final Logger logger = LoggerFactory.getLogger(PolicyQuartzJob.class);
    @Autowired
    PolicyJobService policyJobService;

    // Excute a job policy
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        JobDataMap jdm = jobExecutionContext.getMergedJobDataMap();
        Long policyJobId = jdm.getLong("policyJobId");
        logger.info("#DELETE_POLICY_QUARTZ");
        try {
            //1st.Delete Job
            policyJobService.deleteQuartzJob(policyJobId);
            //2st.Delete Trigger
            policyJobService.deleteTriger(policyJobId);
            //3st.Delete Preset
            policyJobService.deletePreset(policyJobId);

            PolicyJob policyJob = policyJobService.get(policyJobId);
            policyJob.status = "STOP";
            policyJobService.update(policyJobId, policyJob);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error(ex.toString());
        }
    }
}